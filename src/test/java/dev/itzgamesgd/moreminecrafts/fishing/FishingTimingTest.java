package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingTimingTest {
    @Test void difficultyControlsOnlyDeterministicMarkerSpeed() {
        assertEquals(1.8, FishingTiming.oneWaySeconds(1));
        assertEquals(1.5, FishingTiming.oneWaySeconds(2));
        assertEquals(1.25, FishingTiming.oneWaySeconds(3));
        assertEquals(1.0, FishingTiming.oneWaySeconds(4));
        assertEquals(0.8, FishingTiming.oneWaySeconds(5));
        assertEquals(FishingTiming.position(7,3), FishingTiming.position(57,3));
    }

    @Test void zonesClassifyAndApplyCaptureProbabilities() {
        assertEquals(FishingTiming.Zone.LIGHT_GREEN, FishingTiming.zone(.5));
        assertEquals(FishingTiming.Zone.YELLOW, FishingTiming.zone(.35));
        assertEquals(FishingTiming.Zone.ORANGE, FishingTiming.zone(.2));
        assertEquals(FishingTiming.Zone.RED, FishingTiming.zone(.01));
        for(int i=0;i<100;i++) assertTrue(FishingTiming.captures(FishingTiming.Zone.LIGHT_GREEN, RandomSource.create(i)));
        for(int i=0;i<100;i++) assertFalse(FishingTiming.captures(FishingTiming.Zone.RED, RandomSource.create(i)));
    }

    @Test void timeoutBoundaryIsExplicit() {
        assertFalse(FishingTiming.timedOut(FishingTiming.TIMEOUT_TICKS - 1));
        assertTrue(FishingTiming.timedOut(FishingTiming.TIMEOUT_TICKS));
    }
}
