package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * The ST-01 royal hall (boss room). Natural monster spawning is blocked inside this piece's box only; the rest of the
 * pyramid follows vanilla spawning (owner decision 2026-11-14).
 */
public final class St01RoyalHallRegion {
    private static final ResourceKey<Structure> ST01 =
            ResourceKey.create(Registries.STRUCTURE, ModIds.id("st01_pyramid"));

    private St01RoyalHallRegion() {}

    public static boolean inRoyalHall(ServerLevel level, BlockPos pos) {
        StructureManager structures = level.structureManager();
        StructureStart start = structures.getStructureWithPieceAt(pos, holder -> holder.is(ST01));
        if (start == null || !start.isValid()) return false;
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof St01AssemblyPiece assembly && assembly.isRoyalHall()
                    && assembly.getBoundingBox().isInside(pos)) {
                return true;
            }
        }
        return false;
    }
}
