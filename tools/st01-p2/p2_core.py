"""ST-01 P2 underground verification engine (design-only; independent of the P1 verifier).

A *model* is a dict:
  spaces:  {id: {name, level, kind, boxes[[x0,x1,y0,y1,z0,z1],...], req_w, req_h, axis, treads?}}
           kind in main|optional|stair|room ; boxes are inclusive voxel ranges
  links:   [(a,b), ...]   declared direct connections (face contact REQUIRED)
  res:     {id: {name, box}}   reserved volumes (not carved)
  ports:   [(space_id, res_id, overlap_box)]  declared penetration of a reserved volume by a space
  p1:      {id: box}      read-only P1 context voids (already mirrored for the model)
All coordinates are PROPOSED unless a Q-number says otherwise (see report).
"""
import ast, re, itertools
from collections import deque
import numpy as np

GX = (-64, 64); GZ = (-64, 64); GY = (-60, 40)
NX, NZ, NY = GX[1]-GX[0]+1, GZ[1]-GZ[0]+1, GY[1]-GY[0]+1
def ix(x): return x - GX[0]
def iy(y): return y - GY[0]
def iz(z): return z - GZ[0]
MIN_HEAD = 3          # PROPOSED minimum walked headroom
MIN_WALL = 2          # PROPOSED minimum face-to-face wall between unconnected cavities
COVER = 2             # PROPOSED: cavity top <= -COVER
COVER_UNDER_HALL = 3  # P1 handoff: under surface hall zone top <= -3
HALL_ZONE = ((-18, 18), (-6, 18))   # P1 S04 plan footprint (X, Z)

def load_p1_spaces(p1_path):
    """Read the P1 SPACES literal without importing/modifying the P1 model."""
    src = open(p1_path).read()
    m = re.search(r"^SPACES = (\{.*?^\})", src, re.S | re.M)
    d = ast.literal_eval(m.group(1))
    return {k: [v[2][0], v[2][1], v[3][0], v[3][1], v[4][0], v[4][1]] for k, v in d.items()}

def mirror_box(b): return [-b[1], -b[0], b[2], b[3], b[4], b[5]]

def build(model):
    solid = np.ones((NX, NY, NZ), bool)
    owner = np.full((NX, NY, NZ), -1, np.int16)
    res = np.full((NX, NY, NZ), -1, np.int16)
    ids = list(model["spaces"]) + ["P1:" + k for k in model["p1"]]
    sidx = {k: i for i, k in enumerate(ids)}
    overlaps = []
    def carve(name, box):
        x0, x1, y0, y1, z0, z1 = box
        s = (slice(ix(x0), ix(x1)+1), slice(iy(y0), iy(y1)+1), slice(iz(z0), iz(z1)+1))
        prev = owner[s]
        bad = prev[(prev >= 0) & (prev != sidx[name])]
        if bad.size: overlaps.append((name, sorted({ids[i] for i in set(bad.tolist())}), int(bad.size)))
        owner[s] = sidx[name]; solid[s] = False
    for k, b in model["p1"].items(): carve("P1:" + k, b)
    for k, sp in model["spaces"].items():
        for b in sp["boxes"]: carve(k, b)
    rids = list(model["res"]); ridx = {k: i for i, k in enumerate(rids)}
    for k, r in model["res"].items():
        x0, x1, y0, y1, z0, z1 = r["box"]
        res[ix(x0):ix(x1)+1, iy(y0):iy(y1)+1, iz(z0):iz(z1)+1] = ridx[k]
    return dict(solid=solid, owner=owner, res=res, ids=ids, sidx=sidx, rids=rids, ridx=ridx, overlaps=overlaps, model=model)

def standable(solid):
    free = ~solid
    st = np.zeros_like(solid)
    st[:, 1:-2, :] = free[:, 1:-2, :] & free[:, 2:-1, :] & solid[:, :-3, :]
    return st

def bfs(G, start, st, blocked=None):
    solid = G["solid"]
    seen = {start}; dq = deque([start])
    while dq:
        x, y, z = dq.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, zz = x+dx, z+dz
            if not (GX[0] < xx < GX[1] and GZ[0] < zz < GZ[1]): continue
            for dy in (0, 1, -1):
                yy = y+dy
                if not st[ix(xx), iy(yy), iz(zz)]: continue
                if dy == 1 and solid[ix(x), iy(y+2), iz(z)]: continue
                if blocked is not None and blocked[ix(xx), iy(yy), iz(zz)]: continue
                n = (xx, yy, zz)
                if n not in seen: seen.add(n); dq.append(n)
                break
    return seen

