package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** InventoryMenu adapter over the player's persistent, server-authoritative equipment attachment. */
public final class AccessoryEquipmentContainer extends SimpleContainer {
    private final Player player;

    public AccessoryEquipmentContainer(Player player) {
        super(AccessoryLoadout.SIZE);
        this.player = player;
    }

    private AccessoryLoadout loadout() {
        return player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
    }

    @Override public ItemStack getItem(int slot) {
        AccessoryLoadout.checkSlot(slot);
        return loadout().get(slot);
    }

    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack current = getItem(slot);
        if (current.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack removed = current.copyWithCount(Math.min(count, current.getCount()));
        setItem(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        if (!current.isEmpty()) setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        AccessoryLoadout.checkSlot(slot);
        ItemStack safeStack = stack == null ? ItemStack.EMPTY : stack;
        if (!safeStack.isEmpty() && !AccessorySlotRules.accepts(slot, safeStack)) return;
        player.setAttached(EquipmentAttachments.ACCESSORIES, loadout().with(slot, safeStack));
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return AccessorySlotRules.accepts(slot, stack);
    }

    @Override public int getMaxStackSize() { return 1; }

    @Override public void setChanged() {
        // Every write replaces the immutable attachment and triggers persistence/synchronization.
    }
}
