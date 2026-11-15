package dev.itzgamesgd.moreminecrafts.mob.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** SKY-MOB P1 data/pure-rule checks (Issue #230 Q01-Q87, D1-D6). Runtime behaviour is covered by SkyNormalMobGameTests. */
class SkyNormalMobsDefinitionTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final List<String> IDS =
            List.of("cloud_wraith", "skyguard_archer", "crystal_bug", "cloud_slime", "fallen_sentinel");

    private static JsonObject json(String relative) throws Exception {
        return JsonParser.parseString(Files.readString(RES.resolve(relative))).getAsJsonObject();
    }

    private static Set<String> tag(String vanillaTag) throws Exception {
        JsonArray values = json("data/minecraft/tags/entity_type/" + vanillaTag + ".json").getAsJsonArray("values");
        Set<String> out = new java.util.HashSet<>();
        for (JsonElement e : values) out.add(e.getAsString());
        return out;
    }

    private static Set<String> sky(Set<String> tag) {
        Set<String> out = new java.util.HashSet<>();
        for (String id : IDS) if (tag.contains("moreminecrafts:" + id)) out.add(id);
        return out;
    }

    @Test
    void cloudSlimeHealthMatchesQ16AndKeepsVanillaForUnapprovedSizes() {
        assertEquals(4, SkyMobTraits.cloudSlimeHealth(1));
        assertEquals(12, SkyMobTraits.cloudSlimeHealth(2));
        assertEquals(32, SkyMobTraits.cloudSlimeHealth(4));
        assertEquals(9, SkyMobTraits.cloudSlimeHealth(3), "sizes outside Q16 keep vanilla size^2");
    }

    @Test
    void sentinelWeaknessChanceIsFortyPercentWithSeededRandom() {
        RandomSource random = RandomSource.create(230_1106L);
        int trials = 200_000, hits = 0;
        for (int i = 0; i < trials; i++) if (SkyMobTraits.rollWeakness(random)) hits++;
        double rate = hits / (double) trials;
        assertEquals(0.40, rate, 0.005, "observed weakness rate " + rate);
        assertEquals(0.40F, SkyMobTraits.SENTINEL_WEAKNESS_CHANCE);
        assertEquals(60, SkyMobTraits.SENTINEL_WEAKNESS_TICKS, "3 seconds");
        assertEquals(1.4D, SkyMobTraits.ARCHER_ARROW_DAMAGE_MULTIPLIER);
        assertEquals(2.5D, SkyMobTraits.CRYSTAL_BUG_SCALE);
        assertEquals(1.5D, SkyMobTraits.CLOUD_SLIME_ATTACK_MULTIPLIER);
    }

    @Test
    void lifeformClassificationMatchesQ51ToQ54() throws Exception {
        assertEquals(Set.of("fallen_sentinel"), sky(tag("undead")), "Q51: only the sentinel is undead");
        assertEquals(Set.of("fallen_sentinel"), sky(tag("inverted_healing_and_harm")), "Q52");
        assertEquals(Set.of("fallen_sentinel"), sky(tag("sensitive_to_smite")), "Q53");
        assertEquals(Set.of("crystal_bug"), sky(tag("arthropod")), "Q54: only the crystal bug is an arthropod");
        assertEquals(Set.of("crystal_bug"), sky(tag("sensitive_to_bane_of_arthropods")), "Q54");
    }

    @Test
    void immunitiesAreDataOrTypeDrivenAndNotTagInheritedBeyondTheApprovedSet() throws Exception {
        assertEquals(Set.copyOf(IDS), sky(tag("fall_damage_immune")), "Q37: all five ignore fall damage");
        for (String neverTag : List.of("burn_in_daylight", "zombies", "skeletons", "freeze_immune_entity_types",
                "illager", "raiders")) {
            assertEquals(Set.of(), sky(tag(neverTag)), neverTag + " must not list Sky Realm mobs (Q36 / V5 / D4)");
        }
        JsonObject marker = json("data/moreminecrafts/tags/entity_type/sky_normal_mobs.json");
        assertEquals(5, marker.getAsJsonArray("values").size());
    }

    @Test
    void lootTablesMatchQ07Q14Q18Q22Q24Q85() throws Exception {
        for (String id : IDS) {
            JsonObject table = json("data/moreminecrafts/loot_table/entities/" + id + ".json");
            assertEquals("moreminecrafts:entities/" + id, table.get("random_sequence").getAsString());
            assertFalse(table.toString().contains("\"conditions\"") || table.toString().contains("\"functions\""),
                    id + " uses obsolete 26.3 loot field names");
        }
        assertEquals("minecraft:entities/skeleton", json("data/moreminecrafts/loot_table/entities/skyguard_archer.json")
                .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().get("value").getAsString(), "Q24");
        assertEquals("minecraft:entities/zombie", json("data/moreminecrafts/loot_table/entities/fallen_sentinel.json")
                .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().get("value").getAsString(), "Q22");

        String wraith = Files.readString(RES.resolve("data/moreminecrafts/loot_table/entities/cloud_wraith.json"));
        assertTrue(wraith.contains("minecraft:glowstone_dust") && wraith.contains("\"max\": 2")
                && wraith.contains("minecraft:looting"), "Q07 + Q64");

        JsonObject bug = json("data/moreminecrafts/loot_table/entities/crystal_bug.json");
        String bugText = bug.toString();
        assertTrue(bugText.contains("minecraft:amethyst_shard") && bugText.contains("minecraft:looting"), "Q14");
        JsonObject emeraldPool = bug.getAsJsonArray("pools").get(1).getAsJsonObject();
        JsonObject chance = emeraldPool.getAsJsonObject("condition");
        assertEquals("minecraft:random_chance_with_enchanted_bonus", chance.get("type").getAsString());
        assertEquals(0.05, chance.get("unenchanted_chance").getAsDouble(), 1e-9, "Q85 base 5%");
        assertTrue(emeraldPool.toString().contains("minecraft:emerald"));

        JsonObject slimePool = json("data/moreminecrafts/loot_table/entities/cloud_slime.json")
                .getAsJsonArray("pools").get(0).getAsJsonObject();
        assertTrue(slimePool.getAsJsonObject("condition").toString().contains("\"size\":1"),
                "Q18: wind charges only from the final small slime");
        assertTrue(slimePool.toString().contains("minecraft:wind_charge")
                && slimePool.toString().contains("minecraft:looting"));
    }

    @Test
    void registryDeclaresCommonImmunityFlagsAndStaysOutOfTheLegacyCatalog() throws Exception {
        String registry = Files.readString(Path.of(
                "src/main/java/dev/itzgamesgd/moreminecrafts/mob/sky/SkyNormalMobs.java"));
        assertTrue(registry.contains(".fireImmune()") && registry.contains(".notInPeaceful()"), "Q36/Q49/Q65/Q74");
        assertTrue(registry.contains("MobCategory.MONSTER"), "Q30: shared hostile cap");
        String legacy = Files.readString(Path.of(
                "src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java"));
        for (String id : IDS) assertFalse(legacy.contains("\"" + id + "\""), id + " must not join the legacy roster");
        // §15.6: natural spawning is wired through spawn placements + structure spawn_overrides, never a biome modifier (N1).
        assertFalse(registry.contains("BiomeModifications"), "N1: no general overworld biome spawns");
        assertTrue(registry.contains("SpawnPlacementsInvoker.moreminecrafts$register"), "spawn placements registered");
    }

    @Test
    void everyMobHasKoreanAndEnglishNames() throws Exception {
        for (String lang : List.of("en_us", "ko_kr")) {
            JsonObject names = json("assets/moreminecrafts/lang/" + lang + ".json");
            for (String id : IDS) assertTrue(names.has("entity.moreminecrafts." + id), lang + " " + id);
        }
        assertEquals("구름 망령", json("assets/moreminecrafts/lang/ko_kr.json")
                .get("entity.moreminecrafts.cloud_wraith").getAsString());
    }
}
