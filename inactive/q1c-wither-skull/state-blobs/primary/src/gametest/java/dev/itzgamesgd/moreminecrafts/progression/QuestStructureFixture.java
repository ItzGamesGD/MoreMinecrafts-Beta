package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
}
