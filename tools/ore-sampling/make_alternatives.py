#!/usr/bin/env python3
"""Generate the bounded Adamantium F-3 candidate set written to tools/ore-sampling/alternatives/.

Decided by F-3 and therefore identical in every candidate: Nether only (registration unchanged), vein size 2 (the F-3 maximum),
air-exposure discard 1.0 (same as Ancient Debris), height as a trapezoid with no plateau (so it peaks mid-range).
scattered_ore is used because plain minecraft:ore at size 2 generates nothing (see docs/audits/wave13/ADAMANTIUM_F3_ANALYSIS.md).

Left open for the owner (the only things that differ between candidates):
  * placements per chunk: 1 (the value shipped today) or uniform 1..2
  * origin range of the height provider: 8..22 as literally written, 8..21, or 9..21 (the only one that keeps every
    generated block inside Y 8-22 because scattered_ore spills one block vertically)
"""
import json, os, sys
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "alternatives")
os.makedirs(out, exist_ok=True)
TARGETS = [{"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_nether"},
            "state": "moreminecrafts:adamantium_debris"}]
UNIFORM_1_2 = {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}

def variant(count, lo, hi):
    return {"feature": {"type": "minecraft:scattered_ore", "discard_chance_on_air_exposure": 1.0, "size": 2, "targets": TARGETS},
            "placed_feature": {"feature": "moreminecrafts:ore_adamantium", "placement": [
                {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                    "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
                {"type": "minecraft:biome"}]}}

CANDIDATES = {
    "A_count1_origin8-22": variant(1, 8, 22),
    "B_count1to2_origin8-22": variant(UNIFORM_1_2, 8, 22),
    "C_count1_origin8-21": variant(1, 8, 21),
    "D_count1to2_origin8-21": variant(UNIFORM_1_2, 8, 21),
    "E_count1_origin9-21": variant(1, 9, 21),
    "F_count1to2_origin9-21": variant(UNIFORM_1_2, 9, 21),
}
for name, body in CANDIDATES.items():
    with open(os.path.join(out, name + ".json"), "w") as f:
        json.dump(body, f, indent=2); f.write("\n")
