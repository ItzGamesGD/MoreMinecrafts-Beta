#!/usr/bin/env python3
"""ST-01 P8-A: Reproducible Minecraft 26.3 Royal Hall structure template generator.

Generates the approved EAST royal hall structure NBT (Fabric 26.3 schema, DataVersion 5023)
based on the P6/P7 EAST model and approved user decisions:
- D-I: EAST orientation
- P3-F6: PRIMARY B double chest at (-21,-20,33) [left] and (-20,-20,33) [right] facing north (-Z)
- D-E: Guardian 3B emergence point at (-21,-22,26) with unobstructed combat volume
- SECONDARY chest excluded (alcove remains clear air)
- Closed-state secret hatch floor pavement intact

ST-01 P8-A-D1 (user-authorized amendments, applied as a labelled overlay after the P2/P3 model; nothing else changes):
- Floor detailing: a restrained two-tone pattern (see ``apply_d1``).
- Dais stair correction: the second tread row is raised one block so the ascent is continuous (see ``apply_d1``).
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


# ---- P8-A-D1 amendment constants (world coordinates) -------------------------------------------
FLOOR_Y = -23                      # floor slab layer; its top face (y=-22) is the walking surface
STAND_Y = -22                      # first air layer above the floor
HALL_CENTER = (-21, 21)            # (x, z) centre of the 17x17 field and the carved base
FIELD_RADIUS = 8                   # 17x17 field: |dx|<=8, |dz|<=8
BASE_FRAME_RADIUS = 4              # ring that frames the 5x5 carved base (base itself is |d|<=2)
RUNNER_HALF_WIDTH = 1              # 3-wide processional runner on x=-21
STAIR_X = range(-25, -16)          # 9-wide frontal stair, x -25..-17
STAIR_ROW_FIRST_Z, STAIR_ROW_SECOND_Z = 30, 31
DAIS_X = (-26, -16)                # dais body x range
DAIS_FRONT_Z, DAIS_REAR_Z = 32, 37
# Base tone is the existing floor (smooth/cut sandstone): every sandstone variant shares the sandstone_top face, so only a
# different family gives a visible pattern.
ACCENT = "smooth_red_sandstone"    # red_sandstone_top texture: the only sandstone-family variant with a distinct top face


def d1_accent_floor(x, z):
    """True where the exposed floor takes the accent tone. Symmetric about x=-21."""
    dx, dz = x - HALL_CENTER[0], z - HALL_CENTER[1]
    ring = max(abs(dx), abs(dz))
    in_field = abs(dx) <= FIELD_RADIUS and abs(dz) <= FIELD_RADIUS
    if abs(dx) <= RUNNER_HALF_WIDTH and (2 <= z <= 16 or 26 <= z <= 29):
        return True                                   # entrance and approach runner (stops at the stair front, z=30)
    if in_field and ring == FIELD_RADIUS:
        return True                                   # perimeter tile band of the central field
    if in_field and ring == BASE_FRAME_RADIUS:
        return True                                   # ceremonial frame around the carved base
    return False


def apply_d1(blocks):
    """ST-01 P8-A-D1: floor detailing + dais stair correction. Returns the number of changed voxels by category."""
    min_x, min_y, min_z = MIN_CORNER

    def local(x, y, z):
        return (x - min_x, y - min_y, z - min_z)

    def get(x, y, z):
        entry = blocks.get(local(x, y, z))
        return "air" if entry is None else entry[0]

    def set_block(x, y, z, name, props=None):
        blocks[local(x, y, z)] = (name, tuple(sorted((props or {}).items())), None)

    changed = Counter()

    # 1. Floor: replace exposed floor-slab blocks only (cells whose block above is air); nothing is added above the floor.
    for x in range(min_x, min_x + SIZE[0]):
        for z in range(min_z, min_z + SIZE[2]):
            if get(x, FLOOR_Y, z) not in ("smooth_sandstone", "cut_sandstone"):
                continue
            if get(x, STAND_Y, z) != "air":
                continue                              # hidden under a pillar, base, dais or stair: unchanged
            if d1_accent_floor(x, z):
                set_block(x, FLOOR_Y, z, ACCENT)         # every other exposed floor block is left exactly as it was
                changed[f"floor:{ACCENT}"] += 1

    # 2. Dais top: accent U-frame on the dais surface (front row + both side columns), never touching the chest supports.
    chest_supports = {(-21, -21, 33), (-20, -21, 33)}
    for x in range(DAIS_X[0], DAIS_X[1] + 1):
        for z in range(DAIS_FRONT_Z, DAIS_REAR_Z + 1):
            on_frame = z == DAIS_FRONT_Z or x in DAIS_X
            if not on_frame or (x, -21, z) in chest_supports:
                continue
            if get(x, -21, z) == "smooth_sandstone" and get(x, -20, z) == "air":
                set_block(x, -21, z, ACCENT)
                changed[f"dais_top:{ACCENT}"] += 1

    # 3. Stair correction: raise the second tread row by one block so the ascent is 4 continuous half-steps
    #    (floor -22 -> -21.5 -> -21 -> -20.5 -> -20 = dais top). Width, facing and the first row are unchanged.
    stair_state = {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"}
    for x in STAIR_X:
        assert get(x, -22, STAIR_ROW_SECOND_Z) == "sandstone_stairs" and get(x, -21, STAIR_ROW_SECOND_Z) == "air", (x, "unexpected baseline")
        set_block(x, -22, STAIR_ROW_SECOND_Z, "smooth_sandstone")      # support block
        set_block(x, -21, STAIR_ROW_SECOND_Z, "sandstone_stairs", stair_state)
        changed["stair_row2:support smooth_sandstone (y=-22)"] += 1
        changed["stair_row2:sandstone_stairs (y=-21)"] += 1
    return changed


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

    apply_d1(blocks)

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
    compressed = bytearray(gzip.compress(raw.getvalue(), mtime=0))
    compressed[9] = 3  # gzip OS byte pinned to "Unix": Python >= 3.13 writes 0xff, which made the asset depend on the interpreter
    compressed = bytes(compressed)
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
