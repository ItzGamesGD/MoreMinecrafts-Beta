package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** V3 Villager Trading (`v3.research.53`): a player-specific percentage off each offer's first cost. */
public final class ResearchVillagerDiscount {
    public static final String ID = "v3.research.53";

    private ResearchVillagerDiscount() {}

    /** Approved discount: 5/10/15% for purchased levels 1/2/3. */
    public static int discountPercent(int level) { return 5 * Math.clamp(level, 0, 3); }

    /** Rounds to the nearest emerald count (half up) and never goes below one. */
    public static int discountedCost(int cost, int level) {
        if (cost <= 1 || level <= 0) return cost;
        return Math.max(1, (cost * (100 - discountPercent(level)) + 50) / 100);
    }

    /**
     * Applies the discount on top of the offer prices vanilla has just rebuilt for {@code player}
     * (demand, reputation and Hero of the Village). Vanilla resets every special price before
     * rebuilding, so repeated menu opens and restocks never stack this reduction.
     */
    public static void apply(ServerPlayer player, MerchantOffers offers) {
        int level = ResearchBatch2Runtime.level(player, ID);
        if (level <= 0) return;
        for (MerchantOffer offer : offers) {
            if (T1Supply.isFixedBookOffer(offer)) continue;
            int current = offer.getCostA().getCount();
            int target = discountedCost(current, level);
            if (target < current) offer.addToSpecialPriceDiff(target - current);
        }
    }
}
