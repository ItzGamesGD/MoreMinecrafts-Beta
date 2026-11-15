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
            synchronized (PENDING) {
                if (PENDING.size() >= MAX_PENDING) {
                    Iterator<PendingWindow> it = PENDING.iterator();
                    if (it.hasNext()) {
                        it.next();
                        it.remove();
                    }
                }
                PENDING.add(new PendingWindow(level, wx, wz));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(SpiderColonyWorldgen::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (PENDING) {
                PENDING.clear();
            }
        });
    }

    private static void onServerTick(MinecraftServer server) {
        List<PendingWindow> snapshot;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            snapshot = new ArrayList<>();
            Iterator<PendingWindow> it = PENDING.iterator();
            while (it.hasNext() && snapshot.size() < 4) {
                snapshot.add(it.next());
            }
        }
        for (PendingWindow w : snapshot) {
            if (w.level().getServer() != server) {
                synchronized (PENDING) { PENDING.remove(w); }
                continue;
            }
            if (!isWindowWithHaloLoaded(w.level(), w.wx(), w.wz())) {
                continue;
            }
            synchronized (PENDING) { PENDING.remove(w); }
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
        List<SpiderColonyDetector.Box3i> expandedBoxes = new ArrayList<>(structureBoxes.size());
        for (SpiderColonyDetector.Box3i box : structureBoxes) {
            expandedBoxes.add(box.expand(SpiderColonyDetector.STRUCTURE_BUFFER_BLOCKS));
        }
        boolean[][][] air = sampleAirVoxels(level, originX, originZ, sideBlocks);

        SpiderColonyDetector.DetectionResult detection =
                SpiderColonyDetector.detectWithLabels(air, sideBlocks, originX, originZ, structureBoxes);
        List<SpiderColonyDetector.CoreCandidate> candidates = detection.candidates();
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
                    level, ledger, core, motherFloor, nestRadius, regionX, regionZ,
                    originX, originZ, sideBlocks, detection.airLabels(), expandedBoxes);
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
            int regionZ,
            int originX,
            int originZ,
            int sideBlocks,
            int[][][] airLabels,
            List<SpiderColonyDetector.Box3i> expandedBoxes) {
        RandomSource random = RandomSource.create(
                level.getSeed() ^ ((long) core.worldX() * 341873128712L) ^ ((long) core.worldZ() * 132897987541L));

        double nestZoneRadius = Math.max(18.0, core.radiusBlocks() + 8.0);
        double outerDecorRadius = Math.min(nestRadius, 56.0);
        int outerCeil = (int) Math.ceil(outerDecorRadius);
        int minX = Math.max(originX, core.worldX() - outerCeil);
        int maxX = Math.min(originX + sideBlocks - 1, core.worldX() + outerCeil);
        int minZ = Math.max(originZ, core.worldZ() - outerCeil);
        int maxZ = Math.min(originZ + sideBlocks - 1, core.worldZ() + outerCeil);

        List<BlockPos> ceilingSpots = new ArrayList<>();
        List<BlockPos> floorSpots = new ArrayList<>();
        List<BlockPos> wallWebSpots = new ArrayList<>();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int wx = minX; wx <= maxX; wx++) {
            int lx = wx - originX;
            int dx = wx - core.worldX();
            for (int wz = minZ; wz <= maxZ; wz++) {
                int lz = wz - originZ;
                int dz = wz - core.worldZ();
                if (dx * dx + dz * dz > outerDecorRadius * outerDecorRadius) continue;

                for (int wy = SpiderColonyDetector.Y_MIN + 1; wy <= SpiderColonyDetector.Y_MAX - 1; wy++) {
                    int ly = wy - SpiderColonyDetector.Y_MIN;
                    if (airLabels[lx][lz][ly] != core.airComponentId()) continue;

                    cursor.set(wx, wy, wz);
                    if (level.canSeeSky(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() || !state.getFluidState().isEmpty()) continue;
                    if (isInsideAnyBox(wx, wy, wz, expandedBoxes)) continue;

                    BlockPos above = cursor.above();
                    BlockPos below = cursor.below();
                    boolean sturdyCeiling = level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
                    boolean sturdyFloor = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
                    boolean belowAir = level.getBlockState(below).isAir() && level.getFluidState(below).isEmpty();
                    boolean aboveAir = level.getBlockState(above).isAir() && level.getFluidState(above).isEmpty();

                    if (sturdyCeiling && belowAir) {
                        ceilingSpots.add(cursor.immutable());
                    }
                    if (sturdyFloor && aboveAir && cursor.distSqr(motherPos) >= 9) {
                        floorSpots.add(cursor.immutable());
                    }
                    if (cursor.distSqr(motherPos) >= 16 && (sturdyCeiling || hasSturdyWallNeighbor(level, cursor))) {
                        // Prefer walls/ceilings and rocky corners; avoid blocking open floor paths
                        if (!sturdyFloor || hasSturdyWallNeighbor(level, cursor)) {
                            wallWebSpots.add(cursor.immutable());
                        }
                    }
                }
            }
        }
        if (ceilingSpots.isEmpty()) {
            return null;
        }

        shuffle(ceilingSpots, random);
        shuffle(floorSpots, random);
        shuffle(wallWebSpots, random);

        // 1. Hanging Cocoons (Q55, Q64): concentrated in nest zone, tapering toward colony boundary
        HangingCocoonBlock cocoonBlock = (HangingCocoonBlock) ModBlocks.HANGING_COCOON;
        List<BlockPos> placedCocoons = new ArrayList<>();
        for (BlockPos spot : ceilingSpots) {
            if (placedCocoons.size() >= 28) break;
            double dist = horizDist(spot, core);
            double weight = taperWeight(dist, nestZoneRadius, outerDecorRadius);
            if (dist > nestZoneRadius && random.nextDouble() > weight) continue;
            if (tooClose(spot, placedCocoons, 4)) continue;
            level.setBlock(spot, cocoonBlock.naturalState(random), 3);
            placedCocoons.add(spot);
        }
        if (placedCocoons.isEmpty()) {
            return null;
        }

        // 2. Egg Sacs (Q26, Q29, Q64): evenly distributed across cave floor, tapering at boundary
        EggSacBlock eggSacBlock = (EggSacBlock) ModBlocks.SPIDER_EGG_SAC;
        List<BlockPos> placedEggs = new ArrayList<>();
        for (BlockPos spot : floorSpots) {
            if (placedEggs.size() >= 32) break;
            double dist = horizDist(spot, core);
            double weight = taperWeight(dist, nestZoneRadius + 10.0, outerDecorRadius);
            if (weight <= 0.0 || (dist > nestZoneRadius + 10.0 && random.nextDouble() > weight)) continue;
            if (tooClose(spot, placedEggs, 9)) continue;
            level.setBlock(spot, eggSacBlock.naturalState(random), 3);
            placedEggs.add(spot);
        }

        // 3. Bones & Prey Remains (Q55, Q64): in nest zone, wrapped with cobweb, tapering outward
        List<BlockPos> placedBones = new ArrayList<>();
        for (BlockPos spot : floorSpots) {
            if (placedBones.size() >= 5) break;
            double dist = horizDist(spot, core);
            if (dist > nestZoneRadius + 6.0) continue;
            if (!level.getBlockState(spot).isAir()) continue;
            if (spot.distSqr(motherPos) < 16) continue;
            if (tooClose(spot, placedBones, 25) || tooClose(spot, placedEggs, 4)) continue;
            if (isInsideAnyBox(spot.getX(), spot.getY(), spot.getZ(), expandedBoxes)) continue;

            BlockState boneState = (placedBones.size() % 2 == 0)
                    ? Blocks.BONE_BLOCK.defaultBlockState()
                    : Blocks.SKELETON_SKULL.defaultBlockState();
            level.setBlock(spot, boneState, 3);
            placedBones.add(spot);

            // Wrap adjacent air cell with cobweb as prey remains
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = spot.relative(dir);
                if (adj.distSqr(motherPos) >= 16
                        && !isInsideAnyBox(adj.getX(), adj.getY(), adj.getZ(), expandedBoxes)
                        && level.getBlockState(adj).isAir()
                        && level.getFluidState(adj).isEmpty()) {
                    level.setBlock(adj, Blocks.COBWEB.defaultBlockState(), 3);
                    break;
                }
            }
        }

        // 4. Cobwebs (Q54, Q64, Q69): medium density on walls & ceilings, higher near nest, tapering at border
        int cobwebs = 0;
        for (BlockPos spot : wallWebSpots) {
            if (cobwebs >= 120) break;
            if (!level.getBlockState(spot).isAir() || !level.getFluidState(spot).isEmpty()) continue;
            double dist = horizDist(spot, core);
            double weight = 0.75 * taperWeight(dist, nestZoneRadius, outerDecorRadius);
            if (random.nextDouble() > weight) continue;
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
        MoreMinecrafts.LOGGER.info(
                "Placed Spider Colony {} at ({}, {}, {}) radius={} region=({},{}) cocoons={} eggs={} bones={} webs={}",
                nestId, core.worldX(), core.worldY(), core.worldZ(), nestRadius, regionX, regionZ,
                placedCocoons.size(), placedEggs.size(), placedBones.size(), cobwebs);
        return new ColonyLedger.RecordedColony(
                nestId, core.worldX(), core.worldY(), core.worldZ(), nestRadius, regionX, regionZ);
    }

    private static boolean hasSturdyWallNeighbor(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = pos.relative(dir);
            if (level.getBlockState(adj).isFaceSturdy(level, adj, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideAnyBox(int x, int y, int z, List<SpiderColonyDetector.Box3i> boxes) {
        for (SpiderColonyDetector.Box3i box : boxes) {
            if (box.containsPoint(x, y, z)) return true;
        }
        return false;
    }

    private static double horizDist(BlockPos p, SpiderColonyDetector.CoreCandidate core) {
        double dx = p.getX() - core.worldX();
        double dz = p.getZ() - core.worldZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    static double taperWeight(double dist, double innerRadius, double outerRadius) {
        if (dist <= innerRadius) return 1.0;
        if (dist >= outerRadius) return 0.0;
        return 1.0 - (dist - innerRadius) / Math.max(1.0, outerRadius - innerRadius);
    }

    private static boolean tooClose(BlockPos candidate, List<BlockPos> existing, int minDistSq) {
        for (BlockPos p : existing) {
            if (p.distSqr(candidate) < minDistSq) return true;
        }
        return false;
    }

    private static void shuffle(List<BlockPos> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}
