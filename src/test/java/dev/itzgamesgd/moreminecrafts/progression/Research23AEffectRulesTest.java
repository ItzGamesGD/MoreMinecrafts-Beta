package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.fishing.FishingLogic;
import dev.itzgamesgd.moreminecrafts.fishing.FishingTiming;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Research23AEffectRulesTest {
    @Test
    void fisheriesUsesApprovedLargestRemainderTablesAtEveryPaidLevelAndCrankbaitTier() {
        int[][][] expected = {
                {{45,30,16,7,2}, {30,32,22,12,4}, {10,25,32,24,9}, {0,8,34,38,20}},
                {{44,31,16,7,2}, {29,32,22,12,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{43,31,17,7,2}, {28,33,22,12,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{42,31,17,8,2}, {27,33,23,12,5}, {8,24,33,24,11}, {0,7,34,37,22}},
                {{41,31,17,8,3}, {26,33,23,13,5}, {8,24,33,24,11}, {0,7,33,37,23}},
                {{40,32,17,8,3}, {25,33,23,13,6}, {7,24,33,24,12}, {0,6,33,37,24}}
        };
        for (int level = 0; level <= 5; level++) {
            for (int tier = 0; tier <= 3; tier++) {
                int[] weights = FishingLogic.gradeWeights(tier, level);
                assertArrayEquals(expected[level][tier], weights, "level=" + level + " tier=" + tier);
                assertEquals(100, Arrays.stream(weights).sum());
                if (level == 0) {
                    assertArrayEquals(expected[0][tier], FishingLogic.gradeWeights(tier),
                            "unpurchased Fisheries Study retains the tier baseline");
                }
            }
        }
    }

    @Test
    void largestRemainderTiesAwardThePointToTheLowerGradeIndex() {
        assertArrayEquals(new int[]{34, 33, 33},
                Research23AEffectRules.allocateLargestRemainder(new long[]{167, 167, 166}));
        assertArrayEquals(new int[]{43, 31, 17, 7, 2}, FishingLogic.gradeWeights(0, 2),
                "tier-zero level-two ties award the residual point to grade III, the lowest tied index");
    }

    @Test
    void fisheriesChangesOnlyGradeAndWorksAlongsideFishingTechnique() {
        var withoutStudy = FishingLogic.rollFish(FishingLogic.accessibleSpecies(3), 2, 0,
                net.minecraft.util.RandomSource.create(91));
        var withStudy = FishingLogic.rollFish(FishingLogic.accessibleSpecies(3), 2, 5,
                net.minecraft.util.RandomSource.create(91));
        assertEquals(withoutStudy.species(), withStudy.species());
        assertEquals(withoutStudy.lengthTenthsCm(), withStudy.lengthTenthsCm());
        assertEquals(withoutStudy.weightTenthsKg(), withStudy.weightTenthsKg());
        assertArrayEquals(new int[]{9, 25, 32, 24, 10}, FishingLogic.gradeWeights(2, 1));
        assertEquals(FishingTiming.Zone.LIGHT_GREEN, FishingTiming.zone(0.5, 1));
    }

    @Test
    void survivalScalesSprintAndSwimExhaustionButLeavesEveryOtherCauseAlone() {
        for (int level = 0; level <= 3; level++) {
            double expected = 1.0 - level * 0.08;
            assertEquals(10.0 * expected, Research23AEffectRules.exhaustionAmount(
                    10.0F, level, Research23AEffectRules.ExhaustionSource.SPRINTING), 1e-6);
            assertEquals(10.0 * expected, Research23AEffectRules.exhaustionAmount(
                    10.0F, level, Research23AEffectRules.ExhaustionSource.SWIMMING), 1e-6);
            assertEquals(10.0, Research23AEffectRules.exhaustionAmount(
                    10.0F, level, Research23AEffectRules.ExhaustionSource.OTHER), 0.0);
        }
    }

    @Test
    void xpControlUsesOnlyItsTwoPurchasedLevelsAndInclusiveEuclideanRadii() {
        assertEquals(0.0, Research23AEffectRules.xpCollectionRadius(0));
        assertEquals(4.0, Research23AEffectRules.xpCollectionRadius(1));
        assertEquals(6.0, Research23AEffectRules.xpCollectionRadius(2));
        assertEquals(6.0, Research23AEffectRules.xpCollectionRadius(3));
        assertTrue(Research23AEffectRules.withinXpCollectionRadius(4.0, 0.0, 0.0, 1));
        assertFalse(Research23AEffectRules.withinXpCollectionRadius(4.001, 0.0, 0.0, 1));
        assertTrue(Research23AEffectRules.withinXpCollectionRadius(6.0, 0.0, 0.0, 2));
        assertFalse(Research23AEffectRules.withinXpCollectionRadius(6.001, 0.0, 0.0, 2));
        assertTrue(Research23AEffectRules.withinXpCollectionRadius(4.8, 0.0, 3.6, 2));
        assertFalse(Research23AEffectRules.withinXpCollectionRadius(4.8, 0.0, 3.601, 2));
        assertFalse(Research23AEffectRules.withinXpCollectionRadius(0.0, 0.0, 0.0, 0));
    }

    /** Retains PR #93's unique purchased Fisheries Study codec regression. */
    @Test
    void fisheriesResearchLevelSurvivesProgressionSerialization() {
        ResearchProgressState initial = ResearchProgressState.empty()
                .with("research.v3.research.19", 4);
        var encoded = ResearchProgressState.CODEC.encodeStart(JsonOps.INSTANCE, initial).getOrThrow();
        ResearchProgressState decoded = ResearchProgressState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(initial, decoded);
        assertEquals(4, ResearchRewards.level(decoded, "v3.research.19", "fisheries_grade"));
    }

    @Test
    void importedEffectsArePurchaseGatedAndExistingDuplicateOwnersKeepTheirRewardKeys() {
        assertEquals(0, ResearchRewards.level(ResearchProgressState.empty(),
                "v3.research.19", "fisheries_grade"));
        assertEquals(1, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.19", 1), "v3.research.19", "fisheries_grade"));
        assertEquals(3, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.55", 4), "v3.research.55", "sprint_swim_exhaustion"));
        assertEquals(2, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.67", 3), "v3.research.67", "xp_orb_collection"));
        assertEquals("lumbering_extra_drops", ResearchRewards.effects("v3.research.02").getFirst().id());
        assertEquals("mineral_durability_preservation", ResearchRewards.effects("v3.research.03").getFirst().id());
        assertEquals("beekeeping_extra_honeycomb", ResearchRewards.effects("v3.research.18").getFirst().id());
        assertEquals("fishing_technique_window", ResearchRewards.effects("v3.research.23").getFirst().id());
    }
}
