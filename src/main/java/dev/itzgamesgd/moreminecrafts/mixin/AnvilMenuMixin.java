package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
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
        EnhancementOperation.createResult(menu.getSlot(0).getItem(), menu.getSlot(1).getItem())
                .ifPresent(result -> {
                    menu.getSlot(2).set(result);
                    cost.set(EnhancementOperation.EXPERIENCE_LEVEL_COST);
                    repairItemCountCost = EnhancementOperation.MATERIAL_COST;
                    menu.broadcastChanges();
                    callbackInfo.cancel();
                });
    }
}
