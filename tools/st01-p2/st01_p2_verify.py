#!/usr/bin/env python3
"""ST-01 P2 driver: verifies the EAST and WEST candidates independently, compares them, searches for the minimal
horizontal extension (D01-B), derives depth numbers, and writes plans/sections/json.
Usage: python3 -I st01_p2_verify.py <out_dir>      (exit code 1 if any check FAILs)"""
import sys, os, json, itertools
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import p2_core as C
from st01_p2_model_east import make_east
from st01_p2_model_west import make_west

def independent_box_checks(m):
    """Analytic (no voxel grid) cross-checks: box-pair intersection volume, bounding extents, widths, stair arithmetic."""
    out = {}
    bl = C.boxes_of(m); inter = []
    for (ka, a), (kb, b) in itertools.combinations(bl, 2):
        if ka == kb or (ka.startswith("P1:") and kb.startswith("P1:")): continue
        v = 1
        for k in range(3): v *= max(0, min(a[2*k+1], b[2*k+1]) - max(a[2*k], b[2*k]) + 1)
        if v: inter.append((ka, kb, v))
    out["box_intersections"] = inter
    xs = [b[0] for k, sp in m["spaces"].items() for b in sp["boxes"]] + [b[1] for k, sp in m["spaces"].items() for b in sp["boxes"]]
    zs = [b[4] for k, sp in m["spaces"].items() for b in sp["boxes"]] + [b[5] for k, sp in m["spaces"].items() for b in sp["boxes"]]
    ys = [b[2] for k, sp in m["spaces"].items() for b in sp["boxes"]]
    out["cavity_extent"] = dict(x=[min(xs), max(xs)], z=[min(zs), max(zs)], ymin=min(ys))
    # analytic widths: union of z (or x) extents per column of a corridor
    wid = {}
    for k, sp in m["spaces"].items():
        if not sp.get("req_w") or sp["kind"] == "stair": continue
        ax = sp["axis"]; cols = {}
        for b in sp["boxes"]:
            lo, hi = (b[0], b[1]) if ax == "x" else (b[4], b[5])
            plo, phi = (b[4], b[5]) if ax == "x" else (b[0], b[1])
            for c in range(lo, hi+1):
                cols.setdefault(c, []).append((plo, phi))
        mn = 999
        for c, iv in cols.items():
            iv.sort(); cur = iv[0]; best = cur[1]-cur[0]+1
            for a, b in iv[1:]:
                if a <= cur[1]+1: cur = (cur[0], max(cur[1], b))
                else: cur = (a, b)
                best = max(best, cur[1]-cur[0]+1)
            mn = min(mn, best)
        wid[k] = mn
    out["analytic_widths"] = wid
    return out

def vertical_separation(m):
    """Smallest vertical solid thickness between cavities of different, unconnected spaces whose plans overlap."""
    declared = {frozenset(l) for l in m["links"]}
    best = []
    for (ka, a), (kb, b) in itertools.combinations(C.boxes_of(m), 2):
        if ka == kb or frozenset((ka, kb)) in declared: continue
        if ka.startswith("P1:") and kb.startswith("P1:"): continue
        if min(a[1], b[1]) - max(a[0], b[0]) + 1 > 0 and min(a[5], b[5]) - max(a[4], b[4]) + 1 > 0:
            gap = max(b[2]-a[3], a[2]-b[3]) - 1
            if gap >= 0: best.append((gap, ka, kb))
    best.sort()
    return best[:6]

