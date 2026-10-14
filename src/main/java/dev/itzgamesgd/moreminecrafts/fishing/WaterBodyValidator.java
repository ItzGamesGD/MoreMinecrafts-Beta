package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

/** Deterministic, terrain-tolerant custom-fishing water eligibility check. */
public final class WaterBodyValidator {
    public static final int RADIUS = 16;
    public static final int REQUIRED_CONNECTED_SURFACE = 384;
    public static final int REQUIRED_DEEP_COLUMNS = 64;
    public static final int REQUIRED_DEPTH = 8;

    @FunctionalInterface public interface WaterSample { boolean isWater(int x, int depth, int z); }
    private WaterBodyValidator() {}

    public static boolean isEligible(Level level, BlockPos center) {
        int surfaceY = center.getY();
        while (surfaceY > level.getMinY() && !level.getFluidState(new BlockPos(center.getX(), surfaceY, center.getZ())).is(FluidTags.WATER))
            surfaceY--;
        final int y = surfaceY;
        return isEligible((x, depth, z) -> level.getFluidState(
                new BlockPos(center.getX() + x, y - depth, center.getZ() + z)).is(FluidTags.WATER));
    }

    public static boolean isEligible(WaterSample sample) {
        if (!sample.isWater(0, 0, 0)) return false;
        record Cell(int x, int z) {}
        ArrayDeque<Cell> open = new ArrayDeque<>();
        Set<Cell> visited = new HashSet<>();
        Set<Cell> connected = new HashSet<>();
        open.add(new Cell(0, 0));
        int deep = 0;
        while (!open.isEmpty()) {
            Cell cell = open.removeFirst();
            if (Math.abs(cell.x) > RADIUS || Math.abs(cell.z) > RADIUS || !visited.add(cell)
                    || !sample.isWater(cell.x, 0, cell.z)) continue;
            connected.add(cell);
            boolean deepColumn = true;
            for (int depth = 1; depth < REQUIRED_DEPTH; depth++)
                deepColumn &= sample.isWater(cell.x, depth, cell.z);
            if (deepColumn) deep++;
            open.add(new Cell(cell.x + 1, cell.z)); open.add(new Cell(cell.x - 1, cell.z));
            open.add(new Cell(cell.x, cell.z + 1)); open.add(new Cell(cell.x, cell.z - 1));
        }
        return connected.size() >= REQUIRED_CONNECTED_SURFACE && deep >= REQUIRED_DEEP_COLUMNS;
    }
}
