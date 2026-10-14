package dev.itzgamesgd.moreminecrafts.enhancement;

/** Provisional balance curves, isolated from the locked ordinary-progression structure. */
public enum GrowthCurve {
    NONE { @Override public double valueAt(int level) { return 0.0D; } },
    SWORD_DAMAGE { @Override public double valueAt(int level) {
        int n = clamp(level);
        return band(n, 0, 5, .10D) + band(n, 5, 10, .12D)
                + band(n, 10, 15, .14D) + band(n, 15, 20, .16D);
    } },
    RANGED_DAMAGE { @Override public double valueAt(int level) { return clamp(level) * .08D; } },
    SPEAR_DAMAGE { @Override public double valueAt(int level) { return clamp(level) * .09D; } },
    ARMOR_REDUCTION { @Override public double valueAt(int level) { return clamp(level) * .001D; } },
    TOOL_SPEED { @Override public double valueAt(int level) { return clamp(level) * .10D; } },
    AXE_DAMAGE { @Override public double valueAt(int level) { return clamp(level) * .10D; } };

    public abstract double valueAt(int level);
    private static int clamp(int level) { return Math.max(0, Math.min(level, EnhancementRules.ORDINARY_MAX_LEVEL)); }
    private static double band(int level, int start, int end, double rate) {
        return Math.max(0, Math.min(level, end) - start) * rate;
    }
}
