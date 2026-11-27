#!/usr/bin/env python3
"""Generates the XP Quest 200-quest structure proposal from the authoritative catalog.

Inputs:  src/main/resources/data/moreminecrafts/xp_quest/catalog.json (read only)
Outputs: docs/design/xp-quest/structure/quest_structure_mapping.json
         docs/design/xp-quest/structure/edge_ledger.json
         docs/design/xp-quest/structure/localization_coverage.json

The chapter table below is the proposal under owner review. It is presentation data only: it
registers no quests, changes no rewards, and enables no prerequisites.
"""
import json
import pathlib
from collections import Counter

ROOT = pathlib.Path(__file__).resolve().parents[2]
CATALOG = ROOT / "src/main/resources/data/moreminecrafts/xp_quest/catalog.json"
LANG_EN = ROOT / "src/main/resources/assets/moreminecrafts/lang/en_us.json"
LANG_KO = ROOT / "src/main/resources/assets/moreminecrafts/lang/ko_kr.json"
OUT = ROOT / "docs/design/xp-quest/structure"

TIER_RANK = {"EARLY": 0, "EARLY_MID": 1, "MID": 2, "MID_LATE": 3, "LATE": 4, "ENDGAME": 5}

# chapter id -> (icon, en, ko, [(sub id, en, ko, [quest ids in listed order])])
CHAPTERS = [
    ("ecology", "minecraft:spider_eye", "Ecology", "생태계", [
        ("slice", "Cave, bestiary and habitats", "동굴·도감·서식지", [
            "xpq.cmb.cave_dwellers", "xpq.cmb.bestiary_25", "xpq.cmb.habitats_5"]),
    ]),
    ("exploration", "minecraft:compass", "Exploration", "탐험", [
        ("loot", "Structures and relics", "구조물과 유물", [
            "xpq.exp.first_structure_loot", "xpq.exp.mineshaft_loot", "xpq.exp.dungeon_loot",
            "xpq.exp.temples", "xpq.exp.ocean_relics", "xpq.exp.villages_5", "xpq.exp.structures_15",
            "xpq.exp.grand_expedition", "xpq.exp.archaeology_30", "xpq.exp.sherds_15"]),
        ("biomes", "Biomes and maps", "바이옴과 지도", [
            "xpq.exp.explorer_maps", "xpq.exp.biomes_25", "xpq.exp.rare_biomes", "xpq.exp.biomes_40",
            "xpq.exp.biomes_all_overworld", "xpq.exp.sleeping_bag_biomes"]),
        ("travel", "Distance and movement", "거리와 이동", [
            "xpq.exp.walk_10k", "xpq.exp.walk_50k", "xpq.exp.sail_20k", "xpq.exp.ride_10k",
            "xpq.exp.elytra_25k", "xpq.exp.travel_250k", "xpq.exp.parachute_landing", "xpq.exp.height_extremes"]),
    ]),
    ("monster_hunt", "minecraft:bone", "Monster Hunt", "몬스터 사냥", [
        ("kill_counts", "Kill counts", "처치 횟수", [
            "xpq.cmb.hostile_100", "xpq.cmb.hostile_500", "xpq.cmb.hostile_2000", "xpq.cmb.hostile_5000"]),
        ("species", "Species records", "종 기록", [
            "xpq.cmb.bestiary_5", "xpq.cmb.bestiary_10", "xpq.cmb.vanilla_species_20"]),
    ]),
    ("arms", "minecraft:iron_sword", "Arms and Tactics", "무기와 전술", [
        ("melee", "Melee", "근접", [
            "xpq.cmb.mm_arms", "xpq.cmb.dagger_100", "xpq.cmb.greatsword_100", "xpq.cmb.mace_smash_10",
            "xpq.cmb.netherite_blades", "xpq.cmb.shield_500"]),
        ("ranged", "Ranged", "원거리", ["xpq.cmb.long_shot_25", "xpq.cmb.trident_50"]),
        ("planned", "Planned", "예정", ["xpq.cmb.gauntlet_50", "xpq.cmb.tactical_ammo"]),
    ]),
    ("elite", "minecraft:totem_of_undying", "Elite Foes", "정예 적", [
        ("raids", "Raids and bands", "습격과 무리", [
            "xpq.cmb.bandits_20", "xpq.cmb.raids_3", "xpq.cmb.ravagers_5", "xpq.cmb.brutes_3"]),
        ("elites", "Elite mobs", "정예 몹", [
            "xpq.cmb.breeze_10", "xpq.cmb.guardians_30", "xpq.cmb.lesser_wither_10", "xpq.cmb.wither_skulls_3"]),
        ("elemental", "Elemental (planned)", "원소 (예정)", ["xpq.cmb.elemental_100", "xpq.cmb.elemental_skills"]),
    ]),
    ("mining", "minecraft:diamond_pickaxe", "Mining", "채굴", [
        ("ores", "Ores", "광석", [
            "xpq.min.first_custom_ore", "xpq.min.custom_ore_survey", "xpq.min.ore_1000", "xpq.min.ore_5000",
            "xpq.min.ore_20000", "xpq.min.diamond_ore_64", "xpq.min.deepslate_custom"]),
        ("debris", "Debris and rare", "잔해와 희귀", [
            "xpq.min.adamantium_debris", "xpq.min.adamantium_10", "xpq.min.debris_16", "xpq.min.obsidian_64"]),
        ("stone", "Stone and tools", "돌과 도구", [
            "xpq.min.stone_10000", "xpq.min.amethyst_spyglass", "xpq.min.pickaxe_5000", "xpq.min.vein_500",
            "xpq.min.drill_1000"]),
        ("wood", "Woodcutting", "벌목", ["xpq.min.logs_1000", "xpq.min.all_wood", "xpq.min.lumberjack_200"]),
    ]),
    ("farming", "minecraft:wheat", "Farming and Husbandry", "농사와 목축", [
        ("crops", "Crops and food", "작물과 음식", [
            "xpq.frm.harvest_all_crops", "xpq.frm.composite_foods", "xpq.frm.till_1000", "xpq.frm.harvest_1000",
            "xpq.frm.harvest_5000", "xpq.frm.ancient_harvest", "xpq.frm.soil_study_harvest"]),
        ("livestock", "Livestock", "가축", ["xpq.frm.tame_5", "xpq.frm.breed_200", "xpq.frm.honey_32"]),
        ("trades", "Villagers and trade", "주민과 거래", [
            "xpq.frm.master_trade", "xpq.frm.mending_book", "xpq.frm.professions_10", "xpq.frm.trades_200",
            "xpq.frm.barter_200", "xpq.frm.villager_discount_20"]),
        ("capture", "Capture and automation", "포획과 자동화", [
            "xpq.frm.first_capture", "xpq.frm.capture_10", "xpq.frm.auto_farm"]),
    ]),
    ("fishing", "minecraft:fishing_rod", "Fishing", "낚시", [
        ("catches", "Catches and waters", "어획과 수역", [
            "xpq.fsh.freshwater_4", "xpq.fsh.water_biomes_8", "xpq.fsh.ocean_trio", "xpq.fsh.multi_catch",
            "xpq.fsh.catch_500", "xpq.fsh.full_catalogue", "xpq.fsh.giant_catch"]),
        ("sales", "Sales", "판매", ["xpq.fsh.first_sale", "xpq.fsh.sale_64", "xpq.fsh.emerald_500"]),
        ("rods", "Rods and gear", "낚싯대와 장비", [
            "xpq.fsh.first_module", "xpq.fsh.grade_four", "xpq.fsh.grade_five", "xpq.fsh.tier3_rig",
            "xpq.fsh.grade_five_5"]),
        ("traps", "Traps (planned)", "덫 (예정)", ["xpq.fsh.fish_trap"]),
    ]),
    ("crafting", "minecraft:crafting_table", "Crafting", "제작", [
        ("essentials", "Essentials", "기본", [
            "xpq.cft.sleeping_bag", "xpq.cft.parachute", "xpq.cft.anvil_50", "xpq.cft.smelt_1000",
            "xpq.cft.enchanting_30"]),
        ("weapons", "Weapon sets", "무기 세트", ["xpq.cft.all_daggers", "xpq.cft.all_greatswords"]),
        ("advanced", "Advanced", "고급", [
            "xpq.cft.conduit", "xpq.cft.mm_metallurgy", "xpq.cft.netherite_tools", "xpq.cft.mm_blocks"]),
        ("recipes", "Recipes", "조합법", ["xpq.cft.recipes_200", "xpq.cft.redstone_100"]),
        ("planned", "Planned", "예정", ["xpq.cft.backpack_large", "xpq.cft.portable_furnace"]),
    ]),
    ("building", "minecraft:bricks", "Building", "건축", [
        ("placement", "Placement", "배치", [
            "xpq.bld.place_1000", "xpq.bld.place_10000", "xpq.bld.place_50000", "xpq.bld.place_200000"]),
        ("variety", "Variety and storage", "다양성과 보관", ["xpq.bld.variety_50", "xpq.bld.variety_150", "xpq.bld.storage_50"]),
        ("copper", "Copper ages", "구리 시대", ["xpq.bld.copper_ages"]),
    ]),
    ("alchemy", "minecraft:brewing_stand", "Alchemy", "연금술", [
        ("brewing", "Brewing", "양조", ["xpq.alc.shock_potion", "xpq.alc.splash_lingering", "xpq.alc.shock_10"]),
        ("variety", "Variety", "다양성", ["xpq.alc.types_12", "xpq.alc.ingredients_all", "xpq.alc.effects_16"]),
        ("mastery", "Mastery", "숙달", ["xpq.alc.enhanced_3", "xpq.alc.potions_250", "xpq.alc.injector"]),
    ]),
    ("equipment", "minecraft:anvil", "Equipment and Enhancement", "장비와 강화", [
        ("enhance", "Enhancement", "강화", [
            "xpq.eqp.enhance_5", "xpq.eqp.enhance_10", "xpq.eqp.enhance_15", "xpq.eqp.enhance_trio",
            "xpq.eqp.enhance_20", "xpq.eqp.enhance_20_armor"]),
        ("custom", "Custom enchants", "커스텀 마법", ["xpq.eqp.custom_enchant", "xpq.eqp.custom_enchant_5", "xpq.eqp.custom_enchant_all"]),
        ("t2_cores", "T2 cores", "T2 코어", ["xpq.eqp.first_t2_core", "xpq.eqp.t2_cores_3", "xpq.eqp.all_t2_cores"]),
        ("t2_weapons", "T2 weapons (planned)", "T2 무기 (예정)", ["xpq.eqp.first_t2_weapon", "xpq.eqp.t2_arsenal", "xpq.eqp.t2_plus20"]),
        ("accessories", "Accessories (planned)", "장신구 (예정)", ["xpq.eqp.accessory_first", "xpq.eqp.accessory_full", "xpq.eqp.accessory_all"]),
        ("t3", "T3 (planned)", "T3 (예정)", ["xpq.eqp.t3_special", "xpq.eqp.t3_set"]),
    ]),
    ("research", "minecraft:enchanted_book", "Knowledge and Research", "지식과 연구", [
        ("knowledge", "Knowledge", "지식", ["xpq.grw.knowledge_10", "xpq.grw.knowledge_20", "xpq.grw.knowledge_35",
                                             "xpq.grw.knowledge_50", "xpq.grw.knowledge_complete"]),
        ("research", "Research", "연구", ["xpq.grw.first_research", "xpq.grw.research_10", "xpq.grw.research_25",
                                           "xpq.grw.research_50", "xpq.grw.seven_roots", "xpq.grw.discipline_breadth",
                                           "xpq.grw.discipline_complete"]),
        ("tiers", "Tiers", "단계", ["xpq.grw.first_t2", "xpq.grw.first_t3", "xpq.grw.t3_5", "xpq.grw.spent_10k",
                                     "xpq.grw.spent_50k"]),
        ("mobility", "Mobility (verify)", "이동 (검증 중)", ["xpq.grw.climbing_100", "xpq.grw.wall_jump_50",
                                                           "xpq.grw.fall_roll_20", "xpq.grw.throwable_pickaxe"]),
    ]),
    ("dimensions", "minecraft:ender_eye", "Dimensions", "차원", [
        ("nether", "Nether", "네더", ["xpq.dim.fortress_loot", "xpq.dim.blaze_rods_32", "xpq.dim.bastion_all",
                                       "xpq.dim.nether_travel_50k"]),
        ("end", "End", "엔드", ["xpq.dim.end_cities_3", "xpq.dim.shulker_boxes", "xpq.dim.end_city_loot_10"]),
        ("vaults", "Ruins and vaults", "유적과 금고", ["xpq.dim.trial_vaults_5", "xpq.dim.ancient_city_loot",
                                                     "xpq.dim.ominous_vaults_5"]),
    ]),
    ("bosses", "minecraft:nether_star", "Bosses", "보스", [
        ("sea", "Sea and monuments", "바다와 신전", ["xpq.boss.elder_guardian", "xpq.boss.monument_clear",
                                                    "xpq.boss.monuments_3"]),
        ("deep", "Deep dark", "심층 어둠", ["xpq.boss.warden", "xpq.boss.warden_3"]),
        ("nether_end", "Nether and End", "네더와 엔드", ["xpq.boss.wither", "xpq.boss.dragon_rematch",
                                                       "xpq.boss.twin_bosses"]),
        ("omens", "Omens", "전조", ["xpq.boss.omen_raid"]),
    ]),
]

