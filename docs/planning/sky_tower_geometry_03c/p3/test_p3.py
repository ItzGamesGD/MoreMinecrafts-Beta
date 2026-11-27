#!/usr/bin/env python3
"""P3 regression + NEGATIVE-CONTROL tests (stdlib; uses the independent verifier only).
usage: python3 -I test_p3.py      (needs data/ produced by p3_search.py / p3_generate.py)"""
import csv, importlib.util, json, math, shutil, subprocess, sys, tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent; D = HERE / 'data'; CAND = D / 'candidates'
def load(p, n):
    s = importlib.util.spec_from_file_location(n, p); m = importlib.util.module_from_spec(s); s.loader.exec_module(m); return m
ver = load(HERE / 'p3_verify.py', 'p3v'); p1 = load(HERE.parent / 'audit.py', 'p1')
fails = []
def ok(c, msg):
    print(('PASS ' if c else 'FAIL ') + msg)
    if not c: fails.append(msg)

# 0. P2 results must still reproduce unchanged (P2 artefacts untouched)
r = subprocess.run([sys.executable, '-I', str(HERE.parent / 'p2' / 'test_p2.py')], capture_output=True, text=True)
ok(r.returncode == 0 and r.stdout.count('PASS') >= 30, f'P2 regression suite reproduces ({r.stdout.count("PASS")} PASS lines, exit {r.returncode})')
# 1. conventions: P3 verifier disc == P1 centre / whole-block rule
rg = range(-34, 35)
ok(all(ver.in_c(x, z, d) == p1.center_inside(x, z, d) for x in rg for z in rg for d in (31, 45, 51)), 'verifier centre rule == P1')
ok(all(ver.in_w(x, z, d) == p1.corner_inside(x, z, d) for x in rg for z in rg for d in (45, 51)), 'verifier whole-block rule == P1')
try:
    gen = load(HERE / 'p3_generate.py', 'p3g'); g = gen.Geometry(19.5)
    ok(g.D45 == {(x, z) for x in range(-32, 33) for z in range(-32, 33) if ver.in_c(x, z, 45)}, 'generator D45 == verifier D45')
    ok(g.band == {c for c in g.D45 - g.D31 if ver.WALK_IN <= math.hypot(*c) < ver.WALK_IN + ver.WALK_W}, 'generator walk band == verifier lane band')
except ImportError:
    print('SKIP generator convention (ortools missing)')
# 2. solver-proved selection outcomes (window = every angular window of that size holds a pillar centre)
def st(n): return json.loads((D / f'sel_{n}.json').read_text())['status']
for n, e in [('S1_ann_pillars_only_A5strict_gap27', 'OPTIMAL'), ('S1_ann_pillars_only_A5strict_gap26', 'INFEASIBLE'),
             ('S3_ann_pillars_8balc_contact3_A5relaxed_gap34', 'OPTIMAL'), ('S3_ann_pillars_8balc_contact3_A5relaxed_gap33', 'INFEASIBLE'),
             ('S2_ann_pillars_8balc_contact3_A5strict_gap53', 'OPTIMAL'), ('S2_ann_pillars_8balc_contact3_A5strict_gap52', 'INFEASIBLE'),
             ('Q1_sequential_S1pillars_then_8balc_contact3', 'INFEASIBLE'), ('Q3_balconies_only_contact7_any_pillars', 'INFEASIBLE'),
             ('Q4_balconies_only_contact3', 'OPTIMAL'), ('T1_whole_block_D45_pillars_only_gap80', 'INFEASIBLE')]:
    ok(st(n) == e, f'{n}: {e}')
# 3. candidate verdicts (recomputed now from exported voxels)
exp = {'K1_pillars_slab': {'V6_balconies': False}, 'K2_8balc_A5relaxed_slab_land9': {}, 'K2_8balc_A5relaxed_slab_land3': {},
       'K2_8balc_A5relaxed_slab_land0': {}, 'K3_pillars_stairblocks': {'V4_lane_width4': False, 'V6_balconies': False},
       'K4_8balc_A5strict_slab_land9': {'V2_overlaps': False}}
for n, e in exp.items():
    for conv in ('C1', 'C2'):
        s = ver.check(CAND / n, conv)['summary']
        want = {k: e.get(k, True) for k in s if k != 'V7_pillar_spacing'}
        ok(all(s[k] == v for k, v in want.items()), f'{n} {conv}: {"all numeric checks PASS" if not e else "expected failures " + str(sorted(e))}')
land0 = ver.check(CAND / 'K2_8balc_A5relaxed_slab_land0', 'C1')['checks']['V6_balconies']['items']
ok(not all(b['flat_landing'] for b in land0), 'B3 (no flat landing): at least one balcony landing is sloped -> violates Q73 "flat section"')
land3 = ver.check(CAND / 'K2_8balc_A5relaxed_slab_land3', 'C1')['checks']['V6_balconies']['items']
ok(all(b['flat_landing'] for b in land3), 'B2 (3-cell landing): all 8 landings flat')

