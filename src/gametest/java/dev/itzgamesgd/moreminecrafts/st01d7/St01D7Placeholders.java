package dev.itzgamesgd.moreminecrafts.st01d7;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01MapTradeIndex;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * ST-01 P8-D7 experiment (not production): a non-purchasable placeholder offer that holds the slot of an ST-01 explorer-map
 * trade generated while search preparation is not ready, and its later replacement by the real offer.
 *
 * <p>Placeholder: {@code maxUses = 0} (permanently out of stock, also after restock), xp 0, a first cost no real item can
 * satisfy (exact custom-data predicate carrying the marker), and a harmless empty map as result, marked in custom data with
 * the trade id, the trade-set id and the map radius. The offer itself is the persisted state (villager "Offers" NBT).
 * Replacement runs the unchanged vanilla {@link VillagerTrade#getOffer} with the same loot context vanilla builds, in the
 * same slot. Inert unless {@link #enabled} is set.
 */
public final class St01D7Placeholders {
    private St01D7Placeholders() {}

    public static final String MARKER = "moreminecrafts_st01_pending_map";
    public static volatile boolean enabled = Boolean.getBoolean("mm.st01.d7.enable");
    /** Preload the first eligible target to STRUCTURE_STARTS before replacing (experiment switch, default off). */
    public static volatile boolean preload = Boolean.getBoolean("mm.st01.d7.preload");
    /** Fallback when the real trade finds no structure: "keep" (placeholder stays, unpurchasable) or "reroll" (vanilla-like replacement from the same set). */
    public static volatile String notFound = System.getProperty("mm.st01.d7.notFound", "keep");

    /** Whether a map search from {@code origin} with {@code radius} is ready (default: the C6 service reports READY and its warm-up is idle). */
    public static volatile BiPredicate<ServerLevel, BlockPos> readiness = (level, origin) -> defaultReady(level, origin);
    /** Tests only: produce the real offer instead of the vanilla trade (null = use the vanilla trade). */
    public static volatile BiFunction<Villager, CompoundTag, Optional<MerchantOffer>> resolverOverride;

    public static final AtomicLong PLACEHOLDERS_CREATED = new AtomicLong();
    public static final AtomicLong REPLACED = new AtomicLong();
    public static final AtomicLong NOT_FOUND = new AtomicLong();
    public static final AtomicLong PRELOADS = new AtomicLong();

    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);
    private static final Map<VillagerTrade, Optional<St01MapTradeIndex.MapSearch>> MAP_TRADES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Set<Villager> PENDING = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Villager, CompletableFuture<ChunkResult<ChunkAccess>>> PRELOADING = new WeakHashMap<>();
    public static TicketType TICKET;
    static int radiusForDefaultReady = 20;

    static void registerTicket() {
        if (TICKET == null)
            TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.fromNamespaceAndPath("moreminecrafts", "st01_map_target"),
                    new TicketType(200L, TicketType.FLAG_LOADING));
    }

    static boolean defaultReady(ServerLevel level, BlockPos origin) {
        var service = St01SearchPreparation.service();
        if (service == null) return false;
        var target = St01SearchPreparation.targetFor(level);
        return target.isPresent() && service.status(target.get(), origin, radiusForDefaultReady) == St01SearchPreparation.Status.READY && service.warmIdle();
    }

    // ----------------------------------------------------------------------------------------------- placeholder

    /** Called from the VillagerTrade.getOffer mixin: the placeholder to return instead of running the trade, or null. */
    public static MerchantOffer intercept(VillagerTrade trade, LootContext context) {
        if (!enabled || BYPASS.get()) return null;
        ServerLevel level = context.getLevel();
        Optional<St01MapTradeIndex.MapSearch> search = mapSearch(trade, level);
        if (search.isEmpty()) return null;
        Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
        if (origin == null || !(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Villager villager)) return null;
        BlockPos pos = BlockPos.containing(origin);
        if (readiness.test(level, pos)) return null; // ready: vanilla runs unchanged
        Optional<ResourceKey<VillagerTrade>> tradeKey = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getResourceKey(trade);
        if (tradeKey.isEmpty()) return null;
        String setId = tradeSetOf(level, villager).map(k -> k.identifier().toString()).orElse("");
        St01SearchPreparation.requestFor(level, pos, search.get().radius());
        synchronized (PENDING) {
            PENDING.add(villager);
        }
        PLACEHOLDERS_CREATED.incrementAndGet();
        return placeholder(tradeKey.get().identifier().toString(), setId, search.get().radius());
    }

    public static MerchantOffer placeholder(String tradeId, String tradeSetId, int radius) {
        CompoundTag marker = new CompoundTag();
        marker.putString("trade", tradeId);
        marker.putString("trade_set", tradeSetId);
        marker.putInt("radius", radius);
        CompoundTag root = new CompoundTag();
        root.put(MARKER, marker);
        CustomData data = CustomData.of(root);
        ItemStack result = new ItemStack(Items.MAP);
        result.set(DataComponents.CUSTOM_DATA, data);
        // no real item carries this exact custom data: MerchantOffer.satisfiedBy is always false
        ItemCost unsatisfiable = new ItemCost(BuiltInRegistries.ITEM.wrapAsHolder(Items.EMERALD), 1,
                DataComponentExactPredicate.builder().expect(DataComponents.CUSTOM_DATA, data).build());
        return new MerchantOffer(unsatisfiable, Optional.empty(), result, 0, 0, 0.0f);
    }

    public static Optional<CompoundTag> markerOf(MerchantOffer offer) {
        CustomData data = offer.getResult().get(DataComponents.CUSTOM_DATA);
        if (data == null) return Optional.empty();
        CompoundTag tag = data.copyTag();
        return tag.getCompound(MARKER);
    }

    public static boolean isPlaceholder(MerchantOffer offer) {
        return markerOf(offer).isPresent();
    }

    static Optional<St01MapTradeIndex.MapSearch> mapSearch(VillagerTrade trade, ServerLevel level) {
        return MAP_TRADES.computeIfAbsent(trade, t -> {
            Optional<? extends Holder<Structure>> st01 = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID);
            if (st01.isEmpty()) return Optional.empty();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
            List<St01MapTradeIndex.MapSearch> s = St01MapTradeIndex.searchesOf(t, ops, st01.get());
            return s.isEmpty() ? Optional.empty() : Optional.of(s.getFirst());
        });
    }

    static Optional<ResourceKey<TradeSet>> tradeSetOf(ServerLevel level, Villager v) {
        var data = v.getVillagerData();
        return Optional.ofNullable(data.profession().value().getTrades(data.level()));
    }

    // ----------------------------------------------------------------------------------------------- replacement

    public enum Outcome { NONE, REPLACED, NOT_FOUND_KEPT, NOT_FOUND_REROLLED, SKIPPED }

    /** Server thread: replaces every placeholder of {@code v} in place. */
    public static List<Outcome> replace(Villager v, ServerLevel level) {
        List<Outcome> out = new ArrayList<>();
        MerchantOffers offers = ((St01MerchantOffersAccessor) v).moreminecrafts$offers();
        if (offers == null || v.isRemoved()) return out;
        for (int i = 0; i < offers.size(); i++) {
            Optional<CompoundTag> marker = markerOf(offers.get(i));
            if (marker.isEmpty() || marker.get().getBooleanOr("unresolved", false)) continue;
            Optional<MerchantOffer> real = resolve(v, level, marker.get());
            if (real.isPresent()) {
                offers.set(i, real.get());
                REPLACED.incrementAndGet();
                out.add(Outcome.REPLACED);
                continue;
            }
            NOT_FOUND.incrementAndGet();
            Optional<MerchantOffer> reroll = notFound.equals("reroll") ? reroll(v, level, marker.get(), offers) : Optional.empty();
            if (reroll.isPresent()) {
                offers.set(i, reroll.get());
                out.add(Outcome.NOT_FOUND_REROLLED);
            } else {
                CompoundTag m = marker.get().copy();
                m.putBoolean("unresolved", true);
                offers.set(i, placeholderFrom(m));
                out.add(Outcome.NOT_FOUND_KEPT);
            }
        }
        if (!out.isEmpty()) sync(v, offers);
        return out;
    }

    static MerchantOffer placeholderFrom(CompoundTag marker) {
        MerchantOffer p = placeholder(marker.getStringOr("trade", ""), marker.getStringOr("trade_set", ""), marker.getIntOr("radius", 0));
        CompoundTag root = new CompoundTag();
        root.put(MARKER, marker);
        p.getResult().set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return p;
    }

    /** The vanilla offer of the recorded trade, in the loot context vanilla's addOffersFromTradeSet builds. */
    static Optional<MerchantOffer> resolve(Villager v, ServerLevel level, CompoundTag marker) {
        if (resolverOverride != null) return resolverOverride.apply(v, marker);
        Optional<Holder.Reference<VillagerTrade>> trade = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
                .get(ResourceKey.create(Registries.VILLAGER_TRADE, Identifier.parse(marker.getStringOr("trade", "minecraft:none"))));
        if (trade.isEmpty()) return Optional.empty();
        return Optional.ofNullable(runVanilla(trade.get().value(), v, level, marker));
    }

    static MerchantOffer runVanilla(VillagerTrade trade, Villager v, ServerLevel level, CompoundTag marker) {
        Optional<Identifier> sequence = Optional.empty();
        String setId = marker.getStringOr("trade_set", "");
        if (!setId.isEmpty()) {
            sequence = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).get(ResourceKey.create(Registries.TRADE_SET, Identifier.parse(setId)))
                    .flatMap(h -> h.value().randomSequence());
        }
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, v.position())
                .withParameter(LootContextParams.THIS_ENTITY, v).withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
        LootContext context = new LootContext.Builder(params).create(sequence);
        BYPASS.set(true);
        try {
            return trade.getOffer(context);
        } finally {
            BYPASS.set(false);
        }
    }

    /** Vanilla-like replacement for a map that found nothing: another trade of the same set (not the map trade itself). */
    static Optional<MerchantOffer> reroll(Villager v, ServerLevel level, CompoundTag marker, MerchantOffers offers) {
        String setId = marker.getStringOr("trade_set", "");
        if (setId.isEmpty()) return Optional.empty();
        var set = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).get(ResourceKey.create(Registries.TRADE_SET, Identifier.parse(setId)));
        if (set.isEmpty()) return Optional.empty();
        List<Holder<VillagerTrade>> candidates = new ArrayList<>();
        for (Holder<VillagerTrade> h : set.get().value().trades())
            if (h.unwrapKey().map(k -> !k.identifier().toString().equals(marker.getStringOr("trade", ""))).orElse(true)) candidates.add(h);
        while (!candidates.isEmpty()) {
            Holder<VillagerTrade> pick = candidates.remove(v.getRandom().nextInt(candidates.size()));
            MerchantOffer o = runVanilla(pick.value(), v, level, marker);
            if (o != null) return Optional.of(o);
        }
        return Optional.empty();
    }

    /** Resends the offers to the trading player and recomputes the result slot. */
    static void sync(Villager v, MerchantOffers offers) {
        if (v.getTradingPlayer() instanceof ServerPlayer p && p.containerMenu instanceof MerchantMenu menu) {
            menu.updateSellItem();
            p.sendMerchantOffers(menu.containerId, offers, v.getVillagerData().level(), v.getVillagerXp(), v.showProgressBar(), v.canRestock());
        }
    }

    // ----------------------------------------------------------------------------------------------- server wiring

    /** Registers a villager loaded with placeholders (persisted state) for re-preparation and replacement. */
    static void onLoad(Villager v, ServerLevel level) {
        if (!enabled) return;
        MerchantOffers offers = ((St01MerchantOffersAccessor) v).moreminecrafts$offers();
        if (offers == null) return;
        for (MerchantOffer o : offers) {
            Optional<CompoundTag> m = markerOf(o);
            if (m.isEmpty() || m.get().getBooleanOr("unresolved", false)) continue;
            St01SearchPreparation.requestFor(level, v.blockPosition(), m.get().getIntOr("radius", 20));
            synchronized (PENDING) {
                PENDING.add(v);
            }
            return;
        }
    }

    /** Server thread, each tick: at most one replacement; optional preload of the prepared target first. */
    static void tick() {
        if (!enabled) return;
        List<Villager> snapshot;
        synchronized (PENDING) {
            snapshot = new ArrayList<>(PENDING);
        }
        for (Villager v : snapshot) {
            if (v.isRemoved() || !(v.level() instanceof ServerLevel level)) { drop(v); continue; }
            St01SearchPreparation.requestFor(level, v.blockPosition(), radiusForDefaultReady); // keeps the job alive across moves (deduplicated)
            if (!readiness.test(level, v.blockPosition())) continue;
            if (preload && !preloaded(v, level)) continue;
            replace(v, level);
            drop(v);
            return;
        }
    }

    static void drop(Villager v) {
        synchronized (PENDING) {
            PENDING.remove(v);
        }
        PRELOADING.remove(v);
    }

    /** Adds a short-lived, non-persistent ticket at the STRUCTURE_STARTS level and schedules generation without blocking. */
    static boolean preloaded(Villager v, ServerLevel level) {
        CompletableFuture<ChunkResult<ChunkAccess>> f = PRELOADING.get(v);
        if (f == null) {
            var service = St01SearchPreparation.service();
            var target = St01SearchPreparation.targetFor(level);
            Optional<ChunkPos> chunk = service == null || target.isEmpty() ? Optional.empty()
                    : service.result(target.get(), v.blockPosition(), radiusForDefaultReady).flatMap(St01SearchPreparation.Result::firstEligible);
            if (chunk.isEmpty()) return true; // nothing to preload (not found): replace directly
            level.getChunkSource().addTicket(new Ticket(TICKET, ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS)), chunk.get());
            f = level.getChunkSource().getChunkFuture(chunk.get().x(), chunk.get().z(), ChunkStatus.STRUCTURE_STARTS, false);
            PRELOADING.put(v, f);
            PRELOADS.incrementAndGet();
            return false;
        }
        return f.isDone();
    }

    public static int pendingCount() {
        synchronized (PENDING) {
            return PENDING.size();
        }
    }

    public static void reset() {
        synchronized (PENDING) {
            PENDING.clear();
        }
        PRELOADING.clear();
        MAP_TRADES.clear();
        resolverOverride = null;
        readiness = St01D7Placeholders::defaultReady;
    }
}
