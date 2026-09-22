package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Applies player armor enhancements immediately before vanilla armor/toughness mitigation. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @ModifyArgs(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
            )
    )
    private void moreminecrafts$applyArmorEnhancementReduction(Args args) {
        DamageSource source = args.get(0);
        float incomingDamage = args.get(1);
        if (!((Object) this instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        double reduction = EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.HEAD))
                + EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.CHEST))
                + EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.LEGS))
                + EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.FEET));
        args.set(1, (float) (incomingDamage * (1.0D - reduction)));
    }
}
