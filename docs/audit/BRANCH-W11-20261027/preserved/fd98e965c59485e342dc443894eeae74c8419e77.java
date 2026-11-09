package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.fishing.FishingLogic;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class ResearchParallelBranchRulesTest {
    @Test
    void sprintAndSwimExhaustionUseOnlyTheApprovedPaidLevels() {
        assertEquals(1.0, ResearchParallelBranchRules.sprintSwimExhaustionMultiplier(0), 1.0e-12);
        assertEquals(0.92, ResearchParallelBranchRules.sprintSwimExhaustionMultiplier(1), 1.0e-12);
        assertEquals(0.84, ResearchParallelBranchRules.sprintSwimExhaustionMultiplier(2), 1.0e-12);
        assertEquals(0.76, ResearchParallelBranchRules.sprintSwimExhaustionMultiplier(3), 1.0e-12);
        assertEquals(0.76, ResearchParallelBranchRules.sprintSwimExhaustionMultiplier(99), 1.0e-12);
        assertEquals(1.0, ResearchParallelBranchRules.sprintSwimExhaustionMultiplier(-1), 1.0e-12);
    }

    @Test
    void xpControlUsesAnInclusiveEuclideanRadiusAndTwoPurchasedLevels() {
        assertEquals(0.0, ResearchParallelBranchRules.xpCollectionRadius(0));
        assertEquals(4.0, ResearchParallelBranchRules.xpCollectionRadius(1));
        assertEquals(6.0, ResearchParallelBranchRules.xpCollectionRadius(2));
        assertEquals(6.0, ResearchParallelBranchRules.xpCollectionRadius(3));
        assertTrue(ResearchParallelBranchRules.withinXpCollectionRadius(0, 0, 4, 1));
        assertFalse(ResearchParallelBranchRules.withinXpCollectionRadius(0, 0, 4.001, 1));
        assertTrue(ResearchParallelBranchRules.withinXpCollectionRadius(0, 0, 6, 2));
        assertFalse(ResearchParallelBranchRules.withinXpCollectionRadius(0, 0, 6.001, 2));
        assertFalse(ResearchParallelBranchRules.withinXpCollectionRadius(0, 0, 0, 0));
        assertFalse(ResearchParallelBranchRules.withinXpCollectionRadius(3, 0, 3, 1));
    }

    @Test
    void fisheriesStudyUsesApprovedLargestRemainderTablesAtEveryLevelAndCrankbaitTier() {
        int[][][] expected = {
                {{45,30,16,7,2}, {30,32,22,12,4}, {10,25,32,24,9}, {0,8,34,38,20}},
                {{44,31,16,7,2}, {29,32,22,12,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{43,31,17,7,2}, {28,33,22,12,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{42,31,17,8,2}, {27,33,23,12,5}, {8,24,33,24,11}, {0,7,34,37,22}},
                {{41,31,17,8,3}, {26,33,23,13,5}, {8,24,33,24,11}, {0,7,33,37,23}},
                {{40,32,17,8,3}, {25,33,23,13,6}, {7,24,33,24,12}, {0,6,33,37,24}}
        };
        assertEquals(5, ResearchRewards.effects("v3.research.19").getFirst().maximumLevel());
        assertEquals(0, ResearchRewards.level(ResearchProgressState.empty(),
                "v3.research.19", "fisheries_grade"), "unowned Fisheries Study uses level zero");
        assertEquals(1, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.19", 1), "v3.research.19", "fisheries_grade"));
        assertEquals(5, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.19", 99), "v3.research.19", "fisheries_grade"));

        for (int level = 0; level <= 5; level++) {
            for (int tier = 0; tier <= 3; tier++) {
                int[] weights = FishingLogic.gradeWeights(tier, level);
                assertArrayEquals(expected[level][tier], weights, "level=" + level + " tier=" + tier);
                assertEquals(100, Arrays.stream(weights).sum(), "level=" + level + " tier=" + tier);
                if (level == 0) {
                    assertArrayEquals(expected[0][tier], FishingLogic.gradeWeights(tier),
                            "legacy fishing uses baseline at tier=" + tier);
                }
            }
        }
    }

    @Test
    void fisheriesLargestRemainderTiesGoToTheLowerGradeIndex() {
        // Tier 0, level 2 has equal .4 remainders for grades III–V; only one can receive a point.
        assertArrayEquals(new int[]{43, 31, 17, 7, 2}, FishingLogic.gradeWeights(0, 2));
    }

    @Test
    void fisheriesPurchasedLevelSurvivesProgressionSerialization() {
        ResearchProgressState original = ResearchProgressState.empty()
                .with("research.v3.research.19", 4);
        var encoded = ResearchProgressState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ResearchProgressState restored = ResearchProgressState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, restored);
        assertEquals(4, ResearchRewards.level(restored, "v3.research.19", "fisheries_grade"));
    }

    @Test
    void onlyTheNewAuthorizedEffectsAreRegisteredAndPurchaseGated() {
        assertEquals(3, ResearchRewards.effects("v3.research.55").getFirst().maximumLevel());
        assertEquals(2, ResearchRewards.effects("v3.research.67").getFirst().maximumLevel());
        assertEquals(0, ResearchRewards.level(ResearchProgressState.empty(),
                "v3.research.55", "sprint_swim_exhaustion"));
        assertEquals(1, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.55", 1), "v3.research.55", "sprint_swim_exhaustion"));
        assertEquals(0, ResearchRewards.level(ResearchProgressState.empty(),
                "v3.research.67", "xp_orb_collection"));
        assertEquals(2, ResearchRewards.level(ResearchProgressState.empty()
                .with("research.v3.research.67", 7), "v3.research.67", "xp_orb_collection"));
    }
}
