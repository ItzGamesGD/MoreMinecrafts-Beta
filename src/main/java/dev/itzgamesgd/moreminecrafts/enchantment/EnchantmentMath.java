package dev.itzgamesgd.moreminecrafts.enchantment;

/** Deterministic arithmetic kept separate from world mutation for regression testing. */
public final class EnchantmentMath {
    public static final int MAX_VEIN_BLOCKS = 16 * 16 * 4;
    public static final int MAX_TREE_HEIGHT = 34;
    private EnchantmentMath() {}

    public static int veinExtraDurability(int additionalBlocks, int level) {
        return level >= 2 ? (additionalBlocks + 1) / 2 : additionalBlocks;
    }

    public static int lumberDurability(int destroyedLogs, int unbreakingLevel) {
        return Math.max(0, (int) Math.ceil(destroyedLogs / (double) (unbreakingLevel + 1)));
    }

    public static int airJumps(int level) { return Math.max(0, Math.min(3, level)); }
}
