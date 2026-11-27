package dev.itzgamesgd.moreminecrafts.pyramid;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * ST-01 complete pyramid assembly (P8-C2-A). Owner-approved G0-R1 blueprint c26ea0f8, exported by
 * {@code tools/st01-p8c2a/export_assembly.py}. Three pieces always placed in this order with one shared transform:
 * the route-specific assembly, the original D1 royal hall verbatim, then the 12-cell R1-01 seam exception.
 *
 * <p>Controlled placement and metadata only: no natural worldgen, no route-selection policy, no automatic royal-chest
 * registration. Callers that need the P8-B2 runtime call {@link St01RoyalChestPlacement#registerPlaced} with
 * {@link Placement#d1PlacementOrigin()} and {@link Placement#d1Settings()} after {@link #place} succeeds.
 */
public final class St01PyramidAssembly {
    /** Authored inclusive completion bounds relative to the ground-centre origin. */
    public static final BlockPos COMPLETION_MIN = new BlockPos(-40, -32, -40);
    public static final BlockPos COMPLETION_MAX = new BlockPos(40, 39, 40);
    public static final BlockPos SEAM_MIN = new BlockPos(-8, -24, 33);
    public static final Identifier SEAM_TEMPLATE = Identifier.parse("moreminecrafts:pyramid/st01/d1_seam_exception");
    /** Treasury UI anchor: an empty vanilla chest, never royal SECONDARY (approved bundle 3 representation). */
    public static final BlockPos TREASURY_UI_ANCHOR = new BlockPos(25, -26, 26);

    private St01PyramidAssembly() {}

    /** Direction the authored main entrance faces after rotation. Authored entrance faces SOUTH. */
    public enum Orientation {
        NORTH(Rotation.CLOCKWISE_180), EAST(Rotation.COUNTERCLOCKWISE_90), SOUTH(Rotation.NONE), WEST(Rotation.CLOCKWISE_90);

        public final Rotation rotation;
        Orientation(Rotation rotation) { this.rotation = rotation; }
    }

    /** Secret access route. Treasury, crypt and D1 stay fixed; only the route geometry differs. Selection is C2-B's. */
    public enum AccessRoute {
        EAST("moreminecrafts:pyramid/st01/assembly_east"), WEST("moreminecrafts:pyramid/st01/assembly_west");

        public final Identifier template;
        AccessRoute(String id) { this.template = Identifier.parse(id); }
    }

    /** One placement piece: its template and the authored position of its local (0,0,0). */
    public record Piece(String name, Identifier template, BlockPos authoredMinCorner, boolean royalHall) {
        public BlockPos placementOrigin(BlockPos ground) { return ground.offset(authoredMinCorner); }

        public StructurePlaceSettings settings(Orientation orientation) {
            var settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setRotation(orientation.rotation)
                    .setRotationPivot(BlockPos.ZERO.subtract(authoredMinCorner));
            return royalHall ? St01RoyalChestPlacement.placementSettings(settings) : settings;
        }
    }

    public static List<Piece> pieces(AccessRoute route) {
        return List.of(new Piece("ASSEMBLY", route.template, COMPLETION_MIN, false),
                new Piece("D1_ROYAL_HALL", St01RoyalChestPlacement.TEMPLATE, St01RoyalChestPlacement.REFERENCE_ORIGIN, true),
                new Piece("D1_SEAM_EXCEPTION", SEAM_TEMPLATE, SEAM_MIN, false));
    }

    /** world(p) = ground + R(p), identical to every piece's template transform. */
    public static BlockPos worldPos(BlockPos authored, BlockPos ground, Orientation orientation) {
        return StructureTemplate.transform(authored, Mirror.NONE, orientation.rotation, BlockPos.ZERO).offset(ground);
    }

    public static BoundingBox completionBox(BlockPos ground, Orientation orientation) {
        return BoundingBox.fromCorners(worldPos(COMPLETION_MIN, ground, orientation), worldPos(COMPLETION_MAX, ground, orientation));
    }

    /** Dimension-independent metadata of one placed instance, as consumed by the P8-B2 royal-chest runtime. */
    public record Placement(BlockPos ground, Orientation orientation, AccessRoute route, BlockPos d1PlacementOrigin,
                            StructurePlaceSettings d1Settings, BlockPos primaryLeft, BlockPos primaryRight,
                            BlockPos guardianSpawn, BlockPos treasuryUiAnchor, BoundingBox completionBounds) {}

    public static Placement describe(BlockPos ground, Orientation orientation, AccessRoute route) {
        Piece d1 = pieces(route).get(1);
        return new Placement(ground, orientation, route, d1.placementOrigin(ground), d1.settings(orientation),
                worldPos(St01RoyalChestPlacement.REFERENCE_ANCHOR, ground, orientation),
                worldPos(St01RoyalChestPlacement.REFERENCE_RIGHT, ground, orientation),
                worldPos(St01RoyalChestPlacement.REFERENCE_SPAWN, ground, orientation),
                worldPos(TREASURY_UI_ANCHOR, ground, orientation), completionBox(ground, orientation));
    }

    /** Places all three pieces in the approved order. Requires every chunk of {@link #completionBox} to be loaded. */
    public static boolean place(ServerLevelAccessor level, StructureTemplateManager templates, BlockPos ground,
                                Orientation orientation, AccessRoute route, RandomSource random, int flags) {
        for (Piece piece : pieces(route)) {
            StructureTemplate template = templates.get(piece.template()).orElseThrow(
                    () -> new IllegalStateException("missing ST-01 template " + piece.template()));
            BlockPos origin = piece.placementOrigin(ground);
            if (!template.placeInWorld(level, origin, origin, piece.settings(orientation), random, flags)) return false;
        }
        return true;
    }
}
