package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchXpOrbContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scales only experience orbs created inside a classified, purchase-gated source scope. */
@Mixin(net.minecraft.world.entity.ExperienceOrb.class)
abstract class ResearchExperienceOrbMixin {
    @ModifyVariable(method = "award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int moreminecrafts$applySourceResearchBonus(int value) {
        return ResearchXpOrbContext.adjustOrbValue(value);
    }
}
