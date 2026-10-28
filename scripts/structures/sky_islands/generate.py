#!/usr/bin/env python3
"""Reproducible Sky Islands content generator: structure NBT templates, jigsaw pools and structure definitions.

Run from anywhere: `python3 scripts/structures/sky_islands/generate.py [--check]`. --check fails when committed files differ.
Hand-authored data (loot tables, trial spawner configs, structure sets, biome tags) lives beside the output and is not generated.
"""
import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import buildings  # noqa: E402
import islands  # noqa: E402
import random  # noqa: E402

ROOT = HERE.parents[2]
DATA = ROOT / "src/main/resources/data/moreminecrafts"
NS = "moreminecrafts"

STYLES = ["snowy", "meadow", "forest", "badlands", "desert", "rocky"]

# (structure id, island style, vertical band above the local surface, biomes)
STRUCTURES = {
    "sky_island_mountain_snowy": ("snowy", (18, 70), ["snowy_slopes", "frozen_peaks", "jagged_peaks", "grove"]),
    "sky_island_mountain_rocky": ("rocky", (18, 70), ["stony_peaks", "windswept_hills", "windswept_gravelly_hills", "windswept_forest"]),
    "sky_island_mountain_meadow": ("meadow", (18, 70), ["meadow", "cherry_grove"]),
    "sky_island_snowy": ("snowy", (40, 110), ["snowy_plains", "ice_spikes", "snowy_taiga"]),
    "sky_island_meadow": ("meadow", (40, 110), ["plains", "sunflower_plains", "savanna", "savanna_plateau", "windswept_savanna"]),
    "sky_island_forest": ("forest", (40, 110), ["forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "dark_forest",
                                                  "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga"]),
    "sky_island_desert": ("desert", (40, 110), ["desert"]),
    "sky_island_badlands": ("badlands", (40, 110), ["badlands", "eroded_badlands", "wooded_badlands"]),
}

START_WEIGHTS = {"plain_small_a": 3, "plain_small_b": 3, "plain_medium": 4, "ruins": 6, "tower": 3, "temple": 2}
SATELLITE_WEIGHTS = {"satellite_a": 3, "satellite_b": 3}
SATELLITE_EMPTY_WEIGHT = 4


def pool_element(location, weight):
    return {"weight": weight, "element": {"element_type": "minecraft:single_pool_element", "location": f"{NS}:{location}",
                                          "processors": "minecraft:empty", "projection": "rigid"}}


def pool(elements, empty_weight=0):
    entries = [pool_element(loc, w) for loc, w in elements]
    if empty_weight:
        entries.append({"weight": empty_weight, "element": {"element_type": "minecraft:empty_pool_element"}})
    return {"fallback": "minecraft:empty", "elements": entries}


def build_style_islands(style):
    sat_pool = f"{NS}:sky_islands/{style}/satellite"
    out = {}

    def make(name, **kw):
        v, meta = islands.build_island(style, name, **kw)
        out[name] = v
        return meta

    make("plain_small_a", rx=10, rz=10, depth=8, seed=1)
    make("plain_small_b", rx=11, rz=9, depth=9, seed=2)
    make("plain_medium", rx=17, rz=15, depth=12, seed=3, links=[("east", sat_pool), ("west", sat_pool)],
         link_dy={"east": 5, "west": -4}, veg_scale=1.0)
    # reserved footprints are expressed after the centre is known, so build twice: probe for the centre, then rebuild
    for name, rx, rz, depth, half, socket, links, shift, clearance in [
        ("ruins", 14, 14, 10, (7, 7), f"{NS}:sky_islands/ruins_top", [("east", sat_pool)], {"east": 3}, 16),
        ("tower", 19, 19, 15, (10, 10), f"{NS}:sky_islands/tower_top", [], {}, 58),
        ("temple", 38, 26, 18, (23, 14), f"{NS}:sky_islands/temple_top", [], {}, 26),
    ]:
        probe = islands.build_island(style, name, rx=rx, rz=rz, depth=depth, seed=4, top_socket=socket, links=links, clearance=clearance)[1]
        cx, cz, _ = probe
        reserved = [(cx - half[0], cz - half[1], cx + half[0], cz + half[1])]
        make(name, rx=rx, rz=rz, depth=depth, seed=4, reserved=reserved, top_socket=socket, links=links, link_dy=shift, clearance=clearance)
    make("satellite_a", rx=8, rz=7, depth=7, seed=5, links=[("west", sat_pool), ("east", sat_pool)], link_dy={"west": 0, "east": 0})
    make("satellite_b", rx=7, rz=8, depth=6, seed=6, links=[("west", sat_pool), ("east", sat_pool)], link_dy={"west": 4, "east": -3})
    return out


