package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Chunk-order independence of structure-owned blocks (P8-C2-B §5).
 *
 * <p>Vanilla places every structure piece clipped to the decorating chunk, so the pyramid's own blocks never depend on
 * generation order. Features are different: a feature decorated in a neighbouring chunk (fossils, lakes, disks, desert
 * wells, vegetation...) may write up to one chunk into this one, before or after this chunk placed the pyramid. Before,
 * the pyramid overwrites it; after, it would overwrite the pyramid - the result would depend on which chunk generated
 * first. This guard makes the second case behave like the first: inside the completion box of a valid ST-01 start, only
 * ST-01's own pieces may write during world generation. Nothing outside the box is affected, and nothing after world
 * generation (players, the server level) is affected.
 *
 * <p>Lookups use only the chunk's own structure references and the starts the region already holds for the FEATURES
 * step (STRUCTURE_STARTS within 8 chunks); no chunk is loaded or generated.
 */
public final class St01WorldgenGuard {
    private St01WorldgenGuard() {}

    private static final ThreadLocal<int[]> OWNED_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    /** Count of foreign world-generation writes refused inside ST-01 boxes (diagnostic, current process). */
    public static final AtomicLong REFUSED_WRITES = new AtomicLong();

    /** Runs an ST-01 piece placement; its writes are never refused. */
    public static void owned(Runnable placement) {
        int[] depth = OWNED_DEPTH.get();
        depth[0]++;
        try {
            placement.run();
        } finally {
            depth[0]--;
        }
    }

    /** Whether a world-generation write at {@code pos} through {@code region} must be refused. */
    public static boolean refuses(WorldGenRegion region, BlockPos pos) {
        if (OWNED_DEPTH.get()[0] > 0 || !region.isWithinWriteZone(pos)) return false;
        ChunkAccess chunk = region.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Map<Structure, LongSet> references = chunk.getAllReferences();
        if (references.isEmpty()) return false;
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            if (!(entry.getKey() instanceof St01PyramidStructure)) continue;
            for (LongIterator it = entry.getValue().iterator(); it.hasNext(); ) {
                ChunkPos startChunk = ChunkPos.unpack(it.nextLong());
                if (!region.hasChunk(startChunk.x(), startChunk.z())) continue;
                StructureStart start;
                try {
                    start = region.getChunk(startChunk.x(), startChunk.z(), ChunkStatus.STRUCTURE_STARTS, false)
                            .getStartForStructure(entry.getKey());
                } catch (RuntimeException unavailable) {
                    continue; // not readable at this step (never the FEATURES step): fall back to vanilla behaviour
                }
                if (start != null && start.isValid() && start.getBoundingBox().isInside(pos)) {
                    REFUSED_WRITES.incrementAndGet();
                    return true;
                }
            }
        }
        return false;
    }
}
