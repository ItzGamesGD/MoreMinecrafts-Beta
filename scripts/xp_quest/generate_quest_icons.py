#!/usr/bin/env python3
"""Deterministic per-quest icons for the XP Quest structure preview (D4).

Priority: 1) the quest's actual target item or entity (spawn egg / drop), 2) a related vanilla or
MoreMinecrafts item, 3) a stable category fallback. Icons never depend on completion state.

Validation: every id must exist in the vanilla lang file inside the game jar or in the mod's lang file.
Run with --check to validate the committed file without rewriting it.
"""
import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/design/xp-quest/structure/quest_icons.json"
MAPPING = ROOT / "docs/design/xp-quest/structure/quest_structure_mapping.json"
VANILLA_LANG = ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-7e9a32a5b8/26.3/minecraft-clientOnly-7e9a32a5b8-26.3.jar"
VANILLA_LANG_FALLBACK = pathlib.Path("/root/.gradle/caches/fabric-loom/26.3/minecraft-common.jar")
MOD_LANG = ROOT / "src/main/resources/assets/moreminecrafts/lang/en_us.json"

CATEGORY_FALLBACK = {
    "exploration": ("minecraft:compass", "category"), "dimension": ("minecraft:ender_eye", "category"),
    "combat": ("minecraft:iron_sword", "category"), "boss": ("minecraft:nether_star", "category"),
    "mining": ("minecraft:iron_pickaxe", "category"), "farming": ("minecraft:wheat", "category"),
    "fishing": ("minecraft:fishing_rod", "category"), "crafting": ("minecraft:crafting_table", "category"),
    "building": ("minecraft:bricks", "category"), "alchemy": ("minecraft:brewing_stand", "category"),
    "equipment": ("minecraft:anvil", "category"), "growth": ("minecraft:enchanted_book", "category"),
}

