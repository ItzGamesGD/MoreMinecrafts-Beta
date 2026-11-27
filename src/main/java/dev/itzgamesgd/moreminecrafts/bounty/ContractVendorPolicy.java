package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.Set;

/** Approved vendor access and stock. Counts are offers per vendor, not use counts per offer. */
public final class ContractVendorPolicy {
    public static final Set<String> ELIGIBLE_PROFESSIONS = Set.of(
            "minecraft:farmer", "minecraft:fisherman", "minecraft:fletcher",
            "minecraft:weaponsmith", "minecraft:toolsmith", "minecraft:cartographer", "minecraft:cleric");
    private ContractVendorPolicy() {}

    public static boolean unlocked(String profession, int level, ContractData.Tier tier) {
        if (!ELIGIBLE_PROFESSIONS.contains(profession)) return false;
        return switch (tier) {
            case BEGINNER -> level >= 2;
            case INTERMEDIATE -> level >= 3;
            case ADVANCED -> level >= 4;
            case SPECIAL -> false;
        };
    }

    public static int[] wanderingStock(ContractData.Tier tier) {
        return switch (tier) {
            case BEGINNER -> new int[]{1, 2};
            case INTERMEDIATE, ADVANCED -> new int[]{0, 1};
            case SPECIAL -> new int[]{0, 0};
        };
    }

    public static int villagerStock(ContractData.Tier tier) {
        return switch (tier) {
            case BEGINNER -> 2;
            case INTERMEDIATE, ADVANCED -> 1;
            case SPECIAL -> 0;
        };
    }
}