def hall_corner_search(m, G):
    """Place the hall reservation at each corner of +-40 (entrance side kept north) and measure conflicts / extra depth needed.
    The L3 route (C3,R,A,T) is NOT re-solved for the alternatives (stated limitation)."""
    hb = m["res"]["HALL_RES"]["box"]
    W = hb[1]-hb[0]+1; D = hb[5]-hb[4]+1
    chain = {"A", "R", "T", "C3", "FD"}
    rows = []
    cav = [(k, b) for k, b in C.boxes_of(m) if k not in chain]
    for name, (x0, z0) in {"SW": (-40, 2), "SE": (2, 2), "NW": (-40, -40), "NE": (2, -40)}.items():
        # keep z within +-40: south corners start at z=2 (as designed), north corners start at -40
        x1 = x0+W-1; z1 = z0+D-1
        for shift in range(0, 40):
            y0 = hb[2]-shift; y1 = hb[3]-shift
            conf = sorted({k for k, b in cav if not (b[1] < x0-2 or b[0] > x1+2 or b[3] < y0-2 or b[2] > y1+2 or b[5] < z0-2 or b[4] > z1+2)})
            if shift == 0: conf0 = conf
            if not conf: break
        rows.append(dict(corner=name, x=[x0, x1], z=[z0, z1], conflicts_at_design_depth=conf0, extra_depth_to_clear=shift if not conf else None,
                         lowest_Y_if_cleared=(hb[2]-shift) if not conf else None))
    return rows

def vault_search(G, m, size=(27, 31), height=14, label=""):
    """Smallest horizontal extension e (blocks beyond +-40) at which a 27x31 (either orientation) x height reservation fits
    at the given vertical level with a 2-block margin to every cavity/reservation. Returns list over levels."""
    solid, res, ridx = G["solid"], G["res"], G["ridx"]
    occ = (~solid) | ((res >= 0) & (res != ridx.get("VAULT_A", -99)) & (res != ridx.get("STAIR_A", -99)))
    P = np.zeros((C.NX+1, C.NY+1, C.NZ+1), np.int32)
    P[1:, 1:, 1:] = occ.cumsum(0).cumsum(1).cumsum(2)
    def cnt(x0, x1, y0, y1, z0, z1):
        a, b, c0, d, e, f = C.ix(x0), C.ix(x1)+1, C.iy(y0), C.iy(y1)+1, C.iz(z0), C.iz(z1)+1
        return (P[b, d, f]-P[a, d, f]-P[b, c0, f]-P[b, d, e]+P[a, c0, f]+P[a, d, e]+P[b, c0, e]-P[a, c0, e])
    out = {}
    levels = {"L3-level (lateral, vault interior at F=-22)": (-24, -24+height-1),
              "stacked below royal hall (vault interior F=-37)": (-39, -39+height-1)}
    for lname, (y0, y1) in levels.items():
        found = None
        for e in range(0, 21):
            lim = 40+e; n = 0; first = None; best = None
            hb = m["res"]["HALL_RES"]["box"]
            for (w, d) in (size, size[::-1]):
                for x0 in range(-lim, lim-w+2):
                    for z0 in range(-lim, lim-d+2):
                        if cnt(x0-2, x0+w-1+2, y0-2, y1+2, z0-2, z0+d-1+2) == 0:
                            n += 1; first = first or (x0, x0+w-1, z0, z0+d-1)
                            gx = max(0, hb[0]-(x0+w-1)-1, x0-hb[1]-1); gz = max(0, hb[4]-(z0+d-1)-1, z0-hb[5]-1)
                            g = max(gx, gz)
                            if best is None or g < best[0]: best = (g, (x0, x0+w-1, z0, z0+d-1))
            if n: found = dict(e=e, placements=n, first=first, nearest_to_hall=dict(plan_gap_blocks=best[0], x_z=best[1]), y=[y0, y1]); break
        out[lname] = found
    return out

