package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.mixin.St01VillagerTradeAccessor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P8-D6 C6: which villager trades can produce an ST-01 explorer map, read from the loaded data.
 *
 * <p>For every profession and every level of its {@code tradeSetsByLevel}, each trade's given-item modifier is encoded
 * with the vanilla loot-function codec and searched for {@code minecraft:exploration_map} functions whose destination
 * contains ST-01; their {@code search_radius} and {@code skip_existing_chunks} (vanilla defaults when absent) are recorded.
 * Nothing about professions, levels, radii or probabilities is assumed here: with no such trade in the data the index is
 * empty and search preparation is inert. Rebuilt on server start and after every data-pack reload.
 */
public final class St01MapTradeIndex {
    private St01MapTradeIndex() {}

    private static final Logger LOG = LoggerFactory.getLogger("moreminecrafts/st01-map-trades");

    /** An ST-01 explorer-map trade offered by {@code profession} when its trades for {@code level} are generated. */
    public record Entry(ResourceKey<VillagerProfession> profession, int level, int radius, boolean skipExisting) {}

    /** The explorer-map parameters of one {@code exploration_map} function. */
    public record MapSearch(int radius, boolean skipExisting) {}

    private static volatile List<Entry> entries = List.of();

    /** A trade offered outside the profession's trade sets (added by code when the level's trades are generated). */
    public record AppendedTrade(ResourceKey<VillagerProfession> profession, int level, ResourceKey<VillagerTrade> trade) {}

    private static final List<AppendedTrade> APPENDED = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void registerAppendedTrade(ResourceKey<VillagerProfession> profession, int level, ResourceKey<VillagerTrade> trade) {
        APPENDED.add(new AppendedTrade(profession, level, trade));
    }

    public static List<Entry> entries() { return entries; }

    public static void clear() { entries = List.of(); }

    public static void rebuild(RegistryAccess access) {
        try {
            entries = List.copyOf(scan(access));
        } catch (RuntimeException e) {
            LOG.warn("ST-01 map trade index could not be built; search preparation stays inert", e);
            entries = List.of();
        }
        if (!entries.isEmpty()) LOG.info("ST-01 explorer-map trades found: {}", entries);
    }

    /** All ST-01 explorer-map trades in {@code access}, in profession / level order. */
    public static List<Entry> scan(RegistryAccess access) {
        Optional<? extends Holder<Structure>> st01 = access.lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID);
        if (st01.isEmpty()) return List.of();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        var tradeSets = access.lookupOrThrow(Registries.TRADE_SET);
        Set<Entry> out = new LinkedHashSet<>();
        for (Holder.Reference<VillagerProfession> profession : access.lookupOrThrow(Registries.VILLAGER_PROFESSION).listElements().toList()) {
            for (var level : profession.value().tradeSetsByLevel().int2ObjectEntrySet()) {
                Optional<Holder.Reference<TradeSet>> set = tradeSets.get(level.getValue());
                if (set.isEmpty()) continue;
                for (Holder<VillagerTrade> trade : set.get().value().trades()) {
                    for (MapSearch m : searchesOf(trade.value(), ops, st01.get())) {
                        out.add(new Entry(profession.key(), level.getIntKey(), m.radius(), m.skipExisting()));
                    }
                }
            }
        }
        var trades = access.lookupOrThrow(Registries.VILLAGER_TRADE);
        for (AppendedTrade a : APPENDED) {
            trades.get(a.trade()).ifPresent(trade -> {
                for (MapSearch m : searchesOf(trade.value(), ops, st01.get())) out.add(new Entry(a.profession(), a.level(), m.radius(), m.skipExisting()));
            });
        }
        return new ArrayList<>(out);
    }

    /** Whether {@code trade} applies any explorer-map function (vanilla or ST-01): such trades run a structure search. */
    public static boolean makesExplorerMap(VillagerTrade trade, RegistryOps<JsonElement> ops) {
        Optional<Holder<LootItemFunction>> modifier = ((St01VillagerTradeAccessor) (Object) trade).moreminecrafts$givenItemModifier();
        if (modifier.isEmpty()) return false;
        Optional<JsonElement> json = LootItemFunctions.DIRECT_CODEC.encodeStart(ops, modifier.get().value()).result();
        return json.isPresent() && containsExplorationMap(json.get());
    }

    private static boolean containsExplorationMap(JsonElement json) {
        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) if (containsExplorationMap(e)) return true;
            return false;
        }
        if (!json.isJsonObject()) return false;
        if (isExplorationMap(json.getAsJsonObject())) return true;
        for (var e : json.getAsJsonObject().entrySet()) if (containsExplorationMap(e.getValue())) return true;
        return false;
    }

    /** The ST-01 explorer-map functions applied to what {@code trade} gives. */
    public static List<MapSearch> searchesOf(VillagerTrade trade, RegistryOps<JsonElement> ops, Holder<Structure> st01) {
        Optional<Holder<LootItemFunction>> modifier = ((St01VillagerTradeAccessor) (Object) trade).moreminecrafts$givenItemModifier();
        if (modifier.isEmpty()) return List.of();
        return searchesOf(modifier.get().value(), ops, st01);
    }

    /** The ST-01 explorer-map functions inside {@code function} (sequences and nested functions included). */
    public static List<MapSearch> searchesOf(LootItemFunction function, RegistryOps<JsonElement> ops, Holder<Structure> st01) {
        Optional<JsonElement> json = LootItemFunctions.DIRECT_CODEC.encodeStart(ops, function).result();
        List<MapSearch> out = new ArrayList<>();
        json.ifPresent(j -> walk(j, ops, st01, out));
        return out;
    }

    private static void walk(JsonElement json, RegistryOps<JsonElement> ops, Holder<Structure> st01, List<MapSearch> out) {
        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) walk(e, ops, st01, out);
            return;
        }
        if (!json.isJsonObject()) return;
        JsonObject o = json.getAsJsonObject();
        if (isExplorationMap(o) && o.has("destination")) {
            Optional<HolderSet<Structure>> destination = RegistryCodecs.holderSet(Registries.STRUCTURE).parse(ops, o.get("destination")).result();
            if (destination.isPresent() && destination.get().contains(st01)) {
                int radius = o.has("search_radius") ? o.get("search_radius").getAsInt() : ExplorationMapFunction.DEFAULT_SEARCH_RADIUS;
                boolean skip = o.has("skip_existing_chunks") ? o.get("skip_existing_chunks").getAsBoolean() : ExplorationMapFunction.DEFAULT_SKIP_EXISTING;
                out.add(new MapSearch(radius, skip));
            }
        }
        for (var e : o.entrySet()) walk(e.getValue(), ops, st01, out);
    }

    private static boolean isExplorationMap(JsonObject o) {
        if (!o.has("type") || !o.get("type").isJsonPrimitive()) return false;
        String type = o.get("type").getAsString();
        return type.equals("minecraft:exploration_map") || type.equals("exploration_map");
    }
}
