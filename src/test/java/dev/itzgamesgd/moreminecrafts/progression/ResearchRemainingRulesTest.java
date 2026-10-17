package dev.itzgamesgd.moreminecrafts.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchRemainingRulesTest {
    @Test void consecutiveSweepCapsAtFiveAndExpiresAfterFiveSeconds() {
        int stage = 0;
        for (int i = 1; i <= 7; i++) {
            stage = ResearchRemainingRules.nextSwordStage(stage, i * 10L, (i - 1) * 10L);
            assertEquals(Math.min(i, 5), stage);
            assertEquals(1.0D + 0.04D * Math.min(i, 5),
                    ResearchRemainingRules.sweepMultiplier(stage), 1.0e-9);
        }
        assertEquals(2, ResearchRemainingRules.nextSwordStage(1, 100, 0));
        assertEquals(1, ResearchRemainingRules.nextSwordStage(5, 101, 0));
        assertEquals(1.0D, ResearchRemainingRules.sweepMultiplier(0));
    }
    @Test void splashAndCloudUseEveryApprovedLevel() {
        for (int level = 0; level <= 3; level++) {
            assertEquals(4.0D * (100 + level * 10) / 100.0D,
                    ResearchRemainingRules.splashRadius(level));
            assertEquals(600 + 48 * level, ResearchRemainingRules.lingeringDuration(600, level));
        }
        assertEquals(4.0D, ResearchRemainingRules.splashRadius(-1));
        assertEquals(5.2D, ResearchRemainingRules.splashRadius(99));
        assertEquals(Integer.MAX_VALUE, ResearchRemainingRules.lingeringDuration(Integer.MAX_VALUE, 3));
    }
}
