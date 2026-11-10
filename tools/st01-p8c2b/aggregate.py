#!/usr/bin/env python3
"""ST-01 P8-C2-B: aggregate per-seed natural-worldgen results.

    python3 tools/st01-p8c2b/aggregate.py <dir with <seed>/scan.json [+ verification.json]> <out.json> <out.md>

Reports per-stage candidate counts, starts, completed pyramids, EAST/WEST and orientation counts, density per 100 km^2
with an exact (Garwood) Poisson 95 % interval, and the comparison with the Phase 3.1 / Phase 4 reference values.
Pure stdlib.
"""
import json, math, sys
from pathlib import Path

PHASE4_FIXTURE_DENSITY = 0.254          # Phase 4 royal-hall fixture, 15 seeds, per 100 km^2
PHASE31_MANSION_DENSITY = 0.715         # Phase 3.1 Woodland Mansion reference, per 100 km^2
PHASE31_REFERENCE = {1001: (16, 4), 1002: (18, 6), 1003: (4, 0), 1004: (8, 3), 1005: (10, 2)}  # (full-desert, moderate-eligible)


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


def main():
    src, out_json, out_md = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
    rows, pooled = [], {'area_km2': 0.0, 'starts': 0, 'completed': 0, 'EAST': 0, 'WEST': 0}
    stage_keys = ('potential_anchors', 'full_desert_qualified', 'terrain_qualified', 'entrance_qualified', 'fluid_safe_eligible_predicted', 'actual_valid_starts')
    stage_tot = {k: 0 for k in stage_keys}
    orient = {}
    rejection_tot = {}
    overlaps = []
    for d in sorted(p for p in src.iterdir() if p.is_dir() and (p / 'scan.json').exists()):
        scan = json.loads((d / 'scan.json').read_text())
        ver = json.loads((d / 'verification.json').read_text()) if (d / 'verification.json').exists() else None
        st = scan['anchor_statistics']
        seed = scan['seed']
        starts = scan['starts']
        routes = {'EAST': 0, 'WEST': 0}
        coords = []
        for k, s in starts.items():
            routes[s['route']] += 1
            orient[s['orientation']] = orient.get(s['orientation'], 0) + 1
            a = s['anchor']
            coords.append({'start_chunk': k, 'ground': [a['x'], a['y'], a['z']], 'orientation': s['orientation'], 'route': s['route'],
                           'terrain': s['terrain'], 'overlaps': s['vanilla_structure_overlaps'], 'guard_refused_writes': s['guard_refused_writes'],
                           'all_checks_pass': s['all_checks_pass']})
            for o in s['vanilla_structure_overlaps']: overlaps.append({'seed': seed, 'start_chunk': k, **o})
        completed = sum(1 for s in starts.values() if s['all_checks_pass'])
        nearest = min((math.hypot(c['ground'][0], c['ground'][2]) for c in coords), default=None)
        for k in stage_keys: stage_tot[k] += st[k]
        for k, v in st['rejection_counts'].items(): rejection_tot[k] = rejection_tot.get(k, 0) + v
        pooled['area_km2'] += st['area_km2']; pooled['starts'] += st['actual_valid_starts']; pooled['completed'] += completed
        pooled['EAST'] += routes['EAST']; pooled['WEST'] += routes['WEST']
        rows.append({'seed': seed, **{k: st[k] for k in stage_keys}, 'rejections': st['rejection_counts'],
                     'completed': completed, 'density_per_100km2': st['density_per_100km2'], 'routes': routes,
                     'nearest_start_blocks': nearest, 'verification': None if ver is None else ver['status'],
                     'phase31_reference': PHASE31_REFERENCE.get(seed), 'starts': coords})
    k = pooled['starts']; area = pooled['area_km2']
    lo, hi = garwood(k)
    agg = {'seeds': len(rows), 'pooled_area_km2': area, 'pooled_starts': k, 'pooled_completed': pooled['completed'],
           'pooled_density_per_100km2': k * 100 / area if area else None,
           'pooled_density_95ci': [lo * 100 / area, hi * 100 / area] if area else None,
           'seeds_without_start': sum(1 for r in rows if r['actual_valid_starts'] == 0),
           'routes': {'EAST': pooled['EAST'], 'WEST': pooled['WEST']}, 'orientations': orient,
           'stage_totals': stage_tot, 'rejection_totals': rejection_tot,
           'ratio_to_phase4_fixture_density': (k * 100 / area) / PHASE4_FIXTURE_DENSITY if area else None,
           'ratio_to_phase31_mansion_density': (k * 100 / area) / PHASE31_MANSION_DENSITY if area else None,
           'vanilla_structure_overlaps': overlaps, 'rows': rows}
    out_json.write_text(json.dumps(agg, indent=1) + '\n')
    md = ['| seed | potential | full desert | terrain | entrance | fluid-safe | starts | completed | /100 km² | E/W | nearest | verification |',
          '|---|---|---|---|---|---|---|---|---|---|---|---|']
    for r in rows:
        md.append(f"| {r['seed']} | {r['potential_anchors']} | {r['full_desert_qualified']} | {r['terrain_qualified']} | {r['entrance_qualified']} | "
                  f"{r['fluid_safe_eligible_predicted']} | {r['actual_valid_starts']} | {r['completed']} | {r['density_per_100km2']:.3f} | "
                  f"{r['routes']['EAST']}/{r['routes']['WEST']} | {'' if r['nearest_start_blocks'] is None else round(r['nearest_start_blocks'])} | {r['verification']} |")
    md.append('')
    md.append(f"Pooled: {k} starts ({pooled['completed']} completed and verified) over {area:.1f} km² = "
              f"{agg['pooled_density_per_100km2']:.3f} /100 km² (95 % Garwood CI {agg['pooled_density_95ci'][0]:.3f}–{agg['pooled_density_95ci'][1]:.3f}); "
              f"seeds without a start: {agg['seeds_without_start']}/{len(rows)}; EAST/WEST {pooled['EAST']}/{pooled['WEST']}; orientations {orient}; "
              f"ratio to Phase 4 fixture 0.254: {agg['ratio_to_phase4_fixture_density']:.2f}x; ratio to Phase 3.1 Mansion 0.715: {agg['ratio_to_phase31_mansion_density']:.2f}x.")
    md.append(f"Stage totals: {stage_tot}; rejections: {rejection_tot}; vanilla structure overlaps: {len(overlaps)}.")
    out_md.write_text('\n'.join(md) + '\n')
    print('\n'.join(md))


if __name__ == '__main__':
    main()
