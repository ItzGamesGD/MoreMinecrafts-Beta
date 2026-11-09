package dev.itzgamesgd.moreminecrafts.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

/** Human-approved V4 additions. Older V3 pricing remains sourced from its locked price ledger. */
public final class ResearchCatalog {
    private static final Map<String, ResearchDefinition> V4;
    private static final Map<String, ResearchDefinition> STAGE22_ROOTS;
    private static final Map<String, ResearchDefinition> V3 = loadV3();
    static {
        Map<String, ResearchDefinition> roots = new LinkedHashMap<>();
        addStage22Root(roots, "resources_root", "resources", any(1,2,3,4,5,6,7,8,9,10), m("minecraft:cobblestone",16), m("minecraft:coal",16));
        addStage22Root(roots, "production_root", "production", any(11,12,13,14,15,16,17,18,19), m("minecraft:furnace",16), m("minecraft:iron_ingot",16));
        addStage22Root(roots, "living_root", "living", any(20,21,22,23,24,25,26,27,28,29,30), m("minecraft:wheat_seeds",16), m("minecraft:wheat",16));
        addStage22Root(roots, "world_root", "world", any(53,54,55,56,57,58,59,60,61,62), m("minecraft:compass",16), m("minecraft:paper",16));
        addStage22Root(roots, "alchemy_root", "alchemy", any(63,64,65,66,67,68,69,70), m("minecraft:glass_bottle",16), m("minecraft:nether_wart",16));
        STAGE22_ROOTS = Map.copyOf(roots);
        Map<String, ResearchDefinition> entries = new LinkedHashMap<>();
        add(entries, "combat_root", "combat", 80, any(31,32,33,34,35,36,37,38,39,40), Set.of(), m("minecraft:stick",16), m("minecraft:bone",16));
        add(entries, "ecology_root", "ecology", 80, any(41,42,43,44,45,46,47,48,49,50,51,52), Set.of(), m("minecraft:rotten_flesh",16), m("minecraft:bone",16));
        add(entries, "throwable_pickaxe", "resources", 900, any(9), Set.of("v3.research.01"), m("minecraft:iron_block",4), m("minecraft:redstone",64), m("minecraft:diamond",4));
        add(entries, "climbing", "resources", 650, any(8), Set.of("v3.research.03"), m("minecraft:cobblestone",64), m("minecraft:copper_ingot",16), m("minecraft:iron_ingot",16));
        add(entries, "nearby_chest_ingredients", "production", 1100, any(17), Set.of("v3.research.08"), m("minecraft:chest",8), m("minecraft:iron_block",4), m("minecraft:paper",64));
        add(entries, "hopper_filter", "production", 1050, any(18), Set.of("v3.research.08"), m("minecraft:hopper",4), m("minecraft:redstone",64), m("minecraft:iron_ingot",4));
        add(entries, "automatic_tool_switch", "production", 1100, any(14), Set.of("v3.research.10"), m("minecraft:iron_pickaxe",1), m("minecraft:iron_axe",1), m("minecraft:iron_shovel",1), m("minecraft:redstone",64));
        add(entries, "shield_counter", "combat", 850, any(36), Set.of("v3.research.30"), m("minecraft:shield",1), m("minecraft:iron_block",4));
        add(entries, "ballistic_control", "combat", 900, any(34), Set.of("v3.research.29"), m("minecraft:arrow",64), m("minecraft:string",32), m("minecraft:compass",1));
        add(entries, "bow_charge_mobility", "combat", 950, any(34), Set.of("v3.research.29"), m("minecraft:bow",2), m("minecraft:string",64));
        add(entries, "falling_axe_smash", "combat", 800, any(33), Set.of("v3.research.28"), m("minecraft:iron_axe",2), m("minecraft:iron_block",4));
        add(entries, "weapon_swap_handling", "combat", 850, any(39), Set.of("v3.research.27"), m("minecraft:iron_sword",2), m("minecraft:iron_block",2));
        add(entries, "mob_aggro_redirection", "ecology", 1150, any(42), Set.of("v3.research.37"), m("minecraft:rotten_flesh",64), m("minecraft:bone",64), m("minecraft:spider_eye",16));
        add(entries, "mob_capture", "ecology", 2800, any(51), Set.of("v3.research.46"), m("minecraft:diamond",16), m("minecraft:emerald_block",8), m("minecraft:ghast_tear",8));
        add(entries, "piglin_barter", "world", 2100, any(48), Set.of("v3.research.54"), m("minecraft:gold_block",16), m("minecraft:crying_obsidian",16), m("minecraft:emerald",8));
        add(entries, "wall_jump", "world", 900, any(62), Set.of("v3.research.55"), m("minecraft:slime_ball",32), m("minecraft:iron_boots",1));
        add(entries, "fall_roll", "world", 650, any(54), Set.of("v3.research.55"), m("minecraft:leather_boots",1), m("minecraft:feather",64));
        V4 = Map.copyOf(entries);
    }

