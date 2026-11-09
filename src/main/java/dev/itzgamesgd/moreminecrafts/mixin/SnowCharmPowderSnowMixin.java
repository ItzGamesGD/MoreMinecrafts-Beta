package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.accessory.SnowCharmAbility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs on both logical sides, at the same point vanilla applies the stuck-in-block multiplier. */
@Mixin(Entity.class)
abstract class SnowCharmPowderSnowMixin {
    @Unique private BlockState moreminecrafts$stuckState;

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"))
    private void moreminecrafts$rememberStuckBlock(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        moreminecrafts$stuckState = state;
    }

    @WrapOperation(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;multiply(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 moreminecrafts$snowCharmSinking(Vec3 movement, Vec3 multiplier, Operation<Vec3> original) {
        BlockState state = moreminecrafts$stuckState;
        moreminecrafts$stuckState = null;
        return SnowCharmAbility.adjustStuckMovement((Entity) (Object) this, state, original.call(movement, multiplier));
    }
}
