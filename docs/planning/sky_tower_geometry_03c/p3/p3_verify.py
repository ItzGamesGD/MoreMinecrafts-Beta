#!/usr/bin/env python3
"""GEOMETRY-03C-P3 INDEPENDENT checker (stdlib + frozen 03 navigation module). Never imports p3_generate.py.

Reads exported blocks_{C1,C2}.csv (+ object positions only to know WHERE a balcony/pillar is declared; every
property is recomputed from voxels).  Collision model = frozen geometry03_navigation.Scene (0.6x1.8 AABB,
step-up <= 0.5, drop <= 1, no jump) extended with bottom-slab boxes.
usage: python3 -I p3_verify.py candidate_dir [out.json]
"""
import csv, importlib.util, json, math, statistics, sys
from collections import deque, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
_s = importlib.util.spec_from_file_location('nav03', HERE.parents[1] / 'sky_tower_geometry_03/geometry03_navigation.py')
nav = importlib.util.module_from_spec(_s); _s.loader.exec_module(nav)
BASE = HERE.parents[1] / 'sky_tower_geometry_02/geometry02b/baseline.json'
FLOORS_C1 = (10, 21, 33, 47, 64)
NB4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
WALK_IN, WALK_W = 15.5, 4                      # lanes are 4 disjoint 1-block radial intervals [15.5+j, 16.5+j)
JOINS = {'arcade_arch&pillar', 'pillar&rail'}  # same-voxel contacts treated as intentional joins (as P2), reported separately


def in_c(x, z, D): return 4 * (x * x + z * z) < D * D
def in_w(x, z, D): return (2 * abs(x) + 1) ** 2 + (2 * abs(z) + 1) ** 2 <= D * D


class SlabScene(nav.Scene):
    def __init__(self, blocks):
        super().__init__({k: v for k, v in blocks.items() if v[0] != 'slab'})
        for (x, y, z), (kind, _) in blocks.items():
            if kind == 'slab': self.columns.setdefault((x, z), []).append((40 * x, 40 * y, 40 * z, 40 * x + 40, 40 * y + 20, 40 * z + 40))


def load(folder, conv):
    groups = defaultdict(dict)
    for r in csv.DictReader((folder / f'blocks_{conv}.csv').open()):
        groups[r['group']][int(r['x']), int(r['y']), int(r['z'])] = (r['shape'], r['facing'])
    obj = json.loads((folder / f'objects_{conv}.json').read_text())
    return groups, obj


def baseline(conv, floors):
    base = json.loads(BASE.read_text())
    wall = {tuple(c) for c in base['columns']['wall']}; inner = {tuple(c) for c in base['columns']['inner']}
    out = {}
    for layer in base['models'][conv]['layers']:
        for x, z in (wall if layer['zone'] == 'WALL' else wall | inner): out[x, layer['y'], z] = ('full', '')
    for fy in floors[:4]:
        for x in range(12, 16):
            for y in range(fy + 1, min(fy + 8, 80)):
                for z in range(-1, 3): out.pop((x, y, z), None)
    return out, wall | inner


def scene_blocks(groups, base, names=None):
    b = dict(base)
    for g, bl in groups.items():
        if names is None or g in names:
            for p, s in bl.items():
                if p not in b or s[0] == 'full': b[p] = s
    return b


