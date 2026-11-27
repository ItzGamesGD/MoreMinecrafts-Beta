#!/usr/bin/env python3
"""GEOMETRY-03C-P2 3D exporter for a solved plan (generator side; stdlib only).

Builds ONLY the members the solved plan defines: stair treads (03B schedule/stair-state rules), 3x3 pillar
shafts with provisional capitals, and open rails. Arches, canopies, gateways and balcony bodies are NOT
built here (P2-D / P2-C 3D stages NOT RUN). Output format = 03B export (blocks/claims/objects CSV+JSON) so the
frozen 03B verify.py / navigation modules can audit it without importing this file.
usage: python3 -I p2_build3d.py data/study_X.json out_dir
"""
import csv, json, math, sys
from collections import defaultdict, deque
from pathlib import Path

HERE = Path(__file__).resolve().parent
BASE = HERE.parents[1] / 'sky_tower_geometry_02/geometry02b/baseline.json'
FACING = {(1, 0): 'east', (-1, 0): 'west', (0, 1): 'south', (0, -1): 'north'}
FLOORS = (10, 21, 33, 47, 64)
RISES = (11, 12, 14, 17)
ARCH_LIFT = 3  # provisional, same value as the 03B candidate


def pad(p): return {(p[0] + i, p[1] + j) for i in (-1, 0, 1, 2) for j in (-1, 0, 1, 2)}
def sq3(q): return {(q[0] + i, q[1] + j) for i in (-1, 0, 1) for j in (-1, 0, 1)}
def disc(D, r=32): return {(x, z) for x in range(-r, r + 1) for z in range(-r, r + 1) if 4 * (x * x + z * z) < D * D}


def schedule(loop, extra_protected=frozenset()):
    N = len(loop)
    ds = [(loop[(i + 1) % N][0] - p[0], loop[(i + 1) % N][1] - p[1]) for i, p in enumerate(loop)]
    protected = {j % N for j in range(-4, 5)} | set(extra_protected)
    eligible = [i for i in range(N) if i not in protected and len({ds[(i + k) % N] for k in range(-5, 6)}) == 1]
    if len(eligible) < max(RISES): raise ValueError('insufficient safe straight stair stations')
    out, height = [], FLOORS[0]
    for lap, rise in enumerate(RISES):
        count, prev = 0, height
        for i, p in enumerate(loop):
            count += i in eligible
            y = height + rise * count // len(eligible)
            out.append({'lap': lap, 'station': i, 'xz': p, 'floor': y, 'direction': ds[i],
                        'rise': y - prev, 'turn': ds[i] != ds[(i - 1) % N], 'protected': i in protected})
            prev = y
        height = prev
    out.append({'lap': 4, 'station': 0, 'xz': loop[0], 'floor': FLOORS[4], 'direction': ds[0], 'rise': 0, 'turn': False, 'protected': True})
    return out


def balcony_geometry(b):
    w, h = (7, 3) if b['horiz'] else (3, 7)
    x0, z0 = b['origin']
    R = {(x0 + i, z0 + j) for i in range(w) for j in range(h)}
    ring = {(x0 + i, z0 + j) for i in range(-1, w + 1) for j in range(-1, h + 1)} - R
    contact = {(c[0] + b['side'][0], c[1] + b['side'][1]) for c in R} - R
    cx, cz = x0 + (3 if b['horiz'] else 1), z0 + (1 if b['horiz'] else 3)
    j = int(((math.atan2(cz + .5, cx + .5) / (2 * math.pi)) % 1.0) * 8)
    return R, ring, contact, j


