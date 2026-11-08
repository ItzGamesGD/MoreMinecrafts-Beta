package dev.itzgamesgd.moreminecrafts.st01;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Structure-relative geometry of the secret entrance, anchored at the royal chest block (P3 CHEST_PRIMARY).
 * The 12 cover cells are the P6-verified, user-approved cut (conditionally approved geometry, unchanged here): dx 12..15, dy -3, dz 0..2 for the
 * EAST variant; the WEST variant is the exact X mirror. D-3 (which side is real) is NOT decided: orientation is always an explicit parameter.
 * The stop point and golem spawn are PROPOSED candidates from P7/P8 (O-4: arrival zone Z2; O-6: BOSS_S-type spawn) and are not approved.
 */
public final class EntranceLayout {
    private EntranceLayout() {}

    public enum Orientation {
        EAST(1), WEST(-1);
        public final int sign;
        Orientation(int sign) { this.sign = sign; }
    }

    public static List<BlockPos> coverCells(BlockPos anchor, Orientation o) {
        List<BlockPos> cells = new ArrayList<>(12);
        for (int dx = 12; dx <= 15; dx++) for (int dz = 0; dz <= 2; dz++) cells.add(anchor.offset(o.sign * dx, -3, dz));
        return cells;
    }

    /** PROPOSED (O-4): zone Z2 cell, nearest centre-to-cut 1.58 blocks. */
    public static BlockPos stopPoint(BlockPos anchor, Orientation o) { return anchor.offset(o.sign * 10, -2, 3); }

    /** PROPOSED (O-6): BOSS_S-type emergence cell (hall floor, in front of the chest). */
    public static BlockPos golemSpawn(BlockPos anchor, Orientation o) { return anchor.offset(0, -2, -6); }
}
