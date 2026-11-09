#!/usr/bin/env python3
"""Static validation of the Sky Islands data: template NBT integrity, pool/structure/set wiring, loot and spawner references."""
import gzip
import io
import json
import sys
from pathlib import Path

import nbtlib

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "src/main/resources/data/moreminecrafts"
NS = "moreminecrafts"
VANILLA_SPAWNER_CONFIGS = {
    f"minecraft:trial_chamber/{path}/{mode}"
    for path in ["breeze", "melee/zombie", "melee/husk", "melee/spider", "small_melee/slime", "small_melee/cave_spider",
                 "small_melee/silverfish", "small_melee/baby_zombie", "ranged/skeleton", "ranged/stray", "ranged/poison_skeleton"]
    for mode in ["normal", "ominous"]
}
VANILLA_LOOT_REFS = {"minecraft:chests/simple_dungeon", "minecraft:chests/trial_chambers/reward_rare",
                     "minecraft:chests/trial_chambers/reward_common", "minecraft:chests/trial_chambers/reward_unique"}
GRAVITY = {"minecraft:sand", "minecraft:red_sand", "minecraft:gravel"}
failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def load_template(path):
    raw = gzip.decompress(path.read_bytes())
    return nbtlib.File.parse(io.BytesIO(raw))


def template_info(path):
    nbt = load_template(path)
    size = tuple(int(v) for v in nbt["size"])
    palette = nbt["palette"]
    blocks = {}
    for entry in nbt["blocks"]:
        pos = tuple(int(v) for v in entry["pos"])
        state = palette[int(entry["state"])]
        check(all(0 <= pos[i] < size[i] for i in range(3)), f"{path.name}: block {pos} outside {size}")
        check(pos not in blocks, f"{path.name}: duplicate block at {pos}")
        blocks[pos] = (str(state["id"]), entry.get("nbt"))
    return size, blocks


def pool_locations(pool_path):
    pool = json.loads(pool_path.read_text())
    return [e["element"]["location"] for e in pool["elements"] if e["element"].get("location")]


