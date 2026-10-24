package dev.itzgamesgd.moreminecrafts.progression;

/** Numeric contracts for approved remaining Stage 23-A effects. */
public final class ResearchRemainingRules {
    private ResearchRemainingRules() {}

    // Vanilla 26.3 Horse natural ranges: speed (0.45..1.35)*0.25,
    // jump 0.4..1.0, health 15..30. Each bonus is 5% of its own width/level.
    public static double horseSpeedBonus(int level) { return 0.225D * 0.05D * Math.clamp(level, 0, 5); }
    public static double horseJumpBonus(int level) { return 0.6D * 0.05D * Math.clamp(level, 0, 5); }
    public static double horseHealthBonus(int level) { return 15.0D * 0.05D * Math.clamp(level, 0, 5); }

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
