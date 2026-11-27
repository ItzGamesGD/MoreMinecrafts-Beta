"""ST-01 P2 EAST candidate (E1 exit toward +X). Hand-authored PROPOSED coordinates; see report.
Coordinates: X east+, Z south+, Y layer index (Y=0 lowest above-ground layer), inclusive ranges.
Floors (feet layer F): L1 -6, L1->L2 landing -10, L2 -14, L2->L3 landing -18, L3 -22.
"""
import math, os
from p2_core import load_p1_spaces

HERE = os.path.dirname(os.path.abspath(__file__))
P1_MODEL = os.path.join(HERE, "..", "st01-p1", "st01_p1_model.py")

def box(x0, x1, y0, y1, z0, z1): return [x0, x1, y0, y1, z0, z1]

def jog_boxes(xa, xb, z_lo_start, dz, y0, y1, width=7, x_jog0=None, x_jog1=None):
    """Corridor along X; centre line jogs by dz between x_jog0..x_jog1; adjacent columns are unioned so the
    cross-section never narrows below `width`. Returns merged boxes."""
    def lo(x):
        if x <= x_jog0: return z_lo_start
        if x >= x_jog1: return z_lo_start + dz
        return z_lo_start + int(math.floor(dz * (x - x_jog0) / (x_jog1 - x_jog0) + 0.5))
    cols = []
    for x in range(xa, xb + 1):
        l0 = lo(x); l1 = lo(x + 1) if x < xb else l0
        cols.append((x, min(l0, l1), max(l0, l1) + width - 1))
    out = []
    for x, a, b in cols:
        if out and out[-1][4] == a and out[-1][5] == b and out[-1][1] == x - 1: out[-1][1] = x
        else: out.append([x, x, y0, y1, a, b])
    return out

def stair_boxes(axis, coords, zr_or_xr, F_first, step, headroom=5):
    """Per-tread void boxes: tread i at coordinate coords[i] with feet F_first-i*step; void Y F..F+headroom-1."""
    out = []; treads = []
    for i, c in enumerate(coords):
        F = F_first - i
        if axis == "z": out.append([zr_or_xr[0], zr_or_xr[1], F, F + headroom - 1, c, c])
        else: out.append([c, c, F, F + headroom - 1, zr_or_xr[0], zr_or_xr[1]])
        treads.append((c, F))
    return out, treads

