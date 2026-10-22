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
    private static final int FISH_GRADE_INTERPOLATION_DENOMINATOR = 5;
    private static final int FISH_GRADE_TOTAL = 100;
    private static final long FISH_GRADE_TOTAL_FIFTHS =
            (long) FISH_GRADE_TOTAL * FISH_GRADE_INTERPOLATION_DENOMINATOR;

    private Research23AEffectRules() {}

    public enum ExhaustionSource { SPRINTING, SWIMMING, OTHER }

    /** Returns the existing grade table adjusted only by the purchased Fisheries Study level. */
    public static int[] fishGradeWeights(int crankbaitTier, int researchLevel) {
        if (crankbaitTier < 0 || crankbaitTier >= FISH_GRADE_BASE.length)
            throw new IllegalArgumentException("Crankbait tier must be 0-3");
        int level = clamp(researchLevel, 0, 5);
        if (level == 0) return FISH_GRADE_BASE[crankbaitTier].clone();

        long[] interpolatedFifths = new long[FISH_GRADE_BASE[crankbaitTier].length];
        for (int grade = 0; grade < interpolatedFifths.length; grade++) {
            int base = FISH_GRADE_BASE[crankbaitTier][grade];
            int maximum = FISH_GRADE_MAX[crankbaitTier][grade];
            interpolatedFifths[grade] = (long) base * FISH_GRADE_INTERPOLATION_DENOMINATOR
                    + (long) (maximum - base) * level;
        }
        return allocateLargestRemainder(interpolatedFifths);
    }

    /**
     * Floors each percentage, then allocates the points needed to reach 100 in descending
     * fractional-remainder order. Equal remainders favor the lower (earlier) grade index.
     */
    static int[] allocateLargestRemainder(long[] percentageFifths) {
        if (percentageFifths.length == 0) throw new IllegalArgumentException("At least one grade is required");
        long rawTotalFifths = 0L;
        int[] result = new int[percentageFifths.length];
        int[] remainders = new int[percentageFifths.length];
        int floorTotal = 0;
        for (int i = 0; i < percentageFifths.length; i++) {
            long value = percentageFifths[i];
            if (value < 0L)
                throw new IllegalArgumentException("Grade percentages must be nonnegative");
            rawTotalFifths += value;
            result[i] = Math.toIntExact(value / FISH_GRADE_INTERPOLATION_DENOMINATOR);
            remainders[i] = (int) (value % FISH_GRADE_INTERPOLATION_DENOMINATOR);
            floorTotal += result[i];
        }
        if (rawTotalFifths != FISH_GRADE_TOTAL_FIFTHS)
            throw new IllegalArgumentException("Interpolated grade percentages must sum to 100");

        Integer[] order = new Integer[percentageFifths.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (left, right) -> {
            int remainderOrder = Integer.compare(remainders[right], remainders[left]);
            return remainderOrder != 0 ? remainderOrder : Integer.compare(left, right);
        });
        int residual = FISH_GRADE_TOTAL - floorTotal;
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