# quest id -> (item id, source). source: "target" = the quest's actual target, "related" = thematic.
# Quests not listed here fall back to their category icon.
ICONS = {
    # Ecology slice (combat tab)
    "xpq.cmb.cave_dwellers": ("minecraft:cave_spider_spawn_egg", "target"),
    "xpq.cmb.bestiary_25": ("minecraft:book", "related"),
    "xpq.cmb.habitats_5": ("minecraft:map", "related"),
    # Exploration
    "xpq.exp.first_structure_loot": ("minecraft:chest", "target"),
    "xpq.exp.mineshaft_loot": ("minecraft:minecart", "target"),
    "xpq.exp.dungeon_loot": ("minecraft:spawner", "target"),
    "xpq.exp.walk_10k": ("minecraft:leather_boots", "related"),
    "xpq.exp.temples": ("minecraft:mossy_cobblestone", "related"),
    "xpq.exp.ocean_relics": ("minecraft:heart_of_the_sea", "target"),
    "xpq.exp.explorer_maps": ("minecraft:map", "target"),
    "xpq.exp.biomes_25": ("minecraft:grass_block", "related"),
    "xpq.exp.rare_biomes": ("minecraft:spyglass", "related"),
    "xpq.exp.biomes_40": ("minecraft:grass_block", "related"),
    "xpq.exp.biomes_all_overworld": ("minecraft:oak_sapling", "related"),
    "xpq.exp.sleeping_bag_biomes": ("minecraft:red_bed", "related"),
    "xpq.exp.villages_5": ("minecraft:emerald", "related"),
    "xpq.exp.structures_15": ("minecraft:compass", "target"),
    "xpq.exp.grand_expedition": ("minecraft:filled_map", "related"),
    "xpq.exp.archaeology_30": ("minecraft:brush", "target"),
    "xpq.exp.sherds_15": ("minecraft:decorated_pot", "target"),
    "xpq.exp.height_extremes": ("minecraft:ladder", "related"),
    "xpq.exp.walk_50k": ("minecraft:leather_boots", "related"),
    "xpq.exp.sail_20k": ("minecraft:oak_boat", "target"),
    "xpq.exp.ride_10k": ("minecraft:saddle", "target"),
    "xpq.exp.elytra_25k": ("minecraft:elytra", "target"),
    "xpq.exp.travel_250k": ("minecraft:ender_pearl", "related"),
    "xpq.exp.parachute_landing": ("moreminecrafts:parachute", "target"),
    # Dimensions
    "xpq.dim.fortress_loot": ("minecraft:nether_brick", "target"),
    "xpq.dim.blaze_rods_32": ("minecraft:blaze_rod", "target"),
    "xpq.dim.bastion_all": ("minecraft:gilded_blackstone", "related"),
    "xpq.dim.nether_travel_50k": ("minecraft:soul_sand", "related"),
    "xpq.dim.end_cities_3": ("minecraft:end_rod", "related"),
    "xpq.dim.shulker_boxes": ("minecraft:shulker_box", "target"),
    "xpq.dim.end_city_loot_10": ("minecraft:chorus_fruit", "related"),
    "xpq.dim.trial_vaults_5": ("minecraft:trial_key", "target"),
    "xpq.dim.ancient_city_loot": ("minecraft:echo_shard", "target"),
    "xpq.dim.ominous_vaults_5": ("minecraft:ominous_trial_key", "target"),
    # Monster hunt
    "xpq.cmb.hostile_100": ("minecraft:rotten_flesh", "related"),
    "xpq.cmb.hostile_500": ("minecraft:bone", "related"),
    "xpq.cmb.hostile_2000": ("minecraft:gunpowder", "related"),
    "xpq.cmb.hostile_5000": ("minecraft:skeleton_skull", "related"),
    "xpq.cmb.bestiary_5": ("minecraft:spider_eye", "related"),
    "xpq.cmb.bestiary_10": ("minecraft:bone_meal", "related"),
    "xpq.cmb.vanilla_species_20": ("minecraft:zombie_spawn_egg", "target"),
    # Arms and tactics
    "xpq.cmb.mm_arms": ("minecraft:iron_sword", "related"),
    "xpq.cmb.dagger_100": ("moreminecrafts:iron_dagger", "target"),
    "xpq.cmb.greatsword_100": ("moreminecrafts:iron_greatsword", "target"),
    "xpq.cmb.mace_smash_10": ("minecraft:mace", "target"),
    "xpq.cmb.netherite_blades": ("moreminecrafts:netherite_greatsword", "target"),
    "xpq.cmb.shield_500": ("minecraft:shield", "target"),
    "xpq.cmb.long_shot_25": ("minecraft:bow", "target"),
    "xpq.cmb.trident_50": ("minecraft:trident", "target"),
    "xpq.cmb.gauntlet_50": ("moreminecrafts:gauntlet", "target"),
    "xpq.cmb.tactical_ammo": ("minecraft:arrow", "target"),
    # Elite foes
    "xpq.cmb.bandits_20": ("minecraft:pillager_spawn_egg", "related"),
    "xpq.cmb.raids_3": ("minecraft:ominous_bottle", "related"),
    "xpq.cmb.ravagers_5": ("minecraft:ravager_spawn_egg", "target"),
    "xpq.cmb.brutes_3": ("minecraft:piglin_brute_spawn_egg", "target"),
    "xpq.cmb.breeze_10": ("minecraft:breeze_rod", "target"),
    "xpq.cmb.guardians_30": ("minecraft:prismarine_shard", "target"),
    "xpq.cmb.lesser_wither_10": ("minecraft:wither_skeleton_spawn_egg", "related"),
    "xpq.cmb.wither_skulls_3": ("minecraft:wither_skeleton_skull", "target"),
    "xpq.cmb.elemental_100": ("minecraft:blaze_powder", "related"),
    "xpq.cmb.elemental_skills": ("minecraft:fire_charge", "related"),
    # Bosses
    "xpq.boss.elder_guardian": ("minecraft:elder_guardian_spawn_egg", "target"),
    "xpq.boss.monument_clear": ("minecraft:prismarine", "related"),
    "xpq.boss.monuments_3": ("minecraft:prismarine_crystals", "related"),
    "xpq.boss.warden": ("minecraft:warden_spawn_egg", "target"),
    "xpq.boss.warden_3": ("minecraft:sculk_catalyst", "related"),
    "xpq.boss.wither": ("minecraft:nether_star", "target"),
    "xpq.boss.dragon_rematch": ("minecraft:dragon_breath", "target"),
    "xpq.boss.twin_bosses": ("minecraft:end_crystal", "related"),
    "xpq.boss.omen_raid": ("minecraft:ominous_bottle", "related"),
    # Mining
    "xpq.min.first_custom_ore": ("minecraft:raw_iron", "related"),
    "xpq.min.custom_ore_survey": ("minecraft:copper_ore", "related"),
    "xpq.min.ore_1000": ("minecraft:iron_ore", "target"),
    "xpq.min.ore_5000": ("minecraft:coal_ore", "target"),
    "xpq.min.ore_20000": ("minecraft:emerald_ore", "target"),
    "xpq.min.diamond_ore_64": ("minecraft:diamond", "target"),
    "xpq.min.deepslate_custom": ("minecraft:deepslate", "related"),
    "xpq.min.adamantium_debris": ("moreminecrafts:adamantium_ingot", "target"),
    "xpq.min.adamantium_10": ("moreminecrafts:adamantium_ingot", "target"),
    "xpq.min.debris_16": ("minecraft:ancient_debris", "target"),
    "xpq.min.obsidian_64": ("minecraft:obsidian", "target"),
    "xpq.min.stone_10000": ("minecraft:stone", "target"),
    "xpq.min.amethyst_spyglass": ("minecraft:spyglass", "related"),
    "xpq.min.pickaxe_5000": ("minecraft:iron_pickaxe", "target"),
    "xpq.min.vein_500": ("minecraft:raw_gold", "related"),
    "xpq.min.drill_1000": ("moreminecrafts:mining_drill", "target"),
    "xpq.min.logs_1000": ("minecraft:oak_log", "target"),
    "xpq.min.all_wood": ("minecraft:oak_planks", "related"),
    "xpq.min.lumberjack_200": ("minecraft:iron_axe", "related"),
    # Farming and husbandry
    "xpq.frm.harvest_all_crops": ("minecraft:wheat", "target"),
    "xpq.frm.composite_foods": ("minecraft:bread", "target"),
    "xpq.frm.till_1000": ("minecraft:iron_hoe", "target"),
    "xpq.frm.harvest_1000": ("minecraft:wheat", "target"),
    "xpq.frm.harvest_5000": ("minecraft:carrot", "target"),
    "xpq.frm.ancient_harvest": ("minecraft:sweet_berries", "related"),
    "xpq.frm.soil_study_harvest": ("minecraft:bone_meal", "related"),
    "xpq.frm.tame_5": ("minecraft:lead", "related"),
    "xpq.frm.breed_200": ("minecraft:golden_carrot", "related"),
    "xpq.frm.honey_32": ("minecraft:honeycomb", "target"),
    "xpq.frm.master_trade": ("minecraft:emerald", "target"),
    "xpq.frm.mending_book": ("minecraft:enchanted_book", "target"),
    "xpq.frm.professions_10": ("minecraft:lectern", "related"),
    "xpq.frm.trades_200": ("minecraft:emerald", "target"),
    "xpq.frm.barter_200": ("minecraft:gold_ingot", "target"),
    "xpq.frm.villager_discount_20": ("minecraft:emerald", "related"),
    "xpq.frm.first_capture": ("moreminecrafts:captured_mob_egg", "target"),
    "xpq.frm.capture_10": ("moreminecrafts:mob_capture_tool", "target"),
    "xpq.frm.auto_farm": ("minecraft:hopper", "related"),
    # Fishing
    "xpq.fsh.first_sale": ("minecraft:cod", "target"),
    "xpq.fsh.sale_64": ("minecraft:salmon", "target"),
    "xpq.fsh.emerald_500": ("minecraft:emerald", "target"),
    "xpq.fsh.freshwater_4": ("moreminecrafts:bluegill", "target"),
    "xpq.fsh.first_module": ("minecraft:fishing_rod", "related"),
    "xpq.fsh.grade_four": ("minecraft:fishing_rod", "related"),
    "xpq.fsh.grade_five": ("minecraft:fishing_rod", "related"),
    "xpq.fsh.tier3_rig": ("moreminecrafts:reel_3", "related"),
    "xpq.fsh.water_biomes_8": ("minecraft:lily_pad", "related"),
    "xpq.fsh.multi_catch": ("minecraft:fishing_rod", "related"),
    "xpq.fsh.ocean_trio": ("minecraft:prismarine_shard", "related"),
    "xpq.fsh.catch_500": ("moreminecrafts:common_carp", "target"),
    "xpq.fsh.full_catalogue": ("moreminecrafts:swordfish", "target"),
    "xpq.fsh.giant_catch": ("moreminecrafts:beluga_sturgeon", "target"),
    "xpq.fsh.grade_five_5": ("moreminecrafts:reel_5", "related"),
    "xpq.fsh.fish_trap": ("minecraft:barrel", "related"),
    # Crafting
    "xpq.cft.sleeping_bag": ("minecraft:red_bed", "related"),
    "xpq.cft.parachute": ("moreminecrafts:parachute", "target"),
    "xpq.cft.anvil_50": ("minecraft:anvil", "target"),
    "xpq.cft.smelt_1000": ("minecraft:furnace", "target"),
    "xpq.cft.enchanting_30": ("minecraft:enchanting_table", "target"),
    "xpq.cft.all_daggers": ("moreminecrafts:diamond_dagger", "target"),
    "xpq.cft.all_greatswords": ("moreminecrafts:diamond_greatsword", "target"),
    "xpq.cft.conduit": ("minecraft:conduit", "target"),
    "xpq.cft.mm_metallurgy": ("moreminecrafts:titanium_ingot", "related"),
    "xpq.cft.netherite_tools": ("minecraft:netherite_pickaxe", "target"),
    "xpq.cft.mm_blocks": ("minecraft:bricks", "related"),
    "xpq.cft.recipes_200": ("minecraft:crafting_table", "related"),
    "xpq.cft.redstone_100": ("minecraft:redstone", "target"),
    "xpq.cft.backpack_large": ("moreminecrafts:large_backpack", "target"),
    "xpq.cft.portable_furnace": ("moreminecrafts:portable_furnace", "target"),
    # Building
    "xpq.bld.place_1000": ("minecraft:stone_bricks", "related"),
    "xpq.bld.place_10000": ("minecraft:bricks", "related"),
    "xpq.bld.place_50000": ("minecraft:stone_bricks", "related"),
    "xpq.bld.place_200000": ("minecraft:smooth_stone", "related"),
    "xpq.bld.variety_50": ("minecraft:chiseled_stone_bricks", "related"),
    "xpq.bld.variety_150": ("minecraft:cobblestone_wall", "related"),
    "xpq.bld.storage_50": ("minecraft:chest", "related"),
    "xpq.bld.copper_ages": ("minecraft:copper_block", "target"),
    # Alchemy
    "xpq.alc.shock_potion": ("minecraft:potion", "related"),
    "xpq.alc.splash_lingering": ("minecraft:lingering_potion", "target"),
    "xpq.alc.shock_10": ("minecraft:splash_potion", "target"),
    "xpq.alc.types_12": ("minecraft:brewing_stand", "related"),
    "xpq.alc.ingredients_all": ("minecraft:glistering_melon_slice", "related"),
    "xpq.alc.effects_16": ("minecraft:fermented_spider_eye", "related"),
    "xpq.alc.enhanced_3": ("minecraft:glowstone_dust", "related"),
    "xpq.alc.potions_250": ("minecraft:potion", "target"),
    "xpq.alc.injector": ("moreminecrafts:potion_injector", "target"),
    # Equipment and enhancement
    "xpq.eqp.enhance_5": ("moreminecrafts:enhancement_stone", "target"),
    "xpq.eqp.custom_enchant": ("minecraft:enchanted_book", "target"),
    "xpq.eqp.enhance_10": ("moreminecrafts:enhancement_stone", "target"),
    "xpq.eqp.enhance_trio": ("moreminecrafts:enhancement_stone", "target"),
    "xpq.eqp.enhance_15": ("moreminecrafts:enhancement_stone", "target"),
    "xpq.eqp.custom_enchant_5": ("minecraft:enchanted_book", "target"),
    "xpq.eqp.enhance_20": ("minecraft:netherite_ingot", "related"),
    "xpq.eqp.first_t2_core": ("moreminecrafts:heart_of_thunder", "target"),
    "xpq.eqp.t2_cores_3": ("moreminecrafts:heart_of_currents", "target"),
    "xpq.eqp.enhance_20_armor": ("minecraft:netherite_chestplate", "related"),
    "xpq.eqp.custom_enchant_all": ("minecraft:enchanted_book", "target"),
    "xpq.eqp.all_t2_cores": ("moreminecrafts:burning_heart", "target"),
    "xpq.eqp.first_t2_weapon": ("moreminecrafts:flame_sword", "target"),
    "xpq.eqp.t2_arsenal": ("moreminecrafts:wind_sword", "target"),
    "xpq.eqp.t2_plus20": ("moreminecrafts:lightning_sword", "target"),
    "xpq.eqp.accessory_first": ("moreminecrafts:gauntlet", "related"),
    "xpq.eqp.accessory_full": ("moreminecrafts:gauntlet", "related"),
    "xpq.eqp.accessory_all": ("moreminecrafts:gauntlet", "related"),
    "xpq.eqp.t3_special": ("moreminecrafts:eye_of_typhoon", "target"),
    "xpq.eqp.t3_set": ("moreminecrafts:dark_energy_staff", "target"),
    # Knowledge and research
    "xpq.grw.knowledge_10": ("minecraft:book", "related"),
    "xpq.grw.first_research": ("minecraft:lectern", "related"),
    "xpq.grw.knowledge_20": ("minecraft:book", "related"),
    "xpq.grw.seven_roots": ("minecraft:oak_sapling", "related"),
    "xpq.grw.research_10": ("minecraft:enchanting_table", "related"),
    "xpq.grw.knowledge_35": ("minecraft:book", "related"),
    "xpq.grw.first_t2": ("minecraft:amethyst_shard", "related"),
    "xpq.grw.discipline_breadth": ("minecraft:bookshelf", "related"),
    "xpq.grw.spent_10k": ("minecraft:emerald", "related"),
    "xpq.grw.knowledge_50": ("minecraft:book", "related"),
    "xpq.grw.research_25": ("minecraft:enchanting_table", "related"),
    "xpq.grw.first_t3": ("minecraft:echo_shard", "related"),
    "xpq.grw.spent_50k": ("minecraft:emerald_block", "related"),
    "xpq.grw.knowledge_complete": ("minecraft:bookshelf", "related"),
    "xpq.grw.research_50": ("minecraft:enchanting_table", "related"),
    "xpq.grw.t3_5": ("minecraft:echo_shard", "related"),
    "xpq.grw.discipline_complete": ("minecraft:nether_star", "related"),
    "xpq.grw.climbing_100": ("minecraft:ladder", "target"),
    "xpq.grw.wall_jump_50": ("minecraft:slime_block", "related"),
    "xpq.grw.fall_roll_20": ("minecraft:feather", "related"),
    "xpq.grw.throwable_pickaxe": ("minecraft:iron_pickaxe", "target"),
}


