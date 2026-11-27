package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.function.LongSupplier;

/**
 * A test-owned, forward-only time source for one arena controller. Time moves only through {@link #advance}, so wall-clock timers
 * (absence grace, attempt limit) are decided by the test's explicit steps, never by host speed or server lag.
 */
final class ManualClock implements LongSupplier {
    private long now;

    ManualClock(long start) { now = start; }

    @Override public long getAsLong() { return now; }

    long now() { return now; }

    void advance(long millis) {
        if (millis < 0) throw new IllegalArgumentException("a manual arena clock never moves backwards: " + millis);
        now += millis;
    }
}
