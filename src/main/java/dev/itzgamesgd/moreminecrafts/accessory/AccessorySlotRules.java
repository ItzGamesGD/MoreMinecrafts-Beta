package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.world.item.ItemStack;

/** Shared item and category validation used by storage, menus and shift-click handling. */
public final class AccessorySlotRules {
    private AccessorySlotRules() {}

    public static boolean accepts(int slot, ItemStack stack) {
        if (slot < 0 || slot >= AccessoryLoadout.SIZE || stack == null || stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }
        return stack.getItem() instanceof AccessoryItem accessory
                && accessory.definition() != null
                && accessory.category().ordinal() == slot;
    }

    public static int slotFor(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1
                || !(stack.getItem() instanceof AccessoryItem accessory)) return -1;
        int slot = accessory.category().ordinal();
        return accepts(slot, stack) ? slot : -1;
    }
}