def depth_table(m):
    F = {"L1": -6, "L12": -10, "L2": -14, "L23": -18, "L3": -22}
    rows = [
        ("L1 floor (E1, from P1)", -6, "P1 interface (sourced)"),
        ("L1->L2 total rise", 8, "2 flights x 4 risers, 1:1 (PROPOSED)"),
        ("L2 floor", -14, "derived"),
        ("L2->L3 total rise", 8, "2 flights x 4 risers, 1:1 (PROPOSED)"),
        ("L3 floor / royal-hall floor", -22, "derived"),
        ("Floor slab beneath L3 (2) -> lowest placed block, L1-L3 + hall", -24, "PROPOSED slab 2"),
        ("Hall reservation bottom", m["res"]["HALL_RES"]["box"][2], "interior floor -22, slab 2"),
        ("Secret-vault candidate A bottom (stacked under hall)", m["res"]["VAULT_A"]["box"][2], "UNVERIFIED stage-P4 candidate"),
    ]
    return rows

CH = {}
def letter(k):
    if k.startswith("P1:"): return "P"
    if k in ("C1", "C2", "C3"): return "="
    if k.startswith("BR"): return "-"
    if k in ("T1", "T2", "T3", "B1", "B2", "B3"): return "o"
    if k in ("V12", "V23"): return "v"
    if k in ("FA", "FB", "FC", "FD"): return "s"
    if k.startswith("LND"): return "l"
    if k == "R": return "r"
    if k == "A": return "A"
    if k == "T": return "t"
    return "?"

def render_plan(G, y, xr=(-46, 46), zr=(-46, 46)):
    own, res, ids, rids = G["owner"], G["res"], G["ids"], G["rids"]
    L = [f"PLAN  layer Y={y}  X[{xr[0]},{xr[1]}] Z[{zr[0]},{zr[1]}]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)"]
    L.append("  Z\\X  " + "".join("+" if x == 0 else ("'" if x % 10 == 0 else " ") for x in range(xr[0], xr[1]+1)))
    for z in range(zr[0], zr[1]+1):
        row = []
        for x in range(xr[0], xr[1]+1):
            o = own[C.ix(x), C.iy(y), C.iz(z)]
            if o >= 0: row.append(letter(ids[o])); continue
            r = res[C.ix(x), C.iy(y), C.iz(z)]
            if r >= 0: row.append({"HALL_RES": "h", "HALL_INT": "H", "VAULT_A": "V", "STAIR_A": "S"}.get(rids[r], "?")); continue
            row.append("!" if (abs(x) == 41 or abs(z) == 41) else " ")
        L.append(f"{z:+4d}  " + "".join(row))
    return "\n".join(L)

def render_section(G, axis, c, yr, lo=-46, hi=46):
    own, res, ids, rids = G["owner"], G["res"], G["ids"], G["rids"]
    L = [f"SECTION {axis.upper()}={c}  ({'left=north(-Z) right=south(+Z)' if axis == 'x' else 'left=west(-X) right=east(+X)'})  Y[{yr[0]},{yr[1]}]"]
    for y in range(yr[1], yr[0]-1, -1):
        row = []
        for t in range(lo, hi+1):
            x, z = (c, t) if axis == "x" else (t, c)
            o = own[C.ix(x), C.iy(y), C.iz(z)]
            if o >= 0: row.append(letter(ids[o])); continue
            r = res[C.ix(x), C.iy(y), C.iz(z)]
            if r >= 0: row.append({"HALL_RES": "h", "HALL_INT": "H", "VAULT_A": "V", "STAIR_A": "S"}.get(rids[r], "?")); continue
            row.append("!" if abs(t) == 41 else ("#"))
        L.append(f"{y:+4d}  " + "".join(row))
    return "\n".join(L)

LEGEND = ("legend: = main passage  - optional branch  o optional room  v vestibule  s stair tread  l landing  r royal approach(5)  A antechamber  "
          "t 3-wide threshold  P P1 context  H hall clear interior reservation  h hall shell reservation  V secret vault cand. A  S secret stair cand. A  # solid  ! +-40 marker")

