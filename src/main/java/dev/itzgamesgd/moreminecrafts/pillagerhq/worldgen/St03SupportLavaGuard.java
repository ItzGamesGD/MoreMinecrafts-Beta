package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Keeps non-solid blocks (lava, fluids, plants) out of the ST-03 support band after structure placement.
 *
 * <p>Such blocks in the footprint's support band (y from foundation-16 up to foundation-1, inside the 112x112 footprint)
 * can be written by features that run after the terrain correction piece, including features decorated in neighbouring
 * chunks. Each write is replaced by cobblestone, so the support band stays solid. Air (including cave air), blocks outside
 * the footprint and the foundation level and above are not touched.
 *
 * <p>Lookups use only the chunk's own structure references and the starts the region already holds (STRUCTURE_STARTS);
 * no chunk is loaded or generated.
 */
public final class St03SupportLavaGuard {
    private St03SupportLavaGuard() {}

    /** Depth of support band below the foundation level that is replaced (matches the terrain fill depth). */
    private static final int BAND_DEPTH = St03PlacementConfig.MAX_FOOTING_FILL;

    /**
     * Whether a feature write at {@code pos} through {@code region} must be replaced with cobblestone: a non-solid,
     * non-air block (lava, fluid, plant) inside the footprint's support band. Air, including cave air, is not replaced.
     */
    public static boolean replacesNonSolidSupport(WorldGenRegion region, BlockPos pos, BlockState state) {
        if (state.isSolidRender() || state.isAir() || !region.isWithinWriteZone(pos)) return false;
        ChunkAccess chunk = region.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Map<Structure, LongSet> references = chunk.getAllReferences();
        if (references.isEmpty()) return false;
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            if (!(entry.getKey() instanceof St03HeadquartersStructure)) continue;
            for (LongIterator it = entry.getValue().iterator(); it.hasNext(); ) {
                ChunkPos startChunk = ChunkPos.unpack(it.nextLong());
                if (!region.hasChunk(startChunk.x(), startChunk.z())) continue;
                StructureStart start = region.getChunk(startChunk.x(), startChunk.z(), ChunkStatus.STRUCTURE_STARTS, false)
                        .getStartForStructure(entry.getKey());
                if (start == null || !start.isValid()) continue;
                if (inSupportBand(start, pos)) return true;
            }
        }
        return false;
    }

    private static boolean inSupportBand(StructureStart start, BlockPos pos) {
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof St03TerrainCorrectionPiece terrain)) continue;
            BlockPos origin = terrain.origin();
            int fy = origin.getY();
            return pos.getX() >= origin.getX() && pos.getX() <= origin.getX() + 111
                    && pos.getZ() >= origin.getZ() && pos.getZ() <= origin.getZ() + 111
                    && pos.getY() < fy && pos.getY() >= fy - BAND_DEPTH;
        }
        return false;
    }
}
