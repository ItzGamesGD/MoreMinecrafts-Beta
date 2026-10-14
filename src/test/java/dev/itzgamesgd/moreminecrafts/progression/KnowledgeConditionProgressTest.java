package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class KnowledgeConditionProgressTest {
    @Test void swordHostileKillsKeepSharedProgressMonotonicAcrossBothThresholds() {
        assertSequence(100, 0, 49, 50, 51, 100);
    }

    @Test void structuresVisitedKeepSharedProgressMonotonicAcrossBothThresholds() {
        assertSequence(10, 0, 4, 5, 6, 10);
    }

    @Test void potionsBrewedKeepSharedProgressMonotonicAcrossBothThresholds() {
        assertSequence(50, 0, 1, 20, 21, 50);
    }

    @Test void duplicateLowerAndOverTargetObservationsDoNotDecreaseOrExceedProgress() {
        int progress = KnowledgeConditionProgress.advance(0, 20, 50);
        assertEquals(20, progress);
        assertEquals(20, KnowledgeConditionProgress.advance(progress, 20, 50));
        assertEquals(20, KnowledgeConditionProgress.advance(progress, 7, 50));
        assertEquals(50, KnowledgeConditionProgress.advance(progress, 500, 50));
        assertEquals(50, KnowledgeConditionProgress.advance(50, 20, 50));
    }

    private static void assertSequence(int maximum, int initial, int... observations) {
        int progress = initial;
        for (int observation : observations) {
            progress = KnowledgeConditionProgress.advance(progress, observation, maximum);
            assertEquals(observation, progress);
        }
    }
}
