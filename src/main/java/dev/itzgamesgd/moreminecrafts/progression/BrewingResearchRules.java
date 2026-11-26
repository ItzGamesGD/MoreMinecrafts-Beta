package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Deterministic arithmetic for Alchemy research:
 * `.59` Brewing Basics (-15/-30/-45% brew time),
 * `.60` Potion Study (+8/+16/+24% redstone duration; 10/20/30% glowstone penalty mitigation),
 * `.66` Advanced Brewing (25 brew operations per blaze powder vs vanilla 20).
 */
public final class BrewingResearchRules {
    public static final int VANILLA_BLAZE_POWDER_USES = 20;
    public static final int ADVANCED_BLAZE_POWDER_USES = 25;

    public record BrewStep(int nextBrewTime, double nextRemainder) {}

    private BrewingResearchRules() {}

    public static int fuelUsesPerBlazePowder(int level66) {
        return level66 > 0 ? ADVANCED_BLAZE_POWDER_USES : VANILLA_BLAZE_POWDER_USES;
    }

    public static double extraProgressPerTick(int level59) {
        int clamped = Math.clamp(level59, 0, 3);
        if (clamped == 0) return 0.0D;
        // Target brew time = 400 * (100 - 15 * level) / 100 -> rate = 100 / (100 - 15 * level).
        return (100.0D / (100 - 15 * clamped)) - 1.0D;
    }

    /**
     * Advances fractional extra ticks before vanilla decrements {@code brewTime} by 1. Keeps
     * {@code nextBrewTime >= 1} when {@code currentBrewTime > 1} so vanilla's final decrement reaches 0 cleanly.
     */
    public static BrewStep advanceBrewTime(int currentBrewTime, int level59, double remainder) {
        if (currentBrewTime <= 1 || level59 <= 0) {
            return new BrewStep(currentBrewTime, currentBrewTime <= 1 ? 0.0D : Math.max(0.0D, remainder));
        }
        double accumulated = Math.max(0.0D, remainder) + extraProgressPerTick(level59);
        int extra = (int) Math.floor(accumulated + 1.0E-9D);
        double nextRemainder = Math.max(0.0D, accumulated - extra);
        int applied = Math.min(currentBrewTime - 1, extra);
        return new BrewStep(currentBrewTime - applied, nextRemainder);
    }

    public static int redstoneExtendedDuration(int vanillaDuration, int level60) {
        int clamped = Math.clamp(level60, 0, 3);
        if (vanillaDuration <= 0 || clamped == 0) return Math.max(0, vanillaDuration);
        return ResearchPassiveMath.increasedDuration(vanillaDuration, clamped * 8);
    }

    public static int glowstoneMitigatedDuration(int beforeDuration, int afterDuration, int level60) {
        int clamped = Math.clamp(level60, 0, 3);
        if (clamped == 0 || afterDuration <= 0 || beforeDuration <= afterDuration) {
            return Math.max(0, afterDuration);
        }
        int penalty = beforeDuration - afterDuration;
        int restored = (int) Math.round(penalty * (clamped * 0.10D));
        return Math.min(beforeDuration, afterDuration + restored);
    }
}
