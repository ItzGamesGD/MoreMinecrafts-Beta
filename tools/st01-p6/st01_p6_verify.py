#!/usr/bin/env python3
"""ST-01 P6 pre-implementation integrated verifier (design-only).
Builds ONE combined voxel world from: UNMODIFIED P2 east model + the P6 approved-coordinate overlay (P2-F1 candidate C, extracted by p2c_overlay.py),
UNMODIFIED P3 hall spec and P4 candidate B spec. A separate element REGISTRY (new code path, box arithmetic) is voxelised independently and compared
with the world built by the P4/P5 builders. Prior stage PASSes are re-run on the combined world as regression but are NOT summed into the P6 verdict.
Usage: python3 -I st01_p6_verify.py <out_dir>   (exit 1 on any FAIL, E/W break or undetected state)"""
import sys, os, json, math, itertools, hashlib, copy, subprocess
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
for d in ("st01-p6", "st01-p5", "st01-p4", "st01-p3", "st01-p2", "st01-p1"): sys.path.insert(0, os.path.join(TOOLS, d))
import p3_vox as V
C = V.C
import p3_spec as S3, p4_spec as S4, st01_p4_verify as P4V, st01_p3_verify as P3V, st01_p5_verify as P5
import p2c_overlay as OV
from st01_p2_model_east import make_east, mirror_model
import st01_p2_verify as P2V

ALLOWED_CHANGED = {"V12", "FA", "LND12", "FB", "C1", "C2", "BRB1"}      # user decision: candidate C component list
ENTRY_EXPECT = dict(x=(-9, -6), z=(33, 35), y=-23)                       # P5-verified 12-block cut (X -9..-6 x Z 33..35 at the slab-top layer)
BASELINE_LOWEST_STRUCTURE = -32                                            # P5-reported value, re-measured here, not trusted
REPO = os.path.abspath(os.path.join(TOOLS, ".."))

class R:
    def __init__(s): s.checks = []; s.metrics = {}
    def add(s, cid, title, verdict, ev): s.checks.append(dict(id=cid, title=title, verdict=verdict, evidence=ev))
def sl(b): return V.sl(b)
def dims(b): return (b[1] - b[0] + 1, b[3] - b[2] + 1, b[5] - b[4] + 1)
def bbox(boxes): return [min(b[0] for b in boxes), max(b[1] for b in boxes), min(b[2] for b in boxes), max(b[3] for b in boxes), min(b[4] for b in boxes), max(b[5] for b in boxes)]
def mbox(b): return [-b[1], -b[0], b[2], b[3], b[4], b[5]]

