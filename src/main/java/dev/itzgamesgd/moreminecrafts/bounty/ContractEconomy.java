package dev.itzgamesgd.moreminecrafts.bounty;

/** Approved distribution thresholds. Rolls are integer uniform samples in the indicated range. */
public final class ContractEconomy {
    public enum Difficulty { EASY, MEDIUM, HARD }
    public enum Rarity { COMMON, HIGH_GRADE, RARE, LEGENDARY }
    private ContractEconomy() {}

    public static ContractData.Tier structureTier(Difficulty difficulty, int roll100) {
        if (roll100 < 0 || roll100 >= 100) throw new IllegalArgumentException("roll100");
        int beginner = switch (difficulty) { case EASY -> 75; case MEDIUM -> 45; case HARD -> 20; };
        int intermediate = switch (difficulty) { case EASY -> 22; case MEDIUM -> 40; case HARD -> 45; };
        return roll100 < beginner ? ContractData.Tier.BEGINNER
                : roll100 < beginner + intermediate ? ContractData.Tier.INTERMEDIATE : ContractData.Tier.ADVANCED;
    }

    public static boolean appears(Difficulty difficulty, int roll100) {
        if (roll100 < 0 || roll100 >= 100) throw new IllegalArgumentException("roll100");
        int chance = switch (difficulty) { case EASY -> 5; case MEDIUM -> 10; case HARD -> 18; };
        return roll100 < chance;
    }

    public static ContractData.Tier convertAdvanced(ContractData.Tier tier, int roll100) {
        if (roll100 < 0 || roll100 >= 100) throw new IllegalArgumentException("roll100");
        return tier == ContractData.Tier.ADVANCED && roll100 < 5 ? ContractData.Tier.SPECIAL : tier;
    }

    public static Rarity rarity(ContractData.Tier tier, int roll1000) {
        if (roll1000 < 0 || roll1000 >= 1000) throw new IllegalArgumentException("roll1000");
        int common, high, rare;
        switch (tier) {
            case BEGINNER -> { common = 800; high = 170; rare = 28; }
            case INTERMEDIATE -> { common = 600; high = 300; rare = 90; }
            case ADVANCED -> { common = 400; high = 400; rare = 170; }
            case SPECIAL -> { common = 200; high = 450; rare = 300; }
            default -> throw new IllegalArgumentException("tier");
        }
        if (roll1000 < common) return Rarity.COMMON;
        if (roll1000 < common + high) return Rarity.HIGH_GRADE;
        if (roll1000 < common + high + rare) return Rarity.RARE;
        return Rarity.LEGENDARY;
    }

    /**
     * purchase minimum/maximum, emerald payout minimum/maximum. A special contract produced by the
     * advanced-to-special conversion keeps the Advanced price and payout bands (owner decision, Oct 22).
     */
    public static int[] band(ContractData.Tier tier) {
        return switch (tier) {
            case BEGINNER -> new int[]{2, 4, 5, 10};
            case INTERMEDIATE -> new int[]{6, 10, 12, 24};
            case ADVANCED, SPECIAL -> new int[]{12, 20, 25, 50};
        };
    }

    public static int[] quantity(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> new int[]{16, 48};
            case HIGH_GRADE -> new int[]{8, 24};
            case RARE -> new int[]{2, 8};
            case LEGENDARY -> new int[]{1, 2};
        };
    }
}
