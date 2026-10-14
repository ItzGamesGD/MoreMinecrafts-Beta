package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shared recipe authorization used by result generation and server-side crafting. */
public final class ProgressionRecipeLocks {
    private ProgressionRecipeLocks() {}
    public static boolean allowed(ServerPlayer player, RecipeHolder<?> recipe) {
        if (recipe == null) return true;
        var id = recipe.id().identifier();
        if (!id.getNamespace().equals("moreminecrafts")) return true;
        String path = id.getPath();
        if (path.startsWith("knowledge/knowledge")) {
            String tail = path.substring("knowledge/knowledge".length());
            if (tail.length() < 2 || !Character.isDigit(tail.charAt(0)) || !Character.isDigit(tail.charAt(1)))
                return false;
            return KnowledgeRuntime.knows(player, Integer.parseInt(tail.substring(0,2)));
        }
        // Stable future format: research/<research_id>/level_<required-level>/...
        if (path.startsWith("research/")) {
            String[] parts = path.split("/");
            if (parts.length < 3 || !parts[2].startsWith("level_")) return false;
            try {
                int requiredLevel = Integer.parseInt(parts[2].substring("level_".length()));
                return requiredLevel > 0
                        && KnowledgeRuntime.state(player).researchLevel(parts[1]) >= requiredLevel;
            } catch (NumberFormatException malformed) {
                return false;
            }
        }
        return true;
    }
}
