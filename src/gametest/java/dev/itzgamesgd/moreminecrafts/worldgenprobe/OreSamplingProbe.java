package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matched-sample Nether ore abundance probe (not production; inert unless {@code -Dmm.nether.ore.out} is set).
 *
 * <p>After the dedicated server starts it forces a square of Nether chunks to FULL with the real world
 * generator, then counts Ancient Debris and Adamantium Debris in the same interior chunks. Edge chunks are
 * generated but not counted, because feature placement writes into neighbouring chunks. Output is one JSON
 * document with the dimension, biome mix, scanned height range, per-Y histograms, cluster sizes and per-chunk
 * statistics, so the F-3 acceptance criterion ("fewer generated ore blocks than Ancient Debris in matched
 * samples") is measured instead of argued.
 */
public final class OreSamplingProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("ore-sampling-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MARGIN = 2;

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.nether.ore.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                run(server, Path.of(out));
            } catch (Throwable t) {
                LOG.error("ore sampling failed", t);
                try {
                    Files.createDirectories(Path.of(out).getParent());
                    Files.writeString(Path.of(out), GSON.toJson(Map.of("error", String.valueOf(t))), StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // the missing/invalid output file is itself the failure signal for the driver script
                }
            }
            // Results are on disk. Skip saving thousands of forced chunks; the world is throwaway.
            Runtime.getRuntime().halt(0);
        });
    }

    private static void run(MinecraftServer server, Path out) throws IOException {
        int radius = Integer.getInteger("mm.nether.ore.radius", 20); // chunks each way from the origin
        ServerLevel level = server.getLevel(Level.NETHER);
        if (level == null) throw new IllegalStateException("The Nether is not loaded");
        Block debris = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("ancient_debris"));
        Block adamantium = BuiltInRegistries.BLOCK.getValue(Identifier.parse("moreminecrafts:adamantium_debris"));
        if (debris == null || adamantium == null || debris == adamantium) throw new IllegalStateException("ore blocks not registered");

        for (int cx = -radius; cx < radius; cx++)
            for (int cz = -radius; cz < radius; cz++) level.getChunk(cx, cz, ChunkStatus.FULL, true);

        int minY = level.getMinY();
        int maxY = level.getMaxY();
        Ore a = new Ore("ancient_debris");
        Ore m = new Ore("adamantium_debris");
        List<int[]> perChunk = new ArrayList<>();
        Map<String, Integer> chunksPerBiome = new TreeMap<>();
        int counted = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = -radius + MARGIN; cx < radius - MARGIN; cx++) {
            for (int cz = -radius + MARGIN; cz < radius - MARGIN; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
                int da = 0, dm = 0;
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) for (int y = minY; y <= maxY; y++) {
                    pos.set(chunk.getPos().getMinBlockX() + dx, y, chunk.getPos().getMinBlockZ() + dz);
                    Block b = chunk.getBlockState(pos).getBlock();
                    if (b == debris) { da++; a.add(pos, level); }
                    else if (b == adamantium) { dm++; m.add(pos, level); }
                }
                perChunk.add(new int[] {da, dm});
                counted++;
                String biome = level.getBiome(new BlockPos(chunk.getPos().getMiddleBlockX(), 64, chunk.getPos().getMiddleBlockZ()))
                        .unwrapKey().map(k -> k.identifier().toString()).orElse("?");
                chunksPerBiome.merge(biome, 1, Integer::sum);
            }
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("label", System.getProperty("mm.nether.ore.label", ""));
        doc.put("seed", level.getSeed());
        doc.put("dimension", level.dimension().identifier().toString());
        doc.put("method", "force FULL generation of a (2R)x(2R) chunk square, count interior chunks only (margin " + MARGIN
                + "), scan every block from minY to maxY, same chunks for both ores");
        doc.put("radius_chunks", radius);
        doc.put("scanned_y", List.of(minY, maxY));
        doc.put("chunks_counted", counted);
        doc.put("chunk_center_biomes", chunksPerBiome);
        doc.put("ancient_debris", a.summary(perChunk, 0, counted));
        doc.put("adamantium_debris", m.summary(perChunk, 1, counted));
        int[] sa = perChunk.stream().mapToInt(c -> c[0]).toArray();
        int[] sm = perChunk.stream().mapToInt(c -> c[1]).toArray();
        long totalA = Arrays.stream(sa).asLongStream().sum();
        long totalM = Arrays.stream(sm).asLongStream().sum();
        doc.put("ratio_adamantium_to_debris", totalA == 0 ? null : (double) totalM / totalA);
        doc.put("chunks_where_adamantium_exceeds_debris", countGreater(perChunk));
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(doc), StandardCharsets.UTF_8);
        LOG.info("ORE_SAMPLING_DONE debris={} adamantium={} chunks={}", totalA, totalM, counted);
    }

    private static int countGreater(List<int[]> perChunk) {
        int n = 0;
        for (int[] c : perChunk) if (c[1] > c[0]) n++;
        return n;
    }

    /** Collected positions of one ore, with per-Y and per-biome tallies and 26-neighbour cluster sizes. */
    private static final class Ore {
        final String name;
        final Map<Integer, Integer> perY = new TreeMap<>();
        final Map<String, Integer> perBiome = new TreeMap<>();
        final Set<Long> positions = new HashSet<>();

        Ore(String name) { this.name = name; }

        void add(BlockPos p, ServerLevel level) {
            perY.merge(p.getY(), 1, Integer::sum);
            perBiome.merge(level.getBiome(p).unwrapKey().map(k -> k.identifier().toString()).orElse("?"), 1, Integer::sum);
            positions.add(p.asLong());
        }

        Map<Integer, Integer> clusterSizes() {
            Map<Integer, Integer> sizes = new TreeMap<>();
            Set<Long> seen = new HashSet<>();
            for (long start : positions) {
                if (!seen.add(start)) continue;
                int size = 0;
                ArrayDeque<Long> queue = new ArrayDeque<>();
                queue.add(start);
                while (!queue.isEmpty()) {
                    long cur = queue.poll();
                    size++;
                    BlockPos c = BlockPos.of(cur);
                    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        long n = BlockPos.asLong(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                        if (positions.contains(n) && seen.add(n)) queue.add(n);
                    }
                }
                sizes.merge(size, 1, Integer::sum);
            }
            return sizes;
        }

        Map<String, Object> summary(List<int[]> perChunk, int column, int chunks) {
            int[] counts = perChunk.stream().mapToInt(c -> c[column]).toArray();
            long total = Arrays.stream(counts).asLongStream().sum();
            int[] sorted = counts.clone();
            Arrays.sort(sorted);
            double mean = chunks == 0 ? 0 : (double) total / chunks;
            double var = 0;
            for (int c : counts) var += (c - mean) * (c - mean);
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("total_blocks", total);
            s.put("mean_per_chunk", mean);
            s.put("stddev_per_chunk", chunks == 0 ? 0 : Math.sqrt(var / chunks));
            s.put("max_in_one_chunk", sorted.length == 0 ? 0 : sorted[sorted.length - 1]);
            s.put("chunks_with_zero", Arrays.stream(counts).filter(c -> c == 0).count());
            s.put("min_y", perY.isEmpty() ? null : ((TreeMap<Integer, Integer>) perY).firstKey());
            s.put("max_y", perY.isEmpty() ? null : ((TreeMap<Integer, Integer>) perY).lastKey());
            s.put("blocks_per_y", perY);
            s.put("blocks_per_biome", perBiome);
            s.put("cluster_size_histogram_26_neighbour", clusterSizes());
            return s;
        }
    }
}
