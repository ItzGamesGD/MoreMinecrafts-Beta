package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** No damage or block changes: vanilla motion, resistance and collision handle displacement. */
public final class T1Shockwave {
    // Calibrated on a still, no-AI, zero-resistance zombie on a stone plane, normal gravity.
    public static final double HORIZONTAL_IMPULSE = 0.55D;
    public static final double VERTICAL_IMPULSE = 0.25D;
    private T1Shockwave() {}
    public static void tryActivate(ServerPlayer player) {
        double radius = T1AccessoryRuntime.stat(player, AccessoryDefinition.T1_CRISIS_ESCAPE, "radius_blocks");
        if (radius <= 0 || !player.isAlive() || player.getHealth() >= player.getMaxHealth() * 0.3F
                || !AccessoryCooldownRuntime.start(player, AccessoryDefinition.T1_CRISIS_ESCAPE, 1800)) return;
        player.heal(4);
        var level = (ServerLevel) player.level();
        Vec3 origin = player.getBoundingBox().getCenter();
        var search = new net.minecraft.world.phys.AABB(origin, origin).inflate(radius + 2);
        for (var target : level.getEntities(player, search)) {
            if (target instanceof LivingEntity living) {
                Vec3 center = living.getBoundingBox().getCenter();
                if (!T1CombatTargets.eligible(player, living, true) || center.distanceToSqr(origin) > radius * radius
                        || !visible(player, origin, center)) continue;
                Vec3 direction = center.subtract(origin).multiply(1, 0, 1);
                if (direction.lengthSqr() < 1e-8) direction = new Vec3(1, 0, 0);
                direction = direction.normalize();
                double resistance = Math.max(0, 1 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                living.knockback(HORIZONTAL_IMPULSE, -direction.x, -direction.z, player.damageSources().playerAttack(player), 0);
                var velocity = living.getDeltaMovement();
                living.setDeltaMovement(velocity.x, resistance > 0 ? VERTICAL_IMPULSE * resistance : velocity.y, velocity.z);
                T1AccessoryRuntime.synchronizeMotion(living);
            } else if (target instanceof Arrow || target instanceof SpectralArrow || target instanceof ThrownTrident) {
                var arrow = (AbstractArrow) target;
                Vec3 point = target.position();
                // Embedded/previously-hit projectile handling stays vanilla; only a flying projectile is reflected.
                if (((dev.itzgamesgd.moreminecrafts.mixin.T1ArrowStateAccessor) arrow).moreminecrafts$isInGround()
                        || point.distanceToSqr(origin) > radius * radius || !visible(player, origin, point)) continue;
                Vec3 outward = point.subtract(origin);
                double speed = target.getDeltaMovement().length();
                if (outward.lengthSqr() < 1e-8 || speed == 0) continue;
                target.setDeltaMovement(outward.normalize().scale(speed));
                T1AccessoryRuntime.synchronizeMotion(target);
            }
        }
        // Ring marks the horizontal cross-section of the actual 3D sphere.
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI / 16;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    origin.x + Math.cos(angle) * radius, origin.y, origin.z + Math.sin(angle) * radius,
                    1, 0, 0.05, 0, 0.015);
        }
        T1AccessoryRuntime.effect(player, net.minecraft.core.particles.ParticleTypes.GUST,
                net.minecraft.sounds.SoundEvents.WIND_CHARGE_BURST.value());
    }
    private static boolean visible(ServerPlayer player, Vec3 start, Vec3 end) {
        return player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }
}
