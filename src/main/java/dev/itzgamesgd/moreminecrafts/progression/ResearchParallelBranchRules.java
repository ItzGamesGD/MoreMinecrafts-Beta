package dev.itzgamesgd.moreminecrafts.progression;

/** Pure rules retained while reconciling the two authorized Stage 23-A implementation branches. */
public final class ResearchParallelBranchRules {
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

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(value, max)); }
}
