package dev.itzgamesgd.moreminecrafts.bounty;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Deterministic seller stock. Each stock unit is one {@code maxUses = 1} offer whose result is a tier-only placeholder,
 * so the number of contract offers a seller shows equals its approved stock. Offers are only ever added, and a used
 * offer still counts, so repeated calls never create extra stock. Vanilla offers are never touched.
 *
 * <p>Vanilla restocking resets {@code uses} on every offer, contract offers included; this class does not change that.
 * Whether contract stock should restock is an open owner decision.
 */
public final class ContractTrades {
    private static final List<ContractData.Tier> SELLABLE = List.of(
            ContractData.Tier.BEGINNER, ContractData.Tier.INTERMEDIATE, ContractData.Tier.ADVANCED);

    private ContractTrades() {}

    /** Fixed purchase price at the midpoint of the approved band (PROVISIONAL): 3 / 8 / 16 emeralds. */
    public static int price(ContractData.Tier tier) {
        int[] band = ContractEconomy.band(tier);
        return (band[0] + band[1]) / 2;
    }

    public static ItemStack placeholder(ContractData.Tier tier) {
        ItemStack stack = new ItemStack(ModItems.BOUNTY_CONTRACT);
        stack.set(ModDataComponents.PENDING_CONTRACT, tier);
        return stack;
    }

    /** Price multiplier 0 and xp 0: no demand drift, and contract trades never level a villager. */
    public static MerchantOffer offer(ContractData.Tier tier) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, price(tier)), placeholder(tier), 1, 0, 0.0F);
    }

    /** Contract offers of {@code tier} in the list, including sold-out ones. */
    public static int count(MerchantOffers offers, ContractData.Tier tier) {
        int count = 0;
        for (MerchantOffer offer : offers)
            if (offer.getResult().get(ModDataComponents.PENDING_CONTRACT) == tier) count++;
        return count;
    }

    /**
     * Tops a villager up to the approved stock for every tier its profession and level unlock. Returns the number of
     * offers added. Safe to call at any time, including for villagers saved before this feature existed.
     */
    public static int ensureVillager(String professionId, int level, MerchantOffers offers) {
        int added = 0;
        for (ContractData.Tier tier : SELLABLE) {
            if (!ContractVendorPolicy.unlocked(professionId, level, tier)) continue;
            for (int missing = ContractVendorPolicy.villagerStock(tier) - count(offers, tier); missing > 0; missing--) {
                offers.add(offer(tier));
                added++;
            }
        }
        return added;
    }

    /**
     * Gives a wandering trader its stock once. Beginner always has at least one unit, so any existing Beginner offer
     * means the roll already happened. Each tier is a uniform draw inside its approved range.
     */
    public static int ensureWandering(MerchantOffers offers, RandomSource random) {
        if (count(offers, ContractData.Tier.BEGINNER) > 0) return 0;
        int added = 0;
        for (ContractData.Tier tier : SELLABLE) {
            int[] range = ContractVendorPolicy.wanderingStock(tier);
            int units = range[0] + random.nextInt(range[1] - range[0] + 1);
            for (int unit = 0; unit < units; unit++) {
                offers.add(offer(tier));
                added++;
            }
        }
        return added;
    }
}
