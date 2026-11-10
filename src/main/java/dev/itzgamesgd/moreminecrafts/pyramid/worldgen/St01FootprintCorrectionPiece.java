package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * MODERATE terrain correction over exactly the approved 81x81 footprint, and nowhere else: terrain above the foundation level
 * {@code yf} is cut to {@code yf - 1} (air above), terrain below it is filled up to {@code yf - 1} (sand over sandstone). Candidates were
 * already qualified against cut <= 10 and skirt <= 8 by {@link St01Eligibility}; this piece only runs for accepted starts, so a rejected
 * candidate leaves no terrain change. Each chunk is corrected from its own column heights only (no neighbour reads), so the result is
 * independent of chunk generation order.
 */
public final class St01FootprintCorrectionPiece extends StructurePiece {
    private final int centerX, foundationY, centerZ;

    public St01FootprintCorrectionPiece(int centerX, int foundationY, int centerZ) {
        super(St01Worldgen.FOOTPRINT_CORRECTION_PIECE, 0, new BoundingBox(
                centerX - St01PlacementConfig.FOOTPRINT_HALF_WIDTH, foundationY - St01PlacementConfig.MAX_SKIRT, centerZ - St01PlacementConfig.FOOTPRINT_HALF_WIDTH,
                centerX + St01PlacementConfig.FOOTPRINT_HALF_WIDTH, foundationY + St01PlacementConfig.MAX_CUT, centerZ + St01PlacementConfig.FOOTPRINT_HALF_WIDTH));
        this.centerX = centerX;
        this.foundationY = foundationY;
        this.centerZ = centerZ;
    }

    public St01FootprintCorrectionPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(St01Worldgen.FOOTPRINT_CORRECTION_PIECE, tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.foundationY = tag.getIntOr("FY", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", centerX);
        tag.putInt("FY", foundationY);
        tag.putInt("CZ", centerZ);
    }

    /** Observed per-start correction extremes, for audits (in-memory, current process only). Key = "centerX,centerZ". */
    public static final java.util.concurrent.ConcurrentHashMap<String, Stats> STATS = new java.util.concurrent.ConcurrentHashMap<>();

    public static final class Stats {
        public final java.util.concurrent.atomic.AtomicInteger maxCut = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger maxFill = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger columnsCut = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger columnsFilled = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger columnsSealed = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger columnsBelowSkirtBand = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger columnsProcessed = new java.util.concurrent.atomic.AtomicInteger();
    }

    /**
     * Column rule (all inputs are pure functions of the world seed, never of chunk generation order):
     * <ol>
     *   <li>{@code base} = generator base height (first free Y of the noise terrain). Terrain above {@code yf} is removed up to
     *       {@code base} and, for plants/features already placed above it, up to {@code base + 8}.</li>
     *   <li>Every non-solid block (air, fluid) in the skirt band {@code [yf - MAX_SKIRT, yf - 1]} becomes sand (top 4 layers) or sandstone, so the
     *       foundation surface is closed even where a carved cave or a lower column would leave a pit. Blocks that are already solid are untouched.</li>
     * </ol>
     */
    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        net.minecraft.world.level.levelgen.RandomState randomState = level.getLevel().getChunkSource().randomState();
        correct(level, chunkBox, centerX, foundationY, centerZ,
                (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState));
    }

    /** Applies the column rule inside {@code chunkBox} for the footprint at (centerX, centerZ) with the given base-height source. */
    public static void correct(WorldGenLevel level, BoundingBox chunkBox, int centerX, int foundationY, int centerZ,
            java.util.function.IntBinaryOperator baseHeightOf) {
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        int minX = Math.max(chunkBox.minX(), centerX - half), maxX = Math.min(chunkBox.maxX(), centerX + half);
        int minZ = Math.max(chunkBox.minZ(), centerZ - half), maxZ = Math.min(chunkBox.maxZ(), centerZ + half);
        Stats stats = STATS.computeIfAbsent(centerX + "," + centerZ, k -> new Stats());
        BlockState sand = Blocks.SAND.defaultBlockState(), sandstone = Blocks.SANDSTONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        int bandBottom = foundationY - St01PlacementConfig.MAX_SKIRT;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                stats.columnsProcessed.incrementAndGet();
                int base = baseHeightOf.applyAsInt(x, z);
                if (base > foundationY) {
                    stats.columnsCut.incrementAndGet();
                    stats.maxCut.accumulateAndGet(base - foundationY, Math::max);
                    int top = Math.min(level.getMaxY(), base + 8);
                    for (int y = top; y >= foundationY; y--) {
                        if (!level.getBlockState(pos.set(x, y, z)).isAir()) level.setBlock(pos, air, 2);
                    }
                } else if (base < foundationY) {
                    stats.columnsFilled.incrementAndGet();
                    stats.maxFill.accumulateAndGet(foundationY - base, Math::max);
                    if (foundationY - base > St01PlacementConfig.MAX_SKIRT) stats.columnsBelowSkirtBand.incrementAndGet();
                }
                boolean sealed = false;
                for (int y = bandBottom; y < foundationY; y++) {
                    BlockState existing = level.getBlockState(pos.set(x, y, z));
                    // keep only solid occluding terrain; plants, trees (logs/leaves), fluids and air are replaced so the result does not depend on whether
                    // vegetation features of neighbouring chunks ran before this piece
                    boolean terrain = existing.canOcclude() && existing.getFluidState().isEmpty()
                            && !existing.is(net.minecraft.tags.BlockTags.LOGS) && !existing.is(net.minecraft.tags.BlockTags.LEAVES);
                    if (!terrain) {
                        level.setBlock(pos, y >= foundationY - 4 ? sand : sandstone, 2);
                        sealed = true;
                    }
                }
                if (sealed && base >= foundationY) stats.columnsSealed.incrementAndGet();
            }
        }
    }
}
