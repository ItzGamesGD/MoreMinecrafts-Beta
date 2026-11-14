package dev.itzgamesgd.moreminecrafts.pyramid.maptrade;

import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01MapTradeIndex;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.VillagerTrade;

/**
 * ST-01 P8-D8: the Colossal Pyramid explorer map sold by cartographers (owner decision 2026-11-13: cartographer level 4,
 * 16 emeralds + 1 compass, offered with probability 50 %).
 *
 * <p>The trade itself is data ({@code data/moreminecrafts/villager_trade/cartographer/4/emerald_and_compass_st01_pyramid_map.json},
 * vanilla explorer-map conventions: search radius 100, vanilla {@code skip_existing_chunks}, 12 uses, reputation discount 0.2).
 * It is not part of the vanilla level-4 trade pool: when a cartographer's level-4 trades are generated, the villager rolls
 * once (persisted) and, with probability {@link #CHANCE}, the map trade is added after the vanilla level-4 offers, so the
 * vanilla trades, their number and their order are unchanged. The offer is created by {@link St01MapPlaceholders}, which
 * never searches on the server thread before the search is prepared.
 */
public final class St01MapTrade {
    private St01MapTrade() {}

    public static final ResourceKey<VillagerTrade> TRADE = ResourceKey.create(Registries.VILLAGER_TRADE,
            Identifier.fromNamespaceAndPath("moreminecrafts", "cartographer/4/emerald_and_compass_st01_pyramid_map"));
    public static final ResourceKey<VillagerProfession> PROFESSION = VillagerProfession.CARTOGRAPHER;
    public static final int LEVEL = 4;
    /** Probability that a cartographer reaching level 4 offers the map (owner decision). */
    public static final double CHANCE = 0.5;

    /** Whether this villager has already rolled for the map trade (rolled once, at its level-4 trade generation). */
    public static final AttachmentType<Boolean> ROLLED = AttachmentRegistry.create(ModIds.id("st01_map_trade_rolled"), b -> b.persistent(Codec.BOOL));

    /** {@code -Dmm.st01.maptrade.chance} overrides {@link #CHANCE} (tests and benchmarks only). */
    static double chance() {
        String s = System.getProperty("mm.st01.maptrade.chance");
        if (s == null) return CHANCE;
        try {
            return Math.max(0, Math.min(1, Double.parseDouble(s)));
        } catch (NumberFormatException e) {
            return CHANCE;
        }
    }

    public static boolean disabled() {
        return Boolean.getBoolean("mm.st01.maptrade.disable");
    }

    public static void initialize() {
        St01MapTradeIndex.registerAppendedTrade(PROFESSION, LEVEL, TRADE);
        St01MapPlaceholders.initialize();
    }

    /** Called at the end of {@code Villager.updateTrades}: adds the map trade to a cartographer's level-4 trades (once, 50 %). */
    public static void onTradesGenerated(Villager villager) {
        if (disabled() || !(villager.level() instanceof ServerLevel level)) return;
        VillagerData data = villager.getVillagerData();
        if (data.level() != LEVEL || !data.profession().is(PROFESSION)) return;
        if (Boolean.TRUE.equals(villager.getAttached(ROLLED))) return;
        villager.setAttached(ROLLED, true);
        if (villager.getRandom().nextDouble() >= chance()) return;
        Optional<Holder.Reference<VillagerTrade>> trade = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).get(TRADE);
        MerchantOffers offers = ((St01MerchantOffersAccessor) villager).moreminecrafts$offers();
        if (trade.isEmpty() || offers == null) return;
        MerchantOffer offer = St01MapPlaceholders.createOffer(trade.get(), villager, level, data.profession().value().getTrades(LEVEL), true, offers);
        if (offer != null) offers.add(offer);
    }
}
