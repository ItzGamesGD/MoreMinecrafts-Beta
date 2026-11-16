package dev.itzgamesgd.moreminecrafts.accessory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Resource checks for the Relic Catalyst shaped recipe and the T1 accessory item tag. */
class RelicCatalystRecipeResourceTest {
    private static JsonObject json(String path) throws Exception {
        try (InputStream stream = RelicCatalystRecipeResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void recipeIsTheApprovedThreeByThreeShapedLayout() throws Exception {
        JsonObject recipe = json("data/moreminecrafts/recipe/relic_catalyst.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertFalse(recipe.has("ingredients"), "a shaped recipe defines a fixed layout, not an ingredient multiset");

        List<String> pattern = new ArrayList<>();
        recipe.getAsJsonArray("pattern").forEach(e -> pattern.add(e.getAsString()));
        assertEquals(List.of("SMS", "LEL", "SMS"), pattern);

        Map<String, String> key = new TreeMap<>();
        recipe.getAsJsonObject("key").entrySet().forEach(e -> key.put(e.getKey(), e.getValue().getAsString()));
        assertEquals(new TreeMap<>(Map.of(
                "S", "moreminecrafts:enhancement_stone",
                "M", "moreminecrafts:mythril",
                "L", "minecraft:lapis_lazuli",
                "E", "moreminecrafts:etherium_shard")), key);
    }

    @Test void resultIsOneFreshCatalystWithoutInheritedComponents() throws Exception {
        JsonObject result = json("data/moreminecrafts/recipe/relic_catalyst.json").getAsJsonObject("result");
        assertEquals("moreminecrafts:relic_catalyst", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
        assertFalse(result.has("components"), "the catalyst result carries no components");
    }

    @Test void t1TagMatchesExactlyTheRegisteredT1AccessoryDefinitions() throws Exception {
        JsonObject tag = json("data/moreminecrafts/tags/item/t1_accessories.json");
        List<String> values = new ArrayList<>();
        tag.getAsJsonArray("values").forEach(e -> values.add(e.getAsString()));

        Set<String> registeredT1 = AccessoryDefinition.ALL.stream()
                .filter(AccessoryDefinition::isT1)
                .map(d -> "moreminecrafts:" + d.registryId())
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(registeredT1.isEmpty());
        assertEquals(registeredT1.size(), values.size(), "tag must not contain duplicates or missing entries");
        assertEquals(registeredT1, Set.copyOf(values));
        assertTrue(values.stream().noneMatch(v -> v.contains("moreminecrafts:t2")), "T2 accessories are never catalyst inputs");
    }

    @Test void t1TagEntriesAllMapToRegisteredT1Accessories() throws Exception {
        JsonObject tag = json("data/moreminecrafts/tags/item/t1_accessories.json");
        tag.getAsJsonArray("values").forEach(e -> assertTrue(
                AccessoryDefinition.ALL.stream().anyMatch(d -> d.isT1()
                        && e.getAsString().equals("moreminecrafts:" + d.registryId())),
                e.getAsString() + " must map to a registered T1 accessory"));
    }
}
