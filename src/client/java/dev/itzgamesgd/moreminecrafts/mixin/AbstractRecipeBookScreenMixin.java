package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryLoadout;
import dev.itzgamesgd.moreminecrafts.accessory.AccessoryUiLayout;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The accessory column lies outside the 176px texture; clicks in its cell borders must not drop a held stack. */
@Mixin(AbstractRecipeBookScreen.class)
abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$accessoryColumnIsInside(double mouseX, double mouseY, int left, int top,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (!(((Object) this) instanceof InventoryScreen)) return;
        int columnLeft = left + AccessoryUiLayout.COLUMN_LEFT;
        int columnTop = top + AccessoryUiLayout.SLOT_FIRST_Y - 1;
        int columnHeight = AccessoryLoadout.SIZE * AccessoryUiLayout.SLOT_PITCH;
        if (mouseX >= columnLeft && mouseX < columnLeft + AccessoryUiLayout.CELL_SIZE
                && mouseY >= columnTop && mouseY < columnTop + columnHeight) {
            cir.setReturnValue(false);
        }
    }
}
