#!/usr/bin/env python3
"""ST-01 P8-C2-B counterfactual rarity audit: analysis path 1 (observations -> candidate records, scenarios, statistics).

    python3 -I tools/st01-p8c2b-audit/analyze.py <results-dir> <out-dir> [--seeds seeds.json]

<results-dir>/<seed>/observations-<n>.json are written by St01CounterfactualProbe (raw measurements only). This script
derives every pass/fail, the 2x2 table, the four scenarios, the geometry classification of fluid cells and the statistics
from the RAW measurements; nothing the probe computed is trusted except the production verdict, which is only compared.

Outputs (stable schema, see docs): candidates.csv, candidates.json, summary.json, fluid_cells.json.
Constants are read from the production St01PlacementConfig.java source (not copied), so a drift fails loudly.
"""
import base64, csv, gzip, importlib.util, json, math, random, re, struct, sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CFG = (ROOT / 'src/main/java/dev/itzgamesgd/moreminecrafts/pyramid/worldgen/St01PlacementConfig.java').read_text()


def const(name):
    m = re.search(r'public static final (?:int|String) %s\s*=\s*([^;]+);' % name, CFG)
    if not m: raise SystemExit('constant %s not found in St01PlacementConfig.java' % name)
    v = m.group(1).strip()
    return v.strip('"') if v.startswith('"') else int(v)


HALF = const('FOOTPRINT_HALF_WIDTH'); MAX_DIFF = const('MAX_HEIGHT_DIFFERENCE'); MAX_CUT = const('MAX_CUT'); MAX_SKIRT = const('MAX_SKIRT')
STEP_UP = const('ENTRANCE_MAX_STEP_UP'); MARGIN = const('FLUID_MARGIN'); SPACING = const('SPACING_CHUNKS'); SEPARATION = const('SEPARATION_CHUNKS')
SALT = const('SALT'); EW = const('ENTRANCE_HALF_WIDTH')
VOL = re.search(r'UNDERGROUND_VOLUME = new St01Eligibility\.Volume\(([^)]*)\)', CFG).group(1)
VMIN_DX, VMAX_DX, VMIN_DY, VMAX_DY, VMIN_DZ, VMAX_DZ = [int(x) for x in VOL.split(',')]
assert (HALF, MAX_DIFF, MAX_CUT, MAX_SKIRT, STEP_UP, MARGIN, SPACING, SEPARATION, SALT) == (40, 16, 10, 8, 1, 1, 32, 8, 91401), 'locked constants changed'

GEOM = None  # authored grid, loaded lazily (numpy via the P8-C2-A tooling)


