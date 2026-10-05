package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.world.entity.player.Player;

/** Level-based XP payment for operations that charge whole Minecraft XP levels. */
public final class XpLevels {
    public static boolean canPay(Player player, int levelCost) {
        return levelCost >= 0 && (player.hasInfiniteMaterials() || player.experienceLevel >= levelCost);
    }

    public static boolean pay(Player player, int levelCost) {
        if (!canPay(player, levelCost)) return false;
        if (!player.hasInfiniteMaterials()) player.giveExperienceLevels(-levelCost);
        return true;
    }

    private XpLevels() {}
}
