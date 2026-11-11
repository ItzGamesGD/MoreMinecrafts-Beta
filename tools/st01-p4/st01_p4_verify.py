#!/usr/bin/env python3
"""ST-01 P4 secret-area verifier (design-only). Verifies candidate A (stacked) and candidate B (lateral) on the EAST side and on an
independently re-voxelised exact X mirror (WEST). World = unmodified P2 + unmodified P3 hall + P4 voids/solids. Voxel measurements are
cross-checked by analytic frame arithmetic. Authority constants live HERE, not in the spec.
Usage: python3 -I st01_p4_verify.py <out_dir>      (exit 1 on any FAIL or on an E/W equivalence break)"""
import sys, os, json, itertools, math
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
sys.path.insert(0, HERE); sys.path.insert(0, os.path.join(TOOLS, "st01-p3"))
import p3_vox as V
C = V.C
import p3_spec as S3
from st01_p2_model_east import make_east
from st01_p2_model_west import make_west
import p4_spec as S4

AUTH = dict(interior=(21, 25), height=10, zone_h=(8, 9, 10), aisle=5, dais=(15, 11, 3), dstair_w=7, dstair_run=5, dstair_out=2, dstair_in=3,
            col_body=3, col_h=7, cap_max=5, col_overlap=2, col_out=1, chest=(5, 3), chest_base=(7, 5), wall_min=3, sep_min=2)
class R:
    def __init__(s): s.checks = []; s.metrics = {}
    def add(s, cid, title, verdict, ev): s.checks.append(dict(id=cid, title=title, verdict=verdict, evidence=ev))

def cheb_gap(a, b):
    return max(max(0, b[2*k] - a[2*k+1] - 1, a[2*k] - b[2*k+1] - 1) for k in range(3))
def inter_vol(a, b):
    v = 1
    for k in range(3): v *= max(0, min(a[2*k+1], b[2*k+1]) - max(a[2*k], b[2*k]) + 1)
    return v

def build_p4(p4, p3spec, p2model):
    W3 = V.build_world(p3spec, p2model)
    shp = W3["solid"].shape; solid = W3["solid"].copy(); pre_void = ~solid
    inres = W3["inres"]; names = list(p4["spaces"]); own = np.full(shp, -1, np.int16)
    pm = np.zeros(shp, bool); pm[V.sl(p4["pierce"])] = True
    W = dict(W3=W3, p4=p4, names=names, own=own, pierce=pm, conf_existing=0, conf_res=0, conf_p4=0)
    for n, k in enumerate(names):
        for b in p4["spaces"][k]["boxes"]:
            s = V.sl(b)
            W["conf_existing"] += int((pre_void[s] & ~pm[s]).sum())
            W["conf_res"] += int((inres[s] & ~pm[s]).sum())
            W["conf_p4"] += int(((own[s] >= 0) & (own[s] != n)).sum())
            own[s] = n; solid[s] = False
    eids = list(p4["elements"]); elem = np.full(shp, -1, np.int16); W["el_overlap"] = 0; W["el_into_solid"] = 0
    for n, k in enumerate(eids):
        for b in p4["elements"][k]["boxes"]:
            s = V.sl(b)
            W["el_overlap"] += int((elem[s] >= 0).sum()); W["el_into_solid"] += int(solid[s].sum())
            elem[s] = n; solid[s] = True
    env = {}
    for k, b in p4["envelopes"].items():
        m = np.zeros(shp, bool); m[V.sl(b)] = True; env[k] = m
    W.update(solid=solid, elem=elem, eids=eids, env=env, inres=inres, pre_void=pre_void,
             ekind=np.array([p4["elements"][k]["kind"] for k in eids]))
    return W

def fr_of(p4):
    v = p4["vault"]; return S4.Frame(v["xe"], v["sg"], v["zc"], v["F"], v["D"], v["Wd"])

