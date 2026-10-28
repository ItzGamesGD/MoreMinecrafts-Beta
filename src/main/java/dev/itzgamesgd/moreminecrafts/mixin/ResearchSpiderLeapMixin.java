package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Runtime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Rules;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeapAtTargetGoal.class)
abstract class ResearchSpiderLeapMixin {
    @Shadow @Final private Mob mob;
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$preventSpiderJump(CallbackInfoReturnable<Boolean> cir) {
        if (ResearchBatch2Rules.preventSpiderAction(
                ResearchBatch2Runtime.level(mob.getTarget(), "v3.research.39"), mob instanceof Spider))
            cir.setReturnValue(false);
    }
}
