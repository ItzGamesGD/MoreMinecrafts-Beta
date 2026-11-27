#!/usr/bin/env python3
"""GEOMETRY-03C-P3 candidate GENERATOR (experimental alternative models; not design authority).

Model ANN ("annular walk"): the 4-wide walk is the set of columns whose centre radius r (P1 rule, axis (0.5,0.5))
satisfies 15.5 <= r < RB, i.e. a radial-width band hugging the B25-T3 wall disc D31, instead of P2's union of
4x4 anchor pads (assumption A1).  Treads are level across each radial cross-section and rise with the angle.
  rise='slab'  : half-block steps (bottom slab / full block), walkable in any direction (MC step height 0.6)
  rise='stair' : whole-block steps with stair blocks facing the local ascent (03B-style risers on a curve)
Pillars (3x3), balconies (7x3 + rail ring, D51 only) are SELECTED jointly with CP-SAT (ortools) inside the ring
D45 - D31 - band.  All ordinary members are generated inside the circular D45 (centre rule).
Writes 03B-like exports (blocks_C1/C2.csv with shape slab|full|stair, claims_*.json, objects_*.json).
The independent checker p3_verify.py never imports this file.
"""
import argparse, csv, json, math, sys, time
from collections import deque
from pathlib import Path

HERE = Path(__file__).resolve().parent
BASE = HERE.parents[1] / 'sky_tower_geometry_02/geometry02b/baseline.json'
FLOORS = (10, 21, 33, 47, 64)             # tread block y at each portal; walking surface = y + 1
RISES = tuple(b - a for a, b in zip(FLOORS, FLOORS[1:]))   # 11, 12, 14, 17
ARCH_LIFT = 3
NB4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
FACING = {(1, 0): 'east', (-1, 0): 'west', (0, 1): 'south', (0, -1): 'north'}
TAU = 2 * math.pi


def disc(D, r=32): return {(x, z) for x in range(-r, r + 1) for z in range(-r, r + 1) if 4 * (x * x + z * z) < D * D}
def sq3(q): return {(q[0] + i, q[1] + j) for i in (-1, 0, 1) for j in (-1, 0, 1)}
def ang(c): return math.atan2(c[1], c[0]) % TAU          # column centre offset from the axis is exactly (x, z)


