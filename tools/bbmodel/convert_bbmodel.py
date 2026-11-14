#!/usr/bin/env python3
"""Blockbench (.bbmodel, "free" format, per-face UV) -> MoreMinecrafts entity model JSON converter.

Usage:
    python3 tools/bbmodel/convert_bbmodel.py SOURCE.bbmodel --model-out MODEL.json --texture-out TEXTURE.png [--check]

The output (format "moreminecrafts:bone_model/1") is consumed at runtime by
``client/model/BoneModelLoader``. All coordinate/UV conversion happens HERE so the loader stays trivial.

Coordinate conversion (Blockbench -> Minecraft entity model space, matching vanilla ModelPart conventions):
    x' = -x          (vanilla models are mirrored on X relative to Blockbench's world axes)
    y' = 24 - y      (vanilla entity model space is Y-down with the ground at y = 24)
    z' =  z
    rotation (rx, ry, rz) degrees -> (-rx, -ry, rz); both Blockbench ("ZYX") and vanilla (rotationZYX) use the same Euler order.
Pivots become parent-relative offsets. Every cube that carries its own rotation becomes a child part at the cube origin;
unrotated cubes are emitted straight into their bone.

Per-face UV: Blockbench stores [u0, v0, u1, v1] where (u0,v0) is the face's top-left as seen from OUTSIDE the cube
(up/down use the box-UV convention). The converter emits explicit vertices (position + pixel UV) and outward normals,
so arbitrary per-face UVs and mirrored faces are preserved exactly.
"""
import argparse
import base64
import hashlib
import json
import math
import sys

FORMAT = "moreminecrafts:bone_model/1"
TRANSFORM_DOC = "x'=-x; y'=24-y; z'=z; rot'=(-rx,-ry,rz); bone/cube offsets are parent-relative"


def tx(p):
    """Blockbench absolute point -> Minecraft model-space point."""
    return (-p[0], 24.0 - p[1], p[2])


def sub(a, b):
    return tuple(round(a[i] - b[i], 5) for i in range(3))


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


# For each Blockbench face: outward normal and the Blockbench-space corner for the four UV corners
# (u0v0, u1v0, u1v1, u0v1), expressed as (xsel, ysel, zsel) with "f" = from, "t" = to.
FACES = {
    "north": ((0, 0, -1), [("t", "t", "f"), ("f", "t", "f"), ("f", "f", "f"), ("t", "f", "f")]),
    "south": ((0, 0, 1), [("f", "t", "t"), ("t", "t", "t"), ("t", "f", "t"), ("f", "f", "t")]),
    "east": ((1, 0, 0), [("t", "t", "t"), ("t", "t", "f"), ("t", "f", "f"), ("t", "f", "t")]),
    "west": ((-1, 0, 0), [("f", "t", "f"), ("f", "t", "t"), ("f", "f", "t"), ("f", "f", "f")]),
    "up": ((0, 1, 0), [("f", "t", "f"), ("t", "t", "f"), ("t", "t", "t"), ("f", "t", "t")]),
    "down": ((0, -1, 0), [("f", "f", "t"), ("t", "f", "t"), ("t", "f", "f"), ("f", "f", "f")]),
}
# Blockbench face -> the Minecraft model-space face it lands on after the x'/y' flips.
MC_FACE = {"north": "north", "south": "south", "east": "west", "west": "east", "up": "down", "down": "up"}


