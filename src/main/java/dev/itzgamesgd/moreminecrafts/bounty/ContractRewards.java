package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * Owner-approved additional-reward pool. Each rarity lists its items; the quantity band comes from
 * {@link ContractEconomy#quantity}, clamped by the item-specific cap. Caps are PROVISIONAL BALANCE.
 */
public final class ContractRewards {
    public record Pick(String item, int count) {}

    private static final Map<ContractEconomy.Rarity, List<String>> POOLS = Map.of(
            ContractEconomy.Rarity.COMMON, List.of("minecraft:coal", "minecraft:copper_ingot"),
            ContractEconomy.Rarity.HIGH_GRADE, List.of("minecraft:iron_ingot", "minecraft:gold_ingot"),
            ContractEconomy.Rarity.RARE, List.of("minecraft:diamond", "moreminecrafts:titanium_ingot"),
            ContractEconomy.Rarity.LEGENDARY, List.of("moreminecrafts:adamantium_ingot"));

    private static final Map<String, Integer> CAPS = Map.of(
            "minecraft:diamond", 2,
            "moreminecrafts:titanium_ingot", 4,
            "moreminecrafts:adamantium_ingot", 1);

    private ContractRewards() {}

    public static List<String> pool(ContractEconomy.Rarity rarity) {
        return POOLS.get(rarity);
    }

    /** Smallest and largest quantity this item may receive for the rarity, after the item cap. */
    public static int[] quantityRange(String item, ContractEconomy.Rarity rarity) {
        int[] band = ContractEconomy.quantity(rarity);
        int cap = CAPS.getOrDefault(item, Integer.MAX_VALUE);
        int max = Math.min(band[1], cap);
        return new int[]{Math.min(band[0], max), max};
    }

    /** Deterministic selection from explicit indices, used by tests and by {@link #roll}. */
    public static Pick select(ContractEconomy.Rarity rarity, int itemIndex, int count) {
        List<String> pool = POOLS.get(rarity);
        if (itemIndex < 0 || itemIndex >= pool.size()) throw new IllegalArgumentException("itemIndex");
        String item = pool.get(itemIndex);
        int[] range = quantityRange(item, rarity);
        if (count < range[0] || count > range[1]) throw new IllegalArgumentException("count");
        return new Pick(item, count);
    }

    public static Pick roll(ContractEconomy.Rarity rarity, RandomSource random) {
        List<String> pool = POOLS.get(rarity);
        String item = pool.get(random.nextInt(pool.size()));
        int[] range = quantityRange(item, rarity);
        return new Pick(item, range[0] + random.nextInt(range[1] - range[0] + 1));
    }
}
