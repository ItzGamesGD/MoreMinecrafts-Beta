package dev.itzgamesgd.moreminecrafts.trade;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import com.mojang.serialization.Codec;
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
 * slots, list selection, shift-click and take-from-result. Vanilla's result slot performs the single
 * server-side consume + {@link #notifyTrade}; this class only supplies offers and persists stock.
 * No restock, XP orbs or villager-level progress are implemented (none approved).
 */
public final class PiglinMerchant implements Merchant {
    /** Per-Piglin used-count by offer id; persists with the entity. */
    public static final AttachmentType<Map<String, Integer>> USES = AttachmentRegistry.create(
            ModIds.id("piglin_trade_uses"), b -> b.initializer(Map::of)
                    .persistent(Codec.unboundedMap(Codec.STRING, Codec.INT)));

    private final Piglin piglin;
        private final MerchantOffers offers = new MerchantOffers();
    private final java.util.List<Identifier> ids = new java.util.ArrayList<>();
    private Player tradingPlayer;

    public PiglinMerchant(Piglin piglin, Map<Identifier, PiglinTradeEntry> catalog,
            java.util.function.ToIntFunction<String> researchLevel) {
        this.piglin = piglin;
        Map<String, Integer> uses = piglin.getAttachedOrCreate(USES);
        catalog.forEach((id, entry) -> {
            if (!entry.listedFor(researchLevel)) return;
            MerchantOffer offer = entry.offer().copy();
            int used = Math.max(0, Math.min(uses.getOrDefault(id.toString(), 0), offer.getMaxUses()));
            for (int i = 0; i < used; i++) offer.increaseUses();
            offers.add(offer);
            ids.add(id);
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

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        int index = -1;
        for (int i = 0; i < offers.size(); i++) if (offers.get(i) == offer) { index = i; break; }
        if (index >= 0 && !isClientSide()) {
            Map<String, Integer> next = new HashMap<>(piglin.getAttachedOrCreate(USES));
            next.put(ids.get(index).toString(), offer.getUses());
            piglin.setAttached(USES, Map.copyOf(next));
        }
        piglin.makeSound(getNotifyTradeSound());
    }
}
