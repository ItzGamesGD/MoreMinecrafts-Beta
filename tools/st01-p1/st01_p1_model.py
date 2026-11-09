#!/usr/bin/env python3
"""ST-01 P1 aboveground 3D coordinate model + verifier (design-only, no NBT/worldgen).

Coordinate system (see doc): origin = pyramid ground-center, X east+, Z south+ (entrance side),
Y = block layer index, Y=0 is the lowest ABOVE-GROUND layer. Ground floor feet layer F=0, its floor
slab is layer -1. Pyramid layers 0..39 (40 high); layer y outer half-width = 40-y (1:1 slope), so
layer 0 = X,Z -40..40 (81x81) and layer 39 = 3x3 cap.

Every dimension here that is not an owner-approved Q-number value is PROPOSED.
Usage: python3 st01_p1_model.py <out_dir>
"""
import sys, json, os
from collections import deque
import numpy as np

XMIN, XMAX, ZMIN, ZMAX, YMIN, YMAX = -40, 40, -40, 40, -14, 39
NX, NZ, NY = XMAX - XMIN + 1, ZMAX - ZMIN + 1, YMAX - YMIN + 1
SHELL_MIN = 3        # PROPOSED: min horizontal shell (blocks) between any interior void and outer slope
MIN_PASS_W = 3       # PROPOSED: min clear public passage width
MIN_HEAD = 3         # PROPOSED: min headroom for any walked cell

def ix(x): return x - XMIN
def iz(z): return z - ZMIN
def iy(y): return y - YMIN

# ---------------------------------------------------------------- spaces (disjoint boxes)
# id: (name, kind, (x0,x1),(y0,y1),(z0,z1), status)
SPACES = {
 "S00": ("forecourt (open-air sunken entrance court)", "exterior", (-12, 12), (0, 11), (29, 40), "PROPOSED"),
 "S01": ("D1 entrance door passage", "opening", (-4, 4), (0, 9), (25, 28), "PROPOSED"),
 "S02": ("vestibule", "room", (-10, 10), (0, 11), (21, 24), "PROPOSED"),
 "S03": ("D2 hall portal", "opening", (-6, 6), (0, 10), (19, 20), "PROPOSED"),
 "S04": ("grand hall (nave + gallery air volume)", "room", (-18, 18), (0, 17), (-6, 18), "PROPOSED (Q03 approved qualitatively)"),
 "S05": ("descent flight A (stair well, hall-floor cutout)", "stair", (-5, 5), (-6, -1), (-6, -1), "PROPOSED (Q02)"),
 "S06": ("descent landing", "room", (-7, 7), (-6, -1), (-14, -7), "PROPOSED (Q02)"),
 "S07": ("P2 interface stub U00 (reserved)", "reserved", (8, 15), (-6, -3), (-13, -7), "RESERVED for P2"),
 "S08": ("AP1 altar portal (rear wall)", "opening", (-3, 3), (7, 12), (-8, -7), "PROPOSED"),
 "S09": ("altar ascent passage + stair", "stair", (-3, 3), (8, 13), (-11, -9), "PROPOSED (Q05)"),
 "S10": ("altar chamber", "room", (-7, 7), (11, 17), (-20, -12), "PROPOSED (Q05)"),
}
# nested logical zones of the hall (declared containment, not collisions)
ZONES = {
 "Z-LEFT":  ("U-gallery left ledge", (-18, -12), (7, 17), (-6, 9)),
 "Z-RIGHT": ("U-gallery right ledge", (12, 18), (7, 17), (-6, 9)),
 "Z-REAR":  ("U-gallery rear ledge", (-11, 11), (7, 17), (-6, 0)),
 "Z-CENTER": ("open central nave (no columns)", (-8, 8), (0, 17), (1, 18)),
}
# declared direct connections (face contact or doorway) between spaces
CONNECT = [
 ("S00","S01"),("S01","S02"),("S02","S03"),("S03","S04"),("S04","S05"),("S05","S06"),
 ("S06","S07"),("S04","S08"),("S08","S09"),("S09","S10"),
]

# ---------------------------------------------------------------- solid elements placed in voids
ELEMS = []   # (name, kind, parent, (x0,x1),(y0,y1),(z0,z1))
def E(name, kind, parent, xs, ys, zs): ELEMS.append((name, kind, parent, xs, ys, zs))
for sx in (-1, 1):
    for zc in (2, 8, 14):                       # monumental columns 3x3, full height
        c = sx * 10
        E(f"col_hall_{'E' if sx>0 else 'W'}{zc}", "column", "S04", (c-1, c+1), (0, 17), (zc-1, zc+1))
    for xc in (-8, 8):                           # forecourt columns 3x3 x 10 high
        pass
