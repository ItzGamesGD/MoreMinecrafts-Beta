#!/usr/bin/env python3
"""Merge screenshot batches into raw/ (later batches replace same-named scenes) and write CAPTURE_METADATA.{tsv,json} from scenes.json.
Usage: finalize_captures.py <batch-dir> [<batch-dir> ...]   (batch dirs in capture order, e.g. the full run then the re-shoot run)"""
import json, os, re, shutil, sys
here = os.path.dirname(os.path.abspath(__file__)); raw = os.path.join(here, "raw"); os.makedirs(raw, exist_ok=True)
def key(f): return re.sub(r"^\d+_", "", f)
for b in sys.argv[1:]:
    for f in sorted(os.listdir(b)):
        if not f.endswith(".png"): continue
        for old in os.listdir(raw):
            if old.endswith(".png") and key(old) == key(f): os.remove(os.path.join(raw, old))
        shutil.copy(os.path.join(b, f), os.path.join(raw, f))
sc = json.load(open(os.path.join(here, "scenes.json")))
files = {key(f): f for f in os.listdir(raw) if f.endswith(".png")}
rows = ["file\tseed\tarena_id\tvariant\tcentre_x_y_z\tcamera_eye_x_y_z\tyaw\tpitch\ttarget_x_y_z\tnight_vision\tcutaway_photo_copy\tdescription"]; meta = []; missing = []
for w in sc["worlds"]:
    for s in w["scenes"]:
        k = s["name"] + ".png"
        if k not in files: missing.append(k); continue
        rows.append("\t".join(map(str, [files[k], w["seed"], w["id"], w["variant"], "%d,%d,%d" % tuple(w["center"]), "%.1f,%.1f,%.1f" % tuple(s["eye"]), s["yaw"], s["pitch"], "%.1f,%.1f,%.1f" % tuple(s["target"]), s.get("nightvision", True), s["cutaway"] or "none", s["what"]])))
        meta.append({"file": files[k], "seed": w["seed"], "arena_id": w["id"], "variant": w["variant"], "centre": w["center"], "eye": s["eye"], "yaw": s["yaw"], "pitch": s["pitch"], "target": s["target"],
                     "night_vision": s.get("nightvision", True), "cutaway_photo_copy": s["cutaway"] or None, "description": s["what"],
                     "client": "Minecraft 26.3, Fabric client gametest, Vulkan llvmpipe software renderer, 1920x1080, HUD hidden, spectator camera, fov 85"})
open(os.path.join(raw, "CAPTURE_METADATA.tsv"), "w").write("\n".join(rows) + "\n"); json.dump(meta, open(os.path.join(raw, "CAPTURE_METADATA.json"), "w"), indent=1)
print(len(meta), "captures; scenes without an image:", missing)
