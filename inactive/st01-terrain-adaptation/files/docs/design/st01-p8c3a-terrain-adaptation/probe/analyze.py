#!/usr/bin/env python3
"""P8-C3-A coverage/search analyser. Usage: analyze.py <coverage-dir> [search-dir]. Prints markdown tables."""
import json, glob, sys, os, statistics as st
from collections import Counter, defaultdict

out = sys.argv[1]
search_dir = sys.argv[2] if len(sys.argv) > 2 else out
AREA_KM2 = (20480 / 1000.0) ** 2  # anchors span +-10,240 blocks per axis
PLANES = ['median', 'mean', 'trimmed10', 'p25', 'p40', 'p60', 'p75', 'midrange', 'window_clamped_median']
POL = ['D_16_10_8', 'B_40_24_32']

cov = sorted(glob.glob(os.path.join(out, 'cov-*.json')))
seeds = []
agg = {'potential': 0, 'full_desert': 0, 'ms': 0.0, 'hc': 0}
elig = defaultdict(int)          # (policy, plane) -> eligible count
verdicts = defaultdict(Counter)  # (policy, plane) -> verdict counter
metrics = defaultdict(lambda: defaultdict(list))  # (policy, plane) -> metric -> values (eligible only)
per_seed = []
overlap_rows = []
diff_hist = Counter()
for f in cov:
    d = json.load(open(f))
    seeds.append(d['seed'])
    agg['potential'] += d['potential']; agg['full_desert'] += d['full_desert']
    agg['ms'] += d['total_ms']; agg['hc'] += d['height_computes']
    row = {'seed': d['seed'], 'full': d['full_desert']}
    for c in d['candidates']:
        diff_hist[min(c['diff'] // 8 * 8, 80)] += 1
        for p in POL:
            for pl in PLANES:
                v = c['policies'][p][pl]
                verdicts[(p, pl)][v['verdict']] += 1
                if v['verdict'] == 'ELIGIBLE':
                    elig[(p, pl)] += 1
                    for k in ('cut', 'skirt', 'excavation_blocks', 'excavation_columns',
                              'exposed_body_face_sum', 'exposed_body_face_max',
                              'boundary_cliff_face_sum', 'boundary_cliff_face_max'):
                        metrics[(p, pl)][k].append(v[k])
        row['D'] = row.get('D', 0) + (c['policies']['D_16_10_8']['median']['verdict'] == 'ELIGIBLE')
        row['B'] = row.get('B', 0) + (c['policies']['B_40_24_32']['median']['verdict'] == 'ELIGIBLE')
        row['Bw'] = row.get('Bw', 0) + (c['policies']['B_40_24_32']['window_clamped_median']['verdict'] == 'ELIGIBLE')
        if 'overlap' in c:
            overlap_rows.append((d['seed'], c['chunk'], c['diff'], c['overlap']))
    per_seed.append(row)

n = len(cov)
area = n * AREA_KM2
print(f"# Coverage ({n} seeds: {seeds})\n")
print(f"potential={agg['potential']} full_desert={agg['full_desert']} area_km2={area:.1f} "
      f"total_probe_s={agg['ms']/1000:.1f} height_computes={agg['hc']}\n")
print("| seed | full Desert | D median eligible | B median eligible | B window-clamped eligible |\n|---|---|---|---|---|")
for r in per_seed:
    print(f"| {r['seed']} | {r['full']} | {r.get('D',0)} | {r.get('B',0)} | {r.get('Bw',0)} |")
print()
print("## Diff histogram (full-Desert candidates, bucket of 8)\n\n| diff bucket | count |\n|---|---|")
for k in sorted(diff_hist): print(f"| {k}-{k+7 if k<80 else '+'} | {diff_hist[k]} |")
print()
print("## Eligible counts and density per plane\n\n| policy | plane | eligible | per 100 km² | verdicts |\n|---|---|---|---|---|")
for p in POL:
    for pl in PLANES:
        e = elig[(p, pl)]
        vs = ', '.join(f"{k}={v}" for k, v in sorted(verdicts[(p, pl)].items()))
        print(f"| {p} | {pl} | {e} | {100*e/area:.3f} | {vs} |")
print()
def s(vals):
    if not vals: return '-'
    vals = sorted(vals)
    return f"{st.median(vals):.0f} / p90 {vals[int(0.9*(len(vals)-1))]} / max {vals[-1]}"
print("## Correction metrics on eligible candidates (median / p90 / max)\n")
print("| policy | plane | n | cut | skirt | excavation blocks | excavation cols | exposed body face sum | exposed body face max | boundary cliff sum | boundary cliff max |\n|---|---|---|---|---|---|---|---|---|---|---|")
for p in POL:
    for pl in PLANES:
        m = metrics[(p, pl)]
        if not m: continue
        print(f"| {p} | {pl} | {len(m['cut'])} | " + ' | '.join(s(m[k]) for k in (
            'cut', 'skirt', 'excavation_blocks', 'excavation_columns', 'exposed_body_face_sum',
            'exposed_body_face_max', 'boundary_cliff_face_sum', 'boundary_cliff_face_max')) + ' |')
print()
print(f"## Overlap matrix (B-eligible-plane candidates: {len(overlap_rows)})\n")
kinds = Counter(); any_ov = 0
# Exclude the candidate's own production ST-01 start (present when Policy D already placed it) and
# starts whose structure box touches the volume but no piece box does.
overlap_rows = [(a, b, c, [o for o in ov if not o['structure'].startswith('moreminecrafts:st01_pyramid')
                           and o['pieces_touching'] > 0]) for a, b, c, ov in overlap_rows]
for seed, ch, diff, ov in overlap_rows:
    if ov: any_ov += 1
    for o in ov:
        kinds[o['structure'].split('@')[0] + ' [' + o['step'] + ']'] += 1
print(f"candidates with any overlap: {any_ov}\n")
print("| structure [step] | occurrences |\n|---|---|")
for k, v in kinds.most_common(): print(f"| {k} | {v} |")
for seed, ch, diff, ov in overlap_rows:
    for o in ov:
        print(f"- seed {seed} chunk {ch} diff {diff}: {o['structure']} step={o['step']} pieces={o['pieces_touching']} "
              f"vol_cells={o['piece_box_cells_inside_st01_volume']} exc_cells={o['piece_box_cells_in_excavation']} "
              f"edge_crossing={o['pieces_crossing_footprint_edge']}")
print()

srch = sorted(glob.glob(os.path.join(search_dir, 'search-*.json')))
if srch:
    print("## Search (first eligible anchor in ring order)\n")
    print("| seed | variant | order | ring | first eligible | candidates | height computes | ms |\n|---|---|---|---|---|---|---|---|")
    for f in srch:
        d = json.load(open(f))
        for i, r in enumerate(d.get('runs', [])):
            print(f"| {d['seed']} | {r.get('variant')} | {i} | ring {r.get('ring')} | {r.get('first_eligible')} | "
                  f"{r.get('candidates')} | {r.get('height_computes')} | {r.get('ms', 0):.0f} |")

# ---- excavation and foundation fill from archived grids (B median plane). h = first free Y (WORLD_SURFACE_WG), so the
# natural top solid block is h-1: excavated solid blocks above Y0 = max(0, h-1-yf); body cells placed above natural ground = max(0, yf+1-h).
import base64, struct
print("## Excavation, foundation fill and affected chunks (B-median-eligible grids)\n")
print("| seed | chunk | diff | yf | cut | skirt | excavated solid blocks above Y0 (Σ max(0,h−1−yf)) | body cells above natural ground (Σ max(0,yf+1−h)) | columns whose ground is below Y0 | affected chunks |\n|---|---|---|---|---|---|---|---|---|---|")
tot_rm = []; tot_fill = []; tot_ch = []
for f in cov:
    d = json.load(open(f))
    for c in d['candidates']:
        if 'grid_i16le_b64' not in c: continue
        b = c['policies']['B_40_24_32']['median']
        if b['verdict'] != 'ELIGIBLE': continue
        raw = base64.b64decode(c['grid_i16le_b64']); g = struct.unpack('<%dh' % (len(raw) // 2), raw)
        yf = b['yf']
        rm = sum(max(0, h - 1 - yf) for h in g); fill = sum(max(0, yf + 1 - h) for h in g); low = sum(1 for h in g if h <= yf)
        cx, cz = c['cx'], c['cz']
        ch = (((cx + 40) >> 4) - ((cx - 40) >> 4) + 1) * (((cz + 40) >> 4) - ((cz - 40) >> 4) + 1)
        tot_rm.append(rm); tot_fill.append(fill); tot_ch.append(ch)
        print(f"| {d['seed']} | {c['chunk']} | {c['diff']} | {yf} | {b['cut']} | {b['skirt']} | {rm} | {fill} | {low} | {ch} |")
if tot_rm:
    print(f"\nTotals over {len(tot_rm)} B-median-eligible candidates: excavated median {st.median(tot_rm):.0f} max {max(tot_rm)}; "
          f"fill median {st.median(tot_fill):.0f} max {max(tot_fill)}; affected chunks min {min(tot_ch)} max {max(tot_ch)}\n")
