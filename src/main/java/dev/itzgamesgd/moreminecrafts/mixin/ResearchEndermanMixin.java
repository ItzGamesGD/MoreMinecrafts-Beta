package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Runtime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Rules;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enderman.class)
abstract class ResearchEndermanMixin {
    // All random, damage-escape and pursuit teleport paths converge here.
    @Inject(method = "teleport(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$preventEngagedTeleport(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        var target = ((Enderman) (Object) this).getTarget();
        if (ResearchBatch2Rules.preventEngagedTeleport(ResearchBatch2Runtime.level(target, "v3.research.40"), target != null))
            cir.setReturnValue(false);
    }
}
