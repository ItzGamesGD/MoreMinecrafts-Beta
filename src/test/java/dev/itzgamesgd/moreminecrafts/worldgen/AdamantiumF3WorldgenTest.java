package dev.itzgamesgd.moreminecrafts.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Deterministic checks for the approved F-3 Adamantium rule (docs/preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md):
 * Nether only, height Y 8-22 biased toward Y 15, vein size 1-2, and fewer generated blocks than Ancient Debris.
 *
 * <p>Abundance needs real terrain and is measured by tools/ore-sampling (see docs/audits/wave13/ADAMANTIUM_F3_ANALYSIS.md).
 * This class covers everything that does not: the height distribution (using Minecraft's own {@link TrapezoidHeight}),
 * the structural constraints on every candidate configuration, and a characterization of the data that ships today so
 * that a change to it cannot go unnoticed.
 */
class AdamantiumF3WorldgenTest {
    private static final Path ALTERNATIVES = Path.of("tools", "ore-sampling", "alternatives");

    @BeforeAll
    static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static JsonObject shipped(String kind) throws IOException {
        String path = "/data/moreminecrafts/worldgen/" + kind + "/ore_adamantium.json";
        try (var in = AdamantiumF3WorldgenTest.class.getResourceAsStream(path)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject heightRange(JsonObject placed) {
        for (JsonElement e : placed.getAsJsonArray("placement")) {
            JsonObject modifier = e.getAsJsonObject();
            if (modifier.get("type").getAsString().equals("minecraft:height_range")) return modifier.getAsJsonObject("height");
        }
        throw new AssertionError("placed feature has no height_range");
    }

    private static int absolute(JsonObject anchor) { return anchor.get("absolute").getAsInt(); }

    // --- height distribution, using the real vanilla class -------------------------------------------------------

    @Test
    void trapezoidFromEightToTwentyTwoIsSupportedOnlyThereAndPeaksAtFifteen() {
        TrapezoidHeight height = TrapezoidHeight.of(VerticalAnchor.absolute(8), VerticalAnchor.absolute(22));
        RandomSource random = RandomSource.create(20261027L);
        int samples = 262_144;
        int[] counts = new int[40];
        for (int i = 0; i < samples; i++) counts[height.sample(random, null)]++;   // absolute anchors ignore the context
        int min = -1, max = -1, mode = 0;
        for (int y = 0; y < counts.length; y++) {
            if (counts[y] == 0) continue;
            if (min < 0) min = y;
            max = y;
            if (counts[y] > counts[mode]) mode = y;
        }
        assertEquals(8, min);
        assertEquals(22, max);
        assertEquals(15, mode, "F-3: biased toward Y 15");
        // Exact distribution of two independent uniform draws over 0..7 shifted by 8: P(y) = (7 - |y - 15| + 1) / 64.
        for (int y = 8; y <= 22; y++) {
            double expected = samples * (8 - Math.abs(y - 15)) / 64.0;
            double sigma = Math.sqrt(expected * (1 - expected / samples));
            assertTrue(Math.abs(counts[y] - expected) < 5 * sigma, "Y" + y + " count " + counts[y] + " vs " + expected);
        }
        for (int y = 8; y < 15; y++) assertTrue(counts[y] < counts[y + 1], "rises toward Y15 at " + y);
        for (int y = 15; y < 22; y++) assertTrue(counts[y] > counts[y + 1], "falls away from Y15 at " + y);
    }

    @Test
    void uniformFromEightToTwentyFourIsFlatAndSoDoesNotMeetTheBiasRule() {
        // Documents why the shipped height provider cannot satisfy "biased toward Y 15".
        RandomSource random = RandomSource.create(20261027L);
        int samples = 262_144;
        int[] counts = new int[40];
        for (int i = 0; i < samples; i++) counts[net.minecraft.util.Mth.randomBetweenInclusive(random, 8, 24)]++;
        int lo = Integer.MAX_VALUE, hi = 0;
        for (int y = 8; y <= 24; y++) { lo = Math.min(lo, counts[y]); hi = Math.max(hi, counts[y]); }
        assertTrue(hi < lo * 1.1, "uniform: every level within 10% of every other");
        assertTrue(counts[25] == 0 && counts[7] == 0);
    }

    // --- data that ships today -----------------------------------------------------------------------------------

    /**
     * Characterization, not approval. The shipped data predates F-3 and does not satisfy it (F-3 itself says so). This test
     * fails when somebody changes it, so the change is deliberate: update docs/audits/wave13/ADAMANTIUM_F3_ANALYSIS.md and
     * re-run tools/ore-sampling at the same time.
     */
    @Test
    void shippedAdamantiumDataIsStillThePreF3Configuration() throws IOException {
        JsonObject feature = shipped("feature");
        JsonObject placed = shipped("placed_feature");
        assertEquals("minecraft:ore", feature.get("type").getAsString());
        assertEquals(3, feature.get("size").getAsInt());
        JsonObject height = heightRange(placed);
        assertEquals("minecraft:uniform", height.get("type").getAsString());
        assertEquals(8, absolute(height.getAsJsonObject("min_inclusive")));
        assertEquals(24, absolute(height.getAsJsonObject("max_inclusive")));
    }

    @Test
    void adamantiumIsRegisteredOnlyForNetherBiomes() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "dev", "itzgamesgd", "moreminecrafts", "worldgen", "ModOreGeneration.java"));
        int at = source.indexOf("ore_adamantium");
        assertTrue(at > 0);
        String registration = source.substring(Math.max(0, source.lastIndexOf("BiomeModifications", at)), at);
        assertTrue(registration.contains("foundInTheNether"), "Adamantium must be added to Nether biomes");
        assertFalse(registration.contains("foundInOverworld"), "Adamantium must not be added to Overworld biomes");
    }

    // --- every candidate configuration --------------------------------------------------------------------------

    private static List<Path> alternatives() throws IOException {
        try (Stream<Path> files = Files.list(ALTERNATIVES)) {
            return files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    @Test
    void everyCandidateSatisfiesTheHardF3Constraints() throws IOException {
        List<Path> candidates = alternatives();
        assertFalse(candidates.isEmpty());
        for (Path file : candidates) {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonObject feature = root.getAsJsonObject("feature");
            JsonObject placed = root.getAsJsonObject("placed_feature");
            String name = file.getFileName().toString();
            // Vein size 1-2. Plain minecraft:ore at size 2 generates nothing at all (measured: 0 blocks in 400 chunks at
            // 1-6 placements per chunk), so the feature type that can express F-3 is scattered_ore, as vanilla debris uses.
            assertEquals("minecraft:scattered_ore", feature.get("type").getAsString(), name);
            assertTrue(feature.get("size").getAsInt() >= 1 && feature.get("size").getAsInt() <= 2, name);
            assertEquals(1.0, feature.get("discard_chance_on_air_exposure").getAsDouble(), 0.0, name + ": same exposure rule as Ancient Debris");
            JsonObject target = feature.getAsJsonArray("targets").get(0).getAsJsonObject();
            assertEquals("minecraft:base_stone_nether", target.getAsJsonObject("target").get("tag").getAsString(), name);
            assertEquals("moreminecrafts:adamantium_debris", target.get("state").getAsString(), name);
            // Height: trapezoid, no plateau (peak mid-range), origin range starting at Y8 and not above Y22.
            JsonObject height = heightRange(placed);
            assertEquals("minecraft:trapezoid", height.get("type").getAsString(), name);
            assertFalse(height.has("plateau"), name);
            assertEquals(8, absolute(height.getAsJsonObject("min_inclusive")), name);
            int max = absolute(height.getAsJsonObject("max_inclusive"));
            assertTrue(max == 21 || max == 22, name + ": origin ceiling " + max);
            assertTrue(placed.toString().contains("minecraft:in_square") && placed.toString().contains("minecraft:biome"), name);
        }
    }

    @Test
    void candidatesDifferOnlyInTheUndecidedParameters() throws IOException {
        // The decided F-3 parameters (type, size, discard, floor Y8) are identical everywhere; only the placement count and the
        // origin ceiling (21 vs 22) vary. If this fails, a candidate smuggled in another change.
        JsonObject first = null;
        for (Path file : alternatives()) {
            JsonObject feature = JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("feature");
            if (first == null) first = feature;
            assertEquals(first, feature, file.getFileName().toString());
        }
    }
}
