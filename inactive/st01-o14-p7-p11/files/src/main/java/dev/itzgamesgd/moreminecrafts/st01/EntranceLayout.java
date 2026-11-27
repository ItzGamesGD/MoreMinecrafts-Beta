package dev.itzgamesgd.moreminecrafts.st01;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Structure-relative geometry of the secret entrance, anchored at the royal chest block (P3 CHEST_PRIMARY).
 * The 12 cover cells are the P6-verified, user-approved cut (conditionally approved geometry, unchanged here): dx 12..15, dy -3, dz 0..2 for the
 * EAST variant; the WEST variant is the exact X mirror. D-3 (which side is real) is NOT decided: orientation is always an explicit parameter.
 *
 * <p>Owner-approved ST-01 P11 coordinates (2026-11-08):
 * <ul>
 *   <li><b>O-4 stop point</b>: P10-verified safe Z3 candidate {@code C03} ({@code delta = (-1, 0)} from P9 Z2 reference,
 *       {@code anchor.offset(o.sign * 9, -2, 3)}, cell-center distance {@code 2.550} blocks to nearest cover cell,
 *       minimum AABB clearance {@code >= 3.324} blocks across all E/W and {@code BOSS_S}/{@code DOOR_IN} trials).</li>
 *   <li><b>O-6 golem spawn</b>: {@code BOSS_S} emergence cell ({@code anchor.offset(0, -2, -6)}, model {@code (0, -22, 27)}).</li>
 * </ul>
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

    /** APPROVED (O-4, ST-01 P11): P10-verified safe Z3 cell {@code C03} ({@code delta = (-1, 0)}, nearest centre-to-cut 2.550 blocks). */
    public static BlockPos stopPoint(BlockPos anchor, Orientation o) { return anchor.offset(o.sign * 9, -2, 3); }

    /** APPROVED (O-6, ST-01 P11): {@code BOSS_S} emergence cell (hall floor {@code (0, -22, 27)}, 6 blocks north of the primary chest). */
    public static BlockPos golemSpawn(BlockPos anchor, Orientation o) { return anchor.offset(0, -2, -6); }
}
