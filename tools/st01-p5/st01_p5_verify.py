#!/usr/bin/env python3
"""ST-01 P5 integrated verifier (design-only). Combines the UNMODIFIED P1 context, P2 L1-L3, P3 royal hall and a P4 candidate (B primary, A comparison)
into ONE voxel world per (candidate, P2 layout, side, cover state) and re-verifies it; stage verdicts are NOT summed.
Independent analytic (box arithmetic) cross-checks accompany the voxel scans. Authority constants live here.
Usage: python3 -I st01_p5_verify.py <out_dir>   (exit 1 on any FAIL except the documented P2-F1 X1 finding of the unmodified P2 layout)"""
import sys, os, json, math, itertools
from collections import deque
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
for d in ("st01-p5", "st01-p4", "st01-p3", "st01-p2"): sys.path.insert(0, os.path.join(TOOLS, d))
import p3_vox as V
C = V.C
import p3_spec as S3, p4_spec as S4, st01_p4_verify as P4V
import p2f1_variants as PV
from st01_p2_model_east import make_east, mirror_model

class R:
    def __init__(s): s.checks = []; s.metrics = {}
    def add(s, cid, title, verdict, ev): s.checks.append(dict(id=cid, title=title, verdict=verdict, evidence=ev))

def cheb_gap(a, b): return max(max(0, b[2*k] - a[2*k+1] - 1, a[2*k] - b[2*k+1] - 1) for k in range(3))
def inter_vol(a, b):
    v = 1
    for k in range(3): v *= max(0, min(a[2*k+1], b[2*k+1]) - max(a[2*k], b[2*k]) + 1)
    return v

# P1 descent stair (P1 report table: tread k at z=-k, feet -k, k=1..5, x -5..5; solid below feet). The P2 grid only carries coarse P1 boxes.
def add_p1_treads(solid):
    for k in range(1, 6):
        solid[V.sl([-5, 5, -6, -k - 1, -k, -k])] = True

def bfs_path(solid, start, st, goal, blocked=None):
    ix, iy, iz = C.ix, C.iy, C.iz
    par = {start: None}; dq = deque([start])
    while dq:
        cur = dq.popleft()
        if cur == goal: break
        x, y, z = cur
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, zz = x + dx, z + dz
            for dy in (0, 1, -1):
                yy = y + dy
                if not st[ix(xx), iy(yy), iz(zz)]: continue
                if dy == 1 and solid[ix(x), iy(y + 2), iz(z)]: continue
                if blocked is not None and blocked[ix(xx), iy(yy), iz(zz)]: continue
                n = (xx, yy, zz)
                if n not in par: par[n] = cur; dq.append(n)
                break
    if goal not in par: return None
    p = []; c = goal
    while c is not None: p.append(c); c = par[c]
    return p[::-1]

