#!/usr/bin/env python3
"""Pipeline dry-run asset: a water vortex (funnel of spiralled water chunks with a nested twist).

Stands in for an Earto / BlockImagine export. Writes tools/bbmodel/source/water_vortex_test.bbmodel with one 'spin'
loop: the whole vortex turns once per 1.6 s and every layer sways out of phase, so higher layers lag the base.
"""
import base64
import io
import json
import math
import sys
import uuid
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from add_animations import build_animation, kf, merge, sine  # noqa: E402

NS = uuid.UUID("c1d2e3f4-0000-4000-8000-00000000a001")
MODEL = "water_vortex_test"
LAYERS = 8
LAYER_DY = 3
CHUNKS = 6
RADIUS0, RADIUS_STEP = 3.0, 0.9
LENGTH = 1.6
FACES = ("north", "south", "east", "west", "up", "down")


def uid(name):
    return str(uuid.uuid5(NS, name))


def texture_source():
    img = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            t = y / 31
            img.putpixel((x, y), (int(40 + 60 * t), int(120 + 80 * t), 230, 255))
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def build():
    groups, elements = [], []

    def group(name, origin):
        g = {"uuid": uid("group/" + name), "name": name, "origin": origin, "rotation": [0, 0, 0],
             "export": True, "visibility": True, "color": 0}
        groups.append(g)
        return g

    root = group("vortex_root", [0, 0, 0])
    outliner_root = {"uuid": root["uuid"], "name": root["name"], "origin": [0, 0, 0], "rotation": [0, 0, 0], "children": []}
    parent = outliner_root
    for k in range(LAYERS):
        y0 = 2 + LAYER_DY * k
        g = group(f"layer_{k}", [0, y0, 0])
        node = {"uuid": g["uuid"], "name": g["name"], "origin": [0, y0, 0], "rotation": [0, 0, 0], "children": []}
        parent["children"].append(node)
        radius = RADIUS0 + RADIUS_STEP * k
        for j in range(CHUNKS):
            th = k * 0.9 + j * 2 * math.pi / CHUNKS
            cx, cz = radius * math.cos(th), radius * math.sin(th)
            name = f"chunk_{k}_{j}"
            el = {"uuid": uid("el/" + name), "name": name, "type": "cube",
                  "from": [round(cx - 1.5, 3), y0 - 1.5, round(cz - 1.5, 3)],
                  "to": [round(cx + 1.5, 3), y0 + 1.5, round(cz + 1.5, 3)],
                  "origin": [0, y0, 0], "rotation": [0, 0, 0], "box_uv": False, "rescale": False,
                  "locked": False, "light_emission": 0, "render_order": "default",
                  "allow_mirror_modeling": True, "autouv": 0, "color": 0, "inflate": 0,
                  "faces": {f: {"uv": [0, 0, 4, 4], "texture": 0} for f in FACES}}
            elements.append(el)
            node["children"].append(el["uuid"])
        parent = node

    gmap = {g["name"]: g["uuid"] for g in groups}
    bones = {"vortex_root": {"rotation": kf((0, 0, 0, 0), (LENGTH, 0, 360, 0))}}
    for k in range(LAYERS):
        bones[f"layer_{k}"] = {"rotation": merge(y=sine(25, LENGTH, k * 0.5, 0, 16))}
    anim = build_animation(MODEL, "spin", LENGTH, "loop", bones, gmap)

    return {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": MODEL, "model_identifier": MODEL, "visible_box": "",
        "resolution": {"width": 32, "height": 32},
        "elements": elements, "groups": groups, "outliner": [outliner_root],
        "textures": [{"path": "", "name": "water.png", "folder": "", "namespace": "", "id": "0", "group": "",
                      "width": 32, "height": 32, "uv_width": 32, "uv_height": 32,
                      "source": texture_source(), "particle": False, "use_as_default": False, "visible": True}],
        "animations": [anim],
    }


if __name__ == "__main__":
    out = HERE / "source" / f"{MODEL}.bbmodel"
    out.write_text(json.dumps(build(), indent="\t", ensure_ascii=False), encoding="utf-8")
    print("wrote", out)
