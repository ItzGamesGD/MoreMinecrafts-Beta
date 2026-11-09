package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryUiLayout;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Moves the survival inventory's recipe book left by {@link AccessoryUiLayout#RECIPE_BOOK_SHIFT} while it sits next
 * to the main GUI, so it clears the accessory column. The inventory itself keeps its vanilla position and size.
 * In vanilla's narrow-screen mode (xOffset 0) the book overlays the inventory exactly as in vanilla.
 */
@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {
    @Shadow @Final protected RecipeBookMenu menu;
    @Shadow private int xOffset;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow abstract int getXOrigin();
    @Shadow abstract int getYOrigin();
    @Shadow public abstract boolean isVisible();

    private boolean moreminecrafts$shiftsForAccessories() {
        return menu instanceof InventoryMenu && xOffset == 86;
    }

    @Inject(method = "getXOrigin", at = @At("RETURN"), cancellable = true)
    private void moreminecrafts$clearAccessoryColumn(CallbackInfoReturnable<Integer> cir) {
        if (moreminecrafts$shiftsForAccessories()) {
            cir.setReturnValue(cir.getReturnValueI() - AccessoryUiLayout.RECIPE_BOOK_SHIFT);
        }
    }

    /** Vanilla measures the book as the 147px left of the GUI; use the shifted rectangle instead. */
    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void moreminecrafts$shiftedBookIsInside(double mouseX, double mouseY, int left, int top, int imageWidth,
                                                    int imageHeight, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !isVisible() || !moreminecrafts$shiftsForAccessories()) return;
        int x = getXOrigin();
        int y = getYOrigin();
        if (mouseX >= x && mouseX < x + RecipeBookComponent.IMAGE_WIDTH
                && mouseY >= y && mouseY < y + RecipeBookComponent.IMAGE_HEIGHT) {
            cir.setReturnValue(false);
        }
    }
}
