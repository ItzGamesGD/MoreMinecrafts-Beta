package dev.itzgamesgd.moreminecrafts.enchantment;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
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

    @Test void nativeEffectsAndAcquisitionAreWired() throws IOException {
        assertTrue(resource("data/moreminecrafts/enchantment/multi_shot.json").getAsJsonObject("effects")
                .has("minecraft:projectile_count"));
        assertTrue(resource("data/moreminecrafts/enchantment/feather_boots.json").getAsJsonObject("effects")
                .has("minecraft:damage_immunity"));
        var tagResources = getClass().getClassLoader()
                .getResources("data/minecraft/tags/enchantment/smelts_loot.json");
        while (tagResources.hasMoreElements()) {
            try (var stream = tagResources.nextElement().openStream()) {
                String tag = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertFalse(tag.contains("moreminecrafts:auto_smelt"),
                        "MoreMinecrafts must not contribute Auto Smelt to minecraft:smelts_loot");
            }
        }
        String acquisition = resource("data/minecraft/tags/enchantment/non_treasure.json").toString();
        IDS.forEach(id -> assertTrue(acquisition.contains(id), id));
        for (String inherited : List.of("tradeable", "on_random_loot", "in_enchanting_table",
                "on_mob_spawn_equipment", "on_traded_equipment")) {
            String tag = resource("data/minecraft/tags/enchantment/" + inherited + ".json").toString();
            assertTrue(tag.contains("#minecraft:non_treasure"), inherited);
        }
        assertTrue(resource("data/moreminecrafts/enchantment/explosive_mace.json")
                .getAsJsonObject("effects").isEmpty(),
                "Explosive Mace is runtime-gated by MaceItem.canSmashAttack, not a fall-distance predicate");
    }

    @Test void koreanEnchantmentNamesAreLocalized() {
        JsonObject ko = resource("assets/moreminecrafts/lang/ko_kr.json");
        assertEquals("다중 사격", ko.get("enchantment.moreminecrafts.multi_shot").getAsString());
        assertEquals("쇠뇌 연발", ko.get("enchantment.moreminecrafts.crossbow_barrage").getAsString());
        assertEquals("광맥 채굴", ko.get("enchantment.moreminecrafts.vein_mining").getAsString());
        assertEquals("자동 심기", ko.get("enchantment.moreminecrafts.auto_replant").getAsString());
        assertEquals("자동 제련", ko.get("enchantment.moreminecrafts.auto_smelt").getAsString());
        assertEquals("나무꾼", ko.get("enchantment.moreminecrafts.lumberjack").getAsString());
        assertEquals("겉날개 발사", ko.get("enchantment.moreminecrafts.elytra_launch").getAsString());
        assertEquals("폭발 철퇴", ko.get("enchantment.moreminecrafts.explosive_mace").getAsString());
        assertEquals("깃털 부츠", ko.get("enchantment.moreminecrafts.feather_boots").getAsString());
        assertEquals("제트 부츠", ko.get("enchantment.moreminecrafts.jet_boots").getAsString());
    }
}
