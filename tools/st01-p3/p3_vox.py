"""ST-01 P3 voxelisation + walk/agent search helpers (design-only; reuses the unmodified P2 engine p2_core).
World = P2 voxel grid (P2 cavities already carved, royal-hall reservation still solid) + P3 hall carved into the reservation."""
import os, sys
from collections import deque
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__))
P2DIR = os.path.join(HERE, "..", "st01-p2")
sys.path.insert(0, P2DIR)
import p2_core as C

def sl(b):
    return (slice(C.ix(b[0]), C.ix(b[1]) + 1), slice(C.iy(b[2]), C.iy(b[3]) + 1), slice(C.iz(b[4]), C.iz(b[5]) + 1))

def build_world(spec, p2model):
    G = C.build(p2model)
    solid = G["solid"].copy(); owner = G["owner"]
    shp = solid.shape
    h = spec["hall"]; F, cx, cz, half = h["floor"], h["cx"], h["cz"], h["half"]
    inres = np.zeros(shp, bool); inres[sl(h["res"])] = True
    main = np.zeros(shp, bool)
    for y in range(F, F + h["wall_h"]):
        main[sl([cx - half, cx + half, y, y, cz - half, cz + half])] = True
    for j in range(1, h["rise"] + 1):
        y = F + h["wall_h"] - 1 + j; hj = half - h["run"] * j
        if hj >= 0: main[sl([cx - hj, cx + hj, y, y, cz - hj, cz + hj])] = True
    W = dict(spec=spec, G=G, p2model=p2model, inres=inres, main=main)
    W["main_outside_res"] = int((main & ~inres).sum())
    W["main_on_p2cav"] = int((main & (owner >= 0)).sum())
    solid[main & inres & (owner < 0)] = False
    recess = np.zeros(shp, bool); rid = np.full(shp, -1, np.int16); rnames = list(spec["recesses"])
    W["recess_bad"] = []
    for k, r in spec["recesses"].items():
        s = sl(r["box"]);
        before = solid[s]
        if (~before).any(): W["recess_bad"].append((k, "recess overlaps existing cavity", int((~before).sum())))
        outside = ~inres[s]
        if outside.any(): W["recess_bad"].append((k, "recess leaves the reservation", int(outside.sum())))
        solid[s] = False; recess[s] = True; rid[s] = rnames.index(k)
    elem = np.full(shp, -1, np.int16); eids = list(spec["elements"]); W["elem_overlap"] = 0; W["elem_into_solid"] = []
    for n, k in enumerate(eids):
        for b in spec["elements"][k]["boxes"]:
            s = sl(b)
            ov = int((elem[s] >= 0).sum()); W["elem_overlap"] += ov
            ins = int(solid[s].sum())
            if ins: W["elem_into_solid"].append((k, ins))
            elem[s] = n; solid[s] = True
    W.update(solid=solid, owner=owner, recess=recess, rid=rid, rnames=rnames, elem=elem, eids=eids,
             kind=np.array([spec["elements"][k]["kind"] for k in eids]))
    shell = inres & solid & (elem < 0) & (owner < 0)
    W["shell"] = shell
    return W

def ekind(W, kind):
    idx = [i for i, k in enumerate(W["kind"]) if k == kind]
    return np.isin(W["elem"], idx) if idx else np.zeros_like(W["solid"])

def eprefix(W, prefix):
    idx = [i for i, k in enumerate(W["eids"]) if k.startswith(prefix)]
    return np.isin(W["elem"], idx) if idx else np.zeros_like(W["solid"])

def cells(mask):
    p = np.argwhere(mask)
    return [(int(a) + C.GX[0], int(b) + C.GY[0], int(c) + C.GZ[0]) for a, b, c in p]

def bfs_dist(solid, start, st, blocked=None):
    seen = {start: 0}; dq = deque([start])
    while dq:
        x, y, z = dq.popleft(); d = seen[(x, y, z)]
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, zz = x + dx, z + dz
            if not (C.GX[0] < xx < C.GX[1] and C.GZ[0] < zz < C.GZ[1]): continue
            for dy in (0, 1, -1):
                yy = y + dy
                if not st[C.ix(xx), C.iy(yy), C.iz(zz)]: continue
                if dy == 1 and solid[C.ix(x), C.iy(y + 2), C.iz(z)]: continue
                if blocked is not None and blocked[C.ix(xx), C.iy(yy), C.iz(zz)]: continue
                n = (xx, yy, zz)
                if n not in seen: seen[n] = d + 1; dq.append(n)
                break
    return seen

def agent_reach(solid, start, w, h, blocked=None, max_drop=1):
    """Footprint w x w (cells), height h; state = min corner (x,y,z). A state is valid when the whole w*w*h volume is free and every
    supporting cell below is solid. Moves: 4-neighbours with dy in {0,+1,-1}; +1 needs the swept volume above the start to be free."""
    free = ~solid
    def ok(x, y, z):
        if not (C.GX[0] < x and x + w - 1 < C.GX[1] and C.GZ[0] < z and z + w - 1 < C.GZ[1]): return False
        a = (slice(C.ix(x), C.ix(x) + w), slice(C.iy(y), C.iy(y) + h), slice(C.iz(z), C.iz(z) + w))
        if not free[a].all(): return False
        b = (slice(C.ix(x), C.ix(x) + w), C.iy(y) - 1, slice(C.iz(z), C.iz(z) + w))
        if not solid[b].all(): return False
        if blocked is not None and blocked[a].any(): return False
        return True
    if not ok(*start): return set()
    seen = {start}; dq = deque([start])
    while dq:
        x, y, z = dq.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                n = (x + dx, y + dy, z + dz)
                if n in seen or not ok(*n): continue
                if dy == 1:
                    a = (slice(C.ix(x), C.ix(x) + w), slice(C.iy(y), C.iy(y) + h + 1), slice(C.iz(z), C.iz(z) + w))
                    if not free[a].all(): continue
                seen.add(n); dq.append(n); break
    return seen
