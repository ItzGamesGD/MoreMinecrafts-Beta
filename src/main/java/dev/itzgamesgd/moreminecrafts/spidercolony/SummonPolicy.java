package dev.itzgamesgd.moreminecrafts.spidercolony;

/** Owner-approved combat summon limits; lifecycle accounting remains the caller's responsibility. */
public final class SummonPolicy {
    public static final int INTERVAL_TICKS = 15 * 20;
    public static final int MAX_PER_CYCLE = 2;
    public static final int MAX_CONCURRENT = 4;
    private SummonPolicy() {}
    public static int allowed(boolean combat, int liveOwnedJuveniles) {
        if (!combat) return 0;
        return Math.min(MAX_PER_CYCLE, Math.max(0, MAX_CONCURRENT - Math.max(0, liveOwnedJuveniles)));
    }
}
