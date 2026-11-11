package dev.itzgamesgd.moreminecrafts.pyramid;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * The one shared 54-slot inventory of a royal chest role. Contents live in world saved data (not in the chest block
 * entity), so chunk unloads, structure repair and block replacement cannot lose or duplicate them. It refuses every
 * insertion at the container level (hoppers, droppers, any code that honours {@link #canPlaceItem}); the menu adds
 * the player-side refusal.
 */
public final class RoyalChestContainer extends SimpleContainer {
    private final Runnable onChanged;
    private boolean quiet;

    RoyalChestContainer(Runnable onChanged) {
        super(RoyalChestRewardModel.SLOTS);
        this.onChanged = onChanged;
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }

    @Override public void setChanged() {
        super.setChanged();
        if (!quiet) onChanged.run();
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = super.removeItemNoUpdate(slot);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    /** Loads without notifying (used while restoring or generating, which persist explicitly). */
    void fill(int slot, ItemStack stack) {
        quiet = true;
        try { super.setItem(slot, stack); } finally { quiet = false; }
    }
}
