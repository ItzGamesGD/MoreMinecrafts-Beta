package dev.itzgamesgd.moreminecrafts.progression;

/** Small deterministic helpers shared by passive research handlers and their boundary tests. */
public final class ResearchPassiveMath {
    public record Fraction(int wholeBonus, int remainder) {}

    private ResearchPassiveMath() {}

    /** Accumulates percent as hundredths of one XP point; percent=1 means +1%. */
    public static Fraction accumulatePercent(int eligibleXp, int remainder, int percent) {
        if (eligibleXp <= 0 || percent <= 0) return new Fraction(0, Math.max(0, Math.min(99, remainder)));
        long hundredths = (long) eligibleXp * percent + Math.max(0, Math.min(99, remainder));
        return new Fraction((int) (hundredths / 100), (int) (hundredths % 100));
    }

    public static double multipliedProbability(double probability, double relativeBonus) {
        if (!Double.isFinite(probability) || probability <= 0.0D) return 0.0D;
        if (!Double.isFinite(relativeBonus) || relativeBonus <= 0.0D) return Math.min(1.0D, probability);
        return Math.min(1.0D, probability * (1.0D + relativeBonus));
    }

    /** Probability to add after an existing success test has failed, preserving p * 1.01 overall. */
    public static double conditionalExtraProbability(double originalProbability, double relativeBonus) {
        double target = multipliedProbability(originalProbability, relativeBonus);
        if (originalProbability >= 1.0D || target <= originalProbability) return 0.0D;
        return (target - originalProbability) / (1.0D - originalProbability);
    }

    public static int increasedDuration(int durationTicks, int percent) {
        if (durationTicks <= 0 || percent <= 0) return Math.max(0, durationTicks);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(durationTicks * (1.0D + percent / 100.0D)));
    }

    public static double scaleByBonus(double value, double bonus) {
        return value * (1.0D + Math.max(0.0D, bonus));
    }

    public static double reduceByPercent(double value, double reduction) {
        return value * Math.max(0.0D, 1.0D - Math.max(0.0D, reduction));
    }

    public static int preventDurabilityLoss(int amount, double roll, double chance) {
        return amount > 0 && roll >= 0.0D && roll < chance ? amount - 1 : amount;
    }
}