def make_east():
    p1 = load_p1_spaces(P1_MODEL)
    p1.pop("S07")                       # P1 reservation stub is superseded by P2 corridor C1 (contains it)
    S = {}
    def sp(i, name, level, kind, boxes, **kw): S[i] = dict(name=name, level=level, kind=kind, boxes=boxes, **kw)
    # ---------------- L1 (F=-6, clear 5 -> Y -6..-2; clear 4 under the surface hall zone)
    sp("C1", "L1 main corridor (E1 continuation, 7 wide, gentle jog)", "L1", "main",
       jog_boxes(8, 30, -13, 2, -6, -2, 7, 22, 30), req_w=7, req_h=5, axis="x")
    sp("BR1", "L1 branch to tomb T1 (3 wide, 4 long)", "L1", "optional", [box(11, 13, -6, -2, -17, -14)])
    sp("T1", "L1 ordinary tomb T1 7x7", "L1", "optional", [box(9, 15, -6, -2, -24, -18)])
    sp("BR2", "L1 branch to tomb T2", "L1", "optional", [box(20, 22, -6, -2, -17, -14)])
    sp("T2", "L1 ordinary tomb T2 7x9", "L1", "optional", [box(18, 24, -6, -2, -26, -18)])
    sp("BR3", "L1 branch to tomb T3 (under surface hall: clear 4)", "L1", "optional", [box(11, 13, -6, -3, -6, -4)])
    sp("T3", "L1 ordinary tomb T3 7x7 (clear 4)", "L1", "optional", [box(9, 15, -6, -3, -3, 3)])
    sp("V12", "L1->L2 transition vestibule 9x11", "L1", "main", [box(31, 39, -6, -2, -12, -2)])
    # ---------------- L1 -> L2
    b, t = stair_boxes("z", [-1, 0, 1, 2], (32, 38), -7, 1)
    sp("FA", "Flight A (south, 4 treads, width 7)", "L12", "stair", b, treads=t, axis="z", req_w=7, req_h=5)
    sp("LND12", "Landing L1/L2 (9x9, F=-10)", "L12", "main", [box(31, 39, -10, -6, 3, 11)])
    b, t = stair_boxes("x", [30, 29, 28, 27], (4, 10), -11, 1)
    sp("FB", "Flight B (west, 4 treads, width 7) - turn #1", "L12", "stair", b, treads=t, axis="x", req_w=7, req_h=5)
    # ---------------- L2 (F=-14)
    sp("C2", "L2 central passage 7 wide", "L2", "main", [box(14, 26, -14, -10, 4, 10)], req_w=7, req_h=5, axis="x")
    sp("BRB1", "L2 branch to B1", "L2", "optional", [box(24, 26, -14, -10, 11, 14)])
    sp("B1", "L2 storage B1 7x7 flat", "L2", "optional", [box(22, 28, -14, -10, 15, 21)])
    sp("BRB2", "L2 branch to B2", "L2", "optional", [box(16, 18, -14, -10, 11, 14)])
    sp("B2", "L2 storage B2 9x7 large, taller (clear 8)", "L2", "optional", [box(11, 19, -14, -7, 15, 21)])
    sp("BRB3", "L2 branch to B3", "L2", "optional", [box(19, 21, -14, -10, 0, 3)])
    sp("B3", "L2 storage B3 7x9 flat", "L2", "optional", [box(17, 23, -14, -10, -9, -1)])
    sp("V23", "L2->L3 transition vestibule 11x9", "L2", "main", [box(3, 13, -14, -10, 3, 11)])
    b, t = stair_boxes("z", [2, 1, 0, -1], (5, 11), -15, 1)
    sp("FC", "Flight C (north, 4 treads, width 7)", "L23", "stair", b, treads=t, axis="z", req_w=7, req_h=5)
    sp("LND23", "Landing L2/L3 (9x9, F=-18)", "L23", "main", [box(4, 12, -18, -14, -10, -2)])
    b, t = stair_boxes("x", [3, 2, 1, 0], (-9, -3), -19, 1)
    sp("FD", "Flight D (west, 4 treads, width 7) - turn #2", "L23", "stair", b, treads=t, axis="x", req_w=7, req_h=5)
    # ---------------- L3 (F=-22)
    sp("C3", "L3 main passage 7 wide", "L3", "main", [box(-3, -1, -22, -18, -9, -3)], req_w=7, req_h=5, axis="x")
    sp("R", "L3 royal approach 5 wide (Q40/Q26)", "L3", "main", [box(-11, -4, -22, -19, -9, -5)], req_w=5, req_h=4, axis="x")
    sp("A", "Ceremonial antechamber 19x11, clear 10 (Q27/Q41)", "L3", "main", [box(-30, -12, -22, -13, -12, -2)])
    sp("T", "Exit threshold 3 wide, 6 long incl. 3-block hall-wall port (Q40/Q42)", "L3", "main", [box(-22, -20, -22, -20, -1, 4)], req_w=3, req_h=3, axis="z")
    res = {
        "HALL_RES": dict(name="Royal hall reservation (interior 33x33x16 + >=3 shell)", box=box(-40, -2, -24, -5, 2, 40)),
        "HALL_INT": dict(name="Royal hall clear interior 33x33x16 (Q43/Q44)", box=box(-37, -5, -22, -7, 5, 37)),
        "VAULT_A": dict(name="Secret vault candidate A (interior 21x25x10, below hall)", box=box(-40, -14, -39, -26, 9, 39)),
        "STAIR_A": dict(name="Secret descent candidate A (below hall slab; hall opening TBD in P4)", box=box(-13, -3, -39, -25, 9, 39)),
    }
    links = [("C1", "P1:S06"), ("C1", "BR1"), ("BR1", "T1"), ("C1", "BR2"), ("BR2", "T2"), ("C1", "BR3"), ("BR3", "T3"), ("C1", "V12"),
             ("V12", "FA"), ("FA", "LND12"), ("LND12", "FB"), ("FB", "C2"), ("C2", "BRB1"), ("BRB1", "B1"), ("C2", "BRB2"), ("BRB2", "B2"),
             ("C2", "BRB3"), ("BRB3", "B3"), ("C2", "V23"), ("V23", "FC"), ("FC", "LND23"), ("LND23", "FD"), ("FD", "C3"), ("C3", "R"),
             ("R", "A"), ("A", "T")]
    goals = {"MAIN:C1": (20, -6, -10), "MAIN:V12": (35, -6, -7), "MAIN:LND12": (35, -10, 7), "MAIN:C2": (20, -14, 7), "MAIN:V23": (8, -14, 7),
             "MAIN:LND23": (8, -18, -6), "MAIN:C3": (-2, -22, -6), "MAIN:R": (-8, -22, -7), "MAIN:A": (-21, -22, -7), "MAIN:PORT": (-21, -22, 4),
             "OPT:T1": (12, -6, -21), "OPT:T2": (21, -6, -22), "OPT:T3": (12, -6, 0), "OPT:B1": (25, -14, 18), "OPT:B2": (15, -14, 18), "OPT:B3": (20, -14, -5)}
    return dict(side="EAST", spaces=S, links=links, res=res, ports=[("T", "HALL_RES", box(-22, -20, -22, -20, 2, 4))], p1=p1,
                p1_links=[], goals=goals, flights={"L1-L2": ("FA", "FB", 8), "L2-L3": ("FC", "FD", 8)},
                landings={"L1/L2": "LND12", "L2/L3": "LND23"}, approach_seq=[("C3", 7), ("R", 5), ("T", 3)],
                threshold="T", threshold_len=6, antechamber_h=10, start=(0, -6, -10), port_goal="MAIN:PORT")

def mirror_model(m):
    """Exact X-mirror (x -> -x) of an east model, including the P1 context. Used for the west candidate."""
    def mb(b): return [-b[1], -b[0], b[2], b[3], b[4], b[5]]
    W = dict(m); W["side"] = "WEST"
    W["spaces"] = {}
    for k, s in m["spaces"].items():
        s2 = dict(s); s2["boxes"] = [mb(b) for b in s["boxes"]]
        if "treads" in s: s2["treads"] = [((-c if s["axis"] == "x" else c), F) for c, F in s["treads"]]
        W["spaces"][k] = s2
    W["res"] = {k: dict(r, box=mb(r["box"])) for k, r in m["res"].items()}
    W["ports"] = [(a, b, mb(o)) for a, b, o in m["ports"]]
    W["p1"] = {k: mb(b) for k, b in m["p1"].items()}
    W["goals"] = {k: (-c[0], c[1], c[2]) for k, c in m["goals"].items()}
    W["start"] = (-m["start"][0], m["start"][1], m["start"][2])
    return W
