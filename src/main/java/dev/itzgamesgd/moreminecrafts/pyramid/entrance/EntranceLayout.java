package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Secret-entrance geometry of one placed ST-01 pyramid, in the approved P8-C2-A authored frame (ground-centre origin,
 * authored entrance facing SOUTH) and mapped to the world with the very same transform as every assembly piece
 * ({@link St01PyramidAssembly#worldPos}). There is no separate EAST/WEST entrance mirror: G0-R1 bundle 2 makes both access
 * routes share one cover, one seven-tread descent and one landing, so the layout depends only on ground and orientation.
 *
 * <p>Authored values are the P6/P11 anchor-relative values re-expressed against the D1 PRIMARY anchor (-21,-20,33):
 * <ul>
 *   <li><b>Cover</b>: the 12 separately governed Y-23 cells X-9..-6 / Z33..35 (P6 cut dx 12..15, dy -3, dz 0..2; identical
 *       to {@code tools/st01-p8c2a/export_assembly.py} {@code cover_cells}). Opening removes exactly these 12 cells and
 *       nothing else (G0-R1 VALIDATION: "removes only the separate 12 cover cells, not a larger opening").</li>
 *   <li><b>Stop point</b> (O-4, P11): anchor + (9,-2,3) = (-12,-22,36).</li>
 *   <li><b>Golem spawn</b> (O-6 {@code BOSS_S}, P11): anchor + (0,-2,-6) = (-21,-22,27).</li>
 * </ul>
 */
public final class EntranceLayout {
    private EntranceLayout() {}

    public static final BlockPos AUTHORED_ANCHOR = St01RoyalChestPlacement.REFERENCE_ANCHOR;
    public static final int COVER_MIN_X = -9, COVER_MAX_X = -6, COVER_Y = -23, COVER_MIN_Z = 33, COVER_MAX_Z = 35;
    public static final BlockPos AUTHORED_STOP = AUTHORED_ANCHOR.offset(9, -2, 3);
    public static final BlockPos AUTHORED_GOLEM_SPAWN = AUTHORED_ANCHOR.offset(0, -2, -6);

    public static List<BlockPos> authoredCover() {
        List<BlockPos> cells = new ArrayList<>(12);
        for (int x = COVER_MIN_X; x <= COVER_MAX_X; x++)
            for (int z = COVER_MIN_Z; z <= COVER_MAX_Z; z++) cells.add(new BlockPos(x, COVER_Y, z));
        return cells;
    }

    public static BlockPos tombAnchor(BlockPos ground, St01PyramidAssembly.Orientation o) { return St01PyramidAssembly.worldPos(AUTHORED_ANCHOR, ground, o); }

    public static List<BlockPos> coverCells(BlockPos ground, St01PyramidAssembly.Orientation o) {
        List<BlockPos> cells = new ArrayList<>(12);
        for (BlockPos p : authoredCover()) cells.add(St01PyramidAssembly.worldPos(p, ground, o));
        return cells;
    }

    public static BlockPos stopPoint(BlockPos ground, St01PyramidAssembly.Orientation o) { return St01PyramidAssembly.worldPos(AUTHORED_STOP, ground, o); }

    public static BlockPos golemSpawn(BlockPos ground, St01PyramidAssembly.Orientation o) { return St01PyramidAssembly.worldPos(AUTHORED_GOLEM_SPAWN, ground, o); }
}
