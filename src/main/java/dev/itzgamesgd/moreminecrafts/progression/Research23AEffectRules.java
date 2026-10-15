package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Arrays;

/** Pure scaling rules for the bounded Stage 23-A research batch. */
public final class Research23AEffectRules {
    private static final int[] LUMBERING_CHANCE = {0, 10, 20, 20, 30, 30};
    private static final int[] EXTRA_LOG_CAP = {0, 1, 1, 2, 2, 3};
    private static final int[] BEEKEEPING_CHANCE = {0, 20, 40, 60};
    private static final int[][] FISH_GRADE_BASE = {
            {45, 30, 16, 7, 2}, {30, 32, 22, 12, 4},
            {10, 25, 32, 24, 9}, {0, 8, 34, 38, 20}
    };
    private static final int[][] FISH_GRADE_MAX = {
            {40, 32, 17, 8, 3}, {25, 33, 23, 13, 6},
            {7, 24, 33, 24, 12}, {0, 6, 33, 37, 24}
    };

    private Research23AEffectRules() {}

    public static int lumberingChancePercent(int level) { return LUMBERING_CHANCE[clamp(level, 0, 5)]; }
    public static int maximumExtraLogs(int level) { return EXTRA_LOG_CAP[clamp(level, 0, 5)]; }
    public static int mineralConservationChancePercent(int level) { return clamp(level, 0, 5) * 5; }
    public static int beekeepingChancePercent(int level) { return BEEKEEPING_CHANCE[clamp(level, 0, 3)]; }
    public static boolean shouldGrantBonusHoneycomb(int level, double chanceRoll) {
        return chanceRoll >= 0.0 && chanceRoll < 1.0
                && chanceRoll < beekeepingChancePercent(level) / 100.0;
    }
    public static int rollExtraLogs(int level, double chanceRoll, int countRoll) {
        int cap = maximumExtraLogs(level);
        if (cap == 0 || chanceRoll < 0.0 || chanceRoll >= 1.0
                || chanceRoll >= lumberingChancePercent(level) / 100.0) return 0;
        return Math.floorMod(countRoll, cap) + 1;
    }
    public static double sprintSwimExhaustionMultiplier(int level) {
        return 1.0 - 0.08 * clamp(level, 0, 3);
    }
    public static double xpCollectionRadius(int level) {
        return switch (clamp(level, 0, 2)) { case 1 -> 4.0; case 2 -> 6.0; default -> 0.0; };
    }
    public static boolean withinXpCollectionRadius(double dx, double dy, double dz, int level) {
        double radius = xpCollectionRadius(level);
        return radius > 0.0 && dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public static boolean shouldPreventMiningDamage(int amount, double roll, int level) {
        return amount > 0 && roll >= 0.0 && roll < 1.0
                && roll < mineralConservationChancePercent(level) / 100.0;
    }

    /** Returns a copy; L0 is the existing distribution and L5 is the approved endpoint. */
    public static int[] fishGradeWeights(int crankbaitTier, int researchLevel) {
        if (crankbaitTier < 0 || crankbaitTier >= FISH_GRADE_BASE.length)
            throw new IllegalArgumentException("Crankbait tier must be 0-3");
        int level = clamp(researchLevel, 0, 5);
        if (level == 0) return FISH_GRADE_BASE[crankbaitTier].clone();
        double[] fractional = new double[FISH_GRADE_BASE[crankbaitTier].length];
        int[] result = new int[fractional.length];
        int total = 0;
        for (int i = 0; i < result.length; i++) {
            fractional[i] = FISH_GRADE_BASE[crankbaitTier][i]
                    + (FISH_GRADE_MAX[crankbaitTier][i] - FISH_GRADE_BASE[crankbaitTier][i]) * level / 5.0;
            result[i] = (int) Math.floor(fractional[i]);
            total += result[i];
        }
        Integer[] order = new Integer[result.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (left, right) -> {
            int remainderOrder = Double.compare(fractional[right] - Math.floor(fractional[right]),
                    fractional[left] - Math.floor(fractional[left]));
            return remainderOrder != 0 ? remainderOrder : Integer.compare(left, right);
        });
        for (int i = 0; i < 100 - total; i++) result[order[i]]++;
        return result;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(value, max)); }
}
