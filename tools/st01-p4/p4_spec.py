"""ST-01 P4 secret-area spec (EAST candidates A = stacked below the royal hall, B = lateral east of the hall). PROPOSED coordinates.
Global coordinates as P1-P3: X east+, Z south+, Y layer index, inclusive ranges. The secret area is reached from the P3 hatch zone
(X -9..-5, Z 33..37, Y -22..-19) by a hidden stair that dives east under the hall's east wall.
Approved inputs (Q13-Q16, Q63-Q71, Q72-Q93, Q95-Q98) are NOT encoded here; the verifier owns them. The secret crypt (Q15) has NO approved size:
every crypt dimension below is a PROPOSED representative value, not an approval.
"""
import copy

def box(x0, x1, y0, y1, z0, z1): return [x0, x1, y0, y1, z0, z1]

def cells_to_boxes(cells):
    rows = {}
    for x, y, z in sorted(cells): rows.setdefault((x, y), []).append(z)
    runs = []
    for (x, y), zs in rows.items():
        a = b = zs[0]
        for z in zs[1:]:
            if z == b + 1: b = z
            else: runs.append((x, y, a, b)); a = b = z
        runs.append((x, y, a, b))
    grp = {}
    for x, y, a, b in runs: grp.setdefault((y, a, b), []).append(x)
    out = []
    for (y, a, b), xs in grp.items():
        xs.sort(); s = e = xs[0]
        for x in xs[1:]:
            if x == e + 1: e = x
            else: out.append(box(s, e, y, y, a, b)); s = e = x
        out.append(box(s, e, y, y, a, b))
    return out

class Frame:
    """Vault local frame: a = depth from the entrance interior face (0) to the rear wall (D-1), b = lateral offset from the axis."""
    def __init__(s, xe, sign, zc, F, D, Wd):
        s.xe, s.sg, s.zc, s.F, s.D, s.Wd = xe, sign, zc, F, D, Wd; s.half = (Wd - 1) // 2
    def x(s, a): return s.xe + s.sg * a
    def z(s, b): return s.zc + b
    def box(s, a0, a1, b0, b1, y0, y1):
        xs = sorted((s.x(a0), s.x(a1))); zs = sorted((s.z(b0), s.z(b1)))
        return box(xs[0], xs[1], s.F + y0, s.F + y1, zs[0], zs[1])
    def cell(s, a, b, y): return (s.x(a), s.F + y, s.z(b))