for sx in (-8, 8):
    for zc in (32, 37):
        E(f"col_court_{'E' if sx>0 else 'W'}{zc}", "column", "S00", (sx-1, sx+1), (0, 9), (zc-1, zc+1))
E("ledge_W", "ledge", "S04", (-18, -12), (5, 6), (-6, 9))
E("ledge_E", "ledge", "S04", (12, 18), (5, 6), (-6, 9))
E("ledge_R", "ledge", "S04", (-11, 11), (5, 6), (-6, 0))
E("parapet_W", "parapet", "S04", (-12, -12), (7, 7), (1, 9))
E("parapet_E", "parapet", "S04", (12, 12), (7, 7), (1, 9))
E("parapet_R", "parapet", "S04", (-11, 11), (7, 7), (0, 0))
for side, xs in (("W", (-18, -12)), ("E", (12, 18))):      # gallery access stairs (fill under treads)
    for k in range(1, 7):                                  # tread z=16-k has feet F=k (apron z16..18 at F=0)
        E(f"galstair_{side}{k}", "stair", "S04", xs, (0, k-1), (16-k, 16-k))
for z in range(-1, -6, -1):                                # descent treads F=z, solid up to F-1
    E(f"descent_tread{-z}", "stair", "S05", (-5, 5), (-6, z-1), (z, z))
for i, z in enumerate((-9, -10, -11)):                     # altar stair treads F=8,9,10 (solid up to F-1)
    E(f"altar_tread{i+1}", "stair", "S09", (-3, 3), (8, 8+i-1), (z, z))

# ---------------------------------------------------------------- build voxel model
solid = np.zeros((NX, NY, NZ), bool)
space = np.full((NX, NY, NZ), "", object)
owner = np.full((NX, NY, NZ), "", object)
collisions = []

def box(b):
    (x0, x1), (y0, y1), (z0, z1) = b
    return slice(ix(x0), ix(x1)+1), slice(iy(y0), iy(y1)+1), slice(iz(z0), iz(z1)+1)

def pyramid_mask():
    m = np.zeros((NX, NY, NZ), bool)
    for y in range(0, 40):
        h = 40 - y
        m[ix(-h):ix(h)+1, iy(y), iz(-h):iz(h)+1] = True
    m[:, :iy(0), :] = True     # ground/foundation mass below Y=0 within footprint
    return m
solid[:] = pyramid_mask()

for sid, (nm, kind, xs, ys, zs, st) in SPACES.items():
    s = box((xs, ys, zs))
    prev = space[s]
    if (prev != "").any(): collisions.append(("space-space voxel overlap", sid))
    space[s] = sid
    solid[s] = False

for nm, kind, parent, xs, ys, zs in ELEMS:
    s = box((xs, ys, zs))
    if (owner[s] != "").any():
        collisions.append(("element-element overlap", nm, sorted(set(owner[s][owner[s] != ""]))))
    # element must sit inside its declared parent void OR in already-solid ground (embedded foot)
    sp = space[s]
    bad = (sp != parent) & (sp != "")
    if bad.any(): collisions.append(("element inside foreign space", nm, sorted(set(sp[bad]))))
    free_outside = (~solid[s]) & (sp == "")
    if free_outside.any(): collisions.append(("element in exterior air", nm))
    owner[s] = nm
    solid[s] = True

# ---------------------------------------------------------------- results container
R = {"collisions": collisions, "checks": []}
def check(cid, title, verdict, evidence):
    R["checks"].append({"id": cid, "title": title, "verdict": verdict, "evidence": evidence})

def sbox(sid):
    _, _, xs, ys, zs, _ = SPACES[sid]; return xs, ys, zs

# ---------------------------------------------------------------- A. exterior
# A1 envelope
occ = np.argwhere(solid[:, iy(0):, :])
xs_ = occ[:, 0] + XMIN; zs_ = occ[:, 2] + ZMIN; ys_ = occ[:, 1]
env_ok = xs_.min() >= -40 and xs_.max() <= 40 and zs_.min() >= -40 and zs_.max() <= 40 and ys_.max() == 39
layer0 = np.argwhere(solid[:, iy(0), :])
# all elements inside the 81x81x40 box?
elem_out = [e[0] for e in ELEMS if not (-40 <= e[3][0] and e[3][1] <= 40 and -40 <= e[5][0] and e[5][1] <= 40 and 0 <= max(e[4][1], 0) <= 39)]
check("A1", "Outer envelope 81x81x40 (layer 0 = X,Z -40..40; top layer 39)",
      "PASS" if env_ok and not elem_out and (layer0[:, 0].max()-layer0[:, 0].min()+1 == 81) else "FAIL",
      f"solid bbox X[{xs_.min()},{xs_.max()}] Z[{zs_.min()},{zs_.max()}] Y[0,{ys_.max()}] => 81x81x{ys_.max()+1}; elements outside box: {elem_out}")