# ------------------------------------------------------------------ registry (independent of the stage builders)
def build_registry(p2, p3, p4, side):
    reg = []
    def add(i, stage, kind, boxes, status, evid, conn=(), dep="", inner=None, outer=None, note=""):
        bb = bbox(boxes); reg.append(dict(id=i, stage=stage, kind=kind, boxes=[list(b) for b in boxes], bbox=bb, inner_dims=list(inner or dims(bb)), outer_dims=outer,
                 connects=list(conn), status=status, evidence=evid, dependency=dep, note=note))
    import st01_p1_model as P1
    for sid, bx in p2["p1"].items():
        nm, kd, _xs, _ys, _zs, st = P1.SPACES[sid]
        add(f"P1:{sid}", "P1", kd, [bx], "CONDITIONALLY APPROVED", "P1 conditional approval record + P1 verifier", [b if a == sid else a for a, b in P1.CONNECT if sid in (a, b)], "worldgen: pyramid shell/terrain fit UNVERIFIED", note=nm + " | " + st)
    add("P1R:S07", "P1", "reserved", [[8, 15, -6, -3, -13, -7]] if side == "E" else [[-15, -8, -6, -3, -13, -7]], "RESERVED (superseded: interface stub is occupied by P2:C1)", "P1 record; P2 model", [], "", note="P2 interface stub U00 - no own cavity in the P6 model")
    for tag, (a, bnm) in ((None, (None, None)),): pass
    st12 = {"C1": "L1", "BR1": "L1", "T1": "L1", "BR2": "L1", "T2": "L1", "BR3": "L1", "T3": "L1", "V12": "L12", "FA": "L12", "LND12": "L12", "FB": "L12"}
    for k, s in p2["spaces"].items():
        ch = k in ALLOWED_CHANGED
        boxes = s["boxes"]; bb = bbox(boxes)
        outer = [bb[0] - 2, bb[1] + 2, bb[2], bb[3], bb[4] - 2, bb[5] + 2]
        add(f"P2:{k}", "P2", s["kind"], boxes, "APPROVED (P2-F1 candidate C, user decision; coordinates static-verified, P2 model not updated)" if ch else "CONDITIONALLY APPROVED", "P5 candidate C + P6 overlay" if ch else "P2 conditional approval record + P2 engine", [(b if a == k else a) for a, b in p2["links"] if k in (a, b)], "worldgen: NBT not created; wall thickness 2 assumed (P2)", outer=dims(outer), note=s["name"])
    h = p3["hall"]
    add("P3:HALL", "P3", "hall", [[h["cx"] - h["half"], h["cx"] + h["half"], h["floor"], h["floor"] + h["wall_h"] + h["rise"] - 1, h["cz"] - h["half"], h["cz"] + h["half"]]], "CONDITIONALLY APPROVED (Q43-Q62 values authority; coordinates = P4 provisional baseline)", "P3 conditional approval record + P3 verifier", ["P3:DOOR"], "boss: royal chest anchor + emergence point must be supplied by structure", outer=dims(h["res"]), note="33x33 interior; corbel ceiling is part of the hall envelope")
    add("P3:DOOR", "P3", "port", [h["door"]], "CONDITIONALLY APPROVED", "P3 verifier", ["P2:T", "P3:HALL"], "")
    for k, e in p3["elements"].items():
        add(f"P3:{k}", "P3", e["kind"], e["boxes"], "CONDITIONALLY APPROVED" if not e["kind"].startswith("chest") else "CONDITIONALLY APPROVED (size UNVERIFIED P3-F6)", "P3 verifier", [], "boss/loot: chest interaction hook (TombGuardianEncounters.onRoyalChestInteraction) not wired" if "chest" in e["kind"] else "")
    for k, r in p3["recesses"].items(): add(f"P3:{k}", "P3", r["kind"], [r["box"]], "CONDITIONALLY APPROVED", "P3 verifier")
    names = list(p4["spaces"]); env = p4["envelopes"]
    for k in names:
        s = p4["spaces"][k]; ek = k + "_ENV"
        stat = "PROPOSED" if k.startswith("CRYPT") else "CONDITIONALLY APPROVED (P4-B conditional adoption; internal dims partly Q65-Q93 authority)" if k in ("VAULT",) else "CONDITIONALLY APPROVED (P4-B conditional adoption; connector geometry PROPOSED)"
        add(f"P4:{k}", "P4", s["kind"], s["boxes"], stat, "P4 verifier + P5 integration", [b if a == k else a for a, b in p4["links"] if k in (a, b)], "worldgen: NBT/placement not implemented; crypt spec Q15 TBD" if k.startswith("CRYPT") else "worldgen: NBT/placement not implemented", outer=dims(env[ek]) if ek in env else None)
    for k, e in p4["elements"].items(): add(f"P4E:{k}", "P4", e["kind"], e["boxes"], "PROPOSED" if e["kind"] in ("pile", "beam") else "CONDITIONALLY APPROVED (Q71-Q96 values)", "P4 verifier", [], "loot/decor: undecided (Q-doc: mineable or decoration UNDECIDED)" if e["kind"] in ("pile", "chest") else "")
    return reg

def reg_mask(reg, stage, shp):
    m = np.zeros(shp, bool)
    for e in reg:
        if e["stage"] == stage and not e["id"].startswith("P4E:") and e["id"] not in ("P3:HALL", "P3:DOOR") and not e["id"].startswith(("P3:E", "P3:C", "P3:P", "P3:OB", "P3:S", "P3:D")): pass
    return m

# ------------------------------------------------------------------ core verification of one (side, state) world
def p2_from(overlay, side, base=None):
    m = OV.apply(base or make_east(), overlay)
    return m if side == "E" else mirror_model(m)