def vault(fr, el, sp, env):
    D, Wd, half = fr.D, fr.Wd, fr.half; ib = 8 if half == 12 else 7
    parts = []
    parts.append(fr.box(0, D - 1, -4, 4, 0, 9))                                   # centre zone: clear height 10
    for sgn in (1, -1):
        lo, hi = sorted((sgn * 5, sgn * ib)); parts.append(fr.box(0, D - 1, lo, hi, 0, 8))      # intermediate zone: 9
        lo, hi = sorted((sgn * (ib + 1), sgn * half)); parts.append(fr.box(0, D - 1, lo, hi, 0, 7))   # side zones: 8
    sp["VAULT"] = dict(kind="vault", boxes=parts)
    sp["VAULT_ENT"] = dict(kind="corr", boxes=[fr.box(-3, -1, -2, 2, 0, 5)])      # 5 wide, 6 high, through the 3-thick entrance wall
    env["VAULT_ENV"] = fr.box(-3 - 0, D + 2, -(half + 3), half + 3, -2, 11)
    # columns (Q80-Q88, Q96): corners of the 15x11 dais plan, 3x3 body, 7 high (base 1 + shaft 4 + capital 2), floor-founded
    ccs = [(D - 2, 7), (D - 2, -7), (D - 12, 7), (D - 12, -7)]
    colbody = set()
    for n, (ca, cb) in enumerate(ccs):
        body = [(a, b) for a in range(ca - 1, ca + 2) for b in range(cb - 1, cb + 2)]
        for a, b in body:
            for y in range(0, 3): colbody.add((a, b, y))
        el[f"COL{n}:base"] = dict(kind="column_base", boxes=[fr.box(ca - 1, ca + 1, cb - 1, cb + 1, 0, 0)])
        el[f"COL{n}:shaft"] = dict(kind="column_shaft", boxes=[fr.box(ca - 1, ca + 1, cb - 1, cb + 1, 1, 4)])
        c1 = {(a, b) for a in range(ca - 2, ca + 3) for b in range(cb - 2, cb + 3) if not (abs(a - ca) == 2 and abs(b - cb) == 2)}
        c2 = {(a, b) for a in range(ca - 2, ca + 3) for b in range(cb - 2, cb + 3)}
        cc = []
        for y, cs in ((5, c1), (6, c2)):
            for a, b in cs:
                if a > D - 1: continue                                                 # capital trimmed at the rear wall
                cc.append((fr.x(a), fr.F + y, fr.z(b)))
        el[f"COL{n}:cap"] = dict(kind="column_cap", boxes=cells_to_boxes(cc))
    # dais 15 x 11 x 3 (columns own the 2x2 corner overlap), front stair 7 wide / 5 run (2 outside + 3 inset)
    dcells = set()
    for a in range(D - 12, D - 1):
        for b in range(-7, 8):
            for y in range(3):
                if (a, b, y) in colbody: continue
                if D - 12 <= a <= D - 10 and abs(b) <= 3: continue                 # the stair owns these cells
                dcells.add((fr.x(a), fr.F + y, fr.z(b)))
    el["DAIS"] = dict(kind="dais", boxes=cells_to_boxes(dcells))
    scells = []
    for a, h in ((D - 14, 1), (D - 13, 1), (D - 12, 2), (D - 11, 2), (D - 10, 3)):
        for b in range(-3, 4):
            if a >= D - 12:
                continue
            for y in range(h): scells.append((fr.x(a), fr.F + y, fr.z(b)))
    # inset treads live inside the dais plan: heights 2,2,3 above the floor
    for a, h in ((D - 12, 2), (D - 11, 2), (D - 10, 3)):
        for b in range(-3, 4):
            for y in range(h): scells.append((fr.x(a), fr.F + y, fr.z(b)))
    el["DAIS_STAIR"] = dict(kind="dais_stair", boxes=cells_to_boxes(scells))
    # beams (thin, 1x1) linking the four column tops at y=F+7
    el["BEAMS"] = dict(kind="beam", boxes=[fr.box(D - 2, D - 2, -7, 7, 7, 7), fr.box(D - 12, D - 12, -7, 7, 7, 7),
                                           fr.box(D - 11, D - 3, 7, 7, 7, 7), fr.box(D - 11, D - 3, -7, -7, 7, 7)])
    # giant chest: base tier 7x5x1, body 5(front width) x 3(depth) x 2, gently arched lid layer x1; rear-centred on the dais
    el["CHEST_BASE"] = dict(kind="chest_base", boxes=[fr.box(D - 7, D - 3, -3, 3, 3, 3)])
    el["CHEST_BODY"] = dict(kind="chest_body", boxes=[fr.box(D - 6, D - 4, -2, 2, 4, 5)])
    el["CHEST_LID"] = dict(kind="chest_lid", boxes=[fr.box(D - 6, D - 4, -2, 2, 6, 6)])
    # irregular mineral piles: front approach (|b|>=3) and side strips (|b|>=9); heights 0..4; left/right volumes balanced by seed search
    def piles(seed):
        out = {1: [], -1: []}
        for sg in (1, -1):
            for a in range(0, D):
                for b in range(3, half + 1):
                    front = a <= D - 15
                    side = (a >= D - 14) and b >= 9
                    if not (front or side): continue
                    h = ((a * 5 + b * 3 + seed * (2 if sg > 0 else 3) + (a * b) % 3) % 6) - 1
                    h = max(0, min(4, h))
                    if (a + b + seed + (0 if sg > 0 else 1)) % 7 == 0: h = 0
                    for y in range(h): out[sg].append((fr.x(a), fr.F + y, fr.z(sg * b)))
        return out
    best = None
    for seed in range(60):
        o = piles(seed); va, vb = len(o[1]), len(o[-1]); im = abs(va - vb) / max(1, max(va, vb))
        if best is None or im < best[0]: best = (im, seed, o)
    for sg, nm in ((1, "PILES_P"), (-1, "PILES_N")): el[nm] = dict(kind="pile", boxes=cells_to_boxes(best[2][sg]))
    return dict(pile_seed=best[1], pile_imbalance=best[0])

