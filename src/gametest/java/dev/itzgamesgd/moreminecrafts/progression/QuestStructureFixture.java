package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;

/**
 * Registers a real {@link StructureStart} (real pieces, real chunk start and chunk reference records) so the
 * vanilla {@code StructureManager} queries that production code uses run unmodified. Pieces are genuine
 * end-city template pieces regardless of which structure id they are registered under.
 */
final class QuestStructureFixture {
    private QuestStructureFixture() {}

    record Registered(StructureStart start, List<BoundingBox> pieces) {
        BoundingBox firstPiece() { return pieces.getFirst(); }
        /** A block inside the first piece. */
        BlockPos inside() { BoundingBox b = firstPiece(); return new BlockPos(b.minX() + 1, b.minY() + 1, b.minZ() + 1); }
    }

    static Structure structure(ServerLevel level, String id) {
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .getValue(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(id)));
    }

    /**
     * @param chunk   start chunk of the structure; pieces are placed at its south-east corner
     * @param offsets additional piece origins relative to the first piece (use large offsets to leave gaps)
     */
    static Registered register(ServerLevel level, String structureId, ChunkPos chunk, BlockPos... offsets) {
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, chunk, 3);
        Structure structure = structure(level, structureId);
        if (structure == null) throw new IllegalStateException("unknown structure " + structureId);
        BlockPos origin = new BlockPos(chunk.getMinBlockX() + 2, 70, chunk.getMinBlockZ() + 2);
        List<StructurePiece> pieces = new ArrayList<>();
        pieces.add(new EndCityPieces.EndCityPiece(level.getStructureTemplateManager(), "base_floor", origin, Rotation.NONE, true));
        for (BlockPos offset : offsets)
            pieces.add(new EndCityPieces.EndCityPiece(level.getStructureTemplateManager(), "base_floor",
                    origin.offset(offset), Rotation.NONE, true));
        StructureStart start = new StructureStart(structure, chunk, 0, new PiecesContainer(pieces));
        level.getChunk(chunk.x(), chunk.z()).setStartForStructure(structure, start);
        BoundingBox box = start.getBoundingBox();
        long key = chunk.pack();
        for (int x = SectionPos.blockToSectionCoord(box.minX()); x <= SectionPos.blockToSectionCoord(box.maxX()); x++) {
            for (int z = SectionPos.blockToSectionCoord(box.minZ()); z <= SectionPos.blockToSectionCoord(box.maxZ()); z++) {
                ChunkAccess referenced = level.getChunk(x, z);
                referenced.addReferenceForStructure(structure, key);
            }
        }
        return new Registered(start, pieces.stream().map(StructurePiece::getBoundingBox).toList());
    }

    /** Extra ticks {@link #whenEntitiesTicking} waits for a fixture's chunks before failing. */
    static final int ENTITY_TICKING_WAIT_TICKS = 60;

    /**
     * A chunk only ticks entities once every chunk within two of it is fully loaded. {@link #register} loads the
     * structure's own chunks synchronously but leaves the surrounding fresh terrain to ticket-driven generation, which
     * can lag far behind; this loads that neighbourhood.
     */
    static void loadEntityTickingNeighbourhood(ServerLevel level, Registered... structures) {
        for (Registered structure : structures) {
            BoundingBox box = structure.start().getBoundingBox();
            for (int x = (box.minX() >> 4) - 2; x <= (box.maxX() >> 4) + 2; x++)
                for (int z = (box.minZ() >> 4) - 2; z <= (box.maxZ() >> 4) + 2; z++)
                    level.getChunk(x, z);
        }
    }

    /**
     * Runs {@code body} once every chunk of the given structures has loaded and ticking entities. Before that, an
     * entity added to such a chunk is hidden: entity queries miss it and its load event fires later. How many ticks
     * that takes depends on what else the server is loading, so a fixed delay is not enough.
     */
    static void whenEntitiesTicking(GameTestHelper ctx, Runnable body, Registered... structures) {
        whenEntitiesTicking(ctx, body, structures, 0);
    }

    private static void whenEntitiesTicking(GameTestHelper ctx, Runnable body, Registered[] structures, int waited) {
        ServerLevel level = ctx.getLevel();
        ChunkPos notReady = firstChunkWithoutTickingEntities(level, structures);
        if (notReady == null) {
            body.run();
            return;
        }
        ctx.assertTrue(waited < ENTITY_TICKING_WAIT_TICKS, "fixture chunk " + notReady + " did not reach ticking entities within "
                + ENTITY_TICKING_WAIT_TICKS + " ticks: entitiesLoaded=" + level.areEntitiesLoaded(notReady.pack())
                + " entityTickingRange=" + level.isPositionEntityTicking(notReady.getMiddleBlockPosition(70)));
        ctx.runAfterDelay(1, () -> whenEntitiesTicking(ctx, body, structures, waited + 1));
    }

    static ChunkPos firstChunkWithoutTickingEntities(ServerLevel level, Registered... structures) {
        for (Registered structure : structures) {
            BoundingBox box = structure.start().getBoundingBox();
            for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++)
                for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                    ChunkPos chunk = new ChunkPos(x, z);
                    if (!level.areEntitiesActuallyLoadedAndTicking(chunk)) return chunk;
                }
        }
        return null;
    }
}
