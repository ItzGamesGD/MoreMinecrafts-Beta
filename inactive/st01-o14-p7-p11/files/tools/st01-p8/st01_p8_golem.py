"""ST-01 P8 static golem feasibility on the P6/P7 world: does a (scaled) iron-golem-sized body pass the real hall and which stop points are safe vs the 12-block cover.
Vanilla iron golem base size 1.4 x 2.7 (width x height). Path node size follows the vanilla ground-navigation rule floor(size + 1) per axis
(from knowledge of the vanilla algorithm; NOT verified against the 26.3 sources - no decompiled sources are available offline => UNVERIFIED in-game)."""
import sys, os, math
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
for d in ("st01-p8", "st01-p7", "st01-p6", "st01-p5", "st01-p4", "st01-p3", "st01-p2", "st01-p1"): sys.path.insert(0, os.path.join(TOOLS, d))
import st01_p7_geometry as G
V, C = G.V, G.C
BASE_W, BASE_H = 1.4, 2.7
SCALES = (0.4, 0.5, 0.6, 0.7, 0.8, 1.0, 1.5)       # exploration grid only; none is a proposal

class R:
    def __init__(s): s.checks = []; s.metrics = {}
    def add(s, cid, title, verdict, ev): s.checks.append(dict(id=cid, title=title, verdict=verdict, evidence=ev))

def nav_size(scale): return (math.floor(BASE_W * scale + 1), math.floor(BASE_H * scale + 1))

def analyse(side):
    g = G.analyse(side); r = R(); r6 = g.r6; p3 = g.p3
    cl = r6.closed_solid; F = p3["hall"]["floor"]; stC = C.standable(cl)
    cells = r6.cells; xs = [c[0] for c in cells]; zs = [c[2] for c in cells]; x0, x1, z0, z1 = min(xs), max(xs) + 1, min(zs), max(zs) + 1
    starts = {k: v for k, v in g.starts.items() if k != "CHEST_APPROACH"}      # dais-top start cannot host a wide body
    classes = g.classes; rows = []
    for s in SCALES:
        w, h = nav_size(s); hw = BASE_W * s / 2
        reach = {}
        for sn, sc in starts.items():
            rc = V.agent_reach(cl, sc, w, h, max_drop=1)
            reach[sn] = sum(1 for i, cl_ in classes.items() if any((c[0] - dx, c[1], c[2] - dz) in rc for c, _ in cl_ for dx in range(w) for dz in range(w)))
        # stop safety per zone: (a) body disjoint from the cut (centre-to-cut distance - half width >= 0); (b) body fully unsupported over the cut only if centred deep inside
        safe_a = {k: round(v["min_dist_to_cover"] - hw, 3) for k, v in g.metrics["zones"].items()}
        deep = (x1 - x0 - 2 * hw) >= 0 and (z1 - z0 - 2 * hw) >= 0
        rows.append(dict(scale=s, bbox=(round(BASE_W * s, 2), round(BASE_H * s, 2)), nav=(w, h), reach_min=min(reach.values()), reach=reach, margin_to_cut=safe_a, can_be_fully_unsupported_over_cut=deep))
    r.metrics["scales"] = rows
    allz = len(classes)
    ok = all(row["reach_min"] == allz for row in rows if row["scale"] <= 1.0)
    r.add("GG1", "Scaled iron-golem body (scale 0.4..1.0) reaches every arrival-zone class from every hall start (HALL_DOOR, 4 emergence candidates) with the vanilla path-node footprint; scale 1.5 reported separately",
          "PASS" if ok else "FAIL", "; ".join(f"s={row['scale']}: bbox {row['bbox'][0]}x{row['bbox'][1]}, node {row['nav'][0]}x{row['nav'][0]}x{row['nav'][1]}, zones reached (min over starts) {row['reach_min']}/{allz}" for row in rows))
    # passages: main-route clear sections from the P2 model (req_w/req_h) incl. exit threshold T and hall door
    p2 = r6.p2; passages = {}
    for k, sp in p2["spaces"].items():
        if sp.get("kind") == "main" and "req_w" in sp: passages[k] = (sp["req_w"], sp["req_h"])
    dr = p3["hall"]["door"]; passages["HALL_DOOR"] = (dr[1] - dr[0] + 1, dr[3] - dr[2] + 1)
    mw = min(v[0] for v in passages.values()); mh = min(v[1] for v in passages.values())
    fits = {s: (nav_size(s)[0] <= mw and nav_size(s)[1] <= mh) for s in SCALES}
    r.metrics["passages"] = passages
    r.add("GG2", "Main-route / hall-door clear sections (from the P2/P3 models) vs golem node footprint: narrowest section and the largest scale whose node size fits it", "PASS", f"sections {passages}; narrowest clear {mw} wide x {mh} high; fits per scale {fits}; the native-size golem (scale 1.0, node {nav_size(1.0)[0]}x{nav_size(1.0)[0]}x{nav_size(1.0)[1]}) {'fits' if fits[1.0] else 'does NOT fit'}")
    r.metrics["narrowest"] = (mw, mh)
    # stop-point safety relation
    z0m = g.metrics["zones"]["Z0"]["min_dist_to_cover"]
    maxs_z0 = round(2 * z0m / BASE_W, 3)
    r.add("GG3", "Stop-point safety: largest scale whose body stays fully off the cut when its centre stands in the nearest zone Z0 (centre-to-cut 0.5) = 2*d/1.4; and the cut (4x3) can host a fully unsupported body for every scale <= 2.14, so stop points must be outside the cut",
          "PASS" if abs(maxs_z0 - 0.714) < 0.001 and all(row["can_be_fully_unsupported_over_cut"] for row in rows if row["scale"] <= 1.5) else "FAIL",
          f"Z0 nearest centre-to-cut {z0m}; max scale with body disjoint from the cut at Z0 = {maxs_z0}; partially overhanging bodies stay supported by the remaining floor in vanilla physics (UNVERIFIED in-game), fully unsupported bodies fall")
    r.metrics["max_scale_Z0"] = maxs_z0
    r.g = g
    return r

if __name__ == "__main__":
    for sd in ("E", "W"):
        r = analyse(sd); print(sd, [(c["id"], c["verdict"]) for c in r.checks])
        for c in r.checks: print("  ", c["id"], c["evidence"][:600])
