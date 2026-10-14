package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LanternSlotContainer extends SimpleContainer {
    private final Player player;
    public LanternSlotContainer(Player player) { super(1); this.player = player; }
    @Override public ItemStack getItem(int slot) { return player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack current = getItem(0); ItemStack removed = current.split(count); setItem(0, current); return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(0); setItem(0, ItemStack.EMPTY); return current;
    }
    @Override public void setItem(int slot, ItemStack stack) { player.setAttached(EquipmentAttachments.LANTERN, stack); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.is(ModItems.MINER_LANTERN); }
    @Override public int getMaxStackSize() { return 1; }
}
