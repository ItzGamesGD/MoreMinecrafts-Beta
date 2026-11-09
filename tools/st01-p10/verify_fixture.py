#!/usr/bin/env python3
"""ST-01 P10 fixture verification (design-time, read-only on P1-P6 engines).
Checks, independently of the Java game tests:
  V1  exported CLOSED world digest equals the digest recorded in the approved P6 model JSON (same engines, same overlay) for EAST and WEST
  V2  decoded fixture region == fresh engine world restricted to the region (RLE round trip is lossless)
  V3  WEST fixture is the exact X mirror of EAST (solid mask and element-kind layer)
  V4  every P3 element box of the approved hall spec is solid with the right kind in the fixture; counts per kind; hall interior is 33x33 clear, wall height 8, corbel steps
  V5  the 12 cover cells are solid in CLOSED, are exactly X 12..15 / dy -3 / Z 0..2 relative to the royal chest, and turn into air only in the OPEN world
  V6  mutation controls: a flipped voxel, a shifted element and a wrong-side import are each detected
Usage: python3 -I verify_fixture.py <fixture_dir> <out_json>   (exit 1 on any FAIL)"""
import sys, os, json, copy
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
sys.path.insert(0, HERE)
import export_fixture as X
V6, C, S3 = X.V6, X.C, X.S3

def decode(d):
    x0, x1, y0, y1, z0, z1 = d["region"]; nx, ny, nz = x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1
    def un(rle, dtype):
        out = np.concatenate([np.full(n, v, dtype) for v, n in rle]); return out.reshape((nx, ny, nz), order="F")
    return un(d["solid_rle_xyz"], np.uint8).astype(bool), un(d["kind_rle_xyz"], np.uint8)

