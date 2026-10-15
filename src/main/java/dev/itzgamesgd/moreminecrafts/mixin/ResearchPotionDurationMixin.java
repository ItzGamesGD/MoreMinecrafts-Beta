package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchPassiveMath;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Extends beneficial timed effects only when a researcher consumes a drinkable potion. */
@Mixin(net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect.class)
abstract class ResearchPotionDurationMixin {
    @ModifyArg(method = "apply(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), index = 0)
    private MobEffectInstance moreminecrafts$extendBeneficialPotionEffect(MobEffectInstance effect,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ItemStack stack,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LivingEntity user) {
        if (!(stack.getItem() instanceof PotionItem)) return effect;
        if (!(user instanceof ServerPlayer player)) return effect;
        double bonus = ResearchRewards.value(player, "alchemy_root", "beneficial_potion_duration");
        if (bonus <= 0.0D || !effect.getEffect().value().isBeneficial() || effect.isInfiniteDuration()) return effect;
        int duration = ResearchPassiveMath.increasedDuration(effect.getDuration(), (int) Math.round(bonus * 100.0D));
        return new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(), effect.isAmbient(),
                effect.isVisible(), effect.showIcon());
    }
}