# 4. NEGATIVE CONTROLS: inject a defect into a copy of K2-land9 and require the verifier to catch it
SRC = CAND / 'K2_8balc_A5relaxed_slab_land9'
obj = json.loads((SRC / 'objects_C1.json').read_text())
def mutate(fn):
    tmp = Path(tempfile.mkdtemp()); shutil.copytree(SRC, tmp / 'c')
    rows = list(csv.DictReader((tmp / 'c' / 'blocks_C1.csv').open()))
    rows = fn(rows)
    with (tmp / 'c' / 'blocks_C1.csv').open('w', newline='') as f:
        w = csv.DictWriter(f, fieldnames=['group', 'x', 'y', 'z', 'shape', 'facing'], lineterminator='\n'); w.writeheader(); w.writerows(rows)
    res = ver.check(tmp / 'c', 'C1'); shutil.rmtree(tmp); return res
tops = json.loads((SRC / 'tread_tops_C1.json').read_text())
def row(g, x, y, z, s='full'): return {'group': g, 'x': str(x), 'y': str(y), 'z': str(z), 'shape': s, 'facing': ''}
# N1: 1-block intrusion into the outer lane of lap 1 between 150 and 170 degrees
def n1(rows):
    for k, T in tops.items():
        L, x, z = map(int, k.split(','))
        a = math.degrees(math.atan2(z, x)) % 360
        if L == 1 and 150 <= a <= 170 and 18.5 <= math.hypot(x, z) < 19.5:
            rows += [row('rail', x, math.floor(T), z), row('rail', x, math.floor(T) + 1, z)]
    return rows
r = mutate(n1); lw = r['checks']['V4_lane_width4']['lanes']
ok(not r['summary']['V4_lane_width4'] and [(l['lap'], l['lane']) for l in lw if not l['pass']] == [(1, 3)], 'N1 width intrusion (1 block, lap 1 outer lane) -> lane test fails exactly lap1/lane3')
# N2: remove the floor under one balcony clear column
b0 = obj['balconies'][0]; cx, cz = b0['clear_columns'][10]
def n2(rows): return [r_ for r_ in rows if not (r_['group'] == 'balcony_floor' and int(r_['x']) == cx and int(r_['z']) == cz)]
r = mutate(n2); ok(not r['summary']['V6_balconies'], 'N2 one balcony floor block removed -> 7x3 check fails')
# N3: move one pillar 1 block outward (beyond D45)
p0 = obj['pillars'][0]; qx, qz = p0['xz']; dx, dz = (1, 0) if abs(qx) >= abs(qz) else (0, 1)
dx, dz = (dx * (1 if qx >= 0 else -1), dz * (1 if qz >= 0 else -1)) if dx else (0, 1 if qz >= 0 else -1)
cells = {tuple(c) for c in p0['cells']}
def n3(rows):
    out = [r_ for r_ in rows if not (r_['group'] == 'pillar' and (int(r_['x']), int(r_['y']), int(r_['z'])) in cells)]
    return out + [row('pillar', x + dx, y, z + dz) for x, y, z in cells]
r = mutate(n3); ok(not r['summary']['V1_boundary'] and r['checks']['V1_boundary']['normal_outside_D45_center'] > 0, 'N3 pillar shifted 1 block outward -> D45 boundary fails')
# N4: cut the stair (all treads of lap 2 between 100 and 112 degrees)
def n4(rows):
    cut = set()
    for k, T in tops.items():
        L, x, z = map(int, k.split(','))
        if L == 2 and 100 <= math.degrees(math.atan2(z, x)) % 360 <= 112: cut.add((x, math.floor(T) if T % 1 else int(T) - 1, z))
    return [r_ for r_ in rows if not (r_['group'] == 'stairs' and (int(r_['x']), int(r_['y']), int(r_['z'])) in cut)]
r = mutate(n4); ok(r['checks']['V3_reach']['reached'][:4] == [True, True, False, False], 'N4 stair cut in lap 2 -> F4 and sanctum unreachable')
# N5: delete one arch
a5 = {tuple(c) for c in obj['arches'][5]['cells']} - {tuple(c) for p in obj['pillars'] for c in p['cells']}
def n5(rows): return [r_ for r_ in rows if not (r_['group'] == 'arcade_arch' and (int(r_['x']), int(r_['y']), int(r_['z'])) in a5)]
r = mutate(n5); ok(not r['summary']['V8_arch_continuity'] and len(r['checks']['V8_arch_continuity']['missing']) == 1, 'N5 one arch removed -> continuity fails with exactly 1 missing pair')
# N6: wall off one balcony along its whole perimeter on the walk side
bset = {tuple(c) for c in b0['clear_columns']}
def n6(rows):
    top = b0['top']; y0 = math.floor(top) if top % 1 else int(top)
    add = []
    for x, z in bset:
        for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + ddx, z + ddz)
            if n not in bset and 15.5 <= math.hypot(*n) < 19.5: add += [row('balcony_rail', x, y0, z), row('balcony_rail', x, y0 + 1, z)]
    return rows + add
r = mutate(n6); reached = dict(zip(r['checks']['V3_reach']['targets'], r['checks']['V3_reach']['reached']))
ok(reached[b0['id']] is False and all(reached[k] for k in ('F2', 'F3', 'F4', 'SANCTUM')), 'N6 balcony opening blocked -> that balcony unreachable, floors still reachable')
print(f'{len(fails)} failures'); sys.exit(1 if fails else 0)
