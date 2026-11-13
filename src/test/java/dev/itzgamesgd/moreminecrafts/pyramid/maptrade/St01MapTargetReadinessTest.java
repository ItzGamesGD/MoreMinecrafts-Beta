package dev.itzgamesgd.moreminecrafts.pyramid.maptrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** ST-01 P8-D8: the retry interval of a failing map preparation grows and is capped. */
class St01MapTargetReadinessTest {
    @Test
    void backoffDoublesFromFourSecondsAndIsCappedAtEightySeconds() {
        long[] expected = {80, 80, 160, 320, 640, 1280, 1600, 1600, 1600};
        for (int failures = 0; failures < expected.length; failures++)
            assertEquals(expected[failures], St01MapTargetReadiness.backoffTicks(failures), "failures=" + failures);
    }

    @Test
    void giveUpLimitsAreBounded() {
        long worst = 0;
        for (int f = 1; f <= St01MapPlaceholders.MAX_FAILURES; f++) worst += St01MapTargetReadiness.backoffTicks(f);
        // the retries alone end well before the absolute deadline
        org.junit.jupiter.api.Assertions.assertTrue(worst < St01MapPlaceholders.DEADLINE_TICKS, "worst-case retry span " + worst);
    }
}