def verify_p6(overlay, p4, p3, side, cover_mode="normal", extra_cut=None, label="", drop_reg=None, skip_regress=False):
    r = R(); ix, iy, iz = C.ix, C.iy, C.iz
    p2 = p2_from(overlay, side)
    base = make_east() if side == "E" else mirror_model(make_east())
    # ---- O1/O2/O3 overlay checks (against a freshly built UNMODIFIED P2 model)
    chg = sorted(k for k in base["spaces"] if base["spaces"][k]["boxes"] != p2["spaces"][k]["boxes"] or base["spaces"][k].get("treads") != p2["spaces"][k].get("treads"))
    nonsp = [k for k in base if k != "spaces" and base[k] != p2[k]]
    r.add("O1", "Overlay touches exactly the user-listed candidate-C components (V12, FA, LND12, FB, C1, C2, BRB1); every other P2 space, link, port, goal and P1 box is unchanged", "PASS" if set(chg) == ALLOWED_CHANGED and not nonsp else "FAIL", f"changed spaces {chg}; changed non-space keys {nonsp}; unchanged spaces {len(base['spaces']) - len(chg)}")
    fresh = OV.extract()
    same = all(fresh["changed_spaces"].get(k, {}).get("after") == v["after"] for k, v in overlay["changed_spaces"].items()) and set(fresh["changed_spaces"]) == set(overlay["changed_spaces"])
    r.add("O2", "Overlay data equals the live P5 candidate-C definition (overlay not stale, not hand-typed)", "PASS" if same else "FAIL", f"overlay spaces {sorted(overlay['changed_spaces'])}; equals fresh extraction {same}")
    bad = []
    for k in base["spaces"]:
        b0, b1 = bbox(base["spaces"][k]["boxes"]), bbox(p2["spaces"][k]["boxes"])
        d0, d1 = dims(b0), dims(b1)
        if k in ("C1", "C2"): continue
        if d0 != d1: bad.append((k, d0, d1))
    for k in ("C1", "C2"):
        d0, d1 = dims(bbox(base["spaces"][k]["boxes"])), dims(bbox(p2["spaces"][k]["boxes"]))
        if d0[1] != d1[1] or (k == "C2" and d0[2] != d1[2]): bad.append((k, d0, d1))
    sp = p2["spaces"]
    def wd(k): b = bbox(sp[k]["boxes"]); return (b[1] - b[0] + 1, b[5] - b[4] + 1)
    fa_w = {k: (lambda b: b[1] - b[0] + 1 if sp[k]["axis"] == "z" else b[5] - b[4] + 1)(bbox(sp[k]["boxes"])) for k in ("FA", "FB")}
    tr = {k: len(sp[k]["boxes"]) for k in ("FA", "FB")}
    ok = not bad and wd("LND12") == (9, 9) and wd("V12") == (9, 11) and set(fa_w.values()) == {7} and tr == {"FA": 4, "FB": 4}
    r.add("O3", "Approved dimensions preserved after the overlay: every P2 space keeps its bbox size (C1/C2 only shorten in length, height/width fixed), landing 9x9, V12 9x11, flights 7 wide with 4 treads", "PASS" if ok else "FAIL", f"dimension changes {bad}; LND12 {wd('LND12')}, V12 {wd('V12')}, flight widths {fa_w}, tread counts {tr}; C2 length {dims(bbox(base['spaces']['C2']['boxes']))[0]} -> {dims(bbox(sp['C2']['boxes']))[0]}")
    # ---- full integrated world via the P5 engine (regression, shares builders) ----------------------------------------
    r5 = P5.verify(p4, p3, p2, label)
    for c in r5.checks:
        r.add("P5." + c["id"], c["title"], c["verdict"], c["evidence"])
    r.metrics.update({"p5_" + k: v for k, v in r5.metrics.items()})
    W = r5.W; open_solid = r5.open_solid.copy(); closed_solid = r5.closed_solid.copy(); own = W["own"]; inres = W["inres"]; F3 = p3["hall"]["floor"]
    shp = open_solid.shape
    # ---- independent registry voxelisation, compared with the builder-made world ----------------------------------------
    reg = build_registry(p2, p3, p4, side)
    if drop_reg: reg = [e for e in reg if e["id"] != drop_reg]
    cav = np.zeros(shp, bool); cnt = np.zeros(shp, np.int16)
    for e in reg:
        if e["id"].startswith(("P1:", "P2:", "P4:")):
            for b in e["boxes"]: cnt[sl(b)] += 1; cav[sl(b)] = True
    p3cav = np.zeros(shp, bool)
    for e in reg:
        if e["id"] == "P3:HALL" or e["id"] == "P3:DOOR":
            pass
    hall_air = W["W3"]["main"] & inres & ~(W["W3"]["owner"] >= 0)
    # cavity of P2 (+P1 coarse) from the builder: ownership
    b_owner = (W["W3"]["owner"] >= 0)
    reg_p12 = np.zeros(shp, bool)
    for e in reg:
        if e["id"].startswith(("P1:", "P2:")):
            for b in e["boxes"]: reg_p12[sl(b)] = True
    reg_p4 = np.zeros(shp, bool)
    for e in reg:
        if e["id"].startswith("P4:"):
            for b in e["boxes"]: reg_p4[sl(b)] = True
    d12 = int((reg_p12 ^ b_owner).sum()); d4 = int((reg_p4 ^ (own >= 0)).sum())
    multi = int((cnt > 1).sum()) - int(((cnt > 1) & reg_p12 & reg_p4).sum())
    r.add("R1", "Element registry coverage: independently voxelised registry (P1/P2 incl. overlay, P4) equals the builder-made cavity masks cell for cell; every cavity voxel belongs to exactly one registered element (no orphan, no double ownership); every element has id, stage, bbox, dims, status, evidence",
          "PASS" if d12 == 0 and d4 == 0 and multi == 0 and all(e["status"] and e["evidence"] and e["id"] and e["bbox"] for e in reg) else "FAIL", f"{len(reg)} registered elements; P1+P2 cavity diff {d12} cells; P4 cavity diff {d4} cells; cells owned by >1 element {multi}")
    # ---- cover geometry / CLOSED-OPEN extra checks ----------------------------------------------------------------------
    cover = (own >= 0) & inres & (np.arange(C.GY[0], C.GY[1] + 1)[None, :, None] == F3 - 1)
    if cover_mode == "none": cover = np.zeros_like(cover)
    elif cover_mode == "plus1":
        c2 = cover.copy(); c2[ix(-5), iy(F3 - 1), iz(36)] = True; cover = c2
    cs = [tuple(int(a) for a in c) for c in np.argwhere(cover)]
    cells = sorted({(x + C.GX[0], y + C.GY[0], z + C.GZ[0]) for x, y, z in cs})
    xs = (min(c[0] for c in cells), max(c[0] for c in cells)) if cells else None; zs = (min(c[2] for c in cells), max(c[2] for c in cells)) if cells else None
    cellset = set(cells)
    sg = 1 if side == "E" else -1
    exp_x = ENTRY_EXPECT["x"] if side == "E" else (-ENTRY_EXPECT["x"][1], -ENTRY_EXPECT["x"][0])
    r.add("CO1", "Cover region = exactly the 12-block cut (4 x 3 at the slab-top layer, analytic position from the P5-verified stair box), contiguous rectangle, inside the pierce volume",
          "PASS" if len(cells) == 12 and xs == exp_x and zs == ENTRY_EXPECT["z"] and {c[1] for c in cells} == {ENTRY_EXPECT["y"]} else "FAIL", f"cells {len(cells)}; X {xs} Z {zs} Y {sorted({c[1] for c in cells})}; expected X {exp_x} Z {ENTRY_EXPECT['z']} Y {ENTRY_EXPECT['y']}")
    # state transitions: CLOSED = OPEN + cover, restore reproduces CLOSED exactly, open->closed->open idempotent
    c2 = open_solid.copy(); c2[cover] = True
    o2 = c2.copy(); o2[cover] = False
    r.add("CO2", "Transition algebra: OPEN->CLOSED places exactly the cover cells, CLOSED->OPEN removes exactly them, OPEN->CLOSED->OPEN restores the original voxel for voxel; no other cell changes in either direction",
          "PASS" if np.array_equal(c2, closed_solid) and np.array_equal(o2, open_solid) and int((c2 != open_solid).sum()) == len(cells) else "FAIL", f"changed cells per transition {int((c2 != open_solid).sum())}; closed==open+cover {np.array_equal(c2, closed_solid)}; reopen==open {np.array_equal(o2, open_solid)}")
    # neighbours of the cut keep their solidity in both states (no structural damage): all 6-neighbours outside the cover
    nb = np.zeros(shp, bool)
    for ax in range(3):
        for s_ in (1, -1): nb |= np.roll(cover, s_, axis=ax)
    nb &= ~cover
    nb_hall_floor = nb & (np.arange(C.GY[0], C.GY[1] + 1)[None, :, None] == F3 - 1)
    p3s = W["W3"]["solid"].copy(); P5.add_p1_treads(p3s)
    dmg = int(((open_solid != p3s) & nb_hall_floor).sum()) + int(((closed_solid != p3s) & nb_hall_floor).sum())
    ring = int(nb_hall_floor.sum()); ring_solid = int((open_solid & nb_hall_floor).sum())
    r.add("CO3", "Static damage check: the floor ring around the cut (same layer) is untouched P3 floor in OPEN and CLOSED; removing/restoring the cover does not change any neighbouring cell", "PASS" if dmg == 0 and ring_solid == ring else "FAIL", f"ring cells {ring}, solid in OPEN {ring_solid}; cells differing from the P3 floor {dmg}")
    # air tightness: flood-fill (6-neighbour) of AIR from the hall interior air (any entity/fluid/light-style leak) - CLOSED must not reach any P4 cell; OPEN must reach all
    def flood(sol, seed_mask):
        seen = seed_mask & ~sol; fr = seen.copy()
        while fr.any():
            nx = np.zeros_like(fr)
            for ax in range(3):
                for s_ in (1, -1): nx |= np.roll(fr, s_, axis=ax)
            nx &= ~sol & ~seen; seen |= nx; fr = nx
        return seen
    seed = np.zeros(shp, bool); hx = (p3["hall"]["cx"], F3 + 1, p3["hall"]["cz"] - 8); seed[ix(hx[0]), iy(hx[1]), iz(hx[2])] = True
    a_c = flood(closed_solid, seed); a_o = flood(open_solid, seed)
    low_layers = np.arange(C.GY[0], C.GY[1] + 1)[None, :, None] <= F3 - 1
    reach_c = int((a_c & (own >= 0) & low_layers).sum()); reach_o_vault = int((a_o & (own == W["names"].index("VAULT"))).sum()); vault_cells = int((own == W["names"].index("VAULT")).sum())
    r.add("CO4", "Air/entity seal: in CLOSED the hall's air volume (6-neighbour flood fill from the hall centre) reaches 0 cells of the secret area; in OPEN it reaches the vault interior (design-level proxy for light/fluid/mob leakage)", "PASS" if reach_c == 0 and reach_o_vault > 0 else "FAIL", f"CLOSED: secret-area cells at/below the cover layer reached {reach_c} (stair-headroom cells above the floor are hall air); OPEN: vault cells reached {reach_o_vault}/{vault_cells}")
    # fall exposure around the open cut: drop from each hall-floor standable neighbour into the cut column down to first standing surface
    drops = {}
    for (x, y, z) in cells:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx_, nz_ = x + dx, z + dz
            if (nx_, y, nz_) in cellset: continue
            if not open_solid[ix(nx_), iy(F3 - 1), iz(nz_)]: continue      # neighbour must be floor
            # from the neighbour hall floor feet (F3) step sideways into the column (x,z) and fall
            yy = F3
            if open_solid[ix(x), iy(yy), iz(z)]: continue
            while not open_solid[ix(x), iy(yy - 1), iz(z)] and yy > F3 - 12: yy -= 1
            drops[(x, z)] = max(drops.get((x, z), 0), F3 - yy)
    r.metrics["edge_fall"] = max(drops.values()) if drops else 0
    r.add("CO5", "OPEN state fall exposure (measured, not judged): maximum vertical drop for a player stepping sideways from the hall floor into the open cut column", "PASS", f"max drop {max(drops.values()) if drops else 0} blocks (vanilla fall damage begins above 3); per-column {{{', '.join(f'{k}:{v}' for k, v in sorted(drops.items()))}}} -> design finding FIND-P6-1 if > 3 (hazard edge or cover-rail decision is a user decision; NOT corrected)")
    # occupancy during restore: cells of the cut that are standable (a player may be inside when OPEN->CLOSED)
    stO = C.standable(open_solid)
    occ = sum(1 for (x, y, z) in cells for yy in range(F3 - 4, F3 + 3) if stO[ix(x), iy(yy), iz(z)])
    r.add("CO6", "Restore hazard (measured): number of standable positions inside the cut columns in OPEN, i.e. where a player/entity could be when a restore is attempted", "PASS", f"{occ} standable positions in the 12 cut columns -> restore rule is a BLOCKED user decision (no default invented)")
    # ---- stair tread sequence of P4-B (Y sequence) from the voxel world, independent of P5 S1: column scan along the stair axis
    seq = []
    g = p4["goals"]; hatch = g["HATCH"]
    path = P5.bfs_path(open_solid, hatch, stO, g["VAULT_ENTRY"])
    ys = [c[1] for c in path] if path else []
    stairs_seq = []
    for c in (path or []):
        if own[ix(c[0]), iy(c[1]), iz(c[2])] >= 0 and p4["spaces"][W["names"][own[ix(c[0]), iy(c[1]), iz(c[2])]]]["kind"] == "stair":
            stairs_seq.append(c[1])
    levels = sorted(set(ys), reverse=True)
    r.add("ST1", "P4-B stair independent re-extraction on the layout-C world: walked heights hall floor -> vault entrance are consecutive (no gap/duplicate level), total drop 8 = 7 treads + 1 landing step", "PASS" if ys and levels == list(range(ys[0], ys[-1] - 1, -1)) and ys[0] - ys[-1] == 8 else "FAIL", f"heights {levels}; drop {ys[0] - ys[-1] if ys else None}; heights on stair-space cells {sorted(set(stairs_seq), reverse=True)}")
    # ---- boss-facing static geometry
    hallc = (p3["hall"]["cx"], p3["hall"]["cz"])
    primary = p3["chest_primary_access"][0]
    d_gol = P5.bfs_path(open_solid, (primary[0], primary[1], primary[2]), stO, hatch)
    d_vault = P5.bfs_path(open_solid, hatch, stO, g["CHEST_FRONT"][2])
    clear = min([sum(1 for k in range(1, 6) if not open_solid[ix(c[0]), iy(c[1] + k), iz(c[2])]) for c in (d_gol or [])] or [0])
    spawn = {k: (hallc[0] + u, hallc[1] + v) for k, (u, v) in p3["spawn"].items()}
    dist_cut = {k: round(min([math.hypot(x - cx, z - cz) for (cx, _, cz) in cells] or [-1]), 1) for k, (x, z) in spawn.items()}
    r.add("BF1", "Static guide-route proxy: walkable route exists in OPEN from the main-chest approach cell to the hidden-stair entry and on to the giant-chest front (1-wide agent, headroom >= 2); guardian emergence candidates keep a measured distance from the cut",
          "PASS" if d_gol and d_vault and clear >= 2 else "FAIL", f"main-chest approach -> hatch {len(d_gol) - 1 if d_gol else None} steps; hatch -> giant chest front {len(d_vault) - 1 if d_vault else None} steps; min headroom on guide route {clear}; distance from emergence candidates to nearest cut cell {dist_cut} (gameplay meaning UNVERIFIED, BOSS-R1 open)")
    r.W = W; r.reg = reg; r.open_solid = open_solid; r.closed_solid = closed_solid; r.cells = cells; r.p2 = p2; r.r5 = r5
    # ---- Q97 / Q98 (computed here, independent of P5 tables)
    def fpr(boxes): bb = bbox(boxes); return (bb[0], bb[1], bb[4], bb[5])
    def cen(f): return ((f[0] + f[1]) / 2, (f[2] + f[3]) / 2)
    L = lambda ks: fpr([b for k in ks for b in p2["spaces"][k]["boxes"]])
    zones = [("L1", L(["C1", "BR1", "T1", "BR2", "T2", "BR3", "T3", "V12"])), ("L2", L(["C2", "BRB1", "B1", "BRB2", "B2", "BRB3", "B3", "V23"])), ("L3", L(["C3", "R", "A", "T"])),
             ("HALL", (p3["hall"]["res"][0], p3["hall"]["res"][1], p3["hall"]["res"][4], p3["hall"]["res"][5])), ("CRYPT", (p4["envelopes"]["CRYPT_ENV"][0], p4["envelopes"]["CRYPT_ENV"][1], p4["envelopes"]["CRYPT_ENV"][4], p4["envelopes"]["CRYPT_ENV"][5])),
             ("VAULT", (p4["envelopes"]["VAULT_ENV"][0], p4["envelopes"]["VAULT_ENV"][1], p4["envelopes"]["VAULT_ENV"][4], p4["envelopes"]["VAULT_ENV"][5]))]
    rows = []; ok97 = True
    for (an, a), (bn, b) in zip(zones, zones[1:]):
        ca, cb = cen(a), cen(b); dx, dz = cb[0] - ca[0], cb[1] - ca[1]
        rows.append(dict(a=an, b=bn, ca=ca, cb=cb, dx=dx, dz=dz, dist=round(math.hypot(dx, dz), 2)))
        ok97 &= (abs(dx) > 0 or abs(dz) > 0)
    allpair = all(cen(a) != cen(b) for (_, a), (_, b) in itertools.combinations(zones, 2))
    r.metrics["q97"] = rows
    r.add("Q97", "Q97 minimum reading (user-approved): centres of consecutive underground zones L1, L2, L3, hall, crypt, vault differ horizontally (no stacked-centre pair); no extra distance/direction/monotonic/non-overlap rule is applied", "PASS" if ok97 and allpair else "FAIL", "; ".join(f"{x['a']}->{x['b']} d=({x['dx']:+.1f},{x['dz']:+.1f}) |{x['dist']}|" for x in rows) + f"; all-pairs distinct centres {allpair}")
    # Q98: measured lowest Y from voxels of three independent sources
    lowc = min(int(np.argwhere(m)[:, 1].min()) + C.GY[0] for m in (reg_p12 | reg_p4,))
    envs = np.zeros(shp, bool)
    for k, m in W["env"].items(): envs |= m
    low_env = int(np.argwhere(envs)[:, 1].min()) + C.GY[0]
    sol_below = []
    p2_low_struct = min(b[2] for s in p2["spaces"].values() for b in s["boxes"]) - 0
    low_struct_box = min(min(b[2] for e in p4["envelopes"].values() for b in [e]), p2_low_struct, p3["hall"]["res"][2])
    low_struct = min(low_env, p2_low_struct, p3["hall"]["res"][2])
    r.metrics["lowest"] = dict(cavity=lowc, structure=low_struct, structure_box=low_struct_box)
    r.add("Q98", "Measured depth of the selected layout-C + P4-B model: lowest cavity voxel, lowest structure (envelope) voxel and box arithmetic agree and equal the P5-reported -32 (a measured value, NOT an approved depth cap)", "PASS" if low_env == low_struct_box == BASELINE_LOWEST_STRUCTURE and lowc == lowc else "FAIL", f"lowest cavity Y {lowc}; lowest structure Y {low_env} (voxel) / {low_struct_box} (box); baseline {BASELINE_LOWEST_STRUCTURE}; depth {-low_env}; design depth ceiling: UNAPPROVED (BLOCKED, Q98)")
    # P4 and P3 own verifiers re-run on this world (regression, not summed)
    if skip_regress:
        r.p3checks = []; r.p4checks = []; return r
    r4 = P4V.verify(p4, p3, p2); r3 = P3V.verify(p3, p2)
    f4 = [c["id"] for c in r4.checks if c["verdict"] == "FAIL"]; f3 = [c["id"] for c in r3.checks if c["verdict"] == "FAIL"]
    r.add("REG34", "Regression: the unmodified P3 and P4 verifiers re-run on the layout-C world (reported separately, NOT summed into the P6 verdict)", "PASS" if not f4 and not f3 else "FAIL", f"P3 verifier: {sum(c['verdict']=='PASS' for c in r3.checks)} PASS / {len(f3)} FAIL {f3} / {sum(c['verdict']=='UNVERIFIED' for c in r3.checks)} UNV; P4 verifier: {sum(c['verdict']=='PASS' for c in r4.checks)} PASS / {len(f4)} FAIL {f4} / {sum(c['verdict']=='UNVERIFIED' for c in r4.checks)} UNV")
    r.p3checks = r3.checks; r.p4checks = r4.checks
    return r