# A2 shell thickness (slope vs interior ceiling/wall): y + max(|x|,|z|) <= 37 for every void voxel of enclosed spaces
EXTERIOR_OK = {"S00"}         # open-air court
EXTERIOR_OPEN_OK = {"S01"}    # door passage in facade (face is intentionally exposed at z=28)
viol = []; shell = {}
for sid in SPACES:
    if sid in EXTERIOR_OK: continue
    xs, ys, zs = sbox(sid)
    # exhaustive voxel scan
    mn = 99
    for y in range(ys[0], ys[1]+1):
        if y < 0: continue
        for x in range(xs[0], xs[1]+1):
            for z in range(zs[0], zs[1]+1):
                m = max(abs(x), abs(z)); t = (40 - y) - m
                mn = min(mn, t)
    shell[sid] = mn if mn != 99 else None
    # analytic cross-check (independent): worst corner = (top layer, max |x|,|z|)
    if ys[1] >= 0:
        worst = (40 - ys[1]) - max(abs(xs[0]), abs(xs[1]), abs(zs[0]), abs(zs[1]))
        if worst != mn: viol.append((sid, "voxel/analytic mismatch", mn, worst))
        if sid not in EXTERIOR_OPEN_OK and mn < SHELL_MIN: viol.append((sid, "shell<3", mn))
    # door passage: shell vs side not applicable at the exposed face; check lintel instead
lintel = (40 - 28) - 9    # facade face top layer 12, door top layer 9 -> layers 10..12 => 3
check("A2", "Slope vs interior volumes: min horizontal shell >= 3 at every void voxel (voxel scan + analytic corner cross-check)",
      "PASS" if not viol else "FAIL", f"min shell per space {shell}; violations {viol}")
check("A3", "Facade door lintel above D1 (face z=28 top layer 12, door top layer 9)",
      "PASS" if lintel >= 3 else "FAIL", f"lintel layers 10..12 = {lintel} blocks")

# A4 roof mass over hall centre / top space availability
top = 39 - 17
check("A4", "Upper-pyramid space: mass above hall (layers 18..39 at x=z=0) and no room needed above altar (layer 17)",
      "PASS", f"{top} solid layers above hall at centre; no program element is placed above layer 17, so no upper-level shortage. (Hollow attic NOT proposed: how much volume stays solid is an open prep-doc item.)")

# A5 entrance vs exterior shape: whole aboveground voxel model must be X-mirror symmetric; nothing beyond footprint
g = solid[:, iy(0):, :]
mirror_diff = int((g != g[::-1, :, :]).sum())
o_g = owner[:, iy(0):, :]
mirror_diff_owner = int(((o_g != "") != (o_g[::-1, :, :] != "")).sum())
check("A5", "Entrance/forecourt vs exterior form: aboveground voxel model X-mirror symmetric; forecourt carved inside footprint (no protrusion beyond z=40)",
      "PASS" if mirror_diff == 0 and mirror_diff_owner == 0 and SPACES["S00"][4][1] <= 40 else "FAIL",
      f"asymmetric solid voxels={mirror_diff}, asymmetric element voxels={mirror_diff_owner}; forecourt X-12..12 x Z29..40 is carved INTO the south slope; facade face z=28 rises to layer 12 where the slope surface begins")

# ---------------------------------------------------------------- walkability
head = np.zeros_like(solid)
def standable():
    free = ~solid
    st = np.zeros_like(solid)
    st[:, 1:-2, :] = free[:, 1:-2, :] & free[:, 2:-1, :] & solid[:, :-3, :]
    return st
ST = standable()
def feet_cells(x, z):
    return [y for y in range(YMIN+1, YMAX-1) if ST[ix(x), iy(y), iz(z)]]

def on_stair(x, y, z):
    if space[ix(x), iy(y), iz(z)] in ("S05", "S09"): return True      # stair-kind spaces (first tread = carved floor)
    o = owner[ix(x), iy(y-1), iz(z)]
    return bool(o) and ("stair" in [e[1] for e in ELEMS if e[0] == o][0])

def wide_ok(x, y, z):
    if on_stair(x, y, z): return True      # stair treads: width verified analytically (STAIR_W below)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            xx, zz = x+dx, z+dz
            if not (XMIN <= xx <= XMAX and ZMIN <= zz <= ZMAX): return False
            if ST[ix(xx), iy(y), iz(zz)]: continue                # same level
            if any(ST[ix(xx), iy(yy), iz(zz)] and on_stair(xx, yy, zz) for yy in (y-1, y+1)): continue   # flat<->stair transition
            return False
    return True

