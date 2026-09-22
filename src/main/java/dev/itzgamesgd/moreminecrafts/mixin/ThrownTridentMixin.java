package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Applies the thrown stack's SPEAR value after vanilla has calculated the hit's damage. */
@Mixin(ThrownTrident.class)
abstract class ThrownTridentMixin {
    @ModifyArg(
            method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            index = 1
    )
    private float moreminecrafts$applyThrownDamage(float vanillaDamage) {
        ThrownTrident trident = (ThrownTrident) (Object) this;
        return vanillaDamage + (float) EnhancementEffects.projectileDamage(trident.getWeaponItem());
    }
}
