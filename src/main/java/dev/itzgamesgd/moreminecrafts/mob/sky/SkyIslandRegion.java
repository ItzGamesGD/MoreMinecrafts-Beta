package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerLayout;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerWorldgenRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;

/**
 * SKY-MOB §15.6 P3 (owner decision R1): sky-island <em>region</em> membership for natural spawning.
 *
 * <p>A position is inside the region when it lies in the bounding box of an actual saved Jigsaw piece
 * ({@code moreminecrafts:sky_islands/...}) of a generated sky-island start. The supporting block is not
 * judged here: it is left to the vanilla surface rule of each mob (Q79), so player-placed blocks count (Q80).
 * The region never extends past saved piece boxes. {@link SkyIslandHabitat}'s contract is unchanged.
 *
 * <p>Sky Tower interiors (Q33 B) are computed from the tower piece itself with the approved
 * {@link SkyTowerLayout} footprint, so the exclusion also holds before the tower site is registered.
 */
public final class SkyIslandRegion {
    private static final String TOWER = "sky_islands/(snowy|meadow|forest|badlands|desert|rocky)/tower";

    private SkyIslandRegion() {}

    public enum Kind { OUTSIDE, ISLAND, SKY_TOWER }

    public static boolean isIsland(ServerLevel level, BlockPos pos) {
        return classify(level, pos) == Kind.ISLAND;
    }

    public static boolean inSkyTower(ServerLevel level, BlockPos pos) {
        return classify(level, pos) == Kind.SKY_TOWER;
    }

    public static Kind classify(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD || !level.hasChunkAt(pos)) return Kind.OUTSIDE;
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        boolean island = false;
        // Only starts referenced by this loaded chunk; never scan regions or call locate.
        for (var start : level.structureManager().startsForStructure(pos.getX() >> 4, pos.getZ() >> 4,
                structure -> SkyIslandHabitat.isSkyStructure(registry.getKey(structure)))) {
            if (!start.isValid()) continue;
            for (var raw : start.getPieces()) {
                if (!(raw instanceof PoolElementStructurePiece piece)
                        || !(piece.getElement() instanceof SinglePoolElement single)) continue;
                Identifier id = single.getTemplateLocation();
                if (id == null || !id.getNamespace().equals("moreminecrafts")
                        || !id.getPath().startsWith("sky_islands/")) continue;
                if (id.getPath().matches(TOWER) && insideTower(piece, level, pos)) return Kind.SKY_TOWER;
                if (piece.getBoundingBox().isInside(pos)) island = true;
            }
        }
        return island ? Kind.ISLAND : Kind.OUTSIDE;
    }

    private static boolean insideTower(PoolElementStructurePiece piece, ServerLevel level, BlockPos pos) {
        var origin = SkyTowerWorldgenRegistration.towerOrigin(piece.getPosition(), piece.getRotation());
        var site = new SkyTowerLayout.Site(level.dimension().identifier().toString(), origin.getX(), origin.getY(),
                origin.getZ(), SkyTowerWorldgenRegistration.rotationIndex(piece.getRotation()));
        int[] b = site.bounds();
        return pos.getX() >= b[0] && pos.getX() <= b[3] && pos.getY() >= b[1] && pos.getY() <= b[4]
                && pos.getZ() >= b[2] && pos.getZ() <= b[5];
    }
}
