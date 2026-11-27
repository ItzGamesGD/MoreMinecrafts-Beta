"""ST-01 P3 royal-hall spec (EAST candidate). Hand-authored PROPOSED coordinates; see the P3 report.
Global coordinates (same as P1/P2): X east+, Z south+ (entrance), Y layer index (Y=0 lowest aboveground layer), inclusive ranges.
Hall frame (east): centre (cx,cz)=(-21,21); interior X -37..-5, Z 5..37; entrance door in the NORTH wall (z 2..4); "rear" = SOUTH (z 37).
Hall-local coordinates used in comments: u = x - cx (east+), v = z - cz (south+, rear = +16).
Everything here is PROPOSED except where a Q-number is cited in the report; nothing is implemented.
"""
import copy

def box(x0, x1, y0, y1, z0, z1): return [x0, x1, y0, y1, z0, z1]

def cells_to_boxes(cells):
    """cells: iterable of (x,y,z) -> merged inclusive boxes (runs along z, then merged along x)."""
    rows = {}
    for x, y, z in sorted(cells): rows.setdefault((x, y), []).append(z)
    runs = []
    for (x, y), zs in rows.items():
        zs.sort(); a = b = zs[0]
        for z in zs[1:]:
            if z == b + 1: b = z
            else: runs.append((x, y, a, b)); a = b = z
        runs.append((x, y, a, b))
    out = {}
    for x, y, a, b in runs: out.setdefault((y, a, b), []).append(x)
    boxes = []
    for (y, a, b), xs in out.items():
        xs.sort(); s = e = xs[0]
        for x in xs[1:]:
            if x == e + 1: e = x
            else: boxes.append(box(s, e, y, y, a, b)); s = e = x
        boxes.append(box(s, e, y, y, a, b))
    return boxes

# ---------------------------------------------------------------- parameters (PROPOSED unless cited)
H = dict(cx=-21, cz=21, half=16,            # Q43: 33x33 clear interior (half=16 -> 2*16+1)
         floor=-22,                           # feet layer F of the hall floor (= P2 L3 floor); floor slab at F-1
         wall_h=8,                            # Q45
         rise=8, run=2,                       # Q46: 1 vertical per 2 horizontal; rise 8 gives the Q44 central height 16
         res=box(-40, -2, -24, -5, 2, 40),    # P2 HALL_RES (read-only context)
         door=box(-22, -20, -22, -20, 2, 4))  # P2 port T x wall (3 wide x 3 high, wall thickness 3)

def cell(side, s, i):
    """Cell of the interior ring: side N/S/E/W, s = 0..32 along the wall, i = inset from the interior face (0 = touching the wall)."""
    x0, x1 = H["cx"] - H["half"], H["cx"] + H["half"]; z0, z1 = H["cz"] - H["half"], H["cz"] + H["half"]
    if side == "N": return (x0 + s, z0 + i)
    if side == "S": return (x0 + s, z1 - i)
    if side == "W": return (x0 + i, z0 + s)
    return (x1 - i, z0 + s)

# Perimeter colonnade layout (Q47-Q52): along-wall centres s; E/W sides have 3 pillars, N (door) and S (dais) sides omit the centre pillar.
PILLAR_S = {"N": [7, 25], "S": [7, 25], "W": [7, 16, 25], "E": [7, 16, 25]}
SETBACK = 2                                   # Q50

def pillar_layers(F):
    """Per-layer footprint offsets (a = along, inset i) for one pillar; heights: base 1, shaft 3, capital 3, lintel 1 (Q61/Q62)."""
    L = {}
    L[("base", F)] = {(a, i) for a in range(-2, 3) for i in range(2, 6)}                     # 5 along x 4 deep: Q53 nominal 5x5 trimmed wallward (Q54)
    for k in range(3): L[("shaft", F + 1 + k)] = {(a, i) for a in range(-1, 2) for i in range(2, 5)}   # 3x3 body (Q48)
    c1 = {(a, i) for a in range(-2, 3) for i in range(1, 6) if not (abs(a) == 2 and i in (1, 5))}
    c2 = {(a, i) for a in range(-2, 3) for i in range(1, 6)}
    c3 = {(a, i) for a in range(-3, 4) for i in range(1, 7) if not (abs(a) == 3 and i in (1, 6))}    # max 7 along, wallward projection 1 (Q56/Q57)
    L[("cap", F + 4)] = c1; L[("cap", F + 5)] = c2; L[("cap", F + 6)] = c3                    # capital height 3 (Q58)
    return L