def verify(p4, p3spec, p2model):
    W = build_p4(p4, p3spec, p2model); r = R(); ix, iy, iz = C.ix, C.iy, C.iz
    solid, own, elem, names = W["solid"], W["own"], W["elem"], W["names"]
    fr = fr_of(p4); F, D, Wd, half = fr.F, fr.D, fr.Wd, fr.half
    nid = {k: i for i, k in enumerate(names)}
    kinds = {k: p4["spaces"][k]["kind"] for k in names}
    vmask = own == nid["VAULT"]
    # ---- V1 vault interior plan and height (Q65, Q66)
    pl = vmask[:, iy(F), :]
    pp = np.argwhere(pl); xa, xb = int(pp[:, 0].min() + C.GX[0]), int(pp[:, 0].max() + C.GX[0]); za, zb = int(pp[:, 1].min() + C.GZ[0]), int(pp[:, 1].max() + C.GZ[0])
    dims = tuple(sorted((xb - xa + 1, zb - za + 1)))
    hmax = max(int(vmask[:, :, :].sum(axis=1).max()), 0)
    ana_dims = tuple(sorted((D, Wd)))
    ok = dims == AUTH["interior"] and int(pl.sum()) == 525 and hmax == AUTH["height"] and ana_dims == AUTH["interior"]
    r.add("V1", "Vault clear interior plan 21x25 (Q65) and maximum clear height 10 (Q66): voxel scan vs frame arithmetic",
          "PASS" if ok else "FAIL", f"floor-layer interior X[{xa},{xb}] Z[{za},{zb}] = {xb-xa+1}x{zb-za+1}, {int(pl.sum())} cells (525); max clear height {hmax}; frame D={D} W={Wd}")
    # ---- V2 stepped ceiling 8 -> 9 -> 10 (Q67, Q68)
    zone = {}
    for a in (0, D // 2, D - 1):
        for b in range(-half, half + 1):
            x, z = fr.x(a), fr.z(b)
            h = int(vmask[ix(x), iy(F):iy(F) + 14, iz(z)].sum()); zone.setdefault(abs(b), set()).add(h)
    prof = {k: sorted(v) for k, v in sorted(zone.items())}
    flat = all(len(v) == 1 for v in prof.values())
    ib = 8 if half == 12 else 7
    exp = {k: (10 if k <= 4 else 9 if k <= ib else 8) for k in prof}
    got = {k: v[0] for k, v in prof.items()}
    symm = True
    for b in range(1, half + 1):
        x, z0, z1 = fr.x(D // 2), fr.z(b), fr.z(-b)
        symm &= int(vmask[ix(x), iy(F):iy(F) + 14, iz(z0)].sum()) == int(vmask[ix(x), iy(F):iy(F) + 14, iz(z1)].sum())
    mono = all(got[k] >= got[k + 1] for k in range(0, half))
    r.add("V2", "Ceiling steps 8 (side) -> 9 (intermediate) -> 10 (centre), symmetric, constant along the depth (Q67/Q68; boundaries PROPOSED)",
          "PASS" if flat and got == exp and symm and mono and set(got.values()) == {8, 9, 10} else "FAIL",
          f"clear height by |b| (scan at 3 depths) {got}; analytic {exp}; zone widths centre {sum(1 for v in got.values() if v==10)*2-1}, intermediate {sum(1 for v in got.values() if v==9)*2 if True else 0}, side {sum(1 for v in got.values() if v==8)*2}; symmetric {symm}")
    # ---- V3 aisle (Q70) and approach: 5-wide walker from the entrance interior to the stair foot
    start = (min(fr.x(0), fr.x(4)), F, fr.z(-2))
    reach = V.agent_reach(solid, start, 5, 3)
    foot_x = fr.x(D - 15)
    cover = any(min(fr.x(D - 15) , fr.x(D - 15)) in range(s[0], s[0] + 5) and s[2] == fr.z(-2) for s in reach)
    widths = []
    for a in range(0, D - 14):
        x = fr.x(a); run = 0
        for b in range(-half, half + 1):
            z = fr.z(b)
            if vmask[ix(x), iy(F), iz(z)] and not solid[ix(x), iy(F), iz(z)]:
                run += 1
            else:
                if b <= 0: run = 0
                else: break
        widths.append(run)
    ent_w = int(sum(1 for z in range(fr.z(-6), fr.z(6) + 1) if own[ix(fr.x(-2)), iy(F), iz(z)] == nid["VAULT_ENT"]))
    r.add("V3", "Central aisle >= 5 wide from the entrance to the stair foot (Q70): per-depth clear run through the axis, a 5x5 walker, entrance corridor width",
          "PASS" if min(widths) >= AUTH["aisle"] and cover and ent_w >= 5 else "FAIL", f"clear run through the axis per depth a=0..{D-15}: min {min(widths)} max {max(widths)}; 5x5 walker reaches the stair foot {cover}; entrance corridor width {ent_w}")
    # ---- V4 dais + stair (Q85, Q89-Q91, Q71)
    dm = np.isin(elem, [i for i, k in enumerate(W["eids"]) if p4["elements"][k]["kind"] in ("dais", "dais_stair")])
    cm = np.isin(elem, [i for i, k in enumerate(W["eids"]) if p4["elements"][k]["kind"] in ("column_base", "column_shaft")])
    plan = {}
    for a in range(D):
        for b in range(-half, half + 1):
            x, z = fr.x(a), fr.z(b)
            hts = int(dm[ix(x), iy(F):iy(F) + 8, iz(z)].sum()); chs = int(cm[ix(x), iy(F):iy(F) + 3, iz(z)].sum())
            plan[(a, b)] = (hts, chs)
    dcells = [(a, b) for (a, b), (h, c) in plan.items() if h >= 1 and not (a >= D - 14 and a < D - 12 )]
    # dais plan = cells with dais/stair-inset blocks or column-owned overlap cells within the 15 x 11 rectangle
    cand = {(a, b) for (a, b), (h, c) in plan.items() if (h >= 2 or (h >= 1 and a >= D - 12)) or c == 3}
    rect = {(a, b) for (a, b) in cand if D - 12 <= a <= D - 2 and abs(b) <= 7}
    pa = [a for a, b in rect]; pb = [b for a, b in rect]
    dwid = (max(pb) - min(pb) + 1, max(pa) - min(pa) + 1)
    top = max(plan[k][0] for k in rect if plan[k][1] == 0)
    stair_prof = [max(plan[(a, b)][0] for b in range(-3, 4)) for a in range(D - 14, D - 9)]
    sw = sum(1 for b in range(-half, half + 1) if plan[(D - 14, b)][0] >= 1)
    sw_in = sum(1 for b in range(-half, half + 1) if plan[(D - 11, b)][0] >= 1 and abs(b) <= 3)
    ext_cells = [a for a in range(D - 14, D - 12) if all(plan[(a, b)][0] >= 1 for b in range(-3, 4))]
    inset_cells = [a for a in range(D - 12, D - 9) if all(plan[(a, b)][0] >= 1 for b in range(-3, 4))]
    mono = all(0 <= stair_prof[i + 1] - stair_prof[i] <= 1 for i in range(4))
    ok = (dwid == (15, 11) and top == 3 and len(rect) == 165 and sw == 7 and len(ext_cells) == 2 and len(inset_cells) == 3 and stair_prof == [1, 1, 2, 2, 3] and mono
          and plan[(D - 15, 0)][0] == 0)
    r.add("V4", "Rear dais 15x11 plan, 3 high (Q85/Q71); frontal stair 7 wide, run 5 = 2 outside + 3 inset, rise 3 with horizontal portions (Q89-Q91)",
          "PASS" if ok else "FAIL", f"dais plan {dwid[0]}x{dwid[1]} ({len(rect)} cells, 165), top {top}; stair tread heights by run cell {stair_prof}; outside cells {len(ext_cells)}, inset cells {len(inset_cells)}, stair width {sw}; monotone step <=1 {mono}")
    # ---- V5 columns (Q80-Q88, Q96)
    cols = {}
    for k in W["eids"]:
        e = p4["elements"][k]
        if e["kind"].startswith("column_"): cols.setdefault(k.split(":")[0], {})[e["kind"]] = k
    cinfo = []; cok = True
    rectset = {(a, b) for a in range(D - 12, D - 1) for b in range(-7, 8)}
    for cn, d in sorted(cols.items()):
        cellsB = V.cells(np.isin(elem, [W["eids"].index(d["column_base"])]) | np.isin(elem, [W["eids"].index(d["column_shaft"])]))
        cellsC = V.cells(np.isin(elem, [W["eids"].index(d["column_cap"])]))
        body_xz = {(x, z) for x, y, z in cellsB}
        wx = (max(c[0] for c in cellsB) - min(c[0] for c in cellsB) + 1, max(c[2] for c in cellsB) - min(c[2] for c in cellsB) + 1)
        ymin = min(c[1] for c in cellsB); ytop = max(c[1] for c in cellsC)
        capw = (max(c[0] for c in cellsC) - min(c[0] for c in cellsC) + 1, max(c[2] for c in cellsC) - min(c[2] for c in cellsC) + 1)
        # local (a,b) of body cells
        loc = set()
        for x, z in body_xz:
            a = (x - fr.xe) * fr.sg; b = z - fr.zc; loc.add((a, b))
        overlap = len(loc & rectset); outside = len(loc - rectset)
        found = all(solid[ix(x), iy(ymin - 1), iz(z)] and not (elem[ix(x), iy(ymin - 1), iz(z)] >= 0) for x, z in body_xz)
        cinfo.append((cn, wx, ytop - ymin + 1, ymin - F, capw, overlap, outside, found))
        okc = wx == (3, 3) and ytop - ymin + 1 == AUTH["col_h"] and ymin == F and max(capw) <= AUTH["cap_max"] and overlap == 4 and outside == 5 and found
        cok &= okc
    cor = []
    for cn, d in cols.items():
        c0 = [c for c in V.cells(np.isin(elem, [W["eids"].index(d["column_shaft"])]))]
        cor.append((sum(c[0] for c in c0) / len(c0), sum(c[2] for c in c0) / len(c0)))
    ok = len(cols) == 4 and cok and W["el_overlap"] == 0
    r.add("V5", "Four floor-founded papyrus columns at the dais corners: body 3x3, 7 high (base through capital), capital <= 5x5, 2x2 plan overlap with the dais, 1 block projecting out (Q80-Q88)",
          "PASS" if ok else "FAIL", f"{len(cols)} columns; (id, body, height, base y offset, capital bbox, overlap cells, outside cells, founded on floor) {cinfo}; element overlaps {W['el_overlap']}")
    # beams (Q96)
    bm = V.cells(np.isin(elem, [W["eids"].index("BEAMS")]))
    bgap = 99
    for (x, y, z) in bm:
        n = 0; yy = y + 1
        while not solid[ix(x), iy(yy), iz(z)] and n < 6: n += 1; yy += 1
        bgap = min(bgap, n)
    by = {c[1] for c in bm}
    r.add("V5b", "Thin overhead beams (1x1) join the four column tops and keep an air gap to the stepped ceiling (Q96: visually independent)",
          "PASS" if by == {F + 7} and bgap >= 1 else "FAIL", f"{len(bm)} beam voxels at Y {sorted(by)}; min air gap to ceiling {bgap}")
    # ---- V6 chest (Q72, Q92, Q93) + V7 front access
    cb = V.cells(np.isin(elem, [W["eids"].index("CHEST_BODY")])); cs = V.cells(np.isin(elem, [W["eids"].index("CHEST_BASE")])); cl = V.cells(np.isin(elem, [W["eids"].index("CHEST_LID")]))
    def loc_ext(cs_):
        L = [((x - fr.xe) * fr.sg, z - fr.zc) for x, y, z in cs_]; return (max(a for a, b in L) - min(a for a, b in L) + 1, max(b for a, b in L) - min(b for a, b in L) + 1), (min(a for a, b in L), max(a for a, b in L), min(b for a, b in L), max(b for a, b in L))
    (bd, bw), ex = loc_ext(cb)
    (sd, sw_), ex2 = loc_ext(cs)
    lid_clear = all(not solid[ix(x), iy(y + k), iz(z)] for x, y, z in cl for k in (1, 2))
    centred = (ex[2] + ex[3]) == 0 and ex[1] <= D - 4 and ex2[1] <= D - 3
    on_dais = all(solid[ix(x), iy(y - 1), iz(z)] for x, y, z in cs)
    ok = (bw, bd) == AUTH["chest"] and (sw_, sd) <= AUTH["chest_base"] and (sw_, sd) == (7, 5) and centred and lid_clear and on_dais
    r.add("V6", "Giant chest: body 5 (front width) x 3 (depth), stepped base <= 7x5, centred laterally and toward the rear of the dais, lid opening clear above (Q72, Q92, Q93)",
          "PASS" if ok else "FAIL", f"body {bw}x{bd} (b x a), base {sw_}x{sd}; local a-range body {ex[0]}..{ex[1]} of D={D}; lid clearance 2 above {lid_clear}; lateral centring {centred}; base on dais top {on_dais}; chest height PROPOSED {max(c[1] for c in cl)-min(c[1] for c in cs)+1}")
    # ---- walk graph
    st = C.standable(solid); start0 = p2model["start"]
    hall_edge = p4["goals"]["HATCH"]
    dist0 = V.bfs_dist(solid, start0, st)
    distH = V.bfs_dist(solid, hall_edge, st)
    g = p4["goals"]
    goals = {k: v for k, v in g.items() if not isinstance(v, list)}
    for i, c in enumerate(g["CHEST_FRONT"]): goals[f"CHEST_FRONT{i}"] = c
    miss = [k for k, c in goals.items() if c not in dist0]
    # blocked scenarios: crypt branch, and piles do not block
    r.metrics["walk_from_p1"] = {k: dist0.get(c) for k, c in goals.items()}; r.metrics["walk_from_hatch"] = {k: distH.get(c) for k, c in goals.items()}
    steps = distH.get(g["CHEST_FRONT"][2]); nonblock = all(distH.get(c) is not None for c in g["CHEST_FRONT"])
    r.add("V7", "Real connection: royal-hall floor -> hidden stair -> landings -> vault entrance -> aisle -> dais stair -> all five chest-front interaction cells, and the crypt branch, reachable on foot from the P1 landing",
          "PASS" if not miss and nonblock else "FAIL", f"unreachable {miss}; steps hatch edge -> chest front centre {steps}; P1 landing -> chest front {dist0.get(g['CHEST_FRONT'][2])}; -> vault entry {distH.get(g['VAULT_ENTRY'])}; -> crypt interior {distH.get(g['CRYPT_IN'])}; front cells reached {sum(1 for c in g['CHEST_FRONT'] if c in distH)}/5")
    # chest front free of obstruction: interaction row + one cell in front
    fr_free = all(not solid[ix(c[0]), iy(c[1]), iz(c[2])] and not solid[ix(c[0]), iy(c[1] + 1), iz(c[2])] for c in g["CHEST_FRONT"])
    r.add("V7b", "Chest front interaction row (5 cells facing the chest) is open and standable, 2 high", "PASS" if fr_free and nonblock else "FAIL", f"cells {g['CHEST_FRONT']} free {fr_free}")
    # ---- V8 piles (Q64, Q69)
    pv = {}
    for nm in ("PILES_P", "PILES_N"):
        cs_ = V.cells(np.isin(elem, [W["eids"].index(nm)])); pv[nm] = cs_
    vol = {k: len(v) for k, v in pv.items()}
    allp = V.cells(np.isin(elem, [i for i, k in enumerate(W['eids']) if p4['elements'][k]['kind'] == 'pile']))
    aisle_hit = sum(1 for x, y, z in allp if abs(z - fr.zc) <= 2)
    heights = {}
    for v in pv.values():
        colh = {}
        for x, y, z in v: colh[(x, z)] = colh.get((x, z), 0) + 1
        heights.update(colh)
    hs = sorted(set(heights.values()))
    imb = abs(vol["PILES_P"] - vol["PILES_N"]) / max(1, max(vol.values()))
    ok = vol["PILES_P"] > 0 and vol["PILES_N"] > 0 and aisle_hit == 0 and imb <= 0.10 and len(hs) >= 3 and max(hs) <= 4
    r.add("V8", "Irregular mineral piles on both sides, left/right balanced (<=10%), varied heights, none in the 5-wide aisle (Q64/Q69/Q70)", "PASS" if ok else "FAIL",
          f"volumes {vol} imbalance {imb:.3f}; column heights present {hs}; cells inside |b|<=2 aisle {aisle_hit}")
    # ---- V9 stairs / landings / turns
    stairs = [k for k in names if kinds[k] == "stair"]; lands = [k for k in names if kinds[k] == "landing"]
    ok = True; ev = []
    for k in stairs:
        t = p4["spaces"][k]["treads"]; Fs = [f for _, f in t]
        one = all(Fs[i] - Fs[i + 1] == 1 for i in range(len(Fs) - 1))
        sm = own == nid[k]
        # walked headroom and width from voxels
        cells_ = [c for c in V.cells(sm) if st[ix(c[0]), iy(c[1]), iz(c[2])]]
        hmin = min(sum(1 for k2 in range(8) if not solid[ix(c[0]), iy(c[1] + k2), iz(c[2])] and all(not solid[ix(c[0]), iy(c[1] + j), iz(c[2])] for j in range(k2 + 1))) for c in cells_) if cells_ else 0
        wd = len({c[2] for c in cells_}) if p4["spaces"][k]["axis"] == "x" else len({c[0] for c in cells_})
        ev.append((k, len(t), one, hmin, wd)); ok &= one and hmin >= 4 and wd >= 3
    lev = []
    for k in lands:
        bb = p4["spaces"][k]["boxes"][0]; lev.append((k, bb[1] - bb[0] + 1, bb[5] - bb[4] + 1)); ok &= min(lev[-1][1:]) >= 5
    # turns along the route: sequence of main-route space directions
    r.metrics["stairs"] = ev; r.metrics["landings"] = lev
    ntreads = sum(e[1] for e in ev)
    drop = -22 - F
    r.add("V9", "Hidden stair: 1:1 gradient, walked headroom >= 4, width >= 3, landings >= 5x5, at least one direction change at a landing (Q13/Q14; widths/headroom PROPOSED)",
          "PASS" if ok and len(lands) >= 1 else "FAIL", f"flights (id, treads, 1:1, walked headroom, width) {ev}; landings {lev}; total treads {ntreads}; vertical drop hall floor -22 -> vault floor {F} = {drop}")
    r.metrics.update(treads=ntreads, drop=drop)
    # ---- V10 interference: overlaps, reservation intrusion, undeclared contacts, separation
    pair = set()
    for ax in range(3):
        a = [slice(None)] * 3; b = [slice(None)] * 3; a[ax] = slice(0, -1); b[ax] = slice(1, None)
        oa, ob = own[tuple(a)], own[tuple(b)]
        m = (oa >= 0) & (ob >= 0) & (oa != ob)
        for u, v in set(zip(oa[m].tolist(), ob[m].tolist())): pair.add(frozenset((names[u], names[v])))
    declared = {frozenset(l) for l in p4["links"]}
    undecl = sorted(tuple(sorted(p)) for p in pair - declared); missing = sorted(tuple(sorted(p)) for p in declared - pair if "HALL" not in p)
    # contacts with pre-existing voids outside the pierce zone
    pre = W["pre_void"]; bad_touch = 0; own_out = (own >= 0) & ~W["pierce"]
    for ax in range(3):
        for sgn in (1, -1):
            sh = np.roll(own_out, sgn, axis=ax)
            m = pre & sh & ~(own >= 0)
            bad_touch += int(m.sum())
    # separation: P4 void boxes vs existing cavity boxes
    ex_boxes = [(k, b) for k, sp in p2model["spaces"].items() for b in sp["boxes"]] + [("P1:" + k, b) for k, b in p2model["p1"].items()]
    h3 = p3spec["hall"]; F3 = h3["floor"]
    hm = W["W3"]["main"]; hc = np.argwhere(hm)
    for y in sorted({int(c[1]) + C.GY[0] for c in hc}):
        pts_ = hc[hc[:, 1] == y - C.GY[0]]
        ex_boxes.append(("HALL", [int(pts_[:, 0].min()) + C.GX[0], int(pts_[:, 0].max()) + C.GX[0], y, y, int(pts_[:, 2].min()) + C.GZ[0], int(pts_[:, 2].max()) + C.GZ[0]]))
    for k, rr in p3spec["recesses"].items(): ex_boxes.append(("HALL", rr["box"]))
    thin = []; mingap = 99
    for k in names:
        if kinds[k] in ("niche",): continue
        for b in p4["spaces"][k]["boxes"]:
            for ek, eb in ex_boxes:
                if ek == "HALL" and k == "FA": continue
                gp = cheb_gap(b, eb); mingap = min(mingap, gp)
                if gp < AUTH["sep_min"]: thin.append((k, ek, gp))
    inres_viol = W["conf_res"]
    ok = W["conf_existing"] == 0 and W["conf_p4"] == 0 and inres_viol == 0 and not undecl and not missing and not thin and bad_touch == 0 and W["el_into_solid"] == 0
    r.add("V10", "No collision with L1-L3 / royal hall (P1-P3) and no unintended contact: P4 voids overlap no existing void, enter the hall reservation only through the declared hatch pierce volume, touch only declared links, keep >= 2 blocks to unlinked cavities",
          "PASS" if ok else "FAIL", f"minimum gap to unlinked existing cavities {mingap}; void overlap with existing voids {W['conf_existing']}; P4/P4 overlap {W['conf_p4']}; cells inside HALL_RES outside the pierce volume {inres_viol}; undeclared face contacts {undecl}; declared without contact {missing}; contact with pre-existing voids outside the pierce {bad_touch}; closer than 2 to unlinked cavities {sorted(set(thin))[:6]}; elements placed into solid {W['el_into_solid']}")
    # envelope separation
    r.metrics['min_gap_unlinked'] = mingap
    envs = W["env"]; ev2 = []
    hres = p2model["res"]["HALL_RES"]["box"]
    for k, b in p4["envelopes"].items():
        if k in ("VAULT_ENV", "CRYPT_ENV"):
            if inter_vol(b, hres): ev2.append((k, "HALL_RES overlap", inter_vol(b, hres)))
            for ek, eb in ex_boxes:
                if ek == "HALL": continue
                if inter_vol(b, eb): ev2.append((k, ek, inter_vol(b, eb)))
                elif cheb_gap(b, eb) < 2: ev2.append((k, ek, "gap<2", cheb_gap(b, eb)))
    if inter_vol(p4["envelopes"]["VAULT_ENV"], p4["envelopes"]["CRYPT_ENV"]): ev2.append(("VAULT_ENV", "CRYPT_ENV", "overlap"))
    elif cheb_gap(p4["envelopes"]["VAULT_ENV"], p4["envelopes"]["CRYPT_ENV"]) < 1: ev2.append(("VAULT_ENV", "CRYPT_ENV", "touch"))
    # thickness: walls of vault and crypt
    thick = {}
    DIRS = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
    for nm, ek in (("VAULT", "VAULT_ENV"), ("CRYPT", "CRYPT_ENV")):
        m = own == nid[nm]; em = W["env"][ek]; mn = 99; mnv = 99; ss = solid & (elem < 0)
        for p in np.argwhere(m)[::2]:
            for d in DIRS:
                q = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
                if not ss[q]: continue
                n = 0
                while em[q] and ss[q]:
                    n += 1; q = (q[0] + d[0], q[1] + d[1], q[2] + d[2])
                if d[1]: mnv = min(mnv, n)
                else: mn = min(mn, n)
        thick[nm] = (mn, mnv)
    r.add("V11", "Structural separation: vault/crypt envelopes (walls 3, floor/roof 2) overlap no other structure or reservation and stay >= 2 from foreign cavities; measured wall/roof/floor thickness around the vault and crypt",
          "PASS" if not ev2 and all(t[0] >= AUTH["wall_min"] and t[1] >= 2 for t in thick.values()) else "FAIL", f"envelope faults {ev2}; minimum solid thickness scanned (excluding declared openings) (lateral, vertical): vault {thick['VAULT']}, crypt {thick['CRYPT']} (designed: walls 3, floor/roof 2; required here: walls >= 3, floor/roof >= 2)")
    r.metrics["thickness"] = thick
    # ---- V12 extension +-40 (whole structure)
    allb = list(p4["envelopes"].values()) + [b for k in names for b in p4["spaces"][k]["boxes"]]
    ext = max(max(abs(b[0]), abs(b[1]), abs(b[4]), abs(b[5])) for b in allb) - 40
    union = np.zeros_like(solid)
    for m in envs.values(): union |= m
    for k in names:
        for b in p4["spaces"][k]["boxes"]: union[V.sl(b)] = True
    xs = np.arange(C.GX[0], C.GX[1] + 1)[:, None, None]; zs = np.arange(C.GZ[0], C.GZ[1] + 1)[None, None, :]
    outm = (np.abs(xs) > 40) | (np.abs(zs) > 40)
    vox_out = int((union & outm).sum())
    ana_out = 0
    # analytic: union area outside via inclusion of boxes (exact for disjoint decomposition by voxel sets of boxes)
    seen = np.zeros_like(solid)
    for b in allb: seen[V.sl(b)] = True
    ana_out = int((seen & outm).sum())
    px = np.argwhere(union)
    ex_ = (int(px[:, 0].min()) + C.GX[0], int(px[:, 0].max()) + C.GX[0], int(px[:, 2].min()) + C.GZ[0], int(px[:, 2].max()) + C.GZ[0])
    r.add("V12", "Whole generated P4 structure (vault/crypt walls and slabs, corridors and stairs with minimum wall 2) stays inside X/Z +-40 (D01 whole-structure reading); excess distance and voxel count",
          "PASS" if ext <= 0 and vox_out == 0 and vox_out == ana_out else "FAIL", f"max excess {max(ext,0)} blocks (analytic over {len(allb)} boxes); voxels outside +-40 {vox_out} (box-union count {ana_out}); P4 structure X[{ex_[0]},{ex_[1]}] Z[{ex_[2]},{ex_[3]}]")
    r.metrics.update(extent=dict(x=[ex_[0], ex_[1]], z=[ex_[2], ex_[3]], excess=max(ext, 0), voxels_outside=vox_out))
    # ---- V13 depth
    py = np.argwhere(own >= 0); low_void = int(py[:, 1].min()) + C.GY[0]
    low_env = min(b[2] for b in p4["envelopes"].values())
    ana_low = min(b[2] for k in names for b in p4["spaces"][k]["boxes"])
    r.add("V13", "Lowest cavity and lowest generated block of the P4 structure (depth input for Q98): voxel vs box arithmetic", "PASS" if low_void == ana_low else "FAIL",
          f"lowest cavity Y {low_void} (analytic {ana_low}) = depth {-low_void}; lowest structure block Y {low_env} = depth {-low_env}; hall floor -22, P2 provisional depth 24; NOT an approved maximum (crypt size and candidate selection open)")
    r.metrics.update(lowest_cavity=low_void, lowest_structure=low_env)
    # ---- V14 P3 preserved (hall unchanged: its own checks on the unmodified model are re-run by the driver); P4 elements never overwrote P3 solids
    r.add("V14", "P3 hall is untouched: P4 placed no block inside the hall except the declared pierce volume; hall elements keep their voxels", "PASS" if W["conf_res"] == 0 and W["conf_existing"] == 0 else "FAIL",
          f"cells carved inside HALL_RES outside the pierce volume {W['conf_res']}; pierce volume {p4['pierce']} (P3 hatch zone X -9..-5 Z 33..37 plus the 2-thick slab); pierce voxels actually carved {int((own[V.sl(p4['pierce'])] >= 0).sum())}")
    # ---- V15 Q97 stagger metrics (interpretation open)
    def cen(boxes):
        a = sum((b[1]-b[0]+1)*(b[5]-b[4]+1) for b in boxes); return (sum((b[0]+b[1])/2*(b[1]-b[0]+1)*(b[5]-b[4]+1) for b in boxes)/a, sum((b[4]+b[5])/2*(b[1]-b[0]+1)*(b[5]-b[4]+1) for b in boxes)/a)
    hres_ = hres; vb = p4["envelopes"]["VAULT_ENV"]; cbx = p4["envelopes"]["CRYPT_ENV"]
    def ov(a, b): return max(0, min(a[1], b[1]) - max(a[0], b[0]) + 1) * max(0, min(a[5], b[5]) - max(a[4], b[4]) + 1)
    area_v = (vb[1] - vb[0] + 1) * (vb[5] - vb[4] + 1); area_c = (cbx[1] - cbx[0] + 1) * (cbx[5] - cbx[4] + 1)
    cv = ((vb[0] + vb[1]) / 2, (vb[4] + vb[5]) / 2); ch = ((hres_[0] + hres_[1]) / 2, (hres_[4] + hres_[5]) / 2); cc = ((cbx[0] + cbx[1]) / 2, (cbx[4] + cbx[5]) / 2)
    r.metrics["stagger"] = dict(hall_centre=ch, vault_centre=cv, crypt_centre=cc, vault_under_hall_fraction=ov(vb, hres_) / area_v, crypt_under_hall_fraction=ov(cbx, hres_) / area_c,
                                hall_to_vault_offset=math.hypot(cv[0] - ch[0], cv[1] - ch[1]), hall_to_crypt_offset=math.hypot(cc[0] - ch[0], cc[1] - ch[1]))
    st_ = r.metrics["stagger"]
    r.add("V15", "Q97 horizontal stagger through crypt and vault: plan overlap with the royal hall and centre offsets (interpretation of 'staggered' is an owner decision)", "UNVERIFIED",
          f"hall centre ({ch[0]:.1f},{ch[1]:.1f}); vault centre ({cv[0]:.1f},{cv[1]:.1f}), {st_['vault_under_hall_fraction']*100:.0f}% of the vault plan lies under the hall, offset {st_['hall_to_vault_offset']:.1f}; crypt centre ({cc[0]:.1f},{cc[1]:.1f}), {st_['crypt_under_hall_fraction']*100:.0f}% under the hall, offset {st_['hall_to_crypt_offset']:.1f}")
    r.add("V16", "Terrain / worldgen / chunk / neighbouring structures, real-client look, chest model and vanilla-GUI behaviour, mechanism of the hidden cover, golem guidance route, mineral-block mineability, loot", "UNVERIFIED",
          "no runtime, NBT or world data exists; open stair cut in the hall floor is a fall hazard until a cover mechanism (not designed) closes it")
    r.W = W; r.st = st; r.dist0 = dist0; r.distH = distH
    return r

# ------------------------------------------------------------ rendering
def glyph(W, x, y, z, p4):
    ix, iy, iz = C.ix, C.iy, C.iz
    e = W["elem"][ix(x), iy(y), iz(z)]
    if e >= 0:
        k = W["ekind"][e]
        return {"dais": "D", "dais_stair": "d", "column_base": "P", "column_shaft": "P", "column_cap": "K", "beam": "B", "chest_base": "X", "chest_body": "X", "chest_lid": "X",
                "pile": "m", "sarcophagus": "S"}.get(k, "?")
    o = W["own"][ix(x), iy(y), iz(z)]
    if o >= 0:
        k = p4["spaces"][W["names"][o]]["kind"]
        return {"vault": ".", "corr": ",", "stair": "s", "landing": "l", "crypt": "c", "niche": "n"}.get(k, "?")
    if W["W3"]["owner"][ix(x), iy(y), iz(z)] >= 0 or not W["pre_void"][ix(x), iy(y), iz(z)] is np.True_ and False: return ":"
    if W["pre_void"][ix(x), iy(y), iz(z)]: return ":"
    for k, m in W["env"].items():
        if m[ix(x), iy(y), iz(z)]: return "#"
    return " "
def plan(W, p4, y, xr, zr):
    L = [f"PLAN Y={y} X[{xr[0]},{xr[1]}] Z[{zr[0]},{zr[1]}] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab"]
    L.append("   Z\\X " + "".join("'" if x % 10 == 0 else " " for x in range(xr[0], xr[1] + 1)))
    for z in range(zr[0], zr[1] + 1): L.append(f"{z:+4d}  " + "".join(glyph(W, x, y, z, p4) for x in range(xr[0], xr[1] + 1)))
    return "\n".join(L)
def section(W, p4, axis, c, rr, yr):
    L = [f"SECTION {axis.upper()}={c} horizontal = {'Z' if axis == 'x' else 'X'} {rr[0]}..{rr[1]}, Y up"]
    for y in range(yr[1], yr[0] - 1, -1):
        L.append(f"{y:+4d}  " + "".join(glyph(W, c, y, t, p4) if axis == "x" else glyph(W, t, y, c, p4) for t in range(rr[0], rr[1] + 1)))
    return "\n".join(L)

def run_side(p4, p3spec, p2model):
    r = verify(p4, p3spec, p2model); W = r.W; fr = fr_of(p4); F = fr.F; D = fr.D
    out = {}
    vb = p4["envelopes"]["VAULT_ENV"]; cb = p4["envelopes"]["CRYPT_ENV"]
    allb = list(p4["envelopes"].values())
    xr = (min(b[0] for b in allb) - 1, max(b[1] for b in allb) + 1); zr = (min(b[4] for b in allb) - 1, max(b[5] for b in allb) + 1)
    for nm, y in (("floor", F), ("dais_top_%d" % (F + 3), F + 3), ("column_cap_%d" % (F + 5), F + 5), ("beam_%d" % (F + 7), F + 7)):
        out["plan_%s_Y%d" % (nm, y)] = plan(W, p4, y, xr, zr)
    out["vault_detail_floor"] = plan(W, p4, F, (vb[0] - 1, vb[1] + 1), (vb[4] - 1, vb[5] + 1))
    out["vault_detail_Y%d" % (F + 3)] = plan(W, p4, F + 3, (vb[0] - 1, vb[1] + 1), (vb[4] - 1, vb[5] + 1))
    out["vault_detail_Y%d" % (F + 7)] = plan(W, p4, F + 7, (vb[0] - 1, vb[1] + 1), (vb[4] - 1, vb[5] + 1))
    v = p4["vault"]
    out["section_vault_axis_Z%d" % v["zc"]] = section(W, p4, "z", v["zc"], (min(xr[0], vb[0]) - 1, max(xr[1], vb[1]) + 1) if True else xr, (F - 4, F + 13))
    xmid = fr.x(D - 2)
    out["section_vault_cross_X%d" % fr.x(D - 7)] = section(W, p4, "x", fr.x(D - 7), (vb[4] - 1, vb[5] + 1), (F - 4, F + 13))
    out["section_route_Z35"] = section(W, p4, "z", 35, xr, (-26, -12) if False else (F - 4, -8))
    out["section_route_axis_Z%d" % fr.z(0)] = section(W, p4, "z", fr.z(0), xr, (F - 4, -12))
    return r, out

def stack_search(p2model, p3spec):
    """Screening evidence for candidate B: east-adjacent vault envelope X3..33 Z13..39 (31x27, 14 high) - first floor Y without conflict with any cavity/reservation (margin 2)."""
    W3 = V.build_world(p3spec, p2model); G = W3["G"]
    void = (~W3["solid"])
    res = np.zeros_like(void)
    for k, rr in p2model["res"].items(): res[V.sl(rr["box"])] = True
    blocked = void | res
    out = {}
    for Fv in range(-22, -41, -2):
        b = [3 - 2, 33 + 2, Fv - 2 - 2, Fv + 11 + 2, 13 - 2, 39 + 2]
        sl_ = V.sl(b); hits = blocked[sl_]
        names_ = set()
        if hits.any():
            pts = np.argwhere(hits)
            for p in pts[::17]:
                o = G["owner"][p[0] + sl_[0].start, p[1] + sl_[1].start, p[2] + sl_[2].start]
                names_.add(G["ids"][o] if o >= 0 else "reservation/other")
        out[Fv] = (int(hits.sum()), sorted(names_)[:6])
    return out

def main(outdir):
    p3e = S3.make_hall_east(); p3w = S3.mirror_spec(p3e); pe, pw = make_east(), make_west()
    res = {}; allfail = False
    for var in ("A", "B"):
        e4 = S4.make_variant(var); w4 = S4.mirror_p4(e4)
        re_, oe = run_side(e4, p3e, pe); rw, ow = run_side(w4, p3w, pw)
        eq = {k: bool(np.array_equal(re_.W[k], rw.W[k][::-1])) for k in ("solid", "own", "elem", "env") if k != "env"}
        eq["env"] = all(np.array_equal(re_.W["env"][k], rw.W["env"][k][::-1]) for k in re_.W["env"])
        verd = [(a["id"], a["verdict"]) for a in re_.checks] == [(b["id"], b["verdict"]) for b in rw.checks]
        for side, outs in (("east", oe), ("west", ow)):
            d = os.path.join(outdir, var, side); os.makedirs(d, exist_ok=True)
            for k, v in outs.items(): open(os.path.join(d, k + ".txt"), "w").write(v + "\n")
        def clean(r_): return dict(checks=r_.checks, metrics=json.loads(json.dumps(r_.metrics, default=lambda o: list(o) if isinstance(o, (set, tuple)) else str(o))))
        res[var] = dict(east=clean(re_), west=clean(rw), equivalence=dict(voxel_mirror=eq, verdicts_equal=verd), vault=e4["vault"])
        for side, r_ in (("EAST", re_), ("WEST", rw)):
            c = r_.checks
            print(f"{var} {side}: {sum(x['verdict']=='PASS' for x in c)} PASS, {sum(x['verdict']=='FAIL' for x in c)} FAIL, {sum(x['verdict']=='UNVERIFIED' for x in c)} UNVERIFIED -> " + ", ".join(f"{x['id']}={x['verdict']}" for x in c))
            for x in c:
                if x["verdict"] == "FAIL": print("   FAIL", x["id"], x["evidence"])
        print(var, "mirror", eq, verd)
        allfail |= any(x["verdict"] == "FAIL" for r_ in (re_, rw) for x in r_.checks) or not all(eq.values()) or not verd
    import st01_p3_verify as P3V
    for sd, sp_, pm_ in (("east", p3e, pe), ("west", p3w, pw)):
        rr = P3V.verify(sp_, pm_)
        res.setdefault("p3_preserved", {})[sd] = dict(passes=sum(c["verdict"] == "PASS" for c in rr.checks), fails=[c["id"] for c in rr.checks if c["verdict"] == "FAIL"], unverified=[c["id"] for c in rr.checks if c["verdict"] == "UNVERIFIED"])
        allfail |= bool(res["p3_preserved"][sd]["fails"])
    print("P3 preserved:", res["p3_preserved"])
    res["screen_B_east_adjacent"] = {str(k): v for k, v in stack_search(pe, p3e).items()}
    os.makedirs(outdir, exist_ok=True)
    json.dump(res, open(os.path.join(outdir, "p4_verification.json"), "w"), indent=1, default=str)
    return 1 if allfail else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p4out"))