def build_cube(el, origin_bb):
    f, t = el["from"], el["to"]
    sel = {"f": f, "t": t}
    out_faces = {}
    for bb_face, (normal_bb, corners) in FACES.items():
        face = el["faces"].get(bb_face)
        if not face or face.get("uv") is None or face.get("texture") is None:
            continue  # face not textured -> not rendered (Blockbench semantics)
        u0, v0, u1, v1 = face["uv"]
        uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
        verts = []
        for (cx, cy, cz), (u, v) in zip(corners, uvs):
            p_bb = (sel[cx][0], sel[cy][1], sel[cz][2])
            p = tx(p_bb)
            o = tx(origin_bb)
            verts.append([round(p[0] - o[0], 5), round(p[1] - o[1], 5), round(p[2] - o[2], 5), u, v])
        normal = (-normal_bb[0], -normal_bb[1], normal_bb[2])
        # Enforce vanilla winding: cross(e1, e2) must point along the outward normal.
        e1 = [verts[1][i] - verts[0][i] for i in range(3)]
        e2 = [verts[2][i] - verts[1][i] for i in range(3)]
        if sum(a * b for a, b in zip(cross(e1, e2), normal)) < 0:
            verts.reverse()
        out_faces[MC_FACE[bb_face]] = {"normal": list(normal), "vertices": verts}
    o = tx(origin_bb)
    mn = [min(-f[0], -t[0]) - o[0], min(24 - f[1], 24 - t[1]) - o[1], min(f[2], t[2]) - o[2]]
    sz = [abs(t[0] - f[0]), abs(t[1] - f[1]), abs(t[2] - f[2])]
    rot = el.get("rotation") or [0, 0, 0]
    return {
        "name": el["name"],
        "rotation": [-rot[0], -rot[1], rot[2]],
        "origin": [round(c, 5) for c in tx(origin_bb)],
        "box": {"min": [round(c, 5) for c in mn], "size": [round(c, 5) for c in sz]},
        "faces": out_faces,
    }


def convert(bb, source_sha256, source_name):
    elements = {e["uuid"]: e for e in bb["elements"]}
    tex = bb["textures"][0]
    res = bb["resolution"]
    bones = []
    used = set()

    def walk(node, parent_name, parent_pivot_bb):
        name = node["name"]
        pivot_bb = node.get("origin", [0, 0, 0])
        rot = node.get("rotation", [0, 0, 0])
        cubes = []
        for child in node["children"]:
            if isinstance(child, str):
                el = elements[child]
                used.add(child)
                origin = el["origin"] if (el.get("rotation") and any(el["rotation"])) else pivot_bb
                cube = build_cube(el, origin)
                cube["rotated"] = bool(el.get("rotation") and any(el["rotation"]))
                cubes.append(cube)
        bones.append({
            "name": name,
            "parent": parent_name,
            "pivot": [round(c, 5) for c in tx(pivot_bb)],
            "offset": [round(c, 5) for c in (sub(tx(pivot_bb), tx(parent_pivot_bb)) if parent_name else tx(pivot_bb))],
            "rotation": [-rot[0], -rot[1], rot[2]],
            "cubes": cubes,
        })
        for child in node["children"]:
            if not isinstance(child, str):
                walk(child, name, pivot_bb)

    for top in bb["outliner"]:
        if isinstance(top, dict):
            walk(top, None, [0, 0, 0])
    missing = set(elements) - used
    if missing:
        raise SystemExit("elements not reachable from outliner: %s" % sorted(elements[m]["name"] for m in missing))
    names = [b["name"] for b in bones]
    if len(names) != len(set(names)):
        raise SystemExit("duplicate bone names")
    return {
        "format": FORMAT,
        "name": bb.get("name", ""),
        "source": {"file": source_name, "sha256": source_sha256, "meta": bb.get("meta", {})},
        "transform": TRANSFORM_DOC,
        "texture": {"width": tex["width"], "height": tex["height"], "uv_width": tex.get("uv_width", res["width"]),
                    "uv_height": tex.get("uv_height", res["height"])},
        "cube_count": len(elements),
        "bones": bones,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("source")
    ap.add_argument("--model-out", required=True)
    ap.add_argument("--texture-out", required=True)
    ap.add_argument("--check", action="store_true", help="do not write; fail if outputs differ from what would be written")
    a = ap.parse_args()
    raw = open(a.source, "rb").read()
    bb = json.loads(raw)
    model = convert(bb, hashlib.sha256(raw).hexdigest(), a.source.replace("\\", "/").split("/")[-1])
    png = base64.b64decode(bb["textures"][0]["source"].split(",", 1)[1])
    text = json.dumps(model, indent=1, ensure_ascii=False) + "\n"
    if a.check:
        ok = open(a.model_out, encoding="utf-8").read() == text and open(a.texture_out, "rb").read() == png
        print("up to date" if ok else "OUT OF DATE")
        sys.exit(0 if ok else 1)
    with open(a.model_out, "w", encoding="utf-8") as fh:
        fh.write(text)
    with open(a.texture_out, "wb") as fh:
        fh.write(png)
    print("bones=%d cubes=%d" % (len(model["bones"]), model["cube_count"]))


if __name__ == "__main__":
    main()
