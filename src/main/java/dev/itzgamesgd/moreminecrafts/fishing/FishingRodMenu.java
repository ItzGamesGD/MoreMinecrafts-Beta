package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import dev.itzgamesgd.moreminecrafts.registry.ModMenuTypes;

/** Three server-authoritative module slots, backed by the held rod's data component. */
public final class FishingRodMenu extends AbstractContainerMenu {
    private static final int ROD_SLOTS = 3;
    private static final int PLAYER_END = ROD_SLOTS + 36;
    private final Container equipment;
    private final ItemStack rod;

    /** Client menu receives its slots from normal Minecraft container synchronization. */
    public FishingRodMenu(int id, Inventory inventory) {
        this(id, inventory, ItemStack.EMPTY, new SimpleContainer(ROD_SLOTS));
    }

    public FishingRodMenu(int id, Inventory inventory, ItemStack rod) {
        this(id, inventory, rod, new RodEquipmentContainer(rod));
    }

    private FishingRodMenu(int id, Inventory inventory, ItemStack rod, Container equipment) {
        super(ModMenuTypes.FISHING_ROD, id);
        this.rod = rod;
        this.equipment = equipment;
        for (int i = 0; i < ROD_SLOTS; i++) {
            final int slot = i;
            addSlot(new Slot(equipment, i, 107 + i * 75, 45) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof FishingEquipmentItem item
                            && item.family().ordinal() == slot && equipment.canPlaceItem(slot, stack);
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(playerSlot(inventory, col + row * 9 + 9, 82 + col * 18, 190 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(playerSlot(inventory, col, 82 + col * 18, 244));
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
        return rod.isEmpty() || rod.getItem() instanceof FishingRodItem
                && (player.getMainHandItem() == rod || player.getOffhandItem() == rod);
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
