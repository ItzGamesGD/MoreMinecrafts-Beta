#!/usr/bin/env python3
"""GEOMETRY-03C-P2 independent plan-level checker (stdlib only). Does NOT import p2_search.py.

Re-derives every set from the exported loop/pillar/balcony coordinates. Conventions are re-implemented here
and cross-checked against the frozen 02B baseline and the P1 audit rule. PASS/FAIL below is a numeric
geometry statement for ONE candidate; it is not design approval.
usage: python3 -I p2_verify.py data/study_X.json [out.json]
"""
import json, math, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
BASE = HERE.parents[1] / 'sky_tower_geometry_02/geometry02b/baseline.json'
FLOORS = (10, 21, 33, 47, 64)
RISES = (11, 12, 14, 17)


def inside_center(x, z, D):          # P1 rule, integer form
    return 4 * (x * x + z * z) < D * D


def inside_center_float(x, z, D):    # independent float form
    return math.hypot(x, z) < D / 2


def inside_whole(x, z, D):           # alternative whole-block comparison (kept separate, never the default)
    return (2 * abs(x) + 1) ** 2 + (2 * abs(z) + 1) ** 2 <= D * D


def pad(p):
    return {(p[0] + i, p[1] + j) for i in (-1, 0, 1, 2) for j in (-1, 0, 1, 2)}


def square3(q):
    return {(q[0] + i, q[1] + j) for i in (-1, 0, 1) for j in (-1, 0, 1)}


def convention_check():
    r = range(-34, 35)
    base = json.loads(BASE.read_text())
    wall = {tuple(c) for c in base['columns']['wall']}; inner = {tuple(c) for c in base['columns']['inner']}
    ok_ints = all(inside_center(x, z, D) == inside_center_float(x, z, D) for x in r for z in r for D in (31, 45, 51))
    d31 = {(x, z) for x in r for z in r if inside_center(x, z, 31)}
    return {'integer_vs_float_disc_agree': ok_ints, 'D31_equals_02B_inner_plus_wall': d31 == (wall | inner),
            'wall_columns': len(wall), 'inner_columns': len(inner),
            'axis': '(0.5,0.5); column (x,z) centre distance = hypot(x,z)'}, d31


def winding(loop):
    """Net turns around the axis, computed from consecutive pad-centre angles (independent of the solver)."""
    tot = 0.0
    for a, b in zip(loop, loop[1:] + loop[:1]):
        t1 = math.atan2(a[1] + .5, a[0] + .5); t2 = math.atan2(b[1] + .5, b[0] + .5)
        d = t2 - t1
        while d > math.pi: d -= 2 * math.pi
        while d < -math.pi: d += 2 * math.pi
        tot += d
    return round(tot / (2 * math.pi), 6)


def schedule_check(loop):
    N = len(loop)
    ds = [(loop[(i + 1) % N][0] - p[0], loop[(i + 1) % N][1] - p[1]) for i, p in enumerate(loop)]
    protected = {j % N for j in range(-4, 5)}
    eligible = [i for i in range(N) if i not in protected and len({ds[(i + k) % N] for k in range(-5, 6)}) == 1]
    turns = sum(ds[i] != ds[i - 1] for i in range(N))
    return {'stations': N, 'direction_changes': turns, 'eligible_stations': len(eligible),
            'rise_needed_max_lap': max(RISES), 'each_lap_rise_fits_one_block_per_station': len(eligible) >= max(RISES),
            'eligible': eligible, 'directions': ds}


def pillar_stats(loop, pillars, side_sets):
    N = len(loop)
    walk = side_sets['W']
    rows = []
    for q in pillars:
        s = min(range(N), key=lambda i: ((loop[i][0] + .5 - q[0]) ** 2 + (loop[i][1] + .5 - q[1]) ** 2, i))
        # outer side: sample the segment axis -> pillar centre; crossing a walk column before the pillar => walk lies between
        r = math.hypot(q[0], q[1]); crossed = False
        for t in range(int(15.5 * 4), int(max(r - 1.5, 15.5) * 4) + 1):
            f = t / 4 / max(r, 1e-9)
            if (math.floor(q[0] * f + .5), math.floor(q[1] * f + .5)) in walk: crossed = True; break
        sc = square3(q)
        nb = {(x + dx, z + dz) for x, z in sc for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))} - sc
        rows.append({'xz': list(q), 'station': s, 'angle_deg': round(math.degrees(math.atan2(q[1] + .5, q[0] + .5)) % 360, 2),
                     'outer_of_walk': crossed, 'touches_walk': bool(nb & walk),
                     'inside_D45_center': all(inside_center(x, z, 45) for x, z in sc),
                     'inside_D45_whole_block': all(inside_whole(x, z, 45) for x, z in sc)})
    rows.sort(key=lambda r: r['station'])
    gaps = [(rows[(i + 1) % len(rows)]['station'] - rows[i]['station']) % N or N for i in range(len(rows))] if rows else []
    eu = [math.hypot(rows[(i + 1) % len(rows)]['xz'][0] - rows[i]['xz'][0], rows[(i + 1) % len(rows)]['xz'][1] - rows[i]['xz'][1])
          for i in range(len(rows))] if rows else []
    ang = sorted(r['angle_deg'] for r in rows)
    ag = [(ang[(i + 1) % len(ang)] - ang[i]) % 360 or 360 for i in range(len(ang))] if ang else []
    return rows, {'count': len(rows), 'path_gap_stations': gaps, 'euclid_gap': [round(g, 2) for g in eu],
                  'path_gap_min': min(gaps) if gaps else None, 'path_gap_max': max(gaps) if gaps else None,
                  'path_gap_mean': round(sum(gaps) / len(gaps), 2) if gaps else None,
                  'max_angular_gap_deg': max(ag) if ag else None, 'target_about': 7,
                  'pillars_if_every_7_stations': round(N / 7, 1),
                  'tolerance_defined': False, 'verdict': 'UNVERIFIED (Q85 tolerance not defined); deviation from 7 reported only'}