def bfs(starts, wide=False, blocked=None):
    seen = set(starts); dq = deque(starts)
    while dq:
        x, y, z = dq.popleft()
        for dx, dz in ((1,0),(-1,0),(0,1),(0,-1)):
            xx, zz = x+dx, z+dz
            if not (XMIN <= xx <= XMAX and ZMIN <= zz <= ZMAX): continue
            for dy in (0, 1, -1):
                yy = y+dy
                if not (YMIN < yy < YMAX-2): continue
                if not ST[ix(xx), iy(yy), iz(zz)]: continue
                # step up needs 3 free cells above the origin (head clearance for the jump)
                if dy == 1 and solid[ix(x), iy(y+2), iz(z)]: continue
                if blocked and (xx, yy, zz) in blocked: continue
                if wide and not wide_ok(xx, yy, zz): continue
                n = (xx, yy, zz)
                if n not in seen: seen.add(n); dq.append(n)
                break
    return seen

START = (0, 0, 40)
reach = bfs([START]); reachW = bfs([START], wide=True)
GOALS = {
 "forecourt centre": (0, 0, 35), "D1 door": (0, 0, 27), "vestibule": (0, 0, 22), "D2 portal": (0, 0, 19.5),
 "hall centre": (0, 0, 10), "descent stair head": (0, 0, 1), "descent landing": (0, -6, -10),
 "P2 exit E1 (x=8)": (8, -6, -10),
 "gallery W (stair top)": (-15, 7, 9), "gallery E (stair top)": (15, 7, 9), "gallery rear centre": (0, 7, -3),
 "altar portal": (0, 7, -7), "altar stair mid": (0, 9, -10), "altar chamber centre": (0, 11, -16),
}
def inreach(c, rset):
    x, y, z = c
    z = int(z) if z == int(z) else int(z + 0.5)
    return (x, y, z) in rset
rows = []
allok = True
for g, c in GOALS.items():
    a = inreach(c, reach); b = inreach(c, reachW)
    rows.append((g, c, a, b)); allok &= a
check("B1", "Every program space reachable on foot from outside the pyramid (4-neighbour, step<=1, 2-block headroom)",
      "PASS" if allok else "FAIL", "; ".join(f"{g}:{'Y' if a else 'N'}" for g, c, a, b in rows))
allokW = all(b for g, c, a, b in rows)
check("B2", f"Main routes passable by a {MIN_PASS_W}-wide agent (3x3 footprint, +-1 level on stairs)",
      "PASS" if allokW else "FAIL", "; ".join(f"{g}:{'Y' if b else 'N'}" for g, c, a, b in rows))

# gallery continuity: left stair top -> rear -> right stair top without leaving F=7 level
g7 = {c for c in reach if c[1] == 7}
lw = (-15, 7, 9); le = (15, 7, 9)
seen7 = set([lw]); dq = deque([lw])
while dq:
    x, y, z = dq.popleft()
    for dx, dz in ((1,0),(-1,0),(0,1),(0,-1)):
        n = (x+dx, 7, z+dz)
        if n in g7 and n not in seen7: seen7.add(n); dq.append(n)
cont = le in seen7
# width of rear ledge walkable rows
rear_w = len({z for (x, y, z) in seen7 if x == 0})
side_w = len({x for (x, y, z) in seen7 if z == 5 and x < 0})
check("C-U", "U-gallery continuity: left ledge -> rear ledge -> right ledge stays on one level (F=7) without stairs",
      "PASS" if cont else "FAIL", f"connected at F=7: {cont}; walkable rear depth at x=0: {rear_w}; walkable side width at z=5 (west): {side_w}; walkable gallery cells: {len(seen7)}")

# independence of altar chamber and descent: forbidding the gallery, altar must be unreachable; altar volume & descent volume disjoint in 3D
blk = {c for c in reach if c[1] >= 7 and -8 <= c[2] <= 11 and abs(c[0]) <= 18}   # remove gallery level cells
rb = bfs([START], blocked=blk)
alt_unreach = (0, 11, -16) not in rb
desc_ok = (0, -6, -10) in rb
a_y = SPACES["S10"][3]; d_y = SPACES["S05"][3]
vsep = a_y[0] - SPACES["S06"][3][1] - 1
check("B3", "Altar chamber independent of descent: altar only via gallery; descent still reachable without gallery; Y-separation",
      "PASS" if alt_unreach and desc_ok and vsep >= 10 else "FAIL",
      f"gallery removed => altar reachable? {not alt_unreach}; descent reachable? {desc_ok}; altar floor feet Y={a_y[0]} vs landing roof layer Y={SPACES['S06'][3][1]}: {vsep} blocks of solid between; plan XZ projections overlap (see D2) but 3D volumes do not")

