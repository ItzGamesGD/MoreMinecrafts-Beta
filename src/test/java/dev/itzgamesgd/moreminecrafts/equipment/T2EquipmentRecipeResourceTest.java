package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** T2 section 18.2/18.3/18.5 resource checks for the implemented equipment recipes and their core-gated unlocks. */
class T2EquipmentRecipeResourceTest {
    private static JsonObject json(String path) throws Exception {
        try (InputStream stream = T2EquipmentRecipeResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<String> rows(JsonObject recipe) {
        List<String> rows = new ArrayList<>();
        recipe.getAsJsonArray("pattern").forEach(e -> rows.add(e.getAsString()));
        return rows;
    }

    private static Map<String, Integer> counts(JsonObject recipe) {
        Map<String, Integer> counts = new TreeMap<>();
        JsonObject key = recipe.getAsJsonObject("key");
        for (String row : rows(recipe)) for (char c : row.toCharArray())
            if (c != ' ') counts.merge(key.get(String.valueOf(c)).getAsString(), 1, Integer::sum);
        return counts;
    }

    private record Expected(String name, List<String> pattern, Map<String, Integer> ingredients, String core) {}

    private static final List<Expected> EXPECTED = List.of(
            new Expected("flame_sword", List.of("G H", " A ", "E  "), Map.of("minecraft:golden_sword", 1,
                    "moreminecrafts:burning_heart", 1, "moreminecrafts:adamantium_ingot", 1, "moreminecrafts:etherium_ingot", 1),
                    "burning_heart"),
            new Expected("flowing_water_sword", List.of("SPP", "PCH", "PPE"), Map.of("minecraft:diamond_sword", 1,
                    "minecraft:prismarine_shard", 5, "minecraft:heart_of_the_sea", 1, "moreminecrafts:heart_of_currents", 1,
                    "moreminecrafts:etherium_ingot", 1), "heart_of_currents"),
            new Expected("wind_sword", List.of("FTF", "BSB", "BEB"), Map.of("moreminecrafts:sky_feather", 2,
                    "moreminecrafts:eye_of_typhoon", 1, "minecraft:breeze_rod", 4, "minecraft:iron_sword", 1,
                    "moreminecrafts:etherium_ingot", 1), "eye_of_typhoon"),
            new Expected("wind_archers_bow", List.of("FGF", "BWB", "BEB"), Map.of("moreminecrafts:sky_feather", 2,
                    "moreminecrafts:gale_shard", 1, "minecraft:breeze_rod", 4, "minecraft:bow", 1,
                    "moreminecrafts:etherium_ingot", 1), "gale_shard"),
            new Expected("earth_mace", List.of("DHD", " A ", " E "), Map.of("minecraft:diamond_block", 2,
                    "minecraft:heavy_core", 1, "moreminecrafts:ancient_heart", 1, "moreminecrafts:etherium_ingot", 1),
                    "ancient_heart"),
            new Expected("lightning_sword", List.of(" S ", "GTN", " E "), Map.of("minecraft:diamond_sword", 1,
                    "minecraft:gold_block", 1, "moreminecrafts:heart_of_thunder", 1, "minecraft:netherite_ingot", 1,
                    "moreminecrafts:etherium_ingot", 1), "heart_of_thunder"));

    @Test void implementedRecipesMatchSection18Exactly() throws Exception {
        for (Expected e : EXPECTED) {
            JsonObject recipe = json("data/moreminecrafts/recipe/" + e.name() + ".json");
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), e.name());
            assertEquals(e.pattern(), rows(recipe), e.name());
            assertEquals(new TreeMap<>(e.ingredients()), counts(recipe), e.name());
            assertEquals("moreminecrafts:" + e.name(), recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
            assertFalse(recipe.getAsJsonObject("result").has("components"), "result must be a fresh item: " + e.name());
        }
    }

    @Test void eachCoreUnlocksOnlyItsOwnRecipeThroughVanillaAdvancements() throws Exception {
        for (Expected e : EXPECTED) {
            JsonObject adv = json("data/moreminecrafts/advancement/recipes/equipment/" + e.name() + ".json");
            assertEquals("minecraft:recipes/root", adv.get("parent").getAsString());
            assertEquals(List.of("moreminecrafts:" + e.name()),
                    adv.getAsJsonObject("rewards").getAsJsonArray("recipes").asList().stream().map(x -> x.getAsString()).toList());
            JsonObject trigger = adv.getAsJsonObject("criteria").getAsJsonObject("has_" + e.core());
            assertEquals("minecraft:inventory_changed", trigger.get("trigger").getAsString());
            assertEquals("moreminecrafts:" + e.core(), trigger.getAsJsonObject("conditions").getAsJsonArray("items")
                    .get(0).getAsJsonObject().get("items").getAsString());
            assertFalse(adv.has("display"), "recipe unlocks are silent");
        }
    }

    @Test void skyFeatherHasNoTemporaryAcquisitionRoute() throws Exception {
        // BLOCKED_DEPENDENCY: the Sky Island vault (15%) and flying mobs do not exist; no stand-in drop may be added.
        try (var files = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/resources/data"))) {
            for (java.nio.file.Path file : files.filter(f -> f.toString().contains("loot_table") && f.toString().endsWith(".json")).toList())
                assertFalse(java.nio.file.Files.readString(file).contains("sky_feather"), file.toString());
        }
        assertFalse(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java")).contains("SKY_FEATHER"));
    }
}
