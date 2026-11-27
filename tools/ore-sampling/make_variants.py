#!/usr/bin/env python3
"""Generate the bounded candidate grid for Adamantium F-3 (see docs/audits/wave13/ADAMANTIUM_F3_ANALYSIS.md).

Fixed by F-3 (not tuned): Nether only, trapezoid height 8..22 (plateau 0 -> peak Y15), vein size 2, air-exposure discard 1.0
(same as Ancient Debris). Varied (undecided): feature type and placements per chunk.
"""
import json, os, sys
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "variants")
os.makedirs(out, exist_ok=True)
TARGETS = [{"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_nether"},
            "state": "moreminecrafts:adamantium_debris"}]
def variant(kind, size, count):
    return {
        "feature": {"type": f"minecraft:{kind}", "discard_chance_on_air_exposure": 1.0, "size": size, "targets": TARGETS},
        "placed_feature": {"feature": "moreminecrafts:ore_adamantium", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                "min_inclusive": {"absolute": 8}, "max_inclusive": {"absolute": 22}}},
            {"type": "minecraft:biome"}]},
    }
for kind, tag in (("ore", "ore"), ("scattered_ore", "scat")):
    for count in (1, 2, 3, 4, 5, 6):
        json.dump(variant(kind, 2, count), open(os.path.join(out, f"{tag}2_c{count}.json"), "w"), indent=2)