def lane_bfs(scene, lane, y_lo, y_hi, a_start, a_goal, goal_feet, limit=200000):
    """BFS on the frozen quarter-grid with the player centre confined to radial interval `lane`."""
    def ok_pos(u, v):
        x, z = u / 4 + .125 - .5, v / 4 + .125 - .5
        r = math.hypot(x, z)
        return lane[0] <= r < lane[1]
    def angle(u, v): return math.degrees(math.atan2(v / 4 + .125 - .5, u / 4 + .125 - .5)) % 360
    starts = []
    rng = range(4 * -25, 4 * 25)
    for u in rng:
        for v in rng:
            if not ok_pos(u, v): continue
            a = angle(u, v)
            if not (a_start[0] <= a <= a_start[1]): continue
            for H in scene.surfaces(u, v):
                if y_lo * 40 <= H <= (y_lo + 0.6) * 40 and scene.stand(u, v, H): starts.append((u, v, H))
    if not starts: return {'pass': False, 'reason': 'no start position in lane'}
    parent = {s: None for s in starts}; q = deque(starts); end = None
    while q and len(parent) < limit:
        u, v, h = q.popleft()
        if a_goal[0] <= angle(u, v) <= a_goal[1] and abs(h - goal_feet * 40) <= 1: end = (u, v, h); break
        for du, dv in NB4:
            U, V = u + du, v + dv
            if not ok_pos(U, V): continue
            for H in scene.surfaces(U, V):
                n = (U, V, H)
                if n in parent or not (h - 40 <= H <= h + 20) or not (y_lo * 40 - 1 <= H <= y_hi * 40 + 1) or not scene.stand(U, V, H): continue
                x, z, X, Z = 10 * u + 5, 10 * v + 5, 10 * U + 5, 10 * V + 5; top = max(h, H)
                if not scene.clear_box((min(x, X) - 12, top, min(z, Z) - 12, max(x, X) + 12, top + 72, max(z, Z) + 12)): continue
                if H > h and not scene.clear_box((x - 12, h, z - 12, x + 12, H + 72, z + 12)): continue
                parent[n] = (u, v, h); q.append(n)
    return {'pass': end is not None, 'visited': len(parent), 'limit_hit': len(parent) >= limit, 'starts': len(starts)}


