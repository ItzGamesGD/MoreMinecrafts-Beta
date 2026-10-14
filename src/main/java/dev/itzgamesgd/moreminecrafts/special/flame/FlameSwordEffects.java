package dev.itzgamesgd.moreminecrafts.special.flame;

import dev.itzgamesgd.moreminecrafts.special.vfx.T2ElementalVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class FlameSwordEffects {
    public static final double SWEEP_HALF_WIDTH = 3.5D;
    public static final double SWEEP_DEPTH = 1.5D;
    public static final float SWEEP_DAMAGE = 3.5F; // PROVISIONAL, intentionally independent of melee modifiers

    private FlameSwordEffects() {
    }

    public static void sweep(ServerPlayer attacker, LivingEntity primaryTarget) {
        ServerLevel level = attacker.level();
        AABB search = primaryTarget.getBoundingBox().inflate(SWEEP_HALF_WIDTH, 1.0D, SWEEP_HALF_WIDTH);
        double forwardX = attacker.getLookAngle().x;
        double forwardZ = attacker.getLookAngle().z;
        double length = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (length < 1.0E-8D) return;
        forwardX /= length;
        forwardZ /= length;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, search, candidate ->
                candidate != attacker && candidate.isAlive() && !candidate.isAlliedTo(attacker)
                        && (!(candidate instanceof ServerPlayer player) || attacker.canHarmPlayer(player)))) {
            double dx = nearby.getX() - primaryTarget.getX();
            double dz = nearby.getZ() - primaryTarget.getZ();
            double depth = Math.abs(dx * forwardX + dz * forwardZ);
            double lateral = Math.abs(-dx * forwardZ + dz * forwardX);
            if (depth <= SWEEP_DEPTH && lateral <= SWEEP_HALF_WIDTH) {
                nearby.hurtServer(level, attacker.damageSources().playerAttack(attacker), SWEEP_DAMAGE);
            }
        }
        T2ElementalVfx.fireSweep(level, primaryTarget.position().add(0.0D, 0.9D, 0.0D),
                new net.minecraft.world.phys.Vec3(forwardX, 0.0D, forwardZ));
    }
}
