package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class ResearchStatusEffectMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$blockSpiderPoison(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (effect.is(MobEffects.POISON) && ResearchBatch2Rules.preventSpiderAction(
                ResearchBatch2Runtime.level((LivingEntity) (Object) this, "v3.research.39"), source instanceof Spider))
            cir.setReturnValue(false);
    }

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance moreminecrafts$scaleRecipientDuration(MobEffectInstance effect) {
        int level = ResearchBatch2Runtime.level((LivingEntity) (Object) this, "v3.research.62");
        var type = effect.getEffect().value();
        if (level == 0 || type.isInstantaneous()) return effect;
        int duration = ResearchBatch2Rules.effectDuration(effect.getDuration(), level,
                type.isBeneficial(), type.getCategory() == MobEffectCategory.HARMFUL);
        if (duration == effect.getDuration()) return effect;
        // Copy before changing duration: an AoE's shared input must not affect another recipient.
        return moreminecrafts$copyWithDuration(effect, duration);
    }

    @Unique
    private static MobEffectInstance moreminecrafts$copyWithDuration(MobEffectInstance effect, int duration) {
        var copy = new MobEffectInstance(effect);
        var access = (ResearchEffectDurationAccessor) (Object) copy;
        access.moreminecrafts$setDuration(duration);
        var hidden = ((ResearchEffectDurationAccessor) (Object) effect).moreminecrafts$getHiddenEffect();
        if (hidden != null) access.moreminecrafts$setHiddenEffect(
                moreminecrafts$copyWithDuration(hidden, hidden.getDuration()));
        copy.copyBlendState(effect);
        return copy;
    }
}
