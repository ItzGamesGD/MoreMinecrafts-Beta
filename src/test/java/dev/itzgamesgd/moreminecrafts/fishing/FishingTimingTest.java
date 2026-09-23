package dev.itzgamesgd.moreminecrafts.fishing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FishingTimingTest {
 @Test void pingPongIsDeterministicAndConstant(){assertEquals(0,FishingTiming.position(0));assertEquals(.5,FishingTiming.position(10));assertEquals(1,FishingTiming.position(20));assertEquals(.5,FishingTiming.position(30));assertEquals(0,FishingTiming.position(40));}
 @Test void centerBoundsAreInclusive(){assertTrue(FishingTiming.inSuccessWindow(FishingTiming.SUCCESS_MIN));assertTrue(FishingTiming.inSuccessWindow(FishingTiming.SUCCESS_MAX));assertTrue(FishingTiming.successful(10));assertFalse(FishingTiming.successful(5));assertFalse(FishingTiming.successful(15));}
 @Test void timeoutBoundaryIsExplicit(){assertFalse(FishingTiming.timedOut(FishingTiming.TIMEOUT_TICKS-1));assertTrue(FishingTiming.timedOut(FishingTiming.TIMEOUT_TICKS));}
}
