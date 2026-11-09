"""ST-01 P7 geometric analysis on the P6 integrated world (static, design-only): opening safety of the 12-block cover, golem route/arrival candidates, timer sizing inputs.
Uses the UNMODIFIED P6 verifier to obtain the voxel world (candidate C overlay + P3 hall + P4-B)."""
import sys, os, math, itertools
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
for d in ("st01-p7", "st01-p6", "st01-p5", "st01-p4", "st01-p3", "st01-p2", "st01-p1"): sys.path.insert(0, os.path.join(TOOLS, d))
import st01_p6_verify as X
V, C, P5 = X.V, X.C, X.P5

class R:
    def __init__(s): s.checks = []; s.metrics = {}
    def add(s, cid, title, verdict, ev): s.checks.append(dict(id=cid, title=title, verdict=verdict, evidence=ev))

def pt_rect_dist(px, pz, x0, x1, z0, z1):      # rectangle [x0,x1) x [z0,z1) in block-edge coordinates
    dx = max(x0 - px, 0, px - x1); dz = max(z0 - pz, 0, pz - z1); return math.hypot(dx, dz)

def build(side, mutate=None):
    ov = X.OV.extract(); p3 = X.S3.make_hall_east(); p4 = X.S4.make_variant("B")
    if side == "W": p3 = X.S3.mirror_spec(p3); p4 = X.S4.mirror_p4(p4)
    if mutate: mutate(p4, p3)
    return X.verify_p6(ov, p4, p3, side, skip_regress=True), p3, p4

