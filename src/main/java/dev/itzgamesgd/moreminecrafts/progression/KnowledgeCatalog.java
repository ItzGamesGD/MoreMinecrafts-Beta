package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;

/**
 * Stable, free-knowledge roster. The condition kinds deliberately describe observed gameplay
 * facts rather than UI labels; event collectors can be added without changing saved IDs.
 */
public final class KnowledgeCatalog {
    public enum ConditionType {
        ORE_TYPES_MINED, DEEPSLATE_ORE_TYPES_MINED, ITEM_SET_ACQUIRED, ITEM_COUNT_ACQUIRED,
        PLANT_TYPES_ACQUIRED, SOIL_TYPES_ACQUIRED, ROCK_TYPES_MINED, TOOL_DURABILITY_USED,
        ORE_TYPES_SMELTED, CRAFTED_ITEM_SET, RECIPES_COMPLETED, ANVIL_REPAIRS,
        BLOCKS_PLACED, BLOCKS_HARVESTED, CROPS_TILLED, FARM_WATERED, ANIMAL_TYPES_FED,
        ANIMAL_BREEDINGS, FISH_SPECIES_CAUGHT, DISTINCT_WATER_FISHING, FISHING_SUCCESSES,
        HOSTILE_KILLS_BY_WEAPON, SHIELD_BLOCKS, ARMOR_WORN, PROJECTILE_WEAPON_KILLS,
        RUNNING_KILLS, AMMUNITION_USED, ENTITY_KILLS, PIGLIN_BARTERS,
        DISTINCT_ENTITY_TYPES_ENCOUNTERED, BIOMES_VISITED, BOAT_DISTANCE, HORSE_DISTANCE,
        ELYTRA_DISTANCE, DIMENSIONS_VISITED, ARCHAEOLOGY_ITEMS_BRUSHED,
        DISTINCT_STRUCTURES_VISITED, VILLAGER_TRADES, BREWING_FIRST, POTION_TYPES_BREWED, POTION_ITEMS_BREWED,
        POTION_ITEMS_BREWED, EFFECT_TYPES_EXPERIENCED, BREWING_INGREDIENTS_ACQUIRED
    }

    public record Entry(int number, String discipline, String koreanName,
                        ConditionType conditionType, int target, String conditionKey,
                        String rewardKey, boolean unresolved) {
        public String id() { return "knowledge." + number; }
        public String stateKey() { return id(); }
        public String titleKey() { return "moreminecrafts.knowledge." + number + ".title"; }
        public String descriptionKey() { return "moreminecrafts.knowledge." + number + ".description"; }
    }

