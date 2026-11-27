package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import dev.itzgamesgd.moreminecrafts.pillagerhq.St03HeadquartersCompletion;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Registers ST-03 Pillager Headquarters structure type, assembly piece type, and completion/gameplay hooks.
 */
public final class St03Worldgen {
    private St03Worldgen() {}

    public static final Identifier STRUCTURE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st03_pillager_headquarters");
    public static final Identifier ASSEMBLY_PIECE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st03_assembly_piece");

    public static StructureType<St03HeadquartersStructure> STRUCTURE_TYPE;
    public static StructurePieceType ASSEMBLY_PIECE;
    public static StructurePieceType TERRAIN_PIECE;

    public static void initialize() {
        if (STRUCTURE_TYPE != null) {
            return;
        }
        STRUCTURE_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, STRUCTURE_ID, () -> St03HeadquartersStructure.CODEC);
        ASSEMBLY_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, ASSEMBLY_PIECE_ID,
                (StructurePieceType) St03AssemblyPiece::new);
        TERRAIN_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
                Identifier.fromNamespaceAndPath("moreminecrafts", "st03_terrain_correction"),
                (StructurePieceType) St03TerrainCorrectionPiece::new);
        St03HeadquartersCompletion.initialize();
    }
}
