package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import java.util.function.IntBinaryOperator;
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
 * MODERATE terrain adaptation for the complete assembly (replaces the Phase 4 fixture correction).
 *
 * <p>The complete assembly owns every cell of the footprint from Y-32 up to and including the full 81x81 Y0 layer, so
 * everything below the walking level is replaced by the pyramid itself: the "cut" and "skirt" of the qualified candidate
 * are the only terrain consequences below Y1, and both are bounded by {@link St01Eligibility} (cut <= 10, skirt <= 8).
 * What the assembly does not own is the open air above Y0 outside the stepped shell (structure void). This piece clears
 * leftover natural terrain there, inside the approved 81x81 correction area only, and only up to the natural terrain
 * height of the column plus {@link St01PlacementConfig#VEGETATION_CLEARANCE} (plants, dead bushes, cactus already placed
 * above it), never above the completion box. It never writes outside the footprint, never adds blocks, and runs only for
 * accepted starts (a rejected candidate has no start). Its inputs are the generator base height (a pure function of the
 * seed) and the chunk's own blocks, so the result is independent of chunk generation order. It runs before the assembly
 * piece, which then writes every owned cell; cells it cleared that the assembly owns are rewritten by the assembly.
 */
public final class St01FootprintCorrectionPiece extends StructurePiece {
    private final int centerX, foundationY, centerZ;

    public St01FootprintCorrectionPiece(int centerX, int foundationY, int centerZ) {
        super(St01Worldgen.FOOTPRINT_CORRECTION_PIECE, 0, box(centerX, foundationY, centerZ));
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

    /** Footprint columns, from the first above-ground layer (Y1) to the top of the completion box (Y39). */
    static BoundingBox box(int centerX, int foundationY, int centerZ) {
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        return new BoundingBox(centerX - half, foundationY + 1, centerZ - half,
                centerX + half, foundationY + 39, centerZ + half);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", centerX);
        tag.putInt("FY", foundationY);
        tag.putInt("CZ", centerZ);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        var randomState = level.getLevel().getChunkSource().randomState();
        St01WorldgenGuard.owned(() -> correct(level, chunkBox, centerX, foundationY, centerZ,
                (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState)));
    }

    /** Applies the column rule inside {@code chunkBox}; returns the number of blocks cleared (diagnostic). */
    public static int correct(WorldGenLevel level, BoundingBox chunkBox, int centerX, int foundationY, int centerZ,
            IntBinaryOperator baseHeightOf) {
        BoundingBox box = box(centerX, foundationY, centerZ);
        int minX = Math.max(chunkBox.minX(), box.minX()), maxX = Math.min(chunkBox.maxX(), box.maxX());
        int minZ = Math.max(chunkBox.minZ(), box.minZ()), maxZ = Math.min(chunkBox.maxZ(), box.maxZ());
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cleared = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int base = baseHeightOf.applyAsInt(x, z); // first free Y of the natural terrain
                int top = Math.min(box.maxY(), Math.max(base, foundationY + 1) + St01PlacementConfig.VEGETATION_CLEARANCE);
                for (int y = box.minY(); y <= top; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
                        level.setBlock(pos, air, 2);
                        cleared++;
                    }
                }
            }
        }
        return cleared;
    }
}
