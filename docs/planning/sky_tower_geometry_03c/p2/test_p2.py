#!/usr/bin/env python3
"""P2 regression/consistency tests (stdlib; the generator-convention test needs ortools and is skipped without it).
usage: python3 -I test_p2.py   (run p2_search.py first only if data/ is missing)"""
import csv, importlib.util, json, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
D = HERE / 'data'
def load(path, name):
    s = importlib.util.spec_from_file_location(name, path); m = importlib.util.module_from_spec(s); s.loader.exec_module(m); return m
ver = load(HERE / 'p2_verify.py', 'p2v'); p1 = load(HERE.parent / 'audit.py', 'p1'); b3 = load(HERE / 'p2_build3d.py', 'p2b')
def study(n): return json.loads((D / f'study_{n}.json').read_text())
fails = []
def ok(cond, msg):
    print(('PASS ' if cond else 'FAIL ') + msg)
    if not cond: fails.append(msg)

# 1. coordinate conventions: P1 audit == P2 verifier == P2 builder
r = range(-34, 35)
ok(all(p1.center_inside(x, z, d) == ver.inside_center(x, z, d) == ver.inside_center_float(x, z, d) for x in r for z in r for d in (31, 45, 51)), 'P1 centre rule == verifier int/float rule (D31/45/51)')
ok(all(p1.corner_inside(x, z, d) == ver.inside_whole(x, z, d) for x in r for z in r for d in (45, 51)), 'P1 whole-block rule == verifier whole-block rule')
ok(all(ver.pad(p) == b3.pad(p) and ver.square3(p) == b3.sq3(p) for p in [(0, 0), (-5, 7), (17, 0)]), 'verifier pad/3x3 == builder pad/3x3 (offsets -1..2 / -1..1)')
ok(b3.disc(45) == {(x, z) for x in range(-32, 33) for z in range(-32, 33) if ver.inside_center(x, z, 45)}, 'builder D45 disc == verifier D45')
try:
    gen = load(HERE / 'p2_search.py', 'p2s')
    ok(gen.D45 == b3.disc(45, 32) and gen.D31 == b3.disc(31, 32) and gen.D51 == b3.disc(51, 32), 'generator discs == builder discs')
    ok(all(gen.pad(p) == ver.pad(p) and gen.sq3(p) == ver.square3(p) for p in [(0, 0), (-5, 7), (17, 0)]), 'generator pad/3x3 == verifier')
except ImportError:
    print('SKIP generator-convention test (ortools not installed)')
conv, _ = ver.convention_check()
ok(conv['integer_vs_float_disc_agree'] and conv['D31_equals_02B_inner_plus_wall'], 'D31 == frozen 02B inner+wall')

# 2. reproduce the P1 259 outside-D45 columns on the frozen 03B C1 export, with the P2 verifier's rule
src = HERE.parents[1] / 'sky_tower_geometry_03b/data/best/blocks_C1.csv'
normal = {(int(x['x']), int(x['z'])) for x in csv.DictReader(src.open()) if not x['group'].startswith('balcony_')}
ok(len([c for c in normal if not ver.inside_center(c[0], c[1], 45)]) == 259, 'P1 finding reproduced by P2 rule: 259 columns outside D45 in frozen 03B best')

# 3. solver-proved study outcomes recorded in data/
exp = {'A0_loop_only': 'OPTIMAL', 'B1_max_pillars_pads': 12, 'B2_max_pillars_pads_no_ray': 12, 'B3_max_pillars_pads_no_ray_no_adj': 12,
       'B5_max_pillars_lean_walk': 12, 'B6_max_pillars_lean_no_ray_no_adj': 12, 'S8_sectors8_pads': 'OPTIMAL', 'S9_sectors9_pads': 'INFEASIBLE',
       'S17_sectors17_pads': 'INFEASIBLE', 'C0_max_rise_stations_no_balconies': 21, 'C1_loop_8_balconies_flat_landings': 'INFEASIBLE',
       'C2_max_rise_stations_with_8_balconies_flat': 6, 'C3_max_balconies_with_17_rise_stations': 4, 'D1_RELAXED_contact1_loop_8_balconies_flat': 'INFEASIBLE',
       'E1_NOBINS_max_balconies_with_17_rise_contact7': 4, 'E2_NOBINS_max_balconies_with_17_rise_contact1': 4, 'F1_4_balconies_max_pillars_17_rise': 8}
for n, e in exp.items():
    s = study(n)
    if isinstance(e, int): ok(s['status'] == 'OPTIMAL' and round(s['objective']) == e and round(s['bound']) == e, f'{n}: proved optimum {e}')
    else: ok(s['status'] == e, f'{n}: {e}')

# 4. candidates: independent plan verifier + frozen-module 3D audit
for n in ('BEST_max_pillars_sectors8', 'F1_4_balconies_max_pillars_17_rise'):
    v = ver.check(study(n))
    print(n, 'plan-verify FAIL:', v['summary']['FAIL'], 'UNVERIFIED:', v['summary']['UNVERIFIED'])
    a = json.loads((D / 'candidates' / f'audit3d_{n}.json').read_text())
    for c in ('C1', 'C2'):
        ok(all(s['status'] == 'PASS' for s in a[c]['stages']) and a[c]['boundary']['outside_D45_center'] == 0 and a[c]['boundary']['height_within_0_79'], f'{n} {c}: 3D stages PASS, D45 centre exceedance 0, height in 0..79')
v = ver.check(study('BEST_max_pillars_sectors8')); ok(v['summary']['FAIL'] == [], 'BEST plan-level numeric checks: no FAIL')
v = ver.check(study('F1_4_balconies_max_pillars_17_rise')); ok(v['summary']['FAIL'] == ['balcony_count_8'], 'BAL4: only the 8-balcony count fails (4 built); the 4 built are 7x3/D51/contact/no-overlap clean')
# 5. the schedule itself refuses the 8-balcony plan (cannot place 17 one-block rises)
try:
    b3.build(study('C2_max_rise_stations_with_8_balconies_flat')); ok(False, '8-balcony flat plan must not build')
except ValueError as e:
    ok('insufficient' in str(e), '8 flat balconies: schedule has <17 rise stations (' + str(e) + ')')
sys.exit(1 if fails else 0)
