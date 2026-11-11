package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Registers the ST-01 structure type, its piece types and the completion handoff. Structure and structure-set data live in
 * {@code data/moreminecrafts/worldgen}.
 */
public final class St01Worldgen {
    private St01Worldgen() {}

    public static final Identifier STRUCTURE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st01_pyramid");
    public static final Identifier FOOTPRINT_CORRECTION_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st01_footprint_correction");
    public static final Identifier ASSEMBLY_PIECE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st01_assembly_piece");

    public static StructureType<St01PyramidStructure> STRUCTURE_TYPE;
    public static StructurePieceType FOOTPRINT_CORRECTION_PIECE;
    public static StructurePieceType ASSEMBLY_PIECE;

    public static void initialize() {
        if (STRUCTURE_TYPE != null) return;
        STRUCTURE_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, STRUCTURE_ID, () -> St01PyramidStructure.CODEC);
        FOOTPRINT_CORRECTION_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, FOOTPRINT_CORRECTION_ID,
                (StructurePieceType) St01FootprintCorrectionPiece::new);
        ASSEMBLY_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, ASSEMBLY_PIECE_ID,
                (StructurePieceType) St01AssemblyPiece::new);
        St01PyramidCompletion.initialize();
    }
}
