#!/usr/bin/env python3
"""Reproducible Minecraft 26.3 arena template, using the shipped id/properties NBT schema."""
import argparse
from collections import Counter
import gzip
import io
import json
import math
from pathlib import Path

import nbtlib
from nbtlib import Compound, Int, List, String

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/data/moreminecrafts/structure/ancient_city/arena.nbt"
SIZE = (37, 13, 37)
CENTER = 18
DATA_VERSION = 5023


def build():
    blocks = {}

    def put(x, y, z, name, props=None, entity=None):
        assert 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]
        blocks[x, y, z] = (name, tuple(sorted((props or {}).items())), entity)

    def stairs(name, facing):
        return dict(facing=facing, half="bottom", shape="straight", waterlogged="false")

    def facing(dx, dz):
        if abs(dx) > abs(dz):
            return "east" if dx > 0 else "west"
        return "south" if dz > 0 else "north"

    # Air is authored inside the bowl, so the structure's own room clears terrain.
    # Unauthored corner cells preserve the surrounding cave instead of cutting a square.
    for x in range(37):
        for z in range(37):
            dx, dz = x - CENTER, z - CENTER
            r = math.hypot(dx, dz)
            if r > 18.25:
                continue
            put(x, 0, z, "polished_deepslate")
            for y in range(1, 13):
                put(x, y, z, "air")
            west_gate = x < CENTER and abs(dz) <= 2
            south_gate = z > CENTER and abs(dx) <= 2
            if west_gate or south_gate:
                put(x, 0, z, "gray_wool")
                continue
            if 9.75 <= r < 11:
                put(x, 0, z, "deepslate_tiles")
            if r >= 11:
                tier = min(4, 1 + int((r - 11) / 2))
                ruined = dx > 5 and dz > 4 and r >= 13
                if ruined:
                    tier = max(0, tier - 2)
                for y in range(0, tier):
                    put(x, y, z, "cobbled_deepslate" if y == 0 else "deepslate_bricks")
                if tier > 0:
                    if (r - 11) % 2 < 0.9:
                        put(x, tier, z, "deepslate_brick_stairs", stairs("deepslate_brick_stairs", facing(dx, dz)))
                    else:
                        put(x, tier, z, "deepslate_brick_slab", {"type": "bottom", "waterlogged": "false"})
                if r >= 17.25 and not ruined:
                    for y in range(tier, 6):
                        put(x, y, z, "deepslate_bricks")
                # Thin arcade lintel, supported at regular piers. One contiguous sector is gone.
                angle = math.degrees(math.atan2(dz, dx)) % 360
                if r >= 17 and not (24 < angle < 87):
                    put(x, 8, z, "deepslate_tiles")
                    put(x, 9, z, "deepslate_tile_slab", {"type": "bottom", "waterlogged": "false"})

    # Stout city-style piers, chiseled bands, and restrained soul-light brackets.
    for index in range(12):
        angle = 15 + 30 * index
        x = round(CENTER + 17 * math.cos(math.radians(angle)))
        z = round(CENTER + 17 * math.sin(math.radians(angle)))
        height = 4 if index == 1 else 6 if index == 2 else 10
        for dx in range(2):
            for dz in range(2):
                for y in range(height + 1):
                    put(x + dx, y, z + dz, "chiseled_deepslate" if y in (1, 6) else "deepslate_bricks")
                put(x + dx, height + 1, z + dz, "deepslate_brick_slab", {"type": "bottom", "waterlogged": "false"})
        if index not in (1, 2) and index % 2 == 0:
            lx = x + (1 if x < CENTER else -1)
            lz = z
            put(lx, 8, lz, "polished_deepslate")
            put(lx, 7, lz, "soul_lantern", {"hanging": "true", "waterlogged": "false"})

    # Primary entrance follows the exact vanilla west-facing connect_structure convention.
    for z in range(15, 23):
        for x in (1, 2):
            put(x, 7, z, "deepslate_bricks")
    for x in range(0, 19):
        for z in range(16, 21):
            put(x, 0, z, "gray_wool")
            for y in range(1, 6):
                put(x, y, z, "air")

    # Local, connected sculk growth patches; no new rewards, entities, or progression.
    for x in range(7, 30):
        for z in range(7, 30):
            dx, dz = x - CENTER, z - CENTER
            if math.hypot(dx, dz) > 9.5 or (x < CENTER and abs(dz) <= 2):
                continue
            patch = min(math.hypot(x - 12, z - 23), math.hypot(x - 25, z - 13))
            if patch < 3.2:
                put(x, 0, z, "sculk")
            elif patch < 4.1 and (x + z) % 3 != 0:
                put(x, 1, z, "sculk_vein", dict(down="true", up="false", north="false", south="false", east="false", west="false", waterlogged="false"))
    for x, z in [(12, 23), (25, 13)]:
        put(x, 1, z, "sculk_sensor", {"sculk_sensor_phase": "inactive", "power": "0", "waterlogged": "false"})

    # Low collapsed masonry sits on the existing foundation in the damaged quadrant.
    for x, z, height in [(29, 29, 2), (30, 28, 1), (27, 31, 1), (31, 27, 2), (28, 30, 1)]:
        for y in range(1, height + 1):
            put(x, y, z, "cracked_deepslate_bricks")
    put(0, 0, 18, "jigsaw", {"orientation": "west_up"}, {
        "id": String("minecraft:jigsaw"), "name": String("minecraft:connect_structure"),
        "target": String("minecraft:empty"), "pool": String("minecraft:empty"),
        "joint": String("rollable"), "final_state": String("minecraft:gray_wool"),
        "selection_priority": Int(0), "placement_priority": Int(0),
    })

    palette, lookup, entries = [], {}, []
    for position, (name, properties, entity) in sorted(blocks.items()):
        key = (name, properties)
        if key not in lookup:
            lookup[key] = len(palette)
            state = Compound({"id": String("minecraft:" + name)})
            if properties:
                state["properties"] = Compound({key: String(value) for key, value in properties})
            palette.append(state)
        entry = Compound({"pos": List[Int](position), "state": Int(lookup[key])})
        if entity:
            entry["nbt"] = Compound(entity)
        entries.append(entry)
    structure = nbtlib.File({
        "DataVersion": Int(DATA_VERSION), "size": List[Int](SIZE),
        "palette": List[Compound](palette), "blocks": List[Compound](entries),
        "entities": List[Compound]([]),
    }, root_name="")
    raw = io.BytesIO()
    structure.write(raw)
    return gzip.compress(raw.getvalue(), mtime=0), Counter(name for name, _, _ in blocks.values() if name != "air")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Fail when the committed asset differs from its generator")
    args = parser.parse_args()
    data, materials = build()
    if args.check:
        assert OUTPUT.is_file() and OUTPUT.read_bytes() == data, "Committed arena asset differs from its reproducible source"
    else:
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        OUTPUT.write_bytes(data)
    print(json.dumps({"template": str(OUTPUT.relative_to(ROOT)), "size": SIZE, "populated_blocks": sum(materials.values()), "materials": dict(materials), "bytes": len(data), "checked": args.check}, sort_keys=True))


if __name__ == "__main__":
    main()
