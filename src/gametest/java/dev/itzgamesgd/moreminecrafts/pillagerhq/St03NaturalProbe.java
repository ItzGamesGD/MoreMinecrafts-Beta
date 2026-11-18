package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.*;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server actual start survey, 4-stage (Baseline / A / A+B / A+B+C) measurement,
 * and natural-completion/first-proximity validation. Inert without opt-in.
 */
public final class St03NaturalProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st03-natural-v2");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int RADIUS = 25, COMMON_MIN = -1200, COMMON_MAX = 1248;
    private static final long SIDE = 51L * 66 * 16, AREA = SIDE * SIDE, COMMON_AREA = 2448L * 16 * 2448 * 16;
    private static final double PREDICTED = 64000000.0 * (1739.0 / 26010) / (256 * 66 * 66);
    private static final int[] OFFSETS_112 = St03Eligibility.sampleOffsets(112);
    private static final int[] OFFSETS_96 = St03Eligibility.sampleOffsets(96);
    private static final Set<String> ALL_19 = Set.copyOf(St03BiomeRules.ELIGIBLE_BIOME_IDS);
    private static final Set<String> EDGE_5 = Set.of(
            "minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore",
            "minecraft:river", "minecraft:frozen_river");

    private static Session session;

    @Override
    public void onInitialize() {
        String output = System.getProperty("mm.st03.natural.out");
        if (output == null) return;
        Identifier after = Identifier.fromNamespaceAndPath("moreminecrafts", "st03_natural_validation");
        ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(Event.DEFAULT_PHASE, after);
        ServerTickEvents.END_SERVER_TICK.register(after, server -> {
            if (session == null) return;
            try { session.tick(); } catch (Throwable t) { session.finish(t); }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> result = new LinkedHashMap<>();
            long start = System.nanoTime();
            Path dir = Path.of(output);
            result.put("head_sha", System.getenv("GITHUB_SHA"));
            result.put("run_id", System.getenv("GITHUB_RUN_ID"));
            result.put("seed", server.overworld().getSeed());
            try {
                Files.createDirectories(dir);
                String saved = System.getProperty("mm.st03.natural.scan");
                if (saved == null) scan(server.overworld(), dir, result);
                else restoreScan(server.overworld(), Path.of(saved), result);
                Files.writeString(dir.resolve("scan.json"), JSON.toJson(result));
                if (Boolean.getBoolean("mm.st03.natural.scanOnly")) {
                    result.put("scope", "actual_start_scan_only");
                    result.put("full_generation_status", "NOT RUN");
                    result.put("full_generation_reason", "Standalone start survey; full terrain/completion validation runs separately");
                    writeCapturePlan(dir, result);
                    writeAndExit(server, dir, result, start, null);
                    return;
                }
                if (server.overworld().getSeed() == 10079) {
                    long testStart = System.nanoTime();
                    result.put("normal_world_terrain_gametest", St03TerrainGameTests.verifyCorrectionFixture(server.overworld(), new BlockPos(400000, 240, 400000)));
                    result.put("normal_world_terrain_gametest_seconds", (System.nanoTime() - testStart) / 1e9);
                }
                session = new Session(server, dir, result, start);
            } catch (Throwable t) {
                writeAndExit(server, dir, result, start, t);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void restoreScan(ServerLevel level, Path source, Map<String, Object> result) throws Exception {
        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator))
            throw new AssertionError("Runtime revalidation requires normal noise world");
        Map<String, Object> saved = JSON.fromJson(Files.readString(source), Map.class);
        if (((Number) saved.get("seed")).longValue() != level.getSeed() || ((Number) saved.get("potential_candidates")).intValue() != 2601)
            throw new AssertionError("Saved scan seed/region suite mismatch");
        if (((Number) saved.get("spacing")).intValue() != 66 || ((Number) saved.get("separation")).intValue() != 22)
            throw new AssertionError("Saved scan must use approved 66/22");
        if (!saved.containsKey("actual_column_gate_site_checks") || ((Number) saved.get("dense_heightmap_parity_columns")).intValue() != 768)
            throw new AssertionError("Runtime replay requires a completed full-column-policy placement scan");
        if (Boolean.getBoolean("mm.st03.natural.scanOnly"))
            throw new AssertionError("Saved scans are for full runtime revalidation only");
        Object head = result.get("head_sha"), run = result.get("run_id");
        result.putAll(saved);
        result.put("source_scan_head", saved.get("head_sha"));
        result.put("source_scan_run", saved.get("run_id"));
        result.put("head_sha", head);
        result.put("run_id", run);
        result.put("scope", "fresh_world_runtime_revalidation_from_saved_start_scan");
        LOG.info("ST03_NATURAL_GENERATOR seed={} class={} source_scan_run={}", level.getSeed(), level.getChunkSource().getGenerator().getClass().getName(), saved.get("run_id"));
    }

    @SuppressWarnings("unchecked")
    private static void writeCapturePlan(Path dir, Map<String, Object> result) throws Exception {
        var starts = (List<Map<String, Object>>) result.get("starts");
        var targets = List.copyOf(prioritizeNewTargets(starts).subList(0, Math.min(3, starts.size())));
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("seed", result.get("seed"));
        plan.put("head_sha", result.get("head_sha"));
        plan.put("run_id", result.get("run_id"));
        plan.put("targets", targets);
        Files.writeString(dir.resolve("capture-plan.json"), JSON.toJson(plan));
    }

    private static void writeAndExit(MinecraftServer server, Path dir, Map<String, Object> result, long start, Throwable error) {
        result.put("status", error == null ? "PASS" : "FAIL");
        result.put("total_seconds", (System.nanoTime() - start) / 1e9);
        if (error != null) {
            LOG.error("Natural placement validation failed", error);
            result.put("error", error.toString());
            result.put("stack", Arrays.toString(error.getStackTrace()));
        }
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("result.json"), JSON.toJson(result));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().halt(error == null ? 0 : 1);
    }

    /**
     * Full-generation targets prefer starts newly unlocked by A+B+C (`newly_unlocked_vs_baseline == true`)
     * that do not overlap a village, preferring the 11 newly approved biomes first (stable scan order otherwise).
     */
    private static List<Map<String, Object>> prioritizeNewTargets(List<Map<String, Object>> starts) {
        List<Map<String, Object>> ordered = new ArrayList<>(starts);
        ordered.sort(Comparator
                .comparingInt((Map<String, Object> row) -> Boolean.TRUE.equals(row.get("overlap_village")) ? 1 : 0)
                .thenComparingInt(row -> Boolean.TRUE.equals(row.get("newly_unlocked_vs_baseline")) ? 0 : 1)
                .thenComparingInt(row -> St03BiomeRules.ADDED_BIOME_IDS.contains(row.get("center_biome")) ? 0 : 1));
        return ordered;
    }

    private static final class FastEvalCache {
        final ServerLevel level;
        final NoiseBasedChunkGenerator gen;
        final RandomState rs;
        final BiomeResolver resolver;
        final Map<Long, Integer> surfaces = new HashMap<>();
        final Map<Long, Integer> floors = new HashMap<>();
        final Map<Long, String> biomeAtQuart = new HashMap<>();
        final Map<Long, int[]> colBiomes = new HashMap<>();
        final Map<Long, St03TerrainColumns.Columns> denseChunks = new HashMap<>();
        final Map<String, Integer> biomeDict = new HashMap<>();
        final List<String> idByIdx = new ArrayList<>();

        FastEvalCache(ServerLevel level, NoiseBasedChunkGenerator gen, RandomState rs, net.minecraft.world.level.biome.Climate.Sampler sampler) {
            this.level = level;
            this.gen = gen;
            this.rs = rs;
            this.resolver = gen.getBiomeSource().createResolver(sampler);
        }

        static long key2(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }
        static long key3(int x, int y, int z) { return (((long) (x & 0x1FFFFF)) << 42) | (((long) (y & 0x1FFFFF)) << 21) | (z & 0x1FFFFFL); }

        void clear() {
            surfaces.clear();
            floors.clear();
            biomeAtQuart.clear();
            colBiomes.clear();
            denseChunks.clear();
        }

        int surface(int x, int z) {
            St03TerrainColumns.Columns dc = denseChunks.get(key2(x >> 4, z >> 4));
            if (dc != null) return dc.surfaceAt(x, z);
            return surfaces.computeIfAbsent(key2(x, z), k -> gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs));
        }

        int floor(int x, int z) {
            St03TerrainColumns.Columns dc = denseChunks.get(key2(x >> 4, z >> 4));
            if (dc != null) return dc.floorAt(x, z);
            return floors.computeIfAbsent(key2(x, z), k -> gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs));
        }

        St03TerrainColumns.Columns dense(int x, int z) {
            return denseChunks.computeIfAbsent(key2(x >> 4, z >> 4), k -> St03TerrainColumns.read(gen, level, rs, x & ~15, z & ~15));
        }

        String quartBiome(int qx, int qy, int qz) {
            return biomeAtQuart.computeIfAbsent(key3(qx, qy, qz), k -> {
                Holder<Biome> h = resolver.getNoiseBiome(qx, qy, qz);
                return h.unwrapKey().map(rk -> rk.identifier().toString()).orElse("unknown");
            });
        }

        int biomeIdx(String id) {
            Integer idx = biomeDict.get(id);
            if (idx == null) {
                idx = idByIdx.size();
                biomeDict.put(id, idx);
                idByIdx.add(id);
            }
            return idx;
        }

        int[] columnBiomeInfo(int x, int z) {
            return colBiomes.computeIfAbsent(key2(x, z), k -> {
                int qx = Math.floorDiv(x, 4), qz = Math.floorDiv(z, 4);
                int qy = Math.floorDiv(surface(x, z) - 3, 4);
                String b0 = quartBiome(qx, qy, qz);
                String b1 = quartBiome(qx, qy + 1, qz);
                int k19 = (ALL_19.contains(b0) && ALL_19.contains(b1)) ? 0
                        : (EDGE_5.contains(b0) && EDGE_5.contains(b1)) ? 1 : 2;
                return new int[]{biomeIdx(b0), k19};
            });
        }
    }

    private static final class OffsetEval {
        final int dx, dz, originX, originZ;
        final String centerBiome;
        final boolean centerPass;
        // 112x112 stats (for Stage 1 Baseline)
        int good112, edge112, water112, pWidth112, rWidth112, sampleY112, optCut112, optSkirt112, optFill112, score112;
        String sampleVerdict112 = "REJECT_CENTER_BIOME";
        boolean denseEval112;
        String denseVerdict112 = "NONE";
        // 96x96 stats (for Stage 2 A, Stage 3 A+B, Stage 4 A+B+C)
        int good96, edge96, water96, pWidth96, rWidth96, sampleY96, optCut96, optSkirt96, optFill96, score96;
        String sampleVerdict96W5 = "REJECT_CENTER_BIOME";
        String sampleVerdict96W15 = "REJECT_CENTER_BIOME";
        boolean denseEval96;
        int denseMin96, denseMax96, denseFloorMin96, denseWaterCols96, denseWaterCols112;
        String denseVerdict96FixedY = "NONE";
        String denseVerdict96ReclampY = "NONE";
        int reclampY96;

        OffsetEval(FastEvalCache c, int originX, int originZ, int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
            this.originX = originX;
            this.originZ = originZ;
            int[] ci = c.columnBiomeInfo(originX + 56, originZ + 56);
            this.centerBiome = c.idByIdx.get(ci[0]);
            this.centerPass = (ci[1] == 0);
            if (!centerPass) return;

            // 112x112 sample evaluation
            int g112 = 0, e112 = 0, g96 = 0, e96 = 0;
            for (int ix = 0; ix < 15; ix++) {
                int x = originX - 2 + ix * 8;
                for (int iz = 0; iz < 15; iz++) {
                    int z = originZ - 2 + iz * 8;
                    int[] bi = c.columnBiomeInfo(x, z);
                    if (bi[1] == 0) g112++;
                    else if (bi[1] == 1) e112++;
                    if (ix >= 1 && ix <= 13 && iz >= 1 && iz <= 13) {
                        if (bi[1] == 0) g96++;
                        else if (bi[1] == 1) e96++;
                    }
                }
            }
            this.good112 = g112;
            this.edge112 = e112;
            this.good96 = g96;
            this.edge96 = e96;

            int denom112 = 225 - e112;
            boolean fp112 = denom112 > 0 && 100L * g112 >= 80L * denom112;
            int denom96 = 169 - e96;
            boolean fp96 = denom96 > 0 && 100L * g96 >= 80L * denom96;

            if (fp112) {
                int[] h112 = new int[225];
                int n = 0, w = 0, fMin = Integer.MAX_VALUE;
                for (int ox : OFFSETS_112) {
                    for (int oz : OFFSETS_112) {
                        int s = c.surface(originX + ox, originZ + oz);
                        int f = c.floor(originX + ox, originZ + oz);
                        h112[n++] = s;
                        if (s > f + 1) w++;
                        if (f < fMin) fMin = f;
                    }
                }
                this.water112 = w;
                Arrays.sort(h112);
                int min = h112[0], max = h112[224], med = h112[112];
                this.pWidth112 = St03Eligibility.nearestRank(h112, 0.95) - St03Eligibility.nearestRank(h112, 0.05);
                this.rWidth112 = max - min;
                int low = max - 10, highS = min + 12, highF = fMin + 16;
                this.sampleY112 = Math.max(low, Math.min(med, Math.min(highS, highF)));
                this.optCut112 = Math.max(0, max - sampleY112);
                this.optSkirt112 = Math.max(0, sampleY112 - min);
                this.optFill112 = Math.max(0, sampleY112 - fMin);
                this.score112 = Math.max(optCut112, Math.max(optSkirt112, optFill112)) * 100000 + rWidth112 * 1000 + Math.max(Math.abs(dx), Math.abs(dz)) * 10;
                if (100L * w > 5L * 225L) sampleVerdict112 = "REJECT_WATER";
                else if (pWidth112 > 20) sampleVerdict112 = "REJECT_HEIGHT_DIFFERENCE";
                else if (low > highS) sampleVerdict112 = "REJECT_CUT_SKIRT";
                else if (low > highF) sampleVerdict112 = "REJECT_FOOTING_FILL";
                else sampleVerdict112 = "ELIGIBLE";
            } else {
                sampleVerdict112 = "REJECT_FOOTPRINT_BIOME";
            }

            if (fp96) {
                int[] h96 = new int[169];
                int n = 0, w = 0, fMin = Integer.MAX_VALUE;
                int ox0 = originX + 8, oz0 = originZ + 8;
                for (int ox : OFFSETS_96) {
                    for (int oz : OFFSETS_96) {
                        int s = c.surface(ox0 + ox, oz0 + oz);
                        int f = c.floor(ox0 + ox, oz0 + oz);
                        h96[n++] = s;
                        if (s > f + 1) w++;
                        if (f < fMin) fMin = f;
                    }
                }
                this.water96 = w;
                Arrays.sort(h96);
                int min = h96[0], max = h96[168], med = h96[84];
                this.pWidth96 = St03Eligibility.nearestRank(h96, 0.95) - St03Eligibility.nearestRank(h96, 0.05);
                this.rWidth96 = max - min;
                int low = max - 10, highS = min + 12, highF = fMin + 16;
                this.sampleY96 = Math.max(low, Math.min(med, Math.min(highS, highF)));
                this.optCut96 = Math.max(0, max - sampleY96);
                this.optSkirt96 = Math.max(0, sampleY96 - min);
                this.optFill96 = Math.max(0, sampleY96 - fMin);
                this.score96 = Math.max(optCut96, Math.max(optSkirt96, optFill96)) * 100000 + rWidth96 * 1000 + Math.max(Math.abs(dx), Math.abs(dz)) * 10;

                sampleVerdict96W5 = classify96(w, 5, pWidth96, low, highS, highF);
                sampleVerdict96W15 = classify96(w, 15, pWidth96, low, highS, highF);
            } else {
                sampleVerdict96W5 = "REJECT_FOOTPRINT_BIOME";
                sampleVerdict96W15 = "REJECT_FOOTPRINT_BIOME";
            }
        }

        private static String classify96(int w, int maxWaterPct, int pW, int low, int highS, int highF) {
            if (100L * w > (long) maxWaterPct * 169L) return "REJECT_WATER";
            if (pW > 20) return "REJECT_HEIGHT_DIFFERENCE";
            if (low > highS) return "REJECT_CUT_SKIRT";
            if (low > highF) return "REJECT_FOOTING_FILL";
            return "ELIGIBLE";
        }

        void ensureDense112(FastEvalCache c) {
            if (denseEval112) return;
            denseEval112 = true;
            var res = St03FullTerrainCheck.evaluate((x, z) -> c.dense(x, z).surfaceAt(x, z),
                    (x, z) -> c.dense(x, z).floorAt(x, z), originX, originZ, sampleY112, 112, false);
            denseVerdict112 = res.eligible() ? "ELIGIBLE" : "REJECT_DENSE_" + res.verdict().name();
        }

        void ensureDense96(FastEvalCache c) {
            if (denseEval96) return;
            denseEval96 = true;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, fMin = Integer.MAX_VALUE, wCols96 = 0, wCols112 = 0;
            String fixedFail = null;
            for (int x = originX; x < originX + 112; x++) {
                for (int z = originZ; z < originZ + 112; z++) {
                    var col = c.dense(x, z);
                    int s = col.surfaceAt(x, z), f = col.floorAt(x, z);
                    if (s > f + 1) wCols112++;
                    if (x >= originX + 8 && x < originX + 104 && z >= originZ + 8 && z < originZ + 104) {
                        if (s < min) min = s;
                        if (s > max) max = s;
                        if (f < fMin) fMin = f;
                        if (s > f + 1) wCols96++;
                        if (fixedFail == null) {
                            if (s - sampleY96 > 10) fixedFail = "REJECT_DENSE_CUT";
                            else if (sampleY96 - s > 12) fixedFail = "REJECT_DENSE_SKIRT";
                            else if (sampleY96 - f > 16) fixedFail = "REJECT_DENSE_FOOTING_FILL";
                        }
                    }
                }
            }
            this.denseMin96 = min;
            this.denseMax96 = max;
            this.denseFloorMin96 = fMin;
            this.denseWaterCols96 = wCols96;
            this.denseWaterCols112 = wCols112;
            this.denseVerdict96FixedY = (fixedFail == null) ? "ELIGIBLE" : fixedFail;

            int low = max - 10, highS = min + 12, highF = fMin + 16;
            int high = Math.min(highS, highF);
            if (low <= high) {
                this.reclampY96 = Math.max(low, Math.min(sampleY96, high));
                this.denseVerdict96ReclampY = "ELIGIBLE";
            } else {
                this.reclampY96 = sampleY96;
                int cutAtSample = Math.max(0, max - sampleY96);
                int skirtAtSample = Math.max(0, sampleY96 - min);
                if (cutAtSample > 10) this.denseVerdict96ReclampY = "REJECT_DENSE_CUT";
                else if (skirtAtSample > 12 || highS <= highF) this.denseVerdict96ReclampY = "REJECT_DENSE_SKIRT";
                else this.denseVerdict96ReclampY = "REJECT_DENSE_FOOTING_FILL";
            }
        }
    }

    private static final Map<String, Integer> STAGE_RANK = Map.of(
            "REJECT_CENTER_BIOME", 0,
            "REJECT_FOOTPRINT_BIOME", 1,
            "REJECT_WATER", 2,
            "REJECT_HEIGHT_DIFFERENCE", 3,
            "REJECT_CUT_SKIRT", 4,
            "REJECT_FOOTING_FILL", 5,
            "REJECT_DENSE_CUT", 6,
            "REJECT_DENSE_SKIRT", 7,
            "REJECT_DENSE_FOOTING_FILL", 8,
            "ELIGIBLE", 9
    );

    private static void scan(ServerLevel level, Path dir, Map<String, Object> result) throws Exception {
        Files.createDirectories(dir);
        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator gen))
            throw new IllegalStateException("Expected normal noise world");
        LOG.info("ST03_NATURAL_GENERATOR seed={} class={}", level.getSeed(), gen.getClass().getName());
        var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, St03Worldgen.STRUCTURE_ID));
        Structure structure = holder.value();
        var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, St03Worldgen.STRUCTURE_ID)).value();
        var placement = (RandomSpreadStructurePlacement) set.placement();
        if (placement.spacing() != 66 || placement.separation() != 22 || !placement.spreadType().getSerializedName().equals("triangular"))
            throw new AssertionError("66/22 TRIANGULAR required");
        var state = level.getChunkSource().getGeneratorState();
        var rs = level.getChunkSource().randomState();
        var sampler = rs.createClimateSampler(SamplerContext.builder().enableCaches().build());

        // Pre-discover village structure sets for fast village overlap check on valid starts
        List<Holder<StructureSet>> villageSets = new ArrayList<>();
        for (Holder<StructureSet> h : state.possibleStructureSets()) {
            String id = h.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (id.contains("village")) villageSets.add(h);
        }
        Map<String, Optional<BoundingBox>> villageBoxCache = new HashMap<>();

        result.put("generator", gen.getClass().getName());
        result.put("spacing", 66);
        result.put("separation", 22);
        result.put("salt", 91403);
        result.put("eval_box_size", St03PlacementConfig.EVAL_BOX_SIZE);
        result.put("eval_samples", St03PlacementConfig.EVAL_SAMPLES);
        result.put("eval_columns", St03PlacementConfig.EVAL_COLUMNS);
        result.put("max_water_percent", St03PlacementConfig.MAX_WATER_PERCENT);
        result.put("max_water_samples_96", (St03PlacementConfig.EVAL_SAMPLES * St03PlacementConfig.MAX_WATER_PERCENT) / 100);
        result.put("region_radius", RADIUS);
        result.put("area_blocks_squared", AREA);
        result.put("min_block_inclusive", -25 * 66 * 16);
        result.put("max_block_exclusive", 26 * 66 * 16);
        result.put("predicted_per_64km2", PREDICTED);
        result.put("common_min_chunk", COMMON_MIN);
        result.put("common_max_chunk_exclusive", COMMON_MAX);
        result.put("common_area_blocks_squared", COMMON_AREA);

        int potential = 0, placementPass = 0, valid = 0, commonPotential = 0, commonValid = 0;
        Map<St03FullTerrainCheck.Verdict, Long> before = new EnumMap<>(St03FullTerrainCheck.Verdict.class);
        St03HeadquartersStructure.ACTUAL_COLUMN_COUNTS.forEach((v, n) -> before.put(v, n.get()));

        Map<String, Integer> centerBiomeCandidates = new TreeMap<>(), centerEligibleByBiome = new TreeMap<>(), validByCenterBiome = new TreeMap<>();
        List<Map<String, Object>> starts = new ArrayList<>();

        // 4-stage counters: [0]=Baseline(112,W5,FixedY), [1]=StageA(96,W5,FixedY), [2]=StageAB(96,W5,ReclampY), [3]=StageABC(96,W15,ReclampY)
        String[] stageNames = {"stage1_baseline_112_w5_fixedY", "stage2_A_96_w5_fixedY", "stage3_AB_96_w5_reclampY", "stage4_ABC_96_w15_reclampY"};
        int[] stageValid = new int[4];
        int[] stageVillageOverlaps = new int[4];
        List<Map<String, Integer>> stageN0Pareto = List.of(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>());
        List<Map<String, Integer>> stageN2Pareto = List.of(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>());
        List<Map<String, Integer>> stageByBiomeN0Cand = List.of(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>());
        List<Map<String, Integer>> stageByBiomeSelected = List.of(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>());

        // Stage B invariance & rescue counters
        int stageAValidCount = 0;
        int stageAYUnchangedInAB = 0;
        int stageAYChangedInAB = 0;
        int stageABetterOffsetSelectedInAB = 0;
        int stageAAllPassingOffsetsChecked = 0;
        int stageAAllPassingOffsetsYChanged = 0;
        int baselineValidCount = 0;
        int baselineSameSiteInABC = 0;
        int baselineSameSiteYUnchangedInABC = 0;
        int stageBRescuedCandidates = 0;
        int stageCRescuedCandidates = 0;
        Map<String, Integer> stageBRescuedYDeltaHist = new TreeMap<>();
        Map<Integer, Integer> stageABCWaterSamplesHist = new TreeMap<>();
        Map<Integer, Integer> stageABCWaterCols96Hist = new TreeMap<>();
        Map<Integer, Integer> stageABCWaterCols112Hist = new TreeMap<>();

        FastEvalCache fastCache = new FastEvalCache(level, gen, rs, sampler);
        long prodScanNs = 0;
        long totalStarted = System.nanoTime();

        try (BufferedWriter out = Files.newBufferedWriter(dir.resolve("starts.csv"));
             BufferedWriter stagesCsv = Files.newBufferedWriter(dir.resolve("candidates_4stages.csv"))) {
            out.write("seed,rx,rz,candidate_chunk_x,candidate_chunk_z,valid,origin_x,foundation_y,origin_z,dx_chunks,dz_chunks,pieces,common_rectangle\n");
            stagesCsv.write("seed,rx,rz,cx,cz,n0_center_biome,n0_center_eligible,"
                    + "s1_n0_verdict,s1_n2_verdict,s1_pass,s1_dx,s1_dz,s1_y,"
                    + "s2_n0_verdict,s2_n2_verdict,s2_pass,s2_dx,s2_dz,s2_y,"
                    + "s3_n0_verdict,s3_n2_verdict,s3_pass,s3_dx,s3_dz,s3_y,s3_sample_y,"
                    + "s4_n0_verdict,s4_n2_verdict,s4_pass,s4_dx,s4_dz,s4_y,s4_sample_y,s4_water_samples_169,s4_water_cols_96,s4_water_cols_112,s4_overlap_village\n");

            for (int rx = -RADIUS; rx <= RADIUS; rx++) {
                for (int rz = -RADIUS; rz <= RADIUS; rz++) {
                    fastCache.clear();
                    ChunkPos cp = placement.getPotentialStructureChunk(level.getSeed(), rx * 66, rz * 66);
                    boolean common = cp.x() >= COMMON_MIN && cp.x() < COMMON_MAX && cp.z() >= COMMON_MIN && cp.z() < COMMON_MAX;
                    potential++;
                    if (common) commonPotential++;
                    if (!placement.isStructureChunk(state, cp.x(), cp.z())) continue;
                    placementPass++;

                    int baseOx = cp.getMinBlockX() - St03PlacementConfig.BOX_SIZE / 2 + 8;
                    int baseOz = cp.getMinBlockZ() - St03PlacementConfig.BOX_SIZE / 2 + 8;

                    // 1. Evaluate all 25 N<=2 offsets for the 4 stages
                    OffsetEval[][] grid = new OffsetEval[5][5];
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            OffsetEval oe = new OffsetEval(fastCache, baseOx + dx * 16, baseOz + dz * 16, dx, dz);
                            if ("ELIGIBLE".equals(oe.sampleVerdict112)) oe.ensureDense112(fastCache);
                            if ("ELIGIBLE".equals(oe.sampleVerdict96W5) || "ELIGIBLE".equals(oe.sampleVerdict96W15)) {
                                oe.ensureDense96(fastCache);
                                if ("ELIGIBLE".equals(oe.denseVerdict96FixedY)) {
                                    stageAAllPassingOffsetsChecked++;
                                    if (oe.reclampY96 != oe.sampleY96) stageAAllPassingOffsetsYChanged++;
                                }
                            }
                            grid[dx + 2][dz + 2] = oe;
                        }
                    }

                    OffsetEval n0 = grid[2][2];
                    centerBiomeCandidates.merge(n0.centerBiome, 1, Integer::sum);
                    if (n0.centerPass) centerEligibleByBiome.merge(n0.centerBiome, 1, Integer::sum);

                    // Stage 1..4 N=0 verdicts & N=2 best selection
                    String[] n0Verdict = new String[4];
                    String[] n2Verdict = new String[4];
                    OffsetEval[] bestSite = new OffsetEval[4];
                    int[] bestY = new int[4];

                    for (int st = 0; st < 4; st++) {
                        n0Verdict[st] = offsetVerdict(n0, st);
                        stageN0Pareto.get(st).merge(n0Verdict[st], 1, Integer::sum);

                        String deepest = "REJECT_CENTER_BIOME";
                        int bestScore = Integer.MAX_VALUE;
                        OffsetEval chosen = null;
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                OffsetEval oe = grid[dx + 2][dz + 2];
                                String v = offsetVerdict(oe, st);
                                if (STAGE_RANK.get(v) > STAGE_RANK.get(deepest)) deepest = v;
                                int sc = (st == 0) ? oe.score112 : oe.score96;
                                String sVerdict = (st == 0) ? oe.sampleVerdict112 : (st <= 2 ? oe.sampleVerdict96W5 : oe.sampleVerdict96W15);
                                if ("ELIGIBLE".equals(sVerdict) && sc < bestScore && "ELIGIBLE".equals(v)) {
                                    bestScore = sc;
                                    chosen = oe;
                                }
                            }
                        }
                        n2Verdict[st] = (chosen != null) ? "ELIGIBLE" : deepest;
                        stageN2Pareto.get(st).merge(n2Verdict[st], 1, Integer::sum);
                        bestSite[st] = chosen;
                        if (chosen != null) {
                            bestY[st] = (st == 0) ? chosen.sampleY112 : (st == 1 ? chosen.sampleY96 : chosen.reclampY96);
                            stageValid[st]++;
                            if (n0.centerPass) stageByBiomeN0Cand.get(st).merge(n0.centerBiome, 1, Integer::sum);
                            stageByBiomeSelected.get(st).merge(chosen.centerBiome, 1, Integer::sum);
                        }
                    }

                    // Stage B Y-invariance checks
                    if (bestSite[1] != null) {
                        stageAValidCount++;
                        // 1) At the exact site selected by Stage A, Stage B recomputation must NEVER change Y
                        if (bestSite[1].reclampY96 == bestSite[1].sampleY96) {
                            stageAYUnchangedInAB++;
                        } else {
                            stageAYChangedInAB++;
                        }
                        // 2) Also check if Stage B rescued a lower-score (better) offset within the same N=2 neighborhood
                        if (bestSite[2] != null && (bestSite[2].dx != bestSite[1].dx || bestSite[2].dz != bestSite[1].dz)) {
                            stageABetterOffsetSelectedInAB++;
                        }
                    }
                    if (bestSite[0] != null) {
                        baselineValidCount++;
                        if (bestSite[3] != null && bestSite[3].dx == bestSite[0].dx && bestSite[3].dz == bestSite[0].dz) {
                            baselineSameSiteInABC++;
                            if (bestY[3] == bestY[0]) baselineSameSiteYUnchangedInABC++;
                        }
                    }
                    if (bestSite[1] == null && bestSite[2] != null) {
                        stageBRescuedCandidates++;
                        int deltaY = bestSite[2].reclampY96 - bestSite[2].sampleY96;
                        stageBRescuedYDeltaHist.merge((deltaY >= 0 ? "+" : "") + deltaY, 1, Integer::sum);
                    }
                    if (bestSite[2] == null && bestSite[3] != null) {
                        stageCRescuedCandidates++;
                    }
                    if (bestSite[3] != null) {
                        stageABCWaterSamplesHist.merge(bestSite[3].water96, 1, Integer::sum);
                        stageABCWaterCols96Hist.merge(bestSite[3].denseWaterCols96, 1, Integer::sum);
                        stageABCWaterCols112Hist.merge(bestSite[3].denseWaterCols112, 1, Integer::sum);
                    }

                    // Check village overlaps for each stage's valid start
                    boolean[] ovVillage = new boolean[4];
                    List<String> s4VillageHits = List.of();
                    for (int st = 0; st < 4; st++) {
                        if (bestSite[st] != null) {
                            BoundingBox hqBox = new BoundingBox(
                                    bestSite[st].originX, bestY[st] - 16, bestSite[st].originZ,
                                    bestSite[st].originX + 111, bestY[st] + 30, bestSite[st].originZ + 111);
                            List<String> hits = findVillageOverlaps(level, gen, rs, state, sampler, villageSets, villageBoxCache, hqBox);
                            ovVillage[st] = !hits.isEmpty();
                            if (ovVillage[st]) stageVillageOverlaps[st]++;
                            if (st == 3) s4VillageHits = hits;
                        }
                    }

                    // 2. Run actual production Structure.generate (timed separately for pure production scan_seconds)
                    long tProd0 = System.nanoTime();
                    StructureStart generated = structure.generate(holder, level.dimension(), level.registryAccess(), gen, gen.getBiomeSource(), sampler, rs,
                            level.getStructureTemplateManager(), level.getSeed(), cp, 0, level, structure.biomes()::contains);
                    prodScanNs += (System.nanoTime() - tProd0);

                    // Verify production Structure.generate matches Stage 4 (A+B+C)
                    if (generated.isValid() != (bestSite[3] != null)) {
                        throw new AssertionError("Production Structure.generate mismatch vs Stage 4 (A+B+C) at " + cp
                                + ": generated=" + generated.isValid() + ", stage4=" + (bestSite[3] != null));
                    }

                    stagesCsv.write(level.getSeed() + "," + rx + "," + rz + "," + cp.x() + "," + cp.z() + ","
                            + n0.centerBiome + "," + n0.centerPass + ","
                            + n0Verdict[0] + "," + n2Verdict[0] + "," + (bestSite[0] != null) + ","
                            + (bestSite[0] != null ? bestSite[0].dx : "") + "," + (bestSite[0] != null ? bestSite[0].dz : "") + "," + (bestSite[0] != null ? bestY[0] : "") + ","
                            + n0Verdict[1] + "," + n2Verdict[1] + "," + (bestSite[1] != null) + ","
                            + (bestSite[1] != null ? bestSite[1].dx : "") + "," + (bestSite[1] != null ? bestSite[1].dz : "") + "," + (bestSite[1] != null ? bestY[1] : "") + ","
                            + n0Verdict[2] + "," + n2Verdict[2] + "," + (bestSite[2] != null) + ","
                            + (bestSite[2] != null ? bestSite[2].dx : "") + "," + (bestSite[2] != null ? bestSite[2].dz : "") + "," + (bestSite[2] != null ? bestY[2] : "") + ","
                            + (bestSite[2] != null ? bestSite[2].sampleY96 : "") + ","
                            + n0Verdict[3] + "," + n2Verdict[3] + "," + (bestSite[3] != null) + ","
                            + (bestSite[3] != null ? bestSite[3].dx : "") + "," + (bestSite[3] != null ? bestSite[3].dz : "") + "," + (bestSite[3] != null ? bestY[3] : "") + ","
                            + (bestSite[3] != null ? bestSite[3].sampleY96 : "") + ","
                            + (bestSite[3] != null ? bestSite[3].water96 : "") + ","
                            + (bestSite[3] != null ? bestSite[3].denseWaterCols96 : "") + ","
                            + (bestSite[3] != null ? bestSite[3].denseWaterCols112 : "") + ","
                            + ovVillage[3] + "\n");

                    if (!generated.isValid()) {
                        out.write(level.getSeed() + "," + rx + "," + rz + "," + cp.x() + "," + cp.z() + ",false,,,,,,," + common + "\n");
                        continue;
                    }
                    valid++;
                    if (common) commonValid++;

                    var first = generated.getPieces().stream().filter(p -> p instanceof St03AssemblyPiece).map(p -> (St03AssemblyPiece) p).findFirst().orElseThrow();
                    BlockPos origin = first.compoundOrigin();
                    int dx = (origin.getX() - (cp.getMinBlockX() - 48)) / 16, dz = (origin.getZ() - (cp.getMinBlockZ() - 48)) / 16;
                    if (Math.abs(dx) > 2 || Math.abs(dz) > 2) throw new AssertionError("Selected site outside N=2");
                    if (origin.getX() != bestSite[3].originX || origin.getZ() != bestSite[3].originZ || origin.getY() != bestY[3]) {
                        throw new AssertionError("Selected site origin/Y mismatch vs Stage 4 at " + cp + ": " + origin + " vs " + bestSite[3].originX + "," + bestY[3] + "," + bestSite[3].originZ);
                    }

                    String vbId = bestSite[3].centerBiome;
                    validByCenterBiome.merge(vbId, 1, Integer::sum);

                    String stageUnlocked = (bestSite[0] != null) ? "baseline"
                            : (bestSite[1] != null) ? "A_96x96"
                            : (bestSite[2] != null) ? "B_reclampY" : "C_water15";

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("candidate_chunk_x", cp.x());
                    row.put("candidate_chunk_z", cp.z());
                    row.put("region_x", rx);
                    row.put("region_z", rz);
                    row.put("origin_x", origin.getX());
                    row.put("origin_z", origin.getZ());
                    row.put("foundation_y", origin.getY());
                    row.put("sample_foundation_y", bestSite[3].sampleY96);
                    row.put("common_rectangle", common);
                    row.put("center_biome", vbId);
                    row.put("n0_center_biome", n0.centerBiome);
                    row.put("dx_chunks", dx);
                    row.put("dz_chunks", dz);
                    row.put("pieces", generated.getPieces().size());
                    row.put("rotation", first.compoundRotation().name());
                    row.put("cut", Math.max(0, bestSite[3].denseMax96 - origin.getY()));
                    row.put("skirt", Math.max(0, origin.getY() - bestSite[3].denseMin96));
                    row.put("fill", Math.max(0, origin.getY() - bestSite[3].denseFloorMin96));
                    row.put("water_samples_169", bestSite[3].water96);
                    row.put("water_cols_96", bestSite[3].denseWaterCols96);
                    row.put("water_cols_112", bestSite[3].denseWaterCols112);
                    row.put("stage_unlocked", stageUnlocked);
                    row.put("newly_unlocked_vs_baseline", bestSite[0] == null);
                    row.put("overlap_village", ovVillage[3]);
                    row.put("village_overlaps", s4VillageHits);
                    starts.add(row);
                    out.write(level.getSeed() + "," + rx + "," + rz + "," + cp.x() + "," + cp.z() + ",true," + origin.getX() + "," + origin.getY() + "," + origin.getZ() + "," + dx + "," + dz + "," + generated.getPieces().size() + "," + common + "\n");
                }
                out.flush();
                stagesCsv.flush();
                LOG.info("ST03_NATURAL_PROGRESS seed={} region_x={} potential={} s1={} s2={} s3={} s4={} prod_scan_s={} total_s={}",
                        level.getSeed(), rx, potential, stageValid[0], stageValid[1], stageValid[2], stageValid[3],
                        prodScanNs / 1e9, (System.nanoTime() - totalStarted) / 1e9);
                Files.writeString(dir.resolve("progress.json"), JSON.toJson(Map.of(
                        "seed", level.getSeed(), "potential", potential, "valid", valid,
                        "s1_baseline", stageValid[0], "s2_A", stageValid[1], "s3_AB", stageValid[2], "s4_ABC", stageValid[3],
                        "prod_scan_seconds", prodScanNs / 1e9, "seconds", (System.nanoTime() - totalStarted) / 1e9)));
            }
        }

        Map<String, Object> fourStages = new LinkedHashMap<>();
        for (int st = 0; st < 4; st++) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("valid_starts", stageValid[st]);
            sm.put("acceptance_pct", 100.0 * stageValid[st] / potential);
            sm.put("starts_per_64km2", stageValid[st] * 64000000.0 / AREA);
            sm.put("village_overlaps", stageVillageOverlaps[st]);
            sm.put("n0_pareto", stageN0Pareto.get(st));
            sm.put("n2_pareto", stageN2Pareto.get(st));
            sm.put("valid_by_n0_center_biome", stageByBiomeN0Cand.get(st));
            sm.put("valid_by_selected_center_biome", stageByBiomeSelected.get(st));
            fourStages.put(stageNames[st], sm);
        }
        result.put("four_stage_comparison", fourStages);

        Map<String, Object> stageBVerification = new LinkedHashMap<>();
        stageBVerification.put("stageA_valid_count", stageAValidCount);
        stageBVerification.put("stageA_y_unchanged_in_AB", stageAYUnchangedInAB);
        stageBVerification.put("stageA_y_changed_in_AB", stageAYChangedInAB);
        stageBVerification.put("stageA_better_offset_selected_in_AB", stageABetterOffsetSelectedInAB);
        stageBVerification.put("stageA_all_passing_offsets_checked", stageAAllPassingOffsetsChecked);
        stageBVerification.put("stageA_all_passing_offsets_y_changed", stageAAllPassingOffsetsYChanged);
        stageBVerification.put("baseline_valid_count", baselineValidCount);
        stageBVerification.put("baseline_same_site_in_ABC", baselineSameSiteInABC);
        stageBVerification.put("baseline_same_site_y_unchanged_in_ABC", baselineSameSiteYUnchangedInABC);
        stageBVerification.put("stageB_rescued_candidates", stageBRescuedCandidates);
        stageBVerification.put("stageB_rescued_y_delta_hist", stageBRescuedYDeltaHist);
        stageBVerification.put("stageC_rescued_candidates", stageCRescuedCandidates);
        result.put("stage_b_verification", stageBVerification);

        Map<String, Object> stageCWaterDist = new LinkedHashMap<>();
        stageCWaterDist.put("water_samples_169_hist", stageABCWaterSamplesHist);
        stageCWaterDist.put("water_cols_96_hist", stageABCWaterCols96Hist);
        stageCWaterDist.put("water_cols_112_hist", stageABCWaterCols112Hist);
        result.put("stage_c_water_distribution", stageCWaterDist);

        result.put("center_biome_candidates", centerBiomeCandidates);
        result.put("center_eligible_by_biome", centerEligibleByBiome);
        result.put("center_eligible_total", centerEligibleByBiome.values().stream().mapToInt(Integer::intValue).sum());
        result.put("valid_starts_by_center_biome", validByCenterBiome);
        result.put("biome_list_size", St03BiomeRules.ELIGIBLE_BIOME_IDS.size());
        result.put("potential_candidates", potential);
        result.put("placement_pass", placementPass);
        result.put("valid_starts", valid);
        result.put("acceptance_pct", 100.0 * valid / potential);
        result.put("starts_per_64km2", valid * 64000000.0 / AREA);
        result.put("delta_vs_prediction_pct", 100 * (valid * 64000000.0 / AREA - PREDICTED) / PREDICTED);
        result.put("starts", starts);
        result.put("common_potential_candidates", commonPotential);
        result.put("common_valid_starts", commonValid);
        result.put("common_starts_per_64km2", commonValid * 64000000.0 / COMMON_AREA);
        result.put("scan_seconds", prodScanNs / 1e9);
        result.put("scan_ms_per_candidate", (prodScanNs / 1e6) / potential);
        result.put("total_with_4stage_audit_seconds", (System.nanoTime() - totalStarted) / 1e9);

        if (potential != 2601 || starts.isEmpty())
            throw new AssertionError("Incomplete suite or no actual starts");
        if (stageAYChangedInAB != 0 || stageAAllPassingOffsetsYChanged != 0)
            throw new AssertionError("Stage B changed foundationY on an already-passing Stage A site! changed="
                    + stageAYChangedInAB + ", allOffsetsChanged=" + stageAAllPassingOffsetsYChanged);

        Map<String, Long> denseCounts = new LinkedHashMap<>();
        St03HeadquartersStructure.ACTUAL_COLUMN_COUNTS.forEach((v, n) -> denseCounts.put(v.name(), n.get() - before.get(v)));
        result.put("actual_column_gate_site_checks", denseCounts);

        int parity = 0;
        for (var row : starts.subList(0, Math.min(3, starts.size()))) {
            int minX = ((Number) row.get("candidate_chunk_x")).intValue() * 16, minZ = ((Number) row.get("candidate_chunk_z")).intValue() * 16;
            var columns = St03TerrainColumns.read(gen, level, rs, minX, minZ);
            for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
                if (columns.surfaceAt(x, z) != gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs)
                        || columns.floorAt(x, z) != gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs))
                    throw new AssertionError("Dense WG heightmap parity failed at " + x + "," + z);
                parity++;
            }
        }
        result.put("dense_heightmap_parity_columns", parity);
    }

    private static String offsetVerdict(OffsetEval oe, int stage) {
        if (!oe.centerPass) return "REJECT_CENTER_BIOME";
        if (stage == 0) {
            if (!"ELIGIBLE".equals(oe.sampleVerdict112)) return oe.sampleVerdict112;
            return oe.denseVerdict112;
        }
        String sv = (stage <= 2) ? oe.sampleVerdict96W5 : oe.sampleVerdict96W15;
        if (!"ELIGIBLE".equals(sv)) return sv;
        return (stage == 1) ? oe.denseVerdict96FixedY : oe.denseVerdict96ReclampY;
    }

    private static List<String> findVillageOverlaps(
            ServerLevel level, NoiseBasedChunkGenerator gen, RandomState rs,
            net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,
            net.minecraft.world.level.biome.Climate.Sampler sampler,
            List<Holder<StructureSet>> villageSets, Map<String, Optional<BoundingBox>> cache,
            BoundingBox hqBox) {
        Set<String> hits = new TreeSet<>();
        int minCx = (hqBox.minX() >> 4) - 5, maxCx = (hqBox.maxX() >> 4) + 5;
        int minCz = (hqBox.minZ() >> 4) - 5, maxCz = (hqBox.maxZ() >> 4) + 5;
        long seed = level.getSeed();
        for (Holder<StructureSet> h : villageSets) {
            StructureSet set = h.value();
            if (!(set.placement() instanceof RandomSpreadStructurePlacement p)) continue;
            String setId = h.unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
            int sp = p.spacing();
            int rx0 = Math.floorDiv(minCx, sp), rx1 = Math.floorDiv(maxCx, sp);
            int rz0 = Math.floorDiv(minCz, sp), rz1 = Math.floorDiv(maxCz, sp);
            for (int rx = rx0; rx <= rx1; rx++) {
                for (int rz = rz0; rz <= rz1; rz++) {
                    ChunkPos cp = p.getPotentialStructureChunk(seed, rx * sp, rz * sp);
                    if (cp.x() < minCx || cp.x() > maxCx || cp.z() < minCz || cp.z() > maxCz) continue;
                    if (!p.isStructureChunk(state, cp.x(), cp.z())) continue;
                    String cacheKey = setId + "@" + cp.x() + "," + cp.z();
                    Optional<BoundingBox> cached = cache.get(cacheKey);
                    BoundingBox stBox = null;
                    if (cached != null) {
                        stBox = cached.orElse(null);
                    } else {
                        List<StructureSet.StructureSelectionEntry> options = new ArrayList<>(set.structures());
                        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
                        random.setLargeFeatureSeed(seed, cp.x(), cp.z());
                        int total = options.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
                        while (!options.isEmpty()) {
                            int index = 0;
                            if (options.size() != 1) {
                                int choice = random.nextInt(total);
                                for (var o : options) {
                                    choice -= o.weight();
                                    if (choice < 0) break;
                                    index++;
                                }
                            }
                            var option = options.get(index);
                            Structure st = option.structure().value();
                            StructureStart generated = st.generate(option.structure(), level.dimension(), level.registryAccess(),
                                    gen, gen.getBiomeSource(), sampler, rs,
                                    level.getStructureTemplateManager(), seed, cp, 0, level, st.biomes()::contains);
                            if (generated.isValid()) {
                                stBox = generated.getBoundingBox();
                                break;
                            }
                            total -= option.weight();
                            options.remove(index);
                        }
                        cache.put(cacheKey, Optional.ofNullable(stBox));
                    }
                    if (stBox != null && stBox.intersects(hqBox)) {
                        hits.add(cacheKey);
                    }
                }
            }
        }
        return List.copyOf(hits);
    }

    private static final class Session {
        final MinecraftServer server;
        final ServerLevel level;
        final Path dir;
        final Map<String, Object> result;
        final long started;
        final List<Map<String, Object>> targets, verified = new ArrayList<>();
        final ServerPlayer player;
        int index, phase, ticks;
        Map<String, Object> current;
        St03HeadquartersData.HqInstance hq;
        List<ChunkPos> forced = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Session(MinecraftServer server, Path dir, Map<String, Object> result, long started) {
            this.server = server;
            this.level = server.overworld();
            this.dir = dir;
            this.result = result;
            this.started = started;
            List<Map<String, Object>> starts = (List<Map<String, Object>>) result.get("starts");
            List<Map<String, Object>> selected = new ArrayList<>();
            for (var row : prioritizeNewTargets(starts)) {
                if (selected.size() < 3) selected.add(row);
            }
            // On seed 10079, also explicitly inspect the historical village-overlapping savanna case (-1429, -123)
            if (level.getSeed() == 10079L) {
                for (var row : starts) {
                    int cx = ((Number) row.get("candidate_chunk_x")).intValue();
                    int cz = ((Number) row.get("candidate_chunk_z")).intValue();
                    if (cx == -1429 && cz == -123) {
                        Map<String, Object> copy = new LinkedHashMap<>(row);
                        copy.put("known_village_overlap_diagnostic", true);
                        selected.add(copy);
                        break;
                    }
                }
            }
            if (selected.isEmpty()) throw new AssertionError("Too few natural starts");
            this.targets = List.copyOf(selected);
            result.put("full_generation", verified);
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "st03-probe"), false);
            player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(0, 200, 0);
        }

        void tick() {
            if (++ticks > 400) throw new AssertionError("Natural completion/proximity timed out at target " + index + " phase " + phase);
            if (phase == 0) { prepare(); phase = 1; ticks = 0; return; }
            if (phase == 1) {
                if (hq == null || !hq.structureCompleted) return;
                int ready = 0;
                for (var cp : forced)
                    if (level.areEntitiesLoaded(cp.pack()) && level.isPositionEntityTicking(new BlockPos(cp.getMinBlockX() + 8, hq.origin.getY(), cp.getMinBlockZ() + 8)))
                        ready++;
                current.put("entity_ticking_chunks_before_proximity", ready);
                if (ready != forced.size()) return;
                if (hq.garrisonInitialized || defenders() != 0) throw new AssertionError("Garrison spawned before first proximity");
                current.put("completion_record_created", true);
                current.put("structure_completed", true);
                current.put("pre_proximity_defenders", 0);
                player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().invulnerable = true;
                player.onUpdateAbilities();
                player.teleportTo(hq.origin.getX() + 56.5, hq.origin.getY() + 1, hq.origin.getZ() + 56.5);
                phase = 2;
                ticks = 0;
                return;
            }
            if (!hq.garrisonInitialized) return;
            int defenders = defenders();
            current.put("garrison_initialized", true);
            current.put("defenders_first_proximity", defenders);
            current.putIfAbsent("first_entity_observation", defenders);
            current.put("entity_observation_ticks", ticks);
            current.put("hq_id", hq.id());
            current.put("committed_chunks", hq.committedChunks.size());
            if (defenders != 40) {
                if (ticks < 40) return;
                throw new AssertionError("Natural first proximity must spawn 40 defenders, got " + defenders + "; ready chunks=" + current.get("entity_ticking_chunks_before_proximity"));
            }
            Map<String, Integer> types = new TreeMap<>();
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(hq.box.minX(), hq.box.minY(), hq.box.minZ(), hq.box.maxX() + 1, hq.box.maxY() + 1, hq.box.maxZ() + 1).inflate(32), this::ownsDefender))
                types.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), 1, Integer::sum);
            current.put("defender_types", types);
            if (Boolean.TRUE.equals(current.get("known_village_overlap_diagnostic"))) {
                result.put("seed_10079_savanna_village_overlap_case", current);
            } else if (Boolean.TRUE.equals(current.get("post_structure_cave_air_intersection"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> caveCases = (List<Map<String, Object>>) result.computeIfAbsent("cave_air_feature_intersections", k -> new ArrayList<>());
                caveCases.add(current);
            } else if (verified.size() < 2) {
                verified.add(current);
            }
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(0, 200, 0);
            for (var cp : forced) level.setChunkForced(cp.x(), cp.z(), false);
            forced.clear();
            while (++index < targets.size() && verified.size() >= 2 && !Boolean.TRUE.equals(targets.get(index).get("known_village_overlap_diagnostic"))) {
                // Already verified 2 clean newly-unlocked targets; skip remaining non-diagnostic fallback candidates
            }
            if (index >= targets.size()) {
                if (verified.size() < 2) {
                    finish(new AssertionError("Fewer than 2 clean full_generation targets verified: " + verified.size()));
                    return;
                }
                finish(null);
                return;
            }
            phase = 0;
            ticks = 0;
        }

        boolean ownsDefender(Mob mob) {
            return mob.isAlive() && mob.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id())
                    && !mob.entityTags().contains(St03GarrisonManager.TAG_PRISONER);
        }

        int defenders() {
            return level.getEntitiesOfClass(Mob.class, new AABB(hq.box.minX(), hq.box.minY(), hq.box.minZ(), hq.box.maxX() + 1, hq.box.maxY() + 1, hq.box.maxZ() + 1).inflate(32), this::ownsDefender).size();
        }

        void prepare() {
            var target = targets.get(index);
            boolean isVillageDiag = Boolean.TRUE.equals(target.get("known_village_overlap_diagnostic"));
            int cx = ((Number) target.get("candidate_chunk_x")).intValue(), cz = ((Number) target.get("candidate_chunk_z")).intValue();
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(St03Worldgen.STRUCTURE_ID);
            var actual = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            if (actual == null || !actual.isValid()) throw new AssertionError("Actual chunk pipeline start absent");
            var modules = actual.getPieces().stream().filter(p -> p instanceof St03AssemblyPiece).map(p -> (St03AssemblyPiece) p).toList();
            if (modules.size() != 34 || actual.getPieces().size() != 35)
                throw new AssertionError("Terrain correction plus 34 unchanged modules required");
            BlockPos origin = modules.getFirst().compoundOrigin();
            int chunks = 0, maxRadius = 0;
            if (origin.getX() != ((Number) target.get("origin_x")).intValue() || origin.getY() != ((Number) target.get("foundation_y")).intValue()
                    || origin.getZ() != ((Number) target.get("origin_z")).intValue() || !modules.getFirst().compoundRotation().name().equals(target.get("rotation")))
                throw new AssertionError("Fresh chunk pipeline start differs from recorded placement scan");

            var rs = level.getChunkSource().randomState();
            var gen = level.getChunkSource().getGenerator();
            Map<Long, St03TerrainColumns.Columns> dense = new HashMap<>();
            java.util.function.BiFunction<Integer, Integer, St03TerrainColumns.Columns> column = (x, z) -> dense.computeIfAbsent(new ChunkPos(x >> 4, z >> 4).pack(),
                    k -> St03TerrainColumns.read(gen, level, rs, x & ~15, z & ~15));
            int sampleY = ((Number) target.getOrDefault("sample_foundation_y", origin.getY())).intValue();
            var actualLimits = St03FullTerrainCheck.evaluate((x, z) -> column.apply(x, z).surfaceAt(x, z), (x, z) -> column.apply(x, z).floorAt(x, z), origin.getX(), origin.getZ(), sampleY);
            if (!actualLimits.eligible() || actualLimits.columnsChecked() != St03PlacementConfig.EVAL_COLUMNS || actualLimits.foundationY() != origin.getY())
                throw new AssertionError("Full terrain gate failed on natural start: " + actualLimits);

            for (int x = origin.getX() >> 4; x <= (origin.getX() + 111) >> 4; x++)
                for (int z = origin.getZ() >> 4; z <= (origin.getZ() + 111) >> 4; z++) {
                    level.setChunkForced(x, z, true);
                    forced.add(new ChunkPos(x, z));
                    level.getChunk(x, z, ChunkStatus.FULL);
                    chunks++;
                    maxRadius = Math.max(maxRadius, Math.max(Math.abs(x - cx), Math.abs(z - cz)));
                }
            if (maxRadius > 8) throw new AssertionError("Structure exceeds vanilla reference radius");
            hq = St03HeadquartersData.get(level).instances().get(new ChunkPos(cx, cz).pack());
            if (hq == null || hq.origin == null || !hq.origin.equals(origin))
                throw new AssertionError("Completion did not record natural HQ origin");

            int unsupported = 0, caveAirUnsupported = 0, wet = 0, airBelow = 0, waterBelow = 0, planks = 0;
            int preGenWaterCols112 = 0, preGenWaterCols96 = 0;
            int outerRingCols = 0, outerRingCutExceed10 = 0, outerRingSkirtExceed12 = 0, outerRingFillExceed16 = 0, outerRingWaterCols = 0;
            int outerRingUnsupported = 0, outerRingWet = 0, outerRingFloatingBelow16 = 0;
            List<Map<String, Object>> badColumns = new ArrayList<>();

            for (int x = origin.getX(); x < origin.getX() + 112; x++) {
                for (int z = origin.getZ(); z < origin.getZ() + 112; z++) {
                    boolean inCore96 = (x >= origin.getX() + 8 && x < origin.getX() + 104 && z >= origin.getZ() + 8 && z < origin.getZ() + 104);
                    int baseSurf = column.apply(x, z).surfaceAt(x, z);
                    int baseFlr = column.apply(x, z).floorAt(x, z);
                    if (baseSurf > baseFlr + 1) {
                        preGenWaterCols112++;
                        if (inCore96) preGenWaterCols96++;
                        else outerRingWaterCols++;
                    }
                    if (!inCore96) {
                        outerRingCols++;
                        if (baseSurf - origin.getY() > 10) outerRingCutExceed10++;
                        if (origin.getY() - baseSurf > 12) outerRingSkirtExceed12++;
                        if (origin.getY() - baseFlr > 16) outerRingFillExceed16++;
                    }

                    var block = level.getBlockState(new BlockPos(x, origin.getY() - 1, z));
                    if (!block.isSolidRender()) {
                        unsupported++;
                        if (block.is(net.minecraft.world.level.block.Blocks.CAVE_AIR)) caveAirUnsupported++;
                        if (!inCore96) outerRingUnsupported++;
                    }
                    if (!block.getFluidState().isEmpty()) {
                        wet++;
                        if (!inCore96) outerRingWet++;
                    }
                    if ((!block.isSolidRender() || !block.getFluidState().isEmpty()) && badColumns.size() < 16)
                        badColumns.add(Map.of("x", x, "y", origin.getY() - 1, "z", z, "in_core_96", inCore96, "block", block.toString(),
                                "base_surface", baseSurf, "base_floor", baseFlr));
                    if (!inCore96) {
                        var below17 = level.getBlockState(new BlockPos(x, origin.getY() - 17, z));
                        if (!below17.isSolidRender()) outerRingFloatingBelow16++;
                    }
                    for (int dy = 1; dy <= 16; dy++) {
                        var below = level.getBlockState(new BlockPos(x, origin.getY() - dy, z));
                        if (below.isAir()) airBelow++;
                        if (!below.getFluidState().isEmpty()) waterBelow++;
                    }
                    for (int dy = 1; dy <= 26; dy++)
                        if (level.getBlockState(new BlockPos(x, origin.getY() + dy, z)).is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS))
                            planks++;
                }
            }

            current = new LinkedHashMap<>(target);
            current.put("actual_start_valid", true);
            current.put("assembly_pieces", modules.size());
            current.put("total_pieces", actual.getPieces().size());
            result.put("current_target", current);
            current.put("actual_max_cut", actualLimits.cut());
            current.put("actual_max_skirt", actualLimits.skirt());
            current.put("actual_max_footing_fill", actualLimits.footingFill());
            current.put("actual_columns_checked", actualLimits.columnsChecked());
            current.put("full_chunks_generated", chunks);
            current.put("max_chunk_radius", maxRadius);
            current.put("support_plane_columns", 112 * 112);
            current.put("unsupported_columns", unsupported);
            current.put("wet_columns", wet);
            current.put("wet_column_ratio_pct", 100.0 * wet / (112.0 * 112.0));
            current.put("pre_gen_water_columns_96", preGenWaterCols96);
            current.put("pre_gen_water_ratio_96_pct", 100.0 * preGenWaterCols96 / 9216.0);
            current.put("pre_gen_water_columns_112", preGenWaterCols112);
            current.put("pre_gen_water_ratio_112_pct", 100.0 * preGenWaterCols112 / 12544.0);
            current.put("outer_ring_diagnostics", Map.of(
                    "outer_ring_columns", outerRingCols,
                    "pre_gen_cut_exceed_10_cols", outerRingCutExceed10,
                    "pre_gen_skirt_exceed_12_cols", outerRingSkirtExceed12,
                    "pre_gen_fill_exceed_16_cols", outerRingFillExceed16,
                    "pre_gen_water_cols", outerRingWaterCols,
                    "post_gen_unsupported_at_y_minus_1", outerRingUnsupported,
                    "post_gen_wet_at_y_minus_1", outerRingWet,
                    "post_gen_non_solid_at_y_minus_17", outerRingFloatingBelow16
            ));
            current.put("current_air_cells_below_1_16", airBelow);
            current.put("current_water_cells_below_1_16", waterBelow);
            current.put("bad_support_columns", badColumns);
            current.put("placed_spruce_planks_above_foundation", planks);
            current.put("overlaps", overlaps(actual, hq.box));

            if (!isVillageDiag && unsupported > 0 && unsupported == caveAirUnsupported) {
                current.put("post_structure_cave_air_intersection", true);
            }
            boolean isCaveAirDiag = Boolean.TRUE.equals(current.get("post_structure_cave_air_intersection"));
            // Allow the explicit seed 10079 village-overlap diagnostic case and post-structure cave_air feature intersections to be recorded without aborting
            if (!isVillageDiag && !isCaveAirDiag && (unsupported != 0 || 100L * wet > 15L * 12544L || 100L * preGenWaterCols96 > 20L * 9216L || planks == 0))
                throw new AssertionError("Natural template placement/support validation failed: " + current);
        }

        List<String> overlaps(StructureStart own, BoundingBox box) {
            Set<String> hits = new TreeSet<>();
            Set<String> visited = new HashSet<>();
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++)
                for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
                    for (var refs : level.getChunk(x, z).getAllReferences().entrySet()) {
                        if (refs.getKey() instanceof St03HeadquartersStructure) continue;
                        for (long packed : refs.getValue()) {
                            String key = registry.getKey(refs.getKey()) + "@" + packed;
                            if (!visited.add(key)) continue;
                            var pos = ChunkPos.unpack(packed);
                            var other = level.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(refs.getKey());
                            if (other != null && other.isValid() && other.getBoundingBox().intersects(box))
                                hits.add(registry.getKey(refs.getKey()) + "@" + pos.x() + "," + pos.z());
                        }
                    }
            return List.copyOf(hits);
        }

        void finish(Throwable error) {
            if (error == null) {
                try {
                    server.getPlayerList().remove(player);
                    server.saveEverything(true, true, true);
                    result.put("world_saved", true);
                    result.remove("current_target");
                    writeCapturePlan(dir, result);
                    result.put("full_generation_status", "PASS");
                } catch (Throwable t) {
                    error = t;
                }
            }
            writeAndExit(server, dir, result, started, error);
        }
    }
}
