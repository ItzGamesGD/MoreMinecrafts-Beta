package dev.itzgamesgd.moreminecrafts.accessory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 2026-11-13 T2 record §2.1: the 13 shaped 3x3 recipes, written out slot by slot (rows top to bottom), plus the §1 Etherium
 * rule (ingot for blood covenant / ring of time / heart of gravity, shard for the others, none for the feather charm).
 */
class T2AccessoryRecipeResourceTest {
    private static final String M = "moreminecrafts:";
    /** accessory id -> nine ingredients in slot order 1..9. */
    private static final Map<String, List<String>> DOC = new LinkedHashMap<>();
    static {
        DOC.put("star_covenant", List.of("minecraft:diamond", M + "void_star_shard", "minecraft:diamond",
                M + "blazing_relic_core", M + "etherium_shard", M + "blazing_relic_core",
                "minecraft:gold_ingot", M + "mythril", "minecraft:gold_ingot"));
        DOC.put("blood_covenant", List.of("minecraft:diamond", M + "gladiator_blood_crest", "minecraft:diamond",
                M + "blazing_relic_core", M + "etherium_ingot", M + "blazing_relic_core",
                M + "adamantium_ingot", M + "mythril", M + "adamantium_ingot"));
        DOC.put("phantom_mirror", List.of("minecraft:diamond", M + "shimmering_soul_glass", "minecraft:diamond",
                M + "earthen_relic_core", M + "etherium_shard", M + "earthen_relic_core",
                "minecraft:lapis_lazuli", M + "mythril", "minecraft:lapis_lazuli"));
        DOC.put("ring_of_time", List.of("minecraft:diamond", M + "ancient_clock_spring", "minecraft:diamond",
                M + "glacial_relic_core", M + "etherium_ingot", M + "glacial_relic_core",
                "minecraft:echo_shard", M + "glacial_relic_core", M + "mythril"));
        DOC.put("heart_of_gravity", List.of("minecraft:diamond", M + "void_gravity_core", "minecraft:diamond",
                M + "abyssal_relic_core", M + "etherium_ingot", M + "abyssal_relic_core",
                "minecraft:obsidian", M + "abyssal_relic_core", M + "mythril"));
        DOC.put("soul_chain", List.of("minecraft:diamond", M + "skeleton_king_chain", "minecraft:diamond",
                M + "earthen_relic_core", M + "etherium_shard", M + "earthen_relic_core",
                M + "titanium_ingot", M + "mythril", M + "titanium_ingot"));
        DOC.put("ember_ring", List.of("minecraft:diamond", M + "lava_heartstone", "minecraft:diamond",
                M + "blazing_relic_core", M + "etherium_shard", M + "blazing_relic_core",
                "minecraft:gold_ingot", M + "mythril", "minecraft:gold_ingot"));
        DOC.put("ring_of_life", List.of("minecraft:diamond", "minecraft:totem_of_undying", "minecraft:diamond",
                M + "blazing_relic_core", M + "etherium_shard", M + "blazing_relic_core",
                "minecraft:golden_apple", M + "mythril", "minecraft:golden_apple"));
        DOC.put("seed_of_life", List.of("minecraft:diamond", M + "ancient_world_tree_sap", "minecraft:diamond",
                M + "abyssal_relic_core", M + "etherium_shard", M + "abyssal_relic_core",
                "minecraft:gold_ingot", M + "mythril", "minecraft:gold_ingot"));
        DOC.put("snow_charm", List.of("minecraft:diamond", M + "permafrost_crystal", "minecraft:diamond",
                M + "glacial_relic_core", M + "etherium_shard", M + "glacial_relic_core",
                M + "titanium_ingot", M + "mythril", M + "titanium_ingot"));
        DOC.put("deep_sea_pearl", List.of("minecraft:diamond", "minecraft:heart_of_the_sea", "minecraft:diamond",
                M + "tidal_relic_core", M + "etherium_shard", M + "tidal_relic_core",
                M + "abyssal_pearl_core", M + "mythril", M + "abyssal_pearl_core"));
        DOC.put("whisper_of_the_sea", List.of("minecraft:diamond", M + "mermaid_scale", "minecraft:diamond",
                M + "tidal_relic_core", M + "etherium_shard", M + "tidal_relic_core",
                "minecraft:lapis_lazuli", M + "mythril", "minecraft:lapis_lazuli"));
        DOC.put("feather_charm", List.of(M + "sky_feather", M + "gale_shard", M + "sky_feather",
                M + "glacial_relic_core", "minecraft:diamond", M + "glacial_relic_core",
                "minecraft:diamond", M + "mythril", "minecraft:diamond"));
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = T2AccessoryRecipeResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<String> slots(JsonObject recipe) {
        List<String> rows = new ArrayList<>();
        recipe.getAsJsonArray("pattern").forEach(e -> rows.add(e.getAsString()));
        assertEquals(3, rows.size());
        JsonObject key = recipe.getAsJsonObject("key");
        List<String> out = new ArrayList<>();
        for (String row : rows) {
            assertEquals(3, row.length());
            for (char c : row.toCharArray()) {
                assertNotEquals(' ', c, "no empty slots in any T2 accessory recipe");
                out.add(key.get(String.valueOf(c)).getAsString());
            }
        }
        return out;
    }

    @Test void thirteenRecipesMatchTheDocumentSlotBySlot() throws Exception {
        assertEquals(13, DOC.size());
        for (var entry : DOC.entrySet()) {
            JsonObject recipe = json("data/moreminecrafts/recipe/" + entry.getKey() + ".json");
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), entry.getKey());
            assertEquals(entry.getValue(), slots(recipe), entry.getKey());
            JsonObject result = recipe.getAsJsonObject("result");
            assertEquals(M + entry.getKey(), result.get("id").getAsString());
            assertEquals(1, result.get("count").getAsInt());
        }
    }

    @Test void everyRecipeResultIsARegisteredAccessoryAndAllThirteenT2AccessoriesHaveOne() {
        var t2 = AccessoryDefinition.ALL.stream().filter(d -> !d.isT1()).map(AccessoryDefinition::registryId).sorted().toList();
        assertEquals(13, t2.size());
        assertEquals(t2, DOC.keySet().stream().sorted().toList());
    }

    @Test void commonFloorAndEtheriumRuleHold() {
        for (var entry : DOC.entrySet()) {
            long mythril = entry.getValue().stream().filter((M + "mythril")::equals).count();
            long diamonds = entry.getValue().stream().filter("minecraft:diamond"::equals).count();
            assertTrue(mythril >= 1, entry.getKey() + " mythril >= 1");
            assertTrue(diamonds >= 2, entry.getKey() + " diamonds >= 2");
            boolean ingot = entry.getValue().contains(M + "etherium_ingot");
            boolean shard = entry.getValue().contains(M + "etherium_shard");
            if (List.of("blood_covenant", "ring_of_time", "heart_of_gravity").contains(entry.getKey())) assertTrue(ingot && !shard, entry.getKey());
            else if (entry.getKey().equals("feather_charm")) assertTrue(!ingot && !shard, "the document's feather charm layout has no etherium");
            else assertTrue(shard && !ingot, entry.getKey());
        }
    }

    @Test void coreCountsMatchTheDocument() {
        Map<String, Integer> expected = Map.ofEntries(Map.entry("star_covenant", 2), Map.entry("blood_covenant", 2), Map.entry("phantom_mirror", 2),
                Map.entry("ring_of_time", 3), Map.entry("heart_of_gravity", 3), Map.entry("soul_chain", 2), Map.entry("ember_ring", 2),
                Map.entry("ring_of_life", 2), Map.entry("seed_of_life", 2), Map.entry("snow_charm", 2), Map.entry("deep_sea_pearl", 2),
                Map.entry("whisper_of_the_sea", 2), Map.entry("feather_charm", 2));
        for (var entry : DOC.entrySet())
            assertEquals(expected.get(entry.getKey()), (int) entry.getValue().stream().filter(i -> i.endsWith("_relic_core")).count(), entry.getKey());
    }

    private static final List<String> NEW_ITEMS = List.of("skeleton_king_chain", "gladiator_blood_crest", "ancient_clock_spring",
            "ancient_world_tree_sap", "lava_heartstone", "permafrost_crystal", "abyssal_pearl_core", "mermaid_scale", "void_star_shard",
            "void_gravity_core", "shimmering_soul_glass", "feather_charm");

    @Test void newItemsHaveDefinitionModelTexturePlaceholderMarkerAndBothNames() throws Exception {
        JsonObject ko = json("assets/moreminecrafts/lang/ko_kr.json");
        JsonObject en = json("assets/moreminecrafts/lang/en_us.json");
        ClassLoader cl = T2AccessoryRecipeResourceTest.class.getClassLoader();
        String marker;
        try (InputStream in = cl.getResourceAsStream("assets/moreminecrafts/textures/item/PLACEHOLDER_T2_TEXTURES.txt")) {
            assertNotNull(in, "placeholder textures must be flagged");
            marker = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String id : NEW_ITEMS) {
            assertNotNull(cl.getResource("assets/moreminecrafts/items/" + id + ".json"), id + " item definition");
            assertNotNull(cl.getResource("assets/moreminecrafts/models/item/" + id + ".json"), id + " model");
            assertNotNull(cl.getResource("assets/moreminecrafts/textures/item/" + id + ".png"), id + " texture");
            assertTrue(marker.contains(id + ".png"), id + " flagged as placeholder");
            assertTrue(ko.has("item.moreminecrafts." + id) && en.has("item.moreminecrafts." + id), id + " names");
        }
        assertEquals("깃털 부적", ko.get("item.moreminecrafts.feather_charm").getAsString());
        assertEquals(Map.ofEntries(
                Map.entry("skeleton_king_chain", "스켈레톤 킹의 쇠사슬"),
                Map.entry("gladiator_blood_crest", "검투사의 핏빛 문장"),
                Map.entry("ancient_clock_spring", "고대 시계 태엽"),
                Map.entry("ancient_world_tree_sap", "고대 세계수의 수액"),
                Map.entry("lava_heartstone", "용암 심장석"),
                Map.entry("permafrost_crystal", "만년설의 결정"),
                Map.entry("abyssal_pearl_core", "심해의 진주핵"),
                Map.entry("mermaid_scale", "인어의 비늘"),
                Map.entry("void_star_shard", "공허의 별조각"),
                Map.entry("void_gravity_core", "공허의 중력핵"),
                Map.entry("shimmering_soul_glass", "아른거리는 영혼 유리")),
                NEW_ITEMS.stream().filter(i -> !i.equals("feather_charm")).collect(java.util.stream.Collectors.toMap(i -> i, i -> ko.get("item.moreminecrafts." + i).getAsString())));
    }
}
