package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.Map;

/** PROVISIONAL BALANCE: owner-approved quantity exceptions, pending live measurement. */
public final class ContractBalance {
    public static final Map<String, int[]> DELIVERY_EXCEPTIONS = Map.of(
            "minecraft:diamond", new int[]{2, 4},
            "moreminecrafts:adamantium_ingot", new int[]{1, 1});

    private ContractBalance() {}
}