def structure_json(style, band, tag):
    return {
        "type": "minecraft:jigsaw",
        "biomes": f"#{NS}:has_structure/{tag}",
        "dimension_padding": 4,
        "max_distance_from_center": 80,
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "size": 4,
        "spawn_overrides": {},
        "start_height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": band[0]}, "max_inclusive": {"absolute": band[1]}},
        "start_pool": f"{NS}:sky_islands/{style}/start",
        "step": "surface_structures",
        "terrain_adaptation": "none",
        "use_expansion_hack": False,
    }


def biome_tag(biomes):
    return {"replace": False, "values": [f"minecraft:{b}" for b in biomes]}


def collect():
    files = {}  # path -> bytes
    stats = {}

    def text(path, obj):
        files[DATA / path] = (json.dumps(obj, indent=2, ensure_ascii=False) + "\n").encode()

    for style in STYLES:
        for name, v in build_style_islands(style).items():
            files[DATA / f"structure/sky_islands/{style}/{name}.nbt"] = v.to_nbt()
        text(f"worldgen/template_pool/sky_islands/{style}/start.json",
             pool([(f"sky_islands/{style}/{n}", w) for n, w in START_WEIGHTS.items()]))
        text(f"worldgen/template_pool/sky_islands/{style}/satellite.json",
             pool([(f"sky_islands/{style}/{n}", w) for n, w in SATELLITE_WEIGHTS.items()], SATELLITE_EMPTY_WEIGHT))

    for name, builder in buildings.RUINS.items():
        v = builder(random.Random(f"{name}/ruin"))
        files[DATA / f"structure/sky_islands/{name}.nbt"] = v.to_nbt()
        stats[name] = v
    for floors in (5, 6, 7):
        v = buildings.tower(floors, random.Random(f"tower_{floors}"))
        files[DATA / f"structure/sky_islands/tower_{floors}.nbt"] = v.to_nbt()
        stats[f"tower_{floors}"] = v
    v = buildings.temple(random.Random("temple"))
    files[DATA / "structure/sky_islands/temple.nbt"] = v.to_nbt()
    stats["temple"] = v

    text("worldgen/template_pool/sky_islands/ruins_top.json", pool([(f"sky_islands/{n}", 1) for n in buildings.RUINS]))
    text("worldgen/template_pool/sky_islands/tower_top.json", pool([("sky_islands/tower_5", 3), ("sky_islands/tower_6", 2), ("sky_islands/tower_7", 1)]))
    text("worldgen/template_pool/sky_islands/temple_top.json", pool([("sky_islands/temple", 1)]))

    for sid, (style, band, biomes) in STRUCTURES.items():
        text(f"worldgen/structure/{sid}.json", structure_json(style, band, sid))
        text(f"tags/worldgen/biome/has_structure/{sid}.json", biome_tag(biomes))
    manifest = {}
    for name, v in stats.items():
        manifest[name] = {"size": list(v.size or v.extent()), "trial_spawner": v.count("trial_spawner"), "vault": v.count("vault"),
                          "chest": v.count("chest"), "jigsaw": v.count("jigsaw")}
    return files, manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files, manifest = collect()
    if args.check:
        stale = [str(p.relative_to(ROOT)) for p, data in files.items() if not p.is_file() or p.read_bytes() != data]
        assert not stale, "Committed sky island assets differ from their generator: " + ", ".join(stale[:10])
    else:
        for path, data in files.items():
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    print(json.dumps({"files": len(files), "checked": args.check, "buildings": manifest}, sort_keys=True))


if __name__ == "__main__":
    main()