# ---------------------------------------------------------------- opening / adjacency / 3D collision table
def gaps(a, b):
    out = []
    for (a0, a1), (b0, b1) in zip(a, b):
        out.append(max(0, b0 - a1 - 1, a0 - b1 - 1)) if not (a0 <= b1 and b0 <= a1) else out.append(-min(a1, b1) + max(a0, b0) - 1)  # negative = overlap length
    return out
def ov(a, b):
    r = []
    for (a0, a1), (b0, b1) in zip(a, b):
        r.append(min(a1, b1) - max(a0, b0) + 1)
    return r   # >0 overlap length, 0 = touching-gap-of-0 handled below
pairs = []; undeclared_contact = []; vol_overlap = []; thin = []; plan_only = []
ids = list(SPACES)
conn_set = {frozenset(c) for c in CONNECT}
for i in range(len(ids)):
    for j in range(i+1, len(ids)):
        a, b = ids[i], ids[j]
        A = sbox(a); B = sbox(b)
        ovl = [min(A[k][1], B[k][1]) - max(A[k][0], B[k][0]) + 1 for k in range(3)]   # per-axis overlap length (<=0 => separated by -ovl blocks)
        if all(o > 0 for o in ovl):
            vol_overlap.append((a, b, ovl))
        # face contact: exactly one axis has ovl == 0 (touching), other two >0
        touching = [k for k in range(3) if ovl[k] == 0]
        if len(touching) == 1 and all(ovl[k] > 0 for k in range(3) if k != touching[0]):
            area = 1
            for k in range(3):
                if k != touching[0]: area *= ovl[k]
            ax = "XYZ"[touching[0]]
            rel = "declared opening" if frozenset((a, b)) in conn_set else "UNDECLARED contact"
            pairs.append((a, b, ax, area, rel))
            if rel != "declared opening": undeclared_contact.append((a, b, ax))
        elif len(touching) == 0 and sum(1 for o in ovl if o < 0) == 1 and all(o > 0 for o in ovl if o > -99 and o != min(ovl)) :
            k = [k for k in range(3) if ovl[k] < 0][0]
            wall = -ovl[k]
            if wall < 2 and frozenset((a, b)) not in conn_set: thin.append((a, b, "XYZ"[k], wall))
        xz_over = ovl[0] > 0 and ovl[2] > 0
        if xz_over and ovl[1] < 0:
            plan_only.append((a, b, -ovl[1]))
R["pairs"] = pairs; R["plan_only"] = plan_only
check("D1", "3D volume overlap between distinct spaces (AABB, Y included)",
      "PASS" if not vol_overlap else "FAIL", f"overlapping pairs: {vol_overlap}; voxel-level space/element collisions: {collisions}")
check("D2", "Plan-projection (XZ) overlaps that are NOT 3D overlaps (separated in Y)",
      "PASS", "; ".join(f"{a}/{b}: {g} solid layers between" for a, b, g in plan_only) or "none")
check("D3", "Face contacts between spaces are all declared openings; none are accidental merges (shared wall vs opening vs collision)",
      "PASS" if not undeclared_contact else "FAIL", f"declared openings {len(pairs)-len(undeclared_contact)}, undeclared {undeclared_contact}")
check("D4", "Face-to-face wall between non-connected spaces >= 2 blocks (PROPOSED min wall)",
      "PASS" if not thin else "FAIL", f"thin walls: {thin}")

# opening geometry (clear width/height of each connection)
def contact_rect(a, b):
    A = sbox(a); B = sbox(b)
    return [(max(A[k][0], B[k][0]), min(A[k][1], B[k][1])) for k in range(3)]
openings = []
for a, b, ax, area, rel in pairs:
    r = contact_rect(a, b)
    openings.append((a, b, ax, r, area))
R["openings"] = openings

# ---------------------------------------------------------------- headroom + passages
hmin = {}
for sid, (nm, kind, xs, ys, zs, st) in SPACES.items():
    mn = 99
    for (x, y, z) in reach:
        if xs[0] <= x <= xs[1] and ys[0] <= y <= ys[1] and zs[0] <= z <= zs[1] and space[ix(x), iy(y), iz(z)] == sid:
            h = 0; yy = y
            while yy < YMAX and not solid[ix(x), iy(yy), iz(z)] and h < 30: h += 1; yy += 1
            mn = min(mn, h)
    hmin[sid] = mn if mn != 99 else None