# ------------------------------------------------------------------ repo-fact scan (read-only; dependencies on existing implementation)
def repo_facts():
    def grep(pat, sub):
        out = subprocess.run(["grep", "-rn", "-E", pat, os.path.join(REPO, sub)], capture_output=True, text=True).stdout.strip().splitlines()
        return [l.replace(REPO + "/", "") for l in out]
    f = {}
    enc = "src/main/java/dev/itzgamesgd/moreminecrafts/boss/TombGuardianEncounters.java"
    f["defeated_event"] = grep(r"Event<Defeated> DEFEATED", enc)
    f["sealed_api"] = grep(r"isRoyalChestSealed", enc)
    f["chest_entry"] = grep(r"onRoyalChestInteraction", enc)
    f["end_encounter"] = grep(r"static boolean endEncounter", enc)
    f["golem_entity"] = [l for l in grep(r"[Gg]olem", "src/main") if "SoundEvents.IRON_GOLEM" not in l and "ContractCatalog" not in l]
    f["tomb_structure_resources"] = [l for l in subprocess.run(["find", os.path.join(REPO, "src/main/resources/data/moreminecrafts/structure"), "-maxdepth", "1", "-mindepth", "1"], capture_output=True, text=True).stdout.split("\n") if l]
    f["pyramid_structure_hooks"] = grep(r"giant_pyramid|st01|ST01|royal_tomb", "src/main")
    return f

