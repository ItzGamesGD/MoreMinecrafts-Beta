package dev.itzgamesgd.moreminecrafts.enchantment;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnchantmentResourcesTest {
    private static final List<String> IDS = List.of("multi_shot", "crossbow_barrage", "vein_mining",
            "auto_replant", "auto_smelt", "lumberjack", "elytra_launch", "explosive_mace",
            "feather_boots", "jet_boots");

    private JsonObject resource(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test void allEnchantmentsHaveLifecycleMetadata() {
        for (String id : IDS) {
            JsonObject json = resource("data/moreminecrafts/enchantment/" + id + ".json");
            assertTrue(json.has("supported_items"), id);
            assertTrue(json.has("weight"), id);
            assertTrue(json.has("anvil_cost"), id);
            assertTrue(json.has("effects"), id);
        }
    }

    @Test void nativeEffectsAndAcquisitionAreWired() {
        assertTrue(resource("data/moreminecrafts/enchantment/multi_shot.json").getAsJsonObject("effects")
                .has("minecraft:projectile_count"));
        assertTrue(resource("data/moreminecrafts/enchantment/feather_boots.json").getAsJsonObject("effects")
                .has("minecraft:damage_immunity"));
        assertTrue(resource("data/minecraft/tags/enchantment/smelts_loot.json").toString().contains("auto_smelt"));
        String acquisition = resource("data/minecraft/tags/enchantment/non_treasure.json").toString();
        IDS.forEach(id -> assertTrue(acquisition.contains(id), id));
    }
}
