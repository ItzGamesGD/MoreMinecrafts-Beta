package dev.itzgamesgd.moreminecrafts.trade;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Datapack-driven Piglin offers. Each {@code data/<ns>/piglin_trade/offers/<name>.json} is a
 * {@link PiglinTradeEntry}: {@code {"required_research": "<id>" (optional), "offer": <vanilla MerchantOffer codec>}}.
 * Reload only keeps the raw JSON: item components and enchantments need bound, registry-aware ops that
 * do not exist yet while data reloads, so offers are parsed on first use against the live registries.
 */
public final class PiglinTradeCatalog {
    public record Loaded(PiglinTradeConfig config, Map<Identifier, PiglinTradeEntry> offers) {
        public static final Loaded EMPTY = new Loaded(PiglinTradeConfig.CLOSED, Map.of());
    }

    /** Raw datapack contents captured at reload. */
    public record Raw(PiglinTradeConfig config, Map<Identifier, JsonElement> files) {
        public static final Raw EMPTY = new Raw(PiglinTradeConfig.CLOSED, Map.of());
    }

    private static final FileToIdConverter OFFER_FILES = FileToIdConverter.json("piglin_trade/offers");
    private static final Identifier CONFIG_FILE = ModIds.id("piglin_trade/config.json");
    private static volatile Raw raw = Raw.EMPTY;
    private static volatile Loaded parsed;
    private static volatile Loaded override;

    private PiglinTradeCatalog() {}

    /** Unlock gate; needs no registry access. */
    public static PiglinTradeConfig config() {
        Loaded forced = override;
        return forced != null ? forced.config() : raw.config();
    }

    /** Parsed offers, built once per reload against the supplied live registries. */
    public static Loaded current(HolderLookup.Provider registries) {
        Loaded forced = override;
        if (forced != null) return forced;
        Loaded cached = parsed;
        if (cached == null) {
            cached = parse(raw, registries);
            parsed = cached;
        }
        return cached;
    }

    /** Test hook: replace the active catalog; {@code null} restores the datapack-backed one. */
    public static void setForTest(Loaded loaded) { override = loaded; }

    public static Loaded parse(Raw source, HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Map<Identifier, PiglinTradeEntry> sorted = new TreeMap<>();
        for (Map.Entry<Identifier, JsonElement> file : source.files().entrySet()) {
            PiglinTradeEntry entry = PiglinTradeEntry.CODEC.parse(ops, file.getValue()).resultOrPartial(
                    error -> MoreMinecrafts.LOGGER.warn("Piglin trade offer {} rejected: {}", file.getKey(), error)).orElse(null);
            if (entry == null) continue;
            MerchantOffer offer = entry.offer();
            if (offer.getDemand() != 0) {
                MoreMinecrafts.LOGGER.warn("Piglin trade offer {} skipped: demand must be 0", file.getKey());
                continue;
            }
            MerchantOffer fresh = offer.copy();
            fresh.resetUses();
            fresh.resetSpecialPriceDiff();
            sorted.put(file.getKey(), new PiglinTradeEntry(entry.requiredResearch(), fresh));
        }
        MoreMinecrafts.LOGGER.info("Piglin trade catalog parsed: {} offers, unlock research={}", sorted.size(),
                source.config().requiredResearch().orElse("<none: closed>"));
        return new Loaded(source.config(), Collections.unmodifiableMap(new LinkedHashMap<>(sorted)));
    }

    public static final class Listener extends SimplePreparableReloadListener<Raw> {
        @Override
        protected Raw prepare(ResourceManager manager, ProfilerFiller profiler) { return load(manager); }

        static Raw load(ResourceManager manager) {
            PiglinTradeConfig config = PiglinTradeConfig.CLOSED;
            var configResource = manager.getResource(CONFIG_FILE);
            if (configResource.isPresent()) {
                JsonElement json = read(configResource.get(), CONFIG_FILE);
                if (json != null) config = PiglinTradeConfig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(
                        error -> MoreMinecrafts.LOGGER.warn("Piglin trade config rejected: {}", error)).orElse(PiglinTradeConfig.CLOSED);
            }
            Map<Identifier, JsonElement> files = new TreeMap<>();
            for (Map.Entry<Identifier, Resource> entry : OFFER_FILES.listMatchingResources(manager).entrySet()) {
                Identifier id = OFFER_FILES.fileToId(entry.getKey());
                JsonElement json = read(entry.getValue(), id);
                if (json != null) files.put(id, json);
            }
            return new Raw(config, Collections.unmodifiableMap(files));
        }

        @Override
        protected void apply(Raw loaded, ResourceManager manager, ProfilerFiller profiler) {
            raw = loaded;
            parsed = null;
            MoreMinecrafts.LOGGER.info("Piglin trade datapack read: {} offer files, unlock research={}",
                    loaded.files().size(), loaded.config().requiredResearch().orElse("<none: closed>"));
        }

        private static JsonElement read(Resource resource, Identifier id) {
            try (Reader reader = resource.openAsReader()) {
                return JsonParser.parseReader(reader);
            } catch (Exception e) {
                MoreMinecrafts.LOGGER.warn("Piglin trade file {} unreadable: {}", id, e.toString());
                return null;
            }
        }
    }
}
