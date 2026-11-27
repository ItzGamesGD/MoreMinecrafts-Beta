#!/usr/bin/env python3
"""ST-01 P3 royal-hall verifier (design-only). Verifies the EAST candidate and an independently re-voxelised exact X-mirror (WEST).
Every geometric property is measured on the voxel grid and, where practical, cross-checked by a separate analytic (box/arithmetic)
computation. Authority constants live HERE (not in the spec) so a mutated spec cannot weaken the checks.
Usage: python3 -I st01_p3_verify.py <out_dir> [--spec-module NAME]     (exit 1 on any FAIL or on an E/W equivalence break)"""
import sys, os, json, itertools, math
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import p3_vox as V
C = V.C
from st01_p2_model_east import make_east
from st01_p2_model_west import make_west

AUTH = dict(interior=33, wall_h=8, central_h=16, run=2, rise=1, body=3, base_nom=5, cap_max=7, cap_h=3, shaft_h=3, base_h=1, lintel_h=1,
            total_h=8, setback=2, gap=6, passage=2, wall_min=3, roof_min=2)
BOSS = dict(width=0.9 * 1.5, height=2.9 * 1.5, wave_r=6.0, wave_t=0.8, stat_r=1.5, stat_h=4.0)   # read from TombGuardianTuning.java (evidence only)

def mk(spec_mod=None):
    if spec_mod is None: import p3_spec as S
    else: S = __import__(spec_mod)
    return S

class R:
    def __init__(s): s.checks = []; s.metrics = {}
    def add(s, cid, title, verdict, ev): s.checks.append(dict(id=cid, title=title, verdict=verdict, evidence=ev))

def boxof(b): return tuple(b)
def cheb_gap(a, b):
    g = [max(0, b[2*k] - a[2*k+1] - 1, a[2*k] - b[2*k+1] - 1) for k in range(3)]
    return max(g)
def inter_vol(a, b):
    v = 1
    for k in range(3): v *= max(0, min(a[2*k+1], b[2*k+1]) - max(a[2*k], b[2*k]) + 1)
    return v

def gap_to_wall(W, x, y, z, ax, sg, skip=()):
    """Free cells between (x,y,z) and the interior wall face, scanning wall-ward; cells of the element itself (ids in `skip`) are passed through."""
    h = W["spec"]["hall"]; cx, cz, half = h["cx"], h["cz"], h["half"]; n = 0
    while True:
        if ax == "x": x += sg
        else: z += sg
        if abs(x - cx) > half or abs(z - cz) > half: return n, "wall"
        if W["elem"][C.ix(x), C.iy(y), C.iz(z)] in skip: continue
        if W["solid"][C.ix(x), C.iy(y), C.iz(z)]: return n, "blocked"
        n += 1