def run_side(m):
    R = C.analyse(m, m["start"], m["port_goal"])
    G = R.pop("G"); R.pop("st"); R.pop("S")
    ind = independent_box_checks(m)
    # analytic/voxel agreement checks
    vox_ext = R["metrics"]
    agree = (ind["cavity_extent"]["x"] == vox_ext["cavity_x"] and ind["cavity_extent"]["z"] == vox_ext["cavity_z"] and ind["cavity_extent"]["ymin"] == vox_ext["lowest_cavity_Y"])
    R["checks"].append(dict(id="I1", title="Independent AABB geometry agrees with the voxel model (cavity extents, lowest Y)", verdict="PASS" if agree else "FAIL",
                            evidence=f"analytic {ind['cavity_extent']} vs voxel X{vox_ext['cavity_x']} Z{vox_ext['cavity_z']} Ymin {vox_ext['lowest_cavity_Y']}"))
    boxbad = [t for t in ind["box_intersections"] if not (t[0].split(':')[0] == 'P1' and t[1].split(':')[0] == 'P1')]
    declared = {frozenset(l) for l in m["links"]}
    boxbad = [t for t in boxbad if frozenset((t[0], t[1])) not in declared]
    R["checks"].append(dict(id="I2", title="Independent pairwise AABB intersection volume between different cavities is zero (no collision, Y included)",
                            verdict="PASS" if not boxbad else "FAIL", evidence=f"intersections: {boxbad}"))
    wbad = []
    for k, w in ind["analytic_widths"].items():
        need = m["spaces"][k]["req_w"]
        if w < need: wbad.append((k, w, need))
    R["checks"].append(dict(id="I3", title="Independent analytic corridor widths (union of box extents per cross-section) meet Q40",
                            verdict="PASS" if not wbad else "FAIL", evidence=f"analytic min widths {ind['analytic_widths']}; violations {wbad}"))
    R["independent"] = ind
    # shortest walking distance (4-neighbour steps) from the P1 landing to each main goal
    from collections import deque
    st = C.standable(G["solid"]); solid = G["solid"]
    dist = {m["start"]: 0}; dq = deque([m["start"]])
    while dq:
        x, y, z = dq.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                n = (x+dx, y+dy, z+dz)
                if not st[C.ix(n[0]), C.iy(n[1]), C.iz(n[2])]: continue
                if dy == 1 and solid[C.ix(x), C.iy(y+2), C.iz(z)]: continue
                if n not in dist: dist[n] = dist[(x, y, z)]+1; dq.append(n)
                break
    R["route_distance"] = {g: dist.get(c) for g, c in m["goals"].items()}
    R["vsep"] = vertical_separation(m)
    return R, G

