package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.fishing.FishingLogic;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Research23AEffectRulesTest {
    @Test
    void lumberingScalesChanceAndExtraLogCapByPaidLevel() {
        assertArrayEquals(new int[]{0, 10, 20, 20, 30, 30},
                java.util.stream.IntStream.rangeClosed(0, 5)
                        .map(Research23AEffectRules::lumberingChancePercent).toArray());
        assertArrayEquals(new int[]{0, 1, 1, 2, 2, 3},
                java.util.stream.IntStream.rangeClosed(0, 5)
                        .map(Research23AEffectRules::maximumExtraLogs).toArray());
        assertEquals(1, Research23AEffectRules.rollExtraLogs(1, 0.099, 0));
        assertEquals(0, Research23AEffectRules.rollExtraLogs(1, 0.10, 0));
        assertEquals(2, Research23AEffectRules.rollExtraLogs(3, 0.0, 1));
        assertEquals(3, Research23AEffectRules.rollExtraLogs(5, 0.0, 2));
    }

    @Test
    void mineralConservationIsLinearAndDoesNotTurnZeroDamageIntoAReward() {
        assertArrayEquals(new int[]{0, 5, 10, 15, 20, 25},
                java.util.stream.IntStream.rangeClosed(0, 5)
                        .map(Research23AEffectRules::mineralConservationChancePercent).toArray());
        assertFalse(Research23AEffectRules.shouldPreventMiningDamage(0, 0.0, 5));
        assertTrue(Research23AEffectRules.shouldPreventMiningDamage(1, 0.049, 1));
        assertFalse(Research23AEffectRules.shouldPreventMiningDamage(1, 0.05, 1));
        assertFalse(Research23AEffectRules.shouldPreventMiningDamage(1, 0.0, 0));
    }

    @Test
    void beekeepingScalesToOneExtraCombPerHarvest() {
        assertArrayEquals(new int[]{0, 20, 40, 60},
                java.util.stream.IntStream.rangeClosed(0, 3)
                        .map(Research23AEffectRules::beekeepingChancePercent).toArray());
        assertTrue(Research23AEffectRules.shouldGrantBonusHoneycomb(1, 0.199));
        assertFalse(Research23AEffectRules.shouldGrantBonusHoneycomb(1, 0.20));
        assertTrue(Research23AEffectRules.shouldGrantBonusHoneycomb(3, 0.599));
        assertFalse(Research23AEffectRules.shouldGrantBonusHoneycomb(3, 0.60));
    }

    @Test
    void survivalReducesOnlyEligibleExhaustionByApprovedLevel() {
        assertEquals(1.0, Research23AEffectRules.sprintSwimExhaustionMultiplier(0));
        assertEquals(0.92, Research23AEffectRules.sprintSwimExhaustionMultiplier(1), 1e-9);
        assertEquals(0.84, Research23AEffectRules.sprintSwimExhaustionMultiplier(2), 1e-9);
        assertEquals(0.76, Research23AEffectRules.sprintSwimExhaustionMultiplier(3), 1e-9);
    }

    @Test
    void xpControlUsesInclusiveEuclideanRadiusAndOnlyTwoPaidLevels() {
        assertEquals(0.0, Research23AEffectRules.xpCollectionRadius(0));
        assertEquals(4.0, Research23AEffectRules.xpCollectionRadius(1));
        assertEquals(6.0, Research23AEffectRules.xpCollectionRadius(2));
        assertEquals(6.0, Research23AEffectRules.xpCollectionRadius(3));
        assertTrue(Research23AEffectRules.withinXpCollectionRadius(4.0, 0.0, 0.0, 1));
        assertFalse(Research23AEffectRules.withinXpCollectionRadius(4.001, 0.0, 0.0, 1));
        assertTrue(Research23AEffectRules.withinXpCollectionRadius(6.0, 0.0, 0.0, 2));
        assertFalse(Research23AEffectRules.withinXpCollectionRadius(6.001, 0.0, 0.0, 2));
        assertFalse(Research23AEffectRules.withinXpCollectionRadius(0.0, 0.0, 0.0, 0));
    }

    @Test
    void fisheriesInterpolatesEachExistingGradeTableWithoutChangingOtherFishRolls() {
        int[][][] expected = {
                {{45,30,16,7,2}, {30,32,22,12,4}, {10,25,32,24,9}, {0,8,34,38,20}},
                {{44,31,16,7,2}, {29,32,22,12,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{43,31,16,8,2}, {28,32,22,13,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{42,31,17,7,3}, {27,33,23,12,5}, {8,24,33,24,11}, {0,7,34,37,22}},
                {{41,31,17,8,3}, {26,33,23,13,5}, {8,24,33,24,11}, {0,7,33,37,23}},
                {{40,32,17,8,3}, {25,33,23,13,6}, {7,24,33,24,12}, {0,6,33,37,24}}
        };
        for (int level = 0; level <= 5; level++) {
            for (int crankbait = 0; crankbait <= 3; crankbait++) {
                int[] weights = FishingLogic.gradeWeights(crankbait, level);
                assertArrayEquals(expected[level][crankbait], weights,
                        "level=" + level + " crankbait=" + crankbait);
                assertEquals(100, Arrays.stream(weights).sum());
            }
        }
        var withoutStudy = FishingLogic.rollFish(FishingLogic.accessibleSpecies(3), 2, 0,
                net.minecraft.util.RandomSource.create(91));
        var withStudy = FishingLogic.rollFish(FishingLogic.accessibleSpecies(3), 2, 5,
                net.minecraft.util.RandomSource.create(91));
        assertEquals(withoutStudy.species(), withStudy.species());
        assertEquals(withoutStudy.lengthTenthsCm(), withStudy.lengthTenthsCm());
        assertEquals(withoutStudy.weightTenthsKg(), withStudy.weightTenthsKg());
    }

    @Test
    void everySelectedEffectRemainsInactiveUntilItsPurchaseLevelIsRecorded() {
        String[][] effects = {
                {"v3.research.02", "lumbering_extra_logs"},
                {"v3.research.03", "mineral_conservation"},
                {"v3.research.18", "beekeeping_honeycomb"},
                {"v3.research.19", "fisheries_grade"},
                {"v3.research.55", "sprint_swim_exhaustion"},
                {"v3.research.67", "xp_orb_collection"}
        };
        for (String[] effect : effects) {
            assertEquals(0, ResearchRewards.level(ResearchProgressState.empty(), effect[0], effect[1]), effect[0]);
            assertEquals(1, ResearchRewards.level(ResearchProgressState.empty()
                    .with("research." + effect[0], 1), effect[0], effect[1]), effect[0]);
        }
        assertEquals(2, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.67", 3), "v3.research.67", "xp_orb_collection"));
    }

    @Test
    void selectedPurchaseLevelsRemainCodecCompatibleWithoutNewEffectState() {
        ResearchProgressState beforeSave = ResearchProgressState.empty()
                .with("research.v3.research.02", 5)
                .with("research.v3.research.03", 3)
                .with("research.v3.research.18", 2)
                .with("research.v3.research.19", 4)
                .with("research.v3.research.55", 3)
                .with("research.v3.research.67", 2);
        var encoded = ResearchProgressState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, beforeSave)
                .result().orElseThrow();
        ResearchProgressState afterLoad = ResearchProgressState.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertEquals(beforeSave, afterLoad);
        assertEquals(beforeSave.researchLevel("v3.research.67"), afterLoad.researchLevel("v3.research.67"));
    }
}