def check(plan):
    conv, d31 = convention_check()
    loop = [tuple(p) for p in plan['loop']]
    R = {'convention': conv, 'candidate': plan['name'], 'checks': []}

    def add(id_, ok, ev, status=None):
        R['checks'].append({'id': id_, 'status': status or ('PASS' if ok else 'FAIL'), 'evidence': ev})

    N = len(loop)
    steps_ok = all(abs(a[0] - b[0]) + abs(a[1] - b[1]) == 1 for a, b in zip(loop, loop[1:] + loop[:1]))
    add('loop_closed_simple_4neighbour', steps_ok and len(set(loop)) == N, {'stations': N, 'distinct': len(set(loop))})
    add('loop_contains_portal_anchor_17_0', (17, 0) in loop, None)
    w = winding(loop); add('loop_winds_once_around_wall', abs(w) == 1.0, {'winding': w})
    W = set().union(*(pad(p) for p in loop)) if loop else set()
    mode = plan['options']['walk']
    d45 = {(x, z) for x in range(-34, 35) for z in range(-34, 35) if inside_center(x, z, 45)}
    add('walk_D45_center_exceedance_0', not (W - d45), {'outside_columns': sorted(W - d45)[:10], 'walk_columns_pad_union': len(W)})
    add('walk_whole_block_D45 (alternative rule, informational)', True,
        {'outside_columns': len({c for c in W if not inside_whole(c[0], c[1], 45)})}, 'INFO')
    add('walk_clear_of_wall_disc_D31', not (W & d31), {'overlap': len(W & d31)})
    add('stair_frame_width_4_by_construction', True, 'each station: 4 contiguous columns; 4x4 pad at turns (03B certificate geometry)', 'INFO')
    sc = schedule_check(loop)
    ev = {k: v for k, v in sc.items() if k not in ('eligible', 'directions')}
    add('rise_stations_sufficient (>=17 one-block rises/lap, 03B eligibility rule, entrance +-4 protected)',
        sc['eligible_stations'] >= max(RISES), ev)
    P = [tuple(q) for q in plan.get('pillars', [])]
    allp = [c for q in P for c in square3(q)]
    add('pillars_pairwise_disjoint', len(allp) == len(set(allp)), {'columns': len(allp)})
    add('pillars_disjoint_from_walk_and_wall', not (set(allp) & (W | d31)), None)
    d45all = all(inside_center(x, z, 45) for x, z in allp)
    add('pillars_D45_center_exceedance_0', d45all, None)
    rows, stats = pillar_stats(loop, P, {'W': W})
    add('pillars_touch_walk_and_lie_outside_it', all(r['touches_walk'] and r['outer_of_walk'] for r in rows) if rows else True,
        {'inner_side_or_detached': [r['xz'] for r in rows if not (r['touches_walk'] and r['outer_of_walk'])]})
    add('pillar_spacing_about_7 (Q85)', False, stats, 'UNVERIFIED')
    R['pillars'] = rows; R['pillar_stats'] = stats
    bal = plan.get('balconies', [])
    brs = []
    for b in bal:
        w_, h_ = (7, 3) if b['horiz'] else (3, 7)
        x0, z0 = b['origin']
        Rr = {(x0 + i, z0 + j) for i in range(w_) for j in range(h_)}
        ring = {(x0 + i, z0 + j) for i in range(-1, w_ + 1) for j in range(-1, h_ + 1)} - Rr
        contact = {(c[0] + b['side'][0], c[1] + b['side'][1]) for c in Rr} - Rr
        ex = max(math.hypot(x, z) for x, z in Rr | ring) - 22.5
        brs.append({'origin': [x0, z0], 'horiz': b['horiz'], 'clear_cells': len(Rr),
                    'inside_D51_center': all(inside_center(x, z, 51) for x, z in Rr | ring),
                    'inside_D51_whole_block': all(inside_whole(x, z, 51) for x, z in Rr | ring),
                    'contact_in_walk': contact <= W, 'overlaps_walk_or_wall': bool(Rr & (W | d31)) or bool(ring & d31),  # ring cells on the contact row are walk columns by design
                    'overlaps_pillar': bool((Rr | ring) & set(allp)), 'max_center_excess_over_D45': round(ex, 3),
                    'locality_status': 'UNVERIFIED (airborne exception and support not established by bounds)'})
    if bal or plan['options']['balconies']:
        add('balcony_count_8', len(bal) == 8, {'count': len(bal)})
        add('balconies_clear_7x3_D51_contact_no_overlap', all(b['clear_cells'] == 21 and b['inside_D51_center'] and b['contact_in_walk']
            and not b['overlaps_walk_or_wall'] and not b['overlaps_pillar'] for b in brs), None)
        R['balconies'] = brs
    R['summary'] = {'FAIL': [c['id'] for c in R['checks'] if c['status'] == 'FAIL'],
                    'UNVERIFIED': [c['id'] for c in R['checks'] if c['status'] == 'UNVERIFIED']}
    R['plan_level_only'] = True
    R['not_run'] = ['3D headroom', 'arch/canopy/gateway collision', 'in-game']
    return R


if __name__ == '__main__':
    plan = json.loads(Path(sys.argv[1]).read_text())
    res = check(plan)
    if len(sys.argv) > 2: Path(sys.argv[2]).write_text(json.dumps(res, ensure_ascii=False, separators=(',', ':')) + '\n')
    print(plan['name'], 'FAIL:', res['summary']['FAIL'], 'UNVERIFIED:', res['summary']['UNVERIFIED'])
