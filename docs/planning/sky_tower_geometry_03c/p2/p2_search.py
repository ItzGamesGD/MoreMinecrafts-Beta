#!/usr/bin/env python3
"""GEOMETRY-03C-P2 plan-level joint search (CP-SAT). Candidate GENERATOR only.

Requires `ortools` (recorded in data/search_env.json). The independent checker p2_verify.py is stdlib
only and never imports this file. No NBT/Java/worldgen. Design values are inputs, not decisions.

Fixed inputs: B25-T3 wall outer disc D31 (baseline.json), ordinary members D45 by the P1 centre rule
4(x^2+z^2) < D^2 (column index relative to the (0.5,0.5) axis), walk = union of 4x4 pads (frozen 03B
width certificate), 3x3 pillars, balcony-only members D51.
Modelled family (every conclusion below is relative to it, see REPORT):
  loop   : simple closed 4-neighbour cycle of pad anchors, every pad fully inside D45 minus wall disc,
           odd crossing of the +x cut ray (winds once around the wall), passes anchor (17,0) (portal);
  walk W : union of the anchor pads (mode 'pads') or 4-wide frames + 4x4 turn pads (mode 'lean');
  stairs : >= Emin 'eligible' stations (11 consecutive same-direction steps, as in 03B schedule());
  pillar : 3x3 columns in D45 minus wall, disjoint from W and from each other.
"""
import argparse, json, math, sys, time
from pathlib import Path
from ortools.sat.python import cp_model
import ortools

HERE = Path(__file__).resolve().parent
DATA = HERE / 'data'
BASE = HERE.parents[1] / 'sky_tower_geometry_02/geometry02b/baseline.json'
DIRS = ((1, 0), (-1, 0), (0, 1), (0, -1))


def disc(D, r=32):
    return {(x, z) for x in range(-r, r + 1) for z in range(-r, r + 1) if 4 * (x * x + z * z) < D * D}


D31, D45, D51 = disc(31), disc(45), disc(51)
FREE = D45 - D31
# Rails (Q26/Q31: railing is inside the 45 envelope; 03B rail rule puts a rail on every non-walk 4-neighbour
# of a walk column that is not wall): a walk column may only exist if all its 4-neighbours are inside D45.
NB4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
FREE_R = {c for c in FREE if all((c[0] + dx, c[1] + dz) in D45 for dx, dz in NB4)}
pad = lambda p: {(p[0] + i, p[1] + j) for i in (-1, 0, 1, 2) for j in (-1, 0, 1, 2)}
sq3 = lambda q: {(q[0] + i, q[1] + j) for i in (-1, 0, 1) for j in (-1, 0, 1)}
PILL = sorted(q for q in FREE if sq3(q) <= FREE)


def frame(p, d):
    x, z = p
    return {(x, z + j) for j in (-1, 0, 1, 2)} if d[0] else {(x + j, z) for j in (-1, 0, 1, 2)}


def angle(c):
    return math.atan2(c[1] + .5, c[0] + .5) / (2 * math.pi)