def authored_designs():
    global GEOM
    if GEOM is None:
        sys.path.insert(0, str(ROOT / 'tools/st01-p8c2a'))
        spec = importlib.util.spec_from_file_location('verify_placement', ROOT / 'tools/st01-p8c2a/verify_placement.py')
        V = importlib.util.module_from_spec(spec); spec.loader.exec_module(V)
        X = V.X
        bp = json.loads(X.BLUEPRINT.read_bytes()); d1 = X.read_nbt(X.D1.read_bytes())
        GEOM = {}
        for route in X.ROUTES:
            palette, grid, written, _ = V.design(route, bp, d1)
            air = {i for i, s in enumerate(palette) if s['id'] in ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air')}
            GEOM[route] = (palette, grid, written, air, V.LO, V.SHAPE)
    return GEOM


def classify_cell(route, ax, ay, az):
    """Authored class of the authored-frame cell (relative to the ground anchor): OUTSIDE_FRAME/SOLID/CAVITY_AIR/UNWRITTEN/AUTHORED_FLUID."""
    palette, grid, written, air, lo, shape = authored_designs()[route]
    ix, iy, iz = ax - lo[0], ay - lo[1], az - lo[2]
    if not (0 <= ix < shape[0] and 0 <= iy < shape[1] and 0 <= iz < shape[2]): return 'OUTSIDE_FRAME'
    if not written[ix, iy, iz]: return 'UNWRITTEN'
    g = int(grid[ix, iy, iz])
    if g in air: return 'CAVITY_AIR'
    sid = palette[g]['id']
    return 'AUTHORED_FLUID' if sid in ('minecraft:water', 'minecraft:lava') else 'SOLID'


def to_authored(rec, dx, dz):
    """Inverse of the authored->world offset rotation, from the vectors the probe exported (orthonormal integer matrix)."""
    uxx, uxz = rec['authored_unit_x_to_world']; uzx, uzz = rec['authored_unit_z_to_world']
    return dx * uxx + dz * uxz, dx * uzx + dz * uzz   # M^T * world


def poisson_cdf(k, mu):
    term = total = math.exp(-mu)
    for i in range(1, k + 1):
        term *= mu / i; total += term
    return total


def garwood(k, alpha=0.05):
    def solve(f, lo, hi):
        for _ in range(200):
            mid = (lo + hi) / 2
            lo, hi = (mid, hi) if f(mid) else (lo, mid)
        return (lo + hi) / 2
    lower = 0.0 if k == 0 else solve(lambda mu: 1 - poisson_cdf(k - 1, mu) < alpha / 2, 0.0, k + 50.0)
    upper = solve(lambda mu: poisson_cdf(k, mu) > alpha / 2, 0.0, k + 50.0 + 10 * math.sqrt(k + 1))
    return lower, upper


def fisher_two_sided(a, b, c, d):
    r1, r2, c1, n = a + b, c + d, a + c, a + b + c + d
    def p(x): return math.comb(r1, x) * math.comb(r2, c1 - x) / math.comb(n, c1)
    lo, hi = max(0, c1 - r2), min(r1, c1)
    pobs = p(a)
    return min(1.0, sum(p(x) for x in range(lo, hi + 1) if p(x) <= pobs * (1 + 1e-9)))


def load_observations(results):
    obs = {}
    for d in sorted(Path(results).iterdir()):
        f = d / 'observations-1.json'
        if d.is_dir() and f.exists():
            o = json.loads(f.read_text())
            if 'error' in o: raise SystemExit('probe error in %s: %s' % (f, o['error']))
            obs[o['seed']] = o
    return obs


def build_records(obs):
    records, fluid_cells = [], []
    for seed in sorted(obs):
        o = obs[seed]
        for c in o['candidates']:
            key = '%d,%d' % (c['chunk_x'], c['chunk_z'])
            raw = base64.b64decode(o['height_grids_int16le_b64'][key])
            heights = list(struct.unpack('<%dh' % (len(raw) // 2), raw))
            assert len(heights) == (2 * HALF + 1) ** 2
            hs = sorted(heights); mn, mx, yf = hs[0], hs[-1], hs[len(hs) // 2]
            diff, cut, skirt = mx - mn, max(0, mx - yf), max(0, yf - mn)
            assert (mn, mx, yf) == (c['min_height'], c['max_height'], c['foundation_y']), 'height recomputation mismatch %s' % key
            footprint_desert = c['footprint_desert']; stub = c['stub_cell_desert']
            height_ok, cut_ok, skirt_ok = diff <= MAX_DIFF, cut <= MAX_CUT, skirt <= MAX_SKIRT
            terrain_ok = footprint_desert and stub and height_ok and cut_ok and skirt_ok
            # entrance: obstruction above walking level = raw first-free-Y minus foundation level
            obstr = [h - yf for h in c['entrance_approach_heights']]
            max_obstr = max(obstr)
            entrance_ok = max_obstr <= STEP_UP
            cols_over = sum(1 for v in obstr if v > STEP_UP)
            outside = all(to_authored(c, dx, dz)[1] > HALF for dx, dz in c['entrance_approach_offsets'])
            inside_edge_rise = c['entrance_inside_edge_row_max_height'] - yf
            # fluid
            runs = c['fluid_runs_dx_dz_dy0_dy1_kind']
            wl = c['fluid_blocks_total_water_lava_other']
            total_fluid = sum(wl)
            in_vol = 0; by_dy = Counter(); cls = Counter(); adj_cavity_margin = 0; touches_boundary = 0
            kind_in_vol = Counter(); in_vol_cols = set(); margin_cols = set()
            for dx, dz, y0, y1, kind in runs:
                ax, az = to_authored(c, dx, dz)
                for dy in range(y0, y1 + 1):
                    inside = (VMIN_DX <= ax <= VMAX_DX and VMIN_DZ <= az <= VMAX_DZ and VMIN_DY <= dy <= VMAX_DY)
                    by_dy[dy] += 1
                    if inside:
                        in_vol += 1; kind_in_vol[kind] += 1; in_vol_cols.add((dx, dz))
                        k = classify_cell(c['route'], ax, dy, az)
                        cls['VOLUME_' + k] += 1
                        if abs(ax) == VMAX_DX or abs(az) == VMAX_DZ or dy == VMIN_DY: touches_boundary += 1
                    else:
                        margin_cols.add((dx, dz))
                        k = classify_cell(c['route'], ax, dy, az)
                        cls['MARGIN_' + k] += 1
                        # does a cavity cell sit face-adjacent to this margin fluid cell (an opening in the boundary)?
                        for nx, ny, nz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                            bx, by_, bz = ax + nx, dy + ny, az + nz
                            if VMIN_DX <= bx <= VMAX_DX and VMIN_DZ <= bz <= VMAX_DZ and VMIN_DY <= by_ <= VMAX_DY and classify_cell(c['route'], bx, by_, bz) == 'CAVITY_AIR':
                                adj_cavity_margin += 1; break
            fluid_ok_margin1 = total_fluid == 0
            fluid_ok_margin0 = in_vol == 0
            if total_fluid == 0: fluid_class = 'NONE'
            elif in_vol == 0: fluid_class = 'MARGIN_ONLY'
            elif cls['VOLUME_UNWRITTEN'] > 0 or cls['VOLUME_AUTHORED_FLUID'] > 0: fluid_class = 'UNKNOWN'
            elif cls['VOLUME_CAVITY_AIR'] > 0: fluid_class = 'IN_VOLUME_INCLUDES_AUTHORED_CAVITY'
            else: fluid_class = 'IN_VOLUME_SOLID_ONLY'
            rec = {
                'seed': seed, 'dimension': c['dimension'], 'chunk_x': c['chunk_x'], 'chunk_z': c['chunk_z'],
                'anchor_x': c['anchor_x'], 'anchor_z': c['anchor_z'], 'foundation_y': yf,
                'rotation': c['rotation'], 'orientation': c['orientation'], 'route': c['route'],
                'footprint_desert': footprint_desert, 'stub_cell_desert': stub, 'terrain_ok': terrain_ok,
                'min_height': mn, 'max_height': mx, 'height_variation': diff, 'required_cut': cut, 'required_skirt': skirt,
                'height_ok': height_ok, 'cut_ok': cut_ok, 'skirt_ok': skirt_ok,
                'entrance_max_obstruction': max_obstr, 'entrance_columns_over_threshold': cols_over, 'entrance_ok': entrance_ok,
                'entrance_row_outside_footprint': outside, 'entrance_inside_edge_row_rise': inside_edge_rise,
                'fluid_bounds': c['fluid_inspection_bounds'],
                'fluid_blocks_total': total_fluid, 'fluid_blocks_water': wl[0], 'fluid_blocks_lava': wl[1], 'fluid_blocks_other': wl[2],
                'fluid_blocks_in_volume': in_vol, 'fluid_blocks_margin_only': total_fluid - in_vol,
                'fluid_ok': fluid_ok_margin1, 'fluid_ok_without_margin': fluid_ok_margin0,
                'fluid_class': fluid_class, 'fluid_margin_adjacent_to_cavity_flag': adj_cavity_margin > 0, 'fluid_blocks_touching_volume_boundary': touches_boundary,
                'fluid_cells_by_class': dict(cls), 'fluid_margin_cells_adjacent_to_authored_cavity': adj_cavity_margin,
                'production_verdict': c['production_verdict'], 'actual_valid_start': c['actual_valid_start'],
                'vanilla_overlaps': c.get('vanilla_overlaps'),
            }
            rec['eligible_A'] = terrain_ok and entrance_ok and fluid_ok_margin1
            rec['eligible_B'] = terrain_ok and fluid_ok_margin1
            rec['eligible_C'] = terrain_ok and entrance_ok
            rec['eligible_D'] = terrain_ok
            records.append(rec)
            if terrain_ok:
                fluid_cells.append({'seed': seed, 'chunk': key, 'route': c['route'], 'orientation': c['orientation'], 'foundation_y': yf,
                                    'by_dy': {str(k): v for k, v in sorted(by_dy.items())}, 'class_counts': dict(cls), 'kinds_in_volume': {str(k): v for k, v in kind_in_vol.items()},
                                    'columns_in_volume': len(in_vol_cols), 'columns_margin_only': len(margin_cols - in_vol_cols)})
    return records, fluid_cells


def production_map(rec):
    """Production verdict implied by the raw observations under the production stage order."""
    if not rec['footprint_desert']: return 'REJECT_BIOME'
    if not rec['height_ok']: return 'REJECT_HEIGHT_DIFFERENCE'
    if not rec['cut_ok']: return 'REJECT_CUT'
    if not rec['skirt_ok']: return 'REJECT_SKIRT'
    if not rec['stub_cell_desert']: return 'REJECT_BIOME'
    if not rec['entrance_ok']: return 'REJECT_ENTRANCE'
    if not rec['fluid_ok']: return 'REJECT_FLUID'
    return 'ELIGIBLE'


def percentile(sorted_vals, q):
    if not sorted_vals: return None
    k = (len(sorted_vals) - 1) * q; f = math.floor(k); c = math.ceil(k)
    return sorted_vals[f] if f == c else sorted_vals[f] + (sorted_vals[c] - sorted_vals[f]) * (k - f)


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    results, out = Path(args[0]), Path(args[1]); out.mkdir(parents=True, exist_ok=True)
    obs = load_observations(results)
    seeds = sorted(obs)
    records, fluid_cells = build_records(obs)

    # ---- integrity: uniqueness, production agreement ---------------------------------------------------------------
    keys = [(r['seed'], r['chunk_x'], r['chunk_z']) for r in records]
    dup = len(keys) - len(set(keys))
    mism = [(r['seed'], r['chunk_x'], r['chunk_z'], r['production_verdict'], production_map(r)) for r in records if r['production_verdict'] != production_map(r)]
    # every desert-footprint candidate is in the records; non-desert ones are counted from the production verdict counters
    potential = sum(o['potential_anchors'] for o in obs.values())
    prod_counts = Counter()
    for o in obs.values(): prod_counts.update(o['production_verdict_counts'])
    actual_starts = sum(o['actual_valid_starts']['count'] for o in obs.values())
    desert_records = len(records)
    # candidates counted as REJECT_BIOME by production inside the desert-footprint population (stub-cell failures)
    stub_failures = sum(1 for r in records if r['footprint_desert'] and not r['stub_cell_desert'] and r['height_ok'] and r['cut_ok'] and r['skirt_ok'])

    # ---- funnel (production-order, from raw) ------------------------------------------------------------------------
    full_desert = desert_records
    terrain_q = [r for r in records if r['terrain_ok']]
    n11 = sum(1 for r in terrain_q if r['entrance_ok'] and r['fluid_ok'])
    n10 = sum(1 for r in terrain_q if r['entrance_ok'] and not r['fluid_ok'])
    n01 = sum(1 for r in terrain_q if not r['entrance_ok'] and r['fluid_ok'])
    n00 = sum(1 for r in terrain_q if not r['entrance_ok'] and not r['fluid_ok'])
    T = len(terrain_q)
    scen = {s: sum(1 for r in records if r['eligible_' + s]) for s in 'ABCD'}
    funnel = {'potential_anchors': potential, 'full_desert_footprint': full_desert,
              'terrain_qualified': T, 'entrance_qualified_sequential': n11 + n10, 'fluid_qualified_after_entrance': n11,
              'actual_valid_starts_in_real_world': actual_starts}

    # ---- per-seed and statistics --------------------------------------------------------------------------------------
    radius = obs[seeds[0]]['radius_blocks']
    area_seed = (2.0 * radius / 1000.0) ** 2          # km^2 of the square [-radius, radius]^2 scanned per seed
    per_seed = {}
    for s in seeds:
        rs = [r for r in records if r['seed'] == s]
        per_seed[s] = {'potential_anchors': obs[s]['potential_anchors'], 'full_desert': len(rs), 'terrain_qualified': sum(r['terrain_ok'] for r in rs),
                       **{'scenario_' + k: sum(1 for r in rs if r['eligible_' + k]) for k in 'ABCD'},
                       'actual_valid_starts': obs[s]['actual_valid_starts']['count']}
    area = area_seed * len(seeds)
    stats = {}
    rng = random.Random(20261111)
    B = 20000
    boots = {k: [] for k in 'ABCD'}
    for _ in range(B):
        pick = [seeds[rng.randrange(len(seeds))] for _ in seeds]
        for k in 'ABCD': boots[k].append(sum(per_seed[s]['scenario_' + k] for s in pick))
    for k in 'ABCD':
        counts = [per_seed[s]['scenario_' + k] for s in seeds]
        tot = sum(counts); lo, hi = garwood(tot)
        bs = sorted(v * 100.0 / area for v in boots[k])
        stats[k] = {'eligible_candidates': tot, 'per_seed': dict(zip(map(str, seeds), counts)), 'seeds_with_zero': sum(1 for v in counts if v == 0),
                    'mean_per_seed': tot / len(seeds), 'median_per_seed': percentile(sorted(counts), 0.5),
                    'density_per_100km2': tot * 100.0 / area, 'poisson_ci95_per_100km2': [lo * 100.0 / area, hi * 100.0 / area],
                    'cluster_bootstrap_ci95_per_100km2': [percentile(bs, 0.025), percentile(bs, 0.975)]}
    base = stats['A']['density_per_100km2']
    for k in 'ABCD':
        d = stats[k]['density_per_100km2']
        stats[k]['relative_to_A'] = d / base if base else None
        stats[k]['absolute_difference_vs_A_per_100km2'] = d - base
    ratio_boot = {}
    for k in 'BCD':
        rs = []
        for i in range(B):
            if boots['A'][i] > 0: rs.append(boots[k][i] / boots['A'][i])
        rs.sort()
        ratio_boot[k] = {'defined_resamples': len(rs), 'of': B, 'ci95': [percentile(rs, 0.025), percentile(rs, 0.975)] if rs else None}

    # ---- 2x2 interaction -------------------------------------------------------------------------------------------------
    pE_fail = (n01 + n00) / T if T else None; pF_fail = (n10 + n00) / T if T else None
    interaction = {
        'P(fluid_fail|entrance_pass)': n10 / (n11 + n10) if n11 + n10 else None,
        'P(fluid_fail|entrance_fail)': n00 / (n01 + n00) if n01 + n00 else None,
        'P(entrance_fail|fluid_pass)': n01 / (n11 + n01) if n11 + n01 else None,
        'P(entrance_fail|fluid_fail)': n00 / (n10 + n00) if n10 + n00 else None,
        'marginal_entrance_fail_rate': pE_fail, 'marginal_fluid_fail_rate': pF_fail,
        'expected_n00_if_independent': (T * pE_fail * pF_fail) if T else None,
        'odds_ratio_entrance_pass_vs_fluid_pass': ((n11 * n00) / (n10 * n01)) if n10 * n01 else None,
        'phi': ((n11 * n00 - n10 * n01) / math.sqrt((n11 + n10) * (n01 + n00) * (n11 + n01) * (n10 + n00))) if all(x > 0 for x in ((n11 + n10), (n01 + n00), (n11 + n01), (n10 + n00))) else None,
        'fisher_exact_two_sided_p': fisher_two_sided(n11, n10, n01, n00) if T else None,
    }

    # ---- entrance diagnostics (entrance-failing terrain-qualified candidates) ------------------------------------------
    ent_fail = [r for r in terrain_q if not r['entrance_ok']]
    excess = [r['entrance_max_obstruction'] - STEP_UP for r in ent_fail]
    entrance = {
        'failing': len(ent_fail), 'max_obstruction_values': sorted(r['entrance_max_obstruction'] for r in ent_fail),
        'excess_over_threshold_eq_1': sum(1 for e in excess if e == 1), 'excess_2_to_4': sum(1 for e in excess if 2 <= e <= 4),
        'excess_5_to_10': sum(1 for e in excess if 5 <= e <= 10), 'excess_over_10': sum(1 for e in excess if e > 10),
        'obstruction_exceeds_approved_cut_limit_10': sum(1 for r in ent_fail if r['entrance_max_obstruction'] > MAX_CUT),
        'row_outside_footprint_all': all(r['entrance_row_outside_footprint'] for r in terrain_q),
        'inside_edge_row_rise_values': sorted(r['entrance_inside_edge_row_rise'] for r in ent_fail),
        'columns_over_threshold_values': sorted(r['entrance_columns_over_threshold'] for r in ent_fail),
        'passing_max_obstruction_values': sorted(r['entrance_max_obstruction'] for r in terrain_q if r['entrance_ok']),
    }

    # ---- fluid diagnostics ----------------------------------------------------------------------------------------------
    fl_fail = [r for r in terrain_q if not r['fluid_ok']]
    cell_class = Counter(); dy_hist = Counter(); kinds = Counter(); band = Counter()
    for fc in fluid_cells:
        for k, v in fc['class_counts'].items(): cell_class[k] += v
        for k, v in fc['by_dy'].items():
            dy_hist[int(k)] += v
        for k, v in fc['kinds_in_volume'].items(): kinds[k] += v
    for dy, v in dy_hist.items(): band[(dy // 8) * 8] += v
    fluid = {
        'failing': len(fl_fail),
        'candidate_class_counts': dict(Counter(r['fluid_class'] for r in fl_fail)),
        'would_pass_without_margin': sum(1 for r in fl_fail if r['fluid_ok_without_margin']),
        'in_volume_fluid_blocks_total': sum(r['fluid_blocks_in_volume'] for r in fl_fail),
        'margin_only_fluid_blocks_total': sum(r['fluid_blocks_margin_only'] for r in fl_fail),
        'water_blocks_total': sum(r['fluid_blocks_water'] for r in fl_fail), 'lava_blocks_total': sum(r['fluid_blocks_lava'] for r in fl_fail),
        'other_blocks_total': sum(r['fluid_blocks_other'] for r in fl_fail),
        'fluid_blocks_by_dy_band_8': {str(k): v for k, v in sorted(band.items())},
        'fluid_blocks_by_dy_minus33_to_0': {str(k): v for k, v in sorted(dy_hist.items())},
        'cell_classes': dict(cell_class),
        'candidates_with_fluid_in_cavity': sum(1 for r in fl_fail if r['fluid_cells_by_class'].get('VOLUME_CAVITY_AIR', 0) > 0),
        'candidates_with_margin_fluid_adjacent_to_cavity': sum(1 for r in fl_fail if r['fluid_margin_cells_adjacent_to_authored_cavity'] > 0),
        'candidates_with_in_volume_fluid_touching_boundary_cells': sum(1 for r in fl_fail if r['fluid_blocks_touching_volume_boundary'] > 0),
        'candidates_with_fluid_in_unwritten_cells': sum(1 for r in fl_fail if r['fluid_cells_by_class'].get('VOLUME_UNWRITTEN', 0) > 0),
        'per_candidate_in_volume_fluid_blocks': sorted(r['fluid_blocks_in_volume'] for r in fl_fail),
    }

    # ---- overlaps -------------------------------------------------------------------------------------------------------
    ov = Counter(); ov_rows = []
    for r in terrain_q:
        for o in (r['vanilla_overlaps'] or []):
            ov[o['structure']] += 1
            ov_rows.append({'seed': r['seed'], 'chunk': '%d,%d' % (r['chunk_x'], r['chunk_z']), **o, 'A': r['eligible_A'], 'entrance_ok': r['entrance_ok'], 'fluid_ok': r['fluid_ok']})
    overlaps = {'terrain_qualified_candidates_with_any_overlap': sum(1 for r in terrain_q if r['vanilla_overlaps']), 'by_structure': dict(ov), 'rows': ov_rows,
                'by_scenario_eligible_with_overlap': {k: sum(1 for r in records if r['eligible_' + k] and r['vanilla_overlaps']) for k in 'ABCD'}}

    summary = {
        'schema': 'st01-cf-summary/1', 'seeds': seeds, 'radius_blocks': radius, 'area_km2_per_seed': area_seed, 'area_km2_total': area,
        'locked_constants': {'half': HALF, 'max_diff': MAX_DIFF, 'max_cut': MAX_CUT, 'max_skirt': MAX_SKIRT, 'entrance_step_up': STEP_UP, 'fluid_margin': MARGIN,
                             'volume': [VMIN_DX, VMAX_DX, VMIN_DY, VMAX_DY, VMIN_DZ, VMAX_DZ], 'spacing': SPACING, 'separation': SEPARATION, 'salt': SALT},
        'funnel': funnel, 'production_verdict_counts': dict(prod_counts), 'stub_cell_biome_rejections_after_terrain': stub_failures,
        'contingency_terrain_qualified': {'n11_ent_pass_fluid_pass': n11, 'n10_ent_pass_fluid_fail': n10, 'n01_ent_fail_fluid_pass': n01, 'n00_ent_fail_fluid_fail': n00, 'total': T},
        'scenario_counts': scen, 'scenario_stats': stats, 'ratio_bootstrap_vs_A': ratio_boot, 'interaction': interaction,
        'entrance': entrance, 'fluid': fluid, 'overlaps': overlaps, 'per_seed': {str(k): v for k, v in per_seed.items()},
        'integrity': {'duplicate_candidate_records': dup, 'production_verdict_disagreements': mism, 'candidate_records': len(records),
                      'route_counts_terrain_qualified': dict(Counter(r['route'] for r in terrain_q)),
                      'orientation_counts_terrain_qualified': dict(Counter(r['orientation'] for r in terrain_q))},
    }
    (out / 'summary.json').write_text(json.dumps(summary, indent=1, sort_keys=True))
    (out / 'candidates.json').write_text(json.dumps(records, indent=1, sort_keys=True))
    (out / 'fluid_cells.json').write_text(json.dumps(fluid_cells, indent=1, sort_keys=True))
    cols = ['seed', 'dimension', 'chunk_x', 'chunk_z', 'anchor_x', 'foundation_y', 'anchor_z', 'rotation', 'orientation', 'route', 'footprint_desert', 'stub_cell_desert',
            'min_height', 'max_height', 'height_variation', 'required_cut', 'required_skirt', 'terrain_ok', 'entrance_max_obstruction', 'entrance_columns_over_threshold',
            'entrance_ok', 'fluid_blocks_total', 'fluid_blocks_water', 'fluid_blocks_lava', 'fluid_blocks_in_volume', 'fluid_blocks_margin_only', 'fluid_ok', 'fluid_ok_without_margin',
            'fluid_class', 'production_verdict', 'actual_valid_start', 'eligible_A', 'eligible_B', 'eligible_C', 'eligible_D']
    with open(out / 'candidates.csv', 'w', newline='') as f:
        w = csv.writer(f, lineterminator='\n'); w.writerow(cols)
        for r in records: w.writerow([str(r[c]).lower() if isinstance(r[c], bool) else r[c] for c in cols])
    status = 'PASS' if (dup == 0 and not mism) else 'FAIL'
    print('analysis', status, json.dumps({'funnel': funnel, 'n': [n11, n10, n01, n00], 'scenarios': scen}))
    if status != 'PASS': sys.exit(1)


if __name__ == '__main__':
    main()
