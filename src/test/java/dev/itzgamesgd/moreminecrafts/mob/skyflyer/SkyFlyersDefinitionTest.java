package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Sky Spirit / Storm Hunter data and pure-rule checks (design: SKY_ISLANDS_T2_CONTEXT section 3). Runtime is in SkyFlyerGameTests. */
class SkyFlyersDefinitionTest {
    private static final Path RES = Path.of("src/main/resources");

    private static JsonObject json(String relative) throws Exception {
        return JsonParser.parseString(Files.readString(RES.resolve(relative))).getAsJsonObject();
    }

    private static JsonObject pool(String mob) throws Exception {
        return json("data/moreminecrafts/loot_table/entities/" + mob + ".json").getAsJsonArray("pools").get(0).getAsJsonObject();
    }

    @Test
    void spiritDropsOneSkyFeatherAtThreePercentWithoutLooting() throws Exception {
        JsonObject pool = pool("sky_spirit");
        assertEquals("minecraft:random_chance", pool.getAsJsonObject("condition").get("type").getAsString());
        assertEquals(0.03F, pool.getAsJsonObject("condition").get("chance").getAsFloat());
        assertEquals(SkyFlyerRules.SPIRIT_FEATHER_CHANCE, pool.getAsJsonObject("condition").get("chance").getAsFloat());
        JsonArray entries = pool.getAsJsonArray("entries");
        assertEquals(1, entries.size());
        JsonObject entry = entries.get(0).getAsJsonObject();
        assertEquals("moreminecrafts:sky_feather", entry.get("name").getAsString());
        assertEquals(1, entry.getAsJsonArray("modifier").size(), "only set_count, no looting modifier");
        assertEquals(1, entry.getAsJsonArray("modifier").get(0).getAsJsonObject().get("count").getAsInt());
        assertFalse(Files.readString(RES.resolve("data/moreminecrafts/loot_table/entities/sky_spirit.json")).contains("looting"));
    }

    @Test
    void hunterDropsSixPercentThenOneOrTwoFeathersEightyTwentyWithoutLooting() throws Exception {
        JsonObject pool = pool("storm_hunter");
        assertEquals(0.06F, pool.getAsJsonObject("condition").get("chance").getAsFloat());
        assertEquals(SkyFlyerRules.HUNTER_FEATHER_SUCCESS_CHANCE, pool.getAsJsonObject("condition").get("chance").getAsFloat());
        JsonArray entries = pool.getAsJsonArray("entries");
        assertEquals(2, entries.size());
        int one = 0, two = 0;
        for (var e : entries) {
            JsonObject entry = e.getAsJsonObject();
            assertEquals("moreminecrafts:sky_feather", entry.get("name").getAsString());
            int count = entry.getAsJsonArray("modifier").get(0).getAsJsonObject().get("count").getAsInt();
            int weight = entry.get("weight").getAsInt();
            if (count == 1) one = weight; else if (count == 2) two = weight;
        }
        assertEquals(80, one);
        assertEquals(20, two);
        assertEquals(SkyFlyerRules.HUNTER_ONE_FEATHER_WEIGHT, one);
        assertEquals(SkyFlyerRules.HUNTER_TWO_FEATHER_WEIGHT, two);
        assertFalse(Files.readString(RES.resolve("data/moreminecrafts/loot_table/entities/storm_hunter.json")).contains("looting"));
    }

    @Test
    void hunterOverallFeatherDistributionMatchesDesign() {
        assertEquals(0.94D, SkyFlyerRules.hunterFeatherProbability(0), 1e-6);
        assertEquals(0.048D, SkyFlyerRules.hunterFeatherProbability(1), 1e-6);
        assertEquals(0.012D, SkyFlyerRules.hunterFeatherProbability(2), 1e-6);
        assertEquals(1.0D, SkyFlyerRules.hunterFeatherProbability(0) + SkyFlyerRules.hunterFeatherProbability(1)
                + SkyFlyerRules.hunterFeatherProbability(2), 1e-6);
    }