def verify(p4, p3spec, p2model, label):
    r = R(); ix, iy, iz = C.ix, C.iy, C.iz
    W = P4V.build_p4(p4, p3spec, p2model)
    W3 = W["W3"]; names = W["names"]; own = W["own"]; elem = W["elem"]; inres = W["inres"]
    open_solid = W["solid"].copy(); add_p1_treads(open_solid)
    F3 = p3spec["hall"]["floor"]
    cover = (own >= 0) & inres & (np.arange(C.GY[0], C.GY[1] + 1)[None, :, None] == F3 - 1)
    closed_solid = open_solid.copy(); closed_solid[cover] = True
    p3_solid = W3["solid"].copy(); add_p1_treads(p3_solid)
    ncov = int(cover.sum())
    # ---- K1 closed hall identical to P3 above the slab top
    y_lo = iy(F3 - 1)
    reg = np.zeros_like(inres); reg[:, y_lo:, :] = inres[:, y_lo:, :]
    diff_closed = int((closed_solid[reg] != p3_solid[reg]).sum())
    hid = int(((own >= 0) & inres & (np.arange(C.GY[0], C.GY[1] + 1)[None, :, None] < F3 - 1)).sum())
    r.add("K1", "Closed state: the royal hall (all cells of the P3 reservation at floor-top level and above) is voxel-identical to the P3 model - safe walking surface, no hole, no change to columns/dais/obelisk/arena",
          "PASS" if diff_closed == 0 else "FAIL", f"differing cells {diff_closed}; cover cells placed {ncov}; hidden cavity cells left below the cover (inside the 2-thick slab zone, y<{F3-1}) {hid}")
    # ---- K4 open delta equals the analytic cut
    diff_open = (open_solid != p3_solid) & reg
    exp = np.zeros_like(inres)
    for k in names:
        if p4["spaces"][k]["kind"] == "stair":
            for b in p4["spaces"][k]["boxes"]:
                bb = [b[0], b[1], max(b[2], F3 - 1), b[3], b[4], b[5]]
                if bb[2] <= bb[3]: exp[V.sl(bb)] = True
    exp &= inres & p3_solid & reg
    pcs = {tuple(c) for c in np.argwhere(diff_open)}; ecs = {tuple(c) for c in np.argwhere(exp)}
    in_pierce = bool(W["pierce"][diff_open].all()) if diff_open.any() else True
    r.add("K4", "Open state: the only change to the hall at/above the floor-top level is the stair cut implied by the P4 stair boxes (voxel diff == analytic box intersection), inside the declared pierce volume, no extra holes",
          "PASS" if pcs == ecs and in_pierce else "FAIL", f"changed cells {len(pcs)} (analytic {len(ecs)}) all inside pierce {in_pierce}; cut footprint at floor-top layer {int(cover.sum())} cells")
    # ---- walk graphs
    p1_start = (0, 0, 36)
    st_c = C.standable(closed_solid); st_o = C.standable(open_solid)
    d_c = V.bfs_dist(closed_solid, p1_start, st_c); d_o = V.bfs_dist(open_solid, p1_start, st_o)
    g = p4["goals"]
    hall_goals = {"HALL_ENTRY": (p3spec["hall"]["door"][0] + 1, F3, p3spec["hall"]["door"][5] + 1), "P2_START": p2model["start"]}
    dais_top = (p3spec["hall"]["cx"], F3 + 2, p3spec["hall"]["cz"] + 11)
    hall_goals["DAIS_TOP"] = dais_top
    for k, c in p3spec["spawn"].items(): pass
    p4cells = own >= 0
    reach_c = np.zeros_like(p4cells)
    for (x, y, z) in d_c: reach_c[ix(x), iy(y), iz(z)] = True
    leak = int((reach_c & p4cells & (np.arange(C.GY[0], C.GY[1] + 1)[None, :, None] < F3)).sum())
    miss = [k for k, c in hall_goals.items() if c not in d_c]
    r.add("K2", "Closed state: forecourt -> P1 hall -> P1 descent -> P2 L1-L3 -> royal hall (entrance, dais) all walkable; no walkable cell of the secret area is reachable (player cannot fall into / enter the stair)",
          "PASS" if not miss and leak == 0 else "FAIL", f"unreachable hall goals {miss}; secret-area walkable cells reachable below the hall floor {leak}; steps forecourt->hall entry {d_c.get(hall_goals['HALL_ENTRY'])}, ->dais top {d_c.get(dais_top)}; total walkable states {len(d_c)}")
    goals = {k: v for k, v in g.items() if not isinstance(v, list)}
    for i, c in enumerate(g["CHEST_FRONT"]): goals[f"CHEST_FRONT{i}"] = c
    missO = [k for k, c in goals.items() if c not in d_o]
    # head room over the first tread and path
    path = bfs_path(open_solid, p1_start, st_o, g["CHEST_FRONT"][2])
    r.add("K3", "Open state: complete chain forecourt -> ... -> royal hall -> hidden stair -> landing(s) -> vault entrance -> aisle -> dais stair -> chest front (5 cells) and the crypt are reachable on foot",
          "PASS" if not missO and path else "FAIL", f"unreachable {missO}; chain length forecourt->chest front {len(path)-1 if path else None} steps; goals checked {len(goals)}")
    r.metrics["chain_steps"] = len(path) - 1 if path else None
    # ---- S1 stair extraction (independent of the P4 verifier): walk the shortest route hall floor -> vault floor and record every height
    hatch = g["HATCH"]; vf = g["VAULT_ENTRY"]
    pp = bfs_path(open_solid, hatch, st_o, vf)
    seq = []
    own_names = lambda c: names[own[ix(c[0]), iy(c[1]), iz(c[2])]] if own[ix(c[0]), iy(c[1]), iz(c[2])] >= 0 else "hall/floor"
    if pp:
        for c in pp:
            if not seq or seq[-1][1] != c[1] or seq[-1][3] != own_names(c): seq.append([c[0], c[1], c[2], own_names(c)])
    ys = [c[1] for c in pp] if pp else []
    dys = [ys[i + 1] - ys[i] for i in range(len(ys) - 1)]
    drops = sum(1 for d in dys if d < 0); rises = sum(1 for d in dys if d > 0)
    steps_by_space = {}
    for i, d in enumerate(dys):
        if d != 0: steps_by_space.setdefault(own_names(pp[i + 1]), []).append(ys[i + 1])
    treads = sum(len(p4["spaces"][k]["treads"]) for k in names if p4["spaces"][k]["kind"] == "stair")
    hmin = 99
    for c in (pp or []):
        h = 0
        while not open_solid[ix(c[0]), iy(c[1] + h), iz(c[2])] and h < 8: h += 1
        hmin = min(hmin, h)
    stair_cells = [c for c in (pp or []) if own[ix(c[0]), iy(c[1]), iz(c[2])] >= 0 and p4["spaces"][own_names(c)]["kind"] == "stair"]
    shmin = min([sum(1 for k in range(8) if not any(open_solid[ix(c[0]), iy(c[1] + j), iz(c[2])] for j in range(k + 1))) for c in stair_cells] or [0])
    total_drop = ys[0] - ys[-1] if ys else None
    levels = [ys[0]] + [ys[i + 1] for i, d in enumerate(dys) if d != 0] if ys else []
    ok = bool(pp) and set(dys) <= {-1, 0, 1} and rises == 0 and drops == total_drop and shmin >= 4 and levels == list(range(levels[0], levels[-1] - 1, -1))
    r.add("S1", "Independent stair drop check: shortest walk hall floor -> vault entrance has only unit steps, no rise, no missing first/last step, consecutive heights, headroom >= 4 on stair cells; drop = number of unit descents",
          "PASS" if ok else "FAIL", f"start Y {ys[0] if ys else None} -> end Y {ys[-1] if ys else None}; total drop {total_drop}; unit descents {drops}, rises {rises}, max |dy| {max([abs(d) for d in dys] or [0])}; heights visited {levels}; stair-space treads (P4 definition) {treads}; descents per space { {k: len(v) for k, v in steps_by_space.items()} }; min headroom on stair cells {shmin}, along whole path {hmin}")
    r.metrics["stair"] = dict(levels=levels, drop=total_drop, treads=treads, descents=drops, per_space={k: v for k, v in steps_by_space.items()}, path_len=len(pp) - 1 if pp else None)
    # ---- I1 integrated overlaps / contacts: stage masks rebuilt from boxes (independent of build_p4 bookkeeping)
    shp = open_solid.shape
    m1 = np.zeros(shp, bool); m2 = np.zeros(shp, bool); m3 = np.zeros(shp, bool); m4 = np.zeros(shp, bool)
    for b in p2model["p1"].values(): m1[V.sl(b)] = True
    for s in p2model["spaces"].values():
        for b in s["boxes"]: m2[V.sl(b)] = True
    m3 = (W3["main"] & inres & ~(W3["owner"] >= 0)) | W3["recess"]
    for k in names:
        for b in p4["spaces"][k]["boxes"]: m4[V.sl(b)] = True
    M = {1: m1, 2: m2, 3: m3, 4: m4}
    ov = {}; ct = {}
    for i, j in itertools.combinations(M, 2):
        ov[(i, j)] = int((M[i] & M[j]).sum())
        c_ = 0
        for ax in range(3):
            for sgn in (1, -1):
                c_ += int((M[i] & np.roll(M[j], sgn, axis=ax) & ~M[j]).sum())
        ct[(i, j)] = c_
    allowed_overlap = {(3, 4): int((m3 & m4).sum())}
    exp34 = int(((m3 & m4) & ~W["pierce"]).sum())
    # P2 start cell sits on P1 S06 floor; P1 descent S06 <-> C1 and T <-> hall are the declared inter-stage links
    bad_pairs = [k for k, v in ov.items() if v and k != (3, 4)]
    unexpected_ct = [(k, v) for k, v in ct.items() if v and k not in ((1, 2), (2, 3), (3, 4))]
    ok = not bad_pairs and exp34 == 0 and not unexpected_ct and W["conf_existing"] == 0 and W["conf_p4"] == 0 and W["conf_res"] == 0
    r.add("I1", "Whole-structure collision/contact scan on one combined voxel world (P1 context, P2, P3 hall cavity, P4): no overlap except the declared hatch pierce, face contacts only at the declared links (P1<->P2 landing, P2 port<->hall, hall<->stair cut)",
          "PASS" if ok else "FAIL", f"overlap voxels per stage pair {{{', '.join(f'{k}:{v}' for k, v in ov.items())}}}; pierce overlap (3,4) {allowed_overlap[(3, 4)]}, outside the pierce {exp34}; face-contact voxels per pair {{{', '.join(f'{k}:{v}' for k, v in ct.items())}}}; unexpected contact pairs {unexpected_ct}; P4 internal overlaps {W['conf_p4']}; carved inside HALL_RES outside pierce {W['conf_res']}")
    p2r = C.analyse(p2model, p2model["start"], p2model["port_goal"])
    nf2 = [c["id"] for c in p2r["checks"] if c["verdict"] != "PASS"]
    r.add("P2R", "Re-run of the P2 engine on the P2 layout used in this integrated world (intra-P2 collisions, undeclared contacts, widths, reachability)", "PASS" if not nf2 and not p2r["G"]["overlaps"] else "FAIL", f"{len(p2r['checks'])} P2 engine checks, not PASS: {nf2}; P2 cavity overlaps {len(p2r['G']['overlaps'])}")
    # ---- I2 analytic separation between stages
    boxes = {1: list(p2model["p1"].values()), 2: [b for s in p2model["spaces"].values() for b in s["boxes"]], 4: [b for k in names for b in p4["spaces"][k]["boxes"] if p4["spaces"][k]["kind"] != "niche"]}
    hm = W3["main"]; hc = np.argwhere(hm & inres); b3 = []
    for y in sorted({int(c[1]) for c in hc}):
        pts = hc[hc[:, 1] == y]; b3.append([int(pts[:, 0].min()) + C.GX[0], int(pts[:, 0].max()) + C.GX[0], y + C.GY[0], y + C.GY[0], int(pts[:, 2].min()) + C.GZ[0], int(pts[:, 2].max()) + C.GZ[0]])
    b3 += [rr["box"] for rr in p3spec["recesses"].values()]; boxes[3] = b3
    gaps = {}
    exempt = {(1, 2), (2, 3), (3, 4)}
    for i, j in itertools.combinations(sorted(boxes), 2):
        mg = min(cheb_gap(a, b) for a in boxes[i] for b in boxes[j])
        gaps[(i, j)] = mg
    # linked pairs are allowed to touch; unlinked pairs need >= 2
    bad = {k: v for k, v in gaps.items() if v < 2 and k not in exempt}
    # (3,4): the stair FA may touch the hall only in the pierce; other P4 boxes need >= 2
    min34 = min(cheb_gap(a, b) for k in names if k != "FA" and p4["spaces"][k]["kind"] != "niche" for a in p4["spaces"][k]["boxes"] for b in boxes[3])
    r.add("I2", "Analytic (box arithmetic) minimum wall between cavities of different stages is >= 2 except at the declared links; P4 spaces other than the stair cut keep >= 2 to the hall",
          "PASS" if not bad and min34 >= 2 else "FAIL", f"stage-pair minimum Chebyshev gaps {{{', '.join(f'{k}:{v}' for k, v in gaps.items())}}}; hall<->(P4 except FA) {min34}; violations {bad}")
    # ---- X1 extension +-40 with walls
    ext_boxes = []
    for s in p2model["spaces"].values():
        for b in s["boxes"]: ext_boxes.append([b[0] - 2, b[1] + 2, b[2], b[3], b[4] - 2, b[5] + 2])
    ext_boxes.append(list(p3spec["hall"]["res"])); ext_boxes += [list(b) for b in p4["envelopes"].values()]
    ex = max(max(abs(b[0]), abs(b[1]), abs(b[4]), abs(b[5])) for b in ext_boxes) - 40
    offenders = sorted({k for k, s in p2model["spaces"].items() for b in s["boxes"] if max(abs(b[0]) + 2, abs(b[1]) + 2, abs(b[4]) + 2, abs(b[5]) + 2) > 40})
    cnt = 0; seen = np.zeros(shp, bool)
    for b in ext_boxes: seen[V.sl(b)] = True
    xs = np.arange(C.GX[0], C.GX[1] + 1)[:, None, None]; zs = np.arange(C.GZ[0], C.GZ[1] + 1)[None, None, :]
    cnt = int((seen & ((np.abs(xs) > 40) | (np.abs(zs) > 40))).sum())
    r.add("X1", "Whole generated underground structure (P2 cavities + wall 2, P3 reservation, P4 vault/crypt/corridor structures) stays inside X/Z +-40 (D01 whole-structure reading)",
          "PASS" if ex <= 0 and cnt == 0 else "FAIL", f"max excess {max(ex, 0)} blocks; voxels outside {cnt}; P2 spaces whose wall-inclusive extent exceeds 40: {offenders}")
    r.metrics["excess"] = max(ex, 0); r.metrics["voxels_outside"] = cnt
    # ---- D1 depth, independent
    low_box = min(b[2] for b in ext_boxes); low_cav = min(b[2] for bs in boxes.values() for b in bs)
    pyv = np.argwhere(own >= 0); lowv = int(pyv[:, 1].min()) + C.GY[0]
    r.add("D1", "Lowest cavity and lowest structure block of the whole integrated structure recomputed from boxes and from voxels (Q98 input; not an approved maximum)",
          "PASS" if lowv == min(b[2] for b in boxes[4]) else "FAIL", f"lowest P4 cavity Y {lowv} (box {min(b[2] for b in boxes[4])}); lowest cavity overall {low_cav}; lowest structure block {low_box} = depth {-low_box}")
    r.metrics.update(lowest_cavity=low_cav, lowest_structure=low_box)
    # ---- L1 P2 layout facts
    sp2 = p2model["spaces"]
    def dims(k): b = sp2[k]["boxes"][0]; return (b[1] - b[0] + 1, b[5] - b[4] + 1)
    lnd = dims("LND12"), dims("LND23"); v12 = dims("V12")
    fa = [b for b in sp2["FA"]["boxes"]]; faw = fa[0][1] - fa[0][0] + 1
    r.add("L1", "P2 transition spaces keep the approved-by-P2 proportions: landings 9x9, L1/L2 vestibule 9x11, 7-wide stair flights", "PASS" if lnd == ((9, 9), (9, 9)) and v12 == (9, 11) and faw == 7 else "FAIL",
          f"landings {lnd}, V12 {v12}, flight A width {faw}")
    r.metrics["p2_route"] = d_c.get((-21, -22, 5)) if (-21, -22, 5) in d_c else None
    r.metrics["steps"] = dict(forecourt_to_hall=d_c.get(hall_goals["HALL_ENTRY"]), forecourt_to_chest=len(path) - 1 if path else None, hall_to_chest=len(bfs_path(open_solid, hatch, st_o, g["CHEST_FRONT"][2]) or []) - 1)
    # ---- cover candidates (numbers)
    hb = p3spec["hatch"]
    pocket_y = F3 - 2
    pocket = [p4["pierce"][0] - 4, p4["pierce"][0] - 1, pocket_y, pocket_y, 33, 35] if p4["side"] == "EAST" else [p4["pierce"][1] + 1, p4["pierce"][1] + 4, pocket_y, pocket_y, 33, 35]
    hall_air = [b for b in boxes[3]]
    gap_pocket = min(cheb_gap(pocket, b) for b in hall_air)
    r.metrics["cover"] = dict(cells=ncov, pocket_cells=ncov, gap_to_hall_air_if_pocket_in_slab=gap_pocket)
    r.add("C1", "Cover candidates: spatial requirement of block removal (A), sliding panel (B), piston/structure (C) in the 2-thick hall slab", "UNVERIFIED",
          f"cut at floor-top layer = {ncov} blocks. A needs 0 extra cells. B/C need >= {ncov} pocket cells beside the cut; a pocket in the lower slab layer (Y {pocket_y}) leaves {gap_pocket} solid layer(s) to the hall floor air (needs >= 2), so B/C require a thicker slab or outside-footprint pocket (not designed). Mechanism feasibility/in-game behaviour not verifiable here.")
    r.W = W; r.closed_solid = closed_solid; r.open_solid = open_solid
    return r