def stair(axis, coords, lat, feet_first, head=4):
    out = []; tr = []
    for i, c in enumerate(coords):
        f = feet_first - i
        out.append(box(c, c, f, f + head - 1, lat[0], lat[1]) if axis == "x" else box(lat[0], lat[1], f, f + head - 1, c, c)); tr.append((c, f))
    return out, tr

def crypt(F, xi, zi, h, entry, sp, el, env, links_out, tag="CRYPT"):
    """Representative secret crypt (Q15): central sarcophagus + side burial niches. NO approved size/count: all values PROPOSED."""
    x0, x1 = xi; z0, z1 = zi
    sp[tag] = dict(kind="crypt", boxes=[box(x0, x1, F, F + h - 1, z0, z1)])
    env[tag + "_ENV"] = box(x0 - 3, x1 + 3, F - 2, F + h + 1, z0 - 3, z1 + 3)
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    el[tag + ":SARC"] = dict(kind="sarcophagus", boxes=[box(cx - 1, cx + 1, F, F + 1, cz - 2, cz + 2)])
    n = 0
    for k in range(3):
        xs = x0 + 1 + 4 * k
        links_out.extend([(tag, f"{tag}_NICHE_N{k}"), (tag, f"{tag}_NICHE_S{k}")])
        sp[f"{tag}_NICHE_N{k}"] = dict(kind="niche", boxes=[box(xs, xs + 2, F, F + 2, z0 - 2, z0 - 1)])
        sp[f"{tag}_NICHE_S{k}"] = dict(kind="niche", boxes=[box(xs, xs + 2, F, F + 2, z1 + 1, z1 + 2)])
    return cx, cz

