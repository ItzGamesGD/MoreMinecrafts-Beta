package dev.itzgamesgd.moreminecrafts.progression;

/** Numeric contracts for V3 Soil Study (`v3.research.16`). */
public final class SoilStudyRules {
    public static final int VANILLA_HYDRATION_REACH = 4;
    public static final int L3_HYDRATION_REACH = 6;
    public static final int FALLBACK_RESEARCHER_RADIUS = 16;
    public static final double L3_GROWTH_RELATIVE_BONUS = 0.20D;

    private SoilStudyRules() {}

    public static boolean tramplingResisted(int cultivatorLevel) { return cultivatorLevel >= 1; }

    /** Horizontal (Chebyshev) water reach in blocks; vanilla is 4. */
    public static int hydrationReach(int level) {
        return level >= 3 ? L3_HYDRATION_REACH : VANILLA_HYDRATION_REACH;
    }

    public static double growthRelativeBonus(int level) { return level >= 3 ? L3_GROWTH_RELATIVE_BONUS : 0.0D; }

    /**
     * L2 adds 30% of the eligible vanilla seed count: the whole part is guaranteed and the fractional
     * part is one additional seed with that probability. {@code roll} is uniform in [0, 1).
     */
    public static int seedYieldBonus(int eligibleSeeds, int level, double roll) {
        if (level < 2 || eligibleSeeds <= 0) return 0;
        int scaled = eligibleSeeds * 30;
        return scaled / 100 + (roll * 100.0D < scaled % 100 ? 1 : 0);
    }

    /** Composes independent relative probability bonuses once; a single bonus is returned unchanged. */
    public static double composeRelativeBonuses(double... bonuses) {
        double factor = 1.0D;
        double only = 0.0D;
        int active = 0;
        for (double bonus : bonuses) {
            if (!(bonus > 0.0D)) continue;
            factor *= 1.0D + bonus;
            only = bonus;
            active++;
        }
        return active == 0 ? 0.0D : active == 1 ? only : factor - 1.0D;
    }
}
