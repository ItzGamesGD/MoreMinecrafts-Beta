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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-03 Pillager Headquarters 2nd worldgen terrain-condition audit probe (analysis only, no rule changes).
 * Inert unless {@code -Dmm.st03.audit2.out} is set.
 */
public final class St03TerrainAuditProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st03-terrain-audit-v2");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final int RADIUS = 25;
    private static final int[] OFFSETS_112 = St03Eligibility.sampleOffsets();

    private static final Set<String> ORIG_8 = Set.copyOf(St03BiomeRules.ORIGINAL_BIOME_IDS);
    private static final Set<String> ALL_19 = Set.copyOf(St03BiomeRules.ELIGIBLE_BIOME_IDS);
    private static final Set<String> EDGE_5 = Set.of(
            "minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore",
            "minecraft:river", "minecraft:frozen_river");

    @Override
    public void onInitialize() {
        String outDir = System.getProperty("mm.st03.audit2.out");
        if (outDir == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> summary = new LinkedHashMap<>();
            Path dir = Path.of(outDir);
            try {
                Files.createDirectories(dir);
                runAudit(server.overworld(), dir, summary);
                summary.put("status", "PASS");
            } catch (Throwable t) {
                LOG.error("ST-03 terrain audit v2 failed", t);
                summary.put("status", "FAIL");
                summary.put("error", t.toString());
                summary.put("stack", Arrays.toString(t.getStackTrace()));
            }
            try {
                Files.writeString(dir.resolve("audit-summary.json"), JSON.toJson(summary));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Runtime.getRuntime().halt("PASS".equals(summary.get("status")) ? 0 : 1);
        });
    }

    private static final class AuditTerrain {
        final ServerLevel level;
        final NoiseBasedChunkGenerator gen;
        final RandomState rs;
        final BiomeResolver resolver;
        final Map<Long, Integer> surfaces = new HashMap<>();
        final Map<Long, Integer> floors = new HashMap<>();
        final Map<Long, String> biomeAtQuart = new HashMap<>();
        final Map<Long, int[]> colBiomes = new HashMap<>();
        final Map<Long, St03TerrainColumns.Columns> denseChunks = new HashMap<>();
        final Map<String, Optional<BoundingBox>> otherStructureBoxes = new HashMap<>();
        final Map<String, Integer> biomeDict = new HashMap<>();
        final List<String> idByIdx = new ArrayList<>();
        long surfaceQueries, denseChunkReads;

        AuditTerrain(ServerLevel level) {
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
            colBiomes.clear();
            denseChunks.clear();
        }

        void preloadDenseBox(int originX, int originZ, int size) {
            int minCx = originX >> 4, maxCx = (originX + size - 1) >> 4;
            int minCz = originZ >> 4, maxCz = (originZ + size - 1) >> 4;
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    dense(cx << 4, cz << 4);
                }
            }
        }

        int surface(int x, int z) {
            St03TerrainColumns.Columns dc = denseChunks.get(key2(x >> 4, z >> 4));
            if (dc != null) return dc.surfaceAt(x, z);
            return surfaces.computeIfAbsent(key2(x, z), k -> {
                surfaceQueries++;
                return gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
            });
        }

        int floor(int x, int z) {
            St03TerrainColumns.Columns dc = denseChunks.get(key2(x >> 4, z >> 4));
            if (dc != null) return dc.floorAt(x, z);
            return floors.computeIfAbsent(key2(x, z), k ->
                    gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs));
        }

        St03TerrainColumns.Columns dense(int x, int z) {
            return denseChunks.computeIfAbsent(key2(x >> 4, z >> 4), k -> {
                denseChunkReads++;
                return St03TerrainColumns.read(gen, level, rs, x & ~15, z & ~15);
            });
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

        /**
         * Returns [lowerQuartBiomeIdx, upperQuartBiomeIdx, kind19, kind8, catMask]
         * kind: 0 = eligible, 1 = edge-neutral, 2 = ineligible
         * catMask bit0 = ocean, bit1 = river, bit2 = beach/shore
         */
        int[] columnBiomeInfo(int x, int z) {
            return colBiomes.computeIfAbsent(key2(x, z), k -> {
                int qx = Math.floorDiv(x, 4), qz = Math.floorDiv(z, 4);
                int qy = Math.floorDiv(surface(x, z) - 3, 4);
                String b0 = quartBiome(qx, qy, qz);
                String b1 = quartBiome(qx, qy + 1, qz);
                int k19 = (ALL_19.contains(b0) && ALL_19.contains(b1)) ? 0
                        : (EDGE_5.contains(b0) && EDGE_5.contains(b1)) ? 1 : 2;
                int k8 = (ORIG_8.contains(b0) && ORIG_8.contains(b1)) ? 0
                        : (EDGE_5.contains(b0) && EDGE_5.contains(b1)) ? 1 : 2;
                int mask = 0;
                if (b0.contains("ocean") || b1.contains("ocean")) mask |= 1;
                if (b0.contains("river") || b1.contains("river")) mask |= 2;
                if (b0.contains("beach") || b0.contains("shore") || b1.contains("beach") || b1.contains("shore")) mask |= 4;
                return new int[]{biomeIdx(b0), biomeIdx(b1), k19, k8, mask};
            });
        }
    }

    private record SubBoxStats(
            int size, int samples, int good19, int good8, int edge, int water,
            int min, int p05, int median, int p95, int max, int floorMin,
            int pWidth, int rWidth, int reqCut, int reqSkirt, int reqFill, int reqCutSkirtSpan,
            int foundationY, int optCut, int optSkirt, int optFill,
            boolean denseEvaluated, int denseMaxCut, int denseMaxSkirt, int denseMaxFill,
            int denseExceedCutCols, int denseExceedSkirtCols, int denseExceedFillCols, int denseExceedAnyCols,
            int denseMin, int denseMax, int denseFloorMin
    ) {
        boolean samplePass(boolean centerPass, boolean use19) {
            if (!centerPass) return false;
            int good = use19 ? good19 : good8;
            int denom = samples - edge;
            if (denom <= 0 || 100L * good < 80L * denom) return false;
            if (100L * water > 5L * samples) return false;
            if (pWidth > St03PlacementConfig.MAX_HEIGHT_DIFFERENCE) return false;
            int low = max - St03PlacementConfig.MAX_CUT;
            if (low > min + St03PlacementConfig.MAX_SKIRT) return false;
            if (low > floorMin + St03PlacementConfig.MAX_FOOTING_FILL) return false;
            return true;
        }
    }

    private static final class SiteEval {
        final int originX, originZ, dxChunks, dzChunks;
        final String centerBiomeLower, centerBiomeUpper;
        final boolean centerPass19, centerPass8;
        final boolean fullSampleEvaluated;
        final int good19, good8, edge, oceanCells, riverCells, beachCells;
        final int waterSamples, min, p05, p25, median, p75, p95, max, floorMin;
        final int pWidth, rWidth;
        final int reqCut, reqSkirt, reqFill, reqCutSkirtSpan;
        final int foundationY, optCut, optSkirt, optFill;
        final double meanWest, meanEast, meanNorth, meanSouth, slopeDx, slopeDz, slopeMag, slopeAngleDeg;
        final String slopeOctant;
        final boolean adjOcean64, adjRiver64;
        boolean denseEvaluated;
        int denseMaxCut, denseMaxSkirt, denseMaxFill;
        int denseExceedCutCols, denseExceedSkirtCols, denseExceedFillCols, denseExceedAnyCols;
        int denseWaterCols;
        int denseMin = 0, denseMax = 0, denseFloorMin = 0;
        SubBoxStats sub96, sub80, sub64;

        SiteEval(AuditTerrain t, int originX, int originZ, int dxChunks, int dzChunks, boolean isN0, boolean isN2) {
            this.originX = originX;
            this.originZ = originZ;
            this.dxChunks = dxChunks;
            this.dzChunks = dzChunks;

            int[] cInfo = t.columnBiomeInfo(originX + 56, originZ + 56);
            this.centerBiomeLower = t.idByIdx.get(cInfo[0]);
            this.centerBiomeUpper = t.idByIdx.get(cInfo[1]);
            this.centerPass19 = cInfo[2] == 0;
            this.centerPass8 = cInfo[3] == 0;

            // If N=0 and center is eligible, preload the 64 dense chunks of the 112x112 box up front
            // so all sample, dense, and subbox queries at N=0 (and overlapping N<=2 sites) hit memory!
            if (isN0 && (centerPass19 || centerPass8)) {
                t.preloadDenseBox(originX, originZ, 112);
            }

            // Always evaluate all sample stages at N=0, and at any N<=4 offset whose center biome is eligible.
            // If !isN0 and center biome is ineligible for both 19 and 8 biomes, skip redundant sample queries.
            if (!isN0 && !centerPass19 && !centerPass8) {
                this.fullSampleEvaluated = false;
                this.good19 = this.good8 = this.edge = this.oceanCells = this.riverCells = this.beachCells = 0;
                this.waterSamples = this.min = this.p05 = this.p25 = this.median = this.p75 = this.p95 = this.max = this.floorMin = 0;
                this.pWidth = this.rWidth = this.reqCut = this.reqSkirt = this.reqFill = this.reqCutSkirtSpan = 0;
                this.foundationY = this.optCut = this.optSkirt = this.optFill = 0;
                this.meanWest = this.meanEast = this.meanNorth = this.meanSouth = this.slopeDx = this.slopeDz = this.slopeMag = this.slopeAngleDeg = 0.0;
                this.slopeOctant = "FLAT";
                this.adjOcean64 = this.adjRiver64 = false;
                return;
            }

            int g19 = 0, g8 = 0, ed = 0, oc = 0, rv = 0, bc = 0;
            for (int x = originX - 2; x <= originX + 110; x += 8) {
                for (int z = originZ - 2; z <= originZ + 110; z += 8) {
                    int[] info = t.columnBiomeInfo(x, z);
                    if (info[2] == 0) g19++;
                    if (info[3] == 0) g8++;
                    if (info[2] == 1) ed++;
                    if ((info[4] & 1) != 0) oc++;
                    if ((info[4] & 2) != 0) rv++;
                    if ((info[4] & 4) != 0) bc++;
                }
            }
            this.good19 = g19;
            this.good8 = g8;
            this.edge = ed;
            this.oceanCells = oc;
            this.riverCells = rv;
            this.beachCells = bc;

            // For outer offsets N=3..4 (!isN0 && !isN2), we only need baseline N=3/N=4 pass flags,
            // so if 80% footprint biome fails for both 19 and 8 biomes, skip height sampling at N=3..4.
            int denom = 225 - ed;
            if (!isN0 && !isN2 && (denom <= 0 || (100L * g19 < 80L * denom && 100L * g8 < 80L * denom))) {
                this.fullSampleEvaluated = false;
                this.waterSamples = this.min = this.p05 = this.p25 = this.median = this.p75 = this.p95 = this.max = this.floorMin = 0;
                this.pWidth = this.rWidth = this.reqCut = this.reqSkirt = this.reqFill = this.reqCutSkirtSpan = 0;
                this.foundationY = this.optCut = this.optSkirt = this.optFill = 0;
                this.meanWest = this.meanEast = this.meanNorth = this.meanSouth = this.slopeDx = this.slopeDz = this.slopeMag = this.slopeAngleDeg = 0.0;
                this.slopeOctant = "FLAT";
                this.adjOcean64 = this.adjRiver64 = false;
                return;
            }

            this.fullSampleEvaluated = true;
            boolean ringOcean = oc > 0, ringRiver = rv > 0;
            if (isN0 && (!ringOcean || !ringRiver)) {
                int cx = originX + 56, cz = originZ + 56;
                int[][] ringOffsets = {
                        {-120, 0}, {120, 0}, {0, -120}, {0, 120},
                        {-120, -120}, {-120, 120}, {120, -120}, {120, 120},
                        {-88, 0}, {88, 0}, {0, -88}, {0, 88}
                };
                for (int[] ro : ringOffsets) {
                    int[] rInfo = t.columnBiomeInfo(cx + ro[0], cz + ro[1]);
                    if ((rInfo[4] & 1) != 0) ringOcean = true;
                    if ((rInfo[4] & 2) != 0) ringRiver = true;
                }
            }
            this.adjOcean64 = ringOcean;
            this.adjRiver64 = ringRiver;

            int[] heights = new int[225];
            int count = 0, water = 0, fMin = Integer.MAX_VALUE;
            long sumWest = 0, sumEast = 0, sumNorth = 0, sumSouth = 0;
            int nWest = 0, nEast = 0, nNorth = 0, nSouth = 0;
            double sumX = 0, sumZ = 0, sumY = 0, sumXX = 0, sumZZ = 0, sumXY = 0, sumZY = 0;

            for (int ix = 0; ix < 15; ix++) {
                int dx = OFFSETS_112[ix];
                for (int iz = 0; iz < 15; iz++) {
                    int dz = OFFSETS_112[iz];
                    int s = t.surface(originX + dx, originZ + dz);
                    int f = t.floor(originX + dx, originZ + dz);
                    heights[count++] = s;
                    if (s > f + 1) water++;
                    fMin = Math.min(fMin, f);

                    if (ix <= 6) { sumWest += s; nWest++; }
                    if (ix >= 8) { sumEast += s; nEast++; }
                    if (iz <= 6) { sumNorth += s; nNorth++; }
                    if (iz >= 8) { sumSouth += s; nSouth++; }

                    double rx = dx - 56.0, rz = dz - 56.0;
                    sumX += rx; sumZ += rz; sumY += s;
                    sumXX += rx * rx; sumZZ += rz * rz;
                    sumXY += rx * s; sumZY += rz * s;
                }
            }
            this.waterSamples = water;
            this.floorMin = fMin;
            Arrays.sort(heights);
            this.min = heights[0];
            this.max = heights[224];
            this.median = heights[112];
            this.p05 = St03Eligibility.nearestRank(heights, 0.05);
            this.p25 = St03Eligibility.nearestRank(heights, 0.25);
            this.p75 = St03Eligibility.nearestRank(heights, 0.75);
            this.p95 = St03Eligibility.nearestRank(heights, 0.95);
            this.pWidth = p95 - p05;
            this.rWidth = max - min;

            this.reqCut = Math.max(0, max - median);
            this.reqSkirt = Math.max(0, median - min);
            this.reqFill = Math.max(0, median - floorMin);
            this.reqCutSkirtSpan = max - min;

            int low = max - St03PlacementConfig.MAX_CUT;
            int highSkirt = min + St03PlacementConfig.MAX_SKIRT;
            int highFill = floorMin + St03PlacementConfig.MAX_FOOTING_FILL;
            int y = Math.max(low, Math.min(median, Math.min(highSkirt, highFill)));
            this.foundationY = y;
            this.optCut = Math.max(0, max - y);
            this.optSkirt = Math.max(0, y - min);
            this.optFill = Math.max(0, y - floorMin);

            this.meanWest = (double) sumWest / nWest;
            this.meanEast = (double) sumEast / nEast;
            this.meanNorth = (double) sumNorth / nNorth;
            this.meanSouth = (double) sumSouth / nSouth;

            double n = 225.0;
            double varX = sumXX - (sumX * sumX) / n;
            double varZ = sumZZ - (sumZ * sumZ) / n;
            double covXY = sumXY - (sumX * sumY) / n;
            double covZY = sumZY - (sumZ * sumY) / n;
            this.slopeDx = covXY / varX;
            this.slopeDz = covZY / varZ;
            this.slopeMag = Math.hypot(slopeDx, slopeDz) * 112.0;
            double deg = Math.toDegrees(Math.atan2(slopeDz, slopeDx));
            if (deg < 0) deg += 360.0;
            this.slopeAngleDeg = deg;
            String[] octants = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
            this.slopeOctant = this.slopeMag < 1.0 ? "FLAT" : octants[((int) Math.round(deg / 45.0)) % 8];

            if (isN0 || isN2) {
                boolean denseNow = isN0 && (centerPass19 || centerPass8);
                this.sub96 = evalSubBox(t, originX, originZ, 96, denseNow);
                this.sub80 = evalSubBox(t, originX, originZ, 80, denseNow);
                this.sub64 = evalSubBox(t, originX, originZ, 64, denseNow);
            }
        }

        void evaluateDense(AuditTerrain t, boolean alsoSubBoxes) {
            if (!denseEvaluated) {
                denseEvaluated = true;
                int cMax = 0, sMax = 0, fMax = 0;
                int exC = 0, exS = 0, exF = 0, exAny = 0, wCols = 0;
                int dMin = Integer.MAX_VALUE, dMax = Integer.MIN_VALUE, dfMin = Integer.MAX_VALUE;
                for (int x = originX; x < originX + 112; x++) {
                    for (int z = originZ; z < originZ + 112; z++) {
                        var col = t.dense(x, z);
                        int s = col.surfaceAt(x, z);
                        int f = col.floorAt(x, z);
                        dMin = Math.min(dMin, s);
                        dMax = Math.max(dMax, s);
                        dfMin = Math.min(dfMin, f);
                        if (s > f + 1) wCols++;
                        int c = s - foundationY;
                        int sk = foundationY - s;
                        int fi = foundationY - f;
                        cMax = Math.max(cMax, c);
                        sMax = Math.max(sMax, sk);
                        fMax = Math.max(fMax, fi);
                        boolean bC = c > St03PlacementConfig.MAX_CUT;
                        boolean bS = sk > St03PlacementConfig.MAX_SKIRT;
                        boolean bF = fi > St03PlacementConfig.MAX_FOOTING_FILL;
                        if (bC) exC++;
                        if (bS) exS++;
                        if (bF) exF++;
                        if (bC || bS || bF) exAny++;
                    }
                }
                this.denseMaxCut = Math.max(0, cMax);
                this.denseMaxSkirt = Math.max(0, sMax);
                this.denseMaxFill = Math.max(0, fMax);
                this.denseExceedCutCols = exC;
                this.denseExceedSkirtCols = exS;
                this.denseExceedFillCols = exF;
                this.denseExceedAnyCols = exAny;
                this.denseWaterCols = wCols;
                this.denseMin = dMin;
                this.denseMax = dMax;
                this.denseFloorMin = dfMin;
            }
            if (alsoSubBoxes) {
                if (sub96 == null || !sub96.denseEvaluated()) sub96 = evalSubBox(t, originX, originZ, 96, true);
                if (sub80 == null || !sub80.denseEvaluated()) sub80 = evalSubBox(t, originX, originZ, 80, true);
                if (sub64 == null || !sub64.denseEvaluated()) sub64 = evalSubBox(t, originX, originZ, 64, true);
            }
        }

        private static SubBoxStats evalSubBox(AuditTerrain t, int baseOriginX, int baseOriginZ, int size, boolean withDense) {
            int margin = (112 - size) / 2;
            int ox = baseOriginX + margin;
            int oz = baseOriginZ + margin;
            int steps = size / 8 + 1;
            int[] offsets = new int[steps];
            for (int i = 0; i < steps - 1; i++) offsets[i] = i * 8;
            offsets[steps - 1] = size - 1;

            int g19 = 0, g8 = 0, ed = 0;
            for (int x = ox - 2; x <= ox + size - 2; x += 8) {
                for (int z = oz - 2; z <= oz + size - 2; z += 8) {
                    int[] info = t.columnBiomeInfo(x, z);
                    if (info[2] == 0) g19++;
                    if (info[3] == 0) g8++;
                    if (info[2] == 1) ed++;
                }
            }

            int totalSamples = steps * steps;
            int[] heights = new int[totalSamples];
            int count = 0, water = 0, fMin = Integer.MAX_VALUE;
            for (int dx : offsets) {
                for (int dz : offsets) {
                    int s = t.surface(ox + dx, oz + dz);
                    int f = t.floor(ox + dx, oz + dz);
                    heights[count++] = s;
                    if (s > f + 1) water++;
                    fMin = Math.min(fMin, f);
                }
            }
            Arrays.sort(heights);
            int min = heights[0], max = heights[totalSamples - 1], median = heights[totalSamples / 2];
            int p05 = St03Eligibility.nearestRank(heights, 0.05);
            int p95 = St03Eligibility.nearestRank(heights, 0.95);
            int pW = p95 - p05, rW = max - min;
            int reqC = Math.max(0, max - median), reqS = Math.max(0, median - min), reqF = Math.max(0, median - fMin);
            int low = max - St03PlacementConfig.MAX_CUT;
            int highSkirt = min + St03PlacementConfig.MAX_SKIRT;
            int highFill = fMin + St03PlacementConfig.MAX_FOOTING_FILL;
            int y = Math.max(low, Math.min(median, Math.min(highSkirt, highFill)));
            int optC = Math.max(0, max - y), optS = Math.max(0, y - min), optF = Math.max(0, y - fMin);

            int dMaxC = 0, dMaxS = 0, dMaxF = 0, exC = 0, exS = 0, exF = 0, exAny = 0;
            int dMin = 0, dMax = 0, dfMin = 0;
            if (withDense) {
                dMin = Integer.MAX_VALUE;
                dMax = Integer.MIN_VALUE;
                dfMin = Integer.MAX_VALUE;
                for (int x = ox; x < ox + size; x++) {
                    for (int z = oz; z < oz + size; z++) {
                        var col = t.dense(x, z);
                        int s = col.surfaceAt(x, z);
                        int f = col.floorAt(x, z);
                        dMin = Math.min(dMin, s);
                        dMax = Math.max(dMax, s);
                        dfMin = Math.min(dfMin, f);
                        int c = s - y, sk = y - s, fi = y - f;
                        dMaxC = Math.max(dMaxC, c);
                        dMaxS = Math.max(dMaxS, sk);
                        dMaxF = Math.max(dMaxF, fi);
                        boolean bC = c > St03PlacementConfig.MAX_CUT;
                        boolean bS = sk > St03PlacementConfig.MAX_SKIRT;
                        boolean bF = fi > St03PlacementConfig.MAX_FOOTING_FILL;
                        if (bC) exC++;
                        if (bS) exS++;
                        if (bF) exF++;
                        if (bC || bS || bF) exAny++;
                    }
                }
            }
            return new SubBoxStats(
                size, totalSamples, g19, g8, ed, water,
                min, p05, median, p95, max, fMin,
                pW, rW, reqC, reqS, reqF, rW,
                y, optC, optS, optF,
                withDense, Math.max(0, dMaxC), Math.max(0, dMaxS), Math.max(0, dMaxF),
                exC, exS, exF, exAny,
                dMin, dMax, dfMin
            );
        }

        boolean footprintBiomePass(boolean use19, int minBiomePct) {
            if (!fullSampleEvaluated) return false;
            int good = use19 ? good19 : good8;
            int denom = 225 - edge;
            return denom > 0 && 100L * good >= (long) minBiomePct * denom;
        }

        boolean waterPass(int maxWaterPct) {
            return fullSampleEvaluated && 100L * waterSamples <= (long) maxWaterPct * 225L;
        }

        boolean heightPass(int maxPWidth) {
            return fullSampleEvaluated && pWidth <= maxPWidth;
        }

        boolean cutSkirtPass(int maxCut, int maxSkirt) {
            return fullSampleEvaluated && (max - maxCut) <= (min + maxSkirt);
        }

        boolean footingFillPass(int maxCut, int maxFill) {
            return fullSampleEvaluated && (max - maxCut) <= (floorMin + maxFill);
        }

        boolean samplePass(boolean use19, int minBiomePct, int maxWaterPct, int maxPWidth, int maxCut, int maxSkirt, int maxFill) {
            boolean cPass = use19 ? centerPass19 : centerPass8;
            return cPass && fullSampleEvaluated && footprintBiomePass(use19, minBiomePct)
                    && waterPass(maxWaterPct) && heightPass(maxPWidth)
                    && cutSkirtPass(maxCut, maxSkirt) && footingFillPass(maxCut, maxFill);
        }

        String firstSampleFailure(boolean use19, int minBiomePct, int maxWaterPct, int maxPWidth, int maxCut, int maxSkirt, int maxFill) {
            if (!(use19 ? centerPass19 : centerPass8)) return "REJECT_CENTER_BIOME";
            if (!footprintBiomePass(use19, minBiomePct)) return "REJECT_FOOTPRINT_BIOME";
            if (!waterPass(maxWaterPct)) return "REJECT_WATER";
            if (!heightPass(maxPWidth)) return "REJECT_HEIGHT_DIFFERENCE";
            if (!cutSkirtPass(maxCut, maxSkirt)) return "REJECT_CUT_SKIRT";
            if (!footingFillPass(maxCut, maxFill)) return "REJECT_FOOTING_FILL";
            return "ELIGIBLE";
        }
    }

    private static void runAudit(ServerLevel level, Path dir, Map<String, Object> summary) throws Exception {
        long started = System.nanoTime();
        long seed = level.getSeed();
        AuditTerrain t = new AuditTerrain(level);
        var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, St03Worldgen.STRUCTURE_ID));
        Structure structure = holder.value();
        var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, St03Worldgen.STRUCTURE_ID)).value();
        var placement = (RandomSpreadStructurePlacement) set.placement();
        var state = level.getChunkSource().getGeneratorState();
        var sampler = t.rs.createClimateSampler(SamplerContext.builder().enableCaches().build());

        summary.put("seed", seed);
        summary.put("head_sha", System.getenv("GITHUB_SHA"));
        summary.put("run_id", System.getenv("GITHUB_RUN_ID"));
        summary.put("spacing", placement.spacing());
        summary.put("separation", placement.separation());
        summary.put("salt", St03PlacementConfig.SALT);
        summary.put("region_radius", RADIUS);

        // Pre-discover other structure sets for fast bounding-box overlap check on valid starts
        // Exclude sky_islands (aerial Y>180 structures) and st03 itself
        List<Holder<StructureSet>> otherSets = new ArrayList<>();
        for (Holder<StructureSet> h : state.possibleStructureSets()) {
            String id = h.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (!id.equals(St03Worldgen.STRUCTURE_ID.toString()) && !id.contains("sky_islands")) {
                otherSets.add(h);
            }
        }

        int totalCandidates = 0;
        int validStarts19 = 0, validStarts8 = 0;
        int validStartsOverlapAny3d = 0, validStartsOverlapVillage3d = 0;
        int validStartsOverlapAny2d = 0, validStartsOverlapVillage2d = 0;
        List<Map<String, Object>> validOverlapsDetail = new ArrayList<>();

        try (BufferedWriter candCsv = Files.newBufferedWriter(dir.resolve("candidates_audit.csv"));
             BufferedWriter searchCsv = Files.newBufferedWriter(dir.resolve("search_n2_audit.csv"));
             BufferedWriter subboxCsv = Files.newBufferedWriter(dir.resolve("subboxes_audit.csv"));
             BufferedWriter startsCsv = Files.newBufferedWriter(dir.resolve("valid_starts_overlaps.csv"))) {

            candCsv.write("seed,rx,rz,cx,cz,offset_x,offset_z,chebyshev_center,manhattan_center,"
                    + "center_biome_lower,center_biome_upper,center_pass_19,center_pass_8,"
                    + "good_19,good_8,edge,ocean_cells,river_cells,beach_cells,adj_ocean_64,adj_river_64,"
                    + "water_samples,min,p05,p25,median,p75,p95,max,floor_min,p_width,r_width,"
                    + "req_cut,req_skirt,req_fill,req_cut_skirt_span,foundation_y,opt_cut,opt_skirt,opt_fill,"
                    + "slope_dx,slope_dz,slope_mag,slope_angle_deg,slope_octant,"
                    + "first_fail_19,first_fail_8,"
                    + "dense_eval,dense_max_cut,dense_max_skirt,dense_max_fill,"
                    + "dense_ex_cut_cols,dense_ex_skirt_cols,dense_ex_fill_cols,dense_ex_any_cols,dense_water_cols,"
                    + "dense_min,dense_max,dense_floor_min,"
                    + "n0_pass_19_sample,n0_pass_19_dense,n1_pass_19_sample,n1_pass_19_dense,"
                    + "n2_pass_19_sample,n2_pass_19_dense,n3_pass_19_sample,n3_pass_19_dense,n4_pass_19_sample,n4_pass_19_dense,"
                    + "n0_pass_8_sample,n0_pass_8_dense,n1_pass_8_sample,n1_pass_8_dense,"
                    + "n2_pass_8_sample,n2_pass_8_dense,n3_pass_8_sample,n3_pass_8_dense,n4_pass_8_sample,n4_pass_8_dense,"
                    + "n2_best_dx_19,n2_best_dz_19,n2_best_biome_19,n2_best_dx_8,n2_best_dz_8\n");

            searchCsv.write("seed,rx,rz,dx,dz,cx,cz,center_biome,center_pass_19,center_pass_8,"
                    + "good_19,good_8,edge,water_samples,min,p05,median,p95,max,floor_min,p_width,r_width,"
                    + "foundation_y,opt_cut,opt_skirt,opt_fill,first_fail_19,first_fail_8,"
                    + "dense_eval,dense_max_cut,dense_max_skirt,dense_max_fill,"
                    + "dense_ex_cut_cols,dense_ex_skirt_cols,dense_ex_fill_cols,dense_ex_any_cols,"
                    + "dense_min,dense_max,dense_floor_min\n");

            subboxCsv.write("seed,rx,rz,size,samples,center_biome,center_pass_19,center_pass_8,"
                    + "good_19,good_8,edge,water,min,p05,median,p95,max,floor_min,p_width,r_width,"
                    + "foundation_y,opt_cut,opt_skirt,opt_fill,"
                    + "dense_eval,dense_max_cut,dense_max_skirt,dense_max_fill,"
                    + "dense_ex_cut_cols,dense_ex_skirt_cols,dense_ex_fill_cols,dense_ex_any_cols,"
                    + "dense_min,dense_max,dense_floor_min,"
                    + "n2_pass_19_sample,n2_pass_19_dense,n2_pass_8_sample,n2_pass_8_dense\n");

            startsCsv.write("seed,rx,rz,cand_cx,cand_cz,dx,dz,origin_x,foundation_y,origin_z,center_biome,"
                    + "adj_ocean_64,adj_river_64,overlap_any_3d,overlap_village_3d,overlap_any_2d,overlap_village_2d,overlaps_3d,overlaps_2d\n");

            for (int rx = -RADIUS; rx <= RADIUS; rx++) {
                for (int rz = -RADIUS; rz <= RADIUS; rz++) {
                    t.clearRegionCache();
                    ChunkPos cp = placement.getPotentialStructureChunk(seed, rx * 66, rz * 66);
                    totalCandidates++;
                    int offsetX = cp.x() - rx * 66;
                    int offsetZ = cp.z() - rz * 66;
                    int chebCenter = Math.max(Math.abs(offsetX - 21), Math.abs(offsetZ - 21));
                    int manhCenter = Math.abs(offsetX - 21) + Math.abs(offsetZ - 21);

                    int baseOx = cp.getMinBlockX() - St03PlacementConfig.BOX_SIZE / 2 + 8;
                    int baseOz = cp.getMinBlockZ() - St03PlacementConfig.BOX_SIZE / 2 + 8;

                    // Evaluate N=0 first so its dense chunks (when center-eligible) warm the cache for N=1..4
                    SiteEval[][] grid = new SiteEval[9][9];
                    SiteEval n0 = new SiteEval(t, baseOx, baseOz, 0, 0, true, true);
                    if (n0.centerPass19 || n0.centerPass8 || n0.samplePass(true, 80, 5, 20, 10, 12, 16)) {
                        n0.evaluateDense(t, true);
                    }
                    grid[4][4] = n0;

                    for (int dx = -4; dx <= 4; dx++) {
                        for (int dz = -4; dz <= 4; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            boolean isN2 = (Math.abs(dx) <= 2 && Math.abs(dz) <= 2);
                            SiteEval se = new SiteEval(t, baseOx + dx * 16, baseOz + dz * 16, dx, dz, false, isN2);
                            boolean needDense = se.samplePass(true, 80, 5, 20, 10, 12, 16)
                                    || se.samplePass(false, 80, 5, 20, 10, 12, 16)
                                    || (isN2 && se.centerPass19 && (
                                            se.samplePass(true, 60, 5, 20, 10, 12, 16)
                                            || se.samplePass(true, 80, 15, 20, 10, 12, 16)
                                            || se.samplePass(true, 80, 5, 32, 16, 20, 24)));
                            if (needDense) {
                                se.evaluateDense(t, false);
                            }
                            grid[dx + 4][dz + 4] = se;
                        }
                    }

                    // Compute N=0..4 pass flags & N=2 best site using St03SiteSearch's exact score ordering
                    boolean[] pass19Sample = new boolean[5];
                    boolean[] pass19Dense = new boolean[5];
                    boolean[] pass8Sample = new boolean[5];
                    boolean[] pass8Dense = new boolean[5];
                    SiteEval best19N2 = null, best8N2 = null;

                    for (int n = 0; n <= 4; n++) {
                        SiteEval best19 = null, best8 = null;
                        int bScore19 = Integer.MAX_VALUE, bScore8 = Integer.MAX_VALUE;
                        boolean s19 = false, s8 = false;
                        for (int dx = -n; dx <= n; dx++) {
                            for (int dz = -n; dz <= n; dz++) {
                                SiteEval se = grid[dx + 4][dz + 4];
                                int score = Math.max(se.optCut, Math.max(se.optSkirt, se.optFill)) * 100000
                                        + se.rWidth * 1000 + Math.max(Math.abs(dx), Math.abs(dz)) * 10;
                                if (se.samplePass(true, 80, 5, 20, 10, 12, 16)) {
                                    s19 = true;
                                    if (score < bScore19 && se.denseExceedAnyCols == 0) {
                                        bScore19 = score;
                                        best19 = se;
                                    }
                                }
                                if (se.samplePass(false, 80, 5, 20, 10, 12, 16)) {
                                    s8 = true;
                                    if (score < bScore8 && se.denseExceedAnyCols == 0) {
                                        bScore8 = score;
                                        best8 = se;
                                    }
                                }
                            }
                        }
                        pass19Sample[n] = s19;
                        pass19Dense[n] = (best19 != null);
                        pass8Sample[n] = s8;
                        pass8Dense[n] = (best8 != null);
                        if (n == 2) {
                            best19N2 = best19;
                            best8N2 = best8;
                        }
                    }

                    // Compute N=2 pass flags for sub-boxes (96, 80, 64)
                    boolean sub96S19 = false, sub96D19 = false, sub96S8 = false, sub96D8 = false;
                    boolean sub80S19 = false, sub80D19 = false, sub80S8 = false, sub80D8 = false;
                    boolean sub64S19 = false, sub64D19 = false, sub64S8 = false, sub64D8 = false;

                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            SiteEval se = grid[dx + 4][dz + 4];
                            if (!se.fullSampleEvaluated || se.sub96 == null) continue;
                            if (se.sub96.samplePass(se.centerPass19, true)) {
                                sub96S19 = true;
                                if (!sub96D19) {
                                    if (!se.sub96.denseEvaluated()) se.sub96 = SiteEval.evalSubBox(t, se.originX, se.originZ, 96, true);
                                    if (se.sub96.denseExceedAnyCols() == 0) sub96D19 = true;
                                }
                            }
                            if (se.sub96.samplePass(se.centerPass8, false)) {
                                sub96S8 = true;
                                if (!sub96D8) {
                                    if (!se.sub96.denseEvaluated()) se.sub96 = SiteEval.evalSubBox(t, se.originX, se.originZ, 96, true);
                                    if (se.sub96.denseExceedAnyCols() == 0) sub96D8 = true;
                                }
                            }
                            if (se.sub80.samplePass(se.centerPass19, true)) {
                                sub80S19 = true;
                                if (!sub80D19) {
                                    if (!se.sub80.denseEvaluated()) se.sub80 = SiteEval.evalSubBox(t, se.originX, se.originZ, 80, true);
                                    if (se.sub80.denseExceedAnyCols() == 0) sub80D19 = true;
                                }
                            }
                            if (se.sub80.samplePass(se.centerPass8, false)) {
                                sub80S8 = true;
                                if (!sub80D8) {
                                    if (!se.sub80.denseEvaluated()) se.sub80 = SiteEval.evalSubBox(t, se.originX, se.originZ, 80, true);
                                    if (se.sub80.denseExceedAnyCols() == 0) sub80D8 = true;
                                }
                            }
                            if (se.sub64.samplePass(se.centerPass19, true)) {
                                sub64S19 = true;
                                if (!sub64D19) {
                                    if (!se.sub64.denseEvaluated()) se.sub64 = SiteEval.evalSubBox(t, se.originX, se.originZ, 64, true);
                                    if (se.sub64.denseExceedAnyCols() == 0) sub64D19 = true;
                                }
                            }
                            if (se.sub64.samplePass(se.centerPass8, false)) {
                                sub64S8 = true;
                                if (!sub64D8) {
                                    if (!se.sub64.denseEvaluated()) se.sub64 = SiteEval.evalSubBox(t, se.originX, se.originZ, 64, true);
                                    if (se.sub64.denseExceedAnyCols() == 0) sub64D8 = true;
                                }
                            }
                        }
                    }

                    // Write N=0 subboxes (112, 96, 80, 64) with their N=2 pass flags
                    writeSubBoxRow(subboxCsv, seed, rx, rz, n0, new SubBoxStats(
                            112, 225, n0.good19, n0.good8, n0.edge, n0.waterSamples,
                            n0.min, n0.p05, n0.median, n0.p95, n0.max, n0.floorMin,
                            n0.pWidth, n0.rWidth, n0.reqCut, n0.reqSkirt, n0.reqFill, n0.reqCutSkirtSpan,
                            n0.foundationY, n0.optCut, n0.optSkirt, n0.optFill,
                            n0.denseEvaluated, n0.denseMaxCut, n0.denseMaxSkirt, n0.denseMaxFill,
                            n0.denseExceedCutCols, n0.denseExceedSkirtCols, n0.denseExceedFillCols, n0.denseExceedAnyCols,
                            n0.denseMin, n0.denseMax, n0.denseFloorMin
                    ), pass19Sample[2], pass19Dense[2], pass8Sample[2], pass8Dense[2]);
                    writeSubBoxRow(subboxCsv, seed, rx, rz, n0, n0.sub96, sub96S19, sub96D19, sub96S8, sub96D8);
                    writeSubBoxRow(subboxCsv, seed, rx, rz, n0, n0.sub80, sub80S19, sub80D19, sub80S8, sub80D8);
                    writeSubBoxRow(subboxCsv, seed, rx, rz, n0, n0.sub64, sub64S19, sub64D19, sub64S8, sub64D8);

                    // Write N<=2 25 sites to search_n2_audit.csv
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            SiteEval se = grid[dx + 4][dz + 4];
                            searchCsv.write(seed + "," + rx + "," + rz + "," + dx + "," + dz + ","
                                    + (cp.x() + dx) + "," + (cp.z() + dz) + ","
                                    + se.centerBiomeLower + "," + se.centerPass19 + "," + se.centerPass8 + ","
                                    + se.good19 + "," + se.good8 + "," + se.edge + "," + se.waterSamples + ","
                                    + se.min + "," + se.p05 + "," + se.median + "," + se.p95 + "," + se.max + "," + se.floorMin + ","
                                    + se.pWidth + "," + se.rWidth + ","
                                    + se.foundationY + "," + se.optCut + "," + se.optSkirt + "," + se.optFill + ","
                                    + se.firstSampleFailure(true, 80, 5, 20, 10, 12, 16) + ","
                                    + se.firstSampleFailure(false, 80, 5, 20, 10, 12, 16) + ","
                                    + se.denseEvaluated + "," + se.denseMaxCut + "," + se.denseMaxSkirt + "," + se.denseMaxFill + ","
                                    + se.denseExceedCutCols + "," + se.denseExceedSkirtCols + "," + se.denseExceedFillCols + "," + se.denseExceedAnyCols + ","
                                    + se.denseMin + "," + se.denseMax + "," + se.denseFloorMin + "\n");
                        }
                    }

                    // Write candidate row
                    candCsv.write(seed + "," + rx + "," + rz + "," + cp.x() + "," + cp.z() + ","
                            + offsetX + "," + offsetZ + "," + chebCenter + "," + manhCenter + ","
                            + n0.centerBiomeLower + "," + n0.centerBiomeUpper + "," + n0.centerPass19 + "," + n0.centerPass8 + ","
                            + n0.good19 + "," + n0.good8 + "," + n0.edge + "," + n0.oceanCells + "," + n0.riverCells + "," + n0.beachCells + ","
                            + n0.adjOcean64 + "," + n0.adjRiver64 + ","
                            + n0.waterSamples + "," + n0.min + "," + n0.p05 + "," + n0.p25 + "," + n0.median + "," + n0.p75 + "," + n0.p95 + "," + n0.max + "," + n0.floorMin + ","
                            + n0.pWidth + "," + n0.rWidth + ","
                            + n0.reqCut + "," + n0.reqSkirt + "," + n0.reqFill + "," + n0.reqCutSkirtSpan + ","
                            + n0.foundationY + "," + n0.optCut + "," + n0.optSkirt + "," + n0.optFill + ","
                            + String.format(Locale.ROOT, "%.4f,%.4f,%.2f,%.1f,%s,", n0.slopeDx, n0.slopeDz, n0.slopeMag, n0.slopeAngleDeg, n0.slopeOctant)
                            + n0.firstSampleFailure(true, 80, 5, 20, 10, 12, 16) + ","
                            + n0.firstSampleFailure(false, 80, 5, 20, 10, 12, 16) + ","
                            + n0.denseEvaluated + "," + n0.denseMaxCut + "," + n0.denseMaxSkirt + "," + n0.denseMaxFill + ","
                            + n0.denseExceedCutCols + "," + n0.denseExceedSkirtCols + "," + n0.denseExceedFillCols + "," + n0.denseExceedAnyCols + "," + n0.denseWaterCols + ","
                            + n0.denseMin + "," + n0.denseMax + "," + n0.denseFloorMin + ","
                            + pass19Sample[0] + "," + pass19Dense[0] + "," + pass19Sample[1] + "," + pass19Dense[1] + ","
                            + pass19Sample[2] + "," + pass19Dense[2] + "," + pass19Sample[3] + "," + pass19Dense[3] + "," + pass19Sample[4] + "," + pass19Dense[4] + ","
                            + pass8Sample[0] + "," + pass8Dense[0] + "," + pass8Sample[1] + "," + pass8Dense[1] + ","
                            + pass8Sample[2] + "," + pass8Dense[2] + "," + pass8Sample[3] + "," + pass8Dense[3] + "," + pass8Sample[4] + "," + pass8Dense[4] + ","
                            + (best19N2 != null ? best19N2.dxChunks : "") + "," + (best19N2 != null ? best19N2.dzChunks : "") + ","
                            + (best19N2 != null ? best19N2.centerBiomeLower : "") + ","
                            + (best8N2 != null ? best8N2.dxChunks : "") + "," + (best8N2 != null ? best8N2.dzChunks : "") + "\n");

                    if (best8N2 != null) validStarts8++;

                    if (best19N2 != null) {
                        validStarts19++;
                        // Spot-check parity with actual Structure.generate on first 5 valid starts per seed
                        if (validStarts19 <= 5) {
                            StructureStart genStart = structure.generate(holder, level.dimension(), level.registryAccess(),
                                    t.gen, t.gen.getBiomeSource(), sampler, t.rs,
                                    level.getStructureTemplateManager(), seed, cp, 0, level, structure.biomes()::contains);
                            if (!genStart.isValid()) {
                                throw new AssertionError("Audit N=2 pass did not produce valid StructureStart at " + cp);
                            }
                        }
                        BoundingBox hqBox = new BoundingBox(
                                best19N2.originX, best19N2.foundationY - 16, best19N2.originZ,
                                best19N2.originX + 111, best19N2.foundationY + 30, best19N2.originZ + 111);
                        OverlapResult ov = findOverlaps(level, t, state, sampler, otherSets, hqBox);
                        boolean ovAny3d = !ov.hits3d.isEmpty();
                        boolean ovVillage3d = ov.hits3d.stream().anyMatch(s -> s.contains("village"));
                        boolean ovAny2d = !ov.hits2d.isEmpty();
                        boolean ovVillage2d = ov.hits2d.stream().anyMatch(s -> s.contains("village"));
                        if (ovAny3d) validStartsOverlapAny3d++;
                        if (ovVillage3d) validStartsOverlapVillage3d++;
                        if (ovAny2d) validStartsOverlapAny2d++;
                        if (ovVillage2d) validStartsOverlapVillage2d++;
                        startsCsv.write(seed + "," + rx + "," + rz + "," + cp.x() + "," + cp.z() + ","
                                + best19N2.dxChunks + "," + best19N2.dzChunks + ","
                                + best19N2.originX + "," + best19N2.foundationY + "," + best19N2.originZ + ","
                                + best19N2.centerBiomeLower + "," + best19N2.adjOcean64 + "," + best19N2.adjRiver64 + ","
                                + ovAny3d + "," + ovVillage3d + "," + ovAny2d + "," + ovVillage2d + ",\""
                                + String.join(";", ov.hits3d) + "\",\"" + String.join(";", ov.hits2d) + "\"\n");
                        if (ovAny3d || ovAny2d) {
                            Map<String, Object> ovRow = new LinkedHashMap<>();
                            ovRow.put("seed", seed);
                            ovRow.put("rx", rx);
                            ovRow.put("rz", rz);
                            ovRow.put("cand_cx", cp.x());
                            ovRow.put("cand_cz", cp.z());
                            ovRow.put("origin_x", best19N2.originX);
                            ovRow.put("foundation_y", best19N2.foundationY);
                            ovRow.put("origin_z", best19N2.originZ);
                            ovRow.put("center_biome", best19N2.centerBiomeLower);
                            ovRow.put("overlaps_3d", ov.hits3d);
                            ovRow.put("overlaps_2d", ov.hits2d);
                            validOverlapsDetail.add(ovRow);
                        }
                    }
                }
                candCsv.flush();
                searchCsv.flush();
                subboxCsv.flush();
                startsCsv.flush();
                LOG.info("ST03_AUDIT2_PROGRESS seed={} rx={} total={} valid19={} valid8={} elapsed_s={}",
                        seed, rx, totalCandidates, validStarts19, validStarts8, (System.nanoTime() - started) / 1e9);
            }
        }

        summary.put("total_candidates", totalCandidates);
        summary.put("valid_starts_19", validStarts19);
        summary.put("valid_starts_8", validStarts8);
        summary.put("valid_starts_overlap_any", validStartsOverlapAny3d);
        summary.put("valid_starts_overlap_village", validStartsOverlapVillage3d);
        summary.put("valid_starts_overlap_any_2d", validStartsOverlapAny2d);
        summary.put("valid_starts_overlap_village_2d", validStartsOverlapVillage2d);
        summary.put("valid_overlaps_detail", validOverlapsDetail);
        summary.put("surface_queries", t.surfaceQueries);
        summary.put("dense_chunk_reads", t.denseChunkReads);
        summary.put("elapsed_seconds", (System.nanoTime() - started) / 1e9);
    }

    private static void writeSubBoxRow(
            BufferedWriter out, long seed, int rx, int rz, SiteEval n0, SubBoxStats sb,
            boolean n2S19, boolean n2D19, boolean n2S8, boolean n2D8) throws Exception {
        out.write(seed + "," + rx + "," + rz + "," + sb.size() + "," + sb.samples() + ","
                + n0.centerBiomeLower + "," + n0.centerPass19 + "," + n0.centerPass8 + ","
                + sb.good19() + "," + sb.good8() + "," + sb.edge() + "," + sb.water() + ","
                + sb.min() + "," + sb.p05() + "," + sb.median() + "," + sb.p95() + "," + sb.max() + "," + sb.floorMin() + ","
                + sb.pWidth() + "," + sb.rWidth() + ","
                + sb.foundationY() + "," + sb.optCut() + "," + sb.optSkirt() + "," + sb.optFill() + ","
                + sb.denseEvaluated() + "," + sb.denseMaxCut() + "," + sb.denseMaxSkirt() + "," + sb.denseMaxFill() + ","
                + sb.denseExceedCutCols() + "," + sb.denseExceedSkirtCols() + "," + sb.denseExceedFillCols() + "," + sb.denseExceedAnyCols() + ","
                + sb.denseMin() + "," + sb.denseMax() + "," + sb.denseFloorMin() + ","
                + n2S19 + "," + n2D19 + "," + n2S8 + "," + n2D8 + "\n");
    }

    private record OverlapResult(List<String> hits3d, List<String> hits2d) {}

    private static OverlapResult findOverlaps(
            ServerLevel level, AuditTerrain t, net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,
            net.minecraft.world.level.biome.Climate.Sampler sampler,
            List<Holder<StructureSet>> otherSets, BoundingBox hqBox) {
        Set<String> hits3d = new TreeSet<>();
        Set<String> hits2d = new TreeSet<>();
        int minCx = (hqBox.minX() >> 4) - 5;
        int maxCx = (hqBox.maxX() >> 4) + 5;
        int minCz = (hqBox.minZ() >> 4) - 5;
        int maxCz = (hqBox.maxZ() >> 4) + 5;
        long seed = level.getSeed();

        for (Holder<StructureSet> h : otherSets) {
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
                    Optional<BoundingBox> cached = t.otherStructureBoxes.get(cacheKey);
                    String matchedLabel = null;
                    BoundingBox stBox = null;
                    if (cached != null) {
                        if (cached.isPresent()) {
                            stBox = cached.get();
                            matchedLabel = cacheKey;
                        }
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
                                    t.gen, t.gen.getBiomeSource(), sampler, t.rs,
                                    level.getStructureTemplateManager(), seed, cp, 0, level, st.biomes()::contains);
                            if (generated.isValid()) {
                                stBox = generated.getBoundingBox();
                                String stId = option.structure().unwrapKey().map(k -> k.identifier().toString()).orElse(setId);
                                matchedLabel = stId + "@" + cp.x() + "," + cp.z();
                                break;
                            }
                            total -= option.weight();
                            options.remove(index);
                        }
                        t.otherStructureBoxes.put(cacheKey, Optional.ofNullable(stBox));
                    }
                    if (stBox != null && matchedLabel != null) {
                        if (stBox.intersects(hqBox)) {
                            hits3d.add(matchedLabel);
                        }
                        if (stBox.maxX() >= hqBox.minX() && stBox.minX() <= hqBox.maxX()
                                && stBox.maxZ() >= hqBox.minZ() && stBox.minZ() <= hqBox.maxZ()) {
                            hits2d.add(matchedLabel);
                        }
                    }
                }
            }
        }
        return new OverlapResult(List.copyOf(hits3d), List.copyOf(hits2d));
    }
}
