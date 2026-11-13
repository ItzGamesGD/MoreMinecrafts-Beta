package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * P8-D6 C6: cooperative cancellation of an ST-01 decision that runs on the search-preparation worker.
 *
 * <p>Only the preparation worker installs a signal ({@link #enter}); on every other thread (server thread, worldgen
 * workers) {@link #check} is a no-op, so vanilla evaluations are never interrupted. A cancelled decision throws before it
 * is stored, so the C4 memo only ever holds complete, exact decisions.
 */
public final class St01Cancellation {
    private St01Cancellation() {}

    private static final ThreadLocal<BooleanSupplier> CURRENT = new ThreadLocal<>();

    static void enter(BooleanSupplier cancelled) { CURRENT.set(cancelled); }

    static void exit() { CURRENT.remove(); }

    /** Throws {@link CancellationException} if this thread's preparation was cancelled. */
    static void check() {
        BooleanSupplier s = CURRENT.get();
        if (s != null && s.getAsBoolean()) throw new CancellationException("ST-01 search preparation cancelled");
    }
}