def main(out):
    os.makedirs(out, exist_ok=True)
    E = make_east(); W = make_west()
    RE, GE = run_side(E); RW, GW = run_side(W)
    # mirror equivalence at voxel level (W built independently from the mirrored model)
    eq_solid = bool((GE["solid"] == GW["solid"][::-1, :, :]).all())
    eq_owner = bool((GE["owner"] == GW["owner"][::-1, :, :]).all())
    eq_res = bool((GE["res"] == GW["res"][::-1, :, :]).all())
    me, mw = RE["metrics"], RW["metrics"]
    mirrored = (me["cavity_x"] == [-mw["cavity_x"][1], -mw["cavity_x"][0]] and me["cavity_z"] == mw["cavity_z"] and me["lowest_cavity_Y"] == mw["lowest_cavity_Y"]
                and me["p2_cavity_voxels"] == mw["p2_cavity_voxels"] and me["cavity_voxels_outside_pm40"] == mw["cavity_voxels_outside_pm40"])
    same_verdicts = [(a["id"], a["verdict"]) for a in RE["checks"]] == [(a["id"], a["verdict"]) for a in RW["checks"]]
    # search / depth on EAST (west is the exact mirror, proven above)
    hall = hall_corner_search(E, GE)
    vault = vault_search(GE, E)
    depth = depth_table(E)
    # secret candidate A sanity (inside hall footprint, no collisions with cavities, vertical separation)
    vb = E["res"]["VAULT_A"]["box"]; hb = E["res"]["HALL_RES"]["box"]; sb = E["res"]["STAIR_A"]["box"]
    vinside = hb[0] <= vb[0] and vb[1] <= hb[1] and hb[4] <= vb[4] and vb[5] <= hb[5]
    vsolid = hb[2] - (vb[3]+1)          # solid layers between vault reservation top and hall reservation bottom (hall slab bottom)
    # vault void top = vb top - roof(2); separation to hall floor-slab bottom (hb[2]) counts the vault roof slab
    void_top = vb[3]-2
    sep = hb[2]-void_top-1
    overlapVS = not (vb[1] < sb[0] or vb[0] > sb[1] or vb[5] < sb[4] or vb[4] > sb[5])
    cand = dict(vault_plan=[vb[1]-vb[0]+1, vb[5]-vb[4]+1], interior=[vb[1]-vb[0]+1-6, vb[5]-vb[4]+1-6, void_top-(vb[2]+2)+1],
                inside_hall_footprint=vinside, solid_between_vault_void_and_hall_bottom=sep, stair_overlaps_vault=overlapVS, stair_box=sb,
                strip_width_for_stair=sb[1]-sb[0]+1, rise_hall_to_vault=-22-(vb[2]+2), horizontal_run_available=sb[5]-sb[4]+1)
    cand["ok"] = bool(vinside and sep >= 3 and not overlapVS and cand["interior"][0] >= 21 and cand["interior"][1] >= 25 and cand["interior"][2] >= 10)
    allc = RE["checks"] + RW["checks"]
    res = dict(east=RE, west=RW, equivalence=dict(voxel_solid=eq_solid, voxel_owner=eq_owner, voxel_reserved=eq_res, metrics_mirrored=mirrored, same_verdicts=same_verdicts),
               hall_corner_search=hall, vault_search=vault, depth=depth, secret_candidate=cand)
    json.dump(res, open(os.path.join(out, "p2_verification.json"), "w"), indent=1, default=lambda o: o.tolist() if hasattr(o, "tolist") else str(o))
    # drawings
    for side, G in (("east", GE), ("west", GW)):
        d = os.path.join(out, side); os.makedirs(d, exist_ok=True)
        for y, nm in ((-6, "L1"), (-10, "L12"), (-14, "L2"), (-18, "L23"), (-22, "L3"), (-33, "secret_vault_candidate")):
            open(f"{d}/plan_{nm}_Y{y}.txt", "w").write(LEGEND + "\n" + render_plan(G, y) + "\n")
        sx = 9 if side == "east" else -9
        open(f"{d}/section_X{sx:+d}.txt", "w").write(LEGEND + "\n" + render_section(G, "x", sx, (-42, 0)) + "\n")
        open(f"{d}/section_Z-7.txt", "w").write(LEGEND + "\n" + render_section(G, "z", -7, (-42, 0)) + "\n")
        open(f"{d}/section_Z+7.txt", "w").write(LEGEND + "\n" + render_section(G, "z", 7, (-42, 0)) + "\n")
    bad = [c for c in allc if c["verdict"] == "FAIL"]
    print("EAST", sum(c["verdict"] == "PASS" for c in RE["checks"]), "PASS /", len(RE["checks"]), "| WEST", sum(c["verdict"] == "PASS" for c in RW["checks"]), "PASS /", len(RW["checks"]))
    print("mirror equivalence:", res["equivalence"])
    print("hall corners:", json.dumps(hall)[:700])
    print("vault search:", vault)
    print("secret candidate:", cand)
    print("tight vertical separations:", RE["vsep"])
    for c in bad: print("FAIL", c["id"], c["evidence"][:300])
    return 1 if bad or not (eq_solid and eq_owner and eq_res and mirrored and same_verdicts) else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "."))
