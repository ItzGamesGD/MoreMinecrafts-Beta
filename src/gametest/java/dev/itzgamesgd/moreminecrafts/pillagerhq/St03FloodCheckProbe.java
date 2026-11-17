package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.*;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated-server ST-03 Pillager Headquarters flood and terrain surface audit probe.
 * Measurement only, no worldgen or production changes.
 * Activated via {@code -Dmm.st03.flood.out=<path>}.
 */
public final class St03FloodCheckProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st03-flood-check");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final int RADIUS = 25;
    private static final Set<String> ALL_19 = Set.copyOf(St03BiomeRules.ELIGIBLE_BIOME_IDS);
    private static final Set<String> EDGE_5 = Set.of(
            "minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore",
            "minecraft:river", "minecraft:frozen_river");

    @Override
    public void onInitialize() {
        String outDir = System.getProperty("mm.st03.flood.out");
        if (outDir == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> summary = new LinkedHashMap<>();
            Path dir = Path.of(outDir);
            try {
                Files.createDirectories(dir);
                runFloodAudit(server.overworld(), dir, summary);
                summary.put("status", "PASS");
            } catch (Throwable t) {
                LOG.error("ST-03 flood check audit failed", t);
                summary.put("status", "FAIL");
                summary.put("error", t.toString());
                summary.put("stack", Arrays.toString(t.getStackTrace()));
            }
            try {
                Files.writeString(dir.resolve("flood-summary.json"), JSON.toJson(summary));
                // Provide result.json for standard runner/publish pipeline
                Files.writeString(dir.resolve("result.json"), JSON.toJson(summary));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Runtime.getRuntime().halt("PASS".equals(summary.get("status")) ? 0 : 1);
        });
    }

    private static final class FloodTerrain {
        final ServerLevel level;
        final NoiseBasedChunkGenerator gen;
        final RandomState rs;
        final BiomeResolver resolver;
        final Map<Long, Integer> surfaces = new HashMap<>();
        final Map<Long, Integer> floors = new HashMap<>();
        final Map<Long, String> biomeAtQuart = new HashMap<>();
        final Map<Long, St03TerrainColumns.Columns> denseChunks = new HashMap<>();

        FloodTerrain(ServerLevel level) {
            this.level = level;
            if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator n)) {
                throw new IllegalStateException("Expected NoiseBasedChunkGenerator");
            }
            this.gen = n;
            this.rs = level.getChunkSource().randomState();
            var sampler = rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
            this.resolver = gen.getBiomeSource().createResolver(sampler);
        }

        static long key2(int x, int z) {
            return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        }

        static long key3(int x, int y, int z) {
            return (((long) (x & 0x1FFFFF)) << 42) | (((long) (y & 0x1FFFFF)) << 21) | (z & 0x1FFFFFL);
        }

        void clearRegionCache() {
            surfaces.clear();
            floors.clear();
            biomeAtQuart.clear();
            denseChunks.clear();
        }

        void preloadDenseBox(int minX, int minZ, int size) {
            int maxX = minX + size - 1, maxZ = minZ + size - 1;
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                    dense(cx << 4, cz << 4);
                }
            }
        }

        int surface(int x, int z) {
            St03TerrainColumns.Columns dc = denseChunks.get(key2(x >> 4, z >> 4));
            if (dc != null) return dc.surfaceAt(x, z);
            return surfaces.computeIfAbsent(key2(x, z), k ->
                    gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs));
        }

        int floor(int x, int z) {
            St03TerrainColumns.Columns dc = denseChunks.get(key2(x >> 4, z >> 4));
            if (dc != null) return dc.floorAt(x, z);
            return floors.computeIfAbsent(key2(x, z), k ->
                    gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs));
        }

        St03TerrainColumns.Columns dense(int x, int z) {
            return denseChunks.computeIfAbsent(key2(x >> 4, z >> 4), k ->
                    St03TerrainColumns.read(gen, level, rs, x & ~15, z & ~15));
        }

        String quartBiome(int qx, int qy, int qz) {
            return biomeAtQuart.computeIfAbsent(key3(qx, qy, qz), k -> {
                Holder<Biome> h = resolver.getNoiseBiome(qx, qy, qz);
                return h.unwrapKey().map(rk -> rk.identifier().toString()).orElse("unknown");
            });
        }

        int columnKind19(int x, int z) {
            int qx = Math.floorDiv(x, 4), qz = Math.floorDiv(z, 4);
            int qy = Math.floorDiv(surface(x, z) - 3, 4);
            String b0 = quartBiome(qx, qy, qz);
            String b1 = quartBiome(qx, qy + 1, qz);
            if (ALL_19.contains(b0) && ALL_19.contains(b1)) return 0;
            if (EDGE_5.contains(b0) && EDGE_5.contains(b1)) return 1;
            return 2;
        }

        St03Eligibility.Terrain asEligibilityTerrain() {
            return new St03Eligibility.Terrain() {
                @Override public int surfaceHeight(int x, int z) { return surface(x, z); }
                @Override public int oceanFloorHeight(int x, int z) { return floor(x, z); }
                @Override public boolean isEligibleBiomeCell(int qx, int qy, int qz) {
                    return ALL_19.contains(quartBiome(qx, qy, qz));
                }
                @Override public boolean isEdgeBiomeCell(int qx, int qy, int qz) {
                    return EDGE_5.contains(quartBiome(qx, qy, qz));
                }
            };
        }
    }

    public record CandidateFloodRow(
            int candidateChunkX, int candidateChunkZ, int regionX, int regionZ,
            boolean centerEligible, boolean passedFinalPlacement,
            int originX, int foundationY, int originZ, int sampleFoundationY,
            int dxChunks, int dzChunks, String rotation,
            int waterCols96, int waterYMin96, int waterYMax96, int waterYEqual63_96, int waterYNot63_96,
            int waterCols112, int waterYMin112, int waterYMax112, int waterYEqual63_112, int waterYNot63_112,
            int ringWaterCols96, int ringWaterYMin96, int ringWaterYMax96, int ringWaterYEqual63_96, int ringWaterYNot63_96,
            int ringWaterCols112, int ringWaterYMin112, int ringWaterYMax112, int ringWaterYEqual63_112, int ringWaterYNot63_112,
            int dryGroundBelow63_112, int dryGroundBelowFy_112, int dryGroundFloodRisk8b_112,
            int boundaryWaterTouchCutN, int boundaryWaterTouchCutS, int boundaryWaterTouchCutW, int boundaryWaterTouchCutE,
            int boundaryWaterTouchCutTotal, boolean gatehouseWaterContact
    ) {}

    private static void runFloodAudit(ServerLevel level, Path outDir, Map<String, Object> summary) throws Exception {
        long started = System.nanoTime();
        long seed = level.getSeed();
        FloodTerrain t = new FloodTerrain(level);
        RandomSpreadStructurePlacement placement = new RandomSpreadStructurePlacement(
                St03PlacementConfig.SPACING_CHUNKS, St03PlacementConfig.SEPARATION_CHUNKS,
                RandomSpreadType.TRIANGULAR, St03PlacementConfig.SALT);

        summary.put("seed", seed);
        summary.put("head_sha", System.getenv("GITHUB_SHA"));
        summary.put("run_id", System.getenv("GITHUB_RUN_ID"));
        summary.put("spacing", St03PlacementConfig.SPACING_CHUNKS);
        summary.put("separation", St03PlacementConfig.SEPARATION_CHUNKS);
        summary.put("potential_candidates", (2 * RADIUS + 1) * (2 * RADIUS + 1));
        summary.put("eval_box_size", St03PlacementConfig.EVAL_BOX_SIZE);
        summary.put("max_water_percent", St03PlacementConfig.MAX_WATER_PERCENT);

        List<CandidateFloodRow> allRows = new ArrayList<>(2601);
        List<CandidateFloodRow> passingRows = new ArrayList<>();
        List<CandidateFloodRow> nonPassingRows = new ArrayList<>();

        Path csvPath = outDir.resolve("flood-candidates.csv");
        try (BufferedWriter bw = Files.newBufferedWriter(csvPath)) {
            bw.write("candidate_chunk_x,candidate_chunk_z,region_x,region_z,"
                    + "center_eligible,passed_final_placement,"
                    + "origin_x,foundation_y,origin_z,sample_foundation_y,dx_chunks,dz_chunks,rotation,"
                    + "water_cols_96,water_y_min_96,water_y_max_96,water_y_eq63_96,water_y_not63_96,"
                    + "water_cols_112,water_y_min_112,water_y_max_112,water_y_eq63_112,water_y_not63_112,"
                    + "ring_water_cols_96,ring_water_y_min_96,ring_water_y_max_96,ring_water_y_eq63_96,ring_water_y_not63_96,"
                    + "ring_water_cols_112,ring_water_y_min_112,ring_water_y_max_112,ring_water_y_eq63_112,ring_water_y_not63_112,"
                    + "dry_ground_below_63_112,dry_ground_below_fy_112,dry_ground_flood_risk_8b_112,"
                    + "boundary_water_cut_n,boundary_water_cut_s,boundary_water_cut_w,boundary_water_cut_e,boundary_water_cut_total,"
                    + "gatehouse_water_contact\n");

            for (int rx = -RADIUS; rx <= RADIUS; rx++) {
                for (int rz = -RADIUS; rz <= RADIUS; rz++) {
                    ChunkPos cp = placement.getPotentialStructureChunk(seed, rx, rz);
                    int cx = cp.x(), cz = cp.z();
                    int candidateOriginX = cx * 16 - 48, candidateOriginZ = cz * 16 - 48;
                    boolean centerEligible = (t.columnKind19(candidateOriginX + 56, candidateOriginZ + 56) == 0);

                    if (centerEligible) {
                        t.preloadDenseBox(candidateOriginX - 32, candidateOriginZ - 32, 112 + 64);
                    }

                    var elTerrain = t.asEligibilityTerrain();
                    var siteOpt = St03SiteSearch.selectWithDenseY(elTerrain, candidateOriginX, candidateOriginZ, site -> {
                        int sampleY = site.result().foundationY();
                        var check = St03FullTerrainCheck.evaluate(
                                (x, z) -> t.surface(x, z),
                                (x, z) -> t.floor(x, z),
                                site.originX(), site.originZ(), sampleY);
                        return check.eligible() ? check.foundationY() : Integer.MIN_VALUE;
                    });

                    boolean passed = siteOpt.isPresent();
                    int evalX, evalZ, fy, sampleFy, dx = 0, dz = 0;
                    Rotation rotation = Rotation.NONE;
                    if (passed) {
                        var site = siteOpt.get();
                        evalX = site.originX();
                        evalZ = site.originZ();
                        fy = site.result().foundationY();
                        sampleFy = site.result().heightDifference(); // placeholder or sample Y
                        dx = site.dxChunks();
                        dz = site.dzChunks();
                        rotation = St03AssemblyPlacement.selectRotation(level.registryAccess(), seed, cp);
                    } else {
                        evalX = candidateOriginX;
                        evalZ = candidateOriginZ;
                        int sMid = t.surface(evalX + 56, evalZ + 56);
                        fy = sMid;
                        sampleFy = sMid;
                    }

                    // Preload the box area including 8-block margins for neighborhood searches
                    t.preloadDenseBox(evalX - 8, evalZ - 8, 112 + 16);

                    // --- Item A: Water height stats inside 96x96, 112x112, and 1-block rings ---
                    int wCols96 = 0, wMin96 = 999, wMax96 = -999, wEq63_96 = 0, wNot63_96 = 0;
                    for (int x = evalX + 8; x < evalX + 104; x++) {
                        for (int z = evalZ + 8; z < evalZ + 104; z++) {
                            int s = t.surface(x, z), f = t.floor(x, z);
                            if (s > f + 1) {
                                wCols96++;
                                int wy = s - 1;
                                wMin96 = Math.min(wMin96, wy);
                                wMax96 = Math.max(wMax96, wy);
                                if (wy == 63) wEq63_96++;
                                else wNot63_96++;
                            }
                        }
                    }
                    if (wCols96 == 0) { wMin96 = 0; wMax96 = 0; }

                    int wCols112 = 0, wMin112 = 999, wMax112 = -999, wEq63_112 = 0, wNot63_112 = 0;
                    for (int x = evalX; x < evalX + 112; x++) {
                        for (int z = evalZ; z < evalZ + 112; z++) {
                            int s = t.surface(x, z), f = t.floor(x, z);
                            if (s > f + 1) {
                                wCols112++;
                                int wy = s - 1;
                                wMin112 = Math.min(wMin112, wy);
                                wMax112 = Math.max(wMax112, wy);
                                if (wy == 63) wEq63_112++;
                                else wNot63_112++;
                            }
                        }
                    }
                    if (wCols112 == 0) { wMin112 = 0; wMax112 = 0; }

                    // 1-block ring immediately outside 96x96
                    int ringW96 = 0, ringWMin96 = 999, ringWMax96 = -999, ringWEq63_96 = 0, ringWNot63_96 = 0;
                    for (int i = 7; i <= 104; i++) {
                        int[][] ringCoords = {{evalX + i, evalZ + 7}, {evalX + i, evalZ + 104},
                                {evalX + 7, evalZ + i}, {evalX + 104, evalZ + i}};
                        for (int[] rc : ringCoords) {
                            int s = t.surface(rc[0], rc[1]), f = t.floor(rc[0], rc[1]);
                            if (s > f + 1) {
                                ringW96++;
                                int wy = s - 1;
                                ringWMin96 = Math.min(ringWMin96, wy);
                                ringWMax96 = Math.max(ringWMax96, wy);
                                if (wy == 63) ringWEq63_96++;
                                else ringWNot63_96++;
                            }
                        }
                    }
                    if (ringW96 == 0) { ringWMin96 = 0; ringWMax96 = 0; }

                    // 1-block ring immediately outside 112x112
                    int ringW112 = 0, ringWMin112 = 999, ringWMax112 = -999, ringWEq63_112 = 0, ringWNot63_112 = 0;
                    for (int i = -1; i <= 112; i++) {
                        int[][] ringCoords = {{evalX + i, evalZ - 1}, {evalX + i, evalZ + 112},
                                {evalX - 1, evalZ + i}, {evalX + 112, evalZ + i}};
                        for (int[] rc : ringCoords) {
                            int s = t.surface(rc[0], rc[1]), f = t.floor(rc[0], rc[1]);
                            if (s > f + 1) {
                                ringW112++;
                                int wy = s - 1;
                                ringWMin112 = Math.min(ringWMin112, wy);
                                ringWMax112 = Math.max(ringWMax112, wy);
                                if (wy == 63) ringWEq63_112++;
                                else ringWNot63_112++;
                            }
                        }
                    }
                    if (ringW112 == 0) { ringWMin112 = 0; ringWMax112 = 0; }

                    // --- Item B: Dry ground below 63 / below foundation and flood risk ---
                    int dryBelow63 = 0, dryBelowFy = 0, dryFloodRisk = 0;
                    for (int x = evalX; x < evalX + 112; x++) {
                        for (int z = evalZ; z < evalZ + 112; z++) {
                            int s = t.surface(x, z), f = t.floor(x, z);
                            boolean isWater = (s > f + 1);
                            if (!isWater) {
                                int gy = s - 1;
                                if (s < 63 || gy < 63) dryBelow63++;
                                if (gy < fy) dryBelowFy++;

                                if (s < 63 || gy < 63) {
                                    boolean hasWaterNear = false;
                                    for (int ddx = -8; ddx <= 8 && !hasWaterNear; ddx++) {
                                        for (int ddz = -8; ddz <= 8 && !hasWaterNear; ddz++) {
                                            int ns = t.surface(x + ddx, z + ddz);
                                            int nf = t.floor(x + ddx, z + ddz);
                                            if (ns > nf + 1 && (ns - 1 == 63)) {
                                                hasWaterNear = true;
                                            }
                                        }
                                    }
                                    if (hasWaterNear) dryFloodRisk++;
                                }
                            }
                        }
                    }

                    // --- Item D: Boundary cut contact with outside water ---
                    int cutN = 0, cutS = 0, cutW = 0, cutE = 0;
                    for (int i = 0; i < 112; i++) {
                        // North edge: z = evalZ - 1, x = evalX + i
                        int sN = t.surface(evalX + i, evalZ - 1), fN = t.floor(evalX + i, evalZ - 1);
                        if (sN > fN + 1 && (sN - 1 >= fy + 1)) cutN++;

                        // South edge: z = evalZ + 112, x = evalX + i
                        int sS = t.surface(evalX + i, evalZ + 112), fS = t.floor(evalX + i, evalZ + 112);
                        if (sS > fS + 1 && (sS - 1 >= fy + 1)) cutS++;

                        // West edge: x = evalX - 1, z = evalZ + i
                        int sW = t.surface(evalX - 1, evalZ + i), fW = t.floor(evalX - 1, evalZ + i);
                        if (sW > fW + 1 && (sW - 1 >= fy + 1)) cutW++;

                        // East edge: x = evalX + 112, z = evalZ + i
                        int sE = t.surface(evalX + 112, evalZ + i), fE = t.floor(evalX + 112, evalZ + i);
                        if (sE > fE + 1 && (sE - 1 >= fy + 1)) cutE++;
                    }
                    int cutTotal = cutN + cutS + cutW + cutE;

                    // Check if outside water touches gatehouse entrance
                    boolean gateWater = false;
                    for (int g = 53; g <= 58 && !gateWater; g++) {
                        int gx, gz;
                        switch (rotation) {
                            case NONE -> { gx = evalX + g; gz = evalZ + 112; }
                            case CLOCKWISE_90 -> { gx = evalX - 1; gz = evalZ + g; }
                            case CLOCKWISE_180 -> { gx = evalX + (111 - g); gz = evalZ - 1; }
                            case COUNTERCLOCKWISE_90 -> { gx = evalX + 112; gz = evalZ + (111 - g); }
                            default -> { gx = evalX + g; gz = evalZ + 112; }
                        }
                        int gs = t.surface(gx, gz), gf = t.floor(gx, gz);
                        if (gs > gf + 1 && (gs - 1 >= fy + 1)) {
                            gateWater = true;
                        }
                    }

                    CandidateFloodRow row = new CandidateFloodRow(
                            cx, cz, rx, rz, centerEligible, passed,
                            evalX, fy, evalZ, sampleFy, dx, dz, rotation.name(),
                            wCols96, wMin96, wMax96, wEq63_96, wNot63_96,
                            wCols112, wMin112, wMax112, wEq63_112, wNot63_112,
                            ringW96, ringWMin96, ringWMax96, ringWEq63_96, ringWNot63_96,
                            ringW112, ringWMin112, ringWMax112, ringWEq63_112, ringWNot63_112,
                            dryBelow63, dryBelowFy, dryFloodRisk,
                            cutN, cutS, cutW, cutE, cutTotal, gateWater
                    );

                    allRows.add(row);
                    if (passed) passingRows.add(row);
                    else nonPassingRows.add(row);

                    bw.write(String.format(Locale.ROOT,
                            "%d,%d,%d,%d,%b,%b,%d,%d,%d,%d,%d,%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%b\n",
                            cx, cz, rx, rz, centerEligible, passed,
                            evalX, fy, evalZ, sampleFy, dx, dz, rotation.name(),
                            wCols96, wMin96, wMax96, wEq63_96, wNot63_96,
                            wCols112, wMin112, wMax112, wEq63_112, wNot63_112,
                            ringW96, ringWMin96, ringWMax96, ringWEq63_96, ringWNot63_96,
                            ringW112, ringWMin112, ringWMax112, ringWEq63_112, ringWNot63_112,
                            dryBelow63, dryBelowFy, dryFloodRisk,
                            cutN, cutS, cutW, cutE, cutTotal, gateWater));
                }
            }
        }

        // --- Item C: Live post-generation chunk verification on representative starts ---
        List<Map<String, Object>> liveGenerationVerifications = new ArrayList<>();
        List<CandidateFloodRow> testTargets = new ArrayList<>();
        // Priority 1: passing start with lowest foundationY (foundationY < 63) and water
        passingRows.stream().filter(r -> r.foundationY() < 63 && r.waterCols112() > 0)
                .min(Comparator.comparingInt(CandidateFloodRow::foundationY))
                .ifPresent(testTargets::add);
        // Priority 2: passing start with boundary water cut contact
        passingRows.stream().filter(r -> r.boundaryWaterTouchCutTotal() > 0 && !testTargets.contains(r))
                .findFirst()
                .ifPresent(testTargets::add);
        // Priority 3: any other passing start
        if (testTargets.size() < 2 && !passingRows.isEmpty()) {
            for (var r : passingRows) {
                if (!testTargets.contains(r)) {
                    testTargets.add(r);
                    if (testTargets.size() >= 2) break;
                }
            }
        }

        int totalInteriorWaterBlocksObserved = 0;
        for (var target : testTargets) {
            Map<String, Object> targetReport = new LinkedHashMap<>();
            targetReport.put("origin_x", target.originX());
            targetReport.put("foundation_y", target.foundationY());
            targetReport.put("origin_z", target.originZ());
            targetReport.put("rotation", target.rotation());
            targetReport.put("pre_water_cols_112", target.waterCols112());
            targetReport.put("boundary_cut_contact_cols", target.boundaryWaterTouchCutTotal());

            BlockPos origin = new BlockPos(target.originX(), target.foundationY(), target.originZ());
            for (int x = origin.getX() >> 4; x <= (origin.getX() + 111) >> 4; x++) {
                for (int z = origin.getZ() >> 4; z <= (origin.getZ() + 111) >> 4; z++) {
                    level.getChunk(x, z, ChunkStatus.FULL);
                }
            }

            int interiorAirCount = 0;
            int interiorWaterCount = 0;
            List<Map<String, Object>> waterLocations = new ArrayList<>();
            for (int x = origin.getX() + 14; x <= origin.getX() + 98; x++) {
                for (int z = origin.getZ() + 14; z <= origin.getZ() + 98; z++) {
                    for (int dy = 0; dy <= 8; dy++) {
                        BlockPos p = origin.offset(x - origin.getX(), dy, z - origin.getZ());
                        var bs = level.getBlockState(p);
                        if (bs.isAir()) interiorAirCount++;
                        if (bs.is(Blocks.WATER) || !bs.getFluidState().isEmpty()) {
                            interiorWaterCount++;
                            totalInteriorWaterBlocksObserved++;
                            if (waterLocations.size() < 10) {
                                waterLocations.add(Map.of("x", p.getX(), "y", p.getY(), "z", p.getZ(),
                                        "block", bs.toString()));
                            }
                        }
                    }
                }
            }
            targetReport.put("interior_air_monitored", interiorAirCount);
            targetReport.put("interior_water_blocks", interiorWaterCount);
            targetReport.put("water_locations", waterLocations);
            targetReport.put("passed_zero_water", interiorWaterCount == 0);
            liveGenerationVerifications.add(targetReport);
        }

        // Aggregate statistics for summary JSON
        summary.put("total_candidates", allRows.size());
        summary.put("valid_starts", passingRows.size());
        summary.put("non_passing_candidates", nonPassingRows.size());

        // Hypothesis 1: Water height stats
        long passWaterCols96 = passingRows.stream().mapToLong(CandidateFloodRow::waterCols96).sum();
        long passWaterCols112 = passingRows.stream().mapToLong(CandidateFloodRow::waterCols112).sum();
        long passWaterNot63_96 = passingRows.stream().mapToLong(CandidateFloodRow::waterYNot63_96).sum();
        long passWaterNot63_112 = passingRows.stream().mapToLong(CandidateFloodRow::waterYNot63_112).sum();
        long allWaterCols96 = allRows.stream().mapToLong(CandidateFloodRow::waterCols96).sum();
        long allWaterCols112 = allRows.stream().mapToLong(CandidateFloodRow::waterCols112).sum();
        long allWaterNot63_96 = allRows.stream().mapToLong(CandidateFloodRow::waterYNot63_96).sum();
        long allWaterNot63_112 = allRows.stream().mapToLong(CandidateFloodRow::waterYNot63_112).sum();

        summary.put("hypothesis_1_water_height", Map.of(
                "passing_starts", Map.of(
                        "total_starts", passingRows.size(),
                        "starts_with_water_96", passingRows.stream().filter(r -> r.waterCols96() > 0).count(),
                        "starts_with_water_112", passingRows.stream().filter(r -> r.waterCols112() > 0).count(),
                        "total_water_cols_96", passWaterCols96,
                        "total_water_cols_112", passWaterCols112,
                        "water_cols_y_not_63_96", passWaterNot63_96,
                        "water_cols_y_not_63_112", passWaterNot63_112,
                        "water_y_min_across_starts_96", passingRows.stream().filter(r -> r.waterCols96() > 0).mapToInt(CandidateFloodRow::waterYMin96).min().orElse(63),
                        "water_y_max_across_starts_96", passingRows.stream().filter(r -> r.waterCols96() > 0).mapToInt(CandidateFloodRow::waterYMax96).max().orElse(63),
                        "water_y_min_across_starts_112", passingRows.stream().filter(r -> r.waterCols112() > 0).mapToInt(CandidateFloodRow::waterYMin112).min().orElse(63),
                        "water_y_max_across_starts_112", passingRows.stream().filter(r -> r.waterCols112() > 0).mapToInt(CandidateFloodRow::waterYMax112).max().orElse(63)
                ),
                "all_candidates", Map.of(
                        "total_candidates", allRows.size(),
                        "candidates_with_water_96", allRows.stream().filter(r -> r.waterCols96() > 0).count(),
                        "candidates_with_water_112", allRows.stream().filter(r -> r.waterCols112() > 0).count(),
                        "total_water_cols_96", allWaterCols96,
                        "total_water_cols_112", allWaterCols112,
                        "water_cols_y_not_63_96", allWaterNot63_96,
                        "water_cols_y_not_63_112", allWaterNot63_112
                ),
                "verdict", allWaterNot63_112 == 0 ? "CONFIRMED_ALL_WATER_AT_Y63" : "WATER_FOUND_AT_NON_63"
        ));

        // Hypothesis 2: Interior water ingress after generation
        summary.put("hypothesis_2_interior_ingress", Map.of(
                "targets_tested", liveGenerationVerifications.size(),
                "total_interior_water_blocks", totalInteriorWaterBlocksObserved,
                "verdict", totalInteriorWaterBlocksObserved == 0 ? "CONFIRMED_NO_FLOODING" : "FLOODING_DETECTED",
                "details", liveGenerationVerifications
        ));

        // Hypothesis 3: Dry ground below 63 adjacent to water
        long passStartsDryBelow63 = passingRows.stream().filter(r -> r.dryGroundBelow63_112() > 0).count();
        long passStartsDryFloodRisk = passingRows.stream().filter(r -> r.dryGroundFloodRisk8b_112() > 0).count();
        long passTotalDryFloodRiskCols = passingRows.stream().mapToLong(CandidateFloodRow::dryGroundFloodRisk8b_112).sum();
        long allCandidatesDryFloodRisk = allRows.stream().filter(r -> r.dryGroundFloodRisk8b_112() > 0).count();

        summary.put("hypothesis_3_dry_ground_adjacent_water", Map.of(
                "passing_starts_with_dry_ground_below_63", passStartsDryBelow63,
                "passing_starts_with_water_within_8b", passStartsDryFloodRisk,
                "passing_total_dry_flood_risk_cols", passTotalDryFloodRiskCols,
                "all_candidates_with_water_within_8b", allCandidatesDryFloodRisk,
                "verdict", passStartsDryFloodRisk > 0 ? "CONFIRMED_DRY_GROUND_ADJACENT_WATER_EXISTS" : "NO_ADJACENT_WATER_FOUND"
        ));

        // Hypothesis 4: Boundary cut contact with outside water
        long passStartsBoundaryWaterCut = passingRows.stream().filter(r -> r.boundaryWaterTouchCutTotal() > 0).count();
        long passTotalBoundaryCutCols = passingRows.stream().mapToLong(CandidateFloodRow::boundaryWaterTouchCutTotal).sum();
        long passGatehouseWaterContact = passingRows.stream().filter(CandidateFloodRow::gatehouseWaterContact).count();

        summary.put("hypothesis_4_boundary_cut_contact", Map.of(
                "code_cut_footprint", "112x112",
                "code_water_handling", "cleared_to_air_from_fy_plus_1_to_top",
                "passing_starts_with_boundary_water_touch_cut", passStartsBoundaryWaterCut,
                "passing_total_boundary_water_cut_cols", passTotalBoundaryCutCols,
                "passing_starts_with_gatehouse_water_contact", passGatehouseWaterContact,
                "cut_contact_by_edge", Map.of(
                        "north", passingRows.stream().mapToLong(CandidateFloodRow::boundaryWaterTouchCutN).sum(),
                        "south", passingRows.stream().mapToLong(CandidateFloodRow::boundaryWaterTouchCutS).sum(),
                        "west", passingRows.stream().mapToLong(CandidateFloodRow::boundaryWaterTouchCutW).sum(),
                        "east", passingRows.stream().mapToLong(CandidateFloodRow::boundaryWaterTouchCutE).sum()
                ),
                "verdict", passStartsBoundaryWaterCut > 0 ? "CONFIRMED_BOUNDARY_WATER_CUT_EXPOSURE_EXISTS" : "NO_BOUNDARY_WATER_EXPOSURE"
        ));

        summary.put("elapsed_seconds", (System.nanoTime() - started) / 1e9);
        summary.put("csv_file", "flood-candidates.csv");
    }
}
