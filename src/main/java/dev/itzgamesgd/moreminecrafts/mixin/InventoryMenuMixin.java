package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryEquipmentContainer;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryItem;
import dev.itzgamesgd.moreminecrafts.accessory.AccessorySlotRules;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryUiLayout;
import dev.itzgamesgd.moreminecrafts.equipment.LanternSlotContainer;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin extends AbstractContainerMenu {
    private InventoryMenuMixin() { super(null, 0); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void moreminecrafts$addLanternSocket(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        // Keep the socket inside the vanilla inventory screen's clickable bounds.
        var container = new LanternSlotContainer(player);
        addSlot(new Slot(container, 0, 26, 8) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.MINER_LANTERN); }
            @Override public int getMaxStackSize() { return 1; }
        });
        var accessories = new AccessoryEquipmentContainer(player);
        for (int slot = 0; slot < 5; slot++) {
            int accessorySlot = slot;
            addSlot(new Slot(accessories, slot, AccessoryUiLayout.SLOT_X, AccessoryUiLayout.slotY(slot)) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return AccessorySlotRules.accepts(accessorySlot, stack);
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$quickMoveLantern(Player player, int index,
                                                  CallbackInfoReturnable<ItemStack> cir) {
        final int lanternSlot = 46;
        final int accessoryFirst = 47;
        final int accessoryLimit = 52;
        if (index < 0 || index >= slots.size()) return;
        Slot source = slots.get(index);
        if (!source.hasItem()) return;
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        boolean handled;
        if (index >= accessoryFirst && index < accessoryLimit) {
            handled = moveItemStackTo(stack, 9, 46, false);
        } else if (index == lanternSlot) {
            handled = moveItemStackTo(stack, 9, 46, false);
        } else if (stack.getItem() instanceof AccessoryItem) {
            int accessorySlot = AccessorySlotRules.slotFor(stack);
            int destination = accessorySlot >= 0 ? accessoryFirst + accessorySlot : -1;
            handled = accessorySlot >= 0 && destination < slots.size()
                    && !slots.get(destination).hasItem()
                    && moveItemStackTo(stack, destination, destination + 1, false);
        } else {
            handled = stack.is(ModItems.MINER_LANTERN) && !slots.get(lanternSlot).hasItem()
                    && moveItemStackTo(stack, lanternSlot, lanternSlot + 1, false);
        }
        if (!handled) return;
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY); else source.setChanged();
        cir.setReturnValue(original);
    }
}