    @Test
    void flyersAreSeparateFromGroupCIdsPoolsAndRenderers() throws Exception {
        Set<String> flyers = values("data/moreminecrafts/tags/entity_type/sky_flyers.json");
        assertEquals(Set.of("moreminecrafts:sky_spirit", "moreminecrafts:storm_hunter"), flyers);
        Set<String> groupC = values("data/moreminecrafts/tags/entity_type/sky_normal_mobs.json");
        assertEquals(5, groupC.size());
        for (String id : flyers) assertFalse(groupC.contains(id), id + " must not join the group C tag");
        String registry = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/sky/SkyNormalMobs.java"));
        assertFalse(registry.contains("sky_spirit") || registry.contains("storm_hunter") || registry.contains("SkyFlyer"));
        String renderers = Files.readString(Path.of("src/client/java/dev/itzgamesgd/moreminecrafts/client/SkyMobRenderers.java"));
        assertFalse(renderers.contains("SkyFlyer") || renderers.contains("SkySpirit") || renderers.contains("StormHunter"));
    }

    @Test
    void noNaturalSpawnPathReferencesTheFlyers() throws Exception {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(RES.resolve("data"))) {
            files = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
        }
        for (Path file : files) {
            if (file.toString().contains("tags/entity_type/sky_flyers.json") || file.toString().contains("/loot_table/entities/")) continue;
            String text = Files.readString(file);
            assertFalse(text.contains("moreminecrafts:sky_spirit") || text.contains("moreminecrafts:storm_hunter"),
                    file + " would give the flyers a natural spawn, which is not approved (group A spawn policy undecided)");
        }
        String habitat = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/sky/SkyIslandHabitat.java"));
        assertFalse(habitat.contains("SkyFlyers"));
    }

    @Test
    void everyTuningConstantIsMarkedProvisional() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/mob/skyflyer/SkyFlyerTuning.java"));
        int constants = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains("public static final")) continue;
            constants++;
            boolean marked = false;
            for (int back = 1; back <= 4 && i - back >= 0; back++) {
                if (lines.get(i - back).contains("PROVISIONAL")) { marked = true; break; }
            }
            assertTrue(marked, "not marked PROVISIONAL: " + lines.get(i).trim());
        }
        assertTrue(constants >= 15, "expected the tuning constants to exist, found " + constants);
    }

    @Test
    void textureSlotsAreSeparatedAndDefaultToVanillaReuse() throws Exception {
        String source = Files.readString(Path.of("src/client/java/dev/itzgamesgd/moreminecrafts/client/skyflyer/SkyFlyerTextures.java"));
        assertTrue(source.contains("textures/entity/allay/allay.png"));
        assertTrue(source.contains("textures/entity/phantom/phantom.png"));
        assertTrue(source.contains("OWN_SKY_SPIRIT") && source.contains("OWN_STORM_HUNTER"));
        for (String renderer : List.of("SkySpiritRenderer", "StormHunterRenderer")) {
            String text = Files.readString(Path.of("src/client/java/dev/itzgamesgd/moreminecrafts/client/skyflyer/" + renderer + ".java"));
            assertFalse(text.contains("textures/entity"), renderer + " must take its texture from SkyFlyerTextures only");
            assertTrue(text.contains("SkyFlyerTextures."));
        }
    }

    @Test
    void namesExistInBothLanguages() throws Exception {
        for (String lang : List.of("en_us", "ko_kr")) {
            JsonObject names = json("assets/moreminecrafts/lang/" + lang + ".json");
            assertTrue(names.has("entity.moreminecrafts.sky_spirit"), lang);
            assertTrue(names.has("entity.moreminecrafts.storm_hunter"), lang);
        }
        assertEquals("하늘의 정령", json("assets/moreminecrafts/lang/ko_kr.json").get("entity.moreminecrafts.sky_spirit").getAsString());
        assertEquals("폭풍의 사냥꾼", json("assets/moreminecrafts/lang/ko_kr.json").get("entity.moreminecrafts.storm_hunter").getAsString());
    }

    @Test
    void hunterLoopRunsCircleShootDiveRiseInOrderAndFiresOncePerLoop() {
        HunterPattern p = new HunterPattern();
        assertEquals(HunterPattern.Phase.CIRCLE, p.phase());
        int shots = 0;
        HunterPattern.Phase previous = p.phase();
        List<HunterPattern.Phase> order = new java.util.ArrayList<>(List.of(previous));
        for (int tick = 0; tick < 2000 && order.size() < 9; tick++) {
            // The dive "connects" after 5 ticks, the rise "completes" after 15 ticks.
            boolean dived = p.phase() == HunterPattern.Phase.DIVE && p.ticksInPhase() >= 5;
            boolean risen = p.phase() == HunterPattern.Phase.RISE && p.ticksInPhase() >= 15;
            p.tick(dived, risen);
            if (p.shotDue()) shots++;
            if (p.phase() != previous) {
                previous = p.phase();
                order.add(previous);
            }
        }
        assertEquals(List.of(HunterPattern.Phase.CIRCLE, HunterPattern.Phase.SHOOT, HunterPattern.Phase.DIVE,
                HunterPattern.Phase.RISE, HunterPattern.Phase.CIRCLE, HunterPattern.Phase.SHOOT,
                HunterPattern.Phase.DIVE, HunterPattern.Phase.RISE, HunterPattern.Phase.CIRCLE), order);
        assertEquals(2, shots, "one wind shot per loop");
    }

    @Test
    void hunterCirclesForTheConfiguredTimeBeforeShootingAndDiveTimesOut() {
        HunterPattern p = new HunterPattern();
        for (int i = 0; i < SkyFlyerTuning.HUNTER_CIRCLE_TICKS - 1; i++) {
            p.tick(false, false);
            assertEquals(HunterPattern.Phase.CIRCLE, p.phase());
        }
        p.tick(false, false);
        assertEquals(HunterPattern.Phase.SHOOT, p.phase());
        assertTrue(p.shotDue());
        p.tick(false, false);
        assertFalse(p.shotDue());
        for (int i = 0; i < SkyFlyerTuning.HUNTER_SHOT_AFTERGLOW_TICKS - 1; i++) p.tick(false, false);
        assertEquals(HunterPattern.Phase.DIVE, p.phase());
        for (int i = 0; i < SkyFlyerTuning.HUNTER_DIVE_TIMEOUT_TICKS - 1; i++) p.tick(false, false);
        assertEquals(HunterPattern.Phase.DIVE, p.phase());
        p.tick(false, false);
        assertEquals(HunterPattern.Phase.RISE, p.phase(), "a dive that never connects still ends");
        p.reset();
        assertEquals(HunterPattern.Phase.CIRCLE, p.phase());
        assertEquals(0, p.ticksInPhase());
    }

    @Test
    void spiritStopsChasingWhenSightIsLostOrAttackerIsFarOrGone() {
        double near = 10.0D * 10.0D;
        assertFalse(SpiritPursuit.shouldDisengage(true, near, 0));
        assertFalse(SpiritPursuit.shouldDisengage(true, near, SkyFlyerTuning.SPIRIT_LOST_SIGHT_TICKS - 1));
        assertTrue(SpiritPursuit.shouldDisengage(true, near, SkyFlyerTuning.SPIRIT_LOST_SIGHT_TICKS));
        double far = SkyFlyerTuning.SPIRIT_DISENGAGE_DISTANCE;
        assertFalse(SpiritPursuit.shouldDisengage(true, far * far, 0));
        assertTrue(SpiritPursuit.shouldDisengage(true, far * far + 1.0D, 0));
        assertTrue(SpiritPursuit.shouldDisengage(false, near, 0));
    }

    private static Set<String> values(String relative) throws Exception {
        Set<String> out = new HashSet<>();
        json(relative).getAsJsonArray("values").forEach(v -> out.add(v.getAsString()));
        return out;
    }
}
