package dev.itzgamesgd.moreminecrafts.spidercolony;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Connects Spider Colony structure &amp; hanging cocoon generation to Overworld chunk generation.
 *
 * <p>Chunk loads enqueue aligned 8×8-chunk probe windows without blocking {@code CHUNK_LOAD};
 * actual voxel evaluation and colony placement run on {@code END_SERVER_TICK} once the window
 * and its 1-chunk structure halo are at {@link ChunkStatus#FULL}.
 */
public final class SpiderColonyWorldgen {
    private record PendingWindow(ServerLevel level, int wx, int wz) {}

    private static final Set<PendingWindow> PENDING = new LinkedHashSet<>();
    private static final int MAX_PENDING = 512;
    private static boolean initialized;

    private SpiderColonyWorldgen() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) -> {
            if (level.dimension() != Level.OVERWORLD) return;
            if (!newlyGenerated) return;
            ChunkPos cp = chunk.getPos();
            int wx = Math.floorDiv(cp.x(), SpiderColonyDetector.WINDOW_SIDE_CHUNKS) * SpiderColonyDetector.WINDOW_SIDE_CHUNKS;
            int wz = Math.floorDiv(cp.z(), SpiderColonyDetector.WINDOW_SIDE_CHUNKS) * SpiderColonyDetector.WINDOW_SIDE_CHUNKS;
            if (PENDING.size() >= MAX_PENDING) {
                Iterator<PendingWindow> it = PENDING.iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
            PENDING.add(new PendingWindow(level, wx, wz));
        });

        ServerTickEvents.END_SERVER_TICK.register(SpiderColonyWorldgen::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    private static void onServerTick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        Iterator<PendingWindow> it = PENDING.iterator();
        int inspected = 0;
        while (it.hasNext() && inspected < 4) {
            PendingWindow w = it.next();
            inspected++;
            if (w.level().getServer() != server) {
                it.remove();
                continue;
            }
            if (!isWindowWithHaloLoaded(w.level(), w.wx(), w.wz())) {
                continue;
            }
            it.remove();
            evaluateWindow(w.level(), w.wx(), w.wz());
            break; // at most one full 128x128x97 evaluation per tick
        }
    }

    public static boolean isWindowWithHaloLoaded(ServerLevel level, int wx, int wz) {
        int halo = SpiderColonyDetector.CHUNK_HALO;
        int side = SpiderColonyDetector.WINDOW_SIDE_CHUNKS;
        for (int cx = wx - halo; cx <= wx + side; cx++) {
            for (int cz = wz - halo; cz <= wz + side; cz++) {
                if (level.getChunk(cx, cz, ChunkStatus.FULL, false) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Evaluates an 8×8-chunk window (with 1-chunk halo) using Stage 1 criteria and places at most one
     * Spider Colony if the 1.5 km² region quota and 128–256 block non-overlap rules allow.
     *
     * @return the placed colony record, or {@code null} if no colony was placed
     */
    public static ColonyLedger.RecordedColony evaluateWindow(ServerLevel level, int wx, int wz) {
        if (level.dimension() != Level.OVERWORLD) return null;
        ColonyLedger ledger = SpiderColonyEntities.data(level);
        if (ledger.isWindowEvaluated(wx, wz)) return null;
        ledger.markWindowEvaluated(wx, wz);

        int centerChunkX = wx + SpiderColonyDetector.WINDOW_SIDE_CHUNKS / 2;
        int centerChunkZ = wz + SpiderColonyDetector.WINDOW_SIDE_CHUNKS / 2;
        int regionX = SpiderColonyPlacementRules.regionX(centerChunkX);
        int regionZ = SpiderColonyPlacementRules.regionZ(centerChunkZ);

        long seed = level.getSeed();
        int targetCount = SpiderColonyPlacementRules.targetColoniesForRegion(seed, regionX, regionZ);
        if (ledger.regionColonyCount(regionX, regionZ) >= targetCount) {
            return null;
        }

        int sideBlocks = SpiderColonyDetector.WINDOW_SIDE_BLOCKS;
        int originX = wx * 16;
        int originZ = wz * 16;

        List<SpiderColonyDetector.Box3i> structureBoxes = collectStructureBoxes(level, wx, wz);
        boolean[][][] air = sampleAirVoxels(level, originX, originZ, sideBlocks);

        List<SpiderColonyDetector.CoreCandidate> candidates =
                SpiderColonyDetector.detect(air, sideBlocks, originX, originZ, structureBoxes);
        if (candidates.isEmpty()) return null;

        List<ColonyLedger.RecordedColony> existing = ledger.recordedColonies();
        for (SpiderColonyDetector.CoreCandidate core : candidates) {
            BlockPos centerPos = new BlockPos(core.worldX(), core.worldY(), core.worldZ());
            if (level.canSeeSky(centerPos)) {
                continue;
            }
            int nestRadius = SpiderColonyPlacementRules.nestRadiusForSite(
                    seed, core.worldX(), core.worldY(), core.worldZ());
            if (SpiderColonyPlacementRules.overlapsAny(core.worldX(), core.worldZ(), nestRadius, existing)) {
                continue;
            }
            BlockPos motherFloor = findMotherSpawnPos(level, core);
            if (motherFloor == null) {
                continue;
            }
            ColonyLedger.RecordedColony placed = placeColony(
                    level, ledger, core, motherFloor, nestRadius, regionX, regionZ);
            if (placed != null) {
                return placed;
            }
        }
        return null;
    }

    private static List<SpiderColonyDetector.Box3i> collectStructureBoxes(ServerLevel level, int wx, int wz) {
        int halo = SpiderColonyDetector.CHUNK_HALO;
        int side = SpiderColonyDetector.WINDOW_SIDE_CHUNKS;
        List<SpiderColonyDetector.Box3i> boxes = new ArrayList<>();
        for (int cx = wx - halo; cx <= wx + side; cx++) {
            for (int cz = wz - halo; cz <= wz + side; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entry : chunk.getAllStarts().entrySet()) {
                    if (entry.getValue().isValid()) {
                        boxes.add(toBox3i(entry.getValue().getBoundingBox()));
                    }
                }
                for (var entry : chunk.getAllReferences().entrySet()) {
                    for (long ref : entry.getValue()) {
                        ChunkPos cp = ChunkPos.unpack(ref);
                        ChunkAccess startChunk = level.getChunk(cp.x(), cp.z(), ChunkStatus.STRUCTURE_STARTS, false);
                        if (startChunk == null) continue;
                        var st = startChunk.getStartForStructure(entry.getKey());
                        if (st != null && st.isValid()) {
                            boxes.add(toBox3i(st.getBoundingBox()));
                        }
                    }
                }
            }
        }
        return boxes;
    }

    private static SpiderColonyDetector.Box3i toBox3i(BoundingBox bb) {
        return new SpiderColonyDetector.Box3i(
                bb.minX(), bb.minY(), bb.minZ(),
                bb.maxX(), bb.maxY(), bb.maxZ());
    }

    private static boolean[][][] sampleAirVoxels(ServerLevel level, int originX, int originZ, int sideBlocks) {
        boolean[][][] air = new boolean[sideBlocks][sideBlocks][SpiderColonyDetector.Y_SIZE];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < sideBlocks; lx++) {
            int wx = originX + lx;
            for (int lz = 0; lz < sideBlocks; lz++) {
                int wz = originZ + lz;
                for (int ly = 0; ly < SpiderColonyDetector.Y_SIZE; ly++) {
                    int wy = SpiderColonyDetector.Y_MIN + ly;
                    BlockState state = level.getBlockState(pos.set(wx, wy, wz));
                    air[lx][lz][ly] = state.isAir() && state.getFluidState().isEmpty();
                }
            }
        }
        return air;
    }

    private static BlockPos findMotherSpawnPos(ServerLevel level, SpiderColonyDetector.CoreCandidate core) {
        int maxDown = (int) Math.ceil(core.radiusBlocks()) + 6;
        int minY = Math.max(SpiderColonyDetector.Y_MIN + 1, core.worldY() - maxDown);
        for (int r = 0; r <= 6; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = core.worldX() + dx;
                    int z = core.worldZ() + dz;
                    for (int y = core.worldY(); y >= minY; y--) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockPos below = pos.below();
                        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                        if (hasMotherClearance(level, pos)) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasMotherClearance(ServerLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() || !s.getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static ColonyLedger.RecordedColony placeColony(
            ServerLevel level,
            ColonyLedger ledger,
            SpiderColonyDetector.CoreCandidate core,
            BlockPos motherPos,
            int nestRadius,
            int regionX,
            int regionZ) {
        RandomSource random = RandomSource.create(
                level.getSeed() ^ ((long) core.worldX() * 341873128712L) ^ ((long) core.worldZ() * 132897987541L));

        int hRange = Math.min(20, (int) Math.ceil(core.radiusBlocks()) + 4);
        int maxY = Math.min(SpiderColonyDetector.Y_MAX, core.worldY() + (int) Math.ceil(core.radiusBlocks()) + 6);
        int minY = Math.max(SpiderColonyDetector.Y_MIN + 2, core.worldY() - 2);

        List<BlockPos> ceilingSpots = new ArrayList<>();
        for (int dx = -hRange; dx <= hRange; dx++) {
            for (int dz = -hRange; dz <= hRange; dz++) {
                if (dx * dx + dz * dz > hRange * hRange) continue;
                for (int y = minY; y <= maxY; y++) {
                    BlockPos p = new BlockPos(core.worldX() + dx, y, core.worldZ() + dz);
                    if (!level.getBlockState(p).isAir()) continue;
                    if (!level.getBlockState(p.below()).isAir()) continue;
                    if (!level.getBlockState(p.above()).isFaceSturdy(level, p.above(), Direction.DOWN)) continue;
                    if (level.canSeeSky(p)) continue;
                    ceilingSpots.add(p);
                }
            }
        }
        if (ceilingSpots.isEmpty()) {
            return null;
        }

        // Shuffle deterministically
        for (int i = ceilingSpots.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(ceilingSpots, i, j);
        }

        HangingCocoonBlock cocoonBlock = (HangingCocoonBlock) ModBlocks.HANGING_COCOON;
        List<BlockPos> placedCocoons = new ArrayList<>();
        for (BlockPos spot : ceilingSpots) {
            if (placedCocoons.size() >= 12) break;
            boolean tooClose = false;
            for (BlockPos existing : placedCocoons) {
                if (existing.distSqr(spot) < 4) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;
            level.setBlock(spot, cocoonBlock.naturalState(random), 3);
            placedCocoons.add(spot);
        }
        if (placedCocoons.isEmpty()) {
            return null;
        }

        // Place floor egg sacs away from mother spawn
        List<BlockPos> floorSpots = new ArrayList<>();
        int floorMinY = Math.max(SpiderColonyDetector.Y_MIN + 1, core.worldY() - (int) Math.ceil(core.radiusBlocks()) - 4);
        int floorMaxY = Math.min(SpiderColonyDetector.Y_MAX - 1, core.worldY() + 2);
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                if (dx * dx + dz * dz > 196) continue;
                for (int y = floorMinY; y <= floorMaxY; y++) {
                    BlockPos p = new BlockPos(core.worldX() + dx, y, core.worldZ() + dz);
                    if (p.distSqr(motherPos) < 9) continue;
                    if (!level.getBlockState(p).isAir()) continue;
                    if (!level.getBlockState(p.above()).isAir()) continue;
                    if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) continue;
                    floorSpots.add(p);
                }
            }
        }
        for (int i = floorSpots.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(floorSpots, i, j);
        }
        EggSacBlock eggSacBlock = (EggSacBlock) ModBlocks.SPIDER_EGG_SAC;
        int eggCount = 0;
        for (BlockPos spot : floorSpots) {
            if (eggCount >= 6) break;
            level.setBlock(spot, eggSacBlock.naturalState(random), 3);
            eggCount++;
        }

        // Sparse cobwebs on upper walls/ceilings (not near mother spawn)
        int cobwebs = 0;
        for (BlockPos spot : ceilingSpots) {
            if (cobwebs >= 10) break;
            if (!level.getBlockState(spot).isAir()) continue;
            if (spot.distSqr(motherPos) < 16) continue;
            level.setBlock(spot, Blocks.COBWEB.defaultBlockState(), 3);
            cobwebs++;
        }

        String nestId = "colony_" + core.worldX() + "_" + core.worldY() + "_" + core.worldZ();
        MotherSpider mother = SpiderColonyEntities.MOTHER.create(level, EntitySpawnReason.STRUCTURE);
        if (mother == null) return null;
        mother.snapTo(motherPos.getX() + 0.5, motherPos.getY(), motherPos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        if (!mother.bindNest(level, nestId)) {
            mother.discard();
            return null;
        }
        if (!level.addFreshEntity(mother)) {
            return null;
        }

        ledger.recordColony(nestId, core.worldX(), core.worldY(), core.worldZ(), nestRadius, regionX, regionZ);
        MoreMinecrafts.LOGGER.info("Placed Spider Colony {} at ({}, {}, {}) radius={} region=({},{}) cocoons={} eggs={}",
                nestId, core.worldX(), core.worldY(), core.worldZ(), nestRadius, regionX, regionZ, placedCocoons.size(), eggCount);
        return new ColonyLedger.RecordedColony(
                nestId, core.worldX(), core.worldY(), core.worldZ(), nestRadius, regionX, regionZ);
    }
}
