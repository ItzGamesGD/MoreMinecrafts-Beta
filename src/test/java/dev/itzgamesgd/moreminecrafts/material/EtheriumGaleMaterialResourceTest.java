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

    /** Vanilla ore_drops: extra = max(0, nextInt(level + 2) - 1); returns exact P(extra = k). */
    private static double[] vanillaExtraDistribution(int level) {
        double[] p = new double[level + 1];
        p[0] = 2.0 / (level + 2);
        for (int e = 1; e <= level; e++) p[e] = 1.0 / (level + 2);
        return p;
    }

    private static double binom(int n, int k, double p) {
        double c = 1;
        for (int i = 1; i <= k; i++) c = c * (n - k + i) / i;
        return c * Math.pow(p, k) * Math.pow(1 - p, n - k);
    }

    /** Approved rule: every vanilla extra item is removed with 80% probability, whole items only. */
    private static double[] thinned(int level) {
        double[] vanilla = vanillaExtraDistribution(level);
        double[] result = new double[level + 1];
        for (int e = 0; e <= level; e++) for (int k = 0; k <= e; k++) result[k] += vanilla[e] * binom(e, k, 0.2);
        return result;
    }

    private static JsonObject lootPoolForFortune(JsonObject loot, int level) {
        for (var element : loot.getAsJsonArray("pools")) {
            JsonObject pool = element.getAsJsonObject();
            if (!pool.has("conditions")) continue;
            JsonObject fortune = pool.getAsJsonArray("conditions").get(1).getAsJsonObject();
            JsonObject levels = fortune.getAsJsonObject("predicate").getAsJsonObject("predicates")
                    .getAsJsonArray("minecraft:enchantments").get(0).getAsJsonObject().getAsJsonObject("levels");
            if (levels.get("min").getAsInt() == level) return pool;
        }
        throw new AssertionError("no Fortune " + level + " pool");
    }

    @Test void fortuneKeepsTheGuaranteedBaseDropAndRemovesEightyPercentOfExtrasExactly() throws Exception {
        for (String ore : List.of("etherium_ore", "deepslate_etherium_ore")) {
            JsonObject loot = json("data/moreminecrafts/loot_table/blocks/" + ore + ".json");
            // Base pool: unconditional, one roll, a plain count-1 etherium item with no Fortune function or chance.
            JsonObject base = loot.getAsJsonArray("pools").get(0).getAsJsonObject();
            assertFalse(base.has("conditions"));
            assertEquals(1, base.get("rolls").getAsInt());
            JsonObject drop = base.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("children")
                    .get(1).getAsJsonObject();
            assertEquals("moreminecrafts:etherium", drop.get("name").getAsString());
            assertFalse(drop.has("weight"));
            assertFalse(drop.getAsJsonArray("functions").toString().contains("apply_bonus"));
            assertFalse(drop.getAsJsonArray("functions").toString().contains("set_count"));

            for (int level = 1; level <= 3; level++) {
                JsonObject pool = lootPoolForFortune(loot, level);
                assertEquals(1, pool.get("rolls").getAsInt());
                double total = 0;
                for (var e : pool.getAsJsonArray("entries")) total += e.getAsJsonObject().get("weight").getAsInt();
                double[] expected = thinned(level);
                double mean = 0, vanillaMean = 0;
                double[] vanilla = vanillaExtraDistribution(level);
                for (int k = 0; k < vanilla.length; k++) vanillaMean += k * vanilla[k];
                boolean[] seen = new boolean[level + 1];
                for (var element : pool.getAsJsonArray("entries")) {
                    JsonObject entry = element.getAsJsonObject();
                    int count = entry.get("type").getAsString().equals("minecraft:empty") ? 0
                            : entry.getAsJsonArray("functions").get(0).getAsJsonObject().get("count").getAsInt();
                    assertFalse(seen[count], "duplicate outcome " + count);
                    seen[count] = true;
                    assertEquals(expected[count], entry.get("weight").getAsInt() / total, 1e-12,
                            ore + " Fortune " + level + " extra=" + count);
                    mean += count * entry.get("weight").getAsInt() / total;
                }
                for (boolean b : seen) assertTrue(b);
                assertEquals(0.2 * vanillaMean, mean, 1e-12, "mean extras are exactly 20% of vanilla at Fortune " + level);
            }
        }
    }
}