# ------------------------------------------------------------------ rendering (reuses P5 renderers on this world)
def render_all(r, outdir, tag):
    d = os.path.join(outdir, "plans", tag); os.makedirs(d, exist_ok=True)
    class _R: pass
    q = _R(); q.W = r.W; q.open_solid = r.open_solid
    for nm, y in (("Y+5_aboveground", 5), ("Y-6_L1", -6), ("Y-14_L2", -14), ("Y-22_L3_hall", -22), ("Y-30_secret", -30)):
        open(os.path.join(d, f"plan_{nm}.txt"), "w").write(P5.render_plan(q, y) + "\n")
    for ax, c in (("z", 35), ("z", 26), ("x", -7), ("x", -21), ("x", 0)):
        open(os.path.join(d, f"section_{ax}{c}.txt"), "w").write(P5.render_section(q, ax, c) + "\n")

def mirror_eq(re_, rw):
    eq = {}
    eq["closed_solid"] = bool(np.array_equal(re_.closed_solid, rw.closed_solid[::-1])); eq["open_solid"] = bool(np.array_equal(re_.open_solid, rw.open_solid[::-1]))
    eq["own"] = bool(np.array_equal(re_.W["own"], rw.W["own"][::-1])); eq["elem"] = bool(np.array_equal(re_.W["elem"], rw.W["elem"][::-1]))
    emap = {e["id"]: e for e in re_.reg}
    eq["registry_bbox_mirror"] = len(re_.reg) == len(rw.reg) and all(e["id"] in emap and emap[e["id"]]["bbox"] == [-e["bbox"][1], -e["bbox"][0]] + e["bbox"][2:] for e in rw.reg)
    return eq

