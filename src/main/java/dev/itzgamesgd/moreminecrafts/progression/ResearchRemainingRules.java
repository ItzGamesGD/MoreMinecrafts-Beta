package dev.itzgamesgd.moreminecrafts.progression;

/** Numeric contracts for approved remaining Stage 23-A effects. */
public final class ResearchRemainingRules {
    private ResearchRemainingRules() {}

    public static int nextSwordStage(int previousStage, long now, long lastHitTick) {
        return Math.min(5, (now >= lastHitTick && now - lastHitTick <= 100
                ? Math.max(0, previousStage) : 0) + 1);
    }

    public static double sweepMultiplier(int stage) {
        return 1.0D + 0.04D * Math.clamp(stage, 0, 5);
    }

    public static double splashRadius(int level) {
        return 4.0D * (100 + 10 * Math.clamp(level, 0, 3)) / 100.0D;
    }

    public static int lingeringDuration(int baseline, int level) {
        return ResearchPassiveMath.increasedDuration(baseline, 8 * Math.clamp(level, 0, 3));
    }
}