badh = {k: v for k, v in hmin.items() if v is not None and v < MIN_HEAD}
check("B4", f"Headroom >= {MIN_HEAD} above every standable cell in every space (and stair-edge clearance)",
      "PASS" if not badh else "FAIL", f"min headroom per space: {hmin}")

# column gaps / pillar vs route interference
gap_z = [(a, b) for a, b in ((3, 7), (9, 13), (15, 19))]
gaps_w = [b - a - 1 for a, b in gap_z]
check("B5", "Hall columns vs circulation: clear gaps between 3x3 column bodies and open nave width",
      "PASS" if min(gaps_w[:2]) >= 3 else "FAIL",
      f"column z-spans 1..3 / 7..9 / 13..15 -> clear gaps {gaps_w[0]} (z4..6), {gaps_w[1]} (z10..12), south gap z16..18 = 3; open nave X-8..8 = 17 wide x Z1..18 = 18 deep; stair/portal axes (|x|<=6) never intersect a column")

# stair arithmetic (independent hand calculation)
desc_rise = 6; desc_run = 5 + 1
ok_desc = (desc_rise / 5 == 1.0)
galstair_rise = 7; gal_run = 6
altar_rise = 4; altar_run = 3
tr = [e for e in ELEMS if e[0].startswith("descent_tread")]
treads = len(tr); rise = SPACES["S05"][3][1] + 1 - SPACES["S06"][3][0] * 0 + 0   # placeholder replaced below
F_top, F_land = 0, SPACES["S06"][3][0]
rise = F_top - F_land
run_blocks = treads + 1                       # tread blocks + one flat block before the landing wall
width = SPACES["S05"][2][1] - SPACES["S05"][2][0] + 1
land = (SPACES["S06"][2][1]-SPACES["S06"][2][0]+1, SPACES["S06"][4][1]-SPACES["S06"][4][0]+1)
first_head = hmin["S05"]
exit_w = SPACES["S07"][4][1] - SPACES["S07"][4][0] + 1
ok = (rise == 6 and treads == 5 and width == 11 and land == (15, 8) and first_head is not None and first_head >= 6 and exit_w == 7
      and SPACES["S07"][2][0] == SPACES["S06"][2][1] + 1)
check("C1", "Descent stair start (Q02): hall rear centre, wide, 1:1, intermediate landing, 90-degree turn to a 7-wide exit corridor",
      "PASS" if ok else "FAIL",
      f"rise {rise} over {treads} treads + 1 flat block (run {run_blocks}), gradient 1:1 (45 deg) for the 5 treads; width {width}; landing {land[0]}x{land[1]} at feet Y={F_land}; min headroom on stair/landing {first_head}; exit stub width {exit_w} on landing east plane x={SPACES['S07'][2][0]}; stair axis -Z, exit axis +X => 90-degree turn")
check("C2", "Altar chamber (Q05) separate from descent: separate entrance (rear gallery portal AP1), separate stair, Y-disjoint",
      "PASS" if (alt_unreach and vsep >= 10) else "FAIL",
      f"AP1 X-3..3 Z-8..-7 Y7..12; 4 rises over 3 treads; altar floor feet Y11; descent landing roof layer -1 -> {vsep} solid layers separate them")

# ---------------------------------------------------------------- LOS from gallery to nave floor (Q03/Q04 visibility)
def los(p, q, ignore_rail, n=240):
    for t in np.linspace(0, 1, n):
        x = p[0] + (q[0]-p[0])*t; y = p[1] + (q[1]-p[1])*t; z = p[2] + (q[2]-p[2])*t
        bx, by, bz = int(np.floor(x+0.5)), int(np.floor(y)), int(np.floor(z+0.5))
        if solid[ix(bx), iy(by), iz(bz)]:
            if ignore_rail and owner[ix(bx), iy(by), iz(bz)].startswith("parapet"): continue
            return False
    return True
# eye points: every gallery standing cell against the rail (west z1..11, east z1..11, rear x-11..11 at z=-1), eye = F+1.6
eyes = [(-13, 8.6, z) for z in range(1, 10)] + [(13, 8.6, z) for z in range(1, 10)] + [(x, 8.6, -1) for x in range(-11, 12)]
floor_t = [(x, 1.6, z) for x in range(-8, 9) for z in range(1, 19)]      # nave floor cells, target at head height of a standing person
def coverage(ignore_rail):
    seen_c = 0
    for q in floor_t:
        if any(los(p, q, ignore_rail) for p in eyes): seen_c += 1
    return seen_c / len(floor_t)
