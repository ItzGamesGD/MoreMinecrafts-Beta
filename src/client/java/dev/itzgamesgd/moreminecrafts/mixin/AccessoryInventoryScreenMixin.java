package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryCategory;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryEquipmentContainer;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryLoadout;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryUiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws vanilla-style cells behind the five accessory slots that sit left of the armor column, and names the
 * category of an empty hovered slot. The slots themselves belong to the menu; the screen keeps vanilla dimensions.
 */
@Mixin(InventoryScreen.class)
abstract class AccessoryInventoryScreenMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void moreminecrafts$drawAccessorySlotCells(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                       float delta, CallbackInfo ci) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + AccessoryUiLayout.COLUMN_LEFT;
        for (int slot = 0; slot < AccessoryLoadout.SIZE; slot++) {
            int y = accessor.moreminecrafts$getTopPos() + AccessoryUiLayout.slotY(slot) - 1;
            int size = AccessoryUiLayout.CELL_SIZE;
            graphics.fill(x, y, x + size, y + size, 0xFF8B8B8B);
            graphics.fill(x, y, x + size - 1, y + 1, 0xFF373737);
            graphics.fill(x, y, x + 1, y + size - 1, 0xFF373737);
            graphics.fill(x + 1, y + size - 1, x + size, y + size, 0xFFFFFFFF);
            graphics.fill(x + size - 1, y + 1, x + size, y + size, 0xFFFFFFFF);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void moreminecrafts$nameEmptyAccessorySlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                       float delta, CallbackInfo ci) {
        Slot hovered = ((AbstractContainerScreenAccessor) this).moreminecrafts$getHoveredSlot();
        if (hovered == null || hovered.hasItem() || !(hovered.container instanceof AccessoryEquipmentContainer)) return;
        var menu = ((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) (Object) this).getMenu();
        if (!menu.getCarried().isEmpty()) return;
        graphics.setTooltipForNextFrame(AccessoryCategory.values()[hovered.getContainerSlot()].displayName(),
                mouseX, mouseY);
    }
}
