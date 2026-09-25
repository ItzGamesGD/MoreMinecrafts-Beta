package dev.itzgamesgd.moreminecrafts.special.flame;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class FlameSwordEffects {
    public static final double SPLASH_RADIUS = 3.0D;
    public static final float SPLASH_FRACTION = 0.35F;
    /**
     * The 26.3 Item#hurtEnemy callback confirms a successful hit but does not expose resolved damage.
     * This therefore is a PROVISIONAL passive sweep baseline, before armor mitigation.
     */
    public static final float PROTOTYPE_DIRECT_DAMAGE = 9.0F;

    private static final ThreadLocal<Boolean> APPLYING_SPLASH = ThreadLocal.withInitial(() -> false);

    private FlameSwordEffects() {
    }

    public static void splash(ServerPlayer attacker, LivingEntity primaryTarget) {
        if (APPLYING_SPLASH.get()) {
            return;
        }
        float damage = FlameCombatMath.splashDamage(PROTOTYPE_DIRECT_DAMAGE, SPLASH_FRACTION);
        ServerLevel level = attacker.level();
        AABB search = primaryTarget.getBoundingBox().inflate(SPLASH_RADIUS);
        APPLYING_SPLASH.set(true);
        try {
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, search, candidate ->
                    candidate != attacker
                            && candidate != primaryTarget
                            && candidate.isAlive()
                            && !candidate.isAlliedTo(attacker)
                            && candidate.distanceToSqr(primaryTarget) <= SPLASH_RADIUS * SPLASH_RADIUS
                            && (!(candidate instanceof ServerPlayer player) || attacker.canHarmPlayer(player)))) {
                if (nearby.hurtServer(level, attacker.damageSources().playerAttack(attacker), damage)) {
                    level.sendParticles(ParticleTypes.FLAME, nearby.getX(), nearby.getY() + 0.7D, nearby.getZ(),
                            4, 0.2D, 0.3D, 0.2D, 0.01D);
                }
            }
        } finally {
            APPLYING_SPLASH.remove();
        }
    }
}
