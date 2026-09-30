package dev.itzgamesgd.moreminecrafts.enchantment;

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

    public static boolean withinTreeEnvelope(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return maxX - minX + 1 <= 2 && maxZ - minZ + 1 <= 2 && withinTreeHeight(minY, maxY);
    }

    public static int airJumps(int level) { return Math.max(0, Math.min(3, level)); }
}
