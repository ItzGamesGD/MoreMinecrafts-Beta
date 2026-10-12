package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.resources.Identifier;

/** Quantity-based K14 progression for one successful crafting-result take. */
public final class KnowledgeToolCraftProgress {
    private KnowledgeToolCraftProgress() {}

    public static int advance(int current, Identifier itemId, int quantity) {
        if (quantity <= 0 || !KnowledgeEligibleRosters.isEligibleVanillaToolId(itemId)) return Math.max(0, current);
        return (int) Math.min(5L, Math.max(0, current) + (long) quantity);
    }
}