def analyse(side, mutate=None):
    r6, p3, p4 = build(side, mutate)
    r = R(); ix, iy, iz = C.ix, C.iy, C.iz
    op, cl = r6.open_solid, r6.closed_solid; F = p3["hall"]["floor"]
    cells = r6.cells; cset = {(c[0], c[2]) for c in cells}
    stO, stC = C.standable(op), C.standable(cl)
    xs = [c[0] for c in cells]; zs = [c[2] for c in cells]
    x0, x1, z0, z1 = min(xs), max(xs) + 1, min(zs), max(zs) + 1            # block-edge rectangle
    # ---- GA1 player standing on the cover when it opens: landing, fall, headroom, climb-out
    goals = p4["goals"]; rows = []; ok = True; maxfall = 0; dmg = 0
    reach_cache = {}
    for (cx, cy, cz) in sorted(cells, key=lambda c: (c[0], c[2])):
        stand_closed = bool(stC[ix(cx), iy(F), iz(cz)])
        y = F
        while not op[ix(cx), iy(y - 1), iz(cz)] and y > F - 12: y -= 1
        fall = F - y
        head = sum(1 for k in range(0, 5) if not op[ix(cx), iy(y + k), iz(cz)] and all(not op[ix(cx), iy(y + j), iz(cz)] for j in range(k + 1)))
        st = bool(stO[ix(cx), iy(y), iz(cz)])
        d = V.bfs_dist(op, (cx, y, cz), stO) if st else {}
        out = tuple(goals["HATCH"]) in d; vault = tuple(goals["VAULT_ENTRY"]) in d
        ok &= stand_closed and st and head >= 4 and out and vault and fall <= 4
        maxfall = max(maxfall, fall); dmg = max(dmg, max(0, fall - 3))
        rows.append(dict(col=(cx, cz), standable_closed=stand_closed, landing_y=y, fall=fall, headroom=head, climb_out_to_hall=out, reach_vault=vault))
    r.metrics["ga1"] = rows; r.metrics["max_fall"] = maxfall
    r.add("GA1", "Player standing on a cover cell when the cover opens (FIND-P6-2): each of the 12 columns is standable while CLOSED, the player lands on a standable cell (no burial), headroom >= 4, can climb back to the hall floor and can walk on to the vault; drop <= 4 (FIND-P6-1)",
          "PASS" if ok else "FAIL", f"max drop {maxfall} (fall damage {dmg} under the vanilla 'above 3' rule); per column (drop/headroom): " + ", ".join(f"{q['col']}:{q['fall']}/{q['headroom']}" for q in rows) + f"; all can climb out {all(q['climb_out_to_hall'] for q in rows)}; all reach vault entry {all(q['reach_vault'] for q in rows)}")
    # ---- GA2 arrival-zone candidates: hall-floor cells reachable from the chest approach in CLOSED, binned by distance from the cover rectangle (centre-to-edge)
    starts = {k: (p3["hall"]["cx"] + u, F, p3["hall"]["cz"] + v) for k, (u, v) in p3["spawn"].items()}
    starts["CHEST_APPROACH"] = tuple(p3["chest_primary_access"][0]); starts["HALL_DOOR"] = (p3["hall"]["door"][0] + 1, F, p3["hall"]["door"][5] + 1)
    dist_from = {sn: V.bfs_dist(cl, sc, stC) for sn, sc in starts.items() if stC[ix(sc[0]), iy(sc[1]), iz(sc[2])]}
    dchest = dist_from["CHEST_APPROACH"]
    BINS = [(0.0, 0.51), (0.51, 1.51), (1.51, 2.51), (2.51, 3.51), (3.51, 4.51)]
    classes = {}; mism = [False]
    rect_ok = len(cset) == (x1 - x0) * (z1 - z0)
    for (x, y, z), d in dchest.items():
        if y != F or (x, z) in cset: continue
        dist = pt_rect_dist(x + 0.5, z + 0.5, x0, x1, z0, z1)
        dist2 = min(pt_rect_dist(x + 0.5, z + 0.5, cx, cx + 1, cz, cz + 1) for (cx, cz) in cset)
        if abs(dist - dist2) > 1e-9: mism[0] = True
        for i, (lo, hi) in enumerate(BINS):
            if lo < dist <= hi or (i == 0 and dist <= hi): classes.setdefault(i, []).append(((x, y, z), dist)); break
    info = {}
    for i, cl_ in sorted(classes.items()):
        mind = min(d for _, d in cl_)
        info[f"Z{i}"] = dict(bin=BINS[i], cells=len(cl_), min_dist_to_cover=round(mind, 3), r_safe_w1=round(mind - 0.5, 3), r_safe_w2=round(mind - 1.0, 3),
                             on_cover=any((c[0], c[2]) in cset for c, _ in cl_), all_standable_closed=all(bool(stC[ix(c[0]), iy(c[1]), iz(c[2])]) for c, _ in cl_))
    r.metrics["zones"] = info
    ok = rect_ok and not mism[0] and bool(info) and not any(v["on_cover"] for v in info.values()) and all(v["all_standable_closed"] for v in info.values()) and "Z0" in info
    r.add("GA2", "Arrival-zone candidates: hall-floor cells reachable from the chest approach in CLOSED, binned by distance from the cover rectangle; the cover is a full rectangle, none lies on the cover, all standable in CLOSED; the analytic rectangle distance equals the per-cell minimum; r_safe = largest arrival radius (around the stop point) that keeps the golem body off the cover",
          "PASS" if ok else "FAIL", "; ".join(f"{k}: {v['cells']} cells, nearest centre-to-cover {v['min_dist_to_cover']}, r_safe(w=1) {v['r_safe_w1']}, r_safe(w=2) {v['r_safe_w2']}" for k, v in info.items()))
    # ---- GA3 golem route lengths (CLOSED world, 1-wide 2-high agent): start -> nearest cell of each zone class
    lens = {}; unreach = []
    for sn, d in dist_from.items():
        for i, cl_ in classes.items():
            ds = [d[c] for c, _ in cl_ if c in d]
            if ds: lens[(sn, f"Z{i}")] = min(ds)
            else: unreach.append((sn, f"Z{i}"))
    for sn, sc in starts.items():
        if sn not in dist_from: unreach.append((sn, "start not standable"))
    Lmax = max(lens.values()) if lens else None; Lmin = min(lens.values()) if lens else None
    r.metrics["route_len"] = {f"{a}->{b}": v for (a, b), v in lens.items()}; r.metrics["Lmax"] = Lmax; r.metrics["Lmax_Z0"] = max([v for (a, b), v in lens.items() if b == "Z0"] or [None])
    r.add("GA3", "Golem walking route in the CLOSED world: every emergence candidate, the chest approach and the hall door reach every arrival-zone class (shortest steps measured; inputs for the fallback timer)",
          "PASS" if not unreach and lens else "FAIL", f"unreachable {unreach}; steps to the rim zone Z0: " + ", ".join(f"{a}={v}" for (a, b), v in lens.items() if b == "Z0") + f"; overall max {Lmax}")
    # ---- GA4 agent-size sensitivity (golem size UNDECIDED; measurement only)
    sens = {}
    for (w, h) in ((1, 2), (1, 3), (2, 3), (3, 4)):
        row = {}
        for sn, sc in starts.items():
            reach = V.agent_reach(cl, sc, w, h, max_drop=1)
            row[sn] = sum(1 for i, cl_ in classes.items() if any((c[0] - dx, c[1], c[2] - dz) in reach for c, _ in cl_ for dx in range(w) for dz in range(w)))
        sens[f"{w}x{w}x{h}"] = row
    r.metrics["agent_sensitivity"] = sens
    allw1 = all(n == len(classes) for n in sens["1x1x2"].values())
    r.add("GA4", "Agent footprint sensitivity (golem size UNDECIDED): number of zone classes reachable per start for 1x1x2, 1x1x3, 2x2x3, 3x3x4 agents (measurement, not a size proposal); the 1x1x2 agent reaches all classes", "PASS" if allw1 else "FAIL", json_s({k: str(v) for k, v in sens.items()}))
    r.starts = starts; r.classes = classes
    # ---- GA5 cover offsets relative to the royal-chest anchor (P3 CHEST_PRIMARY) - the structure-relative data an implementer needs
    ch = p3["elements"]["CHEST_PRIMARY"]["boxes"][0]; anchor = (ch[0], ch[2], ch[4])
    offs = sorted((c[0] - anchor[0], c[1] - anchor[1], c[2] - anchor[2]) for c in cells)
    dxr = (min(o[0] for o in offs), max(o[0] for o in offs)); dzr = (min(o[2] for o in offs), max(o[2] for o in offs)); dyr = {o[1] for o in offs}
    exp_dx = (12, 15) if side == "E" else (-15, -12)
    r.metrics["anchor"] = anchor; r.metrics["cover_offsets"] = dict(dx=dxr, dy=sorted(dyr), dz=dzr)
    r.add("GA5", "Cover cells relative to the royal-chest anchor (PROPOSED anchor = P3 CHEST_PRIMARY): dx 12..15 (east) / -15..-12 (west), dy -3, dz 0..2; 12 cells", "PASS" if len(offs) == 12 and dxr == exp_dx and dzr == (0, 2) and dyr == {-3} else "FAIL", f"anchor {anchor}; offsets dx {dxr}, dy {sorted(dyr)}, dz {dzr}, count {len(offs)}")
    # ---- GA6 closed-state guarantees that matter while the golem is guiding: cover cells identical solid, no walkable secret cell
    p3s = r6.W["W3"]["solid"].copy(); P5.add_p1_treads(p3s)
    F3 = F
    inres = r6.W["inres"]; ylo = iy(F3 - 1)
    reg = np.zeros_like(inres); reg[:, ylo:, :] = inres[:, ylo:, :]
    same = int((cl[reg] != p3s[reg]).sum())
    r.add("GA6", "While SEALED / GUARDIAN_DEFEATED / GOLEM_GUIDING the world is the CLOSED world: hall voxel-identical to P3 above the floor top, so golem walking and boss/player movement see the P3-verified floor", "PASS" if same == 0 else "FAIL", f"differing cells {same}")
    r.r6 = r6; r.p3 = p3; r.p4 = p4
    return r

def json_s(d): return "; ".join(f"{k}: {v}" for k, v in d.items())

def fallback_sizing(Lmax, speeds=(0.1, 0.2, 0.3)):
    """ILLUSTRATIVE ONLY: ticks to walk Lmax blocks at hypothetical speeds. Not proposals; speed is UNAPPROVED."""
    return {v: math.ceil(Lmax / v) for v in speeds}

if __name__ == "__main__":
    for sd in ("E", "W"):
        r = analyse(sd)
        print(sd, [(c["id"], c["verdict"]) for c in r.checks]); 
        for c in r.checks: print("  ", c["id"], c["evidence"][:400])