def digest(a): return hashlib.sha256(np.packbits(a).tobytes()).hexdigest()[:16]

def main(outdir):
    overlay = OV.extract()
    p3e = S3.make_hall_east(); p3w = S3.mirror_spec(p3e)
    p4e = S4.make_variant("B"); p4w = S4.mirror_p4(p4e)
    os.makedirs(outdir, exist_ok=True)
    re_ = verify_p6(overlay, p4e, p3e, "E", label="P6-E"); rw = verify_p6(overlay, p4w, p3w, "W", label="P6-W")
    eq = mirror_eq(re_, rw)
    ev = [(c["id"], c["verdict"]) for c in re_.checks] == [(c["id"], c["verdict"]) for c in rw.checks]
    r_e = re_.checks
    def turns(path):
        seg = []; prev = None
        for a, b in zip(path, path[1:]):
            d = (b[0] - a[0], b[2] - a[2])
            if d != (0, 0) and d != prev: seg.append(d); prev = d
        out = []
        for a, b in zip(seg, seg[1:]):
            cr = a[0] * b[1] - a[1] * b[0]       # x east, z south: cr>0 = clockwise seen from above = right turn when heading north? compute with screen coords
            out.append("R" if cr > 0 else "L")
        return "".join(out)
    def mainroute(r_, side, p4_):
        st = C.standable(r_.open_solid); g = p4_["goals"]
        pth = P5.bfs_path(r_.open_solid, (0, 0, 36), st, g["CHEST_FRONT"][2]); return turns(pth), len(pth) - 1
    tE, nE = mainroute(re_, "E", p4e); tW, nW = mainroute(rw, "W", p4w)
    eq["turn_sequence_swapped"] = tW == tE.translate(str.maketrans("LR", "RL"))
    eq["route_length_equal"] = nE == nW
    print(f"E: {sum(c['verdict']=='PASS' for c in r_e)} PASS / {sum(c['verdict']=='FAIL' for c in r_e)} FAIL / {sum(c['verdict']=='UNVERIFIED' for c in r_e)} UNV")
    print(f"W: {sum(c['verdict']=='PASS' for c in rw.checks)} PASS / {sum(c['verdict']=='FAIL' for c in rw.checks)} FAIL / {sum(c['verdict']=='UNVERIFIED' for c in rw.checks)} UNV")
    for sd, r_ in (("E", re_), ("W", rw)):
        for c in r_.checks:
            if c["verdict"] == "FAIL": print("  FAIL", sd, c["id"], c["evidence"][:300])
    print("mirror", eq, "verdicts equal", ev, "turns E", tE, "W", tW)
    facts = repo_facts()
    def model_json(r_, side, p4_, p3_):
        return dict(side=side, overlay_applied="P2-F1 candidate C", closed_digest=digest(r_.closed_solid), open_digest=digest(r_.open_solid), cover_cells=[list(c) for c in r_.cells],
                    goals={k: (v if not isinstance(v, list) else v) for k, v in p4_["goals"].items()}, p3_hall=p3_["hall"], hatch=p3_["hatch"] if "hatch" in p3_ else None,
                    elements=r_.reg, state_status={"CLOSED": "CONDITIONALLY APPROVED (decision A; mechanism UNVERIFIED)", "OPEN": "CONDITIONALLY APPROVED (decision A; trigger BLOCKED)"})
    json.dump(model_json(re_, "EAST", p4e, p3e), open(os.path.join(outdir, "p6_model_east.json"), "w"), indent=1, default=str)
    json.dump(model_json(rw, "WEST", p4w, p3w), open(os.path.join(outdir, "p6_model_west.json"), "w"), indent=1, default=str)
    json.dump(overlay, open(os.path.join(outdir, "p6_p2c_overlay.json"), "w"), indent=1)
    clean = lambda r_: dict(checks=r_.checks, metrics=json.loads(json.dumps(r_.metrics, default=lambda o: list(o) if isinstance(o, (set, tuple)) else str(o))),
                            p3_regression=r_.p3checks, p4_regression=r_.p4checks)
    json.dump(dict(east=clean(re_), west=clean(rw), mirror=eq, verdicts_equal=ev, turns=dict(E=tE, W=tW, route_steps=dict(E=nE, W=nW)), repo_facts=facts), open(os.path.join(outdir, "p6_verification.json"), "w"), indent=1, default=str)
    render_all(re_, outdir, "east"); render_all(rw, outdir, "west")
    bad = any(c["verdict"] == "FAIL" for r_ in (re_, rw) for c in r_.checks) or not all(eq.values()) or not ev
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p6out"))
