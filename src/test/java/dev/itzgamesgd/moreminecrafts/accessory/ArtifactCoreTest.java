package dev.itzgamesgd.moreminecrafts.accessory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ArtifactCoreTest {
    private static final List<String> IDS = List.of(
            "earthen_relic_core", "abyssal_relic_core", "glacial_relic_core", "tidal_relic_core", "blazing_relic_core");

    @Test void fiveCoreIdsAndNamesMatchTheApprovedBiomeCoreDocument() {
        assertEquals(IDS, ArtifactCore.values().stream().map(ArtifactCore::id).toList());
        assertEquals(List.of("대지의 유물핵", "심연의 유물핵", "한기의 유물핵", "해류의 유물핵", "작열의 유물핵"),
                ArtifactCore.values().stream().map(ArtifactCore::koreanName).toList());
    }

    @Test void dropTierBoundsMatchTheDocumentWithoutSelectingAnUndocumentedChance() {
        assertEquals(List.of(8, 12, 1, 1), bounds(ArtifactCoreDropRules.Tier.GENERAL_STRONG));
        assertEquals(List.of(35, 50, 1, 1), bounds(ArtifactCoreDropRules.Tier.HIGH_ELITE));
        assertEquals(List.of(100, 100, 1, 2), bounds(ArtifactCoreDropRules.Tier.BOSS));
    }

    private static List<Integer> bounds(ArtifactCoreDropRules.Tier tier) {
        return List.of(tier.minPercent(), tier.maxPercent(), tier.minCount(), tier.maxCount());
    }

    @Test void eachCoreHasItemModelAndBothLocaleEntries() throws Exception {
        JsonObject ko = json("assets/moreminecrafts/lang/ko_kr.json");
        JsonObject en = json("assets/moreminecrafts/lang/en_us.json");
        for (ArtifactCore core : ArtifactCore.values()) {
            assertNotNull(getResource("assets/moreminecrafts/models/item/" + core.id() + ".json"), core.id() + " model");
            assertEquals(core.koreanName(), ko.get("item.moreminecrafts." + core.id()).getAsString());
            assertTrue(en.has("item.moreminecrafts." + core.id()));
        }
    }

    @Test void accessoryMappingContainsEveryDocumentedIdentifierAndNoMadeUpId() {
        assertEquals(20, ArtifactCoreMapping.ALL.size(), "the current authorities enumerate 7 T1 plus 13 T2 names");
        Map<ArtifactCore, Set<String>> actual = ArtifactCoreMapping.ALL.stream().collect(Collectors.groupingBy(
                ArtifactCoreMapping.Entry::core,
                Collectors.mapping(ArtifactCoreMapping.Entry::accessoryId,
                        Collectors.filtering(java.util.Objects::nonNull, Collectors.toSet()))));
        assertEquals(Set.of("t1_combo", "t1_sprint_acceleration", "phantom_mirror", "soul_chain"), actual.get(ArtifactCore.EARTHEN));
        assertEquals(Set.of("t1_status_resilience", "t1_sustained_recovery", "heart_of_gravity", "seed_of_life"), actual.get(ArtifactCore.ABYSSAL));
        assertEquals(Set.of("t1_double_jump", "ring_of_time", "snow_charm"), actual.get(ArtifactCore.GLACIAL));
        assertEquals(Set.of("deep_sea_pearl", "whisper_of_the_sea"), actual.get(ArtifactCore.TIDAL));
        assertEquals(Set.of("t1_crisis_escape", "t1_heavy_buffer", "star_covenant", "blood_covenant", "ember_ring", "ring_of_life"), actual.get(ArtifactCore.BLAZING));
        assertTrue(ArtifactCoreMapping.ALL.stream().anyMatch(e -> e.accessoryId() == null && e.koreanName().equals("깃털 부적")));
    }

    @Test void biomeRulesApplyDocumentedPriorityAcrossConflictingInputs() {
        assertEquals(ArtifactCore.ABYSSAL, ArtifactCoreBiomeRules.classify(location(ArtifactCoreBiomeRules.Dimension.END, 80, true)));
        assertEquals(ArtifactCore.ABYSSAL, ArtifactCoreBiomeRules.classify(location(ArtifactCoreBiomeRules.Dimension.OVERWORLD, 20, false,
                ArtifactCoreBiomeRules.BiomeTag.OCEAN)));
        assertEquals(ArtifactCore.BLAZING, ArtifactCoreBiomeRules.classify(location(ArtifactCoreBiomeRules.Dimension.NETHER, 80, true,
                ArtifactCoreBiomeRules.BiomeTag.OCEAN, ArtifactCoreBiomeRules.BiomeTag.SNOWY)));
        assertEquals(ArtifactCore.TIDAL, ArtifactCoreBiomeRules.classify(location(ArtifactCoreBiomeRules.Dimension.OVERWORLD, 80, true,
                ArtifactCoreBiomeRules.BiomeTag.RIVER, ArtifactCoreBiomeRules.BiomeTag.MOUNTAIN)));
        assertEquals(ArtifactCore.GLACIAL, ArtifactCoreBiomeRules.classify(location(ArtifactCoreBiomeRules.Dimension.OVERWORLD, 140, true)));
        assertEquals(ArtifactCore.EARTHEN, ArtifactCoreBiomeRules.classify(location(ArtifactCoreBiomeRules.Dimension.OVERWORLD, 64, true)));
        assertNull(ArtifactCoreBiomeRules.classify(location(ArtifactCoreBiomeRules.Dimension.OTHER, 64, true)));
    }

    private static ArtifactCoreBiomeRules.Location location(ArtifactCoreBiomeRules.Dimension dimension, int y, boolean sky,
                                                             ArtifactCoreBiomeRules.BiomeTag... tags) {
        return new ArtifactCoreBiomeRules.Location(dimension, y, sky, Set.of(tags));
    }

    private static InputStream getResource(String path) {
        return ArtifactCoreTest.class.getClassLoader().getResourceAsStream(path);
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = getResource(path)) {
            assertNotNull(in, path);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
