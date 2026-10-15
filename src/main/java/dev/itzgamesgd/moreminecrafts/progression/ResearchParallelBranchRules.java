package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Arrays;

/** Pure rules retained while reconciling the two authorized Stage 23-A implementation branches. */
public final class ResearchParallelBranchRules {
    private static final int[][] FISH_GRADE_BASE = {
            {45, 30, 16, 7, 2}, {30, 32, 22, 12, 4},
            {10, 25, 32, 24, 9}, {0, 8, 34, 38, 20}
    };
    private static final int[][] FISH_GRADE_MAX = {
            {40, 32, 17, 8, 3}, {25, 33, 23, 13, 6},
            {7, 24, 33, 24, 12}, {0, 6, 33, 37, 24}
    };

    private ResearchParallelBranchRules() {}

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

    /**
     * Candidate only: preserves PR #92's largest-remainder allocation for review, but is not
     * connected to FishingLogic until the authority selects a residual-allocation rule.
     */
    public static int[] fisheriesLargestRemainderCandidate(int crankbaitTier, int researchLevel) {
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
