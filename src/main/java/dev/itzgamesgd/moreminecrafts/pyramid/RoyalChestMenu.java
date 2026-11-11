package dev.itzgamesgd.moreminecrafts.pyramid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Withdraw-only 6x9 view of a {@link RoyalChestContainer}. Every chest slot refuses placement, which closes click,
 * drag, hotbar-swap and offhand-swap insertion; shift-click only ever moves chest stacks to the player's inventory.
 */
public final class RoyalChestMenu extends AbstractContainerMenu {
    static final int CHEST_SLOTS = RoyalChestRewardModel.SLOTS;
    private final Container container;
    private final ServerLevel level;
    private final BlockPos pos;

    RoyalChestMenu(int containerId, Inventory playerInventory, Container container, ServerLevel level, BlockPos pos) {
        super(MenuType.GENERIC_9x6, containerId);
        this.container = container;
        this.level = level;
        this.pos = pos;
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new WithdrawOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
        addStandardInventorySlots(playerInventory, 8, 140);
    }

    static final class WithdrawOnlySlot extends Slot {
        WithdrawOnlySlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= CHEST_SLOTS) return ItemStack.EMPTY; // never inventory -> chest
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, CHEST_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return RoyalChestRuntime.isGuarded(level, pos)
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    public Container chestContainer() { return container; }
}
