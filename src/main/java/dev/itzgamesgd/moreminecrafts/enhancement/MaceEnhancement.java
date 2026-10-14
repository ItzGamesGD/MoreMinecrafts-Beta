package dev.itzgamesgd.moreminecrafts.enhancement;

/** Isolated PROVISIONAL values implementing the AUTHORITATIVE mace smash-scaling identity. */
public final class MaceEnhancement {
    public static final double FALL_DISTANCE_PER_LEVEL = 0.025D;
    public static final float SECONDARY_DAMAGE_FRACTION = 0.35F;
    private MaceEnhancement() {}

    public static double effectiveFallDistance(double actual, int level) {
        int bounded = Math.max(0, Math.min(EnhancementRules.ORDINARY_MAX_LEVEL, level));
        return actual * (1.0D + FALL_DISTANCE_PER_LEVEL * bounded);
    }
    public static double radius(int level) {
        return switch (EnhancementRules.milestoneCount(level)) {
            case 0 -> 0.0D; case 1 -> 1.5D; case 2 -> 2.0D; case 3 -> 2.5D; default -> 3.0D;
        };
    }
    public static int secondaryTargetCap(int level) {
        return EnhancementRules.milestoneCount(level);
    }
    /** Vanilla's piecewise smash bonus, exposed only for deterministic regression tests. */
    public static double vanillaSmashBonus(double fallDistance) {
        if (fallDistance <= 3.0D) return 4.0D * fallDistance;
        if (fallDistance <= 8.0D) return 12.0D + 2.0D * (fallDistance - 3.0D);
        return 22.0D + fallDistance - 8.0D;
    }
}
