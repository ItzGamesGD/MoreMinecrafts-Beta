package dev.itzgamesgd.moreminecrafts.material;

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

/** T2 §18.1/§18.3 data checks for the etherium chain and the Gale Shard. Resource-level only; not an in-game craft. */
class EtheriumGaleMaterialResourceTest {
    private static JsonObject json(String path) throws Exception {
        try (InputStream stream = EtheriumGaleMaterialResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<String> pattern(JsonObject recipe) {
        List<String> rows = new ArrayList<>();
        recipe.getAsJsonArray("pattern").forEach(e -> rows.add(e.getAsString()));
        return rows;
    }

    private static Map<String, Integer> counts(JsonObject recipe) {
        Map<String, Integer> result = new TreeMap<>();
        JsonObject key = recipe.getAsJsonObject("key");
        for (String row : pattern(recipe)) for (char c : row.toCharArray())
            if (c != ' ') result.merge(key.get(String.valueOf(c)).getAsString(), 1, Integer::sum);
        return result;
    }

    @Test void etheriumIngotUsesExactlyTheSixApprovedMaterialsInTheApprovedLayout() throws Exception {
        JsonObject recipe = json("data/moreminecrafts/recipe/etherium_ingot.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(List.of(" D ", "TSM", "GN "), pattern(recipe));
        assertEquals(Map.of("minecraft:diamond", 1, "minecraft:gold_ingot", 1, "minecraft:netherite_ingot", 1,
                "moreminecrafts:etherium_shard", 1, "moreminecrafts:mythril", 1, "moreminecrafts:titanium_ingot", 1),
                counts(recipe));
        assertEquals("moreminecrafts:etherium_ingot", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test void galeShardUsesFourWindChargesTwoDiamondsAndOneEtheriumIngot() throws Exception {
        JsonObject recipe = json("data/moreminecrafts/recipe/gale_shard.json");
        assertEquals(List.of(" W ", "WEW", "DWD"), pattern(recipe));
        assertEquals(Map.of("minecraft:diamond", 2, "minecraft:wind_charge", 4, "moreminecrafts:etherium_ingot", 1),
                counts(recipe));
        assertEquals("moreminecrafts:gale_shard", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test void shardComesOnlyFromSmeltingTheRawEtheriumDrop() throws Exception {
        for (String kind : List.of("smelting", "blasting")) {
            JsonObject recipe = json("data/moreminecrafts/recipe/etherium_shard_from_" + kind + ".json");
            assertEquals("minecraft:" + kind, recipe.get("type").getAsString());
            assertEquals("moreminecrafts:etherium", recipe.get("ingredient").getAsString());
            assertEquals("moreminecrafts:etherium_shard", recipe.getAsJsonObject("result").get("id").getAsString());
        }
    }

    @Test void oreVeinsAreSingleBlockAtAQuarterOfNominalDiamondVolume() throws Exception {
        assertEquals(1, json("data/moreminecrafts/worldgen/feature/ore_etherium.json").get("size").getAsInt());
        int count = json("data/moreminecrafts/worldgen/placed_feature/ore_etherium.json")
                .getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt();
        assertEquals(MaterialProgression.ETHERIUM_ATTEMPTS_PER_CHUNK, count);
        assertEquals(Math.floor(MaterialProgression.VANILLA_DIAMOND_NOMINAL_ORES_PER_CHUNK / 4), count);
    }

    @Test void fortuneKeepsTheGuaranteedBaseDropAndOnlyThinsTheExtras() throws Exception {
        for (String ore : List.of("etherium_ore", "deepslate_etherium_ore")) {
            JsonObject loot = json("data/moreminecrafts/loot_table/blocks/" + ore + ".json");
            JsonObject drop = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                    .getAsJsonObject().getAsJsonArray("children").get(1).getAsJsonObject();
            assertEquals("moreminecrafts:etherium", drop.get("name").getAsString());
            JsonObject bonus = drop.getAsJsonArray("functions").get(0).getAsJsonObject();
            assertEquals("minecraft:binomial_with_bonus_count", bonus.get("formula").getAsString());
            assertEquals(0, bonus.getAsJsonObject("parameters").get("extra").getAsInt());
            // extras per Fortune level are Binomial(level, p); vanilla ore_drops averages 1/3, 3/4, 6/5.
            double p = bonus.getAsJsonObject("parameters").get("probability").getAsDouble();
            double[] vanillaExtra = {1 / 3.0, 3 / 4.0, 6 / 5.0};
            for (int level = 1; level <= 3; level++) {
                double reduction = 1 - level * p / vanillaExtra[level - 1];
                assertTrue(reduction > 0.75 && reduction < 0.85, "Fortune " + level + " reduction " + reduction);
            }
            assertFalse(drop.getAsJsonArray("functions").toString().contains("set_count"));
        }
    }
}
