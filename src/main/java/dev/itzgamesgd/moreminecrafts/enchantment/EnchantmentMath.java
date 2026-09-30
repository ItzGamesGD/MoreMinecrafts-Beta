package dev.itzgamesgd.moreminecrafts.enchantment;

import java.util.HashSet;
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
        for (BlockPos log : logs) {
            if (!footprint.contains(column(log.getX(), log.getZ())) && log.getY() < branchStartY) {
                return false;
            }
        }

        // A tree canopy has sparse, branching logs. A filled broad horizontal layer is a log
        // wall/mass, not a branch extending beyond the trunk cross-section.
        for (int y = minY; y <= maxY; y++) {
            int layerMinX = Integer.MAX_VALUE, layerMaxX = Integer.MIN_VALUE;
            int layerMinZ = Integer.MAX_VALUE, layerMaxZ = Integer.MIN_VALUE;
            int count = 0;
            for (BlockPos log : logs) {
                if (log.getY() != y) continue;
                count++;
                layerMinX = Math.min(layerMinX, log.getX()); layerMaxX = Math.max(layerMaxX, log.getX());
                layerMinZ = Math.min(layerMinZ, log.getZ()); layerMaxZ = Math.max(layerMaxZ, log.getZ());
            }
            int width = layerMaxX - layerMinX + 1;
            int depth = layerMaxZ - layerMinZ + 1;
            if (width > 2 && depth > 2 && count * 4 >= width * depth * 3) return false;
        }
        return true;
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private static boolean contains(Set<BlockPos> logs, int x, int y, int z) {
        return logs.contains(new BlockPos(x, y, z));
    }

    public static int airJumps(int level) { return Math.max(0, Math.min(3, level)); }
}
