package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Runtime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchEcologyAiRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies `.37` Skeleton aim reset (20/30/40%) when firing at a researched target. */
@Mixin(AbstractSkeleton.class)
abstract class ResearchSkeletonAimMixin {
    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$resetSkeletonAim(LivingEntity target, float velocity, CallbackInfo ci) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        if (skeleton.level().isClientSide()) return;
        int level37 = ResearchBatch2Runtime.level(target, "v3.research.37");
        if (ResearchEcologyAiRules.skeletonAimResetSucceeds(level37, skeleton.getRandom().nextDouble())) {
            ci.cancel();
        }
    }
}
