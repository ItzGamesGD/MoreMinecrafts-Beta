package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Keeps world features (trees, grass, flowers, boulders, ...) out of the ST-03 headquarters.
 *
 * <p>The headquarters is placed as a structure, then the vegetal and surface decoration steps run over the same chunks.
 * Features whose blocks fall inside the 112x112 footprint above the foundation level would grow into the compound. While
 * the headquarters' own pieces are being placed ({@link #beginPlacement}/{@link #endPlacement}, set around
 * {@code StructureStart.placeInChunk}) every write is allowed; outside that scope every write inside the footprint at or above
 * the foundation level is dropped, except air and snow layers (snow settling on the compound is intended).
 *
 * <p>The scope is per thread: chunk generation writes on the worker thread that runs the decoration step.
 */
public final class St03ExteriorFeatureGuard {
    private St03ExteriorFeatureGuard() {}

    /** Blocks above the foundation level that are guarded (structure plus the tallest natural tree). */
    static final int GUARDED_HEIGHT = 96;

    private static final ThreadLocal<Boolean> PLACING_HEADQUARTERS = ThreadLocal.withInitial(() -> false);

    /** Marks the start of a structure start's placement on this thread; only ST-03 starts open the scope. */
    public static void beginPlacement(Structure structure) {
        PLACING_HEADQUARTERS.set(structure instanceof St03HeadquartersStructure);
    }

    public static void endPlacement() {
        PLACING_HEADQUARTERS.set(false);
    }

    /** Whether a feature write of {@code state} at {@code pos} through {@code region} is dropped. */
    public static boolean blocksExteriorWrite(WorldGenRegion region, BlockPos pos, BlockState state) {
        if (PLACING_HEADQUARTERS.get() || allowedExteriorState(state) || !region.isWithinWriteZone(pos)) return false;
        BlockPos origin = St03StructureLookup.footprintOriginAt(region, pos);
        return origin != null && inGuardedVolume(origin.getY(), pos.getY());
    }

    /** Air and snow layers may settle on the compound; everything else is a feature intrusion. */
    static boolean allowedExteriorState(BlockState state) {
        return state.isAir() && !state.is(Blocks.CAVE_AIR) || state.is(Blocks.SNOW);
    }

    /** Whether a block at {@code y} is in the guarded height range for a footprint with foundation level {@code foundationY}. */
    static boolean inGuardedVolume(int foundationY, int y) {
        return y >= foundationY && y < foundationY + GUARDED_HEIGHT;
    }
}