def main(fxdir, out):
    res = []
    def add(i, title, ok, ev): res.append(dict(id=i, title=title, verdict="PASS" if ok else "FAIL", evidence=ev))
    approved = {sd: json.load(open(os.path.join(TOOLS, "..", "docs/design/st01-p6/generated", f"p6_model_{n}.json"))) for sd, n in (("E", "east"), ("W", "west"))}
    data = {}; masks = {}; kinds = {}; worlds = {}
    for sd in ("E", "W"):
        data[sd] = json.load(open(os.path.join(fxdir, f"p10_fixture_{sd}.json")))
        masks[sd], kinds[sd] = decode(data[sd])
        r, p3, p4 = X.build(sd); worlds[sd] = (r, p3, p4)
    for sd in ("E", "W"):
        r, p3, p4 = worlds[sd]
        add(f"V1.{sd}", "CLOSED/OPEN world digests equal the approved P6 model digests", V6.digest(r.closed_solid) == approved[sd]["closed_digest"] == data[sd]["closed_digest"] and V6.digest(r.open_solid) == approved[sd]["open_digest"] == data[sd]["open_digest"],
            f"closed {V6.digest(r.closed_solid)} (P6 {approved[sd]['closed_digest']}), open {V6.digest(r.open_solid)} (P6 {approved[sd]['open_digest']})")
        x0, x1, y0, y1, z0, z1 = data[sd]["region"]
        sl = (slice(C.ix(x0), C.ix(x1) + 1), slice(C.iy(y0), C.iy(y1) + 1), slice(C.iz(z0), C.iz(z1) + 1))
        add(f"V2.{sd}", "Decoded fixture region equals a fresh engine world cell for cell", np.array_equal(masks[sd], r.closed_solid[sl]), f"region {data[sd]['region']} cells {masks[sd].size} solid {int(masks[sd].sum())}")
        outside = int(r.closed_solid.sum()) - int(masks[sd].sum())
        # nothing structural lies outside the region: every cavity/hall cell of the world is inside it
        pts = np.argwhere(r.W["own"] >= 0)
        inside = all(x0 <= p[0] + C.GX[0] <= x1 and y0 <= p[1] + C.GY[0] <= y1 and z0 <= p[2] + C.GZ[0] <= z1 for p in pts)
        add(f"V2b.{sd}", "Region covers every P1/P2/P4 cavity cell and the whole hall reservation", inside, f"{len(pts)} cavity cells checked; solid cells outside region (natural rock, not exported) {outside}")
    # V3 mirror
    e, w = masks["E"], masks["W"]
    mirror = np.array_equal(e, w[::-1]) and np.array_equal(kinds["E"], kinds["W"][::-1]) and data["E"]["region"][:2] == [-data["W"]["region"][1], -data["W"]["region"][0]]
    add("V3", "WEST fixture is the exact X mirror of EAST (solid mask, element-kind layer, region)", mirror, f"solid cells {int(e.sum())}/{int(w.sum())}, kinds equal after mirror {np.array_equal(kinds['E'], kinds['W'][::-1])}")
    # V4 element boxes of the approved P3 hall spec (independent box arithmetic)
    for sd in ("E", "W"):
        r, p3, p4 = worlds[sd]; d = data[sd]; x0, x1, y0, y1, z0, z1 = d["region"]; m, k = masks[sd], kinds[sd]
        bad = []; counts = {}
        for name, el in p3["elements"].items():
            for b in el["boxes"]:
                cells = m[b[0] - x0:b[1] - x0 + 1, b[2] - y0:b[3] - y0 + 1, b[4] - z0:b[5] - z0 + 1]
                if not cells.all(): bad.append((name, "not solid"))
                ki = d["kinds"].index(el["kind"]) + 1 if el["kind"] in d["kinds"] else -1
                kk = k[b[0] - x0:b[1] - x0 + 1, b[2] - y0:b[3] - y0 + 1, b[4] - z0:b[5] - z0 + 1]
                if not (kk == ki).all(): bad.append((name, "kind layer"))
            counts[el["kind"]] = counts.get(el["kind"], 0) + 1
        npil = len({n.split(":")[0] for n in p3["elements"] if n.startswith("P_")})
        add(f"V4a.{sd}", "All P3 element boxes (10 pillars with base/shaft/capital, lintels, obelisk, dais, sarcophagus, chest, statues, braziers) are solid with the right element kind", not bad and npil == 10, f"pillars {npil}; element kinds {counts}; problems {bad[:5]}")
        h = p3["hall"]; F = h["floor"]; cx, cz, half = h["cx"], h["cz"], h["half"]
        interior = r.W["W3"]["main"]
        # interior clear volume at wall height: all non-element cells inside the 33x33 footprint must be air for F..F+7
        footprint = np.zeros_like(m)
        footprint[cx - half - x0:cx + half - x0 + 1, F - y0:F + h["wall_h"] - y0, cz - half - z0:cz + half - z0 + 1] = True
        elem_solid = (k > 0)
        non_el_solid = int((footprint & m & ~elem_solid).sum())
        add(f"V4b.{sd}", "Hall interior is the 33x33 footprint, 8 high with only the listed elements solid inside", non_el_solid == 0 and 2 * half + 1 == 33 and h["wall_h"] == 8, f"33x33 half={half}; wall_h={h['wall_h']}; solid non-element cells inside footprint x height: {non_el_solid}")
        corbel = []; corbel_ok = True
        for j in range(1, h["rise"] + 1):
            y = F + h["wall_h"] - 1 + j; hj = half - h["run"] * j
            if hj < 0: continue
            ring = (slice(cx - hj - x0, cx + hj - x0 + 1), y - y0, slice(cz - hj - z0, cz + hj - z0 + 1))
            air = int((~m[ring]).sum()); elem_in = int((k[ring] > 0).sum()); total = (2 * hj + 1) ** 2
            # one cell outside the ring (the corbel shoulder) must be solid all round
            outer = np.zeros(m.shape[0::2], bool); outer[cx - hj - 1 - x0:cx + hj + 2 - x0, cz - hj - 1 - z0:cz + hj + 2 - z0] = True; outer[cx - hj - x0:cx + hj + 1 - x0, cz - hj - z0:cz + hj + 1 - z0] = False
            shoulder_solid = bool(m[:, y - y0, :][outer].all())
            corbel.append((j, hj, air, total, elem_in, shoulder_solid))
            corbel_ok &= (air == total - elem_in) and shoulder_solid
        add(f"V4c.{sd}", "Corbel ceiling: each of the 8 step rings (1 rise per 2 run) is open air up to its half-width (minus listed elements) with a solid shoulder", corbel_ok and len(corbel) == 8, f"(step, half, air, footprint, element cells, shoulder solid): {corbel}")
    # V5 cover
    for sd in ("E", "W"):
        r, p3, p4 = worlds[sd]; d = data[sd]; x0, x1, y0, y1, z0, z1 = d["region"]; m = masks[sd]
        h = p3["hall"]; anchor = (h["cx"], h["floor"] + 2, h["cz"] + 12); sgn = 1 if sd == "E" else -1
        expect = sorted((anchor[0] + sgn * dx, anchor[1] - 3, anchor[2] + dz) for dx in range(12, 16) for dz in range(0, 3))
        cells = sorted(tuple(c) for c in d["cover_cells"])
        closed_all = all(m[c[0] - x0, c[1] - y0, c[2] - z0] for c in cells)
        op = r.open_solid; open_air = all(not op[C.ix(c[0]), C.iy(c[1]), C.iz(c[2])] for c in cells)
        diff = int((r.closed_solid != r.open_solid).sum())
        add(f"V5.{sd}", "Cover = 12 cells at anchor-relative dx 12..15 (mirrored for WEST), dy -3, dz 0..2; solid when CLOSED, air when OPEN; CLOSED vs OPEN differ in exactly those 12 cells", cells == expect and closed_all and open_air and diff == 12 and len(cells) == 12, f"cells {len(cells)}, equal to anchor-relative formula {cells == expect}, closed solid {closed_all}, open air {open_air}, closed/open diff {diff}")
    # V6 mutation controls (the verifier must be able to fail)
    mut = []
    r, p3, p4 = worlds["E"]; d = data["E"]
    fresh = r.closed_solid.copy(); x0, x1, y0, y1, z0, z1 = d["region"]
    sl = (slice(C.ix(x0), C.ix(x1) + 1), slice(C.iy(y0), C.iy(y1) + 1), slice(C.iz(z0), C.iz(z1) + 1))
    m1 = masks["E"].copy(); i = tuple(np.argwhere(m1)[1000]); m1[i] = False
    mut.append(("flip one solid voxel to air", not np.array_equal(m1, fresh[sl])))
    m2 = masks["E"].copy(); i = tuple(np.argwhere(~m2)[1000]); m2[i] = True
    mut.append(("flip one air voxel to solid", not np.array_equal(m2, fresh[sl])))
    m3 = np.roll(masks["E"], 1, axis=0)
    mut.append(("shift the whole world by one block in X", not np.array_equal(m3, fresh[sl])))
    mut.append(("import WEST data as EAST", not np.array_equal(masks["W"], masks["E"])))
    mirror_bad = masks["W"].copy(); mirror_bad[5, 5, 5] = not mirror_bad[5, 5, 5]
    mut.append(("break the mirror by one voxel", not np.array_equal(masks["E"], mirror_bad[::-1])))
    add("V6", "Mutation controls: each injected defect is detected", all(ok for _, ok in mut), "; ".join(f"{n}: {'detected' if ok else 'MISSED'}" for n, ok in mut))
    fails = [c for c in res if c["verdict"] == "FAIL"]
    json.dump(dict(checks=res, summary=dict(pass_=sum(c["verdict"] == "PASS" for c in res), fail=len(fails))), open(out, "w"), indent=1)
    for c in res: print(c["verdict"], c["id"], c["title"][:100]); 
    for c in fails: print("   FAIL evidence:", c["evidence"][:400])
    return 1 if fails else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
