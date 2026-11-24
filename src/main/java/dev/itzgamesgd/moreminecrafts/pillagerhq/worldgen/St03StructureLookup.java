package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Finds the planned ST-03 headquarters that covers a position during world generation, using only the chunk's own
 * structure references and the starts the region already holds (STRUCTURE_STARTS). No chunk is loaded or generated.
 */
final class St03StructureLookup {
    private St03StructureLookup() {}

    /** Footprint edge in blocks (the 112x112 area of the terrain correction piece). */
    static final int FOOTPRINT = 112;

    /**
     * The terrain-correction origin (x/z of the footprint corner, y of the foundation level) of a valid ST-03 start whose
     * footprint contains the column of {@code pos}, or {@code null} when none does.
     */
    static BlockPos footprintOriginAt(WorldGenRegion region, BlockPos pos) {
        ChunkAccess chunk = region.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Map<Structure, LongSet> references = chunk.getAllReferences();
        if (references.isEmpty()) return null;
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            if (!(entry.getKey() instanceof St03HeadquartersStructure)) continue;
            for (LongIterator it = entry.getValue().iterator(); it.hasNext(); ) {
                ChunkPos startChunk = ChunkPos.unpack(it.nextLong());
                if (!region.hasChunk(startChunk.x(), startChunk.z())) continue;
                StructureStart start = region.getChunk(startChunk.x(), startChunk.z(), ChunkStatus.STRUCTURE_STARTS, false)
                        .getStartForStructure(entry.getKey());
                if (start == null || !start.isValid()) continue;
                BlockPos origin = terrainOrigin(start);
                if (origin != null && columnInFootprint(origin, pos)) return origin;
            }
        }
        return null;
    }

    private static BlockPos terrainOrigin(StructureStart start) {
        for (var piece : start.getPieces()) {
            if (piece instanceof St03TerrainCorrectionPiece terrain) return terrain.origin();
        }
        return null;
    }

    static boolean columnInFootprint(BlockPos origin, BlockPos pos) {
        return pos.getX() >= origin.getX() && pos.getX() < origin.getX() + FOOTPRINT
                && pos.getZ() >= origin.getZ() && pos.getZ() < origin.getZ() + FOOTPRINT;
    }
}