def main(outdir):
    p3e = S3.make_hall_east(); p3w = S3.mirror_spec(p3e)
    pe0 = make_east(); layouts = {"base": pe0, "C": PV.variant_C(pe0)}
    res = {}; bad = False
    for lay, pe in layouts.items():
        pw = mirror_model(pe)
        for cand in ("B", "A"):
            e4 = S4.make_variant(cand); w4 = S4.mirror_p4(e4)
            re_ = verify(e4, p3e, pe, f"{cand}-{lay}-E"); rw = verify(w4, p3w, pw, f"{cand}-{lay}-W")
            eq = {}
            for k in ("closed_solid", "open_solid"):
                eq[k] = bool(np.array_equal(getattr(re_, k), getattr(rw, k)[::-1]))
            eq["own"] = bool(np.array_equal(re_.W["own"], rw.W["own"][::-1])); eq["elem"] = bool(np.array_equal(re_.W["elem"], rw.W["elem"][::-1]))
            verd = [(a["id"], a["verdict"]) for a in re_.checks] == [(b["id"], b["verdict"]) for b in rw.checks]
            key = f"{cand}|{lay}"
            clean = lambda r_: dict(checks=r_.checks, metrics=json.loads(json.dumps(r_.metrics, default=lambda o: list(o) if isinstance(o, (set, tuple)) else str(o))))
            res[key] = dict(east=clean(re_), west=clean(rw), mirror=eq, verdicts_equal=verd)
            for side, r_ in (("E", re_), ("W", rw)):
                c = r_.checks
                print(f"{cand} layout={lay} {side}: {sum(x['verdict']=='PASS' for x in c)} PASS, {sum(x['verdict']=='FAIL' for x in c)} FAIL, {sum(x['verdict']=='UNVERIFIED' for x in c)} UNV -> " + ", ".join(f"{x['id']}={x['verdict']}" for x in c))
                for x in c:
                    if x["verdict"] == "FAIL" and not (x["id"] in ("X1", "L1") and lay == "base"): bad = True; print("   FAIL", x["id"], x["evidence"][:300])
            print("   mirror", eq, verd)
            bad |= not all(eq.values()) or not verd
            if cand == "B" and lay == "base":
                W = re_.W
                for nm, y in (("L1", -6), ("L2", -14), ("L3_hall", -22), ("B_vault", -30)):
                    pass
    # integrated plans / sections (B, base layout and C layout, east + west)
    os.makedirs(outdir, exist_ok=True)
    e4 = S4.make_variant("B"); pe = layouts["base"]
    for lay in ("base", "C"):
        for side in ("east", "west"):
            p2m = layouts[lay] if side == "east" else mirror_model(layouts[lay]); p4s = e4 if side == "east" else S4.mirror_p4(e4); p3s = p3e if side == "east" else p3w
            r_ = verify(p4s, p3s, p2m, "plan")
            d = os.path.join(outdir, "plans", f"B_{lay}_{side}"); os.makedirs(d, exist_ok=True)
            for nm, y in (("Y-6_L1", -6), ("Y-14_L2", -14), ("Y-22_L3_hall", -22), ("Y-30_secret", -30)):
                open(os.path.join(d, f"plan_{nm}.txt"), "w").write(render_plan(r_, y) + "\n")
            for ax, c in (("z", 35), ("z", 26), ("x", -7)):
                open(os.path.join(d, f"section_{ax}{c}.txt"), "w").write(render_section(r_, ax, c) + "\n")
    json.dump(res, open(os.path.join(outdir, "p5_verification.json"), "w"), indent=1, default=str)
    return 1 if bad else 0

