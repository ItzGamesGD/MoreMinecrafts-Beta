package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A successful bow/crossbow projectile launch is the ammunition-use event. */
@Mixin({BowItem.class, CrossbowItem.class})
abstract class KnowledgeProjectileWeaponMixin {
    @Inject(method = "shootProjectile", at = @At("TAIL"))
    private void moreminecrafts$recordSpecialAmmo(LivingEntity shooter, Projectile projectile,
            int projectileIndex, float velocity, float divergence, float yaw, LivingEntity target,
            CallbackInfo ci) {
        if (shooter instanceof ServerPlayer player)
            KnowledgeEventCollectors.specialAmmunitionUsed(player, projectile);
    }
}
