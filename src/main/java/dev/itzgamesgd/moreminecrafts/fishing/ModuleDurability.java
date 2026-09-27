package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.world.item.ItemStack;

/** Applies module wear without destroying the repairable broken item. */
public final class ModuleDurability {
    private ModuleDurability() {}

    public static boolean isActive(ItemStack stack, FishingEquipmentFamily family) {
        return stack.getItem() instanceof FishingEquipmentItem item
                && item.family() == family && remaining(stack) > 1;
    }

    public static int remaining(ItemStack stack) {
        return stack.isEmpty() || !stack.isDamageableItem() ? 0 : stack.getMaxDamage() - stack.getDamageValue();
    }

    public static boolean wear(ItemStack stack, FishingEquipmentFamily family) {
        if (!isActive(stack, family)) return false;
        stack.setDamageValue(stack.getMaxDamage() - remainingAfterWear(remaining(stack)));
        return true;
    }

    public static int remainingAfterWear(int remaining) { return Math.max(1, remaining - 1); }
}
