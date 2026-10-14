package dev.itzgamesgd.moreminecrafts.k5probe;

import java.util.concurrent.atomic.AtomicInteger;

/** Test-only counters proving an independently injected wrapper runs with production K5 Mixins. */
public final class K5CooperatingMixinProbe {
    private static final AtomicInteger BLOCK_BREAKS = new AtomicInteger();
    private static final AtomicInteger LEVEL_MUTATIONS = new AtomicInteger();

    private K5CooperatingMixinProbe() {}

    public static void reset() {
        BLOCK_BREAKS.set(0);
        LEVEL_MUTATIONS.set(0);
    }

    public static void blockBreakWrapped() { BLOCK_BREAKS.incrementAndGet(); }
    public static void levelMutationWrapped() { LEVEL_MUTATIONS.incrementAndGet(); }
    public static int blockBreakCount() { return BLOCK_BREAKS.get(); }
    public static int levelMutationCount() { return LEVEL_MUTATIONS.get(); }
}
