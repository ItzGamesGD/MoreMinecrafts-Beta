package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Pure deterministic rules for Ecology AI research:
 * - `.36` Zombie reinforcement relative reduction (25/50/75%),
 * - `.37` Skeleton aim reset chance (20/30/40%),
 * - `.42` Illager movement speed reduction (-12%),
 * - `.43` Piglin anger prevention chance (30/45/60%).
 */
public final class ResearchEcologyAiRules {
    private ResearchEcologyAiRules() {}

    public static double zombieReinforcementReduction(int level) {
        return Math.clamp(level, 0, 3) * 0.25D;
    }

    public static double skeletonAimResetChance(int level) {
        return switch (Math.clamp(level, 0, 3)) {
            case 1 -> 0.20D;
            case 2 -> 0.30D;
            case 3 -> 0.40D;
            default -> 0.0D;
        };
    }

    public static boolean skeletonAimResetSucceeds(int level, double roll) {
        double chance = skeletonAimResetChance(level);
        return chance > 0.0D && roll >= 0.0D && roll < chance;
    }

    public static double illagerSpeedModifier(int level) {
        return level > 0 ? -0.12D : 0.0D;
    }

    public static double piglinAngerPreventionChance(int level) {
        return switch (Math.clamp(level, 0, 3)) {
            case 1 -> 0.30D;
            case 2 -> 0.45D;
            case 3 -> 0.60D;
            default -> 0.0D;
        };
    }

    public static boolean piglinAngerPrevented(int level, double roll) {
        double chance = piglinAngerPreventionChance(level);
        return chance > 0.0D && roll >= 0.0D && roll < chance;
    }
}
