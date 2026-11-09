package dev.itzgamesgd.moreminecrafts.progression;

/** Timing and scaling rules for the first-axe-strike research. */
public final class ResearchAxeCombatRules {
    public static final long REQUIRED_INACTIVE_TICKS = 60L;
    private ResearchAxeCombatRules() {}

    public static boolean isFirstStrike(long now, long previousAttackTick) {
        return previousAttackTick == Long.MIN_VALUE || now - previousAttackTick >= REQUIRED_INACTIVE_TICKS;
    }

    public static double bonus(int level, boolean firstStrike) {
        return firstStrike ? Math.max(0, Math.min(3, level)) * 0.03D : 0.0D;
    }
}
