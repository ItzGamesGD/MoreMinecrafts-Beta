package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
abstract class LanternSocketScreenMixin {
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected abstract boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY);

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$includeSocket(double mouseX, double mouseY, int left, int top,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!(menu instanceof InventoryMenu) || menu.slots.size() <= 46) return;
        var socket = menu.getSlot(46);
        if (isHovering(socket.x, socket.y, 16, 16, mouseX, mouseY)) cir.setReturnValue(false);
    }

    @Inject(method = "extractSlots", at = @At("HEAD"))
    private void moreminecrafts$drawSocket(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!(menu instanceof InventoryMenu) || menu.slots.size() <= 46) return;
        var socket = menu.getSlot(46);
        // extractSlots already uses inventory-relative coordinates. Reuse a vanilla empty slot.
        graphics.blit(RenderPipelines.GUI_TEXTURED, AbstractContainerScreen.INVENTORY_LOCATION,
                socket.x - 1, socket.y - 1, 7.0F, 83.0F, 18, 18, 256, 256);
    }
}
