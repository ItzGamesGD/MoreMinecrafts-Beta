package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every hopper, hopper minecart and dropper container lookup resolves through this one method, so a registered royal
 * chest is invisible to automated insertion and extraction (D13-03-I06 Q37).
 */
@Mixin(HopperBlockEntity.class)
abstract class RoyalChestHopperMixin {
    @Inject(method = "getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;DDD)Lnet/minecraft/world/Container;",
            at = @At("HEAD"), cancellable = true)
    private static void moreminecrafts$royalChestIsNotAutomatable(Level level, BlockPos pos, BlockState state,
            double x, double y, double z, CallbackInfoReturnable<Container> cir) {
        if (RoyalChestRuntime.isGuarded(level, pos)) cir.setReturnValue(null);
    }
}
