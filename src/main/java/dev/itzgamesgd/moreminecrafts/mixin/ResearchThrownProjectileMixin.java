package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Increases velocity once at launch for tridents and throwable-item projectiles only. */
@Mixin(Projectile.class)
abstract class ResearchThrownProjectileMixin {
    @ModifyVariable(method = "shoot", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float moreminecrafts$applyThrowingResearchSpeed(float velocity) {
        Projectile projectile = (Projectile) (Object) this;
        if (projectile instanceof AbstractArrow
                && !(projectile instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident)) return velocity;
        if (!(projectile.getOwner() instanceof ServerPlayer player)) return velocity;
        double bonus = dev.itzgamesgd.moreminecrafts.progression.ResearchRewards.value(
                player, "v3.research.32", "thrown_projectile_speed");
        return (float) dev.itzgamesgd.moreminecrafts.progression.ResearchPassiveMath.scaleByBonus(velocity, bonus);
    }
}