def boxes_of(model):
    out = []
    for k, sp in model["spaces"].items():
        for b in sp["boxes"]: out.append((k, b))
    for k, b in model["p1"].items(): out.append(("P1:" + k, b))
    return out

def face_to_face_gap(a, b):
    """If the boxes overlap on exactly two axes, return (axis, gap) along the third; else None."""
    ov = []; gp = []
    for k in range(3):
        a0, a1 = a[2*k], a[2*k+1]; b0, b1 = b[2*k], b[2*k+1]
        o = min(a1, b1) - max(a0, b0) + 1
        ov.append(o)
    pos = [k for k in range(3) if ov[k] > 0]
    if len(pos) == 2:
        k = [k for k in range(3) if ov[k] <= 0][0]
        return "XYZ"[k], -ov[k]          # gap = blocks of solid between (0 = touching)
    return None

def analyse(model, start, port_goal, p1_hall_keepout=True):
    G = build(model); solid, owner, res, ids = G["solid"], G["owner"], G["res"], G["ids"]
    st = standable(solid)
    R = {"checks": [], "metrics": {}}
    def check(cid, title, verdict, ev): R["checks"].append(dict(id=cid, title=title, verdict=verdict, evidence=ev))
    sp = model["spaces"]
    p2ids = [i for i, k in enumerate(ids) if not k.startswith("P1:")]

    # ---- E: horizontal extension beyond the pyramid projection (cavities and reservations)
    carved = ~solid
    p2mask = np.isin(owner, p2ids)
    xs = np.arange(GX[0], GX[1]+1)[:, None, None]; zs = np.arange(GZ[0], GZ[1]+1)[None, None, :]
    outside = (np.abs(xs) > 40) | (np.abs(zs) > 40)
    out_cav = int((p2mask & outside).sum())
    out_res = int(((res >= 0) & outside[:, :, :].repeat(NY, 1)).sum()) if False else int(((res >= 0) & ((np.abs(xs) > 40) | (np.abs(zs) > 40))).sum())
    pts = np.argwhere(p2mask)
    cx0, cx1 = int(pts[:, 0].min()+GX[0]), int(pts[:, 0].max()+GX[0]); cz0, cz1 = int(pts[:, 2].min()+GZ[0]), int(pts[:, 2].max()+GZ[0])
    cy0, cy1 = int(pts[:, 1].min()+GY[0]), int(pts[:, 1].max()+GY[0])
    rp = np.argwhere(res >= 0)
    rx0, rx1 = int(rp[:, 0].min()+GX[0]), int(rp[:, 0].max()+GX[0]); rz0, rz1 = int(rp[:, 2].min()+GZ[0]), int(rp[:, 2].max()+GZ[0]); ry0 = int(rp[:, 1].min()+GY[0])
    ext = max(0, -min(cx0, rx0, cz0, rz0)-40, max(cx1, rx1, cz1, rz1)-40)
    # analytic cross-check (sum of box volumes outside +-40)
    def vol_out(b):
        x0, x1, y0, y1, z0, z1 = b
        fx = (x1-x0+1); fz = (z1-z0+1); fy = (y1-y0+1)
        inx = max(0, min(x1, 40)-max(x0, -40)+1); inz = max(0, min(z1, 40)-max(z0, -40)+1)
        return fy*(fx*fz - inx*inz)
    ana_out = sum(vol_out(b) for k, sps in sp.items() for b in sps["boxes"])
    R["metrics"].update(dict(cavity_x=[cx0, cx1], cavity_z=[cz0, cz1], cavity_y=[cy0, cy1], reserve_x=[rx0, rx1], reserve_z=[rz0, rz1],
        lowest_cavity_Y=cy0, lowest_reserved_Y=ry0, ext_blocks=ext, cavity_voxels_outside_pm40=out_cav, reserved_voxels_outside_pm40=out_res,
        analytic_cavity_outside=ana_out, p2_cavity_voxels=int(p2mask.sum())))
    check("X1", "Cavities and reserved volumes stay within the pyramid projection X/Z +-40 (D01-B: extension only if needed)",
          "PASS" if out_cav == 0 and out_res == 0 and ext == 0 else "FAIL",
          f"max excess {ext} blocks; cavity voxels outside {out_cav}; reserved voxels outside {out_res}; analytic outside-volume {ana_out} (must equal voxel count {out_cav}); cavity X[{cx0},{cx1}] Z[{cz0},{cz1}], reserved X[{rx0},{rx1}] Z[{rz0},{rz1}]")
    if ana_out != out_cav: check("X1b", "analytic vs voxel outside-volume mismatch", "FAIL", f"{ana_out} != {out_cav}")

    # ---- C: voxel-level 3D overlaps between different spaces, and with reserved volumes
    allowed = {(a, b) for a, b, _ in model["ports"]}
    bad_ov = [o for o in G["overlaps"]]
    rv = []
    for rk, ri in G["ridx"].items():
        m = res == ri
        hit = owner[m]; hit = hit[hit >= 0]
        names = sorted({ids[i] for i in set(hit.tolist())})
        for n in names:
            if (n, rk) in allowed:
                cnt = int((hit == G["sidx"][n]).sum())
                declared = [o for a, r_, o in model["ports"] if a == n and r_ == rk][0]
                dv = (declared[1]-declared[0]+1)*(declared[3]-declared[2]+1)*(declared[5]-declared[4]+1)
                if cnt != dv: rv.append((n, rk, f"port overlap {cnt} != declared {dv}"))
            else: rv.append((n, rk, "undeclared overlap"))
    check("C1", "No voxel shared by two different cavities (3D, Y included), P1 context included",
          "PASS" if not bad_ov else "FAIL", f"overlaps: {bad_ov}")
    check("C2", "Reserved volumes (royal hall, secret candidates) free of cavities except declared entrance ports",
          "PASS" if not rv else "FAIL", f"violations: {rv}; declared ports: {[(a, r, o) for a, r, o in model['ports']]}")

    # ---- C3: face contacts == declared links (voxel adjacency)
    pairs = set()
    for ax in range(3):
        a = [slice(None)]*3; b = [slice(None)]*3
        a[ax] = slice(0, -1); b[ax] = slice(1, None)
        oa, ob = owner[tuple(a)], owner[tuple(b)]
        m = (oa >= 0) & (ob >= 0) & (oa != ob)
        for u, v in set(zip(oa[m].tolist(), ob[m].tolist())): pairs.add(frozenset((ids[u], ids[v])))
    declared = {frozenset(l) for l in model["links"]}
    p1ctx = {frozenset((a, b)) for a, b in model.get("p1_links", [])}
    undecl = sorted(tuple(sorted(p)) for p in pairs - declared - p1ctx if not all(i.startswith("P1:") for i in p))
    missing = sorted(tuple(sorted(p)) for p in declared - pairs)
    check("C3", "Every face contact between cavities is a declared connection and every declared connection has real face contact",
          "PASS" if not undecl and not missing else "FAIL", f"undeclared contacts {undecl}; declared-without-contact {missing}; {len(declared)} declared links")

    # ---- C4: wall thickness between unconnected cavities (box face-to-face)
    bl = boxes_of(model); thin = []
    for (ka, ba), (kb, bb) in itertools.combinations(bl, 2):
        if ka == kb: continue
        if frozenset((ka, kb)) in declared or frozenset((ka, kb)) in p1ctx: continue
        if ka.startswith("P1:") and kb.startswith("P1:"): continue
        ff = face_to_face_gap(ba, bb)
        if ff and ff[1] < MIN_WALL: thin.append((ka, kb, ff[0], ff[1]))
    check("C4", f"Face-to-face wall/slab between unconnected cavities >= {MIN_WALL} blocks (all axes, P1 context included)",
          "PASS" if not thin else "FAIL", f"thin: {sorted(set(thin))[:12]}")

    # ---- cover / depth
    cov = []
    for (k, b) in bl:
        if k.startswith("P1:"): continue
        x0, x1, y0, y1, z0, z1 = b
        under = not (x1 < HALL_ZONE[0][0] or x0 > HALL_ZONE[0][1] or z1 < HALL_ZONE[1][0] or z0 > HALL_ZONE[1][1])
        lim = -(COVER_UNDER_HALL if under else COVER)
        if y1 > lim: cov.append((k, y1, lim))
    check("C5", f"Cover above cavities: top <= -{COVER} (<= -{COVER_UNDER_HALL} where the plan overlaps the surface hall zone X-18..18 Z-6..18)",
          "PASS" if not cov else "FAIL", f"violations {cov}")
    for k, r in model["res"].items():
        pass

    # ---- reachability
    S = bfs(G, start, st)
    goals = model["goals"]
    miss = [g for g, c in goals.items() if c not in S]
    check("R1", "Every program space (incl. optional rooms and the royal-hall entrance port) reachable on foot from the P1 landing",
          "PASS" if not miss else "FAIL", f"{len(goals)} goals; unreachable {miss}")
    opt_mask = np.zeros_like(solid)
    for k, s in sp.items():
        if s["kind"] == "optional":
            opt_mask |= (owner == G["sidx"][k])
    S2 = bfs(G, start, st, blocked=opt_mask)
    mains = [g for g, c in goals.items() if g.startswith("MAIN:")]
    miss2 = [g for g in mains if goals[g] not in S2]
    check("R2", "Royal hall port reachable with ALL optional rooms/branches blocked (optional exploration not required)",
          "PASS" if not miss2 and goals[port_goal] in S2 else "FAIL", f"main goals missing when optional blocked: {miss2}")
    # return from every optional room: undirected BFS from room cell reaches start and the main route
    back = []
    for g, c in goals.items():
        if g.startswith("OPT:"):
            Sr = bfs(G, c, st)
            if start not in Sr or goals[port_goal] not in Sr: back.append(g)
    check("R3", "Return path from every optional room to the main route / start exists",
          "PASS" if not back else "FAIL", f"rooms without return: {back}")

    # ---- widths / heights (actual clear voxels)
    wrows = []; wbad = []
    for k, s in sp.items():
        if not s.get("req_w") and not s.get("req_h"): continue
        ax = s.get("axis", "x")
        mnw = 999; mnh = 999
        cells = [(x, y, z) for (x, y, z) in S if owner[ix(x), iy(y), iz(z)] == G["sidx"][k]]
        if not cells: wbad.append((k, "no walkable cells")); continue
        by = {}
        for (x, y, z) in cells:
            c = x if ax == "x" else z
            by.setdefault(c, {}).setdefault(y, []).append(z if ax == "x" else x)
        for c, ys in by.items():
            best = 0
            for y, ls in ys.items():
                ls = sorted(set(ls)); run = 1; b = 1
                for a, bb in zip(ls, ls[1:]):
                    run = run+1 if bb == a+1 else 1; b = max(b, run)
                best = max(best, b)
            mnw = min(mnw, best)
        for (x, y, z) in cells:
            h = 0; yy = y
            while yy < GY[1] and not solid[ix(x), iy(yy), iz(z)] and h < 40: h += 1; yy += 1
            mnh = min(mnh, h)
        rw = s.get("req_w", 0); rh = s.get("req_h", 0)
        wrows.append((k, rw, mnw, rh, mnh))
        if mnw < rw or mnh < rh: wbad.append((k, f"width {mnw}/{rw} headroom {mnh}/{rh}"))
    R["metrics"]["width_rows"] = wrows
    check("W1", "Actual clear passage width (contiguous walkable cells across the corridor, per cross-section) and walked headroom meet Q40/Q41 targets",
          "PASS" if not wbad else "FAIL", "; ".join(f"{k}: w {w}>={rw}, h {h}>={rh}" for k, rw, w, rh, h in wrows) + f"; violations {wbad}")

    # ---- stairs
    stairs = []; sbad = []
    for k, s in sp.items():
        if s["kind"] != "stair": continue
        t = s["treads"]; Fs = [f for _, f in t]
        steps = all(abs(Fs[i]-Fs[i+1]) == 1 for i in range(len(Fs)-1))
        rise = len(t) if steps else -1     # first tread is one below the upper floor, so risers == treads
        run = len(t)
        stairs.append((k, rise, run, steps, s["axis"]))
        if not steps: sbad.append(k)
    flights = model["flights"]; fbad = []
    for name, (fa, fb, rise_req) in flights.items():
        tot = sum(r for k, r, run, ok, ax in stairs if k in (fa, fb))
        axes = {ax for k, r, run, ok, ax in stairs if k in (fa, fb)}
        if tot != rise_req or len(axes) != 2: fbad.append((name, tot, axes))
    check("S1", "Stairs: 1:1 gradient (every tread rises exactly 1), each connection is two flights with different axes (one turn) and the planned total rise",
          "PASS" if not sbad and not fbad else "FAIL", f"flights {stairs}; connections {flights}; bad {sbad} {fbad}")
    landings = []
    for name, lk in model["landings"].items():
        b = sp[lk]["boxes"][0]; landings.append((name, b[1]-b[0]+1, b[5]-b[4]+1))
    lbad = [l for l in landings if min(l[1], l[2]) < 7]
    check("S2", "Landings are broad (both plan dimensions >= 7, wider than the 7-wide main passage)", "PASS" if not lbad else "FAIL", f"landings (name, X, Z): {landings}")

    # ---- Q40/Q41/Q42 sequence on the L3 approach
    seq = model["approach_seq"]; ok = True; ev = []
    for k, w_req in seq:
        r = [x for x in wrows if x[0] == k]
        ev.append((k, r[0][2] if r else None)); ok &= bool(r) and r[0][2] >= w_req
    hs = [x[4] for x in wrows if x[0] in [k for k, _ in seq]]
    T = sp[model["threshold"]]; tlen = model["threshold_len"]
    check("Q1", "Q40-Q42: 7-wide passage -> 5-wide royal approach -> 3-wide threshold of 5..7 blocks; ceiling lowers with narrowing; antechamber tall",
          "PASS" if ok and 5 <= tlen <= 7 and hs == sorted(hs, reverse=True) and model["antechamber_h"] >= 10 else "FAIL",
          f"widths {ev}; walked headroom along approach {hs} (non-increasing); threshold length {tlen} (cavity + hall-wall port); antechamber clear height {model['antechamber_h']}")

    # ---- reserved royal hall sizes (Q43-Q45)
    rh = model["res"]["HALL_RES"]["box"]; ri = model["res"]["HALL_INT"]["box"]
    isz = (ri[1]-ri[0]+1, ri[5]-ri[4]+1, ri[3]-ri[2]+1)
    wall = (ri[0]-rh[0], rh[1]-ri[1], ri[4]-rh[4], rh[5]-ri[5], ri[2]-rh[2], rh[3]-ri[3])
    check("H1", "Royal hall reservation contains the Q43/Q44 clear interior 33x33 and 16 high, with >=3 lateral and >=2 vertical structural thickness (PROPOSED thickness)",
          "PASS" if isz[0] >= 33 and isz[1] >= 33 and isz[2] >= 16 and min(wall[:4]) >= 3 and min(wall[4:]) >= 2 else "FAIL",
          f"interior {isz[0]}x{isz[1]}x{isz[2]} at X[{ri[0]},{ri[1]}] Z[{ri[4]},{ri[5]}] Y[{ri[2]},{ri[3]}]; reservation X[{rh[0]},{rh[1]}] Z[{rh[4]},{rh[5]}] Y[{rh[2]},{rh[3]}]; shell thickness W/E/N/S/floor/roof = {wall}")
    # ---- stagger (Q37): level centroids
    cent = {}
    for lv in ("L1", "L2", "L3"):
        pts_ = [(x, z) for k, s in sp.items() if s["level"] == lv for b in s["boxes"] for x in (b[0], b[1]) for z in (b[4], b[5])]
        cent[lv] = (float(np.mean([p[0] for p in pts_])), float(np.mean([p[1] for p in pts_])))
    R["metrics"]["centroids"] = cent
    xs_ = [cent[l][0] for l in ("L1", "L2", "L3")]
    check("Q2", "Q37: L1/L2/L3 are horizontally staggered (distinct plan centres, monotone drift) - exact centres are PROPOSED",
          "PASS" if (xs_[0] > xs_[1] > xs_[2] or xs_[0] < xs_[1] < xs_[2]) else "FAIL", f"plan bounding-corner centroids {cent}")
    R["reach"] = len(S); R["G"] = G; R["st"] = st; R["S"] = S
    return R
