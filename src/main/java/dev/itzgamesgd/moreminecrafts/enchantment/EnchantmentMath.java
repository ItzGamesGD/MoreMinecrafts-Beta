package dev.itzgamesgd.moreminecrafts.enchantment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

/** Deterministic arithmetic kept separate from world mutation for regression testing. */
public final class EnchantmentMath {
    public static final int MAX_VEIN_CHUNKS = 4;
    public static final int MAX_TREE_HEIGHT = 34;
    private EnchantmentMath() {}

    public static int veinDurability(int destroyedBlocks, int level) {
        return level >= 2 ? (destroyedBlocks + 1) / 2 : destroyedBlocks;
    }

    public static int lumberDurability(int destroyedLogs, int unbreakingLevel) {
        return Math.max(0, (int) Math.ceil(destroyedLogs / (double) (unbreakingLevel + 1)));
    }

    public static boolean withinTreeHeight(int minY, int maxY) {
        return maxY - minY + 1 <= MAX_TREE_HEIGHT;
    }

    /**
     * Validates the trunk independently from the connected branches. The lowest log layer defines
     * a 1x1 or complete 2x2 trunk footprint; that footprint must continue vertically without gaps
     * for at least three logs and no more than 34 blocks. Logs outside the footprint are accepted
     * as branches only when they originate in the upper half of the validated trunk.
     *
     * Dense, broad log layers beyond the footprint are rejected as artificial log masses.
     */
    public static boolean isValidTreeStructure(Set<BlockPos> logs) {
        if (logs == null || logs.isEmpty()) return false;

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos log : logs) {
            minY = Math.min(minY, log.getY());
            maxY = Math.max(maxY, log.getY());
        }

        Set<Long> baseLayer = new HashSet<>();
        for (BlockPos log : logs) {
            if (log.getY() == minY) {
                baseLayer.add(column(log.getX(), log.getZ()));
                minX = Math.min(minX, log.getX()); maxX = Math.max(maxX, log.getX());
                minZ = Math.min(minZ, log.getZ()); maxZ = Math.max(maxZ, log.getZ());
            }
        }

        int footprintWidth = maxX - minX + 1;
        int footprintDepth = maxZ - minZ + 1;
        if (baseLayer.size() == 1) {
            // The origin log is the complete lower trunk cross-section.
        } else if (baseLayer.size() == 4 && footprintWidth == 2 && footprintDepth == 2) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!baseLayer.contains(column(x, z))) return false;
                }
            }
        } else {
            return false;
        }

        int trunkHeight = 0;
        boolean trunkEnded = false;
        Set<Long> footprint = new HashSet<>(baseLayer);
        for (int y = minY; y <= maxY; y++) {
            boolean layerHasTrunk = false;
            for (long col : footprint) {
                int x = (int) (col >> 32);
                int z = (int) col;
                if (contains(logs, x, y, z)) layerHasTrunk = true;
            }
            if (!trunkEnded && layerHasTrunk) {
                trunkHeight++;
            } else {
                trunkEnded = true;
                if (layerHasTrunk) return false; // no disconnected trunk restart above a gap
            }
        }
        if (trunkHeight < 3 || trunkHeight > MAX_TREE_HEIGHT) return false;

        int branchStartY = minY + (trunkHeight + 1) / 2;
        Set<BlockPos> branches = new HashSet<>();
        Map<Long, Set<Integer>> branchColumns = new HashMap<>();
        for (BlockPos log : logs) {
            if (!footprint.contains(column(log.getX(), log.getZ()))) {
                if (log.getY() < branchStartY) return false;
                branches.add(log);
                branchColumns.computeIfAbsent(column(log.getX(), log.getZ()), ignored -> new HashSet<>())
                        .add(log.getY());
            }
        }
        // A branch may angle upward, but a second long vertical column is another trunk, not a
        // branch extending beyond the validated trunk footprint.
        for (Set<Integer> heights : branchColumns.values()) {
            int longestRun = 0, currentRun = 0, previousY = Integer.MIN_VALUE;
            for (int y : heights.stream().sorted().toList()) {
                currentRun = y == previousY + 1 ? currentRun + 1 : 1;
                longestRun = Math.max(longestRun, currentRun);
                previousY = y;
            }
            if (longestRun > 2) return false;
        }

        // Broad, dense log sheets in any orientation are artificial walls/masses rather than
        // tree branches. Thin or sparse branches can extend beyond the trunk footprint.
        if (hasDensePlane(branches, 0) || hasDensePlane(branches, 1) || hasDensePlane(branches, 2)) {
            return false;
        }
        return true;
    }

    private static boolean hasDensePlane(Set<BlockPos> logs, int fixedAxis) {
        for (BlockPos fixed : logs) {
            int fixedValue = coordinate(fixed, fixedAxis);
            int minA = Integer.MAX_VALUE, maxA = Integer.MIN_VALUE;
            int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;
            int count = 0;
            int axisA = fixedAxis == 0 ? 1 : 0;
            int axisB = fixedAxis == 2 ? 1 : 2;
            for (BlockPos log : logs) {
                if (coordinate(log, fixedAxis) != fixedValue) continue;
                int a = coordinate(log, axisA), b = coordinate(log, axisB);
                minA = Math.min(minA, a); maxA = Math.max(maxA, a);
                minB = Math.min(minB, b); maxB = Math.max(maxB, b);
                count++;
            }
            int spanA = maxA - minA + 1;
            int spanB = maxB - minB + 1;
            if (spanA > 2 && spanB >= 2 && spanA * spanB >= 6
                    && count * 4 >= spanA * spanB * 3) return true;
        }
        return false;
    }

    private static int coordinate(BlockPos pos, int axis) {
        return axis == 0 ? pos.getX() : axis == 1 ? pos.getY() : pos.getZ();
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private static boolean contains(Set<BlockPos> logs, int x, int y, int z) {
        return logs.contains(new BlockPos(x, y, z));
    }

    public static int airJumps(int level) { return Math.max(0, Math.min(3, level)); }
}
