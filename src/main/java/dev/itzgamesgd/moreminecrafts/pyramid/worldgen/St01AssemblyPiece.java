package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * One of the three approved P8-C2-A placement pieces (ASSEMBLY, D1_ROYAL_HALL, D1_SEAM_EXCEPTION) as a natural-worldgen
 * piece. Template, origin, rotation, pivot and mirror come unchanged from {@link St01PyramidAssembly#pieces}; the D1 piece
 * keeps {@code St01RoyalChestPlacement.placementSettings}. The structure start lists the three pieces in the approved order,
 * and vanilla places each piece clipped to the chunk being decorated, so every chunk writes only its own cells.
 *
 * <p>Two worldgen-only settings, neither of which changes the transform used by the royal-chest runtime:
 * <ul>
 *   <li>{@code knownShape = true}: the exported states are placed exactly as authored. Without it, vanilla re-derives the
 *       shape of every placed block from its neighbours at the clipped chunk edge, which reads the neighbouring chunk's
 *       current contents and therefore depends on generation order.</li>
 *   <li>{@code IGNORE_WATERLOGGING}: an authored state never picks up stray fluid from the terrain it replaces.</li>
 * </ul>
 */
public final class St01AssemblyPiece extends TemplateStructurePiece {
    private final int pieceIndex;
    private final BlockPos ground;
    private final Orientation orientation;
    private final AccessRoute route;

    public St01AssemblyPiece(StructureTemplateManager templates, BlockPos ground, Orientation orientation, AccessRoute route, int pieceIndex) {
        super(St01Worldgen.ASSEMBLY_PIECE, 0, templates, piece(route, pieceIndex).template(), piece(route, pieceIndex).template().toString(),
                settings(orientation, route, pieceIndex), piece(route, pieceIndex).placementOrigin(ground));
        this.pieceIndex = pieceIndex;
        this.ground = ground.immutable();
        this.orientation = orientation;
        this.route = route;
    }

    public St01AssemblyPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(St01Worldgen.ASSEMBLY_PIECE, tag, ctx.structureTemplateManager(),
                id -> settings(orientation(tag), route(tag), tag.getIntOr("St01Piece", 0)));
        this.pieceIndex = tag.getIntOr("St01Piece", 0);
        this.ground = new BlockPos(tag.getIntOr("St01GX", 0), tag.getIntOr("St01GY", 0), tag.getIntOr("St01GZ", 0));
        this.orientation = orientation(tag);
        this.route = route(tag);
    }

    private static Orientation orientation(CompoundTag tag) { return Orientation.valueOf(tag.getStringOr("St01Orientation", "SOUTH")); }

    private static AccessRoute route(CompoundTag tag) { return AccessRoute.valueOf(tag.getStringOr("St01Route", "EAST")); }

    static St01PyramidAssembly.Piece piece(AccessRoute route, int index) { return St01PyramidAssembly.pieces(route).get(index); }

    /** The approved P8-C2-A settings plus the two worldgen-only settings described in the class comment. */
    public static StructurePlaceSettings settings(Orientation orientation, AccessRoute route, int index) {
        return piece(route, index).settings(orientation).setKnownShape(true).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putInt("St01Piece", pieceIndex);
        tag.putInt("St01GX", ground.getX());
        tag.putInt("St01GY", ground.getY());
        tag.putInt("St01GZ", ground.getZ());
        tag.putString("St01Orientation", orientation.name());
        tag.putString("St01Route", route.name());
    }

    public int pieceIndex() { return pieceIndex; }
    public BlockPos ground() { return ground; }
    public Orientation orientation() { return orientation; }
    public AccessRoute route() { return route; }
    public String pieceName() { return piece(route, pieceIndex).name(); }

    @Override
    public Rotation getRotation() { return placeSettings.getRotation(); }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos reference) {
        St01WorldgenGuard.owned(() -> super.postProcess(level, structures, generator, random, chunkBox, chunkPos, reference));
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // The exported templates carry no data markers.
    }
}
