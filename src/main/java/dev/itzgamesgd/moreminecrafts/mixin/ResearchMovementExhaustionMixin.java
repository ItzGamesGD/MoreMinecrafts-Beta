package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.Research23AEffectRules;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Scales only the vanilla sprint and swim exhaustion calls, leaving every other cause untouched. */
@Mixin(ServerPlayer.class)
abstract class ResearchMovementExhaustionMixin {
    @ModifyArg(method = "checkMovementStatistics",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V", ordinal = 0), index = 0)
    private float moreminecrafts$reduceSwimExhaustion(float amount) {
        return scale(amount);
    }

    @ModifyArg(method = "checkMovementStatistics",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V", ordinal = 3), index = 0)
    private float moreminecrafts$reduceSprintExhaustion(float amount) {
        return scale(amount);
    }

    private float scale(float amount) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        int level = ResearchRewards.level(player, "v3.research.55", "sprint_swim_exhaustion");
        return amount * (float) Research23AEffectRules.sprintSwimExhaustionMultiplier(level);
    }
}