def make_pillars():
    F = H["floor"]; layers = pillar_layers(F)
    P = []
    for side, ss in PILLAR_S.items():
        for s in ss:
            P.append(dict(id=f"P_{side}{ss.index(s)+1}", side=side, s=s, centre=cell(side, s, 3)))
    # cell claims per (role, y)
    claims = {}
    for p in P:
        for (role, y), offs in layers.items():
            for a, i in offs:
                claims.setdefault((role, y, cell(p["side"], p["s"] + a, i)), []).append(p["id"])
    cen = {p["id"]: p["centre"] for p in P}
    owned = {}
    for (role, y, xz), ids in claims.items():
        if len(ids) == 1: owned[(role, y, xz)] = ids[0]; continue
        d = sorted((((xz[0] - cen[i][0]) ** 2 + (xz[1] - cen[i][1]) ** 2), i) for i in ids)
        if d[0][0] < d[1][0]: owned[(role, y, xz)] = d[0][1]        # nearest pillar keeps the shared cell
        # tie (diagonal between two corner-adjacent pillars) -> cell is dropped from both (chamfer)
    for p in P:
        for role in ("base", "shaft", "cap"):
            cs = [(xz[0], y, xz[1]) for (r, y, xz), i in owned.items() if i == p["id"] and r == role]
            p[role] = cells_to_boxes(cs)
        p["body"] = box(p["centre"][0] - 1, p["centre"][0] + 1, F + 1, F + 3, p["centre"][1] - 1, p["centre"][1] + 1)
    # lintel (Y = F+7, thickness 1, Q59): continuous over consecutive pillars on one side (pitch 9), interrupted at the door / dais (Q49)
    lint = []
    for side, ss in PILLAR_S.items():
        segs = []; cur = [ss[0]]
        for s in ss[1:]:
            if s - cur[-1] == 9: cur.append(s)
            else: segs.append(cur); cur = [s]
        segs.append(cur)
        for sg in segs:
            cs = [(cell(side, s, i)[0], F + 7, cell(side, s, i)[1]) for s in range(sg[0] - 1, sg[-1] + 2) for i in range(2, 5)]
            lint.append(dict(id=f"L_{side}{ss.index(sg[0])+1}", side=side, boxes=cells_to_boxes(cs), pillars=[f"P_{side}{ss.index(s)+1}" for s in sg]))
    return P, lint

