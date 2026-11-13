package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01FastTerrain;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support.KnownStart;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support.World;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-D3 verification probe (inert unless {@code -Dmm.st01.d3.out} is set; not part of build/check).
 *
 * <ul>
 *   <li>{@code -Dmm.st01.d3.mode=regression}: no dependence on the server's own world. (1) The P8-D2-R1 height comparison
 *       repeated on the production code: every footprint column of the first centre-Desert candidates of seeds 1004 (12),
 *       1001 (20) and 407538027 (20), batched vs {@code getBaseHeight}. (2) For all 20 P8-C2-C seeds and every potential
 *       anchor of the +-10,240-block window, the complete production decision (verdict, measurements, ground, orientation,
 *       route) with the P8-D3 terrain against the pre-P8-D3 terrain, and the eligible set against the 46 real-server starts.</li>
 *   <li>{@code -Dmm.st01.d3.mode=bench}: on the server thread of a fresh dedicated server, what an explorer map does:
 *       vanilla {@code findNearestMapStructure(st01, origin, radius, false)} then the vanilla map creation calls, cold,
 *       repeated, and at a second origin; with or without {@code -Dmm.st01.terrain.legacy=true}.</li>
 * </ul>
 */
public final class St01D3Probe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-d3-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.d3.out");
        if (out == null) return;
        String mode = System.getProperty("mm.st01.d3.mode", "bench");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mode", mode);
            result.put("legacy_forced", St01FastTerrain.legacyForced());
            result.put("available_processors", Runtime.getRuntime().availableProcessors());
            result.put("java", System.getProperty("java.version"));
            try {
                if (mode.equals("regression")) regression(server, result); else bench(server, result);
            } catch (Throwable t) {
                LOG.error("ST01 D3 probe failed", t);
                result.put("error", t.toString());
            }
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
            } catch (Throwable t) {
                LOG.error("could not write", t);
            }
            Runtime.getRuntime().halt(0);
        });
    }

    // ---------------------------------------------------------------------------------------------- regression

    private static void regression(MinecraftServer server, Map<String, Object> result) throws Exception {
        int threads = Integer.getInteger("mm.st01.d3.threads", 4);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        result.put("threads", threads);
        long t0 = System.nanoTime();
        // (1) footprint columns
        long[][] plan = {{1004L, 12}, {1001L, 20}, {407538027L, 20}};
        List<Future<Map<String, Object>>> columnJobs = new ArrayList<>();
        for (long[] p : plan) columnJobs.add(pool.submit(() -> footprintColumns(server, p[0], (int) p[1])));
        List<Map<String, Object>> columnResults = new ArrayList<>();
        long columns = 0, columnMismatches = 0;
        for (Future<Map<String, Object>> f : columnJobs) {
            Map<String, Object> m = f.get();
            columnResults.add(m);
            columns += (long) m.get("columns");
            columnMismatches += (long) m.get("mismatches");
        }
        result.put("footprint_columns", columnResults);
        result.put("footprint_columns_total", columns);
        result.put("footprint_column_mismatches_total", columnMismatches);
        // (2) decisions for all 20 seeds
        List<Future<Map<String, Object>>> seedJobs = new ArrayList<>();
        for (long seed : St01D3Support.SEEDS) seedJobs.add(pool.submit(() -> seedDecisions(server, seed)));
        List<Map<String, Object>> seedResults = new ArrayList<>();
        long anchors = 0, decisionMismatches = 0, eligible = 0, startMismatches = 0;
        double fastMs = 0, legacyMs = 0;
        for (Future<Map<String, Object>> f : seedJobs) {
            Map<String, Object> m = f.get();
            seedResults.add(m);
            anchors += (int) m.get("anchors");
            decisionMismatches += (int) m.get("decision_mismatches");
            eligible += (int) m.get("eligible");
            startMismatches += (int) m.get("real_start_mismatches");
            fastMs += (double) m.get("fast_cpu_ms");
            legacyMs += (double) m.get("legacy_cpu_ms");
        }
        pool.shutdown();
        result.put("seeds", seedResults);
        result.put("anchors_total", anchors);
        result.put("decision_mismatches_total", decisionMismatches);
        result.put("eligible_total", eligible);
        result.put("real_starts_expected", St01D3Support.KNOWN_STARTS.size());
        result.put("real_start_mismatches_total", startMismatches);
        result.put("fast_cpu_ms_total", fastMs);
        result.put("legacy_cpu_ms_total", legacyMs);
        result.put("spot_check_mismatches", St01FastTerrain.SPOT_CHECK_MISMATCHES.get());
        result.put("wall_s", (System.nanoTime() - t0) / 1e9);
        result.put("pass", columnMismatches == 0 && decisionMismatches == 0 && startMismatches == 0
                && eligible == St01D3Support.KNOWN_STARTS.size() && St01FastTerrain.SPOT_CHECK_MISMATCHES.get() == 0);
    }

    private static Map<String, Object> footprintColumns(MinecraftServer server, long seed, int footprints) {
        World w = new World(server.registryAccess(), seed);
        BiomeResolver point = w.biomeSource.createResolver(w.newSampler());
        long columns = 0, mismatches = 0;
        int done = 0;
        List<String> samples = new ArrayList<>();
        outer:
        for (int rx = -40; rx < 40; rx++) for (int rz = -40; rz < 40; rz++) {
            ChunkPos c = w.candidate(rx, rz);
            int cx = c.getMinBlockX() + 8, cz = c.getMinBlockZ() + 8;
            int qx = Math.floorDiv(cx, 4), qz = Math.floorDiv(cz, 4);
            int qy = Math.floorDiv(w.legacyHeight(qx * 4 + 2, qz * 4 + 2) - 1 - 2, 4);
            if (!(point.getNoiseBiome(qx, qy, qz).is(Biomes.DESERT) && point.getNoiseBiome(qx, qy + 1, qz).is(Biomes.DESERT))) continue;
            Structure.GenerationContext ctx = w.context(c);
            St01PyramidStructure.GeneratorTerrain fast = new St01PyramidStructure.GeneratorTerrain(ctx);
            for (int dx = -40; dx <= 40; dx++) for (int dz = -40; dz <= 40; dz++) {
                int a = fast.surfaceHeight(cx + dx, cz + dz), b = w.legacyHeight(cx + dx, cz + dz);
                columns++;
                if (a != b) { mismatches++; if (samples.size() < 10) samples.add((cx + dx) + "," + (cz + dz) + ": " + a + " vs " + b); }
            }
            if (++done >= footprints) break outer;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seed", seed);
        m.put("footprints", done);
        m.put("columns", columns);
        m.put("mismatches", mismatches);
        m.put("mismatch_samples", samples);
        return m;
    }

    private static Map<String, Object> seedDecisions(MinecraftServer server, long seed) {
        World w = new World(server.registryAccess(), seed);
        int spacing = w.placement.spacing(), radiusChunks = St01D3Support.WINDOW_BLOCKS / 16;
        int r0 = Math.floorDiv(-radiusChunks, spacing), r1 = Math.floorDiv(radiusChunks - 1, spacing);
        int anchors = 0, mismatches = 0, eligible = 0, startMismatches = 0;
        long fastNs = 0, legacyNs = 0, fastMax = 0, legacyMax = 0;
        Map<String, Integer> verdicts = new TreeMap<>();
        List<String> samples = new ArrayList<>();
        Map<String, St01Candidate.Decision> eligibleByChunk = new LinkedHashMap<>();
        for (int rx = r0; rx <= r1; rx++) for (int rz = r0; rz <= r1; rz++) {
            ChunkPos c = w.candidate(rx, rz);
            if (Math.abs(c.getMiddleBlockX()) > St01D3Support.WINDOW_BLOCKS || Math.abs(c.getMiddleBlockZ()) > St01D3Support.WINDOW_BLOCKS) continue;
            anchors++;
            long a = THREADS.getCurrentThreadCpuTime();
            St01Candidate.Decision fast = St01D3Support.fastDecision(w, c);
            long b = THREADS.getCurrentThreadCpuTime();
            St01Candidate.Decision legacy = St01D3Support.legacyDecision(w, c);
            long e = THREADS.getCurrentThreadCpuTime();
            fastNs += b - a; legacyNs += e - b;
            fastMax = Math.max(fastMax, b - a); legacyMax = Math.max(legacyMax, e - b);
            verdicts.merge(fast.result().verdict().name(), 1, Integer::sum);
            if (!fast.equals(legacy)) {
                mismatches++;
                if (samples.size() < 10) samples.add(c.x() + "," + c.z() + " fast " + St01D3Support.describe(fast) + " legacy " + St01D3Support.describe(legacy));
            }
            if (fast.result().eligible()) { eligible++; eligibleByChunk.put(c.x() + "," + c.z(), fast); }
        }
        List<String> starts = new ArrayList<>();
        int expected = 0;
        for (KnownStart s : St01D3Support.KNOWN_STARTS) {
            if (s.seed() != seed) continue;
            expected++;
            St01Candidate.Decision d = eligibleByChunk.remove(s.chunkX() + "," + s.chunkZ());
            boolean same = d != null && d.ground().orElseThrow().equals(new BlockPos(s.groundX(), s.groundY(), s.groundZ()))
                    && d.orientation() == s.orientation() && d.route().orElseThrow() == s.route();
            if (!same) startMismatches++;
            starts.add(s.chunkX() + "," + s.chunkZ() + (same ? " same" : " DIFFERENT: " + (d == null ? "not eligible" : St01D3Support.describe(d))));
        }
        for (String extra : eligibleByChunk.keySet()) { startMismatches++; starts.add(extra + " EXTRA (not a real start)"); }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seed", seed);
        m.put("anchors", anchors);
        m.put("verdicts", verdicts);
        m.put("eligible", eligible);
        m.put("real_starts", expected);
        m.put("real_start_mismatches", startMismatches);
        m.put("starts", starts);
        m.put("decision_mismatches", mismatches);
        m.put("decision_mismatch_samples", samples);
        m.put("fast_cpu_ms", fastNs / 1e6);
        m.put("legacy_cpu_ms", legacyNs / 1e6);
        m.put("fast_max_candidate_ms", fastMax / 1e6);
        m.put("legacy_max_candidate_ms", legacyMax / 1e6);
        return m;
    }

    // ---------------------------------------------------------------------------------------------- bench

    private static void bench(MinecraftServer server, Map<String, Object> result) {
        ServerLevel level = server.overworld();
        Holder<Structure> st01 = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID).orElseThrow();
        HolderSet<Structure> set = HolderSet.direct(st01);
        int radius = Integer.getInteger("mm.st01.d3.radius", 20);
        result.put("seed", level.getSeed());
        result.put("radius", radius);
        result.put("generator", level.getChunkSource().getGenerator().getClass().getName());
        result.put("batched_applies", St01FastTerrain.batchedApplies(level.getChunkSource().getGenerator()));
        List<Map<String, Object>> calls = new ArrayList<>();
        BlockPos[] origins = {new BlockPos(0, 64, 0), new BlockPos(6000, 64, -6000)};
        String[] labels = {"cold", "warm1", "warm2", "warm3"};
        for (int o = 0; o < origins.length; o++) {
            for (int i = 0; i < (o == 0 ? labels.length : 2); i++) {
                calls.add(mapCall(level, set, st01.value(), origins[o], radius, (o == 0 ? "origin0_" : "origin1_") + (o == 0 ? labels[i] : (i == 0 ? "partial" : "warm1"))));
            }
        }
        result.put("calls", calls);
    }

    /** What an explorer map does on the server thread (vanilla calls; no structure tag exists yet, so the holder set is direct). */
    private static Map<String, Object> mapCall(ServerLevel level, HolderSet<Structure> set, Structure st01, BlockPos origin, int radius, String label) {
        long evals0 = 0;
        for (var v : St01PyramidStructure.VERDICT_COUNTS.values()) evals0 += v.get();
        long fills0 = St01FastTerrain.BATCHED_CHUNK_FILLS.get(), legacy0 = St01FastTerrain.LEGACY_COLUMN_HEIGHTS.get();
        long pre0 = St01FastTerrain.COLUMN_PREFILTER_TESTS.get();
        int loaded0 = level.getChunkSource().getLoadedChunksCount();
        long cpu0 = THREADS.getCurrentThreadCpuTime(), t0 = System.nanoTime();
        BlockPos found = level.findNearestMapStructure(set, origin, radius, false);
        long tLocate = System.nanoTime();
        ItemStack map = null;
        if (found != null) {
            map = MapItem.create(level, found.getX(), found.getZ(), (byte) 2, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, found, "+", MapDecorationTypes.TARGET_X);
        }
        long t1 = System.nanoTime(), cpu1 = THREADS.getCurrentThreadCpuTime();
        long evals1 = 0;
        for (var v : St01PyramidStructure.VERDICT_COUNTS.values()) evals1 += v.get();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("origin", origin.getX() + "," + origin.getZ());
        m.put("server_thread_blocked_ms", (t1 - t0) / 1e6);
        m.put("locate_ms", (tLocate - t0) / 1e6);
        m.put("map_creation_ms", (t1 - tLocate) / 1e6);
        m.put("server_thread_cpu_ms", (cpu1 - cpu0) / 1e6);
        m.put("st01_evaluations_all_threads", evals1 - evals0);
        m.put("batched_chunk_fills", St01FastTerrain.BATCHED_CHUNK_FILLS.get() - fills0);
        m.put("legacy_column_heights", St01FastTerrain.LEGACY_COLUMN_HEIGHTS.get() - legacy0);
        m.put("prefilter_column_tests", St01FastTerrain.COLUMN_PREFILTER_TESTS.get() - pre0);
        m.put("loaded_chunks_delta", level.getChunkSource().getLoadedChunksCount() - loaded0);
        if (found != null) {
            ChunkPos chunk = new ChunkPos(found.getX() >> 4, found.getZ() >> 4);
            StructureStart start = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(st01);
            m.put("found_chunk", chunk.x() + "," + chunk.z());
            m.put("distance_blocks", Math.sqrt(Math.pow(found.getX() - origin.getX(), 2) + Math.pow(found.getZ() - origin.getZ(), 2)));
            m.put("target_is_valid_st01_start", start != null && start.isValid());
            m.put("target_inside_start_box", start != null && start.isValid()
                    && start.getBoundingBox().minX() <= found.getX() && found.getX() <= start.getBoundingBox().maxX()
                    && start.getBoundingBox().minZ() <= found.getZ() && found.getZ() <= start.getBoundingBox().maxZ());
            MapItemSavedData data = MapItem.getSavedData(map, level);
            m.put("map_created", data != null);
        } else {
            m.put("found_chunk", null);
        }
        return m;
    }
}
