package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchMovementExhaustion;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Scales only vanilla swim and sprint exhaustion in checkMovementStatistics. */
@Mixin(ServerPlayer.class)
abstract class ResearchMovementExhaustionMixin {
    @ModifyArg(method = "checkMovementStatistics",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V", ordinal = 0), index = 0)
    private float moreminecrafts$reduceSwimExhaustion(float amount) {
        return ResearchMovementExhaustion.scale((ServerPlayer) (Object) this, amount);
    }

    @ModifyArg(method = "checkMovementStatistics",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V", ordinal = 3), index = 0)
    private float moreminecrafts$reduceSprintExhaustion(float amount) {
        return ResearchMovementExhaustion.scale((ServerPlayer) (Object) this, amount);
    }
}
