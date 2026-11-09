package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Runtime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRemainingRules;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Set the cloud's lifetime before vanilla derives its radius decay from that lifetime. */
@Mixin(ThrownLingeringPotion.class)
abstract class ResearchLingeringPotionMixin {
    @ModifyArg(method = "onHitAsPotion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/AreaEffectCloud;setDuration(I)V"), index = 0)
    private int moreminecrafts$extendCloud(int duration) {
        var owner = ((ThrownLingeringPotion) (Object) this).getOwner();
        return ResearchRemainingRules.lingeringDuration(duration,
                owner instanceof net.minecraft.server.level.ServerPlayer player
                        ? ResearchBatch2Runtime.level(player, "v3.research.61") : 0);
    }
}
