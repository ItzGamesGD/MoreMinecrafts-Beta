package dev.itzgamesgd.moreminecrafts.alchemy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShockTimingTest {
    @Test
    void gameplayContractUsesSixtySecondsAndSixteenSecondPulses() {
        assertEquals(1_200, ShockTiming.DURATION_TICKS);
        assertEquals(320, ShockTiming.PULSE_INTERVAL_TICKS);
        assertEquals(20, ShockTiming.ROOT_DURATION_TICKS);
        assertEquals(1.0F, ShockTiming.PULSE_DAMAGE);
    }

    @Test
    void firstPulseOccursOnlyAfterFullInterval() {
        for (int remaining = ShockTiming.DURATION_TICKS;
                remaining > ShockTiming.DURATION_TICKS - ShockTiming.PULSE_INTERVAL_TICKS;
                remaining--) {
            assertFalse(ShockTiming.shouldPulse(remaining));
        }
        assertTrue(ShockTiming.shouldPulse(ShockTiming.DURATION_TICKS - ShockTiming.PULSE_INTERVAL_TICKS));
    }

    @Test
    void schedulesEveryCompleteIntervalWithinDuration() {
        assertTrue(ShockTiming.shouldPulse(880));
        assertTrue(ShockTiming.shouldPulse(560));
        assertTrue(ShockTiming.shouldPulse(240));
        assertFalse(ShockTiming.shouldPulse(879));
        assertFalse(ShockTiming.shouldPulse(0));
    }

    @Test
    void refreshToFullDurationRestartsPulseClock() {
        assertTrue(ShockTiming.shouldPulse(880));
        assertFalse(ShockTiming.shouldPulse(ShockTiming.DURATION_TICKS));
        assertFalse(ShockTiming.shouldPulse(ShockTiming.DURATION_TICKS - 1));
    }
}
