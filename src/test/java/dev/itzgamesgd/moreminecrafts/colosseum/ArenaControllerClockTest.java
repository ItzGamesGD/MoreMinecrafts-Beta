package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The per-controller time seam: production ticks on the server reading; an installed clock is private to its controller and never saved. */
class ArenaControllerClockTest {
    @Test
    void productionControllerTicksOnTheServerReading() {
        ArenaController c = new ArenaController("prod");
        assertNull(c.clockOverride());
        assertEquals(123_456L, c.now(123_456L));
    }

    @Test
    void installedClockIsPrivateToItsControllerAndIsNeverPersisted() {
        ArenaController owned = new ArenaController("owned"), other = new ArenaController("other");
        long[] t = {1_000L};
        owned.useClock(() -> t[0]);
        assertEquals(1_000L, owned.now(99L));
        assertEquals(99L, other.now(99L), "another controller keeps the server reading");
        t[0] = 2_500L;
        assertEquals(2_500L, owned.now(99L));
        assertEquals(99L, other.now(99L));

        ArenaController restored = ArenaController.restore("owned", owned.toJson(), null, 5_000L);
        assertNull(restored.clockOverride(), "a saved/restored controller falls back to the wall clock");
        assertEquals(5_000L, restored.now(5_000L));

        owned.useClock(null);
        assertEquals(77L, owned.now(77L), "releasing the clock restores the server reading");
    }
}