    private ResearchCatalog() {}
    private static Set<Integer> any(Integer... ids) { return Set.of(ids); }
    private static ResearchDefinition.MaterialCost m(String id, int count) {
        return new ResearchDefinition.MaterialCost(id,count);
    }
    private static void add(Map<String, ResearchDefinition> map, String id, String discipline, int xp,
                            Set<Integer> knowledge, ResearchDefinition.MaterialCost... costs) {
        add(map, id, discipline, xp, knowledge, Set.of(), costs);
    }
    private static void add(Map<String, ResearchDefinition> map, String id, String discipline, int xp,
                            Set<Integer> knowledge, Set<String> researchPrerequisites,
                            ResearchDefinition.MaterialCost... costs) {
        Set<String> prerequisites = new java.util.HashSet<>(researchPrerequisites);
        if (!id.endsWith("_root")) prerequisites.add(discipline + "_root");
        var definition = new ResearchDefinition(id, discipline,
                "moreminecrafts.research." + id + ".title",
                "moreminecrafts.research." + id + ".description", 1,
                knowledge, prerequisites, List.of(new ResearchDefinition.LevelCost(xp, List.of(costs))));
        if (map.putIfAbsent(id, definition) != null) throw new IllegalStateException("Duplicate research " + id);
    }
    private static void addStage22Root(Map<String, ResearchDefinition> map, String id, String discipline,
            Set<Integer> knowledge, ResearchDefinition.MaterialCost... materials) {
        var definition = new ResearchDefinition(id, discipline,
                "moreminecrafts.research." + id + ".title",
                "moreminecrafts.research." + id + ".description", 1, knowledge, Set.of(),
                List.of(new ResearchDefinition.LevelCost(80, List.of(materials))), "stage22", "ROOT", true);
        if (map.putIfAbsent(id, definition) != null) throw new IllegalStateException("Duplicate research " + id);
    }
    public static List<ResearchDefinition> v4Additions() {
        return V4.values().stream().sorted(java.util.Comparator.comparing(ResearchDefinition::discipline)
                .thenComparing(ResearchDefinition::id)).toList();
    }
    public static List<ResearchDefinition> v3Catalog() {
        return V3.values().stream().sorted(java.util.Comparator.comparing(ResearchDefinition::id)).toList();
    }
    public static List<ResearchDefinition> allCatalogEntries() {
        return java.util.stream.Stream.of(V3.values().stream(), V4.values().stream(), STAGE22_ROOTS.values().stream())
                .flatMap(stream -> stream)
                .sorted(java.util.Comparator.comparing(ResearchDefinition::discipline)
                        .thenComparing(ResearchDefinition::catalogVersion)
                        .thenComparing(ResearchDefinition::id)).toList();
    }
    public static ResearchDefinition byId(String id) {
        ResearchDefinition v4 = V4.get(id);
        if (v4 != null) return v4;
        ResearchDefinition root = STAGE22_ROOTS.get(id);
        return root != null ? root : V3.get(id);
    }

    private record V3File(List<V3Node> entries) {}
    private record V3Node(String id, String discipline, String tier, int levelCap,
                          String titleKey, String descriptionKey, boolean prerequisitesDefined,
                          Set<Integer> knowledgePrerequisites, Set<String> researchPrerequisites,
                          List<V3Cost> costs) {}
    private record V3Cost(int rawXp, List<V3Material> materials) {}
    private record V3Material(String itemId, int count, int compressedUnits) {}

    private static Map<String, ResearchDefinition> loadV3() {
        var stream = ResearchCatalog.class.getResourceAsStream("/data/moreminecrafts/research/research_v3.json");
        if (stream == null) throw new ExceptionInInitializerError("Missing approved V3 research ledger");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            V3File file = new Gson().fromJson(reader, V3File.class);
            Map<String, ResearchDefinition> parsed = new LinkedHashMap<>();
            for (V3Node node : file.entries()) {
                List<ResearchDefinition.LevelCost> costs = node.costs().stream().map(cost ->
                        new ResearchDefinition.LevelCost(cost.rawXp(), cost.materials().stream().map(material ->
                                new ResearchDefinition.MaterialCost(material.itemId(), material.count(), material.compressedUnits()))
                                .toList())).toList();
                var definition = new ResearchDefinition(node.id(), node.discipline(), node.titleKey(),
                        node.descriptionKey(), node.levelCap(), node.knowledgePrerequisites(),
                        node.researchPrerequisites(), costs, "v3", node.tier(), node.prerequisitesDefined());
                if (parsed.putIfAbsent(definition.id(), definition) != null)
                    throw new IllegalStateException("Duplicate research " + definition.id());
            }
            long purchases = parsed.values().stream().mapToLong(ResearchDefinition::maximumLevel).sum();
            long xp = parsed.values().stream().flatMap(entry -> entry.costs().stream())
                    .mapToLong(ResearchDefinition.LevelCost::rawXp).sum();
            if (parsed.size() != 70 || purchases != 178 || xp != 130_360)
                throw new IllegalStateException("V3 research ledger totals are invalid");
            return Map.copyOf(parsed);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