def check(folder, conv, lanes=True):
    groups, obj = load(folder, conv)
    dy = obj['transform']['dy']; floors = [f + dy for f in FLOORS_C1]
    base, d31 = baseline(conv, floors)
    R = {'convention': conv, 'checks': {}}
    C = R['checks']
    # V1 boundaries
    cols = {g: {(x, z) for x, y, z in bl} for g, bl in groups.items() if bl}
    normal = {c for g, s in cols.items() if not g.startswith('balcony') for c in s}
    bal = {c for g, s in cols.items() if g.startswith('balcony') for c in s}
    ys = [p[1] for bl in groups.values() for p in bl]
    out45 = sorted(c for c in normal if not in_c(*c, 45))
    C['V1_boundary'] = {'normal_outside_D45_center': len(out45), 'balcony_outside_D51_center': len([c for c in bal if not in_c(*c, 51)]),
                        'normal_outside_D45_whole_block_alt': len([c for c in normal if not in_w(*c, 45)]),
                        'balcony_outside_D51_whole_block_alt': len([c for c in bal if not in_w(*c, 51)]),
                        'per_group_outside': {g: len([c for c in s if not in_c(*c, 51 if g.startswith('balcony') else 45)]) for g, s in cols.items()},
                        'y_min': min(ys), 'y_max': max(ys), 'height_ok': min(ys) >= -1 and max(ys) <= 79,
                        'outside_columns': out45[:50]}
    C['V1_boundary']['pass'] = not out45 and C['V1_boundary']['balcony_outside_D51_center'] == 0 and C['V1_boundary']['height_ok']
    # V2 overlaps (between groups and with the tower body)
    names = sorted(groups); ov = {}; coll = []
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            s = set(groups[a]) & set(groups[b])
            if s: ov[f'{a}&{b}'] = len(s); coll += [(a, b, p) for p in sorted(s)[:200]] if f'{a}&{b}' not in JOINS else []
    tb = {g: len(set(bl) & set(base)) for g, bl in groups.items() if set(bl) & set(base)}
    C['V2_overlaps'] = {'between_groups': ov, 'joins_allowed': sorted(JOINS), 'with_tower_body': tb,
                        'collisions': [[a, b, *p] for a, b, p in coll], 'pass': not coll}
    blocks = scene_blocks(groups, base); scene = SlabScene(blocks)
    # V3 reachability (global)
    start = (4 * 11 + 1, floors[0] + 1, 1)
    goals = [(4 * 11 + 1, f + 1, 1) for f in floors[1:]]
    balgoals = []
    for b in obj['balconies']:
        cl = b['clear_columns']; X = round(sum(c[0] for c in cl) / len(cl)); Z = round(sum(c[1] for c in cl) / len(cl))
        tops = [h / 40 for h in scene.surfaces(4 * X + 1, 4 * Z + 1)]
        T = min(tops, key=lambda t: abs(t - (b['top'] + dy))) if tops else b['top'] + dy
        balgoals.append((4 * X + 1, T, 4 * Z + 1))
    nr = scene.search(start, goals + balgoals, limit=600000)
    C['V3_reach'] = {'targets': ['F2', 'F3', 'F4', 'SANCTUM'] + [b['id'] for b in obj['balconies']], 'reached': nr['reached'],
                     'visited': nr['visited_states'], 'limit_hit': nr['limit_hit'], 'pass': all(nr['reached'])}
    # V4 lane width (4 disjoint 1-block radial lanes per lap)
    if lanes:
        lw = []
        for L in range(4):
            for j in range(WALK_W):
                r = lane_bfs(scene, (WALK_IN + j, WALK_IN + j + 1), floors[L] + 1, floors[L + 1] + 1, (13, 20), (349, 358), floors[L + 1] + 1)
                lw.append({'lap': L, 'lane': j, **r})
        C['V4_lane_width4'] = {'lanes': lw, 'pass': all(x['pass'] for x in lw)}
    # tread surfaces of the band: stair-group voxels in the 4-wide band
    stair = groups.get('stairs', {})
    occ = defaultdict(list)
    for (x, y, z), (s, f) in blocks.items(): occ[x, z].append((y, s))
    def top_of(y, s): return y + (0.5 if s == 'slab' else 1.0)
    # V5 head clearance above every stair tread in the band
    mins = []
    for (x, y, z), (s_, f) in stair.items():
        if not (WALK_IN <= math.hypot(x, z) < WALK_IN + WALK_W): continue
        T = top_of(y, s_); above = [yy for yy, ss in occ[x, z] if yy > y]
        mins.append(((min(above) - T) if above else 99, (x, y, z)))
    mn = min(mins)
    C['V5_headroom'] = {'min_clearance': round(mn[0], 2), 'at': list(mn[1]), 'below_1.8': len([m for m in mins if m[0] < 1.8]), 'pass': mn[0] >= 1.8}
    # V6 balconies
    bres = []
    for b, g in zip(obj['balconies'], balgoals):
        cl = [tuple(c) for c in b['clear_columns']]
        standable = []
        for x, z in cl:
            ok = any(abs(h / 40 - g[1]) < 1e-6 and scene.stand(4 * x + 1, 4 * z + 1, h) for h in scene.surfaces(4 * x + 1, 4 * z + 1))
            standable.append(ok)
        Rset = set(cl)
        opening = set()
        for x, z in cl:
            for dx, dz in NB4:
                n = (x + dx, z + dz)
                if n in Rset or not (WALK_IN <= math.hypot(*n) < WALK_IN + WALK_W): continue
                for yy, ss in occ[n]:
                    if abs(top_of(yy, ss) - g[1]) <= 0.5 and (n[0], yy, n[1]) in stair:
                        if not any(abs(yb - (yy + 1)) < 1 for yb, _ in occ[n] if yb > yy): opening.add(n)
        land = sorted({top_of(yy, ss) for n in opening for yy, ss in occ[n] if (n[0], yy, n[1]) in stair and abs(top_of(yy, ss) - g[1]) <= .5})
        excess = max(math.hypot(x, z) for x, z in cl) - 22.5
        bres.append({'id': b['id'], 'clear_columns': len(cl), 'all_7x3_standable': len(cl) == 21 and all(standable),
                     'shape_7x3': sorted({max(c[0] for c in cl) - min(c[0] for c in cl) + 1, max(c[1] for c in cl) - min(c[1] for c in cl) + 1}) == [3, 7],
                     'opening_width_columns': len(opening), 'landing_tops': land, 'flat_landing': len(land) == 1,
                     'max_radius_excess_over_D45': round(excess, 3), 'disjoint_from_walk_band': not any(WALK_IN <= math.hypot(*c) < WALK_IN + WALK_W for c in cl)})
    C['V6_balconies'] = {'count': len(bres), 'items': bres, 'pass': len(bres) == 8 and all(x['all_7x3_standable'] and x['shape_7x3'] and x['opening_width_columns'] >= 1 and x['disjoint_from_walk_band'] for x in bres)}
    # V7 pillar spacing (declared centres; footprint presence verified from voxels)
    pil = groups.get('pillar', {}); pcols = {(x, z) for x, y, z in pil}
    perlap = defaultdict(list); missing = []
    for p in obj['pillars']:
        if p['lap'] > 3: continue
        q = tuple(p['xz'])
        if not all((q[0] + i, q[1] + j) in pcols for i in (-1, 0, 1) for j in (-1, 0, 1)): missing.append(p['id'])
        perlap[p['lap']].append(math.degrees(math.atan2(q[1], q[0])) % 360)
    sp = {}
    for L, a in perlap.items():
        a = sorted(a); gaps = [((a[(i + 1) % len(a)] - a[i]) % 360) or 360 for i in range(len(a))]
        arc = [round(math.radians(g) * 17.5, 2) for g in gaps]
        sp[L] = {'count': len(a), 'gap_deg_max': round(max(gaps), 2), 'arc_on_walk_centreline_r17.5': {'min': min(arc), 'median': statistics.median(arc), 'max': max(arc)},
                 'sector8_counts': [sum(1 for x in a if k * 45 <= x < (k + 1) * 45) for k in range(8)], 'longest_pillar_free_arc': max(arc)}
    C['V7_pillar_spacing'] = {'per_lap': sp, 'declared_pillars_missing_voxels': missing, 'target_about': 7, 'tolerance_defined': False,
                              'status': 'UNVERIFIED (Q85 tolerance undefined)'}
    # V8 arch continuity: per lap consecutive pillars (by angle) must be face-connected through arcade_arch voxels
    arch = set(groups.get('arcade_arch', {}))
    comp = {}; cid = 0
    for v in arch:
        if v in comp: continue
        cid += 1; q = deque([v]); comp[v] = cid
        while q:
            x, y, z = q.popleft()
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if n in arch and n not in comp: comp[n] = cid; q.append(n)
    def touch_ids(cells):
        ids = set()
        for x, y, z in cells:
            for n in ((x, y, z), (x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if n in comp: ids.add(comp[n])
        return ids
    posts = [p for p in obj['pillars']]
    pcells = {p['id']: {tuple(c) for c in p['cells']} & set(pil) for p in posts}
    pairs = []
    for L in range(4):
        lp = sorted((p for p in posts if p['lap'] == L), key=lambda p: math.atan2(p['xz'][1], p['xz'][0]) % (2 * math.pi))
        nxt = sorted((p for p in posts if p['lap'] == L + 1), key=lambda p: math.atan2(p['xz'][1], p['xz'][0]) % (2 * math.pi))
        chain = lp + nxt[:1]          # helical arcade: last pillar of lap L continues to the first pillar of lap L+1
        for A, B in zip(chain, chain[1:]):
            pairs.append((A['id'], B['id'], bool(touch_ids(pcells[A['id']]) & touch_ids(pcells[B['id']]))))
    C['V8_arch_continuity'] = {'pairs': len(pairs), 'connected': sum(p[2] for p in pairs), 'missing': [p[:2] for p in pairs if not p[2]],
                               'pass': bool(pairs) and all(p[2] for p in pairs)}
    # V9 fall edges: band tread whose outer/inner non-wall neighbour has no blocker at the tread level
    unguarded = []
    for (x, y, z), (s, f) in stair.items():
        if not (WALK_IN <= math.hypot(x, z) < WALK_IN + WALK_W): continue
        T = top_of(y, s)
        for dx, dz in NB4:
            n = (x + dx, z + dz)
            if n in d31 or (WALK_IN <= math.hypot(*n) < WALK_IN + WALK_W): continue
            tops = [top_of(yy, ss) for yy, ss in occ[n] if yy <= T + 1.0 and top_of(yy, ss) >= T - 1.0]
            if not tops: unguarded.append([x, y, z, n[0], n[1]])
    C['V9_fall_edges'] = {'unguarded_edges': len(unguarded), 'sample': unguarded[:20], 'pass': not unguarded}
    R['summary'] = {k: v.get('pass', v.get('status')) for k, v in C.items()}
    return R


if __name__ == '__main__':
    folder = Path(sys.argv[1]); res = {c: check(folder, c) for c in ('C1', 'C2')}
    if len(sys.argv) > 2: Path(sys.argv[2]).write_text(json.dumps(res, separators=(',', ':')) + '\n')
    for c, r in res.items(): print(c, r['summary'])
