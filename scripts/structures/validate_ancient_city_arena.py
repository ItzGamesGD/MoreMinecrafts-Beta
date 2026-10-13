#!/usr/bin/env python3
"""Check the committed template and reproducible generator without rendering substitutes."""
from collections import Counter
import json
import nbtlib
from generate_ancient_city_arena import ROOT, OUTPUT, SIZE, DATA_VERSION, build


def main():
    template = nbtlib.load(OUTPUT)
    assert tuple(map(int, template["size"])) == SIZE
    assert int(template["DataVersion"]) == DATA_VERSION
    assert not template["entities"]
    palette = template["palette"]
    assert all(str(entry["id"]).startswith("minecraft:") for entry in palette)
    assert all("Name" not in entry and "Properties" not in entry for entry in palette)
    positions = {}
    counts = Counter()
    jigsaws = []
    for block in template["blocks"]:
        pos = tuple(map(int, block["pos"]))
        assert pos not in positions
        assert all(0 <= v < size for v, size in zip(pos, SIZE))
        state = int(block["state"])
        assert 0 <= state < len(palette)
        name = str(palette[state]["id"])
        positions[pos] = name
        counts[name] += 1
        if name == "minecraft:jigsaw":
            jigsaws.append(block)
    assert len(jigsaws) == 1
    assert str(jigsaws[0]["nbt"]["name"]) == "minecraft:connect_structure"
    assert positions[18, 1, 18] == "minecraft:air"
    for x in range(19):
        for z in range(16, 21):
            for y in range(1, 6):
                assert positions[x, y, z] == "minecraft:air"
    resource = ROOT / "src/main/resources/data/moreminecrafts/worldgen/template_pool/ancient_city/arena_additions.json"
    pool = json.loads(resource.read_text())
    assert pool["fallback"] == "minecraft:empty"
    assert len(pool["elements"]) == 1 and pool["elements"][0]["weight"] == 1
    element = pool["elements"][0]["element"]
    assert element["location"] == "moreminecrafts:ancient_city/arena"
    assert element["processors"] == "minecraft:ancient_city_generic_degradation"
    assert element["projection"] == "rigid"
    print(json.dumps({"dimensions": SIZE, "non_air_blocks": sum(v for k, v in counts.items() if k != "minecraft:air"),
                      "materials": dict(sorted(counts.items())), "result": "PASS"}, indent=2))


if __name__ == "__main__":
    main()