def set_domain(kind, D):
    """Sensitivity only: ordinary-member domain = circle of diameter D (centre rule) or the axis-aligned D x D box."""
    global D45, FREE, FREE_R, PILL
    D45 = disc(D) if kind == 'disc' else {(x, z) for x in range(-(D // 2), D // 2 + 1) for z in range(-(D // 2), D // 2 + 1)}
    FREE = D45 - D31
    FREE_R = {c for c in FREE if all((c[0] + dx, c[1] + dz) in D45 for dx, dz in NB4)}
    PILL = sorted(q for q in FREE if sq3(q) <= FREE)


def balcony_candidates(contact_min=7):
    """7x3 clear rectangles (+1-block rail ring) whose long inner side touches the walk."""
    out = []
    for horiz in (True, False):
        w, h = (7, 3) if horiz else (3, 7)
        for x0 in range(-30, 31):
            for z0 in range(-30, 31):
                R = {(x0 + i, z0 + j) for i in range(w) for j in range(h)}
                ring = {(x0 + i, z0 + j) for i in range(-1, w + 1) for j in range(-1, h + 1)} - R
                if not (R | ring) <= D51 or R & D31 or ring & D31:
                    continue
                for side in ((0, -1), (0, 1)) if horiz else ((-1, 0), (1, 0)):
                    contact = {(c[0] + side[0], c[1] + side[1]) for c in R} - R
                    if len(contact & FREE_R) >= contact_min:
                        out.append({'R': R, 'ring': ring, 'contact': contact, 'horiz': horiz, 'side': side,
                                    'origin': (x0, z0)})
    return out


def solve(name, *, walk='pads', emin=17, pillars='max', ray=True, adj=True, sectors=0, offset=0.0,
          min_pillars=0, balconies=False, flat=True, obj_elig=False, contact_min=7, bins=True, domain=('disc', 45), tl=120, workers=4, seed=1, portal=True, rail=True):
    global ANCH
    set_domain(*domain)
    ANCH = sorted(p for p in FREE if pad(p) <= (FREE_R if rail else FREE))
    m = cp_model.CpModel()
    idx = {p: i for i, p in enumerate(ANCH)}
    arcs, lit = [], {}
    for p in ANCH:
        for d in DIRS:
            n = (p[0] + d[0], p[1] + d[1])
            if n in idx:
                l = m.NewBoolVar(''); lit[p, n] = l; arcs.append((idx[p], idx[n], l))
    used = {}
    for p in ANCH:
        used[p] = m.NewBoolVar(''); arcs.append((idx[p], idx[p], used[p].Not()))
    m.AddCircuit(arcs)
    cut = [l for (p, n), l in lit.items() if p[0] == n[0] and p[0] > 0 and {p[1], n[1]} == {-1, 0}]
    k = m.NewIntVar(0, 6, ''); m.Add(sum(cut) == 2 * k + 1)
    if portal:
        m.Add(used[(17, 0)] == 1)
    W = {c: m.NewBoolVar('') for c in FREE}
    if rail:
        for c in FREE - FREE_R: m.Add(W[c] == 0)
    if walk == 'pads':
        cover = {c: [] for c in FREE}
        for p in ANCH:
            for c in pad(p): cover[c].append(used[p])
        for c in FREE:
            for u in cover[c]: m.AddImplication(u, W[c])
            m.Add(W[c] <= sum(cover[c])) if cover[c] else m.Add(W[c] == 0)
    else:
        support = {c: [] for c in FREE}
        for (p, n), l in lit.items():
            for c in frame(p, (n[0] - p[0], n[1] - p[1])):
                m.AddImplication(l, W[c]); support[c].append(l)
        for p in ANCH:
            ins = [(a, l) for (a, b), l in lit.items() if b == p]
            outs = [(b, l) for (a, b), l in lit.items() if a == p]
            turn = m.NewBoolVar('')
            terms = []
            for a, li in ins:
                for b, lo in outs:
                    if (p[0] - a[0], p[1] - a[1]) != (b[0] - p[0], b[1] - p[1]) and a != b:
                        t = m.NewBoolVar(''); m.AddBoolOr([li.Not(), lo.Not(), t]); m.AddImplication(t, li); m.AddImplication(t, lo)
                        terms.append(t)
            m.AddBoolOr(terms + [turn.Not()]) if terms else m.Add(turn == 0)
            for t in terms: m.AddImplication(t, turn)
            for c in pad(p):
                if c in W: m.AddImplication(turn, W[c]); support[c].append(turn)
        for c in FREE:
            m.Add(W[c] <= sum(support[c])) if support[c] else m.Add(W[c] == 0)
    elig, eligmap = [], []
    for p in ANCH:
        for d in DIRS:
            pts = [(p[0] + t * d[0], p[1] + t * d[1]) for t in range(-5, 7)]
            # entrance station (portal anchor 17,0) and +-4 stations are protected in the schedule (Manhattan <=4 is a superset)
            if all(q in idx for q in pts) and not (portal and abs(p[0] - 17) + abs(p[1]) <= 4):
                e = m.NewBoolVar(''); elig.append(e); eligmap.append((p, d, e))
                for a, b in zip(pts, pts[1:]): m.AddImplication(e, lit[a, b])
    m.Add(sum(elig) >= emin)
    if obj_elig: m.Maximize(sum(elig))
    P = {}
    if pillars:
        P = {q: m.NewBoolVar('') for q in PILL}
        occ = {}
        for q in PILL:
            for c in sq3(q):
                occ.setdefault(c, []).append(P[q]); m.AddBoolOr([P[q].Not(), W[c].Not()])
        for c, v in occ.items(): m.Add(sum(v) <= 1)
        for q in PILL:
            s = sq3(q)
            if adj:
                nb = {(x + dx, z + dz) for x, z in s for dx, dz in DIRS} - s
                m.Add(sum(W[c] for c in nb if c in W) >= 1).OnlyEnforceIf(P[q])
            if ray:
                r = math.hypot(*q)
                rc = [c for c in FREE if 15.5 <= math.hypot(*c) <= r - 1.5 and
                      abs(c[0] * q[1] - c[1] * q[0]) / max(r, 1e-9) <= .7 and c[0] * q[0] + c[1] * q[1] > 0]
                m.Add(sum(W[c] for c in rc) >= 1).OnlyEnforceIf(P[q]) if rc else m.Add(P[q] == 0)
        if sectors:
            for kk in range(sectors):
                m.Add(sum(P[q] for q in PILL if int(((angle(q) + offset) % 1.0) * sectors) == kk) >= 1)
        if min_pillars: m.Add(sum(P.values()) >= min_pillars)
        if pillars == 'max': m.Maximize(sum(P.values()))
    B = []
    if balconies:
        cands = balcony_candidates(contact_min)
        for b in cands:
            b['v'] = m.NewBoolVar('')
            for c in b['R']:
                if c in W: m.AddBoolOr([b['v'].Not(), W[c].Not()])
            m.Add(sum(W[c] for c in b['contact'] if c in W) >= contact_min).OnlyEnforceIf(b['v'])
            if P:
                for q in PILL:
                    if sq3(q) & (b['R'] | b['ring']): m.AddBoolOr([b['v'].Not(), P[q].Not()])
        cellocc = {}
        for i, b in enumerate(cands):
            for c in b['R'] | b['ring']: cellocc.setdefault(c, []).append(b['v'])
        for c, v in cellocc.items(): m.Add(sum(v) <= 1)
        for kk in range(8):
            ids = [b['v'] for b in cands if int((angle(((b['origin'][0] + (3 if b['horiz'] else 1)), (b['origin'][1] + (1 if b['horiz'] else 3)))) % 1.0) * 8) == kk]
            if bins: m.Add(sum(ids) <= 1) if balconies in ('max',) or (isinstance(balconies, int) and balconies > 1) else m.Add(sum(ids) == 1)
        if balconies == 'max': m.Maximize(sum(b['v'] for b in cands))
        elif isinstance(balconies, int) and balconies > 1: m.Add(sum(b['v'] for b in cands) == balconies)
        elif not bins: m.Add(sum(b['v'] for b in cands) == 8)
        # balcony landings must stay flat: stations whose pad touches a selected balcony contact (and +-1 neighbour) cannot rise
        touchsel = {p: m.NewBoolVar('') for p in ANCH}
        for b in cands:
            for p in ANCH:
                if pad(p) & b['contact']: m.AddImplication(b['v'], touchsel[p])
        for p, d, e in (eligmap if flat else []):
            for t in (-1, 0, 1):
                q = (p[0] + t * d[0], p[1] + t * d[1])
                if q in touchsel: m.AddImplication(e, touchsel[q].Not())
        B = cands
    s = cp_model.CpSolver()
    s.parameters.max_time_in_seconds = tl; s.parameters.num_workers = workers; s.parameters.random_seed = seed
    t = time.time(); st = s.Solve(m)
    rec = {'name': name, 'options': dict(walk=walk, emin=emin, pillars=pillars, ray=ray, adj=adj, sectors=sectors,
           offset=offset, min_pillars=min_pillars, balconies=balconies, flat=flat, obj_elig=obj_elig, contact_min=contact_min, bins=bins, domain=list(domain), portal=portal, rail=rail),
           'status': s.StatusName(st), 'seconds': round(time.time() - t, 1)}
    if st in (cp_model.OPTIMAL, cp_model.FEASIBLE):
        nodes = [p for p in ANCH if s.Value(used[p])]
        nxt = {p: n for (p, n), l in lit.items() if s.Value(l)}
        order, p = [], (17, 0) if portal else nodes[0]
        while True:
            order.append(p); p = nxt[p]
            if p == order[0]: break
        rec['loop'] = order
        rec['pillars'] = [q for q in PILL if P and s.Value(P[q])]
        rec['walk_columns'] = sorted(c for c in FREE if s.Value(W[c]))
        rec['balconies'] = [{'origin': b['origin'], 'horiz': b['horiz'], 'side': b['side']} for b in B if s.Value(b['v'])]
        rec['eligible_stations_in_solution'] = sum(s.Value(e) for e in elig)
        if pillars == 'max' or obj_elig or balconies == 'max': rec['objective'] = s.ObjectiveValue(); rec['bound'] = s.BestObjectiveBound()
    print(name, rec['status'], rec['seconds'], len(rec.get('pillars', [])), flush=True)
    return rec


STUDIES = [
    ('A0_loop_only', dict(pillars=None)),
    ('B1_max_pillars_pads', dict(pillars='max')),
    ('B2_max_pillars_pads_no_ray', dict(pillars='max', ray=False)),
    ('B3_max_pillars_pads_no_ray_no_adj', dict(pillars='max', ray=False, adj=False)),
    ('B4_max_pillars_pads_no_constraints_but_loop', dict(pillars='max', ray=False, adj=False, emin=0, portal=False)),
    ('B5_max_pillars_lean_walk', dict(pillars='max', walk='lean', tl=300)),
    ('B6_max_pillars_lean_no_ray_no_adj', dict(pillars='max', walk='lean', ray=False, adj=False, emin=0, portal=False, tl=300)),
    ('S8_sectors8_pads', dict(pillars='feas', sectors=8)),
    ('S9_sectors9_pads', dict(pillars='feas', sectors=9)),
    ('S10_sectors10_pads', dict(pillars='feas', sectors=10)),
    ('S10b_sectors10_offset_half', dict(pillars='feas', sectors=10, offset=0.05)),
    ('S12_sectors12_pads', dict(pillars='feas', sectors=12)),
    ('S17_sectors17_pads', dict(pillars='feas', sectors=17)),
    ('S17_sectors17_lean_no_ray_no_adj', dict(pillars='feas', sectors=17, walk='lean', ray=False, adj=False, emin=0, portal=False, tl=300)),
    ('C0_max_rise_stations_no_balconies', dict(pillars=None, emin=0, obj_elig=True)),
    ('C1_loop_8_balconies_flat_landings', dict(pillars=None, balconies=True)),
    ('C2_max_rise_stations_with_8_balconies_flat', dict(pillars=None, balconies=True, emin=0, obj_elig=True)),
    ('C3_max_balconies_with_17_rise_stations', dict(pillars=None, balconies='max')),
    ('C4_loop_8_balconies_NOT_flat_diagnostic', dict(pillars=None, balconies=True, flat=False, emin=0)),
    ('C5_max_pillars_with_8_balconies_NOT_flat_diagnostic', dict(pillars='max', balconies=True, flat=False, emin=0, tl=300)),
    ('BEST_max_pillars_sectors8', dict(pillars='max', sectors=8)),
    ('D1_RELAXED_contact1_loop_8_balconies_flat', dict(pillars=None, balconies=True, contact_min=1)),
    ('D2_RELAXED_contact1_max_rise_with_8_balconies', dict(pillars=None, balconies=True, contact_min=1, emin=0, obj_elig=True)),
    ('D3_RELAXED_contact1_max_pillars_with_8_balconies_17_rise', dict(pillars='max', balconies=True, contact_min=1, tl=300)),
    ('D4_RELAXED_contact3_max_pillars_with_8_balconies_17_rise', dict(pillars='max', balconies=True, contact_min=3, tl=300)),
    ('E1_NOBINS_max_balconies_with_17_rise_contact7', dict(pillars=None, balconies='max', bins=False)),
    ('E2_NOBINS_max_balconies_with_17_rise_contact1', dict(pillars=None, balconies='max', bins=False, contact_min=1)),
    ('E3_NOBINS_8_balconies_max_rise_contact1', dict(pillars=None, balconies=True, bins=False, contact_min=1, emin=0, obj_elig=True)),
    ('F1_4_balconies_max_pillars_17_rise', dict(pillars='max', balconies=4, tl=300)),
    ('F2_4_balconies_loop_17_rise', dict(pillars=None, balconies=4)),
    ('G1_SENS_disc47_max_pillars', dict(pillars='max', domain=('disc', 47))),
    ('G2_SENS_disc49_max_pillars', dict(pillars='max', domain=('disc', 49))),
    ('G3_SENS_disc51_max_pillars', dict(pillars='max', domain=('disc', 51))),
    ('G4_SENS_disc53_max_pillars', dict(pillars='max', domain=('disc', 53))),
    ('G5_SENS_box45_max_pillars', dict(pillars='max', domain=('box', 45))),
    ('G6_SENS_box45_sectors17', dict(pillars='feas', sectors=17, domain=('box', 45))),
    ('G7_SENS_disc51_sectors17', dict(pillars='feas', sectors=17, domain=('disc', 51))),
    ('H1_SENS_disc46_sectors17', dict(pillars='feas', sectors=17, domain=('disc', 46))),
    ('H2_SENS_disc47_sectors17', dict(pillars='feas', sectors=17, domain=('disc', 47))),
    ('H3_SENS_disc48_sectors17', dict(pillars='feas', sectors=17, domain=('disc', 48))),
    ('H4_SENS_disc49_sectors17', dict(pillars='feas', sectors=17, domain=('disc', 49))),
    ('H5_SENS_disc46_max_pillars', dict(pillars='max', domain=('disc', 46))),
    ('N1_NORAIL_max_pillars_pads', dict(pillars='max', rail=False)),
    ('N2_NORAIL_loop_only', dict(pillars=None, rail=False)),
]


def main():
    ap = argparse.ArgumentParser(); ap.add_argument('--only'); ap.add_argument('--workers', type=int, default=4)
    a = ap.parse_args()
    DATA.mkdir(exist_ok=True)
    (DATA / 'search_env.json').write_text(json.dumps({'ortools': ortools.__version__, 'python': sys.version.split()[0],
        'workers': a.workers, 'seed': 1, 'note': 'multi-worker CP-SAT may return different equally valid solutions; '
        'status/optimum values are solver-proved, committed solutions are re-checked by p2_verify.py'}, indent=1))
    out = {}
    for name, kw in STUDIES:
        if a.only and a.only not in name: continue
        out[name] = solve(name, workers=a.workers, **kw)
        (DATA / f'study_{name}.json').write_text(json.dumps(out[name], separators=(',', ':')))
    print('done')


if __name__ == '__main__':
    main()
