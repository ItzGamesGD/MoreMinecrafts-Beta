#!/usr/bin/env python3
"""ST-01 P8-A: Reproducible Minecraft 26.3 Royal Hall structure template generator.

Generates the approved EAST royal hall structure NBT (Fabric 26.3 schema, DataVersion 5023)
based on the P6/P7 EAST model and approved user decisions:
- D-I: EAST orientation
- P3-F6: PRIMARY B double chest at (-21,-20,33) [left] and (-20,-20,33) [right] facing north (-Z)
- D-E: Guardian 3B emergence point at (-21,-22,26) with unobstructed combat volume
- SECONDARY chest excluded (alcove remains clear air)
- Closed-state secret hatch floor pavement intact
"""
import argparse
from collections import Counter
import gzip
import io
import json
import os
from pathlib import Path
import sys

import nbtlib
from nbtlib import Compound, Int, List, String

ROOT = Path(__file__).resolve().parents[3]
OUTPUT = ROOT / "src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt"

# Coordinate Space and Dimensions
# P3/P6 reservation: X in [-40, -2], Y in [-24, -5], Z in [2, 40]
MIN_CORNER = (-40, -24, 2)
SIZE = (39, 20, 39)
DATA_VERSION = 5023

# Import P3/P2 models
sys.path.insert(0, str(ROOT / "tools" / "st01-p3"))
sys.path.insert(0, str(ROOT / "tools" / "st01-p2"))
import p3_spec as S3
import p3_vox as V
import p2_core as C
from st01_p2_model_east import make_east


