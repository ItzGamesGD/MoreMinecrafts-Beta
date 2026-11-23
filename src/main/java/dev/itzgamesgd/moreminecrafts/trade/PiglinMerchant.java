package dev.itzgamesgd.moreminecrafts.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Adapts a Piglin to vanilla's {@link Merchant} so the unmodified MerchantMenu/MerchantScreen handle
 * slots, list selection, shift-click and take-from-result. Every Piglin shares one offer pool and one
 * world-wide stock ({@link PiglinTradeStock}); this class only lists offers and records uses.
 * No XP orbs or villager-level progress are implemented (none approved).
 */
public final class PiglinMerchant implements Merchant {
    private final Piglin piglin;
    private final PiglinTradeStock stock;
    private final MerchantOffers offers = new MerchantOffers();
    private final List<String> ids = new ArrayList<>();
    private Player tradingPlayer;

    public PiglinMerchant(Piglin piglin, Map<Identifier, PiglinTradeEntry> catalog,
            ToIntFunction<String> researchLevel, PiglinTradeStock stock) {
        this.piglin = piglin;
        this.stock = stock;
        catalog.forEach((id, entry) -> {
            if (!entry.listedFor(researchLevel)) return;
            MerchantOffer offer = entry.offer().copy();
            int used = Math.min(stock.usesOf(id.toString()), offer.getMaxUses());
            for (int i = 0; i < used; i++) offer.increaseUses();
            offers.add(offer);
            ids.add(id.toString());
        });
    }

    public Piglin piglin() { return piglin; }

    @Override public void setTradingPlayer(Player player) { this.tradingPlayer = player; }
    @Override public Player getTradingPlayer() { return tradingPlayer; }
    @Override public MerchantOffers getOffers() { return offers; }
    @Override public void overrideOffers(MerchantOffers ignored) {}
    @Override public void overrideXp(int xp) {}
    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public boolean showProgressBar() { return false; }
    @Override public boolean canRestock() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.PIGLIN_ADMIRING_ITEM; }
    @Override public boolean isClientSide() { return piglin.level().isClientSide(); }

    @Override
    public boolean stillValid(Player player) {
        return piglin.isAlive() && !piglin.isRemoved() && getTradingPlayer() == player
                && player.distanceToSqr(piglin) <= 64.0;
    }

    /** Re-applies the shared stock to this session's offers (used after another session trades). */
    void syncFromStock() {
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            int want = Math.min(stock.usesOf(ids.get(i)), offer.getMaxUses());
            if (offer.getUses() != want) {
                offer.resetUses();
                for (int k = 0; k < want; k++) offer.increaseUses();
            }
        }
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i) == offer) {
                if (!isClientSide()) {
                    stock.recordUse(ids.get(i), offer.getUses());
                    PiglinTrading.syncOpenTrades(this);
                }
                break;
            }
        }
        piglin.makeSound(getNotifyTradeSound());
    }
}
