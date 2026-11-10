package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * TEMPORARY, EXPLICITLY IDENTIFIED INTEGRATION FIXTURE (owner decision, Phase 4): the only ST-01 structure asset in the repository
 * is the P8-A royal hall ({@code pyramid/royal_hall.nbt}, 39x20x39). It is NOT the complete ST-01 Pyramid. This adapter places that hall
 * at its approved pyramid-relative position so the approved placement parameters, eligibility rules and registration can be verified in
 * real worlds. The exterior shell, entrances, L1-L3 chambers, connections and vault do not exist and are not invented here.
 *
 * <p>Replacement contract: the finished pyramid supplies (a) its pieces in the pyramid reference frame (P1: world = (cx+X, groundY+Y,
 * cz+Z), anchor X=Z=0, Y=0 = lowest above-ground layer), and (b) its real underground volume. Only {@link #UNDERGROUND_VOLUME} and
 * {@link #addPieces} change; {@link St01PlacementConfig}, {@link St01Eligibility}, {@link St01PyramidStructure} and
 * {@link St01FootprintCorrectionPiece} stay as they are.
 */
public final class St01RoyalHallFixture {
    private St01RoyalHallFixture() {}

    public static final Identifier PIECE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st01_royal_hall_fixture");
    /** Hall reservation in the pyramid reference frame: X[-40,-2], Y[-24,-5], Z[2,40] (P3/P6/P8-A MIN_CORNER and SIZE 39x20x39). */
    public static final St01Eligibility.Volume UNDERGROUND_VOLUME = new St01Eligibility.Volume(-40, -2, -24, -5, 2, 40);

    /** Template-local position of the reference-frame origin: reference MIN_CORNER (-40,-24,2) maps to template (0,0,0). */
    static final BlockPos LOCAL_PIVOT = new BlockPos(40, 24, -2);

    /** Volume of the fixture after rotating about the anchor column, as offsets from the anchor (inclusive). */
    public static St01Eligibility.Volume volume(Rotation rotation) {
        BlockPos a = rotate(new BlockPos(-40, 0, 2), rotation), b = rotate(new BlockPos(-2, 0, 40), rotation);
        return new St01Eligibility.Volume(Math.min(a.getX(), b.getX()), Math.max(a.getX(), b.getX()),
                UNDERGROUND_VOLUME.minDy(), UNDERGROUND_VOLUME.maxDy(),
                Math.min(a.getZ(), b.getZ()), Math.max(a.getZ(), b.getZ()));
    }

    /** Rotation about the anchor column using the same transform as template placement. */
    public static BlockPos rotate(BlockPos reference, Rotation rotation) {
        return net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(
                reference, Mirror.NONE, rotation, BlockPos.ZERO);
    }

    static StructurePlaceSettings settings(Rotation rotation) {
        return St01RoyalChestPlacement.placementSettings(new StructurePlaceSettings()
                .setRotation(rotation).setMirror(Mirror.NONE).setRotationPivot(LOCAL_PIVOT).setIgnoreEntities(false));
    }

    /** World position of template-local (0,0,0) (before rotation) for an anchor at (cx, foundationY, cz). */
    static BlockPos templateOrigin(int cx, int foundationY, int cz) {
        return new BlockPos(cx - 40, foundationY - 24, cz + 2);
    }

    public static void addPieces(StructurePiecesBuilder builder, StructureTemplateManager templates, int cx, int foundationY, int cz,
            Rotation rotation) {
        builder.addPiece(new St01FootprintCorrectionPiece(cx, foundationY, cz));
        builder.addPiece(new Piece(templates, templateOrigin(cx, foundationY, cz), rotation));
    }

    public static final class Piece extends TemplateStructurePiece {
        Piece(StructureTemplateManager templates, BlockPos origin, Rotation rotation) {
            super(St01Worldgen.ROYAL_HALL_FIXTURE_PIECE, 0, templates, St01RoyalChestPlacement.TEMPLATE, St01RoyalChestPlacement.TEMPLATE.toString(),
                    settings(rotation), origin);
        }

        public Piece(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super(St01Worldgen.ROYAL_HALL_FIXTURE_PIECE, tag, ctx.structureTemplateManager(),
                    id -> settings(Rotation.valueOf(tag.getStringOr("St01Rot", "NONE"))));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
            super.addAdditionalSaveData(ctx, tag);
            tag.putString("St01Rot", getRotation().name());
        }

        @Override
        public Rotation getRotation() { return placeSettings.getRotation(); }

        @Override
        protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
            // The fixture template carries no data markers.
        }
    }
}
