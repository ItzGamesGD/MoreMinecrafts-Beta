package dev.itzgamesgd.moreminecrafts.progression;

/** Deterministic level tables for the Stage 23-A event-based research effects. */
public final class ResearchContentRules {
    private ResearchContentRules() {}

    public static double gatheringChance(int level) {
        return switch (boundedLevel(level, 5)) {
            case 1 -> 0.10D;
            case 2, 3 -> 0.20D;
            case 4, 5 -> 0.30D;
            default -> 0.0D;
        };
    }

    public static int maximumGatheringExtra(int level) {
        return switch (boundedLevel(level, 5)) {
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            case 5 -> 3;
            default -> 0;
        };
    }

    public static int gatheringExtraCount(int level, double chanceRoll, double amountRoll) {
        int maximum = maximumGatheringExtra(level);
        if (maximum == 0 || !extraItemSucceeds(chanceRoll, gatheringChance(level))) return 0;
        return gatheringExtraCountAfterSuccess(level, amountRoll);
    }

    public static int gatheringExtraCountAfterSuccess(int level, double amountRoll) {
        int maximum = maximumGatheringExtra(level);
        return maximum == 0 ? 0 : 1 + (int) (clampRoll(amountRoll) * maximum);
    }

    public static double seedChance(int level) {
        return 0.15D * boundedLevel(level, 3);
    }

    public static double honeycombChance(int level) {
        return 0.20D * boundedLevel(level, 3);
    }

    public static boolean extraItemSucceeds(double roll, double chance) {
        return Double.isFinite(roll) && roll >= 0.0D && roll < chance;
    }

    public static int preservedMiningDamage(int amount, double roll, int level) {
        double chance = 0.05D * boundedLevel(level, 5);
        return amount > 0 && extraItemSucceeds(roll, chance) ? amount - 1 : amount;
    }

    public static int woodCraftOutputCount(int baseCount, int level, int maxStackSize) {
        if (baseCount <= 0 || maxStackSize <= 0) return Math.max(0, baseCount);
        return Math.min(maxStackSize, baseCount + boundedLevel(level, 2));
    }

    public static int repairBonusPercent(int level) {
        return switch (boundedLevel(level, 5)) {
            case 1, 2 -> 10;
            case 3, 4 -> 20;
            case 5 -> 30;
            default -> 0;
        };
    }

    public static int additionalRepairDurability(int maximumDurability, int repairMaterials, int level) {
        if (maximumDurability <= 0 || repairMaterials <= 0) return 0;
        int perMaterial = (int) Math.round(maximumDurability * repairBonusPercent(level) / 100.0D);
        return Math.max(0, perMaterial) * repairMaterials;
    }

    public static double anvilXpDiscount(int level) {
        return switch (boundedLevel(level, 5)) {
            case 2, 3 -> 0.045D;
            case 4, 5 -> 0.09D;
            default -> 0.0D;
        };
    }

    public static int discountAnvilLevels(int baseCost, int researchLevel) {
        if (baseCost <= 0) return Math.max(0, baseCost);
        return Math.max(1, (int) Math.round(baseCost * (1.0D - anvilXpDiscount(researchLevel))));
    }

    public static double fishingGreenHalfWidth(int level) {
        return Math.min(0.5D, 0.08D + 0.01D * boundedLevel(level, 3));
    }

    public static boolean inFishingGreenZone(double position, int level) {
        return Double.isFinite(position)
                && Math.abs(position - 0.5D) <= fishingGreenHalfWidth(level);
    }

    private static int boundedLevel(int level, int maximum) {
        return Math.max(0, Math.min(level, maximum));
    }

    private static double clampRoll(double roll) {
        if (!Double.isFinite(roll)) return 0.0D;
        return Math.max(0.0D, Math.min(Math.nextDown(1.0D), roll));
    }
}
