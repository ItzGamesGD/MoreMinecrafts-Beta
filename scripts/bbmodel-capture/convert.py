#!/usr/bin/env python3
"""Converts Blockbench (Bedrock, per-face UV) .bbmodel files into the compact JSON + PNG used by BbModelCapture."""
import base64, json, sys
from pathlib import Path

# Weapon elements are split into <name>_weapon.json so they can be modelled and attached separately.
WEAPON_PREFIXES = {"skyguard_archer": ("bow_",), "fallen_sentinel": ("sw_",)}


def convert(src: Path, name: str, out: Path) -> None:
    d = json.loads(src.read_text())
    tex = d["textures"][0]
    png = base64.b64decode(tex["source"].split(",", 1)[1])
    (out / "textures" / "entity").mkdir(parents=True, exist_ok=True)
    (out / "textures" / "entity" / f"bb_{name}.png").write_bytes(png)
    elements = []
    weapon = []
    prefixes = WEAPON_PREFIXES.get(name, ())
    for e in d["elements"]:
        el = {"n": e["name"], "f": e["from"], "t": e["to"], "o": e.get("origin", [0, 0, 0]),
              "r": e.get("rotation", [0, 0, 0]), "i": e.get("inflate", 0),
              "uv": {k: v["uv"] for k, v in e["faces"].items()}}
        (weapon if prefixes and e["name"].startswith(prefixes) else elements).append(el)
    (out / "bbcapture").mkdir(parents=True, exist_ok=True)
    (out / "bbcapture" / f"{name}.json").write_text(json.dumps(
        {"w": d["resolution"]["width"], "h": d["resolution"]["height"], "elements": elements}, separators=(",", ":")))
    if weapon:
        (out / "bbcapture" / f"{name}_weapon.json").write_text(json.dumps(
            {"w": d["resolution"]["width"], "h": d["resolution"]["height"], "elements": weapon}, separators=(",", ":")))

if __name__ == "__main__":
    out = Path(sys.argv[1])
    for arg in sys.argv[2:]:
        name, path = arg.split("=", 1)
        convert(Path(path), name, out)
