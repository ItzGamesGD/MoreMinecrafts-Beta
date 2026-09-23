package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

/** Server-side slots use the vanilla one-row screen type, with rod-specific slot rules. */
public final class FishingRodMenu extends AbstractContainerMenu {
    private static final int ROD_SLOTS = 9;
    private static final int PLAYER_END = ROD_SLOTS + 36;
    private final RodEquipmentContainer equipment;
    private final ItemStack rod;

    public FishingRodMenu(int id, Inventory inventory, ItemStack rod) {
        super(MenuType.GENERIC_9x1, id);
        this.rod = rod;
        this.equipment = new RodEquipmentContainer(rod);
        for (int i = 0; i < ROD_SLOTS; i++) {
            final int slot = i;
            addSlot(new Slot(equipment, i, 8 + i * 18, 18) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return slot < 3 && equipment.canPlaceItem(slot, stack);
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(playerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 50 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(playerSlot(inventory, col, 8 + col * 18, 108));
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) {
                return getItem() != rod && super.mayPickup(player);
            }
            @Override public boolean mayPlace(ItemStack stack) {
                return getItem() != rod && stack != rod && super.mayPlace(stack);
            }
        };
    }

    @Override public boolean stillValid(Player player) {
        return rod.getItem() instanceof FishingRodItem
                && (player.getMainHandItem() == rod || player.getOffhandItem() == rod);
    }

    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // A hotbar swap or drop must not move the rod while its component backs this menu.
        if (slotId >= ROD_SLOTS && slotId < slots.size() && slots.get(slotId).getItem() == rod) return;
        if (clickType == ClickType.SWAP && button >= 0 && button < 9
                && player.getInventory().getItem(button) == rod) return;
        if (!stillValid(player)) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= PLAYER_END) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || slot.getItem() == rod) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < ROD_SLOTS) {
            if (!moveItemStackTo(original, ROD_SLOTS, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            if (!(original.getItem() instanceof FishingEquipmentItem item)) return ItemStack.EMPTY;
            int target = item.family().ordinal();
            if (!slots.get(target).mayPlace(original)
                    || !moveItemStackTo(original, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