def make_hall_east():
    F = H["floor"]; cx, cz = H["cx"], H["cz"]
    P, lint = make_pillars()
    el = {}                                    # element id -> dict(kind, boxes)
    def add(i, kind, boxes, **kw): el[i] = dict(kind=kind, boxes=boxes, **kw)
    for p in P:
        add(p["id"] + ":base", "pillar_base", p["base"], pillar=p["id"]); add(p["id"] + ":shaft", "pillar_shaft", p["shaft"], pillar=p["id"])
        add(p["id"] + ":cap", "pillar_cap", p["cap"], pillar=p["id"])
    for l in lint: add(l["id"], "lintel", l["boxes"], pillars=l["pillars"])
    # ---- central obelisk (PROPOSED): plinth 5x5x2, shaft 3x3x9, pyramidion 1x1x2; top at F+12
    add("OBELISK:plinth", "obelisk", [box(cx - 2, cx + 2, F, F + 1, cz - 2, cz + 2)])
    add("OBELISK:shaft", "obelisk", [box(cx - 1, cx + 1, F + 2, F + 10, cz - 1, cz + 1)])
    add("OBELISK:tip", "obelisk", [box(cx, cx, F + 11, F + 12, cz, cz)])
    # ---- rear sarcophagus dais (Q10/Q32): 11 wide x 6 deep x 2 high against the rear wall, 9-wide 2-deep frontal tread
    add("DAIS", "dais", [box(cx - 5, cx + 5, F, F + 1, cz + 11, cz + 16)])
    add("DAIS:tread", "dais", [box(cx - 4, cx + 4, F, F, cz + 9, cz + 10)])
    add("SARCOPHAGUS", "sarcophagus", [box(cx - 1, cx + 1, F + 2, F + 3, cz + 13, cz + 16)])
    add("CHEST_PRIMARY", "chest_primary", [box(cx, cx, F + 2, F + 2, cz + 12, cz + 12)])
    # ---- recesses cut into the 3-thick walls (all depths leave >= 1 wall block)
    rec = {}
    def recess(i, kind, b): rec[i] = dict(kind=kind, box=b)
    xe = cx + H["half"]; xw = cx - H["half"]; zn = cz - H["half"]; zs = cz + H["half"]
    recess("ALCOVE_E", "alcove", box(xe + 1, xe + 2, F, F + 4, cz + 3, cz + 6))           # Q34 secondary-chest alcove (inner / pyramid-axis side), depth 2
    add("ALCOVE_E:col1", "alcove_col", [box(xe + 1, xe + 1, F, F + 3, cz + 3, cz + 3)])
    add("ALCOVE_E:col2", "alcove_col", [box(xe + 1, xe + 1, F, F + 3, cz + 6, cz + 6)])
    add("CHEST_SECONDARY", "chest_secondary", [box(xe + 2, xe + 2, F, F, cz + 4, cz + 5)])
    recess("NICHE_W", "statue_niche", box(xw - 2, xw - 1, F, F + 5, cz + 3, cz + 6))        # Q29 guardian-statue niche (mirror of the alcove)
    add("NICHE_W:col1", "alcove_col", [box(xw - 1, xw - 1, F, F + 3, cz + 3, cz + 3)])
    add("NICHE_W:col2", "alcove_col", [box(xw - 1, xw - 1, F, F + 3, cz + 6, cz + 6)])
    add("STATUE_W", "statue", [box(xw - 2, xw - 2, F, F + 4, cz + 4, cz + 5)])
    recess("NICHE_E_A", "statue_niche", box(xe + 1, xe + 2, F, F + 5, cz - 6, cz - 3))     # Q29 statue niches in the gaps between pillars (no pillar in front)
    add("NICHE_E_A:col1", "alcove_col", [box(xe + 1, xe + 1, F, F + 3, cz - 6, cz - 6)])
    add("NICHE_E_A:col2", "alcove_col", [box(xe + 1, xe + 1, F, F + 3, cz - 3, cz - 3)])
    add("STATUE_E_A", "statue", [box(xe + 2, xe + 2, F, F + 4, cz - 5, cz - 4)])
    recess("NICHE_W_A", "statue_niche", box(xw - 2, xw - 1, F, F + 5, cz - 6, cz - 3))
    add("NICHE_W_A:col1", "alcove_col", [box(xw - 1, xw - 1, F, F + 3, cz - 6, cz - 6)])
    add("NICHE_W_A:col2", "alcove_col", [box(xw - 1, xw - 1, F, F + 3, cz - 3, cz - 3)])
    add("STATUE_W_A", "statue", [box(xw - 2, xw - 2, F, F + 4, cz - 5, cz - 4)])
    # braziers: perimeter stone braziers in 1-deep recesses (Q31), outside the combat clearance
    for i, b in {"BRAZ_NW": box(cx - 5, cx - 4, F, F + 2, zn - 1, zn - 1), "BRAZ_NE": box(cx + 4, cx + 5, F, F + 2, zn - 1, zn - 1),
                 "BRAZ_SW": box(cx - 14, cx - 13, F, F + 2, zs + 1, zs + 1), "BRAZ_SE": box(cx + 13, cx + 14, F, F + 2, zs + 1, zs + 1)}.items():
        recess(i, "brazier_recess", b)
        add(i + ":bowl", "brazier", [box(b[0], b[1], F, F, b[4], b[5])])
    # ---- decoration reservations (surface only; no occupancy of the clear interior)
    decor = {
        "RELIEF_BAND": dict(kind="relief", y=[F + 3, F + 6], depth=1, note="hieroglyphic-style relief band on the wall faces (Q29), depth 1 recess into the wall"),
        "FLOOR_FIELD": dict(kind="floor", box=[cx - 8, cx + 8, cz - 8, cz + 8], note="Q30 flat ceremonial centre field 17x17 (telegraph-distinguishing geometry), no height"),
        "FLOOR_TILES": dict(kind="floor", ring=[11, 16], note="Q30 orderly perimeter tile band, no height"),
    }
    hatch = box(cx + 12, cx + 16, F, F + 3, cz + 12, cz + 16)           # P4 secret-access candidate: rear inner corner, 5x5 floor zone, 4 clear
    spawn = {"BOSS_S": (0, 6), "BOSS_N": (0, -6), "BOSS_E": (6, 0), "BOSS_W": (-6, 0)}   # (u,v) emergence candidates (PROPOSED test points)
    return dict(side="EAST", hall=copy.deepcopy(H), elements=el, recesses=rec, decor=decor, hatch=hatch, spawn=spawn,
                chest_primary_access=[(cx, F + 2, cz + 11), (cx - 1, F + 2, cz + 12), (cx + 1, F + 2, cz + 12)],
                chest_secondary_access=[(xe + 1, F, cz + 4), (xe + 1, F, cz + 5)])

def mirror_spec(sp):
    """Exact X mirror (x -> -x) of an east spec: used for the west candidate; verified independently by the same verifier."""
    def mb(b): return [-b[1], -b[0], b[2], b[3], b[4], b[5]]
    W = copy.deepcopy(sp); W["side"] = "WEST"
    H2 = W["hall"]; H2["cx"] = -H2["cx"]; H2["res"] = mb(H2["res"]); H2["door"] = mb(H2["door"])
    for k, e in W["elements"].items(): e["boxes"] = [mb(b) for b in e["boxes"]]
    for k, r in W["recesses"].items(): r["box"] = mb(r["box"])
    W["decor"]["FLOOR_FIELD"]["box"] = [-W["decor"]["FLOOR_FIELD"]["box"][1], -W["decor"]["FLOOR_FIELD"]["box"][0]] + W["decor"]["FLOOR_FIELD"]["box"][2:]
    W["hatch"] = mb(W["hatch"])
    W["spawn"] = {k: (-u, v) for k, (u, v) in W["spawn"].items()}
    W["chest_primary_access"] = [(-x, y, z) for x, y, z in W["chest_primary_access"]]
    W["chest_secondary_access"] = [(-x, y, z) for x, y, z in W["chest_secondary_access"]]
    return W
