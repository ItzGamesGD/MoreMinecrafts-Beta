package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RouteSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;

/** Pure per-candidate decision shared by {@link St01PyramidStructure}, unit tests and the validation probe. */
public final class St01Candidate {
    private St01Candidate() {}

    /** Orientation whose rotation is {@code rotation} (P8-C2-A mapping: SOUTH=NONE, WEST=CW90, NORTH=CW180, EAST=CCW90). */
    public static Orientation orientationOf(Rotation rotation) {
        for (Orientation o : Orientation.values()) if (o.rotation == rotation) return o;
        throw new IllegalArgumentException(rotation.name());
    }

    /** Column offsets of the row directly in front of the main entrance, rotated about the anchor column. */
    public static List<int[]> entranceApproach(Orientation orientation) {
        List<int[]> row = new ArrayList<>();
        for (int x = -St01PlacementConfig.ENTRANCE_HALF_WIDTH; x <= St01PlacementConfig.ENTRANCE_HALF_WIDTH; x++) {
            BlockPos p = St01PyramidAssembly.worldPos(new BlockPos(x, 0, St01PlacementConfig.ENTRANCE_APPROACH_Z), BlockPos.ZERO, orientation);
            row.add(new int[]{p.getX(), p.getZ()});
        }
        return row;
    }

    /** Pure decision for one candidate chunk: shared by production and the validation probe. */
    public record Decision(St01Eligibility.Result result, int centerX, int centerZ, Orientation orientation,
                           Optional<BlockPos> ground, Optional<AccessRoute> route) {}

    public static Decision decide(St01Eligibility.Terrain terrain, ChunkPos chunk, Rotation rotation, long worldSeed) {
        int cx = chunk.getMinBlockX() + 8, cz = chunk.getMinBlockZ() + 8;
        Orientation orientation = orientationOf(rotation);
        St01Eligibility.Result result = St01Eligibility.evaluate(terrain, cx, cz, St01PlacementConfig.UNDERGROUND_VOLUME,
                entranceApproach(orientation));
        if (!result.eligible()) return new Decision(result, cx, cz, orientation, Optional.empty(), Optional.empty());
        BlockPos ground = new BlockPos(cx, result.foundationY(), cz);
        BlockPos tombAnchor = St01PyramidAssembly.worldPos(St01RoyalChestPlacement.REFERENCE_ANCHOR, ground, orientation);
        return new Decision(result, cx, cz, orientation, Optional.of(ground), Optional.of(St01RouteSelection.select(worldSeed, tombAnchor)));
    }

}
