package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Pure deterministic rules for:
 * - `.14` Crop Cultivation (L1..L5 extra staple crop yield),
 * - `.63` Magical Material Study (10/20/30% extra eligible brewing ingredient),
 * - `.65` Catalyst Preservation (5/10/15% brewing catalyst preservation).
 */
public final class ResearchStage3HarvestRules {
    private ResearchStage3HarvestRules() {}

    /**
     * Approved `.14` Crop Cultivation extra harvest count:
     * L1: +1; L2: +2 at 40% else +1; L3: +2; L4: +3 at 30% else +2; L5: +3 at 50% else +2.
     */
    public static int cropCultivationExtra(int level, double roll) {
        return switch (Math.clamp(level, 0, 5)) {
            case 1 -> 1;
            case 2 -> roll >= 0.0D && roll < 0.40D ? 2 : 1;
            case 3 -> 2;
            case 4 -> roll >= 0.0D && roll < 0.30D ? 3 : 2;
            case 5 -> roll >= 0.0D && roll < 0.50D ? 3 : 2;
            default -> 0;
        };
    }

    public static double magicalMaterialChance(int level) {
        return Math.clamp(level, 0, 3) * 0.10D;
    }

    public static boolean magicalMaterialExtraSucceeds(int level, double roll) {
        double chance = magicalMaterialChance(level);
        return chance > 0.0D && roll >= 0.0D && roll < chance;
    }

    public static double catalystPreservationChance(int level) {
        return Math.clamp(level, 0, 3) * 0.05D;
    }

    public static boolean catalystPreserved(int level, double roll) {
        double chance = catalystPreservationChance(level);
        return chance > 0.0D && roll >= 0.0D && roll < chance;
    }
}
