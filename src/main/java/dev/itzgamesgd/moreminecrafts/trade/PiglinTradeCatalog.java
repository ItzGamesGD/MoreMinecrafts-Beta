package dev.itzgamesgd.moreminecrafts.trade;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Datapack-driven Piglin offers. Each {@code data/<ns>/piglin_trade/offers/<name>.json} is one vanilla
 * {@link MerchantOffer} codec object (cost, optional second cost, result, max_uses ...). The shipped mod
 * contains NO offers: approved values are not available, so the catalog is empty until a datapack adds some.
 */
public final class PiglinTradeCatalog {
    public record Loaded(PiglinTradeConfig config, Map<Identifier, MerchantOffer> offers) {
        public static final Loaded EMPTY = new Loaded(PiglinTradeConfig.CLOSED, Map.of());
    }

    private static final FileToIdConverter OFFER_FILES = FileToIdConverter.json("piglin_trade/offers");
    private static final Identifier CONFIG_FILE = ModIds.id("piglin_trade/config.json");
    private static volatile Loaded current = Loaded.EMPTY;

    private PiglinTradeCatalog() {}

    public static Loaded current() { return current; }
    /** Test hook: replace the active catalog. */
    public static void setForTest(Loaded loaded) { current = loaded; }

    public static final class Listener extends SimplePreparableReloadListener<Loaded> {
        @Override
        protected Loaded prepare(ResourceManager manager, ProfilerFiller profiler) {
            PiglinTradeConfig config = PiglinTradeConfig.CLOSED;
            var configResource = manager.getResource(CONFIG_FILE);
            if (configResource.isPresent()) {
                config = parse(configResource.get(), PiglinTradeConfig.CODEC, CONFIG_FILE, PiglinTradeConfig.CLOSED);
            }
            Map<Identifier, MerchantOffer> sorted = new TreeMap<>();
            for (Map.Entry<Identifier, Resource> entry : OFFER_FILES.listMatchingResources(manager).entrySet()) {
                Identifier id = OFFER_FILES.fileToId(entry.getKey());
                MerchantOffer offer = parse(entry.getValue(), MerchantOffer.CODEC, id, null);
                if (offer == null) continue;
                if (offer.getDemand() != 0) {
                    MoreMinecrafts.LOGGER.warn("Piglin trade offer {} skipped: demand must be 0", id);
                    continue;
                }
                MerchantOffer fresh = offer.copy();
                fresh.resetUses();
                fresh.resetSpecialPriceDiff();
                sorted.put(id, fresh);
            }
            return new Loaded(config, java.util.Collections.unmodifiableMap(new LinkedHashMap<>(sorted)));
        }

        @Override
        protected void apply(Loaded loaded, ResourceManager manager, ProfilerFiller profiler) {
            current = loaded;
            MoreMinecrafts.LOGGER.info("Piglin trade catalog loaded: {} offers, unlock research={}",
                    loaded.offers().size(), loaded.config().requiredResearch().orElse("<none: closed>"));
        }

        private static <T> T parse(Resource resource, com.mojang.serialization.Codec<T> codec, Identifier id, T fallback) {
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                return codec.parse(JsonOps.INSTANCE, json).resultOrPartial(
                        error -> MoreMinecrafts.LOGGER.warn("Piglin trade file {} rejected: {}", id, error)).orElse(fallback);
            } catch (Exception e) {
                MoreMinecrafts.LOGGER.warn("Piglin trade file {} unreadable: {}", id, e.toString());
                return fallback;
            }
        }
    }
}