def build():
    sp = S3.make_hall_east()
    p2 = make_east()
    W = V.build_world(sp, p2)

    blocks = {}

    def put(xl, yl, zl, name, props=None, entity=None):
        assert 0 <= xl < SIZE[0] and 0 <= yl < SIZE[1] and 0 <= zl < SIZE[2], f"Out of bounds: ({xl}, {yl}, {zl})"
        blocks[xl, yl, zl] = (name, tuple(sorted((props or {}).items())), entity)

    chest_nbt = {
        "id": String("minecraft:chest")
    }

    min_x, min_y, min_z = MIN_CORNER
    for xl in range(SIZE[0]):
        for yl in range(SIZE[1]):
            for zl in range(SIZE[2]):
                x = xl + min_x
                y = yl + min_y
                z = zl + min_z

                # 1. PRIMARY B double chest (Candidate B, user approved)
                # Facing North (-Z): left half at lower X (-21), right half at higher X (-20)
                if (x, y, z) == (-21, -20, 33):
                    put(xl, yl, zl, "chest", {
                        "facing": "north",
                        "type": "left",
                        "waterlogged": "false"
                    }, chest_nbt)
                    continue
                elif (x, y, z) == (-20, -20, 33):
                    put(xl, yl, zl, "chest", {
                        "facing": "north",
                        "type": "right",
                        "waterlogged": "false"
                    }, chest_nbt)
                    continue

                # 2. Exclude SECONDARY chest (alcove kept clear and free)
                if (x, y, z) in [(-3, -22, 25), (-3, -22, 26)]:
                    put(xl, yl, zl, "air")
                    continue

                solid = W["solid"][C.ix(x), C.iy(y), C.iz(z)]
                elem_id = W["elem"][C.ix(x), C.iy(y), C.iz(z)]

                # 3. Architectural Elements
                if elem_id >= 0:
                    name = W["eids"][elem_id]
                    kind = sp["elements"][name]["kind"]

                    if kind == "chest_primary":
                        # Replaced by candidate B above
                        put(xl, yl, zl, "air")
                    elif kind == "chest_secondary":
                        # Excluded from implementation
                        put(xl, yl, zl, "air")
                    elif kind == "dais":
                        if name == "DAIS:tread":
                            put(xl, yl, zl, "sandstone_stairs", {
                                "facing": "south",
                                "half": "bottom",
                                "shape": "straight",
                                "waterlogged": "false"
                            })
                        else:
                            put(xl, yl, zl, "smooth_sandstone")
                    elif kind == "sarcophagus":
                        put(xl, yl, zl, "chiseled_sandstone")
                    elif kind == "obelisk":
                        if name == "OBELISK:tip":
                            put(xl, yl, zl, "gold_block")
                        elif name == "OBELISK:plinth":
                            put(xl, yl, zl, "chiseled_sandstone")
                        else:
                            put(xl, yl, zl, "smooth_sandstone")
                    elif kind == "pillar_base":
                        put(xl, yl, zl, "chiseled_sandstone")
                    elif kind == "pillar_shaft":
                        put(xl, yl, zl, "smooth_sandstone")
                    elif kind in ("pillar_cap", "lintel", "alcove_col"):
                        put(xl, yl, zl, "cut_sandstone")
                    elif kind in ("statue", "brazier"):
                        put(xl, yl, zl, "chiseled_sandstone")
                    else:
                        put(xl, yl, zl, "sandstone")
                else:
                    # 4. Shell & Cavity (Doorway, corridors, walls, corbel ceiling)
                    if not solid:
                        put(xl, yl, zl, "air")
                    else:
                        if y == -24:
                            # Foundation slab
                            put(xl, yl, zl, "sandstone")
                        elif y == -23:
                            # Floor slab: ceremonial center field vs outer perimeter
                            if -29 <= x <= -13 and 13 <= z <= 29:
                                put(xl, yl, zl, "cut_sandstone")
                            else:
                                put(xl, yl, zl, "smooth_sandstone")
                        elif y >= -6:
                            # Roof layers
                            put(xl, yl, zl, "sandstone")
                        elif y >= -14:
                            # Stepped corbel ceiling
                            put(xl, yl, zl, "smooth_sandstone")
                        else:
                            # Perimeter vertical walls
                            put(xl, yl, zl, "sandstone")

    # Build NBT palette and block entries
    palette, lookup, entries = [], {}, []
    for position, (name, properties, entity) in sorted(blocks.items()):
        key = (name, properties)
        if key not in lookup:
            lookup[key] = len(palette)
            state = Compound({"id": String("minecraft:" + name)})
            if properties:
                state["properties"] = Compound({k: String(v) for k, v in properties})
            palette.append(state)
        entry = Compound({
            "pos": List[Int]([Int(c) for c in position]),
            "state": Int(lookup[key])
        })
        if entity:
            entry["nbt"] = Compound(entity)
        entries.append(entry)

    structure = nbtlib.File({
        "DataVersion": Int(DATA_VERSION),
        "size": List[Int]([Int(s) for s in SIZE]),
        "palette": List[Compound](palette),
        "blocks": List[Compound](entries),
        "entities": List[Compound]([]),
    }, root_name="")

    raw = io.BytesIO()
    structure.write(raw)
    compressed = gzip.compress(raw.getvalue(), mtime=0)
    materials = Counter(name for name, _, _ in blocks.values() if name != "air")
    return compressed, materials, blocks


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Fail when the committed asset differs from generator")
    parser.add_argument("--output", type=Path, default=OUTPUT, help="Output file path")
    args = parser.parse_args()

    data, materials, _ = build()
    out_path = args.output

    if args.check:
        assert out_path.is_file(), f"Structure file does not exist: {out_path}"
        committed = out_path.read_bytes()
        assert committed == data, f"Committed structure asset {out_path} differs from its reproducible source"
        print(f"PASS: Committed asset {out_path} matches generator (bytes: {len(data)})")
    else:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_bytes(data)
        print(f"Generated: {out_path} ({len(data)} bytes, {sum(materials.values())} populated blocks)")
        print(json.dumps({
            "template": str(out_path.relative_to(ROOT) if out_path.is_relative_to(ROOT) else out_path),
            "size": SIZE,
            "populated_blocks": sum(materials.values()),
            "materials": dict(materials),
            "bytes": len(data),
            "checked": args.check
        }, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