    private static final List<Entry> ENTRIES = List.of(
        e(1,"resources","광석 식별",ConditionType.ORE_TYPES_MINED,5,"ore_types_mined","ore_xp",false),
        e(2,"resources","심층 광물학",ConditionType.DEEPSLATE_ORE_TYPES_MINED,5,"deepslate_ore_types_mined","deepslate_ore_xp",false),
        e(3,"resources","희귀 광물학",ConditionType.ITEM_SET_ACQUIRED,3,"rare_ores_acquired","rare_ore_guide",false),
        e(4,"resources","수목 관찰",ConditionType.ITEM_SET_ACQUIRED,6,"log_types_acquired","sapling_drop",false),
        e(5,"resources","거목 생태",ConditionType.ITEM_COUNT_ACQUIRED,128,"large_tree_logs_acquired","tree_guide",false),
        e(6,"resources","식물 채집",ConditionType.PLANT_TYPES_ACQUIRED,10,"plant_types_acquired","plant_guide",false),
        e(7,"resources","토양 분석",ConditionType.SOIL_TYPES_ACQUIRED,4,"soil_types_acquired","soil_guide",false),
        e(8,"resources","암석 분류",ConditionType.ROCK_TYPES_MINED,8,"rock_types_mined","rock_guide",false),
        e(9,"resources","광맥 탐사",ConditionType.ITEM_COUNT_ACQUIRED,256,"ore_items_mined","ore_xp",false),
        e(10,"resources","자원 보존",ConditionType.TOOL_DURABILITY_USED,500,"pickaxe_durability_used","pickaxe_durability",false),
        e(11,"production","제련 원리",ConditionType.ORE_TYPES_SMELTED,5,"ore_types_smelted","smelting_xp",false),
        e(12,"production","고온 제련",ConditionType.CRAFTED_ITEM_SET,1,"furnace_blast_furnace_used","smelting_xp",false),
        e(13,"production","합금 이해",ConditionType.CRAFTED_ITEM_SET,1,"netherite_ingot_crafted","alloy_guide",false),
        e(14,"production","제작 기술",ConditionType.CRAFTED_ITEM_SET,5,"tool_types_crafted","crafting_guide",false),
        e(15,"production","정밀 제작",ConditionType.RECIPES_COMPLETED,50,"recipes_completed","recipe_hints",false),
        e(16,"production","장비 정비",ConditionType.ANVIL_REPAIRS,10,"anvil_repairs","repair_info",false),
        e(17,"production","보관 기술",ConditionType.CRAFTED_ITEM_SET,10,"chests_crafted","storage_guide",false),
        e(18,"production","물류 공학",ConditionType.BLOCKS_PLACED,5,"hoppers_placed","logistics_guide",false),
        e(19,"production","자동화 원리",ConditionType.BLOCKS_PLACED,20,"redstone_components_placed","redstone_guide",false),
        e(20,"living","작물 생장",ConditionType.BLOCKS_HARVESTED,100,"crops_harvested","growth_info",false),
        e(21,"living","종자 연구",ConditionType.ITEM_SET_ACQUIRED,5,"seed_types_acquired","seed_guide",false),
        e(22,"living","토양 관리",ConditionType.CROPS_TILLED,100,"farmland_tilled","farming_info",false),
        e(23,"living","관개 기술",ConditionType.FARM_WATERED,1,"farmland_watered","irrigation_info",false),
        e(24,"living","농업 자동화",ConditionType.CRAFTED_ITEM_SET,3,"farm_automation_parts_crafted","automation_guide",false),
        e(25,"living","가축 사육",ConditionType.ANIMAL_TYPES_FED,5,"animal_types_fed","breeding_guide",false),
        e(26,"living","번식 생태",ConditionType.ANIMAL_BREEDINGS,30,"animal_breedings","breeding_info",false),
        e(27,"living","어류 도감",ConditionType.FISH_SPECIES_CAUGHT,5,"fish_species_caught","fish_guide",false),
        e(28,"living","수역 분석",ConditionType.DISTINCT_WATER_FISHING,2,"water_biomes_fished","water_info",false),
        e(29,"living","낚시 기술",ConditionType.FISHING_SUCCESSES,100,"fishing_successes","fishing_xp",false),
        e(30,"living","식품 가공",ConditionType.CRAFTED_ITEM_SET,10,"foods_crafted_or_cooked","food_guide",false),
        e(31,"combat","검술 관찰",ConditionType.HOSTILE_KILLS_BY_WEAPON,50,"sword_hostile_kills","sword_info",false),
        e(32,"combat","연속 공격",ConditionType.HOSTILE_KILLS_BY_WEAPON,100,"sword_hostile_kills","sword_xp",false),
        e(33,"combat","중량 무기",ConditionType.HOSTILE_KILLS_BY_WEAPON,30,"axe_hostile_kills","heavy_weapon_info",false),
        e(34,"combat","궁술 이해",ConditionType.HOSTILE_KILLS_BY_WEAPON,50,"bow_hostile_kills","bow_info",false),
        e(35,"combat","석궁 구조",ConditionType.HOSTILE_KILLS_BY_WEAPON,30,"crossbow_hostile_kills","crossbow_info",false),
        e(36,"combat","방패 방어",ConditionType.SHIELD_BLOCKS,50,"shield_blocks","shield_durability",false),
        e(37,"combat","방어구 구조",ConditionType.ARMOR_WORN,1,"full_armor_worn","armor_info",false),
        e(38,"combat","투척 무기",ConditionType.PROJECTILE_WEAPON_KILLS,10,"trident_hostile_kills","thrown_weapon_info",false),
        e(39,"combat","전투 기동",ConditionType.RUNNING_KILLS,30,"running_hostile_kills","mobility_info",false),
        e(40,"combat","특수 탄약",ConditionType.AMMUNITION_USED,1,"special_ammunition_used","ammunition_guide",false),
        e(41,"ecology","좀비 생태",ConditionType.ENTITY_KILLS,30,"zombie_kills","zombie_damage",false),
        e(42,"ecology","스켈레톤 생태",ConditionType.ENTITY_KILLS,30,"skeleton_kills","skeleton_damage",false),
        e(43,"ecology","크리퍼 생태",ConditionType.ENTITY_KILLS,30,"creeper_kills","creeper_damage",false),
        e(44,"ecology","거미 생태",ConditionType.ENTITY_KILLS,30,"spider_kills","spider_damage",false),
        e(45,"ecology","엔더맨 생태",ConditionType.ENTITY_KILLS,15,"enderman_kills","enderman_damage",false),
        e(46,"ecology","슬라임 생태",ConditionType.ENTITY_KILLS,20,"slime_kills","slime_damage",false),
        e(47,"ecology","일리저 사회",ConditionType.ENTITY_KILLS,2,"pillager_and_vindicator_kills","illager_damage",false),
        e(48,"ecology","피글린 사회",ConditionType.PIGLIN_BARTERS,20,"piglin_barters","piglin_damage",false),
        e(49,"ecology","호글린 생태",ConditionType.ENTITY_KILLS,20,"hoglin_kills","hoglin_damage",false),
        e(50,"ecology","수중 적대 생물",ConditionType.ENTITY_KILLS,2,"drowned_and_guardian_kills","aquatic_hostile_damage",false),
        e(51,"ecology","희귀 생물",ConditionType.DISTINCT_ENTITY_TYPES_ENCOUNTERED,3,"rare_mob_types_encountered","rare_mob_damage",true),
        e(52,"ecology","보스 생태",ConditionType.ENTITY_KILLS,1,"ender_dragon_or_wither_killed","boss_damage",false),
        e(53,"world","지형 탐사",ConditionType.BIOMES_VISITED,15,"biomes_visited","terrain_guide",false),
        e(54,"world","동굴 탐사",ConditionType.BIOMES_VISITED,3,"cave_biomes_visited","cave_guide",false),
        e(55,"world","해양 항해",ConditionType.BOAT_DISTANCE,5000,"boat_distance","navigation_info",false),
        e(56,"world","승마 기술",ConditionType.HORSE_DISTANCE,2000,"horse_distance","riding_info",false),
        e(57,"world","비행 관찰",ConditionType.ELYTRA_DISTANCE,2000,"elytra_distance","parachute_recipe",false),
        e(58,"world","차원 탐험",ConditionType.DIMENSIONS_VISITED,2,"nether_and_end_visited","dimension_guide",false),
        e(59,"world","고고학",ConditionType.ARCHAEOLOGY_ITEMS_BRUSHED,10,"archaeology_items_brushed","archaeology_guide",false),
        e(60,"world","고대 유적",ConditionType.DISTINCT_STRUCTURES_VISITED,5,"structures_visited","ruin_guide",false),
        e(61,"world","주민 사회",ConditionType.VILLAGER_TRADES,30,"villager_trades","trading_info",false),
        e(62,"world","원정 기술",ConditionType.DISTINCT_STRUCTURES_VISITED,10,"structures_visited","expedition_guide",false),
        e(63,"alchemy","양조 기초",ConditionType.BREWING_FIRST,1,"potions_brewed","brewing_guide",false),
        e(64,"alchemy","물약 성질",ConditionType.POTION_TYPES_BREWED,5,"potion_types_brewed","potion_info",false),
        e(65,"alchemy","투척 물약",ConditionType.POTION_ITEMS_BREWED,1,"splash_potions_brewed","splash_potion_guide",false),
        e(66,"alchemy","잔류 물약",ConditionType.POTION_ITEMS_BREWED,1,"lingering_potions_brewed","lingering_potion_guide",false),
        e(67,"alchemy","상태 효과",ConditionType.EFFECT_TYPES_EXPERIENCED,8,"effect_types_experienced","effect_info",false),
        e(68,"alchemy","마법 재료",ConditionType.BREWING_INGREDIENTS_ACQUIRED,8,"brewing_ingredients_acquired","ingredient_guide",false),
        e(69,"alchemy","고급 양조",ConditionType.POTION_ITEMS_BREWED,50,"potions_brewed","brewing_xp",false),
        e(70,"alchemy","물약 농축",ConditionType.POTION_ITEMS_BREWED,5,"strengthened_potions_brewed","concentration_hint",false)
    );

    private KnowledgeCatalog() {}
    private static Entry e(int n, String d, String name, ConditionType type, int target,
                           String condition, String reward, boolean unresolved) {
        return new Entry(n,d,name,type,target,condition,reward,unresolved);
    }
    public static List<Entry> entries() { return ENTRIES; }
    public static Entry byNumber(int number) {
        return number >= 1 && number <= ENTRIES.size() ? ENTRIES.get(number - 1) : null;
    }
}
