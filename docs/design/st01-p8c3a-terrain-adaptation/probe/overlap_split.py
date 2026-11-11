"""Overlap split by policy. Usage: overlap_split.py <coverage-dir>"""
import json, glob, sys, os
from collections import Counter, defaultdict
rows = []
for f in sorted(glob.glob(os.path.join(sys.argv[1], 'cov-*.json'))):
    d = json.load(open(f))
    for c in d['candidates']:
        if 'overlap' not in c: continue
        pd = c['policies']['D_16_10_8']['median']['verdict'] == 'ELIGIBLE'
        pb = c['policies']['B_40_24_32']['median']['verdict'] == 'ELIGIBLE'
        ov = [o for o in c['overlap'] if not o['structure'].startswith('moreminecrafts:st01_pyramid') and o['pieces_touching'] > 0]
        rows.append((d['seed'], c['chunk'], c['diff'], pd, pb, ov))
def summary(sel, label):
    n = len(sel); anyo = [r for r in sel if r[5]]
    exc = [r for r in anyo if any(o['piece_box_cells_in_excavation'] > 0 for o in r[5])]
    edge = [r for r in anyo if any(o['pieces_crossing_footprint_edge'] > 0 for o in r[5])]
    by = defaultdict(lambda: [0, 0, 0, 0])
    for r in anyo:
        for o in r[5]:
            k = o['structure'].split('@')[0]; b = by[k]
            b[0] += 1; b[1] += o['piece_box_cells_inside_st01_volume']; b[2] += o['piece_box_cells_in_excavation']; b[3] += o['pieces_crossing_footprint_edge'] > 0
    print(f"### {label}: {n} candidates; {len(anyo)} with ≥1 vanilla piece box intersecting the ST-01 volume (±1 ring); "
          f"{len(exc)} with piece-box cells inside the excavation; {len(edge)} with pieces crossing the footprint edge\n")
    print("| structure | starts | piece-box cells in ST-01 volume (sum) | piece-box cells in excavation (sum) | starts with pieces crossing the edge |\n|---|---|---|---|---|")
    for k, b in sorted(by.items(), key=lambda kv: -kv[1][0]): print(f"| {k} | {b[0]} | {b[1]} | {b[2]} | {b[3]} |")
    print()
summary([r for r in rows if r[3]], 'Policy D eligible (median plane) — current production exposure')
summary([r for r in rows if r[4] and not r[3]], 'Policy B only (eligible under B median, rejected under D)')
summary([r for r in rows if r[4]], 'All Policy B eligible (median plane)')
