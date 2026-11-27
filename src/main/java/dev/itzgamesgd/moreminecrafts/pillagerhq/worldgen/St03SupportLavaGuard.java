package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps non-solid blocks (lava, fluids, plants) and cave air out of the ST-03 support band after structure placement.
 *
 * <p>Such blocks in the footprint's support band (y from foundation-16 up to foundation-1, inside the 112x112 footprint)
 * can be written by features that run after the terrain correction piece, including features decorated in neighbouring
 * chunks. Each write is replaced by cobblestone, so the support band stays solid. Ordinary air, blocks outside the
 * footprint and the foundation level and above are not touched.
 *
 * <p>Lookups use only the chunk's own structure references and the starts the region already holds (STRUCTURE_STARTS);
 * no chunk is loaded or generated.
 */
public final class St03SupportLavaGuard {
    private St03SupportLavaGuard() {}

    /** Depth of support band below the foundation level that is replaced (matches the terrain fill depth). */
    private static final int BAND_DEPTH = St03PlacementConfig.MAX_FOOTING_FILL;

    /**
     * Whether a feature write at {@code pos} through {@code region} must be replaced with cobblestone: a non-solid
     * block (lava, fluid, plant) or cave air inside the footprint's support band. Ordinary air is not replaced.
     */
    public static boolean replacesNonSolidSupport(WorldGenRegion region, BlockPos pos, BlockState state) {
        if (state.isSolidRender() || (state.isAir() && !state.is(Blocks.CAVE_AIR)) || !region.isWithinWriteZone(pos)) return false;
        BlockPos origin = St03StructureLookup.footprintOriginAt(region, pos);
        if (origin == null) return false;
        int fy = origin.getY();
        return pos.getY() < fy && pos.getY() >= fy - BAND_DEPTH;
    }
}
