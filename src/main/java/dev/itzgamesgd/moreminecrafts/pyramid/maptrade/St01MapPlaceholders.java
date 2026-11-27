package dev.itzgamesgd.moreminecrafts.pyramid.maptrade;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import dev.itzgamesgd.moreminecrafts.mixin.St01VillagerTradeAccessor;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapTargetReadiness.Pending;
import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapTargetReadiness.State;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01MapTradeIndex;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * ST-01 P8-D8: non-blocking ST-01 explorer-map offers.
 *
 * <p>When an ST-01 map offer is generated (the code-added level-4 cartographer trade, or any ST-01 explorer-map trade a
 * data pack puts in a vanilla trade pool) and {@link St01MapTargetReadiness} does not report READY, the slot receives a
 * <b>placeholder</b> instead of running the search: {@code maxUses = 0} (out of stock forever, also after a restock), xp 0,
 * a result that is a plain empty map, and costs that show the real price but can never be paid (each requires an exact
 * custom-data marker no real item carries, so {@code MerchantOffer.satisfiedBy} and {@code take} always fail and it never
 * becomes the active offer of a merchant menu). The marker (trade id, trade-set id, radius, whether the offer was appended,
 * creation game time) is the persisted state: it is saved with the villager's offers.
 *
 * <p>Each tick, pending villagers are evaluated (preparation, target preload); when READY, the placeholder is replaced in
 * the same slot by the unchanged vanilla {@code VillagerTrade.getOffer} in the loot context vanilla builds (at most one per
 * tick), and an open trading screen is resynchronised. If the search finds nothing, preparation keeps failing
 * ({@value #MAX_FAILURES} failures with back-off) or the placeholder is older than {@value #DEADLINE_TICKS} ticks, the slot
 * receives another trade of the same trade set instead (no explorer-map trade, no duplicate of an existing offer); if there
 * is none, an appended slot is removed and a pool slot keeps an unpurchasable placeholder.
 *
 * <p>{@code -Dmm.st01.maptrade.blocking=true} (or a disabled C6 service) restores the vanilla synchronous behaviour.
 */
public final class St01MapPlaceholders {
    private St01MapPlaceholders() {}

    public static final String MARKER = "moreminecrafts_st01_pending_map";
    public static final long DEADLINE_TICKS = 12_000L;
    public static final int MAX_FAILURES = 6;

    public static final AtomicLong CREATED = new AtomicLong();
    public static final AtomicLong REPLACED = new AtomicLong();
    public static final AtomicLong FALLBACK_TRADES = new AtomicLong();
    public static final AtomicLong REMOVED = new AtomicLong();
    public static final AtomicLong IMMEDIATE = new AtomicLong();

    /** Readiness test (production: {@link St01MapTargetReadiness#evaluate}; GameTests substitute it). */
    @FunctionalInterface
    public interface Readiness {
        State evaluate(Villager villager, ServerLevel level, int radius, Pending pending, boolean allowPreload);
    }

    public static volatile Readiness readiness = St01MapTargetReadiness::evaluate;

    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);
    private static final Map<VillagerTrade, Optional<St01MapTradeIndex.MapSearch>> MAP_SEARCHES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<Villager, Pending> PENDING = new WeakHashMap<>();

    public enum Outcome { REPLACED, FALLBACK_TRADE, REMOVED, KEPT_UNPURCHASABLE }

    static void initialize() {
        TicketType type = Registry.register(BuiltInRegistries.TICKET_TYPE, ModIds.id("st01_map_target"),
                new TicketType(St01MapTargetReadiness.TICKET_TIMEOUT, TicketType.FLAG_LOADING));
        St01MapTargetReadiness.registerTicket(type);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { if (entity instanceof Villager v) onLoad(v); });
        ServerTickEvents.END_SERVER_TICK.register(St01MapPlaceholders::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (PENDING) {
                PENDING.clear();
            }
            MAP_SEARCHES.clear();
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> MAP_SEARCHES.clear());
    }

    public static boolean nonBlocking() {
        return !Boolean.getBoolean("mm.st01.maptrade.blocking") && St01SearchPreparation.service() != null;
    }

    // ------------------------------------------------------------------------------------------------ creation

    /**
     * The offer for an ST-01 map trade being generated for {@code villager}: the real offer if the search is ready, a
     * fallback trade if nothing can be found, otherwise a placeholder. {@code offers} is the list the result will join.
     */
    public static MerchantOffer createOffer(Holder<VillagerTrade> trade, Villager villager, ServerLevel level, ResourceKey<TradeSet> tradeSet,
            boolean appended, MerchantOffers offers) {
        LootContext context = context(villager, level, tradeSet);
        Optional<St01MapTradeIndex.MapSearch> search = mapSearch(trade.value(), level);
        if (search.isEmpty() || !nonBlocking()) return runVanilla(trade.value(), context);
        Pending pending = new Pending();
        State state = readiness.evaluate(villager, level, search.get().radius(), pending, false);
        if (state == State.READY) {
            MerchantOffer real = runVanilla(trade.value(), context);
            if (real != null) {
                IMMEDIATE.incrementAndGet();
                return real;
            }
            state = State.NOT_FOUND;
        }
        if (state == State.NOT_FOUND) {
            FALLBACK_TRADES.incrementAndGet();
            return fallbackTrade(villager, level, tradeSet, offers);
        }
        MerchantOffer placeholder = placeholder(trade, context, tradeSet, search.get().radius(), appended, level.getGameTime());
        register(villager, pending);
        return placeholder;
    }

    /** From the {@code VillagerTrade.getOffer} mixin: a placeholder for an ST-01 map trade of a vanilla trade pool, or null. */
    public static MerchantOffer intercept(VillagerTrade trade, LootContext context) {
        if (BYPASS.get() || !nonBlocking()) return null;
        ServerLevel level = context.getLevel();
        Optional<St01MapTradeIndex.MapSearch> search = mapSearch(trade, level);
        if (search.isEmpty() || !(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Villager villager)) return null;
        Pending pending = new Pending();
        State state = readiness.evaluate(villager, level, search.get().radius(), pending, false);
        if (state != State.NOT_READY) return null; // ready, or nothing to find: vanilla runs without a long search
        Optional<Holder.Reference<VillagerTrade>> holder = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getResourceKey(trade)
                .flatMap(k -> level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).get(k));
        if (holder.isEmpty()) return null;
        var data = villager.getVillagerData();
        MerchantOffer placeholder = placeholder(holder.get(), context, data.profession().value().getTrades(data.level()), search.get().radius(), false,
                level.getGameTime());
        register(villager, pending);
        return placeholder;
    }

    static MerchantOffer placeholder(Holder<VillagerTrade> trade, LootContext context, ResourceKey<TradeSet> tradeSet, int radius, boolean appended, long since) {
        CompoundTag marker = new CompoundTag();
        marker.putString("trade", trade.unwrapKey().map(k -> k.identifier().toString()).orElse(""));
        marker.putString("trade_set", tradeSet == null ? "" : tradeSet.identifier().toString());
        marker.putInt("radius", radius);
        marker.putBoolean("appended", appended);
        marker.putLong("since", since);
        St01VillagerTradeAccessor acc = (St01VillagerTradeAccessor) (Object) trade.value();
        CREATED.incrementAndGet();
        return placeholderOffer(marker, acc.moreminecrafts$wants().toItemCost(context, 0),
                acc.moreminecrafts$additionalWants().map(c -> c.toItemCost(context, 0)));
    }

    static MerchantOffer placeholderOffer(CompoundTag marker, ItemCost shownA, Optional<ItemCost> shownB) {
        CompoundTag root = new CompoundTag();
        root.put(MARKER, marker);
        CustomData data = CustomData.of(root);
        ItemStack result = new ItemStack(Items.MAP);
        result.set(DataComponents.CUSTOM_DATA, data);
        result.set(DataComponents.ITEM_NAME, Component.translatable("item.moreminecrafts.st01_pyramid_map.charting"));
        DataComponentExactPredicate never = DataComponentExactPredicate.builder().expect(DataComponents.CUSTOM_DATA, data).build();
        ItemCost a = new ItemCost(shownA.item(), shownA.count(), never);
        Optional<ItemCost> b = shownB.map(c -> new ItemCost(c.item(), c.count(), never));
        return new MerchantOffer(a, b, result, 0, 0, 0.0f);
    }

    public static Optional<CompoundTag> markerOf(MerchantOffer offer) {
        CustomData data = offer.getResult().get(DataComponents.CUSTOM_DATA);
        return data == null ? Optional.empty() : data.copyTag().getCompound(MARKER);
    }

    public static boolean isPlaceholder(MerchantOffer offer) {
        return markerOf(offer).isPresent();
    }

    static Optional<St01MapTradeIndex.MapSearch> mapSearch(VillagerTrade trade, ServerLevel level) {
        return MAP_SEARCHES.computeIfAbsent(trade, t -> {
            Optional<? extends Holder<Structure>> st01 = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID);
            if (st01.isEmpty()) return Optional.empty();
            List<St01MapTradeIndex.MapSearch> s = St01MapTradeIndex.searchesOf(t, ops(level), st01.get());
            return s.isEmpty() ? Optional.empty() : Optional.of(s.getFirst());
        });
    }

    static RegistryOps<JsonElement> ops(ServerLevel level) {
        return RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
    }

    // ------------------------------------------------------------------------------------------------ vanilla calls

    /** The loot context vanilla's {@code addOffersFromTradeSet} builds for a villager. */
    static LootContext context(Villager villager, ServerLevel level, ResourceKey<TradeSet> tradeSet) {
        Optional<Identifier> sequence = tradeSet == null ? Optional.empty()
                : level.registryAccess().lookupOrThrow(Registries.TRADE_SET).get(tradeSet).flatMap(h -> h.value().randomSequence());
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager).withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).create(sequence);
    }

    static MerchantOffer runVanilla(VillagerTrade trade, LootContext context) {
        boolean previous = BYPASS.get();
        BYPASS.set(true);
        try {
            return trade.getOffer(context);
        } finally {
            BYPASS.set(previous);
        }
    }

    /** Another trade of the same set for a slot whose map cannot be made: never an explorer map, never a duplicate. */
    static MerchantOffer fallbackTrade(Villager villager, ServerLevel level, ResourceKey<TradeSet> tradeSet, MerchantOffers offers) {
        if (tradeSet == null) return null;
        var set = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).get(tradeSet);
        if (set.isEmpty()) return null;
        RegistryOps<JsonElement> ops = ops(level);
        List<Holder<VillagerTrade>> candidates = new ArrayList<>();
        for (Holder<VillagerTrade> h : set.get().value().trades()) if (!St01MapTradeIndex.makesExplorerMap(h.value(), ops)) candidates.add(h);
        LootContext context = context(villager, level, tradeSet);
        while (!candidates.isEmpty()) {
            Holder<VillagerTrade> pick = candidates.remove(villager.getRandom().nextInt(candidates.size()));
            MerchantOffer o = runVanilla(pick.value(), context);
            if (o != null && !duplicates(offers, o)) return o;
        }
        return null;
    }

    static boolean duplicates(MerchantOffers offers, MerchantOffer o) {
        for (MerchantOffer existing : offers) {
            if (ItemStack.isSameItemSameComponents(existing.getResult(), o.getResult()) && existing.getItemCostA().item().equals(o.getItemCostA().item()))
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------ pending villagers

    static void register(Villager villager, Pending pending) {
        synchronized (PENDING) {
            PENDING.putIfAbsent(villager, pending);
        }
    }

    public static int pendingCount() {
        synchronized (PENDING) {
            return PENDING.size();
        }
    }

    /** A villager loaded with a placeholder (saved state) is pending again; its preparation starts over. */
    static void onLoad(Villager villager) {
        MerchantOffers offers = ((St01MerchantOffersAccessor) villager).moreminecrafts$offers();
        if (offers == null) return;
        for (MerchantOffer o : offers) {
            if (markerOf(o).filter(m -> !m.getBooleanOr("unresolved", false)).isPresent()) {
                register(villager, new Pending());
                return;
            }
        }
    }

    static void tick(MinecraftServer server) {
        List<Map.Entry<Villager, Pending>> snapshot;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            snapshot = new ArrayList<>(PENDING.entrySet());
        }
        boolean resolvedThisTick = false;
        for (Map.Entry<Villager, Pending> e : snapshot) {
            Villager v = e.getKey();
            Pending p = e.getValue();
            if (v.isRemoved() || !(v.level() instanceof ServerLevel level)) { drop(v); continue; }
            Optional<CompoundTag> marker = firstPending(v);
            if (marker.isEmpty()) { drop(v); continue; }
            long now = level.getGameTime();
            if (now < p.nextAttempt) continue;
            State state;
            boolean giveUp = now - marker.get().getLongOr("since", now) > DEADLINE_TICKS || p.failures > MAX_FAILURES;
            if (giveUp) state = State.NOT_FOUND;
            else state = readiness.evaluate(v, level, marker.get().getIntOr("radius", 100), p, true);
            if (state == State.NOT_READY || resolvedThisTick) continue;
            resolve(v, level, state == State.READY);
            resolvedThisTick = true;
            drop(v);
        }
    }

    static Optional<CompoundTag> firstPending(Villager v) {
        MerchantOffers offers = ((St01MerchantOffersAccessor) v).moreminecrafts$offers();
        if (offers == null) return Optional.empty();
        for (MerchantOffer o : offers) {
            Optional<CompoundTag> m = markerOf(o);
            if (m.isPresent() && !m.get().getBooleanOr("unresolved", false)) return m;
        }
        return Optional.empty();
    }

    static void drop(Villager v) {
        synchronized (PENDING) {
            PENDING.remove(v);
        }
    }

    /** Replaces every pending placeholder of {@code v} in place; {@code ready}: run the real trade first. */
    public static List<Outcome> resolve(Villager v, ServerLevel level, boolean ready) {
        List<Outcome> out = new ArrayList<>();
        MerchantOffers offers = ((St01MerchantOffersAccessor) v).moreminecrafts$offers();
        if (offers == null || v.isRemoved()) return out;
        var trades = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        for (int i = 0; i < offers.size(); i++) {
            Optional<CompoundTag> marker = markerOf(offers.get(i));
            if (marker.isEmpty() || marker.get().getBooleanOr("unresolved", false)) continue;
            String setId = marker.get().getStringOr("trade_set", "");
            ResourceKey<TradeSet> set = setId.isEmpty() ? null : ResourceKey.create(Registries.TRADE_SET, Identifier.parse(setId));
            MerchantOffer real = null;
            if (ready) {
                Optional<Holder.Reference<VillagerTrade>> trade = trades.get(ResourceKey.create(Registries.VILLAGER_TRADE,
                        Identifier.parse(marker.get().getStringOr("trade", "minecraft:none"))));
                if (trade.isPresent()) real = runVanilla(trade.get().value(), context(v, level, set));
            }
            if (real != null) {
                offers.set(i, real);
                REPLACED.incrementAndGet();
                out.add(Outcome.REPLACED);
                continue;
            }
            MerchantOffer other = fallbackTrade(v, level, set, offers);
            if (other != null) {
                offers.set(i, other);
                FALLBACK_TRADES.incrementAndGet();
                out.add(Outcome.FALLBACK_TRADE);
            } else if (marker.get().getBooleanOr("appended", false)) {
                offers.remove(i--);
                REMOVED.incrementAndGet();
                out.add(Outcome.REMOVED);
            } else {
                CompoundTag m = marker.get().copy();
                m.putBoolean("unresolved", true);
                MerchantOffer current = offers.get(i);
                offers.set(i, placeholderOffer(m, current.getItemCostA(), current.getItemCostB()));
                out.add(Outcome.KEPT_UNPURCHASABLE);
            }
        }
        if (!out.isEmpty()) sync(v, offers);
        return out;
    }

    /** Recomputes the result slot of an open trading screen and resends the offers to its player. */
    static void sync(Villager v, MerchantOffers offers) {
        if (v.getTradingPlayer() instanceof ServerPlayer player && player.containerMenu instanceof MerchantMenu menu) {
            menu.updateSellItem();
            player.sendMerchantOffers(menu.containerId, offers, v.getVillagerData().level(), v.getVillagerXp(), v.showProgressBar(), v.canRestock());
        }
    }

    /** Test seam: the placeholder for {@code trade} as {@link #createOffer} would build it. */
    public static MerchantOffer placeholderFor(Holder<VillagerTrade> trade, Villager villager, ServerLevel level, ResourceKey<TradeSet> tradeSet,
            boolean appended) {
        return placeholder(trade, context(villager, level, tradeSet), tradeSet, 100, appended, level.getGameTime());
    }

    /** Test seam: registers {@code villager} as pending. */
    public static void markPending(Villager villager) {
        register(villager, new Pending());
    }
}