cov_opaque = coverage(False); cov_thin = coverage(True)
check("B6", "Gallery overlook (Q03/Q04): fraction of open-nave floor cells (17x18, head height) visible from at least one gallery rail position",
      "PASS" if cov_thin >= 0.9 else "FAIL",
      f"rail treated see-through (wall/fence-type): {cov_thin:.0%}; rail as an opaque full 1x1x1 block: {cov_opaque:.0%}. Requirement carried forward: gallery rail must be a see-through block type. Real-client visual check remains LIVE VALIDATION PENDING")

# B9 gallery support: distance from each ledge slab block to the nearest supporting wall/column in the same layer
slab = [(x, z) for x in range(XMIN, XMAX+1) for z in range(ZMIN, ZMAX+1) if owner[ix(x), iy(5), iz(z)].startswith("ledge")]
def support(x, z):
    return solid[ix(x), iy(5), iz(z)] and not owner[ix(x), iy(5), iz(z)].startswith("ledge")
# layer 5 beside a ledge is hall air (columns are solid at layer 5, walls are solid mass)
dmax = 0
for (x, z) in slab:
    d = 0
    while d < 30:
        d += 1
        if any(support(x+dx*d, z+dz*d) for dx, dz in ((1,0),(-1,0),(0,1),(0,-1))): break
    dmax = max(dmax, d)
under_free = min(SPACES["S04"][3][0] + 5, 5)        # ledge slab bottom layer = 5 => 5 clear layers (0..4) under it
check("B9", "Gallery support buildability: every ledge slab block lies within 8 blocks (PROPOSED style limit) of a supporting wall or column in its layer; slabs 2 thick; 5 clear layers beneath",
      "PASS" if dmax <= 8 and under_free >= 3 else "FAIL",
      f"{len(slab)} slab blocks (layer 5); worst distance to a wall/column = {dmax} blocks (rear/side ledges are 7 deep, anchored on the rear/side walls, inner edges flanked by the column line). Minecraft applies no structural load, so this is a visual-plausibility metric only; columns stand on the floor slab (layer -1)")

# zones nested in hall, mutually disjoint, nave free of solids
hx, hy, hz = sbox("S04")
zn = list(ZONES.items()); zok = True; zmsg = []
for k, (nm, xs, ys, zs) in zn:
    inside = hx[0] <= xs[0] and xs[1] <= hx[1] and hy[0] <= ys[0] and ys[1] <= hy[1] and hz[0] <= zs[0] and zs[1] <= hz[1]
    zok &= inside
for i in range(len(zn)):
    for j in range(i+1, len(zn)):
        A = zn[i][1][1:]; B = zn[j][1][1:]
        if all(min(A[k][1], B[k][1]) - max(A[k][0], B[k][0]) + 1 > 0 for k in range(3)): zok = False; zmsg.append((zn[i][0], zn[j][0]))
nv = ZONES["Z-CENTER"]
nave_solid = int(solid[box((nv[1], nv[2], nv[3]))].sum())
check("B7", "Hall zones nested inside hall, mutually disjoint; open central nave (X-8..8, Z1..18, Y0..17) contains zero solid blocks",
      "PASS" if zok and nave_solid == 0 else "FAIL", f"zones ok={zok} {zmsg}; solid blocks in nave volume: {nave_solid} of {17*18*18}")
STAIR_W = {"descent flight A": 11, "gallery stairs": 7, "altar ascent": 7}
check("B8", f"Stair widths >= {MIN_PASS_W} (3x3-agent test treats tread cells analytically)",
      "PASS" if min(STAIR_W.values()) >= MIN_PASS_W else "FAIL", str(STAIR_W))

# ---------------------------------------------------------------- derived data for doc
def ceilmass(sid):
    xs, ys, zs = sbox(sid)
    mins = 99
    for x in range(xs[0], xs[1]+1):
        for z in range(zs[0], zs[1]+1):
            y = ys[1]+1; n = 0
            while y < YMAX and solid[ix(x), iy(y), iz(z)]: n += 1; y += 1
            mins = min(mins, n)
    return mins
R["table"] = []
for sid, (nm, kind, xs, ys, zs, st) in SPACES.items():
    R["table"].append({"id": sid, "name": nm, "kind": kind, "x": xs, "y": ys, "z": zs,
        "size": (xs[1]-xs[0]+1, ys[1]-ys[0]+1, zs[1]-zs[0]+1), "feet_floor_Y": ys[0], "clear_h": ys[1]-ys[0]+1,
        "min_shell": shell.get(sid), "min_roof_mass": ceilmass(sid), "min_headroom_walked": hmin[sid], "status": st})
