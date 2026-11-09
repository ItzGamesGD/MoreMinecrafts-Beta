package dev.itzgamesgd.moreminecrafts.colosseum;

/** Approved ST-05 numbers (current authority Q1-Q118, RESOLVE Q25-Q44, Stage 1 round-boundary amendment). No tuning values are invented here. */
public final class RoundPlan {
    private RoundPlan() {}

    public static final int ROUNDS = 7;
    public static final int MAX_ALIVE = 12;
    public static final int REPLENISH_AT_OR_BELOW = 6;
    public static final int WARNING_TICKS = 20;
    public static final int ORDINARY_INTERMISSION_TICKS = 200;
    public static final int SUPPLY_INTERMISSION_TICKS = 700;
    public static final int BLOCKAGE_TICKS = 1200;
    public static final long COOLDOWN_TICKS = 78_000L;
    /** Defaults are the approved 65 minutes / 180 seconds; tests may override via {@link Timing}. */
    public static final long LIMIT_MILLIS = 65L * 60_000L;
    public static final long GRACE_MILLIS = 180_000L;

    /** Wall-clock durations (RESOLVE Q27/Q30: the 65-minute cap and the 180-second grace run on elapsed real time). */
    public record Timing(long limitMillis, long graceMillis) {
        public static final Timing APPROVED = new Timing(LIMIT_MILLIS, GRACE_MILLIS);
    }

    public static int baseBudget(int round) {
        return switch (round) { case 1, 2 -> 20; case 3, 4 -> 32; case 5, 6 -> 48; default -> 0; };
    }

    /** M(n) = 1 + 0.6 (n - 1), in thousandths. */
    public static int multiplierMilli(int n) {
        if (n < 1) throw new IllegalArgumentException("n");
        return 1000 + 600 * (n - 1);
    }

    /** Budget x M(n) rounded to the nearest integer (half up): 20/32/44/56, 32/51/70/90, 48/77/106/134 for n = 1..4. */
    public static int scaledBudget(int round, int n) {
        long scaled = (long) baseBudget(round) * multiplierMilli(n);
        return (int) ((scaled + 500) / 1000);
    }

    public static int scoreCap(int round) { return round <= 2 ? 4 : round <= 4 ? 7 : 10; }

    public static boolean hasSupplies(int round) { return round == 3 || round == 5; }

    /** Interval after clearing {@code round} before the next starts (35 s after R3/R5, otherwise 10 s). */
    public static int intermissionAfter(int round) { return hasSupplies(round) ? SUPPLY_INTERMISSION_TICKS : ORDINARY_INTERMISSION_TICKS; }

    /** Immortal Gladiator maximum HP: 300 plus 35% per additional participant locked at round 7 start (300/405/510/615). */
    public static int bossMaxHp(int n) { return 300 + (int) Math.round(300 * 0.35 * (n - 1)); }
}