def make_variant(var):
    sp, el, env, links, goals = {}, {}, {}, [], {}
    info = {}
    if var == "A":
        F = -37
        fr = Frame(xe=-17, sign=-1, zc=24, F=F, D=21, Wd=25)
        info.update(vault_interior=[-37, -17, 12, 36], F=F)
        b, tr = stair("x", list(range(-9, 0)), (33, 35), -23)
        sp["FA"] = dict(kind="stair", boxes=b, treads=tr, axis="x")
        sp["LA"] = dict(kind="landing", boxes=[box(0, 4, -32, -28, 32, 36)])
        b, tr = stair("z", list(range(31, 26, -1)), (1, 3), -33)
        sp["FB"] = dict(kind="stair", boxes=b, treads=tr, axis="z")
        sp["LB"] = dict(kind="landing", boxes=[box(0, 4, -37, -33, 22, 26)])
        sp["CV"] = dict(kind="corr", boxes=[box(-13, -1, -37, -32, 22, 26)])
        vinfo = vault(fr, el, sp, env)
        links += [("HALL", "FA"), ("FA", "LA"), ("LA", "FB"), ("FB", "LB"), ("LB", "CV"), ("CV", "VAULT_ENT"), ("VAULT_ENT", "VAULT")]
        sp["CR_ENT"] = dict(kind="corr", boxes=[box(5, 7, -37, -33, 23, 25)])
        crypt(-37, (8, 20), (17, 31), 7, None, sp, el, env, links)
        links += [("LB", "CR_ENT"), ("CR_ENT", "CRYPT")]
        pierce = box(-9, -5, -24, -19, 33, 37)
        goals.update(HATCH=(-9, -22, 36), LANDING_A=(2, -32, 34), LANDING_B=(2, -37, 24), VAULT_ENTRY=(-18, -37, 24), CRYPT_IN=(9, -37, 24))
        env_corr = ["FA", "LA", "FB", "LB", "CV", "CR_ENT"]
    else:
        F = -30
        fr = Frame(xe=6, sign=1, zc=26, F=F, D=25, Wd=21)
        info.update(vault_interior=[6, 30, 16, 36], F=F)
        b, tr = stair("x", list(range(-9, -2)), (33, 35), -23)
        sp["FA"] = dict(kind="stair", boxes=b, treads=tr, axis="x")
        sp["LA"] = dict(kind="landing", boxes=[box(-2, 2, -30, -26, 32, 36)])
        sp["CB1"] = dict(kind="corr", boxes=[box(-2, 2, -30, -25, 24, 31)])
        vinfo = vault(fr, el, sp, env)
        sp["VAULT_ENT"]["boxes"] = [fr.box(-3, -1, -2, 2, 0, 5)]
        links += [("HALL", "FA"), ("FA", "LA"), ("LA", "CB1"), ("CB1", "VAULT_ENT"), ("VAULT_ENT", "VAULT")]
        sp["CC"] = dict(kind="corr", boxes=[box(0, 2, -30, -26, 0, 23)])
        sp["CC3"] = dict(kind="corr", boxes=[box(3, 7, -30, -26, 0, 2)])
        crypt(-30, (8, 22), (-7, 5), 7, None, sp, el, env, links)
        links += [("CB1", "CC"), ("CC", "CC3"), ("CC3", "CRYPT")]
        pierce = box(-9, -5, -24, -19, 33, 37)
        goals.update(HATCH=(-9, -22, 36), LANDING_A=(0, -30, 34), VAULT_ENTRY=(7, -30, 26), CRYPT_IN=(10, -30, 1))
        env_corr = ["FA", "LA", "CB1", "CC", "CC3"]
    # corridor/stair structure envelopes (cavity + minimum wall 2) for the whole-structure extent measure
    for k in env_corr + ["VAULT_ENT"] + (["CR_ENT"] if var == "A" else []):
        bb = sp[k]["boxes"]; env[k + "_ENV"] = box(min(b[0] for b in bb) - 2, max(b[1] for b in bb) + 2, min(b[2] for b in bb) - 2, max(b[3] for b in bb) + 2,
                                                   min(b[4] for b in bb) - 2, max(b[5] for b in bb) + 2)
    D = fr.D
    goals["DAIS_FRONT"] = fr.cell(D - 8, 0, 3)
    goals["VAULT_AISLE_START"] = fr.cell(0, 0, 0)
    goals["CHEST_FRONT"] = [fr.cell(D - 8, b, 3) for b in range(-2, 3)]
    goals["STAIR_FOOT"] = fr.cell(D - 15, 0, 0)
    return dict(variant=var, side="EAST", spaces=sp, elements=el, envelopes=env, links=links, goals=goals, pierce=pierce,
                vault=dict(xe=fr.xe, sg=fr.sg, zc=fr.zc, F=fr.F, D=fr.D, Wd=fr.Wd, **vinfo), info=info)

def mirror_p4(m):
    def mb(b): return [-b[1], -b[0], b[2], b[3], b[4], b[5]]
    W = copy.deepcopy(m); W["side"] = "WEST"
    for s in W["spaces"].values():
        s["boxes"] = [mb(b) for b in s["boxes"]]
        if "treads" in s: s["treads"] = [(-c, f) if s["axis"] == "x" else (c, f) for c, f in s["treads"]]
    for e in W["elements"].values(): e["boxes"] = [mb(b) for b in e["boxes"]]
    W["envelopes"] = {k: mb(b) for k, b in W["envelopes"].items()}
    W["pierce"] = mb(W["pierce"])
    W["vault"]["xe"] = -W["vault"]["xe"]; W["vault"]["sg"] = -W["vault"]["sg"]
    def mc(c): return (-c[0], c[1], c[2])
    W["goals"] = {k: ([mc(c) for c in v] if isinstance(v, list) else mc(v)) for k, v in W["goals"].items()}
    return W
