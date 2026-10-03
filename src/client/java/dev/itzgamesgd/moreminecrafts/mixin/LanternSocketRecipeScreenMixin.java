package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
abstract class LanternSocketRecipeScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    protected LanternSocketRecipeScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // Recipe-book screens override this method without calling the base container screen.
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$includeSocket(double mouseX, double mouseY, int left, int top,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!(menu instanceof InventoryMenu) || menu.slots.size() <= 46) return;
        var socket = menu.getSlot(46);
        if (isHovering(socket.x, socket.y, 16, 16, mouseX, mouseY)) cir.setReturnValue(false);
    }
}