# Presentation-only suggestions and cross-chapter candidates. None of these gate gameplay.
PRESENTATION_EDGES = [
    ("xpq.cmb.hostile_100", "xpq.cmb.hostile_500", "suggested_progression", "Same kill-count ladder; visual reading order only."),
    ("xpq.cmb.hostile_500", "xpq.cmb.hostile_2000", "suggested_progression", "Same kill-count ladder; visual reading order only."),
    ("xpq.cmb.hostile_2000", "xpq.cmb.hostile_5000", "suggested_progression", "Same kill-count ladder; visual reading order only."),
    ("xpq.exp.walk_10k", "xpq.exp.walk_50k", "suggested_progression", "Same walking ladder; visual reading order only."),
    ("xpq.min.ore_1000", "xpq.min.ore_5000", "suggested_progression", "Same ore counter ladder; visual reading order only."),
    ("xpq.min.ore_5000", "xpq.min.ore_20000", "suggested_progression", "Same ore counter ladder; visual reading order only."),
    ("xpq.frm.harvest_1000", "xpq.frm.harvest_5000", "suggested_progression", "Same harvest counter ladder; visual reading order only."),
    ("xpq.bld.place_1000", "xpq.bld.place_10000", "suggested_progression", "Same placement ladder; visual reading order only."),
    ("xpq.bld.place_10000", "xpq.bld.place_50000", "suggested_progression", "Same placement ladder; visual reading order only."),
    ("xpq.bld.place_50000", "xpq.bld.place_200000", "suggested_progression", "Same placement ladder; visual reading order only."),
    ("xpq.grw.knowledge_10", "xpq.grw.knowledge_20", "suggested_progression", "Same knowledge ladder; visual reading order only."),
    ("xpq.grw.knowledge_20", "xpq.grw.knowledge_35", "suggested_progression", "Same knowledge ladder; visual reading order only."),
    ("xpq.grw.knowledge_35", "xpq.grw.knowledge_50", "suggested_progression", "Same knowledge ladder; visual reading order only."),
    ("xpq.grw.knowledge_50", "xpq.grw.knowledge_complete", "suggested_progression", "Same knowledge ladder; visual reading order only."),
    ("xpq.eqp.enhance_5", "xpq.eqp.enhance_10", "suggested_progression", "Enhancement milestones in ascending order."),
    ("xpq.eqp.enhance_10", "xpq.eqp.enhance_15", "suggested_progression", "Enhancement milestones in ascending order."),
    ("xpq.eqp.enhance_15", "xpq.eqp.enhance_20", "suggested_progression", "Enhancement milestones in ascending order."),
    ("xpq.eqp.first_t2_core", "xpq.eqp.t2_cores_3", "suggested_progression", "Core collection starts with the first core."),
    ("xpq.eqp.t2_cores_3", "xpq.eqp.all_t2_cores", "suggested_progression", "Core collection starts with the first core."),
    ("xpq.dim.fortress_loot", "xpq.dim.blaze_rods_32", "suggested_progression", "Fortresses are the usual blaze-rod source."),
    ("xpq.boss.monument_clear", "xpq.boss.monuments_3", "suggested_progression", "Single-monument clear before three-monument clear."),
    ("xpq.cft.parachute", "xpq.exp.parachute_landing", "cross_chapter_presentation", "Craft the parachute, then land with it. Presentation link only."),
    ("xpq.dim.blaze_rods_32", "xpq.alc.shock_potion", "cross_chapter_presentation", "Blaze rods feed brewing. Presentation link only."),
    ("xpq.eqp.first_t2_core", "xpq.grw.first_t2", "cross_chapter_presentation", "Reading order: T2 research sits before T2 core work. Presentation link only."),
]