def build(plan):
    loop = [tuple(p) for p in plan['loop']]; N = len(loop)
    bals = sorted((balcony_geometry(b) + (b,) for b in plan.get('balconies', [])), key=lambda t: t[3])
    touch = set()
    for R, ring, contact, j, b in bals:
        for i, p in enumerate(loop):
            if pad(p) & contact: touch |= {(i - 1) % N, i, (i + 1) % N}
    records = schedule(loop, touch)
    heights = defaultdict(dict)
    for i, r in enumerate(records):
        for p in pad(r['xz']): heights[p][r['floor']] = i
    states, floors = {}, defaultdict(list)
    for (x, z), levels in sorted(heights.items()):
        clusters = []
        for y in sorted(levels):
            if clusters and y - clusters[-1][-1] <= 1: clusters[-1].append(y)
            else: clusters.append([y])
        for cl in clusters:
            y = max(cl); r = records[levels[y]]; dx, dz = r['direction']
            back = heights.get((x - dx, z - dz), {})
            stair = y - 1 in back and y not in back
            states[x, y, z] = ('stair', FACING[dx, dz]) if stair else ('full', '')
            floors[x, z].append(y)
    for fy in FLOORS:
        states.update({(x, fy, z): ('full', '') for x in range(11, 20) for z in range(-1, 3)})
    frames = []
    for i, r in enumerate(records):
        x, z = r['xz']; dx, dz = r['direction']; cells = []
        for j in (-1, 0, 1, 2):
            X, Z = (x + j, z) if dz else (x, z + j)
            y = min(floors[X, Z], key=lambda y: abs(y - r['floor'])); cells.append((X, y, Z))
        padc = []
        if r['turn']:
            for X, Z in sorted(pad(r['xz'])):
                y = min(floors[X, Z], key=lambda y: abs(y - r['floor'])); padc.append((X, y, Z))
        frames.append({'index': i, 'lap': r['lap'], 'station': r['station'], 'direction': r['direction'], 'cells': cells, 'turn_pad': padc})
    walk = {(x, z) for x, y, z in states}
    groups = {'stairs': dict(states)}
    d45, d31 = disc(45), disc(31)
    order = sorted(map(tuple, plan['pillars']), key=lambda q: min(range(N), key=lambda i: ((loop[i][0] + .5 - q[0]) ** 2 + (loop[i][1] + .5 - q[1]) ** 2, i)))
    posts = []
    for lap in range(4):
        for k, q in enumerate(order):
            s = min(range(N), key=lambda i: ((loop[i][0] + .5 - q[0]) ** 2 + (loop[i][1] + .5 - q[1]) ** 2, i))
            posts.append({'id': f'P{lap + 1}-{k}', 'lap': lap, 'station': s, 'xz': list(q), 'floor': records[lap * N + s]['floor']})
    if order:
        posts.append({'id': 'P-END', 'lap': 4, 'station': posts[0]['station'], 'xz': list(order[0]), 'floor': FLOORS[4]})
    # Arcade arches between consecutive pillars of the helical chain (03B profile); footprint = 2x2 pads outside the walk.
    def small(p): return {(p[0] + i, p[1] + j) for i in (0, 1) for j in (0, 1)}
    pillar_cols = {c for q in order for c in sq3(q)}
    arches, arch_failures = [], []
    for A, B in zip(posts, posts[1:]):
        valid = {p for p in d45 if small(p) <= d45 and not small(p) & (walk | d31)}
        sA, sB = sq3(tuple(A['xz'])), sq3(tuple(B['xz']))
        starts = sorted(p for p in valid if small(p) <= sA)
        targets = {p for p in valid if small(p) <= sB}
        parent = {p: None for p in starts}; dq = deque(starts); end = None
        while dq:
            p = dq.popleft()
            if p in targets: end = p; break
            for dx, dz in FACING:
                n = (p[0] + dx, p[1] + dz)
                if n in valid and n not in parent: parent[n] = p; dq.append(n)
        path = []
        while end is not None: path.append(end); end = parent[end]
        path.reverse()
        if not path:
            arch_failures.append({'from': A['id'], 'to': B['id'], 'reason': 'no 2x2 footprint path outside walk inside D45'})
            continue
        span = max(1, len(path) - 1); spring = max(A['floor'], B['floor']) + ARCH_LIFT
        cells, prev = set(), spring
        for i, p in enumerate(path):
            y = spring + math.ceil(math.sqrt(max(0, (span / 2) ** 2 - (i - span / 2) ** 2)))
            for Y in range(min(y, prev), max(y, prev) + 1): cells.update((x, Y, z) for x, z in small(p))
            prev = y
        arches.append({'id': f'A{len(arches)}', 'from': A['id'], 'to': B['id'], 'spring': spring, 'span_cells': span,
                       'apex': max(y for x, y, z in cells), 'path_xz': path, 'raster_mode': 'outside_walk_two_by_two', 'cells': sorted(cells)})
        groups.setdefault('arcade_arch', {}).update({c: ('full', '') for c in cells})
    for p in posts:
        q = tuple(p['xz']); x, z = q; fy = p['floor']
        adj = [a for a in arches if p['id'] in (a['from'], a['to'])]
        cap = max([a['spring'] for a in adj] or [fy + ARCH_LIFT])
        body = {(X, y, Z) for X, Z in sq3(q) for y in range(fy, cap)}
        foot = {(X, fy - 1, Z) for X, Z in sq3(q)} | {(x, fy - 2, z)}
        capital = {(x, cap, z)} | {(X, cap + 1, Z) for X, Z in sq3(q)}
        groups.setdefault('pillar', {}).update({c: ('full', '') for c in body | foot | capital})
        p.update(capital_start=cap, cells=sorted(body | foot | capital))
    balconies = []
    for R, ring, contact, j, b in bals:
        lap = j % 4
        touching = [i for i, p in enumerate(loop) if pad(p) & contact]
        hs = sorted({records[lap * N + i]['floor'] for i in touching})
        h = records[lap * N + touching[len(touching) // 2]]['floor']
        edge = ring - walk
        groups.setdefault('balcony_floor', {}).update({(x, h, z): ('full', '') for x, z in R | edge})
        groups.setdefault('balcony_rail', {}).update({(x, h + 1, z): ('full', '') for x, z in edge})
        groups.setdefault('balcony_support', {}).update({(x, h - 1, z): ('full', '') for x, z in R | edge})
        balconies.append({'id': f'B{lap + 1}-{j}', 'lap': lap, 'bin': j, 'floor': h, 'flat_contact_floors': hs, 'flat': len(hs) == 1,
                          'clear': sorted((x, h, z) for x, z in R), 'floor_cells': sorted((x, h, z) for x, z in R | edge),
                          'rail_cells': sorted((x, h + 1, z) for x, z in edge), 'support_cells': sorted((x, h - 1, z) for x, z in R | edge)})
    full = disc(31)
    floorcells = set(states) | set(groups.get('balcony_floor', {}))
    rail = set()
    for x, y, z in states:
        for dx, dz in FACING:
            q = x + dx, z + dz
            if q not in walk and (q[0], y, q[1]) not in floorcells and q not in full:
                rail.add((q[0], y + 1, q[1]))
    groups['rail'] = {c: ('full', '') for c in rail}
    return loop, records, groups, frames, posts, arches, arch_failures, balconies


def export(plan, out):
    out.mkdir(parents=True, exist_ok=True)
    loop, records, groups, frames, posts, arches, arch_failures, balconies = build(plan)
    base = json.loads(BASE.read_text())
    wallcols = disc(31) - disc(25)
    for conv, dy in (('C1', 0), ('C2', -1)):
        with (out / f'blocks_{conv}.csv').open('w', newline='') as f:
            w = csv.writer(f, lineterminator='\n'); w.writerow(['group', 'x', 'y', 'z', 'shape', 'facing'])
            for g, bl in sorted(groups.items()):
                for (x, y, z), (kind, facing) in sorted(bl.items()): w.writerow([g, x, y + dy, z, kind, facing])
        baseline = set()
        for layer in base['models']['C1']['layers']:
            cols = wallcols if layer['zone'] == 'WALL' else disc(31)
            baseline |= {(x, layer['y'], z) for x, z in cols}
        void = sorted((x, y + dy, z) for fy in FLOORS[:4] for x in range(12, 16) for y in range(fy + 1, min(fy + 8, 80)) for z in range(-1, 3) if (x, y, z) in baseline)
        fr = [{**r, 'cells': [[x, y + dy, z] for x, y, z in r['cells']], 'turn_pad': [[x, y + dy, z] for x, y, z in r['turn_pad']]} for r in frames]
        (out / f'claims_{conv}.json').write_text(json.dumps({'frames': fr, 'portal_void': [list(p) for p in void], 'convention': conv,
            'floors': [y + dy for y in FLOORS], 'plan': plan['name']}, separators=(',', ':')) + '\n')
        (out / f'objects_{conv}.json').write_text(json.dumps({'canonical_convention': 'C1', 'transform': {'rotation_quarters': 0, 'dy': dy},
            'pillars': posts, 'arches': arches, 'arch_failures': arch_failures, 'balconies': balconies, 'links_canopies': [],
            'gateway_reservations': {}}, separators=(',', ':')) + '\n')
    return {'blocks': {g: len(b) for g, b in groups.items()}, 'stations': len(loop), 'records': len(records),
            'arches_built': len(arches), 'arch_footprint_failures': len(arch_failures), 'balconies': len(balconies),
            'balcony_flat_contact': [b['flat'] for b in balconies]}


if __name__ == '__main__':
    plan = json.loads(Path(sys.argv[1]).read_text())
    print(export(plan, Path(sys.argv[2])))
