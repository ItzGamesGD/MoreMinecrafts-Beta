package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Arrays;

/** Pure scaling rules for the unique Stage 23-A effects integrated from PR #92. */
public final class Research23AEffectRules {
    private static final int[][] FISH_GRADE_BASE = {
            {45, 30, 16, 7, 2}, {30, 32, 22, 12, 4},
            {10, 25, 32, 24, 9}, {0, 8, 34, 38, 20}
    };
    private static final int[][] FISH_GRADE_MAX = {
            {40, 32, 17, 8, 3}, {25, 33, 23, 13, 6},
            {7, 24, 33, 24, 12}, {0, 6, 33, 37, 24}
    };

    private Research23AEffectRules() {}

    public enum ExhaustionSource { SPRINTING, SWIMMING, OTHER }

    /** Returns the existing grade table adjusted only by the purchased Fisheries Study level. */
    public static int[] fishGradeWeights(int crankbaitTier, int researchLevel) {
        if (crankbaitTier < 0 || crankbaitTier >= FISH_GRADE_BASE.length)
            throw new IllegalArgumentException("Crankbait tier must be 0-3");
        int level = clamp(researchLevel, 0, 5);
        if (level == 0) return FISH_GRADE_BASE[crankbaitTier].clone();

        double[] interpolated = new double[FISH_GRADE_BASE[crankbaitTier].length];
        for (int grade = 0; grade < interpolated.length; grade++) {
            int base = FISH_GRADE_BASE[crankbaitTier][grade];
            int maximum = FISH_GRADE_MAX[crankbaitTier][grade];
            interpolated[grade] = base + (maximum - base) * level / 5.0;
        }
        return allocateLargestRemainder(interpolated);
    }

    /**
     * Floors each percentage, then allocates the points needed to reach 100 in descending
     * fractional-remainder order. Equal remainders favor the lower (earlier) grade index.
     */
    static int[] allocateLargestRemainder(double[] percentages) {
        if (percentages.length == 0) throw new IllegalArgumentException("At least one grade is required");
        double rawTotal = 0.0;
        int[] result = new int[percentages.length];
        double[] fractions = new double[percentages.length];
        int floorTotal = 0;
        for (int i = 0; i < percentages.length; i++) {
            double value = percentages[i];
            if (!Double.isFinite(value) || value < 0.0)
                throw new IllegalArgumentException("Grade percentages must be finite and nonnegative");
            rawTotal += value;
            result[i] = (int) Math.floor(value);
            fractions[i] = value - result[i];
            floorTotal += result[i];
        }
        if (Math.abs(rawTotal - 100.0) > 1.0e-9)
            throw new IllegalArgumentException("Interpolated grade percentages must sum to 100");

        Integer[] order = new Integer[percentages.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (left, right) -> {
            int remainderOrder = Double.compare(fractions[right], fractions[left]);
            return remainderOrder != 0 ? remainderOrder : Integer.compare(left, right);
        });
        int residual = 100 - floorTotal;
        if (residual < 0 || residual > order.length)
            throw new IllegalStateException("Largest-remainder allocation cannot reach 100");
        for (int i = 0; i < residual; i++) result[order[i]]++;
        return result;
    }

    public static double sprintSwimExhaustionMultiplier(int level) {
        return 1.0 - 0.08 * clamp(level, 0, 3);
    }

    /** Reduces only movement exhaustion; all other causes retain their original amount. */
    public static float exhaustionAmount(float amount, int level, ExhaustionSource source) {
        if (source == ExhaustionSource.OTHER) return amount;
        return amount * (float) sprintSwimExhaustionMultiplier(level);
    }

    public static double xpCollectionRadius(int level) {
        return switch (clamp(level, 0, 2)) {
            case 1 -> 4.0;
            case 2 -> 6.0;
            default -> 0.0;
        };
    }

    public static boolean withinXpCollectionRadius(double dx, double dy, double dz, int level) {
        double radius = xpCollectionRadius(level);
        return radius > 0.0 && dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
