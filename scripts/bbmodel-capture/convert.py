#!/usr/bin/env python3
"""Converts Blockbench (Bedrock, per-face UV) .bbmodel files into the bone JSON + PNG used by BbModelParts.

Elements are grouped into bones by name so the vanilla humanoid / skeleton / zombie animation (and the custom Crystal
Bug animation) can move them. Weapon elements are written to <name>_weapon.json and are not part of the body model.
"""
import base64, json, sys
from pathlib import Path

WEAPON_PREFIXES = {"skyguard_archer": ("bow_",), "fallen_sentinel": ("sw_",)}

# bone -> (pivot in Bedrock coordinates [x, y, z])
HUMANOID_PIVOTS = {
    "skyguard_archer": {"head": (0, 23, 0), "body": (0, 23, 0), "right_arm": (6, 20, 0), "left_arm": (-6, 20, 0),
                        "right_leg": (2, 11.5, 0), "left_leg": (-2, 11.5, 0)},
    "fallen_sentinel": {"head": (0, 24, 0), "body": (0, 24, 0), "right_arm": (6.5, 23, 0), "left_arm": (-6.5, 23, 0),
                        "right_leg": (2.225, 12.5, 0), "left_leg": (-2.225, 12.5, 0)},
}

ARCHER_LIMB = {"leg": "leg", "thigh": "leg", "knee": "leg", "shin": "leg", "boot": "leg",
               "pauldron": "arm", "arm": "arm", "elbow": "arm", "bracer": "arm", "hand": "arm", "thumb": "arm"}
ARCHER_HEAD = ("head_", "helm_", "brow_", "crest_", "cheek_", "nasal", "eye_glow_")
SENTINEL_RIGHT_LEFT = ("leg", "arm", "pauldron", "crystal", "elbow", "fist", "hand", "finger", "thumb")
SENTINEL_KIND = {"leg": "leg", "arm": "arm", "pauldron": "arm", "crystal": "arm", "elbow": "arm", "fist": "arm",
                 "hand": "arm", "finger": "arm", "thumb": "arm"}


def archer_bone(n: str) -> str:
    if n.startswith(ARCHER_HEAD):
        return "head"
    side = "right" if n.endswith("_right") else "left" if n.endswith("_left") else None
    kind = ARCHER_LIMB.get(n.split("_")[0])
    if side and kind:
        return f"{side}_{kind}"
    return "body"


def sentinel_bone(n: str) -> str:
    if n.startswith(("helm", "crown_spike")):
        return "head"
    parts = n.split("_")
    if len(parts) >= 2 and parts[0] in SENTINEL_KIND and parts[1] in ("r", "l"):
        return ("right_" if parts[1] == "r" else "left_") + SENTINEL_KIND[parts[0]]
    if n in ("fist_r", "fist_l") or n.startswith(("fist_r", "fist_l")):
        return ("right_" if n.startswith("fist_r") else "left_") + "arm"
    if n.startswith(("hand_l", "finger_l", "thumb_l")):
        return "left_arm"
    return "body"


BUG_LEGS = ("fl", "fr", "ml", "mr", "bl", "br")


def bug_bone(n: str) -> str:
    if n.startswith(("head", "mandible")):
        return "head"
    last = n.split("_")[-1]
    if n.split("_")[0] in ("thigh", "shin", "knee") and last in BUG_LEGS:
        return "leg_" + last
    if n.startswith(("abdomen", "spike_back", "spike_l2", "spike_r2", "tail")):
        return "abdomen"
    return "thorax"


BUG_PIVOTS = {"head": (0, 4, -4), "thorax": (0, 3.5, -1), "abdomen": (0, 3.5, 1),
              "leg_fl": (3, 2.5, -2.5), "leg_ml": (3, 2.5, 0.5), "leg_bl": (3, 2.5, 4.5),
              "leg_fr": (-3, 2.5, -2.5), "leg_mr": (-3, 2.5, 0.5), "leg_br": (-3, 2.5, 4.5)}


def convert(src: Path, name: str, out: Path) -> None:
    d = json.loads(src.read_text())
    png = base64.b64decode(d["textures"][0]["source"].split(",", 1)[1])
    (out / "textures" / "entity").mkdir(parents=True, exist_ok=True)
    (out / "textures" / "entity" / f"bb_{name}.png").write_bytes(png)
    classify = {"skyguard_archer": archer_bone, "fallen_sentinel": sentinel_bone, "crystal_bug": bug_bone}[name]
    pivots = BUG_PIVOTS if name == "crystal_bug" else HUMANOID_PIVOTS[name]
    prefixes = WEAPON_PREFIXES.get(name, ())
    bones: dict[str, list] = {}
    weapon = []
    for e in d["elements"]:
        el = {"n": e["name"], "f": e["from"], "t": e["to"], "o": e.get("origin", [0, 0, 0]),
              "r": e.get("rotation", [0, 0, 0]), "i": e.get("inflate", 0),
              "uv": {k: v["uv"] for k, v in e["faces"].items()}}
        if prefixes and e["name"].startswith(prefixes):
            weapon.append(el)
        else:
            bones.setdefault(classify(e["name"]), []).append(el)
    res = {"w": d["resolution"]["width"], "h": d["resolution"]["height"],
           "bones": [{"name": b, "pivot": list(pivots[b]), "elements": els} for b, els in bones.items()]}
    missing = set(pivots) - set(bones)
    assert not missing, f"{name}: bones without elements {missing}"
    (out / "bbmodel").mkdir(parents=True, exist_ok=True)
    (out / "bbmodel" / f"{name}.json").write_text(json.dumps(res, separators=(",", ":")))
    if weapon:
        (out / "bbmodel" / f"{name}_weapon.json").write_text(json.dumps(
            {"w": res["w"], "h": res["h"], "elements": weapon}, separators=(",", ":")))
    print(name, {b: len(e) for b, e in bones.items()}, "weapon", len(weapon))


if __name__ == "__main__":
    out = Path(sys.argv[1])
    for arg in sys.argv[2:]:
        n, p = arg.split("=", 1)
        convert(Path(p), n, out)