class Geometry:
    def __init__(self, rb=19.5, domain=('disc', 45)):
        self.rb = rb
        self.D31 = disc(31); self.D51 = disc(51)
        kind, D = domain
        if kind == 'disc': self.D45 = disc(D)
        elif kind == 'whole': self.D45 = {(x, z) for x in range(-32, 33) for z in range(-32, 33) if (2 * abs(x) + 1) ** 2 + (2 * abs(z) + 1) ** 2 <= D * D}
        else: self.D45 = {(x, z) for x in range(-(D // 2), D // 2 + 1) for z in range(-(D // 2), D // 2 + 1)}
        self.band = {c for c in self.D45 - self.D31 if math.hypot(*c) < rb}
        self.ring = self.D45 - self.D31 - self.band
        self.pillar_cands = sorted(q for q in self.ring if sq3(q) <= self.ring and
                                   any((x + dx, z + dz) in self.band for x, z in sq3(q) for dx, dz in NB4))

    def balcony_cands(self, contact_min):
        out = []
        for horiz in (True, False):
            w, h = (7, 3) if horiz else (3, 7)
            for x0 in range(-27, 27):
                for z0 in range(-27, 27):
                    R = {(x0 + i, z0 + j) for i in range(w) for j in range(h)}
                    if R & (self.band | self.D31) or not R <= self.D51: continue
                    ring = {(x0 + i, z0 + j) for i in range(-1, w + 1) for j in range(-1, h + 1)} - R
                    if not ring <= self.D51 or ring & self.D31: continue
                    contact = {c for c in ring & self.band if any((c[0] + dx, c[1] + dz) in R for dx, dz in NB4)}
                    if len(contact) < contact_min: continue
                    cx, cz = x0 + (w - 1) / 2, z0 + (h - 1) / 2
                    out.append({'origin': (x0, z0), 'horiz': horiz, 'R': R, 'ring': ring, 'contact': contact,
                                'angle': math.atan2(cz, cx) % TAU})
        return out


def select(g, *, gap_deg, balconies=True, contact_min=3, a5_strict=True, max_pillars=False, tl=120, seed=1, fixed_pillars=None, objective=True):
    """CP-SAT: pillars per lap (shared if a5_strict) covering every angular window of gap_deg, 8 balconies one per 45deg bin."""
    from ortools.sat.python import cp_model
    m = cp_model.CpModel()
    P = g.pillar_cands
    laps = [0] if a5_strict else [0, 1, 2, 3]
    pv = {(L, q): m.NewBoolVar('') for L in laps for q in P}
    for L in laps:
        occ = {}
        for q in P:
            for c in sq3(q): occ.setdefault(c, []).append(pv[L, q])
        for v in occ.values(): m.Add(sum(v) <= 1)
        angs = {q: math.degrees(ang(q)) for q in P}
        for a in range(360):
            ids = [pv[L, q] for q in P if (angs[q] - a) % 360 < gap_deg]
            m.Add(sum(ids) >= 1)
    if fixed_pillars is not None:                  # sequential mode: pillars frozen from an earlier selection
        for L in laps:
            for q in P: m.Add(pv[L, q] == (1 if q in fixed_pillars else 0))
    B = []
    if balconies:
        B = g.balcony_cands(contact_min)
        for b in B:
            b['v'] = m.NewBoolVar(''); b['bin'] = int(b['angle'] / (TAU / 8)) % 8; b['lap'] = b['bin'] % 4
        for k in range(8): m.Add(sum(b['v'] for b in B if b['bin'] == k) == 1)
        cell = {}
        for b in B:
            for c in b['R'] | (b['ring'] - g.band): cell.setdefault(c, []).append(b['v'])
        for v in cell.values(): m.Add(sum(v) <= 1)
        for b in B:
            foot = b['R'] | (b['ring'] - g.band)
            Ls = laps if a5_strict else [b['lap']]
            for L in Ls:
                for q in P:
                    if sq3(q) & foot: m.AddBoolOr([b['v'].Not(), pv[L, q].Not()])
    if max_pillars: m.Maximize(sum(pv.values()))
    elif objective: m.Minimize(sum(pv.values()))            # fewest pillars that still satisfy the window coverage (no clutter)
    s = cp_model.CpSolver(); s.parameters.max_time_in_seconds = tl; s.parameters.num_workers = 1; s.parameters.random_seed = seed   # single worker: deterministic
    t = time.time(); st = s.Solve(m)
    res = {'status': s.StatusName(st), 'seconds': round(time.time() - t, 1), 'gap_deg': gap_deg, 'balconies_required': balconies,
           'contact_min': contact_min, 'a5_strict': a5_strict, 'sequential_fixed_pillars': fixed_pillars is not None}
    if st in (cp_model.OPTIMAL, cp_model.FEASIBLE):
        res['pillars'] = {L: sorted(q for q in P if s.Value(pv[0 if a5_strict else L, q])) for L in range(4)}
        res['balconies'] = [{'origin': list(b['origin']), 'horiz': b['horiz'], 'bin': b['bin'], 'lap': b['lap']} for b in B if s.Value(b['v'])]
        res['objective'] = s.ObjectiveValue()
    return res


# ------------------------------------------------------------------------------------------- 3D build
class Heights:
    """Tread top height per lap as a function of the column angle. Flats: portal landing +-theta_p and balcony landings."""
    def __init__(self, rise, landings, theta_p=math.radians(12)):
        self.rise, self.tp = rise, theta_p
        self.rising = {}
        for L in range(4):
            iv = [(theta_p, TAU - theta_p)]
            for a0, a1 in landings.get(L, []):
                cuts = [(a0, a1)] if 0 <= a0 and a1 <= TAU else [(a0 % TAU, TAU), (0, a1 % TAU)] if a0 < 0 else [(a0, TAU), (0, a1 - TAU)]
                for c0, c1 in cuts:
                    nxt = []
                    for u0, u1 in iv:
                        if c1 <= u0 or c0 >= u1: nxt.append((u0, u1)); continue
                        if c0 > u0: nxt.append((u0, c0))
                        if c1 < u1: nxt.append((c1, u1))
                    iv = nxt
            self.rising[L] = iv

    def measure(self, L, t):
        return sum(max(0.0, min(u1, t) - u0) for u0, u1 in self.rising[L])

    def top(self, L, t):
        total = self.measure(L, TAU)
        f = min(1.0, self.measure(L, t) / total) if t < TAU - self.tp else 1.0
        y0 = FLOORS[L] + 1
        if self.rise == 'slab': return y0 + math.floor(2 * RISES[L] * f + 1e-9) / 2
        return y0 + math.floor(RISES[L] * f + 1e-9)


def build(g, sel, *, rise='slab', landing_cells=9, theta_p_deg=12):
    band = g.band
    bal = []
    for b in sel.get('balconies', []):
        w, h = (7, 3) if b['horiz'] else (3, 7)
        x0, z0 = b['origin']
        R = {(x0 + i, z0 + j) for i in range(w) for j in range(h)}
        ring = {(x0 + i, z0 + j) for i in range(-1, w + 1) for j in range(-1, h + 1)} - R
        contact = {c for c in ring & band if any((c[0] + dx, c[1] + dz) in R for dx, dz in NB4)}
        bal.append({**b, 'R': R, 'ring': ring, 'contact': contact})
    landings = {}
    for b in bal:
        angs = [ang(c) for c in b['contact']]
        mid = math.atan2(sum(math.sin(a) for a in angs), sum(math.cos(a) for a in angs)) % TAU
        half = (landing_cells / 2) / 17.5             # landing length measured on the walk centreline r = 17.5
        span = max(abs(((a - mid + math.pi) % TAU) - math.pi) for a in angs)
        hw = max(half, 0.0) if landing_cells > 0 else 0.0
        a0, a1 = (mid - max(span, hw), mid + max(span, hw)) if landing_cells > 0 else (mid, mid)
        b['landing'] = (a0, a1)
        if landing_cells > 0: landings.setdefault(b['lap'], []).append((a0, a1))
    H = Heights(rise, landings, math.radians(theta_p_deg))
    groups = {g_: {} for g_ in ('stairs', 'pillar', 'rail', 'arcade_arch', 'balcony_floor', 'balcony_rail', 'balcony_support')}
    tread = {}                                        # (L, x, z) -> top height
    for L in range(4):
        for c in band:
            tread[L, c[0], c[1]] = H.top(L, ang(c))
    # landings for the 5th (sanctum) level exist only at the portal: covered by lap 3's end (t >= tau - tp) and lap starts
    def put_tread(x, T, z, ascent_dir=None):
        if abs(T - round(T)) > 1e-9:                  # half height -> bottom slab whose top is T
            groups['stairs'][(x, math.floor(T), z)] = ('slab', '')
        else:
            y = int(round(T)) - 1
            if ascent_dir is not None and rise == 'stair':
                groups['stairs'][(x, y, z)] = ('stair', FACING[ascent_dir])
            else:
                groups['stairs'][(x, y, z)] = ('full', '')
    for (L, x, z), T in tread.items():
        d = None
        if rise == 'stair':
            for dx, dz in NB4:          # a riser cell: some 4-neighbour of the same lap is exactly one lower
                if tread.get((L, x - dx, z - dz)) == T - 1: d = (dx, dz); break
        put_tread(x, T, z, d)
    for fy in FLOORS:                                 # portal bridge, as 03B/P2
        for x in range(11, 20):
            for z in range(-1, 3):
                if (x, z) not in band: groups['stairs'][(x, fy, z)] = ('full', '')
    # pillars + arches per lap (helical chain as 03B)
    posts = []
    for L in range(4):
        for k, q in enumerate(sorted(map(tuple, sel['pillars'][L] if L in sel['pillars'] else sel['pillars'][str(L)]), key=ang)):
            T = H.top(L, ang(q)); fy = math.ceil(T) - 1
            posts.append({'id': f'P{L + 1}-{k}', 'lap': L, 'xz': list(q), 'floor': fy, 'angle': math.degrees(ang(q))})
    # no terminal pier: the stair ends at the sanctum portal landing (P2/03B 'P-END' would float above the sanctum floor here)
    def small(p): return {(p[0] + i, p[1] + j) for i in (0, 1) for j in (0, 1)}
    valid = {p for p in g.D45 if small(p) <= g.D45 and not small(p) & (band | g.D31)}
    arches, arch_fail = [], []
    for A, Bp in zip(posts, posts[1:]):
        sA, sB = sq3(tuple(A['xz'])), sq3(tuple(Bp['xz']))
        starts = sorted(p for p in valid if small(p) <= sA); targets = {p for p in valid if small(p) <= sB}
        par = {p: None for p in starts}; dq = deque(starts); end = None
        while dq:
            p = dq.popleft()
            if p in targets: end = p; break
            for dx, dz in NB4:
                n = (p[0] + dx, p[1] + dz)
                if n in valid and n not in par: par[n] = p; dq.append(n)
        path = []
        while end is not None: path.append(end); end = par[end]
        path.reverse()
        if not path: arch_fail.append({'from': A['id'], 'to': Bp['id']}); continue
        spring = max(A['floor'], Bp['floor']) + ARCH_LIFT
        # semicircle (Q87) over the CLEAR opening between pillar faces, measured along the pillar-centre chord
        ax, az = A['xz']; bx, bz = Bp['xz']; chord = math.hypot(bx - ax, bz - az)
        rad = max(0.5, (chord - 3) / 2); ux, uz = ((bx - ax) / chord, (bz - az) / chord) if chord else (0, 0)
        cells, prev = set(), spring
        for i, p in enumerate(path):
            t = ((p[0] + .5 - ax) * ux + (p[1] + .5 - az) * uz) - chord / 2
            y = spring + math.ceil(math.sqrt(max(0, rad ** 2 - t ** 2)))
            for Y in range(min(y, prev), max(y, prev) + 1): cells.update((x, Y, z) for x, z in small(p))
            prev = y
        arches.append({'id': f'A{len(arches)}', 'from': A['id'], 'to': Bp['id'], 'spring': spring, 'opening': round(chord - 3, 2), 'radius': round(rad, 2),
                       'apex': max(c[1] for c in cells), 'cells': sorted(cells), 'path_xz': path, 'raster_mode': 'chord_semicircle_2x2'})
        groups['arcade_arch'].update({c: ('full', '') for c in cells})
    pillar_lapcells = {}
    for p in posts:
        q = tuple(p['xz']); fy = p['floor']
        adj = [a['spring'] for a in arches if p['id'] in (a['from'], a['to'])]
        cap = max(adj or [fy + ARCH_LIFT])
        cells = {(X, y, Z) for X, Z in sq3(q) for y in range(fy - 1, cap)} | {(q[0], cap, q[1])} | {(X, cap + 1, Z) for X, Z in sq3(q)}
        groups['pillar'].update({c: ('full', '') for c in cells}); p['cells'] = sorted(cells); p['capital_start'] = cap
        if p['lap'] < 4: pillar_lapcells.setdefault(p['lap'], set()).update(sq3(q))
    # balconies (floor = landing tread level of their lap)
    bal_objs = []
    for b in bal:
        L = b['lap']; Ts = sorted({tread[L, x, z] for x, z in b['contact']})
        T = Ts[len(Ts) // 2]
        edge = b['ring'] - band
        shp = ('slab', '') if abs(T - round(T)) > 1e-9 else ('full', '')
        yfl = math.floor(T) if shp[0] == 'slab' else int(round(T)) - 1
        for x, z in b['R'] | edge: groups['balcony_floor'][(x, yfl, z)] = shp; groups['balcony_support'][(x, yfl - 1, z)] = ('full', '')
        for x, z in edge:
            for y in (yfl + 1, yfl + 2): groups['balcony_rail'][(x, y, z)] = ('full', '')
        bal_objs.append({'id': f'B{L + 1}-{b["bin"]}', 'lap': L, 'bin': b['bin'], 'top': T, 'contact_tops': Ts,
                         'flat_contact': len(Ts) == 1, 'clear_columns': sorted(b['R']), 'origin': b['origin'], 'horiz': b['horiz'],
                         'landing_deg': [round(math.degrees(a), 2) for a in b['landing']]})
    # rails: outer/inner non-wall 4-neighbours of each lap's treads, except pillars of that lap and that lap's balcony footprint
    balfoot = {}
    for b in bal: balfoot.setdefault(b['lap'], set()).update(b['R'] | (b['ring'] - band))
    for (L, x, z), T in tread.items():
        for dx, dz in NB4:
            n = (x + dx, z + dz)
            if n in band or n in g.D31 or n in balfoot.get(L, ()): continue
            for y in (math.floor(T), math.floor(T) + 1):
                if (n[0], y, n[1]) not in groups['pillar']: groups['rail'][(n[0], y, n[1])] = ('full', '')
    # rails must not cap other laps' treads at a portal bridge column
    meta = {'rise': rise, 'rb': g.rb, 'landing_cells': landing_cells, 'theta_p_deg': theta_p_deg,
            'tread_tops': {f'{L},{x},{z}': T for (L, x, z), T in tread.items()}}
    return groups, posts, arches, arch_fail, bal_objs, meta


def export(groups, posts, arches, arch_fail, bal_objs, meta, out, sel):
    out.mkdir(parents=True, exist_ok=True)
    base = json.loads(BASE.read_text())
    for conv, dy in (('C1', 0), ('C2', -1)):
        with (out / f'blocks_{conv}.csv').open('w', newline='') as f:
            w = csv.writer(f, lineterminator='\n'); w.writerow(['group', 'x', 'y', 'z', 'shape', 'facing'])
            for gname, bl in sorted(groups.items()):
                for (x, y, z), (s, fc) in sorted(bl.items()): w.writerow([gname, x, y + dy, z, s, fc])
        (out / f'objects_{conv}.json').write_text(json.dumps({'canonical_convention': 'C1', 'transform': {'rotation_quarters': 0, 'dy': dy},
            'pillars': posts, 'arches': arches, 'arch_failures': arch_fail, 'balconies': bal_objs, 'selection': {k: v for k, v in sel.items() if k != 'pillars'},
            'floors': [y + dy for y in FLOORS], 'meta': {k: v for k, v in meta.items() if k != 'tread_tops'}}, separators=(',', ':')) + '\n')
    (out / 'tread_tops_C1.json').write_text(json.dumps(meta['tread_tops'], separators=(',', ':')) + '\n')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--selection', type=Path, required=True); ap.add_argument('--out', type=Path, required=True)
    ap.add_argument('--rise', default='slab'); ap.add_argument('--landing', type=int, default=9); ap.add_argument('--rb', type=float, default=19.5)
    a = ap.parse_args()
    sel = json.loads(a.selection.read_text())
    sel['pillars'] = {int(k): [tuple(q) for q in v] for k, v in sel['pillars'].items()}
    g = Geometry(a.rb)
    res = build(g, sel, rise=a.rise, landing_cells=a.landing)
    export(*res, a.out, sel)
    groups = res[0]
    print({k: len(v) for k, v in groups.items()}, 'arches', len(res[2]), 'arch_fail', len(res[3]), 'balconies', len(res[4]))


if __name__ == '__main__':
    main()
