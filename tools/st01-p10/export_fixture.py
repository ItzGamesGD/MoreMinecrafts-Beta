#!/usr/bin/env python3
"""ST-01 P10: export the P6 integrated (candidate C) voxel world as a compact game-test fixture.
Reuses the UNMODIFIED P1-P6 engines (read-only). Output per side: CLOSED solid mask, element-kind layer, cover cells, goals, digests.
Usage: python3 -I export_fixture.py <out_dir>"""
import sys, os, json, hashlib
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(TOOLS, "st01-p6"))
import st01_p6_verify as V6
C = V6.C; S3 = V6.S3; S4 = V6.S4; OV = V6.OV

REGION = None   # filled per side: bounding box of all cavities incl. hall reservation + 1 margin

def build(side):
    overlay = OV.extract()
    p3 = S3.make_hall_east(); p4 = S4.make_variant("B")
    if side == "W": p3 = S3.mirror_spec(p3); p4 = S4.mirror_p4(p4)
    r = V6.verify_p6(overlay, p4, p3, side, label="P10-" + side, skip_regress=True)
    return r, p3, p4

def rle(mask_bytes):
    out = []; prev = mask_bytes[0]; n = 0
    for v in mask_bytes:
        if v == prev: n += 1
        else: out.append([int(prev), n]); prev = v; n = 1
    out.append([int(prev), n]); return out

def export(side, outdir):
    r, p3, p4 = build(side)
    W = r.W; closed = r.closed_solid; opened = r.open_solid
    h = p3["hall"]
    # region = hall reservation + everything that is cavity in the P1/P2/P4 structure
    own = W["own"]; pts = np.argwhere(own >= 0)
    lo = np.minimum(pts.min(0), [C.ix(h["res"][0]), C.iy(h["res"][2]), C.iz(h["res"][4])])
    hi = np.maximum(pts.max(0), [C.ix(h["res"][1]), C.iy(h["res"][3]), C.iz(h["res"][5])])
    lo = lo - 1; hi = hi + 1
    x0, y0, z0 = int(lo[0] + C.GX[0]), int(lo[1] + C.GY[0]), int(lo[2] + C.GZ[0])
    x1, y1, z1 = int(hi[0] + C.GX[0]), int(hi[1] + C.GY[0]), int(hi[2] + C.GZ[0])
    sl = (slice(C.ix(x0), C.ix(x1) + 1), slice(C.iy(y0), C.iy(y1) + 1), slice(C.iz(z0), C.iz(z1) + 1))
    cs = closed[sl]
    # element kind layer: 0 = plain rock/shell, else index into kinds (P3 hall elements only; P1/P2/P4 are cavities)
    W3 = W["W3"]; elem = W3["elem"][sl]; eids = W3["eids"]; kinds = list(dict.fromkeys(W3["kind"].tolist()))
    kind_idx = np.zeros(elem.shape, np.int16)
    for n, k in enumerate(W3["kind"].tolist()):
        kind_idx[elem == n] = kinds.index(k) + 1
    # P4 elements (stairs, beams, chests...) come from the P5 world as additional solids; keep them as kind "p4"
    solid_flat = cs.astype(np.uint8).ravel(order="F")           # x fastest
    kind_flat = kind_idx.astype(np.uint8).ravel(order="F")
    data = dict(side=r.W["spec"]["side"] if "spec" in W else side, region=[x0, x1, y0, y1, z0, z1], kinds=kinds,
                solid_rle_xyz=rle(solid_flat), kind_rle_xyz=rle(kind_flat),
                cover_cells=[list(c) for c in r.cells], hall=h, goals={k: v for k, v in p4["goals"].items()},
                spawn=p3["spawn"], hatch=p3.get("hatch"), chest_primary_access=p3["chest_primary_access"],
                closed_digest=V6.digest(closed), open_digest=V6.digest(opened),
                closed_region_digest=hashlib.sha256(np.packbits(cs).tobytes()).hexdigest()[:16],
                n_solid=int(cs.sum()), n_cells=int(cs.size))
    os.makedirs(outdir, exist_ok=True)
    json.dump(data, open(os.path.join(outdir, f"p10_fixture_{side}.json"), "w"), separators=(",", ":"))
    return data, closed[sl]

if __name__ == "__main__":
    out = sys.argv[1]
    for sd in ("E", "W"):
        d, cs = export(sd, out)
        print(sd, "region", d["region"], "cells", d["n_cells"], "solid", d["n_solid"], "rle runs", len(d["solid_rle_xyz"]), "kinds", d["kinds"], "digest", d["closed_region_digest"])