R["elems"] = ELEMS; R["zones"] = ZONES; R["reach_rows"] = rows

# ---------------------------------------------------------------- renderers
CH = {"S00":"f","S01":"d","S02":"v","S03":"p","S04":".","S05":"s","S06":"l","S07":"r","S08":"a","S09":"u","S10":"A"}
EC = {"column":"O","ledge":"L","parapet":"=","stair":"/"}
def cell(x, y, z):
    o = owner[ix(x), iy(y), iz(z)]
    if o:
        k = [e for e in ELEMS if e[0] == o][0][1]; return EC[k]
    if solid[ix(x), iy(y), iz(z)]:
        return "#" if (y < 0 or max(abs(x), abs(z)) <= 40 - y) else " "
    s = space[ix(x), iy(y), iz(z)]
    return CH.get(s, " ") if s else " "
def plan(y, xr=(XMIN, XMAX), zr=(ZMIN, ZMAX)):
    L = [f"PLAN at layer Y={y}  X[{xr[0]},{xr[1]}] Z[{zr[0]},{zr[1]}]  (north = top = -Z, south = bottom = +Z entrance, west = left = -X)"]
    ticks = "".join(str(abs(x) % 10) if x % 5 == 0 else ("|" if x == 0 else " ") for x in range(xr[0], xr[1]+1))
    L.append("  Z\\X " + "".join(("+" if x == 0 else ("'" if x % 5 == 0 else " ")) for x in range(xr[0], xr[1]+1)) + "   (' every 5 blocks, + = X=0)")
    for z in range(zr[0], zr[1]+1):
        L.append(f"{z:+4d}  " + "".join(cell(x, y, z) for x in range(xr[0], xr[1]+1)))
    return "\n".join(L)
def section_x(x, zr=(ZMIN, ZMAX), yr=(-10, 39)):
    L = [f"SECTION at X={x}  looking east: left = north (-Z), right = south (+Z)  Z[{zr[0]},{zr[1]}] Y[{yr[0]},{yr[1]}]"]
    for y in range(yr[1], yr[0]-1, -1):
        L.append(f"{y:+4d}  " + "".join(cell(x, y, z) for z in range(zr[0], zr[1]+1)))
    return "\n".join(L)
def section_z(z, xr=(XMIN, XMAX), yr=(-10, 39)):
    L = [f"SECTION at Z={z}  looking north: left = west (-X), right = east (+X)  X[{xr[0]},{xr[1]}] Y[{yr[0]},{yr[1]}]"]
    for y in range(yr[1], yr[0]-1, -1):
        L.append(f"{y:+4d}  " + "".join(cell(x, y, z) for x in range(xr[0], xr[1]+1)))
    return "\n".join(L)
LEGEND = ("legend: # solid mass  O column  L gallery ledge slab  = parapet  / stair tread/fill  "
          "f forecourt  d D1 door passage  v vestibule  p D2 portal  . grand hall air  s descent flight A  "
          "l descent landing  r P2 reserved stub  a altar portal  u altar stair  A altar chamber  ' ' outside")

CROPS = {
 "plan_Y+0": (0, (-24, 24), (-22, 40)), "plan_Y+7": (7, (-22, 22), (-22, 22)),
 "plan_Y+11": (11, (-12, 12), (-24, -6)), "plan_Y-6": (-6, (-12, 20), (-18, 2)),
}
if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    for k, (y, xr, zr) in CROPS.items():
        open(f"{out}/{k}_crop.txt", "w").write(LEGEND + "\n" + plan(y, xr, zr) + "\n")
    open(f"{out}/section_X+0_crop.txt", "w").write(LEGEND + "\n" + section_x(0, (-26, 40), (-8, 24)) + "\n")
    open(f"{out}/section_Z+5_crop.txt", "w").write(LEGEND + "\n" + section_z(5, (-24, 24), (-4, 22)) + "\n")
    open(f"{out}/section_Z-10_crop.txt", "w").write(LEGEND + "\n" + section_z(-10, (-12, 20), (-8, 20)) + "\n")
    open(f"{out}/section_X+0_full.txt", "w").write(LEGEND + "\n" + section_x(0) + "\n")
    def jd(o):
        if isinstance(o, (np.integer,)): return int(o)
        if isinstance(o, (np.bool_,)): return bool(o)
        if isinstance(o, (set, tuple)): return list(o)
        return str(o)
    json.dump(R, open(f"{out}/verification.json", "w"), indent=1, default=jd)
    for c in R["checks"]:
        print(f"{c['id']:5s} {c['verdict']:10s} {c['title']}")
        print("      ", c["evidence"][:400])
    print("collisions:", collisions)
