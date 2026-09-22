package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the launch weapon's resolved bonus to each ordinary bow/crossbow projectile exactly once. */
@Mixin(ProjectileWeaponItem.class)
abstract class ProjectileWeaponItemMixin {
    @Inject(
            method = "createProjectile(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/projectile/Projectile;",
            at = @At("RETURN")
    )
    private void moreminecrafts$applyLaunchWeaponDamage(
            ServerLevel level,
            LivingEntity shooter,
            ItemStack weapon,
            ItemStack ammunition,
            boolean critical,
            CallbackInfoReturnable<Projectile> callbackInfo
    ) {
        Projectile projectile = callbackInfo.getReturnValue();
        if (!(projectile instanceof AbstractArrow arrow)) {
            // Crossbow fireworks have separate explosion semantics and intentionally stay vanilla.
            return;
        }

        double bonus = EnhancementEffects.projectileDamage(weapon);
        if (bonus > 0.0D) {
            // AbstractArrow persists its base damage. Mutating it here snapshots the launching
            // stack before the projectile is spawned and before the player can change hands.
            arrow.setBaseDamage(arrow.getBaseDamage() + bonus);
        }
    }
}
