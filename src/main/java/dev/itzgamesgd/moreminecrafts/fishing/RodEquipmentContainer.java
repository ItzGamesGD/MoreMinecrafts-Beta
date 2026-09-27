package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class RodEquipmentContainer extends SimpleContainer {
    private final ItemStack rod;

    public RodEquipmentContainer(ItemStack rod) {
        super(3);
        this.rod = rod;
        RodEquipment equipment = rod.getOrDefault(ModDataComponents.ROD_EQUIPMENT, RodEquipment.EMPTY);
        for (int i = 0; i < 3; i++) super.setItem(i, equipment.slot(i).copy());
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < 3
                && stack.getItem() instanceof FishingEquipmentItem item
                && item.family().ordinal() == slot;
    }

    @Override public void setChanged() {
        super.setChanged();
        rod.set(ModDataComponents.ROD_EQUIPMENT,
                new RodEquipment(getItem(0), getItem(1), getItem(2)));
    }
}
