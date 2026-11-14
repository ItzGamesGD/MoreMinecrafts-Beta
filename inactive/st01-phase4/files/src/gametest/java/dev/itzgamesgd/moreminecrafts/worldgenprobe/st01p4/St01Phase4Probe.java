package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01p4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01FootprintCorrectionPiece;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PlacementConfig;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01RoyalHallFixture;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 Phase 4 real-server validation probe (inert unless {@code -Dmm.st01.p4.out} is set; not part of build/check).
 *
 * <p>Phases: {@code scan} (fresh world: registration audit, independent stage-by-stage eligibility prediction for EVERY potential
 * anchor in the radius, actual structure starts, real FULL generation of every start, physical verification, controlled-terrain fixture),
 * {@code replay} (fresh world, same seed, REVERSE chunk-generation order; hashes must equal the scan phase) and {@code reload} (saved
 * world of the scan phase restarted; hashes must equal). The royal hall is a temporary integration fixture, not the complete pyramid.
 */
public final class St01Phase4Probe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-p4-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.p4.out");
        if (out == null) return;
        String phase = System.getProperty("mm.st01.p4.phase", "scan");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("phase", phase);
            try {
                run(server, phase, result);
            } catch (Throwable t) {
                LOG.error("ST01 P4 probe failed", t);
                result.put("error", t.toString());
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement e : t.getStackTrace()) sb.append(e).append('\n');
                result.put("error_stack", sb.toString());
            }
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
                server.saveEverything(true, true, true);
            } catch (Throwable t) {
                LOG.error("could not write/save", t);
            }
            Runtime.getRuntime().halt(0);
        });
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    /** Independent terrain view over the live ServerLevel (does not reuse St01PyramidStructure.GeneratorTerrain). */
    private static final class ProbeTerrain implements St01Eligibility.Terrain {
        final ServerLevel level;
        final ChunkGenerator generator;
        final RandomState random;
        final Map<Long, Integer> heights = new HashMap<>();
        final Map<Long, NoiseColumn> columns = new HashMap<>();

        ProbeTerrain(ServerLevel level) {
            this.level = level;
            this.generator = level.getChunkSource().getGenerator();
            this.random = level.getChunkSource().randomState();
        }

        static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

        @Override public int surfaceHeight(int x, int z) {
            return heights.computeIfAbsent(key(x, z), k -> generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random));
        }

        @Override public boolean isDesertCell(int qx, int qy, int qz) {
            return level.getUncachedNoiseBiome(qx, qy, qz).is(Biomes.DESERT);
        }

        @Override public boolean hasFluid(int x, int z, int minY, int maxY) {
            NoiseColumn c = columns.computeIfAbsent(key(x, z), k -> generator.getBaseColumn(x, z, level, random));
            for (int y = Math.max(minY, level.getMinY()); y <= Math.min(maxY, level.getMaxY()); y++) {
                if (!c.getBlock(y).getFluidState().isEmpty()) return true;
            }
            return false;
        }
    }

    private static Rotation rotationFor(long seed, ChunkPos pos) {
        WorldgenRandom r = new WorldgenRandom(new LegacyRandomSource(0L));
        r.setLargeFeatureSeed(seed, pos.x(), pos.z());
        return Rotation.getRandom(r);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    private static void run(MinecraftServer server, String phase, Map<String, Object> result) throws Exception {
        ServerLevel level = server.overworld();
        long seed = level.getSeed();
        boolean reload = "reload".equals(phase);
        boolean reverse = "replay".equals(phase);
        int radius = Integer.getInteger("mm.st01.p4.radius", 10240);
        result.put("seed", seed);
        result.put("radius_blocks", radius);

        // ---- 1. registration audit --------------------------------------------------------------------------------------------
        Map<String, Object> reg = new LinkedHashMap<>();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        Holder<Structure> holder = structures.get(St01Worldgen.STRUCTURE_ID).orElse(null);
        reg.put("structure_registered", holder != null);
        reg.put("structure_type_registered", BuiltInRegistries.STRUCTURE_TYPE.getKey(St01Worldgen.STRUCTURE_TYPE) != null);
        reg.put("structure_class", holder == null ? null : holder.value().getClass().getName());
        reg.put("structure_class_ok", holder != null && holder.value() instanceof St01PyramidStructure);
        reg.put("piece_types_registered", BuiltInRegistries.STRUCTURE_PIECE.getKey(St01Worldgen.ROYAL_HALL_FIXTURE_PIECE) != null
                && BuiltInRegistries.STRUCTURE_PIECE.getKey(St01Worldgen.FOOTPRINT_CORRECTION_PIECE) != null
                && BuiltInRegistries.STRUCTURE_PIECE.getKey(St01Worldgen.FLUID_SEAL_PIECE) != null);
        RandomSpreadStructurePlacement placement = null;
        int setsContaining = 0;
        List<String> otherSetsWithSameStructure = new ArrayList<>();
        for (var entry : sets.entrySet()) {
            for (var sel : entry.getValue().structures()) {
                if (holder != null && sel.structure().value() == holder.value()) {
                    setsContaining++;
                    if (entry.getValue().placement() instanceof RandomSpreadStructurePlacement rs) placement = rs;
                    otherSetsWithSameStructure.add(entry.getKey().identifier().toString());
                }
            }
        }
        reg.put("structure_sets_containing", otherSetsWithSameStructure);
        SEED_FOR_REG.set(seed);
        boolean placementOk = placement != null && placementMatches(placement, reg);
        reg.put("placement_matches_locked_values", placementOk);
        result.put("registration", reg);
        if (holder == null || placement == null) throw new IllegalStateException("ST-01 structure/structure-set not registered");
        Structure structure = holder.value();

        // ---- 2. every potential anchor in the radius: independent staged prediction + actual start -----------------------------
        int spacing = placement.spacing();
        int radiusChunks = radius / 16;
        int r0 = Math.floorDiv(-radiusChunks, spacing), r1 = Math.floorDiv(radiusChunks - 1, spacing);
        List<int[]> anchors = new ArrayList<>(); // regionX, regionZ
        for (int rx = r0; rx <= r1; rx++) for (int rz = r0; rz <= r1; rz++) anchors.add(new int[]{rx, rz});
        if (reverse) java.util.Collections.reverse(anchors);

        ProbeTerrain terrain = new ProbeTerrain(level);
        int potential = 0, centerDesert = 0, biomeQualified = 0, terrainQualified = 0, fluidSafe = 0, actualStarts = 0, mismatches = 0;
        Map<String, Integer> rejection = new TreeMap<>();
        List<Map<String, Object>> startRecords = new ArrayList<>();
        List<String> mismatchSamples = new ArrayList<>();
        long t0 = System.currentTimeMillis();
        List<ChunkPos> startChunks = new ArrayList<>();
        List<int[]> fluidRejected = new ArrayList<>(); // cx, cz, foundationY, rotationOrdinal
        List<int[]> terrainRejected = new ArrayList<>(); // same layout; height-difference / cut / skirt rejections
        for (int[] region : anchors) {
            ChunkPos pc = placement.getPotentialStructureChunk(seed, region[0] * spacing, region[1] * spacing);
            // exclude anchors outside the measured disc/square so that area normalisation is exact
            if (Math.abs(pc.getMiddleBlockX()) > radius || Math.abs(pc.getMiddleBlockZ()) > radius) continue;
            potential++;
            int cx = pc.getMinBlockX() + 8, cz = pc.getMinBlockZ() + 8;
            Rotation rot = rotationFor(seed, pc);
            if (level.getUncachedNoiseBiome(Math.floorDiv(cx, 4), Math.floorDiv(terrain.surfaceHeight(cx, cz) - 1, 4), Math.floorDiv(cz, 4)).is(Biomes.DESERT)) centerDesert++;
            St01Eligibility.Result res = St01Eligibility.evaluate(terrain, cx, cz, St01RoyalHallFixture.volume(rot));
            if (res.biomeQualified()) biomeQualified++;
            if (res.terrainQualified()) terrainQualified++;
            if (res.eligible()) fluidSafe++;
            else rejection.merge(res.verdict().name(), 1, Integer::sum);
            if (res.verdict() == St01Eligibility.Verdict.REJECT_FLUID) fluidRejected.add(new int[]{cx, cz, res.foundationY(), rot.ordinal()});
            if (res.verdict() == St01Eligibility.Verdict.REJECT_HEIGHT_DIFFERENCE || res.verdict() == St01Eligibility.Verdict.REJECT_CUT
                    || res.verdict() == St01Eligibility.Verdict.REJECT_SKIRT) terrainRejected.add(new int[]{cx, cz, res.foundationY(), rot.ordinal()});
            // drop memoised columns of this candidate to bound memory (anchors are >= 8 chunks apart)
            terrain.heights.clear();
            terrain.columns.clear();

            ChunkAccess chunk = level.getChunk(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = chunk.getStartForStructure(structure);
            boolean valid = start != null && start.isValid();
            if (valid) {
                actualStarts++;
                startChunks.add(pc);
            }
            if (valid != res.eligible()) {
                mismatches++;
                if (mismatchSamples.size() < 20) mismatchSamples.add("chunk " + pc.x() + "," + pc.z() + " predicted=" + res.verdict() + " actualValid=" + valid);
            }
        }
        double areaKm2 = Math.pow(2.0 * radius / 1000.0, 2);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("potential_anchors", potential);
        stats.put("center_only_desert_anchors", centerDesert);
        stats.put("desert_footprint_qualified_anchors", biomeQualified);
        stats.put("terrain_qualified_anchors", terrainQualified);
        stats.put("fluid_safe_anchors_predicted", fluidSafe);
        stats.put("actual_valid_starts", actualStarts);
        stats.put("prediction_vs_actual_mismatches", mismatches);
        stats.put("mismatch_samples", mismatchSamples);
        stats.put("rejection_counts_independent", rejection);
        Map<String, Long> prod = new TreeMap<>();
        for (var e : St01PyramidStructure.VERDICT_COUNTS.entrySet()) prod.put(e.getKey().name(), e.getValue().get());
        stats.put("production_verdict_counts_this_process", prod);
        stats.put("area_km2", areaKm2);
        stats.put("density_per_100km2", actualStarts * 100.0 / areaKm2);
        stats.put("scan_ms", System.currentTimeMillis() - t0);
        result.put("anchor_statistics", stats);

        // ---- 3. FULL generation + physical verification of every start ------------------------------------------------------
        List<ChunkPos> order = new ArrayList<>(startChunks);
        if (reverse) java.util.Collections.reverse(order);
        Map<String, Object> startsOut = new TreeMap<>();
        double nearest = Double.MAX_VALUE;
        int completed = 0;
        for (ChunkPos pc : order) {
            StructureStart start = level.getChunk(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            Map<String, Object> rec = verifyStart(level, terrain, structure, start, pc, seed, reverse, reload);
            startsOut.put(pc.x() + "," + pc.z(), rec);
            if (Boolean.TRUE.equals(rec.get("all_checks_pass"))) completed++;
            @SuppressWarnings("unchecked") Map<String, Object> anchor = (Map<String, Object>) rec.get("anchor");
            double dist = Math.hypot((int) anchor.get("x"), (int) anchor.get("z"));
            nearest = Math.min(nearest, dist);
        }
        result.put("starts", startsOut);
        result.put("completed_structures", completed);
        result.put("nearest_start_distance_blocks", actualStarts == 0 ? null : nearest);
        result.put("start_count", actualStarts);

        // ---- 3b. fluid-check effectiveness: generate the real chunks of fluid-rejected candidates and count actual fluid in the hall volume ----
        List<Map<String, Object>> fluidAudit = new ArrayList<>();
        int fluidAuditLimit = Integer.getInteger("mm.st01.p4.fluidaudit", 6);
        for (int[] c : fluidRejected) {
            if (fluidAudit.size() >= fluidAuditLimit) break;
            Rotation rot = Rotation.values()[c[3]];
            St01Eligibility.Volume v = St01RoyalHallFixture.volume(rot);
            int m = St01PlacementConfig.FLUID_MARGIN;
            BoundingBox vb = new BoundingBox(c[0] + v.minDx() - m, c[2] + v.minDy() - m, c[1] + v.minDz() - m, c[0] + v.maxDx() + m, c[2] + v.maxDy() + m, c[1] + v.maxDz() + m);
            for (int x = vb.minX() >> 4; x <= vb.maxX() >> 4; x++) for (int z = vb.minZ() >> 4; z <= vb.maxZ() >> 4; z++) level.getChunk(x, z, ChunkStatus.FULL);
            int water = 0, lava = 0;
            BlockPos.MutableBlockPos mp2 = new BlockPos.MutableBlockPos();
            for (int x = vb.minX(); x <= vb.maxX(); x++) for (int y = vb.minY(); y <= vb.maxY(); y++) for (int z = vb.minZ(); z <= vb.maxZ(); z++) {
                var fs = level.getBlockState(mp2.set(x, y, z)).getFluidState();
                if (fs.is(net.minecraft.tags.FluidTags.WATER)) water++; else if (fs.is(net.minecraft.tags.FluidTags.LAVA)) lava++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("anchor", c[0] + "," + c[2] + "," + c[1]);
            row.put("actual_water_blocks_in_volume_plus_margin", water);
            row.put("actual_lava_blocks_in_volume_plus_margin", lava);
            row.put("fluid_rejection_confirmed_by_real_chunks", water + lava > 0);
            addNoPartialEvidence(level, terrain, structure, row, c, vb);
            fluidAudit.add(row);
        }
        result.put("fluid_rejection_audit", fluidAudit);
        // rejected for terrain: real chunks must show no ST-01 start, no hall blocks and no flattening
        List<Map<String, Object>> terrainAudit = new ArrayList<>();
        for (int[] c : terrainRejected) {
            if (terrainAudit.size() >= Integer.getInteger("mm.st01.p4.rejectaudit", 3)) break;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("anchor", c[0] + "," + c[2] + "," + c[1]);
            St01Eligibility.Volume v = St01RoyalHallFixture.volume(Rotation.values()[c[3]]);
            BoundingBox vb = new BoundingBox(c[0] + v.minDx(), c[2] + v.minDy(), c[1] + v.minDz(), c[0] + v.maxDx(), c[2] + v.maxDy(), c[1] + v.maxDz());
            addNoPartialEvidence(level, terrain, structure, row, c, vb);
            terrainAudit.add(row);
        }
        result.put("terrain_rejection_no_partial_audit", terrainAudit);

        // ---- 4. controlled-terrain correction fixture (scan phase only) ---------------------------------------------------
        if (!reload && !reverse) result.put("correction_fixture", correctionFixture(level));
        result.put("elapsed_ms", System.currentTimeMillis() - t0);
    }

    private static final ThreadLocal<Long> SEED_FOR_REG = ThreadLocal.withInitial(() -> 0L);

    /** A rejected candidate must leave nothing: no start in its chunk, no hall marker blocks in its volume, no flattened footprint. */
    private static void addNoPartialEvidence(ServerLevel level, ProbeTerrain terrain, Structure structure, Map<String, Object> row, int[] c, BoundingBox volume) {
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        for (int x = (c[0] - half) >> 4; x <= (c[0] + half) >> 4; x++) for (int z = (c[1] - half) >> 4; z <= (c[1] + half) >> 4; z++) level.getChunk(x, z, ChunkStatus.FULL);
        ChunkPos pc = new ChunkPos(c[0] >> 4, c[1] >> 4);
        StructureStart st = level.getChunk(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
        int gold = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int x = volume.minX(); x <= volume.maxX(); x++) for (int y = volume.minY(); y <= volume.maxY(); y++) for (int z = volume.minZ(); z <= volume.maxZ(); z++) {
            var b = level.getBlockState(mp.set(x, y, z));
            if (b.is(Blocks.GOLD_BLOCK) || b.is(Blocks.CHISELED_SANDSTONE) || b.is(Blocks.SMOOTH_RED_SANDSTONE)) gold++;
        }
        int stray = 0, flatAtMedian = 0, columns = 0;
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int actual = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, c[0] + dx, c[1] + dz);
            if (Math.abs(actual - terrain.surfaceHeight(c[0] + dx, c[1] + dz)) > 3) stray++;
            if (actual == c[2]) flatAtMedian++;
            columns++;
        }
        terrain.heights.clear();
        row.put("structure_start_in_anchor_chunk", st != null && st.isValid());
        row.put("hall_marker_blocks_in_volume", gold);
        row.put("footprint_columns_differing_from_noise_height_by_more_than_3", stray);
        row.put("footprint_columns_exactly_at_would_be_foundation_level", flatAtMedian + "/" + columns);
        row.put("no_partial_generation", !(st != null && st.isValid()) && gold == 0);
    }

    private static boolean placementMatches(RandomSpreadStructurePlacement p, Map<String, Object> reg) {
        boolean ok = p.spacing() == St01PlacementConfig.SPACING_CHUNKS && p.separation() == St01PlacementConfig.SEPARATION_CHUNKS
                && p.spreadType().getSerializedName().equals(St01PlacementConfig.SPREAD_TYPE);
        reg.put("spacing", p.spacing());
        reg.put("separation", p.separation());
        reg.put("spread_type", p.spreadType().getSerializedName());
        try {
            var f = net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement.class.getDeclaredField("salt");
            f.setAccessible(true);
            int salt = f.getInt(p);
            reg.put("salt", salt);
            ok &= salt == St01PlacementConfig.SALT;
            var fr = net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement.class.getDeclaredField("frequency");
            fr.setAccessible(true);
            reg.put("frequency", fr.getFloat(p));
            ok &= fr.getFloat(p) == 1.0f;
        } catch (Throwable t) {
            reg.put("salt_error", t.toString());
            ok = false;
        }
        // behavioural proof of salt/spacing/separation/spread: independent re-derivation of the potential chunk for 400 regions
        int behaviourMismatch = 0;
        for (int rx = -10; rx < 10; rx++) {
            for (int rz = -10; rz < 10; rz++) {
                WorldgenRandom r = new WorldgenRandom(new LegacyRandomSource(0L));
                r.setLargeFeatureWithSalt(SEED_FOR_REG.get(), rx, rz, St01PlacementConfig.SALT);
                int k = St01PlacementConfig.SPACING_CHUNKS - St01PlacementConfig.SEPARATION_CHUNKS;
                net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType t = net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.TRIANGULAR;
                int dx = t.evaluate(r, k), dz = t.evaluate(r, k);
                ChunkPos expected = new ChunkPos(rx * St01PlacementConfig.SPACING_CHUNKS + dx, rz * St01PlacementConfig.SPACING_CHUNKS + dz);
                if (!expected.equals(p.getPotentialStructureChunk(SEED_FOR_REG.get(), rx * St01PlacementConfig.SPACING_CHUNKS, rz * St01PlacementConfig.SPACING_CHUNKS))) behaviourMismatch++;
            }
        }
        reg.put("potential_chunk_rederivation_mismatches_of_400", behaviourMismatch);
        ok &= behaviourMismatch == 0;
        return ok;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    private static Map<String, Object> verifyStart(ServerLevel level, ProbeTerrain terrain, Structure structure, StructureStart start, ChunkPos pc,
            long seed, boolean reverse, boolean reload) throws Exception {
        Map<String, Object> rec = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        int cx = pc.getMinBlockX() + 8, cz = pc.getMinBlockZ() + 8;
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;

        St01FootprintCorrectionPiece correction = null;
        St01RoyalHallFixture.Piece hall = null;
        List<String> pieceTypes = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            pieceTypes.add(piece.getClass().getSimpleName());
            if (piece instanceof St01FootprintCorrectionPiece c) correction = c;
            if (piece instanceof St01RoyalHallFixture.Piece h) hall = h;
        }
        rec.put("piece_types", pieceTypes);
        if (correction == null || hall == null) throw new IllegalStateException("start lacks expected pieces: " + pieceTypes);
        BoundingBox corrBox = correction.getBoundingBox();
        int yf = corrBox.minY() + St01PlacementConfig.MAX_SKIRT;
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("x", cx); a.put("y", yf); a.put("z", cz);
        rec.put("anchor", a);
        Rotation rot = hall.getRotation();
        rec.put("rotation", rot.name());
        Rotation expectedRot = rotationFor(seed, pc);
        check(failures, "rotation_matches_deterministic_derivation", rot == expectedRot, rot + " vs " + expectedRot);

        // exhaustive pre-correction heights (independent of the 4-block sampling grid used by the placement rule)
        int minH = Integer.MAX_VALUE, maxH = Integer.MIN_VALUE;
        int[][] base = new int[2 * half + 1][2 * half + 1];
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int h = terrain.surfaceHeight(cx + dx, cz + dz);
                base[dx + half][dz + half] = h;
                minH = Math.min(minH, h);
                maxH = Math.max(maxH, h);
            }
        }
        Map<String, Object> ex = new LinkedHashMap<>();
        ex.put("exhaustive_min_height", minH);
        ex.put("exhaustive_max_height", maxH);
        ex.put("exhaustive_height_difference", maxH - minH);
        ex.put("exhaustive_cut", Math.max(0, maxH - yf));
        ex.put("exhaustive_skirt", Math.max(0, yf - minH));
        ex.put("within_height_limit", maxH - minH <= St01PlacementConfig.MAX_HEIGHT_DIFFERENCE);
        ex.put("within_cut_limit", maxH - yf <= St01PlacementConfig.MAX_CUT);
        ex.put("within_skirt_limit", yf - minH <= St01PlacementConfig.MAX_SKIRT);
        rec.put("terrain_exhaustive_audit", ex);
        terrain.heights.clear();

        // force FULL generation of every chunk the footprint and hall touch
        BoundingBox all = start.getBoundingBox();
        int minCx = all.minX() >> 4, maxCx = all.maxX() >> 4, minCz = all.minZ() >> 4, maxCz = all.maxZ() >> 4;
        List<int[]> chunks = new ArrayList<>();
        for (int x = minCx; x <= maxCx; x++) for (int z = minCz; z <= maxCz; z++) chunks.add(new int[]{x, z});
        if (reverse) java.util.Collections.reverse(chunks);
        long g0 = System.currentTimeMillis();
        for (int[] c : chunks) level.getChunk(c[0], c[1], ChunkStatus.FULL);
        rec.put("full_generation_chunks", chunks.size());
        rec.put("full_generation_ms", System.currentTimeMillis() - g0);

        // other structures whose bounding box intersects the footprint volume (vanilla structures are not coordinated with ST-01)
        BoundingBox footprintVolume = new BoundingBox(cx - half, yf - 24, cz - half, cx + half, yf + 12, cz + half);
        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        List<String> intersecting = new ArrayList<>();
        for (int x = (cx - half - 128) >> 4; x <= (cx + half + 128) >> 4; x++) {
            for (int z = (cz - half - 128) >> 4; z <= (cz + half + 128) >> 4; z++) {
                for (var e : level.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS).getAllStarts().entrySet()) {
                    if (e.getKey() == structure || !e.getValue().isValid()) continue;
                    if (e.getValue().getBoundingBox().intersects(footprintVolume)) {
                        var id = structureRegistry.getKey(e.getKey());
                        String name = id == null ? "?" : id.toString();
                        if (!intersecting.contains(name)) intersecting.add(name);
                    }
                }
            }
        }
        rec.put("intersecting_other_structures", intersecting);
        boolean overlapsOther = !intersecting.isEmpty();

        // (a) real biome of every footprint column
        int nonDesert = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (!level.getBiome(mp.set(cx + dx, yf - 1, cz + dz)).is(Biomes.DESERT)) nonDesert++;
            }
        }
        rec.put("footprint_columns", (2 * half + 1) * (2 * half + 1));
        rec.put("footprint_non_desert_columns", nonDesert);
        check(failures, "full_footprint_desert", nonDesert == 0, "non-desert columns=" + nonDesert);

        // (b) terrain correction result: top surface at yf-1 across the footprint, nothing left above, no fluid/air gaps in the skirt band
        // (the hall's own interior is excluded: it is intentionally hollow and is overwritten after the correction)
        BoundingBox hallBox = hall.getBoundingBox();
        int badTop = 0, remainingTerrainAbove = 0, hollowBelow = 0;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int x = cx + dx, z = cz + dz;
                BlockState top = level.getBlockState(mp.set(x, yf - 1, z));
                if (!hallBox.isInside(x, yf - 1, z) && (top.isAir() || !top.getFluidState().isEmpty())) badTop++;
                for (int y = yf; y <= yf + St01PlacementConfig.MAX_CUT + 1; y++) {
                    if (isTerrain(level.getBlockState(mp.set(x, y, z)))) { remainingTerrainAbove++; break; }
                }
                for (int y = yf - 1; y >= yf - St01PlacementConfig.MAX_SKIRT; y--) {
                    if (hallBox.isInside(x, y, z)) continue;
                    BlockState s = level.getBlockState(mp.set(x, y, z));
                    if (s.isAir() || !s.getFluidState().isEmpty()) { hollowBelow++; break; }
                }
            }
        }
        rec.put("columns_without_solid_top_at_yf_minus_1", badTop);
        rec.put("columns_with_terrain_left_above_yf", remainingTerrainAbove);
        rec.put("columns_with_air_or_fluid_in_skirt_band", hollowBelow);
        check(failures, "correction_flat_top", badTop == 0, "badTop=" + badTop);
        check(failures, "correction_cut_complete", remainingTerrainAbove == 0 || overlapsOther, "above=" + remainingTerrainAbove + " (excused only when another structure overlaps)");
        check(failures, "correction_skirt_solid", hollowBelow == 0, "hollow=" + hollowBelow);

        // (c) outside the footprint: edge step height and stray modification proxy (the ring chunks must be generated first)
        for (int x = (cx - half - 4) >> 4; x <= (cx + half + 4) >> 4; x++) for (int z = (cz - half - 4) >> 4; z <= (cz + half + 4) >> 4; z++) level.getChunk(x, z, ChunkStatus.FULL);
        int maxStep = 0; long stepSum = 0; int stepN = 0; int strayModified = 0, ringN = 0;
        for (int d = -half; d <= half; d++) {
            for (int side = 0; side < 4; side++) {
                int ox = side == 0 ? cx - half - 1 : side == 1 ? cx + half + 1 : cx + d;
                int oz = side == 2 ? cz - half - 1 : side == 3 ? cz + half + 1 : cz + d;
                int actual = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ox, oz);
                int baseH = terrain.surfaceHeight(ox, oz);
                int step = Math.abs(actual - yf);
                maxStep = Math.max(maxStep, step); stepSum += step; stepN++;
                if (Math.abs(actual - baseH) > 3) strayModified++;
                ringN++;
            }
        }
        terrain.heights.clear();
        rec.put("edge_max_step_to_foundation", maxStep);
        rec.put("edge_mean_step_to_foundation", stepN == 0 ? 0 : (double) stepSum / stepN);
        rec.put("ring_columns_sampled", ringN);
        rec.put("ring_columns_differing_from_noise_height_by_more_than_3", strayModified);

        var cs = St01FootprintCorrectionPiece.STATS.get(cx + "," + cz);
        Map<String, Object> corr = new LinkedHashMap<>();
        if (cs != null) {
            corr.put("columns_processed", cs.columnsProcessed.get());
            corr.put("actual_max_cut_vs_generator_base_height", cs.maxCut.get());
            corr.put("actual_max_fill_vs_generator_base_height", cs.maxFill.get());
            corr.put("columns_cut", cs.columnsCut.get());
            corr.put("columns_filled", cs.columnsFilled.get());
            corr.put("columns_sealed_air_or_fluid_in_band", cs.columnsSealed.get());
            corr.put("columns_fill_deeper_than_skirt_limit", cs.columnsBelowSkirtBand.get());
            check(failures, "actual_cut_within_10", cs.maxCut.get() <= St01PlacementConfig.MAX_CUT, "maxCut=" + cs.maxCut.get());
            check(failures, "actual_skirt_within_8", cs.maxFill.get() <= St01PlacementConfig.MAX_SKIRT, "maxFill=" + cs.maxFill.get());
            check(failures, "all_footprint_columns_processed_exactly_once", cs.columnsProcessed.get() == (2 * half + 1) * (2 * half + 1), "processed=" + cs.columnsProcessed.get());
        } else corr.put("note", "correction stats unavailable in this process (reload phase)");
        rec.put("correction_actual", corr);

        // (d) the hall: exact block-type census vs committed NBT, chest pair at rotated approved coordinates, fluid, cavity exposure
        BoundingBox hb = hall.getBoundingBox();
        Map<String, Integer> worldCensus = new TreeMap<>();
        for (int x = hb.minX(); x <= hb.maxX(); x++)
            for (int y = hb.minY(); y <= hb.maxY(); y++)
                for (int z = hb.minZ(); z <= hb.maxZ(); z++)
                    worldCensus.merge(BuiltInRegistries.BLOCK.getKey(level.getBlockState(mp.set(x, y, z)).getBlock()).toString(), 1, Integer::sum);
        rec.put("hall_box", box(hb));
        rec.put("hall_census_in_world", worldCensus);
        // Voxel comparison against the committed template (block ids; rotation applied with the same transform as placement). Differences are
        // allowed only where vanilla decoration/features (ores, glow lichen, fossils ...) or another vanilla structure overwrote a template block.
        Map<String, Integer> mismatchPairs = new TreeMap<>();
        int voxelMismatches = 0, unexplained = 0;
        for (var tb : templateBlocks(level)) {
            BlockPos world = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(tb.getKey(), net.minecraft.world.level.block.Mirror.NONE,
                    rot, new BlockPos(40, 24, -2)).offset(hall.templatePosition());
            String want = BuiltInRegistries.BLOCK.getKey(tb.getValue()).getPath();
            String got = BuiltInRegistries.BLOCK.getKey(level.getBlockState(world).getBlock()).getPath();
            if (!want.equals(got)) {
                voxelMismatches++;
                mismatchPairs.merge(want + "->" + got, 1, Integer::sum);
                if (!overlapsOther && !overlayBlock(got)) unexplained++;
            }
        }
        rec.put("hall_voxel_mismatches_vs_template", voxelMismatches);
        rec.put("hall_voxel_mismatch_pairs", mismatchPairs);
        rec.put("hall_voxel_mismatches_unexplained", unexplained);
        check(failures, "hall_voxels_match_template_except_vanilla_overlay", unexplained == 0, "pairs=" + mismatchPairs);
        Map<String, Integer> expectedCensus = nbtCensus(level);
        rec.put("hall_template_masked_sha256", hallMaskedHash(level, hb, hall, rot));
        rec.put("surface_profile_sha256", surfaceProfileHash(level, cx, yf, cz));

        BlockPos left = pyramidWorld(cx, yf, cz, St01RoyalChestPlacement.REFERENCE_ANCHOR, rot);
        BlockPos right = pyramidWorld(cx, yf, cz, St01RoyalChestPlacement.REFERENCE_RIGHT, rot);
        BlockPos viaP8B = St01RoyalChestPlacement.worldPos(St01RoyalChestPlacement.REFERENCE_ANCHOR, hall.templatePosition(), hall.placeSettings());
        BlockState ls = level.getBlockState(left), rs = level.getBlockState(right);
        boolean chests = ls.is(Blocks.CHEST) && rs.is(Blocks.CHEST) && ls.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                && rs.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE;
        rec.put("primary_chest_left", left.toString());
        rec.put("primary_chest_right", right.toString());
        check(failures, "primary_chest_pair_at_rotated_reference_coordinates", chests, ls + " / " + rs);
        check(failures, "p8b_transform_agrees_with_pyramid_frame_rotation", left.equals(viaP8B), left + " vs " + viaP8B);
        BlockPos spawn = pyramidWorld(cx, yf, cz, St01RoyalChestPlacement.REFERENCE_SPAWN, rot);
        rec.put("guardian_spawn_reference", spawn.toString());
        check(failures, "guardian_spawn_volume_is_air", level.getBlockState(spawn).isAir() && level.getBlockState(spawn.above()).isAir(), spawn.toString());

        int fluidInside = 0, fluidMargin = 0, exposedFaces = 0;
        int m = St01PlacementConfig.FLUID_MARGIN;
        for (int x = hb.minX() - m; x <= hb.maxX() + m; x++)
            for (int y = hb.minY() - m; y <= hb.maxY() + m; y++)
                for (int z = hb.minZ() - m; z <= hb.maxZ() + m; z++) {
                    BlockState s = level.getBlockState(mp.set(x, y, z));
                    boolean inside = hb.isInside(x, y, z);
                    if (!s.getFluidState().isEmpty()) { if (inside) fluidInside++; else fluidMargin++; }
                    else if (!inside && s.isAir()) exposedFaces++; // air within the 1-block shell around the box = a cavity touching it
                }
        int fluidTouchingHallAir = 0;
        Map<String, Integer> shellSides = new TreeMap<>();
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        BlockPos.MutableBlockPos np = new BlockPos.MutableBlockPos();
        for (int x = hb.minX() - m; x <= hb.maxX() + m; x++)
            for (int y = hb.minY() - m; y <= hb.maxY() + m; y++)
                for (int z = hb.minZ() - m; z <= hb.maxZ() + m; z++) {
                    if (hb.isInside(x, y, z) || level.getBlockState(mp.set(x, y, z)).getFluidState().isEmpty()) continue;
                    shellSides.merge(x < hb.minX() ? "-x" : x > hb.maxX() ? "+x" : y < hb.minY() ? "-y" : y > hb.maxY() ? "+y" : z < hb.minZ() ? "-z" : "+z", 1, Integer::sum);
                    for (int[] d : dirs) {
                        np.set(x + d[0], y + d[1], z + d[2]);
                        if (hb.isInside(np.getX(), np.getY(), np.getZ()) && !level.getBlockState(np).canOcclude()) { fluidTouchingHallAir++; break; }
                    }
                }
        rec.put("hall_shell_fluid_by_face", shellSides);
        rec.put("hall_shell_fluid_blocks_adjacent_to_non_solid_hall_blocks", fluidTouchingHallAir);
        rec.put("hall_fluid_blocks_inside_box", fluidInside);
        rec.put("hall_fluid_blocks_in_margin_shell", fluidMargin);
        rec.put("air_blocks_in_margin_shell_cavity_exposure_audit", exposedFaces);
        check(failures, "hall_volume_fluid_free", fluidInside == 0 && fluidMargin == 0, "inside=" + fluidInside + " margin=" + fluidMargin);

        // (e) state hashes for deterministic replay / reload comparison
        rec.put("hall_state_sha256", hashBox(level, hb, false));
        BoundingBox fp = new BoundingBox(cx - half, yf - 14, cz - half, cx + half, yf + 14, cz + half);
        rec.put("footprint_state_sha256", hashBox(level, fp, false));
        rec.put("footprint_terrain_sha256", hashBox(level, fp, true));
        // control: an equally sized window 256 blocks east of the anchor with NO ST-01 structure; any scan/replay difference there is vanilla feature ordering
        int ccx = cx + 256;
        for (int x = (ccx - half - 16) >> 4; x <= (ccx + half + 16) >> 4; x++) for (int z = (cz - half - 16) >> 4; z <= (cz + half + 16) >> 4; z++) level.getChunk(x, z, ChunkStatus.FULL);
        rec.put("control_window_center", ccx + "," + yf + "," + cz);
        rec.put("control_window_state_sha256", hashBox(level, new BoundingBox(ccx - half, yf - 14, cz - half, ccx + half, yf + 14, cz + half), false));
        String dumpDir = System.getProperty("mm.st01.p4.dump");
        if (dumpDir != null) {
            dumpFootprint(level, cx, yf, cz, Path.of(dumpDir, pc.x() + "_" + pc.z() + ".txt"));
            dumpFootprint(level, ccx, yf, cz, Path.of(dumpDir, "control_" + pc.x() + "_" + pc.z() + ".txt"));
        }
        rec.put("failures", failures);
        rec.put("all_checks_pass", failures.isEmpty());
        return rec;
    }

    private static void dumpFootprint(ServerLevel level, int cx, int yf, int cz, Path out) throws Exception {
        Files.createDirectories(out.getParent());
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        Map<String, Integer> ids = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int x = cx - half; x <= cx + half; x++) for (int z = cz - half; z <= cz + half; z++) {
            sb.append(x - cx).append(' ').append(z - cz).append(':');
            for (int y = yf - 14; y <= yf + 14; y++) sb.append(ids.computeIfAbsent(level.getBlockState(mp.set(x, y, z)).toString(), k -> ids.size())).append(',');
            sb.append('\n');
        }
        StringBuilder legend = new StringBuilder();
        ids.forEach((k, v) -> legend.append(v).append('=').append(k).append('\n'));
        Files.writeString(out, legend + "---\n" + sb, StandardCharsets.UTF_8);
    }

    private static void check(List<String> failures, String id, boolean ok, String evidence) {
        if (!ok) failures.add(id + ": " + evidence);
    }

    private static boolean isTerrain(BlockState s) {
        return s.is(Blocks.SAND) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SAND) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.STONE)
                || s.is(Blocks.DIRT) || s.is(Blocks.GRAVEL) || s.is(Blocks.TERRACOTTA) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.GRASS_BLOCK);
    }

    private static Map<String, Object> box(BoundingBox b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("min", b.minX() + "," + b.minY() + "," + b.minZ());
        m.put("max", b.maxX() + "," + b.maxY() + "," + b.maxZ());
        return m;
    }

    /** Pyramid reference frame (P1): world = (cx + rot(X), groundY + Y, cz + rot(Z)). Independent of StructureTemplate placement. */
    static BlockPos pyramidWorld(int cx, int yf, int cz, BlockPos reference, Rotation rot) {
        int x = reference.getX(), z = reference.getZ(), rx, rz;
        switch (rot) {
            case NONE -> { rx = x; rz = z; }
            case CLOCKWISE_90 -> { rx = -z; rz = x; }
            case CLOCKWISE_180 -> { rx = -x; rz = -z; }
            default -> { rx = z; rz = -x; }
        }
        return new BlockPos(cx + rx, yf + reference.getY(), cz + rz);
    }

    private static final java.util.Set<String> UNDERGROUND_FEATURE_BLOCKS = java.util.Set.of("stone", "andesite", "diorite", "granite", "tuff", "gravel", "dirt",
            "coarse_dirt", "coal_ore", "iron_ore", "copper_ore", "gold_ore", "redstone_ore", "lapis_ore", "diamond_ore", "emerald_ore", "clay", "infested_stone", "bone_block");

    private static String bid(BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(); }

    /** Non-collidable growth (grass, dead bush, cactus ...) placed by vanilla vegetation features whose order relative to the correction is not fixed. */
    private static boolean isVegetation(ServerLevel level, BlockPos pos, BlockState s) {
        return s.isAir() || s.is(Blocks.CACTUS) || s.is(Blocks.CACTUS_FLOWER) || s.is(net.minecraft.tags.BlockTags.LOGS) || s.is(net.minecraft.tags.BlockTags.LEAVES)
                || s.getCollisionShape(level, pos).isEmpty() && s.getFluidState().isEmpty();
    }

    private static String hashBox(ServerLevel level, BoundingBox b, boolean canonical) throws Exception {
        return hashBox(level, b, canonical, null);
    }

    /**
     * SHA-256 over the block states of a box. Raw: exact states. Canonical (terrain): vegetation/decoration maps to "-" and vanilla underground
     * feature blocks (stone family, ores, gravel, dirt) map to "S", so only structure-owned blocks (sand, sandstone, air, hall blocks) count.
     * With {@code allowed}: any block outside that name set maps to "-" (hall template palette check).
     */
    private static String hashBox(ServerLevel level, BoundingBox b, boolean canonical, java.util.Set<String> allowed) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        StringBuilder line = new StringBuilder();
        for (int x = b.minX(); x <= b.maxX(); x++)
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                line.setLength(0);
                for (int y = b.minY(); y <= b.maxY(); y++) {
                    BlockState st = level.getBlockState(mp.set(x, y, z));
                    String out;
                    if (allowed != null) out = allowed.contains(BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString()) ? st.toString() : "-";
                    else if (!canonical) out = st.toString();
                    else if (isVegetation(level, mp, st)) out = "-";
                    else if (UNDERGROUND_FEATURE_BLOCKS.contains(bid(st)) || bid(st).endsWith("_ore")) out = "S";
                    else out = st.toString();
                    line.append(out).append(';');
                }
                md.update(line.toString().getBytes(StandardCharsets.UTF_8));
            }
        StringBuilder sb = new StringBuilder();
        for (byte v : md.digest()) sb.append(String.format("%02x", v));
        return sb.toString();
    }

    /**
     * Hash of the hall volume in which every vanilla overlay block (decoration, ores, fossils ...) is replaced by the block the template prescribes
     * there; the hash therefore captures exactly the structure-owned content and is independent of vanilla overlay order.
     */
    private static String hallMaskedHash(ServerLevel level, BoundingBox hb, St01RoyalHallFixture.Piece hall, Rotation rot) throws Exception {
        Map<BlockPos, Block> expected = new HashMap<>();
        for (var tb : templateBlocks(level)) {
            expected.put(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(tb.getKey(), net.minecraft.world.level.block.Mirror.NONE,
                    rot, new BlockPos(40, 24, -2)).offset(hall.templatePosition()), tb.getValue());
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        StringBuilder line = new StringBuilder();
        for (int x = hb.minX(); x <= hb.maxX(); x++)
            for (int z = hb.minZ(); z <= hb.maxZ(); z++) {
                line.setLength(0);
                for (int y = hb.minY(); y <= hb.maxY(); y++) {
                    BlockState st = level.getBlockState(mp.set(x, y, z));
                    Block want = expected.get(mp.immutable());
                    line.append(want != null && overlayBlock(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath()) ? want.defaultBlockState().toString() : st.toString()).append(';');
                }
                md.update(line.toString().getBytes(StandardCharsets.UTF_8));
            }
        StringBuilder sb = new StringBuilder();
        for (byte v : md.digest()) sb.append(String.format("%02x", v));
        return sb.toString();
    }

    /** Per footprint column: highest occluding, non-vegetation block within the correction window = the corrected ground profile. */
    private static String surfaceProfileHash(ServerLevel level, int cx, int yf, int cz) throws Exception {
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        StringBuilder sb = new StringBuilder();
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                int top = Integer.MIN_VALUE;
                for (int y = yf + St01PlacementConfig.MAX_CUT + 2; y >= yf - 14; y--) {
                    BlockState st = level.getBlockState(mp.set(x, y, z));
                    if (st.isCollisionShapeFullBlock(level, mp) && !st.is(net.minecraft.tags.BlockTags.LOGS) && !st.is(net.minecraft.tags.BlockTags.LEAVES)) { top = y; break; }
                }
                sb.append(top).append(',');
            }
            sb.append('\n');
        }
        md.update(sb.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte v : md.digest()) out.append(String.format("%02x", v));
        return out.toString();
    }

    private static final java.util.Set<String> OVERLAY = java.util.Set.of("glow_lichen", "bone_block", "cave_air", "air", "moss_block", "moss_carpet", "stone", "andesite",
            "diorite", "granite", "tuff", "gravel", "dirt", "clay", "sculk_vein", "cobweb", "spore_blossom", "pointed_dripstone", "dripstone_block");

    private static boolean overlayBlock(String path) { return OVERLAY.contains(path) || path.endsWith("_ore"); }

    /** Template voxels (local position, block) read from the committed NBT through the registry-aware palette. */
    private static List<Map.Entry<BlockPos, Block>> templateBlocks(ServerLevel level) throws Exception {
        CompoundTag root;
        try (InputStream in = St01Phase4Probe.class.getClassLoader().getResourceAsStream("data/moreminecrafts/structure/pyramid/royal_hall.nbt")) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        var lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        ListTag palette = root.getListOrEmpty("palette");
        List<Block> blocksByIndex = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) blocksByIndex.add(net.minecraft.nbt.NbtUtils.readBlockState(lookup, palette.getCompoundOrEmpty(i)).getBlock());
        ListTag entries = root.getListOrEmpty("blocks");
        List<Map.Entry<BlockPos, Block>> out = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompoundOrEmpty(i);
            ListTag pos = e.getListOrEmpty("pos");
            out.add(Map.entry(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), blocksByIndex.get(e.getIntOr("state", 0))));
        }
        return out;
    }

    /** Block-id census of the committed NBT (rotation invariant), counting every voxel including air. */
    private static Map<String, Integer> nbtCensus(ServerLevel level) throws Exception {
        CompoundTag root;
        try (InputStream in = St01Phase4Probe.class.getClassLoader().getResourceAsStream("data/moreminecrafts/structure/pyramid/royal_hall.nbt")) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        var blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        ListTag palette = root.getListOrEmpty("palette");
        List<String> names = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            names.add(BuiltInRegistries.BLOCK.getKey(net.minecraft.nbt.NbtUtils.readBlockState(blockLookup, palette.getCompoundOrEmpty(i)).getBlock()).toString());
        }
        ListTag blocks = root.getListOrEmpty("blocks");
        Map<String, Integer> census = new TreeMap<>();
        for (int i = 0; i < blocks.size(); i++) census.merge(names.get(blocks.getCompoundOrEmpty(i).getIntOr("state", 0)), 1, Integer::sum);
        return census;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    /**
     * Controlled-terrain fixture: builds a footprint whose columns sit at exactly +10 (cut limit) and -8 (skirt limit) around the foundation
     * level, runs the real correction piece chunk by chunk (in a scrambled order), and verifies exact results and zero change outside
     * the footprint and outside the approved vertical band.
     */
    private static Map<String, Object> correctionFixture(ServerLevel level) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        int cx = 5000 + 8, cz = 5000 + 8, yf = 70; // far from every other probe activity; arbitrary biome (fixture does not depend on biome)
        int minCx = (cx - half - 16) >> 4, maxCx = (cx + half + 16) >> 4, minCz = (cz - half - 16) >> 4, maxCz = (cz + half + 16) >> 4;
        for (int x = minCx; x <= maxCx; x++) for (int z = minCz; z <= maxCz; z++) level.getChunk(x, z, ChunkStatus.FULL);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int windowMin = yf - 20, windowMax = yf + 20;
        // synthetic terrain: column surface = yf + off(x,z), sand over sandstone; off in [-8, +10], extremes both present
        Block sand = Blocks.SAND, stone = Blocks.SANDSTONE, air = Blocks.AIR;
        int wideMin = cx - half - 16, wideMax = cx + half + 16, wideMinZ = cz - half - 16, wideMaxZ = cz + half + 16;
        for (int x = wideMin; x <= wideMax; x++) {
            for (int z = wideMinZ; z <= wideMaxZ; z++) {
                int off = fixtureOffset(x - cx, z - cz, half);
                int surface = yf + off; // first free Y
                for (int y = windowMin; y <= windowMax; y++) {
                    Block b = y >= surface ? air : (y >= surface - 4 ? sand : stone);
                    level.setBlock(mp.set(x, y, z), b.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        // hash of everything outside the footprint inside the window, before correction
        String outsideBefore = hashOutside(level, cx, cz, half, wideMin, wideMax, wideMinZ, wideMaxZ, windowMin, windowMax);
        int maxOff = Integer.MIN_VALUE, minOff = Integer.MAX_VALUE;
        for (int x = -half; x <= half; x++) for (int z = -half; z <= half; z++) { int o = fixtureOffset(x, z, half); maxOff = Math.max(maxOff, o); minOff = Math.min(minOff, o); }
        out.put("fixture_max_offset_cut", maxOff);
        out.put("fixture_min_offset_skirt", -minOff);
        List<int[]> chunks = new ArrayList<>();
        for (int x = (cx - half) >> 4; x <= (cx + half) >> 4; x++) for (int z = (cz - half) >> 4; z <= (cz + half) >> 4; z++) chunks.add(new int[]{x, z});
        java.util.Collections.shuffle(chunks, new java.util.Random(91401));
        for (int[] c : chunks) {
            BoundingBox chunkBox = new BoundingBox(c[0] << 4, windowMin - 8, c[1] << 4, (c[0] << 4) + 15, windowMax + 8, (c[1] << 4) + 15);
            St01FootprintCorrectionPiece.correct(level, chunkBox, cx, yf, cz, (x, z) -> yf + fixtureOffset(x - cx, z - cz, half));
        }
        int wrongTop = 0, wrongAbove = 0, wrongBelow = 0, wrongFill = 0;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int x = cx + dx, z = cz + dz;
                int off = fixtureOffset(dx, dz, half);
                if (level.getBlockState(mp.set(x, yf - 1, z)).isAir()) wrongTop++;
                for (int y = yf; y <= windowMax; y++) if (!level.getBlockState(mp.set(x, y, z)).isAir()) { wrongAbove++; break; }
                for (int y = yf - 1; y >= windowMin; y--) if (level.getBlockState(mp.set(x, y, z)).isAir()) { wrongBelow++; break; }
                if (off < 0) { // filled band [yf+off, yf-1]: sand for the top 4 layers, sandstone below
                    for (int y = yf + off; y < yf; y++) {
                        Block want = y >= yf - 4 ? Blocks.SAND : Blocks.SANDSTONE;
                        if (!level.getBlockState(mp.set(x, y, z)).is(want)) { wrongFill++; break; }
                    }
                }
            }
        }
        String outsideAfter = hashOutside(level, cx, cz, half, wideMin, wideMax, wideMinZ, wideMaxZ, windowMin, windowMax);
        out.put("columns_wrong_fill_material", wrongFill);
        out.put("columns_wrong_top", wrongTop);
        out.put("columns_with_blocks_left_above_foundation", wrongAbove);
        out.put("columns_with_air_below_foundation_in_window", wrongBelow);
        out.put("outside_footprint_unchanged", outsideBefore.equals(outsideAfter));
        check(failures, "fixture_top", wrongTop == 0, "wrongTop=" + wrongTop);
        check(failures, "fixture_cut", wrongAbove == 0, "above=" + wrongAbove);
        check(failures, "fixture_fill_material", wrongFill == 0, "wrongFill=" + wrongFill);
        check(failures, "fixture_fill", wrongBelow == 0, "below=" + wrongBelow);
        check(failures, "fixture_outside_unchanged", outsideBefore.equals(outsideAfter), "outside hash changed");
        out.put("failures", failures);
        out.put("pass", failures.isEmpty());
        return out;
    }

    /** Synthetic offsets: +10 plateau patch, -8 pit, a ramp, and flat elsewhere. Max +10 (cut limit), min -8 (skirt limit). */
    private static int fixtureOffset(int dx, int dz, int half) {
        if (dx >= -30 && dx <= -20 && dz >= -30 && dz <= -20) return 10;
        if (dx >= 20 && dx <= 30 && dz >= 20 && dz <= 30) return -8;
        if (dx >= -10 && dx <= 10) return Math.floorDiv(dz, 10); // ramp strip -4..+4
        return Math.floorMod(dx * 7 + dz * 3, 5) - 2;            // gentle noise -2..+2
    }

    private static String hashOutside(ServerLevel level, int cx, int cz, int half, int x0, int x1, int z0, int z1, int y0, int y1) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int x = x0; x <= x1; x++)
                for (int z = z0; z <= z1; z++) {
                    if (Math.abs(x - cx) <= half && Math.abs(z - cz) <= half) continue;
                    StringBuilder sb = new StringBuilder();
                    for (int y = y0; y <= y1; y++) sb.append(level.getBlockState(mp.set(x, y, z))).append(';');
                    md.update(sb.toString().getBytes(StandardCharsets.UTF_8));
                }
            StringBuilder sb = new StringBuilder();
            for (byte v : md.digest()) sb.append(String.format("%02x", v));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
