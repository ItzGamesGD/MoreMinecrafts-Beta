package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03AssemblyPiece;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03Eligibility;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03HeadquartersStructure;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03PlacementConfig;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03Worldgen;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inert unless -Dmm.st03.site.out is set.
 * Measures ST-03 site acceptance across 10 seeds (3 plains-heavy, 3 forest-heavy,
 * 2 mountain/high-relief control, 2 random) at region radius +-25 using
 * RandomSpreadStructurePlacement.getPotentialStructureChunk and St03Eligibility.evaluate,
 * and verifies real NoiseBasedChunkGenerator chunk placement on any passing candidates.
 */
public final class St03SiteAcceptanceProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st03-site-acceptance");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private static final int REGION_RADIUS = 25;
    private static final long SCREEN_START_SEED = 10001L;
    private static final int SCREEN_COUNT = 150;
    private static final long[] RANDOM_SEEDS = {314159265L, 867530926L};
    private static final long REFERENCE_SEED_7001 = 7001L;

    private static final Set<String> PURE_PLAINS = Set.of(
            "minecraft:plains",
            "minecraft:sunflower_plains"
    );
    private static final Set<String> MEADOW_BIOME = Set.of(
            "minecraft:meadow"
    );
    private static final Set<String> FOREST_BIOMES = Set.of(
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest",
            "minecraft:dark_forest"
    );
    private static final Set<String> MOUNTAIN_BIOMES = Set.of(
            "minecraft:windswept_hills",
            "minecraft:windswept_gravelly_hills",
            "minecraft:windswept_forest",
            "minecraft:stony_peaks",
            "minecraft:jagged_peaks",
            "minecraft:frozen_peaks",
            "minecraft:snowy_slopes",
            "minecraft:grove"
    );
    private static final Set<String> WATER_OR_SHORE_BIOMES = Set.of(
            "minecraft:ocean",
            "minecraft:deep_ocean",
            "minecraft:cold_ocean",
            "minecraft:deep_cold_ocean",
            "minecraft:lukewarm_ocean",
            "minecraft:deep_lukewarm_ocean",
            "minecraft:warm_ocean",
            "minecraft:frozen_ocean",
            "minecraft:deep_frozen_ocean",
            "minecraft:river",
            "minecraft:frozen_river",
            "minecraft:beach",
            "minecraft:snowy_beach",
            "minecraft:stony_shore",
            "minecraft:swamp",
            "minecraft:mangrove_swamp"
    );

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st03.site.out");
        if (out == null) {
            return;
        }
        String mode = System.getProperty("mm.st03.site.mode", "measure-all");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mode", mode);
            try {
                if ("verify-chunks".equals(mode)) {
                    runVerifyChunks(server, result);
                } else {
                    runMeasureAll(server, result);
                }
                result.put("status", "OK");
            } catch (Throwable t) {
                LOG.error("ST03 site acceptance probe failed", t);
                result.put("status", "ERROR");
                result.put("error", t.toString());
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement e : t.getStackTrace()) {
                    sb.append(e).append('\n');
                }
                result.put("error_stack", sb.toString());
            }
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) {
                    Files.createDirectories(p.getParent());
                }
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
                LOG.info("ST03 site acceptance output written to {}", p.toAbsolutePath());
            } catch (Throwable t) {
                LOG.error("Failed to write ST03 site acceptance output", t);
            }
            Runtime.getRuntime().halt("OK".equals(result.get("status")) ? 0 : 1);
        });
    }

    private static void runMeasureAll(MinecraftServer server, Map<String, Object> result) {
        ServerLevel level = server.overworld();
        ChunkGenerator serverGen = level.getChunkSource().getGenerator();
        String generatorClass = serverGen.getClass().getName();
        LOG.info("ST03_SITE_PROBE serverOverworldGenerator={}", generatorClass);
        result.put("server_overworld_generator_class", generatorClass);
        if (!(serverGen instanceof NoiseBasedChunkGenerator)) {
            throw new IllegalStateException("Expected NoiseBasedChunkGenerator on overworld, got " + generatorClass);
        }

        RegistryAccess access = level.registryAccess();
        var setHolder = access.lookupOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, St03Worldgen.STRUCTURE_ID));
        if (!(setHolder.value().placement() instanceof RandomSpreadStructurePlacement placement)) {
            throw new IllegalStateException("Expected RandomSpreadStructurePlacement for ST-03");
        }
        Map<String, Object> placementMeta = new LinkedHashMap<>();
        placementMeta.put("spacing", placement.spacing());
        placementMeta.put("separation", placement.separation());
        placementMeta.put("salt", 91403);
        placementMeta.put("spread_type", placement.spreadType().getSerializedName());
        placementMeta.put("region_radius", REGION_RADIUS);
        placementMeta.put("candidates_per_seed", (2 * REGION_RADIUS + 1) * (2 * REGION_RADIUS + 1));
        result.put("placement_config", placementMeta);

        BiomeSource biomeSource = MultiNoiseBiomeSource.createFromPreset(
                access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                        .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        Holder<NoiseGeneratorSettings> noiseSettings = access.lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        var noiseRegistry = access.lookupOrThrow(Registries.NOISE);

        // 1. Screen 150 deterministic candidate seeds across 17x17 = 289 points in [-3840, +3840]
        long tScreen0 = System.currentTimeMillis();
        List<ScreenScore> screened = new ArrayList<>();
        for (int i = 0; i < SCREEN_COUNT; i++) {
            long s = SCREEN_START_SEED + i;
            RandomState rs = RandomState.create(noiseRegistry, s, noiseSettings.value());
            Climate.Sampler sampler = rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
            BiomeResolver resolver = biomeSource.createResolver(sampler);
            int plains = 0, forest = 0, mountain = 0, water = 0, total = 0;
            for (int x = -3840; x <= 3840; x += 480) {
                for (int z = -3840; z <= 3840; z += 480) {
                    total++;
                    String id = biomeId(resolver.getNoiseBiome(x >> 2, 16, z >> 2));
                    if (PURE_PLAINS.contains(id)) plains++;
                    else if (FOREST_BIOMES.contains(id)) forest++;
                    else if (MOUNTAIN_BIOMES.contains(id)) mountain++;
                    else if (WATER_OR_SHORE_BIOMES.contains(id)) water++;
                }
            }
            screened.add(new ScreenScore(s, plains, forest, mountain, water, total));
        }
        long screenMs = System.currentTimeMillis() - tScreen0;
        LOG.info("ST03_SITE_PROBE screened {} seeds in {} ms", SCREEN_COUNT, screenMs);

        Set<Long> used = new HashSet<>();
        List<SeedEntry> selected = new ArrayList<>();

        screened.stream()
                .sorted(Comparator.comparingInt((ScreenScore a) -> a.plains).reversed().thenComparingLong(a -> a.seed))
                .filter(s -> used.add(s.seed))
                .limit(3)
                .forEach(s -> selected.add(new SeedEntry(s.seed, "plains_heavy")));

        screened.stream()
                .sorted(Comparator.comparingInt((ScreenScore a) -> a.forest).reversed().thenComparingLong(a -> a.seed))
                .filter(s -> used.add(s.seed))
                .limit(3)
                .forEach(s -> selected.add(new SeedEntry(s.seed, "forest_heavy")));

        screened.stream()
                .sorted(Comparator.comparingInt((ScreenScore a) -> a.mountain).reversed().thenComparingLong(a -> a.seed))
                .filter(s -> used.add(s.seed))
                .limit(2)
                .forEach(s -> selected.add(new SeedEntry(s.seed, "mountain_control")));

        for (long rs : RANDOM_SEEDS) {
            used.add(rs);
            selected.add(new SeedEntry(rs, "random"));
        }

        Map<String, Object> selectionMeta = new LinkedHashMap<>();
        selectionMeta.put("screen_pool_range", SCREEN_START_SEED + ".." + (SCREEN_START_SEED + SCREEN_COUNT - 1));
        selectionMeta.put("screen_grid", "17x17 (289 pts, step=480 in [-3840,+3840], qy=16)");
        selectionMeta.put("screen_ms", screenMs);
        selectionMeta.put("random_seeds_fixed", Arrays.stream(RANDOM_SEEDS).boxed().toList());
        result.put("seed_selection_method", selectionMeta);

        // 2. Measure all 10 selected seeds at +-25 regions (+ reference seed 7001) in parallel (4 threads)
        List<SeedEntry> allEntries = new ArrayList<>(selected);
        allEntries.add(new SeedEntry(REFERENCE_SEED_7001, "reference_baseline"));

        int threads = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors()));
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        List<java.util.concurrent.Future<SeedOutcome>> futures = new ArrayList<>();
        for (SeedEntry entry : allEntries) {
            futures.add(pool.submit(() -> {
                List<Map<String, Object>> localEligible = new ArrayList<>();
                BiomeSource threadBiomeSource = MultiNoiseBiomeSource.createFromPreset(
                        access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
                NoiseBasedChunkGenerator seedGen = new NoiseBasedChunkGenerator(threadBiomeSource, noiseSettings);
                Map<String, Object> row = measureSeed(
                        entry.seed, entry.category, REGION_RADIUS, true,
                        access, noiseSettings, threadBiomeSource, seedGen, height, placement,
                        localEligible);
                return new SeedOutcome(entry, row, localEligible);
            }));
        }
        pool.shutdown();

        List<Map<String, Object>> seedResults = new ArrayList<>();
        List<Map<String, Object>> allEligibleCandidates = new ArrayList<>();
        Map<String, Object> ref7001 = null;
        try {
            for (var f : futures) {
                SeedOutcome out = f.get();
                if ("reference_baseline".equals(out.entry.category)) {
                    ref7001 = out.row;
                } else {
                    seedResults.add(out.row);
                    allEligibleCandidates.addAll(out.eligible);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parallel seed measurement failed", e);
        }

        result.put("seeds", seedResults);
        result.put("eligible_candidates_total", allEligibleCandidates.size());
        result.put("eligible_candidates", allEligibleCandidates);
        result.put("reference_seed_7001", ref7001);

        // 3. If 0 candidates passed in +-25 across all 10 seeds, run a fast +-60 diagnostic scan on the #1 plains-heavy seed
        if (allEligibleCandidates.isEmpty()) {
            long topPlainsSeed = selected.get(0).seed;
            List<Map<String, Object>> wideEligible = new ArrayList<>();
            NoiseBasedChunkGenerator wideGen = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);
            Map<String, Object> wideScan = measureSeed(
                    topPlainsSeed, "plains_heavy_wide_60", 60, false,
                    access, noiseSettings, biomeSource, wideGen, height, placement,
                    wideEligible);
            wideScan.put("eligible_candidates", wideEligible);
            result.put("wide_60_diagnostic_top_plains", wideScan);
        }
    }

    private record SeedOutcome(SeedEntry entry, Map<String, Object> row, List<Map<String, Object>> eligible) {}

    private static Map<String, Object> measureSeed(
            long seed,
            String category,
            int regionRadius,
            boolean collectCounterfactuals,
            RegistryAccess access,
            Holder<NoiseGeneratorSettings> noiseSettings,
            BiomeSource biomeSource,
            NoiseBasedChunkGenerator generator,
            LevelHeightAccessor height,
            RandomSpreadStructurePlacement placement,
            List<Map<String, Object>> outEligible) {
        long t0 = System.currentTimeMillis();
        RandomState rs = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, noiseSettings.value());
        Climate.Sampler sampler = rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
        BiomeResolver resolver = biomeSource.createResolver(sampler);

        // A. Surface biome & elevation distribution (21x21 = 441 points in [-3840, +3840], step 384)
        int distTotal = 0, distPurePlains = 0, distMeadow = 0, distForest = 0, distMountain = 0, distWaterShore = 0, distOther = 0;
        int distEligibleBiomes = 0;
        double sumY = 0, sumY2 = 0;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        Map<String, Integer> topBiomes = new TreeMap<>();
        for (int x = -3840; x <= 3840; x += 384) {
            for (int z = -3840; z <= 3840; z += 384) {
                distTotal++;
                int surfY = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, rs);
                sumY += surfY;
                sumY2 += (double) surfY * surfY;
                if (surfY < minY) minY = surfY;
                if (surfY > maxY) maxY = surfY;
                Holder<Biome> b = resolver.getNoiseBiome(x >> 2, Math.floorDiv(surfY - 1, 4), z >> 2);
                String id = biomeId(b);
                topBiomes.merge(id, 1, Integer::sum);
                if (St03HeadquartersStructure.isEligibleBiome(b)) distEligibleBiomes++;
                if (PURE_PLAINS.contains(id)) distPurePlains++;
                else if (MEADOW_BIOME.contains(id)) distMeadow++;
                else if (FOREST_BIOMES.contains(id)) distForest++;
                else if (MOUNTAIN_BIOMES.contains(id)) distMountain++;
                else if (WATER_OR_SHORE_BIOMES.contains(id)) distWaterShore++;
                else distOther++;
            }
        }
        double meanY = sumY / distTotal;
        double stdY = Math.sqrt(Math.max(0.0, sumY2 / distTotal - meanY * meanY));

        Map<String, Object> biomeDist = new LinkedHashMap<>();
        biomeDist.put("sample_points", distTotal);
        biomeDist.put("pure_plains_pct", round2(100.0 * distPurePlains / distTotal));
        biomeDist.put("meadow_pct", round2(100.0 * distMeadow / distTotal));
        biomeDist.put("forest_pct", round2(100.0 * distForest / distTotal));
        biomeDist.put("eligible_biome_total_pct", round2(100.0 * distEligibleBiomes / distTotal));
        biomeDist.put("mountain_pct", round2(100.0 * distMountain / distTotal));
        biomeDist.put("water_shore_pct", round2(100.0 * distWaterShore / distTotal));
        biomeDist.put("other_pct", round2(100.0 * distOther / distTotal));
        biomeDist.put("surface_y_min", minY);
        biomeDist.put("surface_y_mean", round2(meanY));
        biomeDist.put("surface_y_max", maxY);
        biomeDist.put("surface_y_stddev", round2(stdY));
        final int totalSamples = distTotal;
        List<String> top5 = topBiomes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> e.getKey() + "=" + round2(100.0 * e.getValue() / totalSamples) + "%")
                .toList();
        biomeDist.put("top_5_biomes", top5);

        // B. Evaluate St03Eligibility.evaluate across +-regionRadius
        Map<St03Eligibility.Verdict, Integer> verdicts = new EnumMap<>(St03Eligibility.Verdict.class);
        for (var v : St03Eligibility.Verdict.values()) {
            verdicts.put(v, 0);
        }

        int totalCandidates = 0;
        int centerBiomeEligible = 0;
        int fullBiomeEligible = 0;
        int waterFailedAfterFullBiome = 0;
        int heightDiffFailedAfterFullBiome = 0;
        int cutFailedAfterFullBiome = 0;
        int skirtFailedAfterFullBiome = 0;

        // Counterfactual counters on full-biome-eligible sites + sampled center-biome-eligible sites
        int cfFullBiomePassDiff8NoWater = 0;
        int cfFullBiomePassDiff10NoWater = 0;
        int cfFullBiomePassDiff12NoWater = 0;
        int cfFullBiomePassDiff5AllowWater5Pct = 0;
        int cfFullBiomePassDiff8AllowWater5Pct = 0;

        int centerOnlySampled = 0;
        int cfCenterBiome80PassDiff5NoWater = 0;
        int cfCenterBiome80PassDiff8NoWater = 0;
        int cfCenterBiome80PassDiff10NoWater = 0;
        int cfCenterBiomeOnlyPassDiff5NoWater = 0;
        int cfCenterBiomeOnlyPassDiff8NoWater = 0;
        int cfCenterBiomeOnlyPassDiff10NoWater = 0;

        // Reuse terrain cache across nearby evaluations? Keep per-candidate map like GeneratorTerrain to stay exact & bounded!
        final Map<Long, Integer> surfCache = new HashMap<>(2048);
        final Map<Long, Integer> floorCache = new HashMap<>(2048);
        St03Eligibility.Terrain terrain = new St03Eligibility.Terrain() {
            @Override
            public int surfaceHeight(int x, int z) {
                return surfCache.computeIfAbsent(key(x, z),
                        k -> generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, rs));
            }

            @Override
            public int oceanFloorHeight(int x, int z) {
                return floorCache.computeIfAbsent(key(x, z),
                        k -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, rs));
            }

            @Override
            public boolean isEligibleBiomeCell(int quartX, int quartY, int quartZ) {
                return St03HeadquartersStructure.isEligibleBiome(resolver.getNoiseBiome(quartX, quartY, quartZ));
            }
        };

        int spacing = placement.spacing();
        int candidateIdx = 0;
        for (int rx = -regionRadius; rx <= regionRadius; rx++) {
            for (int rz = -regionRadius; rz <= regionRadius; rz++) {
                totalCandidates++;
                candidateIdx++;
                surfCache.clear();
                floorCache.clear();

                ChunkPos pc = placement.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                int originX = pc.getMinBlockX() - St03PlacementConfig.BOX_SIZE / 2 + 8;
                int originZ = pc.getMinBlockZ() - St03PlacementConfig.BOX_SIZE / 2 + 8;

                int cx = originX + St03PlacementConfig.BOX_SIZE / 2;
                int cz = originZ + St03PlacementConfig.BOX_SIZE / 2;
                int cSurfY = terrain.surfaceHeight(cx, cz) - 1;
                int cQy = Math.floorDiv(cSurfY - 2, 4);
                boolean centerOk = terrain.isEligibleBiomeCell(Math.floorDiv(cx, 4), cQy, Math.floorDiv(cz, 4))
                        && terrain.isEligibleBiomeCell(Math.floorDiv(cx, 4), cQy + 1, Math.floorDiv(cz, 4));
                if (centerOk) {
                    centerBiomeEligible++;
                }

                St03Eligibility.Result res = St03Eligibility.evaluate(terrain, originX, originZ);
                verdicts.merge(res.verdict(), 1, Integer::sum);

                if (res.verdict() != St03Eligibility.Verdict.REJECT_BIOME) {
                    fullBiomeEligible++;
                    if (res.verdict() == St03Eligibility.Verdict.REJECT_WATER) waterFailedAfterFullBiome++;
                    else if (res.verdict() == St03Eligibility.Verdict.REJECT_HEIGHT_DIFFERENCE) heightDiffFailedAfterFullBiome++;
                    else if (res.verdict() == St03Eligibility.Verdict.REJECT_CUT) cutFailedAfterFullBiome++;
                    else if (res.verdict() == St03Eligibility.Verdict.REJECT_SKIRT) skirtFailedAfterFullBiome++;

                    if (collectCounterfactuals) {
                        // Measure exact counterfactual terrain stats (non-short-circuiting) on full-biome-passing sites
                        TerrainSummary ts = sampleFullTerrain(terrain, originX, originZ, 4);
                        if (ts.waterCells == 0) {
                            if (ts.diff <= 8) cfFullBiomePassDiff8NoWater++;
                            if (ts.diff <= 10) cfFullBiomePassDiff10NoWater++;
                            if (ts.diff <= 12) cfFullBiomePassDiff12NoWater++;
                        }
                        if (ts.waterPct <= 5.0) {
                            if (ts.diff <= 5) cfFullBiomePassDiff5AllowWater5Pct++;
                            if (ts.diff <= 8) cfFullBiomePassDiff8AllowWater5Pct++;
                        }
                    }
                } else if (collectCounterfactuals && centerOk && centerOnlySampled < 80) {
                    centerOnlySampled++;
                    double biomeCov = footprintBiomeCoverage(terrain, originX, originZ);
                    TerrainSummary ts = sampleFullTerrain(terrain, originX, originZ, 8);
                    if (ts.waterCells == 0) {
                        if (ts.diff <= 5) cfCenterBiomeOnlyPassDiff5NoWater++;
                        if (ts.diff <= 8) cfCenterBiomeOnlyPassDiff8NoWater++;
                        if (ts.diff <= 10) cfCenterBiomeOnlyPassDiff10NoWater++;
                        if (biomeCov >= 0.80) {
                            if (ts.diff <= 5) cfCenterBiome80PassDiff5NoWater++;
                            if (ts.diff <= 8) cfCenterBiome80PassDiff8NoWater++;
                            if (ts.diff <= 10) cfCenterBiome80PassDiff10NoWater++;
                        }
                    }
                }

                if (res.eligible()) {
                    Map<String, Object> cand = new LinkedHashMap<>();
                    cand.put("seed", seed);
                    cand.put("category", category);
                    cand.put("region_x", rx);
                    cand.put("region_z", rz);
                    cand.put("chunk_x", pc.x());
                    cand.put("chunk_z", pc.z());
                    cand.put("origin_x", originX);
                    cand.put("foundation_y", res.foundationY());
                    cand.put("origin_z", originZ);
                    cand.put("min_y", res.minHeight());
                    cand.put("max_y", res.maxHeight());
                    cand.put("height_diff", res.heightDifference());
                    cand.put("cut", res.cut());
                    cand.put("skirt", res.skirt());
                    outEligible.add(cand);
                }
            }
        }

        long elapsedMs = System.currentTimeMillis() - t0;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("seed", seed);
        row.put("category", category);
        row.put("region_radius", regionRadius);
        row.put("candidates_total", totalCandidates);
        row.put("eligible_count", verdicts.get(St03Eligibility.Verdict.ELIGIBLE));
        row.put("acceptance_rate_pct", round4(100.0 * verdicts.get(St03Eligibility.Verdict.ELIGIBLE) / totalCandidates));
        row.put("center_biome_eligible_count", centerBiomeEligible);
        row.put("full_footprint_biome_eligible_count", fullBiomeEligible);

        Map<String, Object> verdictMap = new LinkedHashMap<>();
        for (var e : verdicts.entrySet()) {
            verdictMap.put(e.getKey().name(), e.getValue());
        }
        verdictMap.put("NOT_IMPLEMENTED_CAVE_VOID_CHECK", 0);
        verdictMap.put("NOT_IMPLEMENTED_VILLAGE_STRUCTURE_OVERLAP", 0);
        row.put("verdicts", verdictMap);

        Map<String, Object> cf = new LinkedHashMap<>();
        cf.put("full_biome_water_reject", waterFailedAfterFullBiome);
        cf.put("full_biome_diff_gt_5_reject", heightDiffFailedAfterFullBiome);
        cf.put("full_biome_cut_gt_3_reject", cutFailedAfterFullBiome);
        cf.put("full_biome_skirt_gt_5_reject", skirtFailedAfterFullBiome);
        cf.put("cf_full_biome_diff_le_8_no_water", cfFullBiomePassDiff8NoWater);
        cf.put("cf_full_biome_diff_le_10_no_water", cfFullBiomePassDiff10NoWater);
        cf.put("cf_full_biome_diff_le_12_no_water", cfFullBiomePassDiff12NoWater);
        cf.put("cf_full_biome_diff_le_5_water_le_5pct", cfFullBiomePassDiff5AllowWater5Pct);
        cf.put("cf_full_biome_diff_le_8_water_le_5pct", cfFullBiomePassDiff8AllowWater5Pct);
        cf.put("center_only_nonfull_sampled", centerOnlySampled);
        cf.put("center_only_total_remaining", Math.max(0, centerBiomeEligible - fullBiomeEligible));
        cf.put("cf_sampled_biome80_diff_le_5_no_water", cfCenterBiome80PassDiff5NoWater);
        cf.put("cf_sampled_biome80_diff_le_8_no_water", cfCenterBiome80PassDiff8NoWater);
        cf.put("cf_sampled_biome80_diff_le_10_no_water", cfCenterBiome80PassDiff10NoWater);
        cf.put("cf_sampled_center_only_diff_le_5_no_water", cfCenterBiomeOnlyPassDiff5NoWater);
        cf.put("cf_sampled_center_only_diff_le_8_no_water", cfCenterBiomeOnlyPassDiff8NoWater);
        cf.put("cf_sampled_center_only_diff_le_10_no_water", cfCenterBiomeOnlyPassDiff10NoWater);
        row.put("diagnostics", cf);
        row.put("biome_distribution", biomeDist);
        row.put("elapsed_ms", elapsedMs);
        LOG.info("ST03_SITE_SEED seed={} cat={} candidates={} eligible={} verdicts={} ms={}",
                seed, category, totalCandidates, verdicts.get(St03Eligibility.Verdict.ELIGIBLE), verdictMap, elapsedMs);
        return row;
    }

    private static void runVerifyChunks(MinecraftServer server, Map<String, Object> result) {
        ServerLevel level = server.overworld();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        String genClass = gen.getClass().getName();
        LOG.info("ST03_VERIFY_CHUNKS seed={} generatorClass={}", level.getSeed(), genClass);
        result.put("seed", level.getSeed());
        result.put("generator_class", genClass);
        if (!(gen instanceof NoiseBasedChunkGenerator)) {
            throw new IllegalStateException("Expected NoiseBasedChunkGenerator, got " + genClass);
        }

        String targetsProp = System.getProperty("mm.st03.site.targets", "");
        if (targetsProp.isBlank()) {
            throw new IllegalStateException("Missing -Dmm.st03.site.targets=cx1,cz1;...");
        }

        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Structure st03 = structureRegistry.getValue(St03Worldgen.STRUCTURE_ID);
        if (st03 == null) {
            throw new IllegalStateException("ST-03 structure not registered");
        }

        var rs = level.getChunkSource().randomState();
        St03Eligibility.Terrain terrain = new St03Eligibility.Terrain() {
            @Override
            public int surfaceHeight(int x, int z) {
                return gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
            }

            @Override
            public int oceanFloorHeight(int x, int z) {
                return gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
            }

            @Override
            public boolean isEligibleBiomeCell(int qx, int qy, int qz) {
                return St03HeadquartersStructure.isEligibleBiome(level.getUncachedNoiseBiome(qx, qy, qz));
            }
        };

        List<Map<String, Object>> verifications = new ArrayList<>();
        for (String pair : targetsProp.split(";")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split(",");
            int cx = Integer.parseInt(parts[0].trim());
            int cz = Integer.parseInt(parts[1].trim());
            ChunkPos cp = new ChunkPos(cx, cz);
            int originX = cp.getMinBlockX() - St03PlacementConfig.BOX_SIZE / 2 + 8;
            int originZ = cp.getMinBlockZ() - St03PlacementConfig.BOX_SIZE / 2 + 8;

            St03Eligibility.Result eval = St03Eligibility.evaluate(terrain, originX, originZ);
            ChunkAccess startChunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = startChunk.getStartForStructure(st03);
            boolean validStart = start != null && start.isValid();

            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("chunk_x", cx);
            rec.put("chunk_z", cz);
            rec.put("origin_x", originX);
            rec.put("origin_z", originZ);
            rec.put("live_eligibility_verdict", eval.verdict().name());
            rec.put("live_foundation_y", eval.foundationY());
            rec.put("structure_start_valid", validStart);

            if (validStart) {
                BoundingBox box = start.getBoundingBox();
                rec.put("piece_count", start.getPieces().size());
                long assemblyPieces = start.getPieces().stream().filter(p -> p instanceof St03AssemblyPiece).count();
                rec.put("assembly_piece_count", assemblyPieces);
                rec.put("bounding_box", Map.of(
                        "min_x", box.minX(), "min_y", box.minY(), "min_z", box.minZ(),
                        "max_x", box.maxX(), "max_y", box.maxY(), "max_z", box.maxZ()));

                int minCx = (box.minX() >> 4) - 1;
                int maxCx = (box.maxX() >> 4) + 1;
                int minCz = (box.minZ() >> 4) - 1;
                int maxCz = (box.maxZ() >> 4) + 1;
                int chunksGenerated = 0;
                for (int x = minCx; x <= maxCx; x++) {
                    for (int z = minCz; z <= maxCz; z++) {
                        level.getChunk(x, z, ChunkStatus.FULL);
                        chunksGenerated++;
                    }
                }
                rec.put("full_chunks_generated", chunksGenerated);

                // Count placed non-air blocks inside piece bounding boxes & check St03HeadquartersData
                int pieceSamples = 0;
                int pieceSolidBlocks = 0;
                BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
                for (StructurePiece piece : start.getPieces()) {
                    BoundingBox pb = piece.getBoundingBox();
                    int px = (pb.minX() + pb.maxX()) / 2;
                    int pz = (pb.minZ() + pb.maxZ()) / 2;
                    for (int y = pb.minY(); y <= Math.min(pb.maxY(), pb.minY() + 6); y++) {
                        pieceSamples++;
                        if (!level.getBlockState(mpos.set(px, y, pz)).isAir()) {
                            pieceSolidBlocks++;
                        }
                    }
                }
                rec.put("piece_center_block_samples", pieceSamples);
                rec.put("piece_center_non_air_blocks", pieceSolidBlocks);

                // Check vanilla/other structure overlaps
                List<String> overlaps = new ArrayList<>();
                for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
                    for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                        LevelChunk lc = level.getChunk(x, z);
                        for (var refEntry : lc.getAllReferences().entrySet()) {
                            if (refEntry.getKey() == st03) continue;
                            for (long packed : refEntry.getValue()) {
                                ChunkPos otherPos = ChunkPos.unpack(packed);
                                StructureStart other = level.getChunk(otherPos.x(), otherPos.z(), ChunkStatus.STRUCTURE_STARTS)
                                        .getStartForStructure(refEntry.getKey());
                                if (other != null && other.isValid() && other.getBoundingBox().intersects(box)) {
                                    overlaps.add(structureRegistry.getKey(refEntry.getKey()) + "@" + otherPos.x() + "," + otherPos.z());
                                }
                            }
                        }
                    }
                }
                rec.put("other_structure_overlaps", overlaps);

                // Check cave/void air columns beneath foundationY (step 4 across 112x112, depth 1..12 below foundationY)
                int caveSampleCols = 0;
                int colsWithVoidDirectlyBelow = 0;
                int totalAirCellsBelow = 0;
                int fy = eval.foundationY();
                for (int dx = 0; dx < St03PlacementConfig.BOX_SIZE; dx += 4) {
                    for (int dz = 0; dz < St03PlacementConfig.BOX_SIZE; dz += 4) {
                        caveSampleCols++;
                        int airInCol = 0;
                        for (int dy = 1; dy <= 12; dy++) {
                            if (level.getBlockState(mpos.set(originX + dx, fy - dy, originZ + dz)).isAir()) {
                                airInCol++;
                            }
                        }
                        if (airInCol > 0) colsWithVoidDirectlyBelow++;
                        totalAirCellsBelow += airInCol;
                    }
                }
                rec.put("under_foundation_sampled_cols", caveSampleCols);
                rec.put("under_foundation_cols_with_air_1_to_12", colsWithVoidDirectlyBelow);
                rec.put("under_foundation_total_air_cells", totalAirCellsBelow);

                var hqData = St03HeadquartersData.get(level).instances();
                rec.put("hq_data_instances_count", hqData.size());
            }
            verifications.add(rec);
            LOG.info("ST03_VERIFY_CHUNK chunk={},{} validStart={} eval={}", cx, cz, validStart, eval.verdict());
        }
        result.put("verifications", verifications);
    }

    private record ScreenScore(long seed, int plains, int forest, int mountain, int water, int total) {}
    private record SeedEntry(long seed, String category) {}
    private record TerrainSummary(int min, int max, int yf, int diff, int cut, int skirt, int waterCells, double waterPct) {}

    private static TerrainSummary sampleFullTerrain(St03Eligibility.Terrain terrain, int originX, int originZ, int step) {
        int size = St03PlacementConfig.BOX_SIZE;
        int perAxis = (size - 1) / step + 2;
        int[] heights = new int[perAxis * perAxis];
        int count = 0;
        int water = 0;
        for (int dx = 0; dx < size; dx += step) {
            int sx = originX + dx;
            for (int dz = 0; dz < size; dz += step) {
                int sz = originZ + dz;
                int surf = terrain.surfaceHeight(sx, sz);
                int floor = terrain.oceanFloorHeight(sx, sz);
                if (surf > floor + 1) water++;
                heights[count++] = surf;
            }
            int ez = originZ + size - 1;
            int surfEdge = terrain.surfaceHeight(sx, ez);
            int floorEdge = terrain.oceanFloorHeight(sx, ez);
            if (surfEdge > floorEdge + 1) water++;
            heights[count++] = surfEdge;
        }
        int ex = originX + size - 1;
        for (int dz = 0; dz < size; dz += step) {
            int sz = originZ + dz;
            int surf = terrain.surfaceHeight(ex, sz);
            int floor = terrain.oceanFloorHeight(ex, sz);
            if (surf > floor + 1) water++;
            heights[count++] = surf;
        }
        int surfCorner = terrain.surfaceHeight(ex, originZ + size - 1);
        int floorCorner = terrain.oceanFloorHeight(ex, originZ + size - 1);
        if (surfCorner > floorCorner + 1) water++;
        heights[count++] = surfCorner;

        Arrays.sort(heights, 0, count);
        int min = heights[0];
        int max = heights[count - 1];
        int yf = heights[count / 2];
        int diff = max - min;
        int cut = Math.max(0, max - yf);
        int skirt = Math.max(0, yf - min);
        return new TerrainSummary(min, max, yf, diff, cut, skirt, water, 100.0 * water / Math.max(1, count));
    }

    private static double footprintBiomeCoverage(St03Eligibility.Terrain terrain, int originX, int originZ) {
        int qx0 = Math.floorDiv(originX - 2, 4);
        int qx1 = Math.floorDiv(originX + St03PlacementConfig.BOX_SIZE - 3, 4) + 1;
        int qz0 = Math.floorDiv(originZ - 2, 4);
        int qz1 = Math.floorDiv(originZ + St03PlacementConfig.BOX_SIZE - 3, 4) + 1;
        int total = 0, ok = 0;
        for (int qx = qx0; qx <= qx1; qx += 2) {
            for (int qz = qz0; qz <= qz1; qz += 2) {
                total++;
                int surfaceBlockY = terrain.surfaceHeight(qx * 4 + 2, qz * 4 + 2) - 1;
                int qy = Math.floorDiv(surfaceBlockY - 2, 4);
                if (terrain.isEligibleBiomeCell(qx, qy, qz) && terrain.isEligibleBiomeCell(qx, qy + 1, qz)) {
                    ok++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) ok / total;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
