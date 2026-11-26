package dev.itzgamesgd.moreminecrafts.progression;

/**
 * Pure deterministic rules for World loot research:
 * - `.52` Archaeology: brush time -10/-20/-30%, chest rare weight +3/+6/+9%, thematic extra roll 10/20/30%.
 * - `.54` Piglin Bartering: rare barter weight +5/+10/+15%.
 * - `piglin_barter` (V4): separate 1-level bonus barter roll (25% chance).
 */
public final class ResearchWorldLootRules {
    public static final double V4_PIGLIN_BARTER_BONUS_CHANCE = 0.25D;

    private ResearchWorldLootRules() {}

    public static int archaeologyBrushReductionPercent(int level) {
        return Math.clamp(level, 0, 3) * 10;
    }

    public static int archaeologyRequiredBrushes(int level) {
        return 10 - Math.clamp(level, 0, 3);
    }

    public static double archaeologyRareBonusChance(int level) {
        return Math.clamp(level, 0, 3) * 0.03D;
    }

    public static double archaeologyThematicExtraChance(int level) {
        return Math.clamp(level, 0, 3) * 0.10D;
    }

    public static boolean isArchaeologyStructureChest(String lootTablePath) {
        if (lootTablePath == null || lootTablePath.isBlank()) return false;
        return lootTablePath.contains("desert_pyramid")
                || lootTablePath.contains("jungle_temple")
                || lootTablePath.contains("ancient_city")
                || lootTablePath.contains("buried_treasure")
                || lootTablePath.contains("shipwreck_treasure")
                || lootTablePath.contains("underwater_ruin")
                || lootTablePath.contains("trail_ruins")
                || lootTablePath.contains("archaeology")
                || lootTablePath.contains("stronghold_library");
    }

    public static double piglinBarterRareChance(int level54) {
        return Math.clamp(level54, 0, 3) * 0.05D;
    }

    public static boolean rollsUnder(double roll, double chance) {
        return chance > 0.0D && roll >= 0.0D && roll < chance;
    }
}
