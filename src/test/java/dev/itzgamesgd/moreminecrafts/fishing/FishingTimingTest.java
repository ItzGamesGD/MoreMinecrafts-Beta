package dev.itzgamesgd.moreminecrafts.fishing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingTimingTest {
    @Test void pingPongMovesOneStepEveryTick() {
        for (int tick = 0; tick < 20; tick++)
            assertEquals(1.0 / 20, FishingTiming.position(tick + 1)
                    - FishingTiming.position(tick), 0.0000001);
        for (int tick = 20; tick < 40; tick++)
            assertEquals(-1.0 / 20, FishingTiming.position(tick + 1)
                    - FishingTiming.position(tick), 0.0000001);
        assertEquals(FishingTiming.position(7), FishingTiming.position(47));
    }

    @Test void serverAndClientUseTheSameSynchronizedStartTick() {
        long start = 123456;
        long packetArrival = start + 5;
        long now = start + 10;
        assertEquals(0.5, FishingTiming.position(now - start));
        assertEquals(0.25, FishingTiming.position(now - packetArrival));
        assertNotEquals(FishingTiming.position(now - start),
                FishingTiming.position(now - packetArrival));
    }

    @Test void centerBoundsAreInclusive() {
        assertTrue(FishingTiming.inSuccessWindow(FishingTiming.SUCCESS_MIN));
        assertTrue(FishingTiming.inSuccessWindow(FishingTiming.SUCCESS_MAX));
        assertFalse(FishingTiming.inSuccessWindow(FishingTiming.SUCCESS_MIN - 0.0001));
        assertFalse(FishingTiming.inSuccessWindow(FishingTiming.SUCCESS_MAX + 0.0001));
        assertTrue(FishingTiming.successful(10));
        assertFalse(FishingTiming.successful(8));
        assertFalse(FishingTiming.successful(12));
    }

    @Test void timeoutBoundaryIsExplicit() {
        assertFalse(FishingTiming.timedOut(FishingTiming.TIMEOUT_TICKS - 1));
        assertTrue(FishingTiming.timedOut(FishingTiming.TIMEOUT_TICKS));
    }
}
