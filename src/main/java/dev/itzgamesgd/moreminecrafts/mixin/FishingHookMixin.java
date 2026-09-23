package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow private boolean biting;

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$startTiming(ItemStack rod, CallbackInfoReturnable<Integer> result) {
        FishingHook hook = (FishingHook) (Object) this;
        if (!biting) return;
        if (hook.getOwner() instanceof ServerPlayer player) {
            FishingChallenges.start(player, hook, rod);
            result.setReturnValue(1);
        } else if (hook.level().isClientSide()) {
            // The server retains the hooked entity until confirmation or cancellation.
            result.setReturnValue(1);
        }
    }
}