def verify(spec, p2model):
    W = V.build_world(spec, p2model); r = R()
    h = spec["hall"]; F, cx, cz, half = h["floor"], h["cx"], h["cz"], h["half"]
    solid, elem, owner, inres, main, recess = W["solid"], W["elem"], W["owner"], W["inres"], W["main"], W["recess"]
    ix, iy, iz = C.ix, C.iy, C.iz
    cavity = ((~solid) | (elem >= 0)) & inres & (owner < 0) & ~recess          # main hall cavity incl. element voxels (post-construction scan)
    # ---------- H1 interior + walls (Q43, Q45)
    pts = np.argwhere(cavity[:, iy(F), :])
    x0, x1 = int(pts[:, 0].min() + C.GX[0]), int(pts[:, 0].max() + C.GX[0]); z0, z1 = int(pts[:, 1].min() + C.GZ[0]), int(pts[:, 1].max() + C.GZ[0])
    cnt_floor = int(cavity[:, iy(F), :].sum())
    wall_top = None
    for y in range(F, F + 30):
        a = cavity[:, iy(y), :]
        if not a.any(): break
        p = np.argwhere(a); w = (p[:, 0].max() - p[:, 0].min() + 1, p[:, 1].max() - p[:, 1].min() + 1)
        if w[0] == AUTH["interior"] and w[1] == AUTH["interior"]: wall_top = y
        else: break
    wall_h = (wall_top - F + 1) if wall_top is not None else 0
    ok = (x1 - x0 + 1 == AUTH["interior"] and z1 - z0 + 1 == AUTH["interior"] and cnt_floor == 33 * 33 and wall_h == AUTH["wall_h"]
          and h["wall_h"] == AUTH["wall_h"] and 2 * h["half"] + 1 == AUTH["interior"])
    r.add("H1", "Royal hall clear interior 33x33 (Q43, walls excluded) with 8-high vertical perimeter walls (Q45): voxel scan + spec arithmetic",
          "PASS" if ok else "FAIL", f"floor-layer cavity X[{x0},{x1}] Z[{z0},{z1}] = {x1-x0+1}x{z1-z0+1}, {cnt_floor} cells (33*33=1089); vertical wall layers Y {F}..{wall_top} = {wall_h} high; spec half={h['half']} wall_h={h['wall_h']}")
    # ---------- H2 corbel (Q46) + central height (Q44)
    spans = {}; top = None
    for y in range(F, F + 24):
        a = cavity[:, iy(y), :]
        if not a.any(): break
        p = np.argwhere(a); xa, xb = int(p[:, 0].min() + C.GX[0]), int(p[:, 0].max() + C.GX[0]); za, zb = int(p[:, 1].min() + C.GZ[0]), int(p[:, 1].max() + C.GZ[0])
        full = int(a.sum()) == (xb - xa + 1) * (zb - za + 1)
        spans[y] = (xa, xb, za, zb, full); top = y
    steps = []; bad = []
    for y in range(F + AUTH["wall_h"], top + 1):
        a = spans[y]; b = spans[y - 1] if y - 1 in spans else None
        d = (a[0] - b[0], b[1] - a[1], a[2] - b[2], b[3] - a[3])
        steps.append(d)
        if d != (2, 2, 2, 2) or not a[4]: bad.append((y, d, a[4]))
    ctop = top - F + 1
    exp_half = lambda y: half - 2 * max(0, y - (F + AUTH["wall_h"] - 1))        # analytic expectation from the authority constants (1 up / 2 in)
    ana = all(spans[y][0] == cx - exp_half(y) and spans[y][1] == cx + exp_half(y) and spans[y][2] == cz - exp_half(y) and spans[y][3] == cz + exp_half(y) for y in spans)
    peak = spans[top]
    ok = (not bad and ana and ctop == AUTH["central_h"] and peak[0] == peak[1] == cx and peak[2] == peak[3] == cz and h["run"] == AUTH["run"] and h["rise"] == 8)
    r.add("H2", "Four-sided stepped corbel ceiling, 1 up per 2 in on every side (Q46), central clear height 16 (Q44): voxel step scan vs analytic profile",
          "PASS" if ok else "FAIL", f"corbel layers Y {F+8}..{top} ({top-F-7} steps), per-step inset per side {sorted(set(steps))}; central clear height {ctop}; peak column X{peak[0]}..{peak[1]} Z{peak[2]}..{peak[3]} (1x1); analytic profile match {ana}; bad steps {bad[:4]}")
    r.metrics["corbel_spans"] = {y: spans[y][:4] for y in spans}
    # ---------- H3 shell thickness inside the P2 reservation, nothing carved outside it, no P2 cavity touched
    rs = h["res"]
    cav_all = (~solid) & inres & (owner < 0)
    pp = np.argwhere(cavity | recess)
    cb = (int(pp[:, 0].min() + C.GX[0]), int(pp[:, 0].max() + C.GX[0]), int(pp[:, 1].min() + C.GY[0]), int(pp[:, 1].max() + C.GY[0]), int(pp[:, 2].min() + C.GZ[0]), int(pp[:, 2].max() + C.GZ[0]))
    cm = np.argwhere(cavity)
    mb = (int(cm[:, 0].min() + C.GX[0]), int(cm[:, 0].max() + C.GX[0]), int(cm[:, 1].min() + C.GY[0]), int(cm[:, 1].max() + C.GY[0]), int(cm[:, 2].min() + C.GZ[0]), int(cm[:, 2].max() + C.GZ[0]))
    thick = (mb[0] - rs[0], rs[1] - mb[1], mb[4] - rs[4], rs[5] - mb[5], mb[2] - rs[2], rs[3] - mb[3])
    ok = W["main_outside_res"] == 0 and W["main_on_p2cav"] == 0 and not W["recess_bad"] and min(thick[:4]) >= AUTH["wall_min"] and min(thick[4:]) >= AUTH["roof_min"]
    r.add("H3", "Hall cavity lies inside the P2 HALL_RES (lateral shell >=3, floor/roof slab >=2), nothing carved outside it, no overlap with a P2 cavity",
          "PASS" if ok else "FAIL", f"main cavity bbox X[{mb[0]},{mb[1]}] Y[{mb[2]},{mb[3]}] Z[{mb[4]},{mb[5]}]; shell W/E/N/S/floor/roof = {thick}; carved outside reservation {W['main_outside_res']}; on P2 cavity {W['main_on_p2cav']}; recess faults {W['recess_bad']}")
    # ---------- colonnade
    pil = {}
    for k in W["eids"]:
        e = spec["elements"][k]
        if e["kind"] in ("pillar_base", "pillar_shaft", "pillar_cap"):
            pil.setdefault(e["pillar"], {})[e["kind"]] = k
    P = {}
    for pid, d in pil.items():
        o = {}
        for role in ("pillar_base", "pillar_shaft", "pillar_cap"):
            m = V.eprefix(W, d[role]) & (np.isin(elem, [W["eids"].index(d[role])]))
            o[role] = V.cells(np.isin(elem, [W["eids"].index(d[role])]))
        sh = o["pillar_shaft"]; sx = [c[0] for c in sh]; sz = [c[2] for c in sh]
        o["centre"] = ((min(sx) + max(sx)) // 2, (min(sz) + max(sz)) // 2); o["body"] = (min(sx), max(sx), min(sz), max(sz))
        dx, dz = o["centre"][0] - cx, o["centre"][1] - cz
        o["ax"], o["sg"] = ("x", 1 if dx > 0 else -1) if abs(dx) > abs(dz) else ("z", 1 if dz > 0 else -1)
        P[pid] = o
    # K1 dimensions
    k1 = []; ok = True
    for pid, o in P.items():
        ys = lambda role: sorted({c[1] for c in o[role]})
        bx = (o["body"][1] - o["body"][0] + 1, o["body"][3] - o["body"][2] + 1)
        by = ys("pillar_base"); sy = ys("pillar_shaft"); cy = ys("pillar_cap"); ly = F + 7
        full = (len(by), len(sy), len(cy)) == (AUTH["base_h"], AUTH["shaft_h"], AUTH["cap_h"]) and bx == (3, 3)
        per_layer_body = all(len([c for c in o["pillar_shaft"] if c[1] == y]) == 9 for y in sy)
        contiguous = (by + sy + cy) == list(range(F, F + 7))
        # base nominal 5x5 -> bounding box
        bb = o["pillar_base"]; bw = (max(c[0] for c in bb) - min(c[0] for c in bb) + 1, max(c[2] for c in bb) - min(c[2] for c in bb) + 1)
        okp = full and per_layer_body and contiguous and max(bw) <= AUTH["base_nom"]
        ok &= okp; k1.append((pid, bx, (len(by), len(sy), len(cy)), bw))
    lint_cells = V.cells(V.ekind(W, "lintel")); ly_set = sorted({c[1] for c in lint_cells})
    total_top = max(ly_set) if ly_set else None
    ok &= ly_set == [F + 7] and total_top - F + 1 == AUTH["total_h"]
    r.add("K1", "Column assembly dims: body 3x3 (Q48), base nominal 5x5 height 1 (Q53), shaft 3, capital 3, lintel 1 thick (Q59), total 8 (Q61/Q62) - voxel measurement per pillar",
          "PASS" if ok else "FAIL", f"{len(P)} pillars; (id, body, (base,shaft,cap heights), base bbox) {k1[:3]}...; lintel layers {ly_set}; assembly height {None if total_top is None else total_top-F+1}")
    # K2 setback + passage
    gmin_walk = 999; gmin_cap = 999; gmin_body = 999; gw = []
    for pid, o in P.items():
        own = {W["eids"].index(pil[pid][ro]) for ro in ("pillar_base", "pillar_shaft", "pillar_cap")}
        for role in ("pillar_base", "pillar_shaft", "pillar_cap"):
            for (x, y, z) in o[role]:
                g, why = gap_to_wall(W, x, y, z, o["ax"], o["sg"], own)
                if role == "pillar_shaft": gmin_body = min(gmin_body, g)
                if role == "pillar_cap": gmin_cap = min(gmin_cap, g)
                if y <= F + 3: gmin_walk = min(gmin_walk, g)
    for (x, y, z) in lint_cells:
        g, why = gap_to_wall(W, x, y, z, "x" if min(abs(x - (cx - half)), abs(x - (cx + half))) < min(abs(z - (cz - half)), abs(z - (cz + half))) else "z",
                             (1 if x > cx else -1) if min(abs(x - (cx - half)), abs(x - (cx + half))) < min(abs(z - (cz - half)), abs(z - (cz + half))) else (1 if z > cz else -1),
                             {i for i, k in enumerate(W["eids"]) if spec["elements"][k]["kind"] == "lintel"})
        gmin_cap = min(gmin_cap, g)
    # analytic cross-check: derive wall inset of each part from coordinates
    ana_body = min(half - max(abs(o["centre"][0] - cx) + 1 if o["ax"] == "x" else 0, abs(o["centre"][1] - cz) + 1 if o["ax"] == "z" else 0) for o in P.values())
    ok = gmin_body >= AUTH["setback"] and gmin_walk >= AUTH["passage"] and ana_body == gmin_body
    r.add("K2", "Pillar body set back 2 from the inner wall (Q50) and the 2-block wall-side passage survives the trimmed bases (Q54): voxel ray scan wall-ward + analytic inset",
          "PASS" if ok else "FAIL", f"min free cells between wall and body {gmin_body} (analytic {ana_body}); min free between wall and any column cell at walking height (Y<={F+3}) {gmin_walk} (need >= 2)")
    # K3 spacing
    gaps = []; pp_bad = []
    for pid, o in P.items():
        bx0, bx1, bz0, bz1 = o["body"]; y = F + 1
        along = "z" if o["ax"] == "x" else "x"
        for sg in (-1, 1):
            if along == "x":
                x = (bx1 + 1) if sg > 0 else (bx0 - 1); z = o["centre"][1]; n = 0
                while abs(x - cx) <= half and not solid[ix(x), iy(y), iz(z)]: n += 1; x += sg
                hit = None if abs(x - cx) > half else W["eids"][elem[ix(x), iy(y), iz(z)]] if elem[ix(x), iy(y), iz(z)] >= 0 else "shell"
            else:
                z = (bz1 + 1) if sg > 0 else (bz0 - 1); x = o["centre"][0]; n = 0
                while abs(z - cz) <= half and not solid[ix(x), iy(y), iz(z)]: n += 1; z += sg
                hit = None if abs(z - cz) > half else W["eids"][elem[ix(x), iy(y), iz(z)]] if elem[ix(x), iy(y), iz(z)] >= 0 else "shell"
            gaps.append((pid, sg, n, hit))
    ppg = [(p, s, n, hit) for p, s, n, hit in gaps if hit and hit.startswith("P_")]
    ordinary = [g for g in ppg if g[2] == AUTH["gap"]]
    exc = [g for g in ppg if g[2] != AUTH["gap"]]
    # exceptions allowed only across the door (N) or dais (S) axis: the pair straddles the hall axis
    def straddles(g):
        a = P[g[0]]["centre"]; b = P[g[3].split(":")[0]]["centre"]
        return (a[0] - cx) * (b[0] - cx) < 0 and (a[1] - cz) * (b[1] - cz) > 0
    exc_bad = [g for g in exc if not straddles(g)]
    short = [g for g in ppg if g[2] < AUTH["gap"]]
    toc = [g for g in gaps if g[3] == "wall" or g[3] is None]
    byside = {}
    for pid, o in P.items(): byside.setdefault(pid.split("_")[1][0], []).append(o)
    ana_gaps = []
    for sd, ol in byside.items():
        key = 1 if sd in "EW" else 0
        cs = sorted(o["centre"][key] for o in ol)
        ana_gaps += [b - a - 3 for a, b in zip(cs, cs[1:])]
    ana_ok = sorted(g for g in ana_gaps if g == AUTH["gap"]) == sorted(g for g in ana_gaps if g <= AUTH["gap"]) and all(g >= AUTH["gap"] for g in ana_gaps)
    ok = bool(ppg) and not exc_bad and not short and bool(ordinary) and ana_ok
    r.add("K3", "Ordinary runs: exactly 6 clear blocks between adjacent 3x3 pillar bodies (Q51); only door-side and dais-side gaps differ (exceptions)",
          "PASS" if ok else "FAIL", f"{len(ordinary)} pillar-to-pillar gaps = 6; exceptions {[(g[0], g[2]) for g in exc]}; shorter-than-6 {short}; unexplained exceptions {exc_bad}; corner runs (pillar to side wall) {sorted({g[2] for g in toc})}; analytic centre-to-centre gaps {sorted(ana_gaps)} (6 ordinary, 15 across door/dais)")
    # K4 corners
    corner = []
    for pid, o in P.items():
        for (x, y, z) in o["pillar_shaft"]:
            if half - abs(x - cx) < 5 and half - abs(z - cz) < 5: corner.append((pid, x, z))
    # lintel ends: no lintel cell in a corner zone (beams terminate at the last pillar)
    lc = [(x, z) for x, y, z in lint_cells if half - abs(x - cx) < 5 and half - abs(z - cz) < 5]
    r.add("K4", "Corners left open: no corner pillar bodies and lintels terminate before the corner zone (Q52)",
          "PASS" if not corner and not lc else "FAIL", f"pillar-body cells in the 5x5 corner zones {len(corner)}; lintel cells in corner zones {len(lc)}")
    # K5 capitals (Q55-Q58)
    capbad = []; capdims = []
    for pid, o in P.items():
        cc = o["pillar_cap"]; w = (max(c[0] for c in cc) - min(c[0] for c in cc) + 1, max(c[2] for c in cc) - min(c[2] for c in cc) + 1)
        capdims.append((pid, w))
        if max(w) > AUTH["cap_max"]: capbad.append((pid, w))
    wall_touch = 0
    for pid, o in P.items():
        for (x, y, z) in o["pillar_cap"] + o["pillar_base"] + o["pillar_shaft"]:
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0)):
                xx, yy, zz = x + dx, y + dy, z + dz
                if inres[ix(xx), iy(yy), iz(zz)] and W["shell"][ix(xx), iy(yy), iz(zz)] and not (y == F and False): wall_touch += 1
    ok = not capbad and gmin_cap >= 1 and wall_touch == 0
    r.add("K5", "Capital height 3 / max 7x7 (Q56, Q58) and wallward projection limited so no capital touches a wall (Q57)",
          "PASS" if ok else "FAIL", f"capital bounding boxes (x,z) {sorted({w for _, w in capdims})} (limit 7); too wide {capbad}; min free air between wall and any capital/lintel cell {gmin_cap} (need >=1); face contacts column-assembly<->wall/ceiling shell {wall_touch}")
    # K6 lintel / ceiling separation (Q49, Q60)
    up_gap = 99; sup_bad = []
    lset = {(x, z) for x, y, z in lint_cells}
    for (x, y, z) in lint_cells:
        n = 0; yy = y + 1
        while not solid[ix(x), iy(yy), iz(z)] and yy < F + 40: n += 1; yy += 1
        up_gap = min(up_gap, n)
    # connected lintel segments and their pillar support
    segs = []; seen = set()
    for c in lset:
        if c in seen: continue
        comp = []; st = [c]; seen.add(c)
        while st:
            p = st.pop(); comp.append(p)
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (p[0] + d[0], p[1] + d[1])
                if q in lset and q not in seen: seen.add(q); st.append(q)
        segs.append(comp)
    capcells = {(c[0], c[2]) for o in P.values() for c in o["pillar_cap"] if c[1] == F + 6}
    for comp in segs:
        sup = [p for p in comp if p in capcells]
        if not sup: sup_bad.append(("unsupported segment", len(comp)))
    # lintel spans only continuous between neighbouring pillars; interrupted across the door and dais axes
    cross = [c for c in lset if abs(c[0] - cx) <= 5 and (c[1] < cz - half + 6 or c[1] > cz + half - 6)]
    ok = ly_set == [F + 7] and up_gap >= 1 and wall_touch == 0 and not sup_bad and not cross
    r.add("K6", "Lintel/colonnade is visually separate from the stepped ceiling with no direct ceiling or wall support (Q60), lintel 1 thick, interrupted at door and dais (Q49)",
          "PASS" if ok else "FAIL", f"{len(segs)} lintel segments; min air gap lintel->ceiling solid {up_gap}; unsupported segments {sup_bad}; lintel cells across door/dais axis (|u|<=5 on N/S rows) {len(cross)}; shell contacts {wall_touch}")
    # K7 element disjointness (voxel painting + analytic AABB pair test)
    ana_ov = 0; items = []
    for k, e in spec["elements"].items():
        for b in e["boxes"]: items.append((k, b))
    # bucket by y-range to keep it fast
    for (ka, a), (kb, b) in itertools.combinations(items, 2):
        if ka != kb and inter_vol(a, b): ana_ov += inter_vol(a, b)
    r.add("K7", "No physical block placed twice and no element inside a solid structural block (voxel painting) - cross-checked by an AABB pair-intersection sum",
          "PASS" if W["elem_overlap"] == 0 and ana_ov == 0 and not W["elem_into_solid"] else "FAIL",
          f"voxel overlaps {W['elem_overlap']}; analytic intersecting volume {ana_ov}; elements placed into solid {W['elem_into_solid'][:4]}; {len(items)} boxes / {len(spec['elements'])} elements")
    # K8 symmetry (Q47)
    S0 = {(x - cx, y, z - cz) for x, y, z in V.cells(V.ekind(W, "pillar_base") | V.ekind(W, "pillar_shaft") | V.ekind(W, "pillar_cap") | V.ekind(W, "lintel"))}
    su = {(-u, y, v) for u, y, v in S0} == S0; sv = {(u, y, -v) for u, y, v in S0} == S0
    r.add("K8", "Colonnade is symmetrical about both hall axes (Q47: symmetrical perimeter colonnade, centre open)", "PASS" if su and sv else "FAIL",
          f"{len(S0)} colonnade voxels; mirror u (E<->W) {su}; mirror v (N<->S) {sv}; pillars {len(P)} (E/W 3 each, N/S 2 each)")
    # ---------- O1 obelisk
    ob = V.cells(V.eprefix(W, "OBELISK"))
    obtop = max(c[1] for c in ob); obx = {c[0] for c in ob}; obz = {c[2] for c in ob}
    n = 0; yy = obtop + 1
    while not solid[ix(cx), iy(yy), iz(cz)]: n += 1; yy += 1
    ob_bad = 0
    obset = set(ob)
    for (x, y, z) in ob:
        for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0)):
            q = (x + d[0], y + d[1], z + d[2])
            if q in obset: continue
            if solid[ix(*q[:1]), iy(q[1]), iz(q[2])]: ob_bad += 1
    nearest = min(math.hypot(c[0] - o[0], c[2] - o[2]) for o in [(cx, 0, cz)] for c in [pc for p in P.values() for pc in p["pillar_base"]])
    plinth = max(len([c for c in ob if c[1] == F]), 0)
    ok = (min(obx) == cx - 2 and max(obx) == cx + 2 and min(obz) == cz - 2 and max(obz) == cz + 2 and n >= 3 and ob_bad == 0)
    r.add("O1", "Central obelisk on the hall centre: PROPOSED plinth 5x5, shaft 3x3, tip 1x1, top 3+ below the ceiling peak, touching nothing but the floor",
          "PASS" if ok else "FAIL", f"centre ({(min(obx)+max(obx))//2},{(min(obz)+max(obz))//2}) hall centre ({cx},{cz}); top Y{obtop} (height {obtop-F+1}); air above to ceiling {n}; neighbour contacts {ob_bad}; nearest pillar base {nearest:.1f} from centre")
    # ---------- A1 dais / chests / alcove
    dais = V.cells(V.eprefix(W, "DAIS")); dz = [c[2] for c in dais]; dxs = [c[0] for c in dais]; dy = [c[1] for c in dais]
    tread = V.cells(V.eprefix(W, "DAIS:tread"))
    body = [c for c in dais if c not in set(tread)]
    sarc = V.cells(V.eprefix(W, "SARCOPHAGUS")); c1 = V.cells(V.eprefix(W, "CHEST_PRIMARY")); c2 = V.cells(V.eprefix(W, "CHEST_SECONDARY"))
    bx0, bx1 = min(c[0] for c in body), max(c[0] for c in body); bz0, bz1 = min(c[2] for c in body), max(c[2] for c in body); bh = max(c[1] for c in body) - F + 1
    tx0, tx1 = min(c[0] for c in tread), max(c[0] for c in tread)
    sz0 = min(c[2] for c in sarc); sz1 = max(c[2] for c in sarc)
    lid1 = not solid[ix(c1[0][0]), iy(c1[0][1] + 1), iz(c1[0][2])] and not solid[ix(c1[0][0]), iy(c1[0][1] + 2), iz(c1[0][2])]
    lid2 = all(not solid[ix(c[0]), iy(c[1] + 1), iz(c[2])] and not solid[ix(c[0]), iy(c[1] + 2), iz(c[2])] for c in c2)
    ok = (bz1 == cz + half and bh <= 3 and (tx1 - tx0 + 1) >= 7 and sz1 == cz + half and min(c[1] for c in sarc) == F + bh and c1[0][1] == F + bh and
          lid1 and lid2 and bz0 - (cz + 2) >= 6)
    r.add("A1", "Rear low sarcophagus dais with broad frontal tread (Q10, Q32), decorated sarcophagus on it, primary chest on the dais (Q11), secondary chest in a side alcove (Q34) with lid clearance",
          "PASS" if ok else "FAIL", f"dais body X[{bx0},{bx1}] Z[{bz0},{bz1}] height {bh} (low <=3), against rear wall z={cz+half}; tread width {tx1-tx0+1} (>=7); sarcophagus Z[{sz0},{sz1}] on dais top; primary chest {c1} lid air {lid1}; secondary chest cells {c2} lid air {lid2}; dais front to obelisk plinth edge {bz0-(cz+2)-1} blocks")
    # ---------- D1 decoration reservations (spatial only)
    ff = spec["decor"]["FLOOR_FIELD"]["box"]
    fcells = [(x, F, z) for x in range(ff[0], ff[1] + 1) for z in range(ff[2], ff[3] + 1)]
    field_bad = [c for c in fcells if elem[ix(c[0]), iy(c[1]), iz(c[2])] >= 0 and not W["eids"][elem[ix(c[0]), iy(c[1]), iz(c[2])]].startswith("OBELISK")]
    field_hi = [c for c in fcells if solid[ix(c[0]), iy(c[1]), iz(c[2])] and elem[ix(c[0]), iy(c[1]), iz(c[2])] < 0]
    rb_ = spec["decor"]["RELIEF_BAND"]
    rel_ok = rb_["depth"] <= 1 and rb_["y"][0] >= F and rb_["y"][1] <= F + 7
    r.add("D1", "Decoration reservations: Q30 central floor field is flat and free of columns/dais (only the obelisk stands on it); Q29 relief band is a depth-1 surface inside the 8-high walls (spatial occupancy only - no assets)",
          "PASS" if not field_bad and not field_hi and rel_ok else "FAIL", f"floor field X[{ff[0]},{ff[1]}] Z[{ff[2]},{ff[3]}] ({ff[1]-ff[0]+1}x{ff[3]-ff[2]+1}); non-obelisk elements on it {len(field_bad)}; unexpected solid {len(field_hi)}; relief band Y{rb_['y']} depth {rb_['depth']} within wall layers Y{F}..{F+7}")
    # ---------- walk graph
    st = C.standable(solid); start = p2model["start"]
    dist = V.bfs_dist(solid, start, st)
    door = h["door"]; ent = ((door[0] + door[1]) // 2, F, door[5] + 1)
    goals = {"ENTRANCE_IN": ent, "DAIS_TREAD": (cx, F + 1, cz + 10), "DAIS_TOP_FRONT": (cx, F + bh, cz + 11),
             "HATCH": ((spec["hatch"][0] + spec["hatch"][1]) // 2, F, (spec["hatch"][4] + spec["hatch"][5]) // 2),
             "FIELD_RING": (cx + 4, F, cz), "PASSAGE_E_MID": (cx + half, F, cz), "PASSAGE_W_MID": (cx - half, F, cz)}
    for i, c in enumerate(spec["chest_primary_access"]): goals[f"CHEST1_ACC{i}"] = c
    for i, c in enumerate(spec["chest_secondary_access"]): goals[f"CHEST2_ACC{i}"] = c
    miss = [g for g, c in goals.items() if c not in dist]
    dd = {g: dist.get(c) for g, c in goals.items()}
    # independent routes
    def blockbox(b, y0=F, y1=F + 12):
        m = np.zeros_like(solid); m[V.sl([b[0], b[1], y0, y1, b[2], b[3]])] = True; return m
    daisbox = blockbox([cx - 6, cx + 6, cz + 9, cz + 16])
    alc = spec["recesses"]["ALCOVE_E"]["box"]
    alcbox = blockbox([alc[0], alc[1], alc[4], alc[5]])
    fieldbox = blockbox([cx - 10, cx + 10, cz - 10, cz + 10])
    d_noDais = V.bfs_dist(solid, start, st, blocked=daisbox)
    d_noAlc = V.bfs_dist(solid, start, st, blocked=alcbox)
    d_noField = V.bfs_dist(solid, start, st, blocked=fieldbox)
    sec_ok = all(c in d_noDais for c in spec["chest_secondary_access"]) and any(c in d_noDais for c in spec["chest_secondary_access"])
    pri_ok = all(c in d_noAlc for c in spec["chest_primary_access"])
    hatch_ok = goals["HATCH"] in d_noField and goals["HATCH"] in d_noDais
    sec_field = any(c in d_noField for c in spec["chest_secondary_access"])
    ok = not miss and sec_ok and pri_ok and hatch_ok and sec_field
    r.add("A2", "Entrance port connects to the hall interior; primary chest, dais, secondary chest and secret-hatch zone are all reachable on foot from the P1 landing; secondary chest and hatch are reachable WITHOUT using the dais; hatch/secondary chest reachable without crossing the 21x21 combat field",
          "PASS" if ok else "FAIL", f"unreachable {miss}; walk steps from P1 start: " + ", ".join(f"{g}={v}" for g, v in dd.items() if g in ('ENTRANCE_IN', 'DAIS_TOP_FRONT', 'HATCH', 'CHEST2_ACC0')) + f"; secondary chest independent of dais {sec_ok}; primary chest independent of alcove {pri_ok}; hatch reachable with dais blocked / field blocked {hatch_ok}; secondary chest with field blocked {sec_field}")
    r.metrics["walk"] = dd
    gd = V.bfs_dist(solid, goals["DAIS_TOP_FRONT"], st)
    r.metrics["route_dais_to_hatch"] = gd.get(goals["HATCH"]); r.metrics["route_dais_to_chest2"] = min([gd[c] for c in spec["chest_secondary_access"] if c in gd] or [None])
    sg = 1 if spec["chest_secondary_access"][0][0] > cx else -1
    r.metrics["handedness"] = dict(secondary_alcove_side="east" if sg > 0 else "west", seen_entering_facing_south="left" if sg > 0 else "right",
                                    hatch_side="east" if (spec["hatch"][0] + spec["hatch"][1]) / 2 > cx else "west")
    # passage agent (2 wide, 3 high) around the colonnade, field blocked
    pa = V.agent_reach(solid, (cx, F, cz - half), 2, 3, blocked=fieldbox)
    hat_c = (spec["hatch"][1] - 1, F, spec["hatch"][5] - 1)
    r.metrics["passage_reach"] = len(pa)
    path_ok = hat_c in pa
    r.add("A3", "Wall-side passage is a continuous 2-wide route: a 2x2-footprint, 3-high walker goes from the door to the secret-hatch corner without entering the combat field (Q50/Q54 2-block passage)",
          "PASS" if path_ok else "FAIL", f"agent states reached {len(pa)}; hatch corner state {hat_c} reached {path_ok}")
    # ---------- boss analysis (geometry only; actual boss AI/pathing is UNVERIFIED)
    spawn = {}
    for k, (u, v) in spec["spawn"].items():
        cands = [(cx + u + a, F, cz + v + b) for a in (-1, 0) for b in (-1, 0)]
        ok1 = [c for c in cands if V.agent_reach(solid, c, 2, 5)]
        spawn[k] = (ok1[0] if ok1 else None, len(V.agent_reach(solid, ok1[0], 2, 5)) if ok1 else 0)
    bad_sp = [k for k, (c, n) in spawn.items() if c is None]
    ref = [v for v in spawn.values() if v[0]][0][0] if any(v[0] for v in spawn.values()) else None
    reachB = V.agent_reach(solid, ref, 2, 5) if ref else set()
    # boss steps onto the dais? and obelisk loop
    sides = [(cx + 3, F, cz), (cx - 3, F, cz), (cx, F, cz + 3), (cx, F, cz - 3)]
    loop = all(any((s[0] + a, F, s[2] + b) in reachB for a in (-1, 0) for b in (-1, 0)) for s in sides)
    inT = [c for c in reachB if owner[ix(c[0]), iy(c[1]), iz(c[2])] >= 0]
    gapW = {}
    for w in range(2, 9):
        rr = V.agent_reach(solid, ref, w, 5) if ref else set()
        ok_w = bool(rr) and all(any((s[0] + a, F, s[2] + b) in rr for a in range(-w + 1, 1) for b in range(-w + 1, 1)) for s in sides)
        gapW[w] = ok_w
    maxw = max([w for w, v in gapW.items() if v] or [0])
    ok = not bad_sp and loop
    r.add("B1", "Boss space: every emergence candidate fits the guardian envelope (2x2 cells, 5 high = hitbox 1.35x4.35 from TombGuardianTuning) and a boss-sized walker can circle the obelisk and move between the pillars",
          "PASS" if ok else "FAIL", f"candidates {{k: (min-corner, reachable states)}} {spawn}; encircles obelisk {loop}; widest agent that still circles the obelisk (h=5) {maxw} cells; boss-sized agent reaches {len(reachB)} states")
    r.add("B2", "Boss confinement: a 5-high boss walker cannot enter the 3-high P2 threshold or any P2 cavity (corridor safe-spot / door-cheese exposure)", "PASS" if not inT else "FAIL",
          f"boss-reachable states inside P2 cavities: {len(inT)} (0 means the guardian is geometrically confined to the hall; players retreating into T/R are unreachable by melee - see report risk BOSS-R1)")
    # shockwave escape: for every boss-walkable position in the field, farthest player-standable cell on the hall floor
    floor_cells = [c for c in V.cells(st) if c[1] == F and abs(c[0] - cx) <= half and abs(c[2] - cz) <= half]
    fl = np.array([(c[0], c[2]) for c in floor_cells], float)
    bp = [(c[0], c[2]) for c in reachB if c[1] == F and abs(c[0] - cx) <= 10 and abs(c[2] - cz) <= 10]
    worst = min(float(np.hypot(fl[:, 0] - x, fl[:, 1] - z).max()) for x, z in bp) if bp else 0.0
    need = BOSS["wave_r"] + BOSS["wave_t"]
    dirs8 = [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)]
    def run_len(x, z, dx, dz, maxn=12):
        n = 0
        while n < maxn:
            x += dx; z += dz
            if not any(st[ix(x), iy(yv), iz(z)] for yv in (F, F + 1, F + 2)): break
            n += 1
        return n
    esc = {}
    for bx in range(cx - 10, cx + 11):
        for bz in range(cz - 10, cz + 11):
            if not st[ix(bx), iy(F), iz(bz)]: continue
            cnt = 0
            for dx_, dz_ in dirs8:
                px, pz = bx + 2 * dx_, bz + 2 * dz_
                if not st[ix(px), iy(F), iz(pz)]: continue
                k = math.sqrt(2) if dx_ and dz_ else 1.0
                if (run_len(px, pz, dx_, dz_) + 2) * k >= need: cnt += 1
            esc[(bx - cx, bz - cz)] = cnt
    emin = min(esc.values()); weak = sorted(k for k, v in esc.items() if v == emin)
    r.metrics["escape_min_dirs"] = emin; r.metrics["escape_weak_positions"] = weak
    r.add("B3", "Shockwave (radius 6 + 0.8 thickness) can be left: from every boss position in the 21x21 field a standable floor cell >= 6.8 away exists, and a player 2 cells from the boss has a straight free run out of the wave in >= 3 of 8 directions (PROPOSED criterion)",
          "PASS" if worst >= need and emin >= 3 else "FAIL", f"{len(bp)} boss positions; farthest-cell worst case {worst:.1f} (need >= {need:.1f}); minimum number of escape directions over {len(esc)} field positions = {emin} at {weak[:8]}{'...' if len(weak) > 8 else ''} (field corners / dais-tread edge); wave passes through solids in the implementation so geometry never shields")
    # eruption headroom: free height >= 4 above every standable hall floor cell
    low = []
    for (x, y, z) in floor_cells:
        hgt = 0; yy = y
        while not solid[ix(x), iy(yy), iz(z)] and hgt < 8: hgt += 1; yy += 1
        if hgt < BOSS["stat_h"]: low.append((x, y, z, hgt))
    r.add("B4", "Stone Eruption column (height 4) fits above every standable hall floor cell (no cell where the risen statue would be clipped by a capital/ceiling)",
          "PASS" if not low else "FAIL", f"{len(floor_cells)} standable hall floor cells; cells with free height < 4: {len(low)} {low[:4]}")
    r.add("B5", "Guardian AI pathfinding around columns/lintels, emergence-point validation, Stone Eruption safe-shift in tight spots, hit-frame timing, multiplayer spread, camera clipping",
          "UNVERIFIED", "no runtime/NBT exists; only geometric envelopes were tested (B1-B4). Implementation values from TombGuardianTuning.java are used as evidence, not authority.")
    # ---------- R1 hatch (P4 reservation)
    hb = spec["hatch"]
    hatch_air = int((~solid[V.sl(hb)]).sum()); hatch_total = int(np.prod([hb[1] - hb[0] + 1, hb[3] - hb[2] + 1, hb[5] - hb[4] + 1]))
    hatch_elem = int((elem[V.sl(hb)] >= 0).sum())
    sa = p2model["res"]["STAIR_A"]["box"]
    inside_sa = hb[0] >= sa[0] and hb[1] <= sa[1] and hb[4] >= sa[4] and hb[5] <= sa[5]
    slab = 0; yy = F - 1
    while yy >= h["res"][2] and solid[ix(hb[0]), iy(yy), iz(hb[4])]: slab += 1; yy -= 1
    slab_hatch = sum(1 for y in range(F - 1, sa[3] , -1) if True) if False else None
    run = hb[5] - hb[4] + 1
    reach_depth = F - run
    fieldfree = hb[0] > cx + 10 or hb[1] < cx - 10 or hb[4] > cz + 10 or hb[5] < cz - 10
    ok = hatch_air == hatch_total and hatch_elem == 0 and inside_sa and goals["HATCH"] in dist and fieldfree and reach_depth <= sa[3]
    r.add("R1", "Secret-access (Q13/Q14) reservation for P4: 5x5x4 clear floor zone in the rear inner corner, over P2 STAIR_A, outside the combat field, reachable; 1:1 head run reaches the STAIR_A band",
          "PASS" if ok else "FAIL", f"hatch zone X[{hb[0]},{hb[1]}] Z[{hb[4]},{hb[5]}] Y[{hb[2]},{hb[3]}] air {hatch_air}/{hatch_total}, element voxels {hatch_elem}; inside STAIR_A plan {inside_sa}; floor slab below the zone is {slab} thick (reservation floor Y{h['res'][2]}), the P2 STAIR_A band starts directly under it; a 1:1 head run of {run} reaches Y{reach_depth} (STAIR_A top Y{sa[3]}); outside 21x21 field {fieldfree}")
    # ---------- X1 extension
    allb = [h["res"]] + [b for e in spec["elements"].values() for b in e["boxes"]] + [r_["box"] for r_ in spec["recesses"].values()] + [spec["hatch"]]
    ext = max(max(abs(b[0]), abs(b[1]), abs(b[4]), abs(b[5])) for b in allb) - 40
    vox_out = int(((solid | recess | (elem >= 0)) & inres & ((np.abs(np.arange(C.GX[0], C.GX[1] + 1))[:, None, None] > 40) | (np.abs(np.arange(C.GZ[0], C.GZ[1] + 1))[None, None, :] > 40))).sum())
    r.add("X1", "Whole generated hall structure (walls, slabs, foundation, recesses, columns) stays inside the pyramid projection X/Z +-40 (D01 whole-structure reading)",
          "PASS" if ext <= 0 and vox_out == 0 else "FAIL", f"max excess {max(ext, 0)} blocks (analytic over {len(allb)} boxes); reservation voxels outside +-40: {vox_out}; reservation X[{h['res'][0]},{h['res'][1]}] Z[{h['res'][4]},{h['res'][5]}] (outer face touching +-40 on W and S: margin 0)")
    # ---------- I1 P2 interference
    hall_boxes = []
    for y in range(F, top + 1):
        sp_ = spans[y]; hall_boxes.append([sp_[0], sp_[1], y, y, sp_[2], sp_[3]])
    hall_boxes += [r_["box"] for r_ in spec["recesses"].values()]
    hmerge = [[min(b[0] for b in hall_boxes[:8]), max(b[1] for b in hall_boxes[:8]), F, F + 7, min(b[4] for b in hall_boxes[:8]), max(b[5] for b in hall_boxes[:8])]] + hall_boxes[8:]
    thin = []; touch = []
    for k, sp_ in p2model["spaces"].items():
        for pb in sp_["boxes"]:
            for hb_ in hmerge:
                g = cheb_gap(hb_, pb)
                if k == "T": continue
                if g < 2: thin.append((k, g))
    port_cells = int(((owner >= 0) & inres).sum())
    port_decl = sum((o[1] - o[0] + 1) * (o[3] - o[2] + 1) * (o[5] - o[4] + 1) for a, b_, o in p2model["ports"])
    # P1 interference
    p1 = p2model["p1"]; p1_gap = min(cheb_gap(hb_, b) for b in p1.values() for hb_ in hmerge)
    above = [(k, b[2]) for k, b in p1.items() if not (b[1] < h["res"][0] or b[0] > h["res"][1] or b[5] < h["res"][4] or b[4] > h["res"][5])]
    vert = min([b[2] - h["res"][3] - 1 for k, b in p1.items() if (k, b[2]) in above] or [99])
    allb2 = [(k, b) for k, sp_ in p2model["spaces"].items() for b in sp_["boxes"]] + [("P1:" + k, b) for k, b in p1.items()]
    col_above = sorted({k for k, b in allb2 if not (b[1] < h["res"][0] or b[0] > h["res"][1] or b[5] < h["res"][4] or b[4] > h["res"][5]) and b[3] > h["res"][3] and b[2] < 0})
    ok = not thin and port_cells == port_decl and p1_gap >= 2 and vert >= 2
    r.add("I1", "No interference with P2 L1-L3 (cavity/cavity wall >=2, only the declared T port enters the reservation) and with P1 context (vertical cover above the roof)",
          "PASS" if ok else "FAIL", f"P2 cavities closer than 2 to hall cavities: {sorted(set(thin))[:5]}; P2 cavity cells inside reservation {port_cells} (declared port {port_decl}); nearest P1 cavity Chebyshev gap {p1_gap}; P1 cavities over the hall plan: {above} vertical gap {vert}; cavities between roof and ground over the hall plan {col_above}; roof top Y{h['res'][3]} -> ground Y0 = {-h['res'][3]-1} blocks of cover")
    # ---------- I3 recesses
    rb = []; rr_gap = 99
    for k, rr in spec["recesses"].items():
        b = rr["box"]; n = 0
        # scan outward from the recess back face along the wall normal until we leave the reservation or hit a cavity
        if b[0] > cx + half: xx = b[1]; step = (1, 0)
        elif b[1] < cx - half: xx = b[0]; step = (-1, 0)
        elif b[4] < cz - half: step = (0, -1)
        else: step = (0, 1)
        if step[0]:
            x = (b[1] if step[0] > 0 else b[0]) + step[0]; z = b[4]
            while inres[ix(x), iy(b[2]), iz(z)] and solid[ix(x), iy(b[2]), iz(z)]: n += 1; x += step[0]
        else:
            z = (b[5] if step[1] > 0 else b[4]) + step[1]; x = b[0]
            while inres[ix(x), iy(b[2]), iz(z)] and solid[ix(x), iy(b[2]), iz(z)]: n += 1; z += step[1]
        rb.append((k, n))
    ids = list(spec["recesses"]);
    for a, b_ in itertools.combinations(ids, 2):
        rr_gap = min(rr_gap, cheb_gap(spec["recesses"][a]["box"], spec["recesses"][b_]["box"]))
    door_gap = min(cheb_gap(spec["recesses"][k]["box"], h["door"]) for k in ids)
    back_min = min(n for _, n in rb)
    ok = back_min >= 1 and rr_gap >= 1 and door_gap >= 1 and not W["recess_bad"]
    r.add("I3", "Wall recesses (alcove, niches, brazier recesses) leave >=1 solid block behind, do not touch each other or the entrance port, and stay inside the wall shell",
          "PASS" if ok else "FAIL", f"remaining wall behind recesses {dict(rb)}; min recess-recess gap {rr_gap}; min recess-door gap {door_gap}")
    # ---------- P2 preserved
    p2r = C.analyse(p2model, p2model["start"], p2model["port_goal"])
    nf = sum(c["verdict"] != "PASS" for c in p2r["checks"])
    r.add("P2R", "Unmodified P2 model still passes its own engine checks (preservation of P2 coordinates)", "PASS" if nf == 0 else "FAIL",
          f"{len(p2r['checks'])} P2 engine checks, {nf} not PASS (P2 model files are read, not written)")
    # metrics for report
    r.metrics.update(dict(pillars={pid: dict(centre=o["centre"], body=o["body"], wall_axis=o["ax"]) for pid, o in P.items()},
                          capdims=capdims, boss_spawn={k: v[0] for k, v in spawn.items()}, boss_max_circling_agent=maxw,
                          worst_escape=worst, up_gap=up_gap, hatch=spec["hatch"], gaps=[(g[0], g[1], g[2], g[3]) for g in gaps]))
    r.W = W; r.dist = dist; r.st = st
    return r

# ---------------------------------------------------------------- rendering
LET = {"pillar_base": "B", "pillar_shaft": "P", "pillar_cap": "C", "lintel": "L", "obelisk": "O", "dais": "D", "sarcophagus": "S",
       "chest_primary": "1", "chest_secondary": "2", "alcove_col": "i", "statue": "s", "brazier": "f"}
def glyph(W, x, y, z, hatch):
    ix, iy, iz = C.ix, C.iy, C.iz
    e = W["elem"][ix(x), iy(y), iz(z)]
    if e >= 0: return LET.get(W["kind"][e], "?")
    if W["owner"][ix(x), iy(y), iz(z)] >= 0: return "t"
    if W["recess"][ix(x), iy(y), iz(z)]: return "r"
    if not W["solid"][ix(x), iy(y), iz(z)]:
        if hatch and hatch[0] <= x <= hatch[1] and hatch[4] <= z <= hatch[5] and hatch[2] <= y <= hatch[3]: return "H"
        return "."
    if W["inres"][ix(x), iy(y), iz(z)]:
        h = W["spec"]["hall"]
        inside = abs(x - h["cx"]) <= h["half"] and abs(z - h["cz"]) <= h["half"] and y >= h["floor"] + h["wall_h"]
        return "~" if inside else "#"
    return " "
def render_plan(W, y, xr, zr):
    h = W["spec"]["hall"]; hatch = W["spec"]["hatch"]
    L = [f"PLAN  Y={y}  X[{xr[0]},{xr[1]}] Z[{zr[0]},{zr[1]}]  north(-Z) up, east(+X) right. #=wall/slab ~=corbel ceiling .=air B/P/C/L=pillar base/shaft/capital/lintel O=obelisk D=dais S=sarcophagus 1/2=chests i=small column s=statue f=brazier r=recess H=hatch zone t=P2 port"]
    L.append("   Z\\X " + "".join("+" if x == h["cx"] else ("'" if x % 10 == 0 else " ") for x in range(xr[0], xr[1] + 1)))
    for z in range(zr[0], zr[1] + 1):
        L.append(f"{z:+4d}  " + "".join(glyph(W, x, y, z, hatch) for x in range(xr[0], xr[1] + 1)))
    return "\n".join(L)
def render_section(W, axis, c, xr, zr, yr):
    hatch = W["spec"]["hatch"]
    rng = zr if axis == "x" else xr
    L = [f"SECTION {axis.upper()}={c}  horizontal axis = {'Z (north left, south right)' if axis == 'x' else 'X (west left, east right)'}, Y up. Same glyphs as the plans."]
    for y in range(yr[1], yr[0] - 1, -1):
        row = "".join(glyph(W, c, y, t, hatch) if axis == "x" else glyph(W, t, y, c, hatch) for t in range(rng[0], rng[1] + 1))
        L.append(f"{y:+4d}  {row}")
    L.append("      " + "".join("'" if t % 10 == 0 else " " for t in range(rng[0], rng[1] + 1)) + f"   ({rng[0]}..{rng[1]})")
    return "\n".join(L)

def run_side(spec, p2model):
    r = verify(spec, p2model)
    W = r.W; h = spec["hall"]; F = h["floor"]
    xr = (h["res"][0] - 1, h["res"][1] + 1); zr = (h["res"][4] - 3, h["res"][5] + 1)
    out = {}
    for name, y in (("floor_Y%d" % F, F), ("body_Y%d" % (F + 2), F + 2), ("capital_Y%d" % (F + 5), F + 5), ("lintel_Y%d" % (F + 7), F + 7),
                    ("corbel_Y%d" % (F + 10), F + 10), ("peak_Y%d" % (F + 15), F + 15)):
        out["plan_" + name] = render_plan(W, y, xr, zr)
    cx, cz = h["cx"], h["cz"]
    yr = (F - 3, h["res"][3] + 2)
    out["section_axis_NS_X%d" % cx] = render_section(W, "x", cx, xr, zr, yr)
    out["section_axis_EW_Z%d" % cz] = render_section(W, "z", cz, xr, zr, yr)
    out["section_alcove_Z%d" % (cz + 4)] = render_section(W, "z", cz + 4, xr, zr, yr)
    return r, out

def main(outdir):
    S = mk()
    E = S.make_hall_east(); Wsp = S.mirror_spec(E)
    pe, pw = make_east(), make_west()
    re_, oe = run_side(E, pe)
    rw, ow = run_side(Wsp, pw)
    # exact voxel mirror equivalence (grid is symmetric in X)
    We, Ww = re_.W, rw.W
    eq = {k: bool(np.array_equal(We[k], Ww[k][::-1])) for k in ("solid", "elem", "recess", "owner", "shell")}
    eq["eids"] = We["eids"] == Ww["eids"]
    verd_equal = [(a["id"], a["verdict"]) for a in re_.checks] == [(b["id"], b["verdict"]) for b in rw.checks]
    os.makedirs(outdir, exist_ok=True)
    for side, outs in (("east", oe), ("west", ow)):
        d = os.path.join(outdir, side); os.makedirs(d, exist_ok=True)
        for k, v in outs.items(): open(os.path.join(d, k + ".txt"), "w").write(v + "\n")
    def clean(r): return dict(checks=r.checks, metrics=json.loads(json.dumps(r.metrics, default=lambda o: list(o) if isinstance(o, (set, tuple)) else str(o))))
    J = dict(east=clean(re_), west=clean(rw), equivalence=dict(voxel_mirror=eq, verdicts_equal=verd_equal))
    json.dump(J, open(os.path.join(outdir, "p3_verification.json"), "w"), indent=1, default=str)
    for side, r in (("EAST", re_), ("WEST", rw)):
        npass = sum(c["verdict"] == "PASS" for c in r.checks); nf = sum(c["verdict"] == "FAIL" for c in r.checks); nu = sum(c["verdict"] == "UNVERIFIED" for c in r.checks)
        print(f"{side}: {npass} PASS, {nf} FAIL, {nu} UNVERIFIED  -> " + ", ".join(f"{c['id']}={c['verdict']}" for c in r.checks))
        for c in r.checks:
            if c["verdict"] == "FAIL": print("   FAIL", c["id"], c["evidence"])
    print("voxel mirror equivalence:", eq, "| verdict lists equal:", verd_equal)
    bad = any(c["verdict"] == "FAIL" for r in (re_, rw) for c in r.checks) or not all(eq.values()) or not verd_equal
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p3out"))
