#!/usr/bin/env python3
"""Write a throwaway datapack that overrides the Adamantium feature/placed_feature for one sampling variant.

usage: build_variant_pack.py <variant.json> <out-dir>
A variant file is {"feature": {...}, "placed_feature": {...}}; omit a key to keep the shipped file.
"""
import json, os, sys

variant = json.load(open(sys.argv[1]))
out = sys.argv[2]
os.makedirs(out, exist_ok=True)
json.dump({"pack": {"description": "Adamantium F-3 sampling variant", "min_format": 121, "max_format": 121}},
          open(os.path.join(out, "pack.mcmeta"), "w"))
for key in ("feature", "placed_feature"):
    if key in variant:
        d = os.path.join(out, "data", "moreminecrafts", "worldgen", key)
        os.makedirs(d, exist_ok=True)
        json.dump(variant[key], open(os.path.join(d, "ore_adamantium.json"), "w"), indent=2)
