#!/usr/bin/env python3
"""Independent consistency check: converted bone-model JSON + texture vs the ORIGINAL .bbmodel.

Does not call the converter. Verifies cube count, bone hierarchy/pivots, cube bounding boxes in Minecraft space, per-face UV
rectangles, outward normals/winding, texture bytes and size, and the recorded source hash.
Usage: validate_converted_model.py SOURCE.bbmodel MODEL.json TEXTURE.png
"""
import base64
import hashlib
import json
import struct
import sys

TOL = 1e-4
src_path, model_path, tex_path = sys.argv[1:4]
raw = open(src_path, "rb").read()
bb = json.loads(raw)
model = json.load(open(model_path, encoding="utf-8"))
errors = []


def err(msg):
    errors.append(msg)


def close(a, b):
    return abs(a - b) <= TOL


# --- source / texture integrity
if model["source"]["sha256"] != hashlib.sha256(raw).hexdigest():
    err("recorded source sha256 does not match the .bbmodel")
png = open(tex_path, "rb").read()
if png != base64.b64decode(bb["textures"][0]["source"].split(",", 1)[1]):
    err("texture PNG differs from the texture embedded in the .bbmodel")
w, h = struct.unpack(">II", png[16:24])
if (w, h) != (bb["resolution"]["width"], bb["resolution"]["height"]):
    err("texture size %dx%d != model resolution" % (w, h))
if model["texture"]["uv_width"] != w or model["texture"]["uv_height"] != h:
    err("model texture size mismatch")

# --- hierarchy and pivots (absolute Minecraft-space pivots recomputed from the source outliner)
elements = {e["uuid"]: e for e in bb["elements"]}
src_bones = {}


def walk(node, parent):
    src_bones[node["name"]] = (parent, node.get("origin", [0, 0, 0]), [c for c in node["children"] if isinstance(c, str)])
    for c in node["children"]:
        if not isinstance(c, str):
            walk(c, node["name"])


for top in bb["outliner"]:
    if isinstance(top, dict):
        walk(top, None)
out_bones = {b["name"]: b for b in model["bones"]}
if set(src_bones) != set(out_bones):
    err("bone set differs: %s" % (set(src_bones) ^ set(out_bones)))
abs_pivot = {}
for name, b in out_bones.items():
    parent, origin, kids = src_bones[name]
    if b["parent"] != parent:
        err("%s parent %s != %s" % (name, b["parent"], parent))
    exp = [-origin[0], 24 - origin[1], origin[2]]
    if not all(close(a, e) for a, e in zip(b["pivot"], exp)):
        err("%s pivot %s != %s" % (name, b["pivot"], exp))
    pivot_parent = abs_pivot.get(parent, [0, 0, 0]) if parent else [0, 0, 0]
    abs_pivot[name] = [pivot_parent[i] + b["offset"][i] for i in range(3)]
    if not all(close(a, e) for a, e in zip(abs_pivot[name], exp)):
        err("%s accumulated offsets %s != absolute pivot %s" % (name, abs_pivot[name], exp))
    if sorted(c["name"] for c in b["cubes"]) != sorted(elements[u]["name"] for u in kids):
        err("%s cube set differs" % name)

# --- cubes
total = 0
for name, b in out_bones.items():
    for c in b["cubes"]:
        total += 1
        el = next(e for e in bb["elements"] if e["name"] == c["name"])
        f, t = el["from"], el["to"]
        mn = [min(-f[0], -t[0]), min(24 - f[1], 24 - t[1]), min(f[2], t[2])]
        mx = [max(-f[0], -t[0]), max(24 - f[1], 24 - t[1]), max(f[2], t[2])]
        rot = el.get("rotation") or [0, 0, 0]
        is_rot = any(rot)
        if c["rotated"] != is_rot:
            err("%s rotated flag" % c["name"])
        if is_rot and not all(close(a, e) for a, e in zip(c["rotation"], [-rot[0], -rot[1], rot[2]])):
            err("%s rotation %s" % (c["name"], c["rotation"]))
        origin = c["origin"]
        if not is_rot and not all(close(a, e) for a, e in zip(origin, out_bones[name]["pivot"])):
            err("%s unrotated cube origin must be its bone pivot" % c["name"])
        if is_rot:
            exp_o = [-el["origin"][0], 24 - el["origin"][1], el["origin"][2]]
            if not all(close(a, e) for a, e in zip(origin, exp_o)):
                err("%s cube origin %s != %s" % (c["name"], origin, exp_o))
        # bounding box of all vertices (relative to origin) must equal the source box (relative to origin)
        pts = [v[:3] for face in c["faces"].values() for v in face["vertices"]]
        for axis in range(3):
            lo = min(p[axis] for p in pts) + origin[axis]
            hi = max(p[axis] for p in pts) + origin[axis]
            if not (close(lo, mn[axis]) and close(hi, mx[axis])):
                err("%s axis %d bounds %.3f..%.3f != %.3f..%.3f" % (c["name"], axis, lo, hi, mn[axis], mx[axis]))
        # faces
        for bb_face, mc_face in {"north": "north", "south": "south", "east": "west", "west": "east", "up": "down", "down": "up"}.items():
            sf = el["faces"].get(bb_face)
            textured = bool(sf) and sf.get("uv") is not None and sf.get("texture") is not None
            if textured != (mc_face in c["faces"]):
                err("%s face %s presence" % (c["name"], bb_face))
                continue
            if not textured:
                continue
            u0, v0, u1, v1 = sf["uv"]
            uvs = {(round(v[3], 4), round(v[4], 4)) for v in c["faces"][mc_face]["vertices"]}
            want = {(u0, v0), (u1, v0), (u1, v1), (u0, v1)}
            if uvs != {(round(a, 4), round(b2, 4)) for a, b2 in want}:
                err("%s face %s UV corners %s != %s" % (c["name"], bb_face, sorted(uvs), sorted(want)))
            for v in c["faces"][mc_face]["vertices"]:
                if not (0 <= v[3] <= w and 0 <= v[4] <= h):
                    err("%s face %s UV outside texture" % (c["name"], bb_face))
            vs = c["faces"][mc_face]["vertices"]
            n = c["faces"][mc_face]["normal"]
            e1 = [vs[1][i] - vs[0][i] for i in range(3)]
            e2 = [vs[2][i] - vs[1][i] for i in range(3)]
            cr = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]]
            if sum(a * b2 for a, b2 in zip(cr, n)) <= 0:
                err("%s face %s winding is not outward" % (c["name"], bb_face))
            # all four vertices lie on the face plane at the correct side of the cube
            axis = [i for i in range(3) if n[i] != 0][0]
            side = max(p[axis] for p in [v[:3] for face in c["faces"].values() for v in face["vertices"]]) if n[axis] > 0 else \
                min(p[axis] for p in [v[:3] for face in c["faces"].values() for v in face["vertices"]])
            if not all(close(v[axis], side) for v in vs):
                err("%s face %s is not on the cube side" % (c["name"], bb_face))
if total != 77 or model["cube_count"] != 77 or len(bb["elements"]) != 77:
    err("cube count %d / %d / %d != 77" % (total, model["cube_count"], len(bb["elements"])))

if errors:
    print("FAILED (%d problems):" % len(errors))
    for e in errors[:40]:
        print("  - " + e)
    sys.exit(1)
print("OK: %d bones, %d cubes, texture %dx%d, all geometry/UV/hierarchy checks passed" % (len(out_bones), total, w, h))