LET = {}
def glyph(r, x, y, z):
    W = r.W; ix, iy, iz = C.ix, C.iy, C.iz
    if W["own"][ix(x), iy(y), iz(z)] >= 0: return "4"
    o = W["W3"]["owner"][ix(x), iy(y), iz(z)]
    if W["W3"]["main"][ix(x), iy(y), iz(z)] or W["W3"]["recess"][ix(x), iy(y), iz(z)]:
        return "e" if W["W3"]["elem"][ix(x), iy(y), iz(z)] >= 0 else "3"
    if W["W3"]["elem"][ix(x), iy(y), iz(z)] >= 0: return "e"
    if o >= 0:
        n = W["G"]["ids"][o] if "G" in W else ""
        return "1" if str(W["W3"]["G"]["ids"][o]).startswith("P1:") else "2"
    if not r.open_solid[ix(x), iy(y), iz(z)]: return "1"
    for k, m in W["env"].items():
        if m[ix(x), iy(y), iz(z)]: return "#"
    return " "
def render_plan(r, y):
    L = [f"INTEGRATED PLAN Y={y} X[-45,45] Z[-45,45] north(-Z) up, east(+X) right. 1=P1 2=P2 3=P3 hall air e=hall columns/dais 4=P4 #=P4 wall/slab; ' '=solid. (open state)"]
    L.append("   Z\\X " + "".join("+" if x == 0 else ("'" if x % 10 == 0 else " ") for x in range(-45, 46)))
    for z in range(-45, 46): L.append(f"{z:+4d}  " + "".join(glyph(r, x, y, z) for x in range(-45, 46)))
    return "\n".join(L)
def render_section(r, axis, c):
    L = [f"INTEGRATED SECTION {axis.upper()}={c} horizontal {'Z' if axis == 'x' else 'X'} -45..45, Y -40..8 (open state)"]
    for y in range(8, -41, -1):
        L.append(f"{y:+4d}  " + "".join(glyph(r, c, y, t) if axis == "x" else glyph(r, t, y, c) for t in range(-45, 46)))
    return "\n".join(L)

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p5out"))
