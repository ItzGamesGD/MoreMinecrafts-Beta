package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Emits the vanilla sweep particle once after a Greatsword area attack hits. */
final class GreatswordSweepVfx {
    private GreatswordSweepVfx() {}

    static Vec3 position(Vec3 origin, Vec3 facing) {
        Vec3 horizontal = new Vec3(facing.x, 0.0D, facing.z);
        if (horizontal.lengthSqr() < 1.0E-8D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        else horizontal = horizontal.normalize();
        return origin.add(horizontal).add(0.0D, 0.8D, 0.0D);
    }

    static void emit(ServerPlayer attacker) {
        Vec3 at = position(attacker.position(), attacker.getLookAngle());
        attacker.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK,
                at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