# Gameplay-enforced candidates: NOT enabled. Each needs explicit owner approval and rule verification.
GAMEPLAY_CANDIDATES = [
    ("xpq.grw.first_t2", "xpq.eqp.first_t2_weapon", "T2 weapons are planned as knowledge-gated. The gate is not yet verified in current authority."),
    ("xpq.grw.first_t3", "xpq.eqp.t3_special", "T3 specials are planned as knowledge-gated. The gate is not yet verified in current authority."),
]


def build():
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    quests = {q["id"]: q for q in catalog["quests"]}
    en = json.loads(LANG_EN.read_text(encoding="utf-8"))
    ko = json.loads(LANG_KO.read_text(encoding="utf-8"))

    assigned = {}
    chapter_nodes = {}
    entries = []
    subs_out = []
    chapters_out = []
    for ci, (cid, icon, cen, cko, subs) in enumerate(CHAPTERS, start=1):
        chapter_nodes[cid] = []
        sub_rows = []
        for sub_id, sen, sko, ids in subs:
            sub_rows.append({"id": f"{cid}.{sub_id}", "title": {"en": sen, "ko": sko}, "quest_count": len(ids)})
            ordered = sorted(ids, key=lambda qid: (TIER_RANK[quests[qid]["tier"]], qid))
            for qid in ordered:
                if qid in assigned:
                    raise SystemExit(f"duplicate assignment {qid}")
                assigned[qid] = (cid, f"{cid}.{sub_id}")
                chapter_nodes[cid].append(qid)
                q = quests[qid]
                entries.append({
                    "quest_id": qid, "category": q["tab"], "chapter": cid, "sub_branch": f"{cid}.{sub_id}",
                    "status": q["status"], "tier": q["tier"], "registered": q["registered"],
                    "reward_raw_xp": q["rewardRawXp"],
                })
        chapters_out.append({
            "id": cid, "chapter_order": ci, "icon_item_id": icon,
            "title": {"en": cen, "ko": cko}, "quest_count": len(chapter_nodes[cid]), "sub_branches": sub_rows,
        })
        subs_out.extend(sub_rows)

    missing = sorted(set(quests) - set(assigned))
    extra = sorted(set(assigned) - set(quests))
    if missing or extra:
        raise SystemExit(f"coverage mismatch missing={missing} extra={extra}")

    for order, e in enumerate(entries, start=1):
        e["presentation_order"] = order
    # Within a chapter the presentation order is contiguous; re-derive the chapter-local index too.
    local = Counter()
    for e in entries:
        local[e["chapter"]] += 1
        e["chapter_index"] = local[e["chapter"]]

    mapping = {
        "schema": 1,
        "status": "PROPOSAL - presentation data only; not loaded by runtime; owner review required",
        "source_catalog": "src/main/resources/data/moreminecrafts/xp_quest/catalog.json",
        "catalog_quest_count": len(quests),
        "catalog_total_raw_xp": catalog["approvedTotal"],
        "chapter_order": [c["id"] for c in chapters_out],
        "chapters": chapters_out,
        "quests": entries,
    }

    ledger_entries = []
    for n, (parent, child, kind, rationale) in enumerate(PRESENTATION_EDGES, start=1):
        ledger_entries.append({
            "id": f"edge.presentation.{n:02d}", "parent": parent, "child": child, "kind": kind,
            "enforcement": "presentation-only", "status": "proposed",
            "rationale": rationale,
            "retroactive_completion": "none: presentation-only edges never block or unlock a claim",
        })
    for n, (parent, child, rationale) in enumerate(GAMEPLAY_CANDIDATES, start=1):
        ledger_entries.append({
            "id": f"edge.gameplay.{n:02d}", "parent": parent, "child": child, "kind": "gameplay_candidate",
            "enforcement": "gameplay-enforced (NOT ENABLED)", "status": "awaiting_owner_approval",
            "rationale": rationale,
            "retroactive_completion": (
                "If enabled: a child already claimed keeps its claim. An unclaimed child waits until the parent "
                "is completed. A parent completed before the rule activates counts immediately. No XP is granted "
                "retroactively to unclaimed children."),
        })
    ledger = {"schema": 1, "status": "PROPOSAL", "edges": ledger_entries}

    # Localization coverage for quest copy and chapter/sub-branch titles.
    per_lang = {}
    for lang, table in (("en", en), ("ko", ko)):
        have = {"title": 0, "description": 0, "objective": 0}
        for qid in quests:
            for field in have:
                if f"moreminecrafts.xpq.{qid[4:]}.{field}" in table:
                    have[field] += 1
        per_lang[lang] = {"quest_fields_present": have, "quest_fields_expected_each": len(quests),
                          "quests_with_full_copy": sum(1 for qid in quests if all(
                              f"moreminecrafts.xpq.{qid[4:]}.{f}" in table for f in have))}
    coverage = {
        "schema": 1,
        "quest_count": len(quests),
        "quest_copy": per_lang,
        "chapter_and_sub_titles": {
            "proposal_supplied_en_and_ko": len(chapters_out) + len(subs_out),
            "note": "Chapter and sub-branch titles are carried in quest_structure_mapping.json, not in lang files.",
        },
    }

    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "quest_structure_mapping.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (OUT / "edge_ledger.json").write_text(json.dumps(ledger, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (OUT / "localization_coverage.json").write_text(json.dumps(coverage, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("quests", len(entries), "chapters", len(chapters_out), "subs", len(subs_out), "edges", len(ledger_entries))
    print("per category", dict(Counter(e["category"] for e in entries)))
    print("localization", json.dumps(per_lang))


if __name__ == "__main__":
    build()
