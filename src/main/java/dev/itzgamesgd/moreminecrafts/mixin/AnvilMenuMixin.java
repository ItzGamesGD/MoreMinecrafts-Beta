package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @Shadow
    public DataSlot cost;

    @Shadow
    private int repairItemCountCost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$createEnhancementResult(CallbackInfo callbackInfo) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        var material = menu.getSlot(1).getItem();
        if (!EnhancementOperation.isEnhancementMaterial(material)) {
            return;
        }

        var preview = EnhancementOperation.preview(menu.getSlot(0).getItem(), material);
        menu.getSlot(2).set(preview.map(EnhancementOperation.Result::stack).orElse(ItemStack.EMPTY));
        cost.set(preview.map(EnhancementOperation.Result::experienceLevelCost).orElse(0));
        repairItemCountCost = preview.map(EnhancementOperation.Result::materialCost).orElse(0);
        menu.broadcastChanges();
        callbackInfo.cancel();
    }
}
