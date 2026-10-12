package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.world.item.ItemStack;

/** Quantity-based K14 progression for one successful crafting-result take. */
public final class KnowledgeToolCraftProgress {
    private KnowledgeToolCraftProgress() {}

    public static int advance(int current, ItemStack crafted) {
        if (crafted == null || crafted.isEmpty()
                || !KnowledgeEligibleRosters.isEligibleVanillaTool(crafted.getItem())) return Math.max(0, current);
        return (int) Math.min(5L, Math.max(0, current) + (long) crafted.getCount());
    }
}
