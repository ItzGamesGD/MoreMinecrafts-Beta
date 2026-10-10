package dev.itzgamesgd.moreminecrafts.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Human-approved V4 additions. Older V3 pricing remains sourced from its locked price ledger. */
public final class ResearchCatalog {
    private static final Map<String, ResearchDefinition> V4;
    static {
        Map<String, ResearchDefinition> entries = new LinkedHashMap<>();
        add(entries, "combat_root", "combat", 80, any(31,32,33,34,35,36,37,38,39,40), m("minecraft:stick",16), m("minecraft:bone",16));
        add(entries, "ecology_root", "ecology", 80, any(41,42,43,44,45,46,47,48,49,50,51,52), m("minecraft:rotten_flesh",16), m("minecraft:bone",16));
        add(entries, "throwable_pickaxe", "resources", 900, any(9), m("minecraft:iron_block",4), m("minecraft:redstone",64), m("minecraft:diamond",4));
        add(entries, "climbing", "resources", 650, any(8), m("minecraft:cobblestone",64), m("minecraft:copper_ingot",16), m("minecraft:iron_ingot",16));
        add(entries, "nearby_chest_ingredients", "production", 1100, any(17), m("minecraft:chest",8), m("minecraft:iron_block",4), m("minecraft:paper",64));
        add(entries, "hopper_filter", "production", 1050, any(18), m("minecraft:hopper",4), m("minecraft:redstone",64), m("minecraft:iron_ingot",4));
        add(entries, "automatic_tool_switch", "production", 1100, any(14), m("minecraft:iron_pickaxe",1), m("minecraft:iron_axe",1), m("minecraft:iron_shovel",1), m("minecraft:redstone",64));
        add(entries, "shield_counter", "combat", 850, any(36), m("minecraft:shield",1), m("minecraft:iron_block",4));
        add(entries, "ballistic_control", "combat", 900, any(34), m("minecraft:arrow",64), m("minecraft:string",32), m("minecraft:compass",1));
        add(entries, "bow_charge_mobility", "combat", 950, any(34), m("minecraft:bow",2), m("minecraft:string",64));
        add(entries, "falling_axe_smash", "combat", 800, any(33), m("minecraft:iron_axe",2), m("minecraft:iron_block",4));
        add(entries, "weapon_swap_handling", "combat", 850, any(39), m("minecraft:iron_sword",2), m("minecraft:iron_block",2));
        add(entries, "mob_aggro_redirection", "ecology", 1150, any(42), m("minecraft:rotten_flesh",64), m("minecraft:bone",64), m("minecraft:spider_eye",16));
        add(entries, "mob_capture", "ecology", 2800, any(51), m("minecraft:diamond",16), m("minecraft:emerald_block",8), m("minecraft:ghast_tear",8));
        add(entries, "piglin_barter", "world", 2100, any(48), m("minecraft:gold_block",16), m("minecraft:crying_obsidian",16), m("minecraft:emerald",8));
        add(entries, "wall_jump", "world", 900, any(62), m("minecraft:slime_ball",32), m("minecraft:iron_boots",1));
        add(entries, "fall_roll", "world", 650, any(54), m("minecraft:leather_boots",1), m("minecraft:feather",64));
        V4 = Map.copyOf(entries);
    }

    private ResearchCatalog() {}
    private static Set<Integer> any(Integer... ids) { return Set.of(ids); }
    private static ResearchDefinition.MaterialCost m(String id, int count) {
        return new ResearchDefinition.MaterialCost(id,count);
    }
    private static void add(Map<String, ResearchDefinition> map, String id, String discipline, int xp,
                            Set<Integer> knowledge, ResearchDefinition.MaterialCost... costs) {
        var definition = new ResearchDefinition(id, discipline,
                "moreminecrafts.research." + id + ".title",
                "moreminecrafts.research." + id + ".description", 1,
                knowledge, Set.of(), List.of(new ResearchDefinition.LevelCost(xp, List.of(costs))));
        if (map.putIfAbsent(id, definition) != null) throw new IllegalStateException("Duplicate research " + id);
    }
    public static List<ResearchDefinition> v4Additions() {
        return V4.values().stream().sorted(java.util.Comparator.comparing(ResearchDefinition::discipline)
                .thenComparing(ResearchDefinition::id)).toList();
    }
    public static ResearchDefinition byId(String id) { return V4.get(id); }
}
