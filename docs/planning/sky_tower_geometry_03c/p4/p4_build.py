#!/usr/bin/env python3
"""GEOMETRY-03C-P4: extract the representative section of P3 candidate K2-land3 and write an experimental NBT.
Section = every member of lap index 1 (F2 floor y21 -> F3 floor y33, C1): its treads (from tread_tops), rails that
guard those treads, its pillars (P2-*) and arches (from P2-*), its two balconies (B2-*), the portal floor slabs at
F2/F3 and the B25-T3 wall/tower body (02B baseline) in the same y range. Nothing is redesigned: block positions are
copied from the P3 export; only materials are chosen (experimental palette) and cut faces are open.
Output: section_blocks.csv (P3 format), k2_lap2.nbt (structure template, origin = min corner), manifest.json.
Requires nbtlib (repo voxel.py writer is reused read-only). NOT linked to worldgen."""
import csv, json, math, sys
from pathlib import Path
HERE = Path(__file__).resolve().parent
CAND = sys.argv[1] if len(sys.argv) > 1 else 'K2_8balc_A5relaxed_slab_land3'
TAG = sys.argv[2] if len(sys.argv) > 2 else 'k2_lap2'
SRC = HERE.parent / 'p3' / 'data' / 'candidates' / CAND
BASE = HERE.parents[1] / 'sky_tower_geometry_02/geometry02b/baseline.json'
sys.path.insert(0, str(HERE.parents[2].parent / 'scripts/structures/sky_islands'))
from voxel import Vox  # noqa: E402  (read-only reuse of the repo NBT writer)
FLOORS = (10, 21, 33, 47, 64)
LAP = int(sys.argv[3]) if len(sys.argv) > 3 else 1
Y0, Y1 = FLOORS[LAP] - 2, FLOORS[LAP + 1] + 9
MAT = {'stairs': 'stone_bricks', 'pillar': 'polished_andesite', 'rail': 'stone_bricks', 'arcade_arch': 'chiseled_stone_bricks',
       'balcony_floor': 'smooth_stone', 'balcony_rail': 'stone_bricks', 'balcony_support': 'andesite', 'tower': 'cracked_stone_bricks', 'portal': 'stone_bricks'}
SLAB = {'stairs': 'stone_brick_slab', 'balcony_floor': 'smooth_stone_slab'}


def main():
    rows = list(csv.DictReader((SRC / 'blocks_C1.csv').open()))
    obj = json.loads((SRC / 'objects_C1.json').read_text())
    tops = {tuple(map(int, k.split(','))): v for k, v in json.loads((SRC / 'tread_tops_C1.json').read_text()).items()}
    tread = {(x, math.floor(T) if T % 1 else int(T) - 1, z) for (L, x, z), T in tops.items() if L == LAP}
    lapT = {(x, z): T for (L, x, z), T in tops.items() if L == LAP}
    pil = {tuple(c) for p in obj['pillars'] if p['lap'] == LAP for c in p['cells']}
    ids = {p['id'] for p in obj['pillars'] if p['lap'] == LAP}
    arch = {tuple(c) for a in obj['arches'] if a['from'] in ids for c in a['cells']}
    bal = [b for b in obj['balconies'] if b['lap'] == LAP]
    keep = {}
    for r in rows:
        p = (int(r['x']), int(r['y']), int(r['z'])); g = r['group']
        take = (g == 'stairs' and (p in tread or (11 <= p[0] <= 15 and p[1] in (FLOORS[LAP], FLOORS[LAP + 1]))) or (g == 'stairs' and p[1] in (FLOORS[LAP], FLOORS[LAP + 1]) and 16 <= p[0] <= 19 and -1 <= p[2] <= 2)
                or (g == 'pillar' and p in pil) or (g == 'arcade_arch' and p in arch)
                or (g.startswith('balcony') and any(min(abs(p[0] - c[0]), 9) <= 2 and abs(p[2] - c[1]) <= 2 and abs(p[1] - b['top']) <= 3 for b in bal for c in b['clear_columns']))
                or (g == 'rail' and any((p[0] + dx, p[2] + dz) in lapT and math.floor(lapT[p[0] + dx, p[2] + dz]) in (p[1], p[1] - 1) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))))
        if take and Y0 <= p[1] <= Y1 and p not in keep: keep[p] = (g, r['shape'], r['facing'])
    base = json.loads(BASE.read_text()); wall = {tuple(c) for c in base['columns']['wall']}; inner = {tuple(c) for c in base['columns']['inner']}
    for layer in base['models']['C1']['layers']:
        y = layer['y']
        if not Y0 <= y <= Y1: continue
        for x, z in (wall if layer['zone'] == 'WALL' else wall | inner):
            if layer['zone'] == 'WALL' and 12 <= x <= 15 and -1 <= z <= 2 and any(fy + 1 <= y < fy + 8 for fy in (FLOORS[LAP], FLOORS[LAP + 1])): continue  # portal void (as P2/P3)
            keep.setdefault((x, y, z), ('tower', 'full', ''))
    xs, ys, zs = zip(*keep); ox, oy, oz = min(xs), min(ys), min(zs)
    v = Vox()
    for (x, y, z), (g, s, f) in sorted(keep.items()):
        X, Y, Z = x - ox, y - oy, z - oz
        if s == 'stair': v.stairs(X, Y, Z, 'stone_brick_stairs', f)
        elif s == 'slab': v.slab(X, Y, Z, SLAB.get(g, 'stone_brick_slab'), 'bottom')
        else: v.put(X, Y, Z, MAT[g])
    (HERE / 'data').mkdir(exist_ok=True)
    (HERE / 'data' / f'{TAG}.nbt').write_bytes(v.to_nbt())
    with (HERE / 'data' / ('section_blocks.csv' if TAG == 'k2_lap2' else f'section_blocks_{TAG}.csv')).open('w', newline='') as f:
        w = csv.writer(f, lineterminator='\n'); w.writerow(['group', 'x', 'y', 'z', 'shape', 'facing', 'block_state'])
        for (x, y, z), (g, s, fc) in sorted(keep.items()):
            w.writerow([g, x, y, z, s, fc, f"minecraft:{SLAB.get(g, 'stone_brick_slab')}[type=bottom,waterlogged=false]" if s == 'slab' else (f'minecraft:stone_brick_stairs[facing={fc},half=bottom,shape=straight,waterlogged=false]' if s == 'stair' else f'minecraft:{MAT[g]}')])
    man = {'source': str(SRC.relative_to(HERE.parents[2].parent)), 'lap_index': LAP, 'y_window': [Y0, Y1], 'origin_offset_C1': [ox, oy, oz],
           'size': list(v.extent()), 'blocks': len(keep), 'groups': {g: sum(1 for k in keep.values() if k[0] == g) for g in sorted({k[0] for k in keep.values()})},
           'pillars': sorted(ids), 'balconies': [b['id'] for b in bal], 'materials_experimental': MAT,
           'note': 'positions copied from P3 export; cut faces (lap 0 below / lap 2 above) are open; materials are not design decisions'}
    (HERE / 'data' / ('manifest.json' if TAG == 'k2_lap2' else f'manifest_{TAG}.json')).write_text(json.dumps(man, indent=1) + '\n'); print(man['size'], man['blocks'], man['groups'])


if __name__ == '__main__':
    main()