def vanilla_items():
    """Item ids of the game version the mod targets, read from the game jar's lang file."""
    for jar in (VANILLA_LANG, VANILLA_LANG_FALLBACK):
        if jar.exists():
            lang = json.loads(zipfile.ZipFile(jar).read("assets/minecraft/lang/en_us.json"))
            # Block items are keyed as block.minecraft.* in the lang file; both are valid item icons.
            return {k.split(".", 2)[2] for k in lang if k.startswith(("item.minecraft.", "block.minecraft."))}
    raise SystemExit("vanilla lang not found; run the Gradle build once to populate the loom cache")


def mod_items():
    lang = json.loads(MOD_LANG.read_text(encoding="utf-8"))
    return {k[len("item.moreminecrafts."):] for k in lang if k.startswith("item.moreminecrafts.")}


def build():
    quests = [q["quest_id"] for q in json.loads(MAPPING.read_text(encoding="utf-8"))["quests"]]
    category = {q["quest_id"]: q["category"] for q in json.loads(MAPPING.read_text(encoding="utf-8"))["quests"]}
    vanilla, mod = vanilla_items(), mod_items()
    entries = []
    problems = []
    for qid in quests:
        if qid in ICONS:
            item, source = ICONS[qid]
        else:
            item, source = CATEGORY_FALLBACK[category[qid]]
        namespace, _, path = item.partition(":")
        if namespace == "minecraft" and path not in vanilla:
            problems.append(f"{qid}: {item} is not a vanilla item")
        if namespace == "moreminecrafts" and path not in mod:
            problems.append(f"{qid}: {item} is not a MoreMinecrafts item")
        if namespace not in ("minecraft", "moreminecrafts"):
            problems.append(f"{qid}: unknown namespace {namespace}")
        entries.append({"quest_id": qid, "icon_item_id": item, "source": source})
    unused = sorted(set(ICONS) - set(quests))
    if unused:
        problems.append(f"icon entries for unknown quests: {unused}")
    return entries, problems


def main():
    entries, problems = build()
    if problems:
        print("\n".join(problems))
        raise SystemExit(1)
    payload = {"schema": 1, "count": len(entries),
                "rule": "target item/entity, then related item, then category fallback; never completion-dependent",
                "icons": entries}
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if "--check" in sys.argv:
        if OUT.read_text(encoding="utf-8") != text:
            print("quest_icons.json is stale; re-run generate_quest_icons.py")
            raise SystemExit(1)
        print("quest_icons.json is current:", len(entries), "entries")
        return
    OUT.write_text(text, encoding="utf-8")
    sources = {}
    for e in entries:
        sources[e["source"]] = sources.get(e["source"], 0) + 1
    print("wrote", len(entries), "icons; sources", sources)


if __name__ == "__main__":
    main()
