package dev.itzgamesgd.moreminecrafts.special;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuccessfulHitCounterTest {
    @Test
    void triggersExactlyOnEveryTenthRecordedHit() {
        SuccessfulHitCounter counter = SuccessfulHitCounter.EMPTY;

        for (int hit = 1; hit <= 20; hit++) {
            SuccessfulHitCounter.Advance advance = counter.recordSuccessfulHit(10);
            assertEquals(hit % 10 == 0, advance.triggered());
            assertEquals(hit % 10, advance.counter().hitsTowardTrigger());
            counter = advance.counter();
        }
    }

    @Test
    void oneRecordCallCannotProduceDuplicateTriggers() {
        SuccessfulHitCounter counter = new SuccessfulHitCounter(9);

        SuccessfulHitCounter.Advance trigger = counter.recordSuccessfulHit(10);

        assertTrue(trigger.triggered());
        assertEquals(SuccessfulHitCounter.EMPTY, trigger.counter());
        SuccessfulHitCounter.Advance followingHit = trigger.counter().recordSuccessfulHit(10);
        assertFalse(followingHit.triggered());
        assertEquals(1, followingHit.counter().hitsTowardTrigger());
    }

    @Test
    void rejectsInvalidThresholdAndState() {
        assertThrows(IllegalArgumentException.class,
                () -> SuccessfulHitCounter.EMPTY.recordSuccessfulHit(0));
        assertThrows(IllegalArgumentException.class, () -> new SuccessfulHitCounter(-1));
        assertThrows(IllegalStateException.class,
                () -> new SuccessfulHitCounter(10).recordSuccessfulHit(10));
    }
}
