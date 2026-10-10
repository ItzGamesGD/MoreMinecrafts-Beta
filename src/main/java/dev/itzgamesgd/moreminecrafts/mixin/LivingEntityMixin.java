package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffects;
import dev.itzgamesgd.moreminecrafts.mob.CustomMobStats;
import dev.itzgamesgd.moreminecrafts.zombie.ZombieVariantState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies player armor enhancements immediately before vanilla armor/toughness mitigation. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "blockUsingShield", at = @At("TAIL"))
    private void moreminecrafts$recordShieldBlock(LivingEntity attacker, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player)
            dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime.recordCondition(player,
                    "shield_blocks", dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime.state(player)
                            .conditionProgress("shield_blocks") + 1);
    }

    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void moreminecrafts$refreshCustomMobArmor(Map<EquipmentSlot, ItemStack> oldItems,
            CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if ((Object) this instanceof Mob mob) CustomMobStats.refreshArmorProfile(mob);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$ignoreLeapFallDamage(double distance, float multiplier, DamageSource source,
                                                      CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (this instanceof ZombieVariantState state
                && state.moreminecrafts$hasLeapFallImmunity(entity.level().getGameTime())) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float moreminecrafts$fallRoll(float multiplier) {
        if ((Object) this instanceof ServerPlayer player
                && dev.itzgamesgd.moreminecrafts.progression.ResearchMovementRuntime.consumeFallRoll(player))
            return multiplier * 0.4F;
        return multiplier;
    }

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
        if (source.getEntity() instanceof ServerPlayer attacker
                && (Object) this instanceof net.minecraft.world.entity.monster.Monster
                && !attacker.level().isClientSide()) {
            double bonus = dev.itzgamesgd.moreminecrafts.progression.ResearchRewards.value(
                    attacker, "combat_root", "hostile_damage");
            if (bonus > 0.0D) incomingDamage = (float) (incomingDamage * (1.0D + bonus));
        }
        if (!((Object) this instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            args.set(1, incomingDamage);
            return;
        }

        double reduction = EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.HEAD))
                + EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.CHEST))
                + EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.LEGS))
                + EnhancementEffects.damageReduction(player.getItemBySlot(EquipmentSlot.FEET));
        args.set(1, (float) (incomingDamage * (1.0D - reduction)));
    }
}
