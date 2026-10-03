package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Vanilla one-row layout with placement enforced before cursor/source stacks are split. */
final class FilteredChestMenu extends ChestMenu {
    FilteredChestMenu(int id, Inventory inventory, Container contents, int usableSlots) {
        super(MenuType.GENERIC_9x1, id, inventory, contents, 1);
        for (int index = 0; index < 9; index++) {
            final int containerSlot = index;
            Slot original = slots.get(index);
            Slot filtered = new Slot(contents, index, original.x, original.y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return containerSlot < usableSlots && contents.canPlaceItem(containerSlot, stack);
                }
                @Override public int getMaxStackSize(ItemStack stack) {
                    // Vanilla's merge phase does not call mayPlace for occupied slots.
                    return mayPlace(stack) ? super.getMaxStackSize(stack) : 0;
                }
                @Override public boolean isActive() { return containerSlot < usableSlots; }
            };
            filtered.index = original.index;
            slots.set(index, filtered);
        }
    }
}
