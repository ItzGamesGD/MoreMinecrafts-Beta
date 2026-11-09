package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import dev.itzgamesgd.moreminecrafts.st01.EntranceLayout;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 Phase 2 Real Minecraft 26.3 Overworld Terrain Measurement Probe.
 *
 * <p>Activated when {@code -Dmm.st01.probe.out=<path>} is set.
 * Measures real Overworld terrain elevation distributions, multi-criteria biome coverage,
 * cut/fill volumes, fluid/cave interference, and candidate rejection rates across reproducible seeds
 * using Minecraft's actual {@code NoiseBasedChunkGenerator} and {@code RandomState}.
 *
 * <p>Diagnostic only: isolated in the GameTest source set and inert in normal game runs.
 */
public final class St01DesertWorldgenProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-desert-worldgen-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int FOOTPRINT_HALF_WIDTH = 40; // 81x81 footprint (-40..+40)
    private static final int HEIGHT_SAMPLE_STEP = 4;    // 21x21 grid = 441 height samples per footprint
    private static final int BIOME_SAMPLE_STEP = 10;    // 9x9 grid = 81 biome samples per footprint

    @Override
    public void onInitialize() {
        String outPath = System.getProperty("mm.st01.probe.out");
        if (outPath == null) {
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            long startTimeMs = System.currentTimeMillis();
            Path out = Path.of(outPath).toAbsolutePath();
            try {
                LOG.info("ST-01 Real Worldgen Measurement Probe starting, output target: {}", out);
                runMeasurement(server, out, startTimeMs);
            } catch (Throwable t) {
                LOG.error("ST-01 Real Worldgen Probe execution failed", t);
                try {
                    Files.createDirectories(out.getParent());
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("status", "FAILED");
                    err.put("error", String.valueOf(t));
                    err.put("stack_trace", Arrays.toString(t.getStackTrace()));
                    Files.writeString(out, GSON.toJson(err), StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                }
                Runtime.getRuntime().halt(1);
            }

            // Flush completed measurement data and terminate headless probe server cleanly.
            // Runtime.halt(0) is used by probe convention to prevent the headless test server from
            // hanging on unmanaged background ticks after measurement data is safely on disk.
            LOG.info("ST-01 Real Worldgen Measurement Probe completed successfully; halting probe process.");
            Runtime.getRuntime().halt(0);
        });
    }

    private static void runMeasurement(MinecraftServer server, Path out, long startTimeMs) throws IOException {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) {
            throw new IllegalStateException("Overworld level not available on server");
        }

        var generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        long seed = level.getSeed();

        int areaRadius = Integer.getInteger("mm.st01.radius", 4096);
        int step = Integer.getInteger("mm.st01.step", 256);
        int maxCandidates = Integer.getInteger("mm.st01.maxcandidates", 500);

        LOG.info("Probing Overworld seed {} across area [+/-{}] with step {} blocks (max candidates: {})",
                seed, areaRadius, step, maxCandidates);

        List<Map<String, Object>> candidateRecords = new ArrayList<>();
        Map<String, Integer> rejectionCounts = new TreeMap<>();
        List<Double> deltaHAll = new ArrayList<>();
        List<Double> cutVolumesAll = new ArrayList<>();
        List<Double> fillVolumesAll = new ArrayList<>();

        int totalEvaluated = 0;
        int centerDesertCount = 0;
        int cornersDesertCount = 0;
        int full100DesertCount = 0;
        int eligibleCount = 0;

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int cx = -areaRadius + 128; cx < areaRadius && totalEvaluated < maxCandidates; cx += step) {
            for (int cz = -areaRadius + 128; cz < areaRadius && totalEvaluated < maxCandidates; cz += step) {
                totalEvaluated++;

                // 1. Biome at anchor
                mpos.set(cx, 64, cz);
                Holder<Biome> anchorBiomeHolder = level.getBiome(mpos);
                String anchorBiomeId = anchorBiomeHolder.unwrapKey()
                        .map(k -> k.identifier().toString())
                        .orElse("unknown");
                boolean centerIsDesert = anchorBiomeHolder.is(Biomes.DESERT);

                // 2. Multi-Criteria Biome Coverage across 81x81 footprint
                int totalBiomeSamples = 0;
                int desertBiomeSamples = 0;
                int perimeterBiomeSamples = 0;
                int perimeterDesertSamples = 0;

                for (int dx = -FOOTPRINT_HALF_WIDTH; dx <= FOOTPRINT_HALF_WIDTH; dx += BIOME_SAMPLE_STEP) {
                    for (int dz = -FOOTPRINT_HALF_WIDTH; dz <= FOOTPRINT_HALF_WIDTH; dz += BIOME_SAMPLE_STEP) {
                        totalBiomeSamples++;
                        mpos.set(cx + dx, 64, cz + dz);
                        boolean isDesert = level.getBiome(mpos).is(Biomes.DESERT);
                        if (isDesert) {
                            desertBiomeSamples++;
                        }
                        if (Math.abs(dx) == FOOTPRINT_HALF_WIDTH || Math.abs(dz) == FOOTPRINT_HALF_WIDTH) {
                            perimeterBiomeSamples++;
                            if (isDesert) {
                                perimeterDesertSamples++;
                            }
                        }
                    }
                }

                // Four corner check
                boolean cNW = level.getBiome(mpos.set(cx - FOOTPRINT_HALF_WIDTH, 64, cz - FOOTPRINT_HALF_WIDTH)).is(Biomes.DESERT);
                boolean cNE = level.getBiome(mpos.set(cx + FOOTPRINT_HALF_WIDTH, 64, cz - FOOTPRINT_HALF_WIDTH)).is(Biomes.DESERT);
                boolean cSW = level.getBiome(mpos.set(cx - FOOTPRINT_HALF_WIDTH, 64, cz + FOOTPRINT_HALF_WIDTH)).is(Biomes.DESERT);
                boolean cSE = level.getBiome(mpos.set(cx + FOOTPRINT_HALF_WIDTH, 64, cz + FOOTPRINT_HALF_WIDTH)).is(Biomes.DESERT);
                boolean cornersDesert = cNW && cNE && cSW && cSE;

                double desertCoverageRatio = (double) desertBiomeSamples / totalBiomeSamples;
                boolean full100Desert = (desertBiomeSamples == totalBiomeSamples);
                boolean perimeter100Desert = (perimeterDesertSamples == perimeterBiomeSamples);

                if (centerIsDesert) centerDesertCount++;
                if (centerIsDesert && cornersDesert) cornersDesertCount++;
                if (full100Desert) full100DesertCount++;

                // If center is not desert, reject candidate early
                if (!centerIsDesert) {
                    rejectionCounts.merge("REJECT_NOT_DESERT", 1, Integer::sum);
                    continue;
                }

                // 3. Real Surface Heights across 81x81 footprint
                List<Integer> heights = new ArrayList<>();
                List<Integer> perimeterHeights = new ArrayList<>();

                for (int dx = -FOOTPRINT_HALF_WIDTH; dx <= FOOTPRINT_HALF_WIDTH; dx += HEIGHT_SAMPLE_STEP) {
                    for (int dz = -FOOTPRINT_HALF_WIDTH; dz <= FOOTPRINT_HALF_WIDTH; dz += HEIGHT_SAMPLE_STEP) {
                        int h = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
                        heights.add(h);
                        if (Math.abs(dx) >= FOOTPRINT_HALF_WIDTH - HEIGHT_SAMPLE_STEP || Math.abs(dz) >= FOOTPRINT_HALF_WIDTH - HEIGHT_SAMPLE_STEP) {
                            perimeterHeights.add(h);
                        }
                    }
                }

                Collections.sort(heights);
                int minH = heights.get(0);
                int maxH = heights.get(heights.size() - 1);
                int deltaH = maxH - minH;
                double meanH = heights.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                int medianH = heights.get(heights.size() / 2);
                int p10 = heights.get((int) (heights.size() * 0.10));
                int p25 = heights.get((int) (heights.size() * 0.25));
                int p75 = heights.get((int) (heights.size() * 0.75));
                int p90 = heights.get((int) (heights.size() * 0.90));

                deltaHAll.add((double) deltaH);

                // 4. Candidate Foundation Elevations and Cut/Fill Analysis
                // Option A: Median height (balanced cut/fill)
                int yfBalanced = medianH;
                // Option B: Minimum height (all-cut)
                int yfAllCut = minH;
                // Option C: Anchor center height
                int yfCenter = generator.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);

                // Calculate cut and fill for balanced foundation
                double cutVolumeApprox = 0.0;
                double fillVolumeApprox = 0.0;
                double sampleArea = HEIGHT_SAMPLE_STEP * HEIGHT_SAMPLE_STEP; // 16 blocks per sample

                for (int h : heights) {
                    if (h > yfBalanced) {
                        cutVolumeApprox += (h - yfBalanced) * sampleArea;
                    } else if (h < yfBalanced) {
                        fillVolumeApprox += (yfBalanced - h) * sampleArea;
                    }
                }

                int maxCutDepth = Math.max(0, maxH - yfBalanced);
                int maxSkirtDepth = Math.max(0, yfBalanced - minH);

                long exposedPerimeterCount = perimeterHeights.stream().filter(h -> h < yfBalanced).count();
                double exposedPerimeterRatio = (double) exposedPerimeterCount / perimeterHeights.size();

                cutVolumesAll.add(cutVolumeApprox);
                fillVolumesAll.add(fillVolumeApprox);

                // 5. Underground Column & Cave / Fluid Check
                NoiseColumn centerCol = generator.getBaseColumn(cx, cz, level, randomState);
                boolean fluidCollision = false;
                boolean deepCaveCollision = false;

                // Scan subterranean layers beneath the foundation (yf - 32 .. yf - 1)
                for (int y = yfBalanced - 32; y < yfBalanced; y++) {
                    if (y >= level.getMinY() && y <= level.getMaxY()) {
                        var bstate = centerCol.getBlock(y);
                        if (!bstate.getFluidState().isEmpty()) {
                            fluidCollision = true;
                        }
                        if (y <= yfBalanced - 25 && bstate.isAir()) {
                            deepCaveCollision = true;
                        }
                    }
                }

                // 6. Rejection Classification against exploratory thresholds
                String candidateVerdict;
                if (deltaH > 12) {
                    candidateVerdict = "REJECT_EXCESSIVE_DELTA_H";
                    rejectionCounts.merge("REJECT_EXCESSIVE_DELTA_H", 1, Integer::sum);
                } else if (maxSkirtDepth > 6) {
                    candidateVerdict = "REJECT_EXCESSIVE_SKIRT";
                    rejectionCounts.merge("REJECT_EXCESSIVE_SKIRT", 1, Integer::sum);
                } else if (maxCutDepth > 8) {
                    candidateVerdict = "REJECT_EXCESSIVE_CUT";
                    rejectionCounts.merge("REJECT_EXCESSIVE_CUT", 1, Integer::sum);
                } else if (fluidCollision) {
                    candidateVerdict = "REJECT_FLUID_COLLISION";
                    rejectionCounts.merge("REJECT_FLUID_COLLISION", 1, Integer::sum);
                } else {
                    candidateVerdict = "ELIGIBLE";
                    rejectionCounts.merge("ELIGIBLE", 1, Integer::sum);
                    eligibleCount++;
                }

                // Deterministic Orientation using shared MurmurHash3 algorithm
                EntranceLayout.Orientation orientation = selectOrientation(seed, cx, cz);

                if (candidateRecords.size() < 100) {
                    Map<String, Object> rec = new LinkedHashMap<>();
                    rec.put("seed", seed);
                    rec.put("anchor_x", cx);
                    rec.put("anchor_z", cz);
                    rec.put("anchor_biome", anchorBiomeId);
                    rec.put("orientation", orientation.name());
                    rec.put("verdict", candidateVerdict);
                    rec.put("biome_metrics", Map.of(
                            "center_is_desert", centerIsDesert,
                            "corners_all_desert", cornersDesert,
                            "perimeter_100_percent_desert", perimeter100Desert,
                            "full_100_percent_desert", full100Desert,
                            "desert_coverage_ratio", desertCoverageRatio
                    ));
                    rec.put("elevation_metrics", Map.of(
                            "min_h", minH,
                            "max_h", maxH,
                            "delta_h", deltaH,
                            "mean_h", round2(meanH),
                            "median_h", medianH,
                            "p10", p10, "p25", p25, "p75", p75, "p90", p90
                    ));
                    rec.put("foundation_cut_fill", Map.of(
                            "chosen_yf_balanced", yfBalanced,
                            "max_cut_depth", maxCutDepth,
                            "max_skirt_depth", maxSkirtDepth,
                            "approx_cut_volume", (long) cutVolumeApprox,
                            "approx_fill_volume", (long) fillVolumeApprox,
                            "exposed_perimeter_ratio", round2(exposedPerimeterRatio)
                    ));
                    rec.put("subterranean_risks", Map.of(
                            "fluid_collision", fluidCollision,
                            "deep_cave_collision", deepCaveCollision
                    ));
                    candidateRecords.add(rec);
                }
            }
        }

        long durationMs = System.currentTimeMillis() - startTimeMs;
        long peakMemoryMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("provenance", "REAL_MINECRAFT_26_3_WORLDGEN_MEASUREMENT");
        report.put("generator", generator.getClass().getName());
        report.put("minecraft_version", "26.3");
        report.put("world_seed", seed);
        report.put("heightmap_type", "WORLD_SURFACE_WG");
        report.put("sampling_grid", Map.of(
                "area_radius_blocks", areaRadius,
                "grid_step_blocks", step,
                "footprint_span_blocks", "81x81 (-40..+40)",
                "height_sample_points_per_candidate", 441,
                "biome_sample_points_per_candidate", 81
        ));
        report.put("performance_metrics", Map.of(
                "duration_ms", durationMs,
                "peak_memory_mb", peakMemoryMb,
                "total_candidates_evaluated", totalEvaluated
        ));
        report.put("candidate_counts", Map.of(
                "total_grid_candidates", totalEvaluated,
                "center_in_desert", centerDesertCount,
                "center_and_corners_in_desert", cornersDesertCount,
                "full_100_percent_in_desert", full100DesertCount,
                "eligible_under_exploratory_limits", eligibleCount
        ));
        report.put("rejection_breakdown", rejectionCounts);

        if (!deltaHAll.isEmpty()) {
            Collections.sort(deltaHAll);
            report.put("real_desert_elevation_delta_distribution", Map.of(
                    "sample_size", deltaHAll.size(),
                    "mean_delta_h", round2(deltaHAll.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)),
                    "median_delta_h", round2(deltaHAll.get(deltaHAll.size() / 2)),
                    "min_delta_h", round2(deltaHAll.get(0)),
                    "max_delta_h", round2(deltaHAll.get(deltaHAll.size() - 1)),
                    "p25_delta_h", round2(deltaHAll.get((int) (deltaHAll.size() * 0.25))),
                    "p75_delta_h", round2(deltaHAll.get((int) (deltaHAll.size() * 0.75))),
                    "p90_delta_h", round2(deltaHAll.get((int) (deltaHAll.size() * 0.90)))
            ));
        }

        report.put("sample_candidate_records", candidateRecords);

        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(report), StandardCharsets.UTF_8);

        LOG.info("ST-01 Real Worldgen Measurement written to {} (evaluated={}, desert={}, eligible={})",
                out, totalEvaluated, centerDesertCount, eligibleCount);
    }

    private static long mix64(long seed, int x, int z) {
        long coordHash = (((long) x & 0xFFFFFFFFL) << 32) | ((long) z & 0xFFFFFFFFL);
        long combined = seed ^ coordHash;
        combined ^= (combined >>> 33);
        combined *= 0xff51afd7ed558ccdL;
        combined ^= (combined >>> 33);
        combined *= 0xc4ceb9fe1a85ec53L;
        combined ^= (combined >>> 33);
        return combined;
    }

    private static EntranceLayout.Orientation selectOrientation(long seed, int x, int z) {
        long h = mix64(seed, x, z);
        return (h & 1L) == 0L ? EntranceLayout.Orientation.EAST : EntranceLayout.Orientation.WEST;
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