def main():
    template_dir = DATA / "structure/sky_islands"
    expected = {"ruins_a", "ruins_b", "ruins_c", "ruins_d", "tower_5", "tower_6", "tower_7", "temple"}
    infos = {}
    for name in expected:
        path = template_dir / f"{name}.nbt"
        check(path.is_file(), f"missing template {name}")
        if path.is_file():
            infos[name] = template_info(path)

    loot = {p.relative_to(DATA / "loot_table").with_suffix("").as_posix(): p for p in (DATA / "loot_table").rglob("*.json")}
    spawner_cfg = {f"{NS}:" + p.relative_to(DATA / "trial_spawner").with_suffix("").as_posix() for p in (DATA / "trial_spawner").rglob("*.json")}

    counts = {}
    for name, (size, blocks) in infos.items():
        spawners = [(pos, nbt) for pos, (bid, nbt) in blocks.items() if bid == "minecraft:trial_spawner"]
        vaults = [(pos, nbt) for pos, (bid, nbt) in blocks.items() if bid == "minecraft:vault"]
        chests = [(pos, nbt) for pos, (bid, nbt) in blocks.items() if bid == "minecraft:chest"]
        jigsaws = [(pos, nbt) for pos, (bid, nbt) in blocks.items() if bid == "minecraft:jigsaw"]
        counts[name] = (len(spawners), len(vaults), len(chests))
        check(len(jigsaws) == 1, f"{name}: expected exactly one root jigsaw, found {len(jigsaws)}")
        for pos, nbt in spawners:
            for key in ("normal_config", "ominous_config"):
                cfg = str(nbt[key])
                check(cfg in VANILLA_SPAWNER_CONFIGS or cfg in spawner_cfg, f"{name}: unknown spawner config {cfg}")
            x, y, z = pos
            check(blocks.get((x, y - 1, z)) is not None, f"{name}: spawner at {pos} has no support")
            for dy in (1, 2):
                check(blocks.get((x, y + dy, z)) is None, f"{name}: spawner at {pos} is covered")
        for pos, nbt in vaults:
            table = str(nbt["config"]["loot_table"])
            check(table.startswith(f"{NS}:chests/sky_islands/") and table.split(":", 1)[1] in loot, f"{name}: vault table {table}")
            check(str(nbt["config"]["key_item"]["id"]) == "minecraft:trial_key", f"{name}: vault must use the vanilla trial key")
            x, y, z = pos
            check(blocks.get((x, y - 1, z)) is not None, f"{name}: vault at {pos} has no support")
        for pos, nbt in chests:
            table = str(nbt["LootTable"])
            check(table == f"{NS}:chests/sky_islands/chest", f"{name}: chest table {table}")
            x, y, z = pos
            check(blocks.get((x, y - 1, z)) is not None, f"{name}: chest at {pos} has no support")
        # a chest cannot be opened with a block above it
        for pos, _ in chests:
            check(blocks.get((pos[0], pos[1] + 1, pos[2])) is None, f"{name}: chest {pos} is capped")
    for name in ("ruins_a", "ruins_b", "ruins_c", "ruins_d"):
        check(counts[name] == (1, 1, 2), f"{name}: expected 1 spawner, 1 vault and 2 visible chests, got {counts[name]}")
    for floors in (5, 6, 7):
        name = f"tower_{floors}"
        size, blocks = infos[name]
        check(size[1] >= floors * 7, f"{name}: too short for {floors} floors")
        per_floor = {}
        for pos, (bid, nbt) in blocks.items():
            if bid in ("minecraft:trial_spawner", "minecraft:vault", "minecraft:chest"):
                per_floor.setdefault(pos[1] // 7, {}).setdefault(bid, []).append((pos, nbt))
        check(sorted(per_floor) == list(range(floors)), f"{name}: every floor needs spawners/vaults/chests, saw {sorted(per_floor)}")
        for floor, content in per_floor.items():
            top = floor == floors - 1
            spawners = len(content.get("minecraft:trial_spawner", []))
            check(1 <= spawners <= 2, f"{name} floor {floor}: {spawners} trial spawners (approved 1-2)")
            check(len(content.get("minecraft:vault", [])) >= 1 and len(content.get("minecraft:chest", [])) >= 1, f"{name} floor {floor}: needs vault and chest")
            tables = {str(n["config"]["loot_table"]) for _, n in content.get("minecraft:vault", [])}
            check(tables == ({f"{NS}:chests/sky_islands/vault_tower_top"} if top else {f"{NS}:chests/sky_islands/vault"}),
                  f"{name} floor {floor}: vault tables {tables}")
        top_content, ground = per_floor[floors - 1], per_floor[1]
        check(len(top_content["minecraft:vault"]) > len(ground["minecraft:vault"]) and len(top_content["minecraft:chest"]) > len(ground["minecraft:chest"]),
              f"{name}: crown floor must hold more vaults and chests than ordinary floors")
        for floor in range(floors - 1):
            check(blocks.get((9, floor * 7, 9)) is not None, f"{name}: missing floor slab {floor}")
    check(counts["temple"][0] >= 2 and counts["temple"][1] >= 1 and counts["temple"][2] >= 1, "temple needs spawners, vaults and chests")
    check(infos["temple"][0][0] >= 41 and infos["temple"][0][2] >= 21, "temple must be large")

    # islands: every template parses, jigsaw wiring is consistent, gravity blocks never rest on air
    pool_names = {}
    for pool in (DATA / "worldgen/template_pool/sky_islands").rglob("*.json"):
        pool_names[pool.relative_to(DATA / "worldgen/template_pool").with_suffix("").as_posix()] = pool
        for location in pool_locations(pool):
            path = DATA / "structure" / (location.split(":", 1)[1] + ".nbt")
            check(path.is_file(), f"{pool.name}: missing template {location}")
    styles = [p.name for p in (template_dir).iterdir() if p.is_dir()]
    check(sorted(styles) == ["badlands", "desert", "forest", "meadow", "rocky", "snowy"], f"styles: {styles}")
    for style in styles:
        for template in sorted((template_dir / style).glob("*.nbt")):
            size, blocks = template_info(template)
            jigs = {pos: nbt for pos, (bid, nbt) in blocks.items() if bid == "minecraft:jigsaw"}
            for pos, nbt in jigs.items():
                pool = str(nbt["pool"])
                check(pool == "minecraft:empty" or pool.split(":", 1)[1] in pool_names, f"{template.name}: unknown pool {pool}")
            for pos, (bid, _) in blocks.items():
                if bid in GRAVITY:
                    check(blocks.get((pos[0], pos[1] - 1, pos[2])) is not None, f"{style}/{template.name}: floating {bid} at {pos}")
            if template.stem in ("ruins", "tower", "temple"):
                check(any(str(n["pool"]).endswith(f"{template.stem}_top") for n in jigs.values()), f"{template.name}: missing structure socket")
            check(template.stem in ("ruins", "tower", "temple") or not any(str(n["pool"]).endswith("_top") for n in jigs.values()),
                  f"{style}/{template.name}: a plain island must not carry a structure socket")

    # structures / sets / tags
    structures = {}
    for sf in (DATA / "worldgen/structure").glob("sky_island_*.json"):
        structure = json.loads(sf.read_text())
        structures[sf.stem] = structure
        check(structure["type"] == "minecraft:jigsaw" and structure["terrain_adaptation"] == "none", f"{sf.name}: jigsaw structure with no terrain adaptation expected")
        check(structure["start_pool"].split(":", 1)[1] in pool_names, f"{sf.name}: unknown start pool")
        tag = DATA / "tags/worldgen/biome" / (structure["biomes"][1:].split(":", 1)[1] + ".json")
        check(tag.is_file(), f"{sf.name}: biome tag missing {tag}")
    in_sets = set()
    for set_file in (DATA / "worldgen/structure_set").glob("sky_islands_*.json"):
        for entry in json.loads(set_file.read_text())["structures"]:
            sid = entry["structure"].split(":", 1)[1]
            check(sid in structures, f"{set_file.name}: unknown structure {sid}")
            in_sets.add(sid)
    check(in_sets == set(structures), f"structures missing from a set: {set(structures) - in_sets}")

    # loot
    for name in ("chest", "vault", "vault_tower_top", "materials"):
        check(f"chests/sky_islands/{name}" in loot, f"missing loot table {name}")
    for name, path in loot.items():
        if not name.startswith("chests/sky_islands/"):
            continue
        for pool in json.loads(path.read_text())["pools"]:
            for entry in pool["entries"]:
                if entry["type"] == "minecraft:loot_table":
                    ref = entry["value"]
                    check(ref in VANILLA_LOOT_REFS or ref.split(":", 1)[1] in loot, f"{name}: unresolved nested table {ref}")

    if failures:
        print("SKY ISLANDS VALIDATION FAILED")
        for line in failures:
            print(" -", line)
        sys.exit(1)
    print(json.dumps({"validated": True, "templates": len(list(template_dir.rglob('*.nbt'))), "counts": counts}, sort_keys=True))


if __name__ == "__main__":
    main()
