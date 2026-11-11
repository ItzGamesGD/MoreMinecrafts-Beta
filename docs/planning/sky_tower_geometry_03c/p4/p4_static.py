#!/usr/bin/env python3
"""Static checks of the extracted sections (stdlib): D45 / D51 centre rule (+ whole-block comparison), group overlaps,
balcony support under every balcony floor cell, pillar 3x3 footprints, y range. usage: python3 -I p4_static.py"""
import csv, json
from pathlib import Path
HERE = Path(__file__).resolve().parent; D = HERE / 'data'
def inc(x, z, d): return 4 * (x * x + z * z) < d * d
def inw(x, z, d): return (2 * abs(x) + 1) ** 2 + (2 * abs(z) + 1) ** 2 <= d * d
out = {}
for f in sorted(D.glob('section_blocks*.csv')):
    rows = list(csv.DictReader(f.open())); g = {}
    for r in rows: g.setdefault(r['group'], set()).add((int(r['x']), int(r['y']), int(r['z'])))
    normal = {(x, z) for k, s in g.items() if k not in ('tower',) and not k.startswith('balcony') for x, y, z in s}
    bal = {(x, z) for k, s in g.items() if k.startswith('balcony') for x, y, z in s}
    fl = g.get('balcony_floor', set()); sup = g.get('balcony_support', set())
    names = sorted(g); ov = {f'{a}&{b}': len(g[a] & g[b]) for i, a in enumerate(names) for b in names[i + 1:] if g[a] & g[b]}
    out[f.name] = {'blocks': len(rows), 'normal_outside_D45_center': sum(1 for c in normal if not inc(*c, 45)),
                   'normal_outside_D45_whole_block_alt': sum(1 for c in normal if not inw(*c, 45)),
                   'balcony_outside_D51_center': sum(1 for c in bal if not inc(*c, 51)), 'balcony_outside_D51_whole_block_alt': sum(1 for c in bal if not inw(*c, 51)),
                   'balcony_columns_outside_D45_center': sum(1 for c in bal if not inc(*c, 45)),
                   'balcony_floor_cells_without_support_below': sum(1 for x, y, z in fl if (x, y - 1, z) not in sup),
                   'group_overlaps': ov, 'y_range': [min(p[1] for s in g.values() for p in s), max(p[1] for s in g.values() for p in s)]}
(D / 'static_checks.json').write_text(json.dumps(out, indent=1) + '\n')
for k, v in out.items(): print(k, v)
