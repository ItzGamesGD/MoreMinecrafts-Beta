package dev.itzgamesgd.moreminecrafts.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBatch2RulesTest {
    @Test void fuseDelayUsesApprovedNonlinearLevelTable() {
        int[] expected = {0, 6, 12, 20};
        for (int level = 0; level <= 3; level++) assertEquals(expected[level], ResearchBatch2Rules.fuseDelay(level));
        assertEquals(0, ResearchBatch2Rules.fuseDelay(-1));
        assertEquals(20, ResearchBatch2Rules.fuseDelay(99));
    }
    @Test void timedEffectsPreserveZeroInfinityAndEveryLevel() {
        for (int level = 0; level <= 3; level++) {
            assertEquals(200 + 10 * level, ResearchBatch2Rules.effectDuration(200, level, true, false));
            assertEquals(200 - 10 * level, ResearchBatch2Rules.effectDuration(200, level, false, true));
            assertEquals(200, ResearchBatch2Rules.effectDuration(200, level, false, false));
            assertEquals(-1, ResearchBatch2Rules.effectDuration(-1, level, true, false));
            assertEquals(0, ResearchBatch2Rules.effectDuration(0, level, true, false));
        }
        assertEquals(Integer.MAX_VALUE, ResearchBatch2Rules.effectDuration(Integer.MAX_VALUE, 3, true, false));
        assertEquals(1, ResearchBatch2Rules.effectDuration(1, 3, false, true));
    }
    @Test void spiderAndEndermanActionsRequireBothPurchaseAndRelevantContext() {
        for (int level : new int[]{-1, 0, 1}) {
            assertEquals(level > 0, ResearchBatch2Rules.preventSpiderAction(level, true));
            assertFalse(ResearchBatch2Rules.preventSpiderAction(level, false));
            assertEquals(level > 0, ResearchBatch2Rules.preventEngagedTeleport(level, true));
            assertFalse(ResearchBatch2Rules.preventEngagedTeleport(level, false));
        }
    }
    @Test void flightDurationComposesWithExistingLifetimeRatherThanReplacingIt() {
        for (int level = 0; level <= 3; level++) {
            assertEquals(100 + 8 * level, ResearchBatch2Rules.flightLifetime(100, level));
            assertEquals(300 + 24 * level, ResearchBatch2Rules.flightLifetime(300, level));
        }
        assertEquals(Integer.MAX_VALUE, ResearchBatch2Rules.flightLifetime(Integer.MAX_VALUE, 3));
    }
}
