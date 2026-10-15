package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

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
    void largestRemainderFisheriesMethodIsPreservedAsCandidateOnly() {
        int[][][] expected = {
                {{45,30,16,7,2}, {30,32,22,12,4}, {10,25,32,24,9}, {0,8,34,38,20}},
                {{44,31,16,7,2}, {29,32,22,12,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{43,31,16,8,2}, {28,32,22,13,5}, {9,25,32,24,10}, {0,7,34,38,21}},
                {{42,31,17,7,3}, {27,33,23,12,5}, {8,24,33,24,11}, {0,7,34,37,22}},
                {{41,31,17,8,3}, {26,33,23,13,5}, {8,24,33,24,11}, {0,7,33,37,23}},
                {{40,32,17,8,3}, {25,33,23,13,6}, {7,24,33,24,12}, {0,6,33,37,24}}
        };
        for (int level = 0; level <= 5; level++) {
            for (int tier = 0; tier <= 3; tier++) {
                int[] candidate = ResearchParallelBranchRules.fisheriesLargestRemainderCandidate(tier, level);
                assertArrayEquals(expected[level][tier], candidate, "candidate level=" + level + " tier=" + tier);
                assertEquals(100, Arrays.stream(candidate).sum());
            }
        }
        assertTrue(ResearchRewards.effects("v3.research.19").isEmpty(),
                "the unresolved allocation is not registered as an active reward");
        for (int tier = 0; tier <= 3; tier++)
            assertArrayEquals(expected[0][tier], FishingLogic.gradeWeights(tier),
                    "active fishing still uses the unchanged vanilla/custom baseline");
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
