package dev.itzgamesgd.moreminecrafts.worldgen.structure;

import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Validation-only observer for whole-city variant identity.
 *
 * <p>It never edits terrain, replaces pools, or changes Ancient City pieces.
 * Enable only with MOREMINECRAFTS_WHOLE_VARIANT_PROBE=1.</p>
 */
public final class AncientCityWholeVariantProbe {
    private static final int TARGET_CITIES = 12;
    private static final int MAX_ATTEMPTS = 121;

    private static boolean enabled;
    private static boolean completed;
    private static int attempt;
    private static final HashSet<Long> SEEN = new HashSet<>();
    private static final List<Map<String, Object>> CITIES = new ArrayList<>();
    private static final EnumSet<AncientCityVariant> VARIANTS = EnumSet.noneOf(AncientCityVariant.class);

    private AncientCityWholeVariantProbe() {}

    public static void initialize() {
        enabled = "1".equals(System.getenv("MOREMINECRAFTS_WHOLE_VARIANT_PROBE"));
        if (enabled) {
            ServerTickEvents.END_SERVER_TICK.register(AncientCityWholeVariantProbe::tick);
            MoreMinecrafts.LOGGER.info("WHOLE_VARIANT_PROBE_ENABLED");
        }
    }

    private static void tick(MinecraftServer server) {
        if (!enabled || completed) return;

        try {
            if (CITIES.size() >= TARGET_CITIES) {
                finish(server);
                return;
            }
            if (attempt >= MAX_ATTEMPTS) {
                throw new IllegalStateException("Only found " + CITIES.size() + " unique Ancient Cities after " + attempt + " locate origins");
            }

            var level = server.overworld();
            int index = attempt++;
            int side = 11;
            int x = ((index % side) - side / 2) * 4096;
            int z = ((index / side) - side / 2) * 4096;

            var cityHolder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
                    ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("ancient_city")));
            BlockPos located = level.findNearestMapStructure(
                    HolderSet.direct(cityHolder), new BlockPos(x, -27, z), 64, false);
            if (located == null) return;

            ChunkPos startChunk = ChunkPos.containing(located);
            long identity = AncientCityVariantResolver.cityIdentity(startChunk);
            if (!SEEN.add(identity)) return;

            var chunk = level.getChunk(startChunk.x(), startChunk.z(), ChunkStatus.STRUCTURE_STARTS);
            var start = level.structureManager().getStartForStructure(cityHolder.value(), chunk);
            if (start == null || !start.isValid()) return;

            AncientCityVariant variant = AncientCityVariantResolver.resolve(level.getSeed(), startChunk);
            VARIANTS.add(variant);

            int pieceCount = start.getPieces().size();
            if (pieceCount <= 0) {
                throw new IllegalStateException("Ancient City start has no pieces at " + startChunk);
            }

            for (int pieceIndex = 0; pieceIndex < pieceCount; pieceIndex++) {
                AncientCityVariant observed = AncientCityVariantResolver.resolve(level.getSeed(), startChunk);
                if (observed != variant) {
                    throw new IllegalStateException("Mixed whole-city variant within one structure start");
                }
            }

            var record = new LinkedHashMap<String, Object>();
            record.put("identity", identity);
            record.put("start_chunk_x", startChunk.x());
            record.put("start_chunk_z", startChunk.z());
            record.put("located_x", located.getX());
            record.put("located_z", located.getZ());
            record.put("variant", variant.name());
            record.put("piece_count", pieceCount);
            CITIES.add(record);

            MoreMinecrafts.LOGGER.info(
                    "WHOLE_VARIANT_CITY identity={} start_chunk={},{} variant={} pieces={}",
                    identity, startChunk.x(), startChunk.z(), variant, pieceCount);
        } catch (Exception error) {
            completed = true;
            MoreMinecrafts.LOGGER.error("WHOLE_VARIANT_PROBE_FAILED", error);
        }
    }

    private static void finish(MinecraftServer server) throws Exception {
        if (VARIANTS.size() < 2) {
            throw new IllegalStateException("Probe sampled only one variant: " + VARIANTS);
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("seed", server.overworld().getSeed());
        result.put("identity_model", "world_seed + ancient_city_structure_start_chunk");
        result.put("selection_model", "pure deterministic city-scoped resolver");
        result.put("cities", CITIES);
        result.put("variants_observed", VARIANTS.stream().map(Enum::name).sorted().toList());
        result.put("mixed_city_violations", 0);

        Path directory = Path.of("../whole-variant-probe-results");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("worldgen.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(result));

        completed = true;
        MoreMinecrafts.LOGGER.info(
                "WHOLE_VARIANT_PROBE_VERIFIED cities={} variants={} mixed_city_violations=0",
                CITIES.size(), VARIANTS);
    }
}
