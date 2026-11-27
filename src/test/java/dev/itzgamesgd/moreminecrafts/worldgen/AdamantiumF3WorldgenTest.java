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
    void shippedOriginRangeIsSupportedOnlyFromNineToTwentyOneAndPeaksAtFifteen() throws IOException {
        JsonObject range = heightRange(shipped("placed_feature"));
        TrapezoidHeight height = TrapezoidHeight.of(VerticalAnchor.absolute(absolute(range.getAsJsonObject("min_inclusive"))),
                VerticalAnchor.absolute(absolute(range.getAsJsonObject("max_inclusive"))));
        RandomSource random = RandomSource.create(20261027L);
        int[] counts = new int[40];
        for (int i = 0; i < 262_144; i++) counts[height.sample(random, null)]++;
        int min = -1, max = -1, mode = 0;
        for (int y = 0; y < counts.length; y++) {
            if (counts[y] == 0) continue;
            if (min < 0) min = y;
            max = y;
            if (counts[y] > counts[mode]) mode = y;
        }
        assertEquals(9, min);
        assertEquals(21, max);
        assertEquals(15, mode);
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
     * The shipped data is exactly candidate E (docs/audits/wave13/ADAMANTIUM_F3_ANALYSIS.md), selected by the provisional
     * owner decisions D-F3-1 (Y 8-22 bounds every generated block) and D-F3-2 (one attempt per chunk, as for both vanilla
     * Ancient Debris features, which carry no count modifier).
     */
    @Test
    void shippedAdamantiumDataIsCandidateE() throws IOException {
        JsonObject candidate = JsonParser.parseString(Files.readString(ALTERNATIVES.resolve("E_count1_origin9-21.json"))).getAsJsonObject();
        assertEquals(candidate.getAsJsonObject("feature"), shipped("feature"));
        assertEquals(candidate.getAsJsonObject("placed_feature"), shipped("placed_feature"));
    }

    /**
     * D-F3-1, strict form. scattered_ore moves a block at most one block vertically from the vein origin (measured: Y 7 and
     * Y 23 appear from an 8-22 origin range and never from 9-21), so an origin range of 9..21 confines every block to 8..22.
     * The sampling run (tools/ore-sampling/check_f3.py --strict-block-y) is the empirical proof; this pins the data that proof used.
     */
    @Test
    void shippedOriginRangeConfinesEveryBlockToY8Through22() throws IOException {
        JsonObject height = heightRange(shipped("placed_feature"));
        assertEquals("minecraft:trapezoid", height.get("type").getAsString());
        assertFalse(height.has("plateau"));
        assertTrue(absolute(height.getAsJsonObject("min_inclusive")) - 1 >= 8);
        assertTrue(absolute(height.getAsJsonObject("max_inclusive")) + 1 <= 22);
        assertEquals("minecraft:scattered_ore", shipped("feature").get("type").getAsString());
        assertEquals(2, shipped("feature").get("size").getAsInt());
    }

    /** D-F3-2: no count modifier other than a constant 1, i.e. the single attempt per chunk of vanilla Ancient Debris. */
    @Test
    void shippedPlacementIsOneAttemptPerChunk() throws IOException {
        for (JsonElement e : shipped("placed_feature").getAsJsonArray("placement")) {
            JsonObject modifier = e.getAsJsonObject();
            String type = modifier.get("type").getAsString();
            assertFalse(type.equals("minecraft:rarity_filter") || type.equals("minecraft:count_on_every_layer") || type.equals("minecraft:noise_threshold_count"), type);
            if (type.equals("minecraft:count")) assertEquals(1, modifier.get("count").getAsInt());
        }
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
            // Height: trapezoid, no plateau (peak mid-range). The origin range may be inset by one block, because scattered_ore
            // spills one block vertically; it must never extend beyond Y 8..22, and it must stay centred on Y 15 or just below.
            JsonObject height = heightRange(placed);
            assertEquals("minecraft:trapezoid", height.get("type").getAsString(), name);
            assertFalse(height.has("plateau"), name);
            int min = absolute(height.getAsJsonObject("min_inclusive"));
            int max = absolute(height.getAsJsonObject("max_inclusive"));
            assertTrue(min == 8 || min == 9, name + ": origin floor " + min);
            assertTrue(max == 21 || max == 22, name + ": origin ceiling " + max);
            assertTrue((min + max) / 2.0 >= 14.5 && (min + max) / 2.0 <= 15.0, name + ": peak " + (min + max) / 2.0);
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
