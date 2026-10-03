package dev.itzgamesgd.moreminecrafts.special.vfx;

import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Server-emitted, presentation-only T2 compositions built from reusable world-space primitives. */
public final class T2ElementalVfx {
    private T2ElementalVfx() {}

    public static void arc(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 planeNormal,
                           double radius, double startRadians, double endRadians, int samples) {
        emit(level, particle, T2VfxGeometry.arc(center, planeNormal, radius, startRadians, endRadians, samples));
    }

    public static void trail(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end,
                             double spacing) {
        emit(level, particle, T2VfxGeometry.line(start, end, spacing));
    }

    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int samples) {
        emit(level, particle, T2VfxGeometry.ring(center, radius, samples));
    }

    public static void horizontalArc(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 facing,
                                     double radius, double halfAngleRadians, int samples) {
        emit(level, particle, T2VfxGeometry.horizontalArc(center, facing, radius, halfAngleRadians, samples));
    }

    public static void beam(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, double spacing) {
        trail(level, particle, start, end, spacing);
    }

    public static void burst(ServerLevel level, ParticleOptions particle, Vec3 center, int count, double radius) {
        emit(level, particle, T2VfxGeometry.burst(center, count, radius));
    }

    /** Temporary particle-only ground marks; this method never reads or writes blocks. */
    public static void groundDecal(ServerLevel level, Vec3 center, double radius, int rays) {
        ring(level, ParticleTypes.ASH, center, radius, Math.max(12, rays * 4));
        for (int i = 0; i < rays; i++) {
            double angle = Math.PI * 2.0D * i / rays;
            Vec3 end = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            trail(level, ParticleTypes.DUST_PLUME, center, end, 0.5D);
        }
    }

    public static void firePrimary(ServerLevel level, Vec3 start, Vec3 impact) {
        trail(level, ParticleTypes.SMALL_FLAME, start, impact, 0.65D);
        burst(level, ParticleTypes.ASH, impact, 3, 0.18D);
    }

    public static void fireSweep(ServerLevel level, Vec3 center, Vec3 facing) {
        Vec3 flat = new Vec3(facing.x, 0.0D, facing.z);
        if (flat.lengthSqr() < 1.0E-8D) return;
        horizontalArc(level, ParticleTypes.FLAME, center, flat, 3.5D, Math.PI * 0.42D, 18);
        horizontalArc(level, ParticleTypes.FLAME, center.add(0.0D, 0.18D, 0.0D),
                flat, 3.35D, Math.PI * 0.4D, 16);
        horizontalArc(level, ParticleTypes.FLAME, center.add(0.0D, -0.18D, 0.0D),
                flat, 3.15D, Math.PI * 0.38D, 14);
        horizontalArc(level, ParticleTypes.SMALL_FLAME, center.add(0.0D, 0.12D, 0.0D),
                flat, 3.1D, Math.PI * 0.4D, 14);
    }

    public static void fireRisingArc(ServerLevel level, Vec3 center, Vec3 facing) {
        Vec3 flat = new Vec3(facing.x, 0.0D, facing.z);
        if (flat.lengthSqr() < 1.0E-8D) return;
        Vec3 side = new Vec3(flat.z, 0.0D, -flat.x).normalize();
        Vec3 origin = center.add(flat.normalize().scale(2.2D)).add(0.0D, 0.8D, 0.0D);
        arc(level, ParticleTypes.FLAME, origin, side, 3.2D, -Math.PI * 0.48D, Math.PI * 0.15D, 24);
        arc(level, ParticleTypes.SMALL_FLAME, origin, side, 2.7D,
                -Math.PI * 0.45D, Math.PI * 0.12D, 18);
    }

    public static void waterHit(ServerLevel level, Vec3 start, Vec3 impact) {
        trail(level, ParticleTypes.SPLASH, start, impact, 0.9D);
        burst(level, ParticleTypes.BUBBLE_POP, impact, 3, 0.16D);
    }

    public static void waterLaunch(ServerLevel level, Vec3 feet) {
        ring(level, ParticleTypes.SPLASH, feet, 0.7D, 16);
        ring(level, ParticleTypes.BUBBLE, feet.add(0.0D, 0.04D, 0.0D), 0.45D, 12);
    }

    public static void waterDiveTrail(ServerLevel level, Vec3 start, Vec3 end) {
        trail(level, ParticleTypes.FALLING_WATER, start, end, 0.28D);
        trail(level, ParticleTypes.BUBBLE, start.add(0.0D, 0.12D, 0.0D), end.add(0.0D, 0.12D, 0.0D), 0.55D);
    }

    public static void waterLanding(ServerLevel level, Vec3 center) {
        burst(level, ParticleTypes.SPLASH, center, 16, 1.0D);
        ring(level, ParticleTypes.WATER_WAKE, center, 1.25D, 20);
    }

    public static void waterPillar(ServerLevel level, Vec3 center) {
        beam(level, ParticleTypes.FALLING_WATER, center.add(0.0D, 0.15D, 0.0D),
                center.add(0.0D, 4.0D, 0.0D), 0.22D);
        beam(level, ParticleTypes.SPLASH, center.add(0.0D, 0.2D, 0.0D),
                center.add(0.0D, 3.6D, 0.0D), 0.42D);
    }

    public static void windHit(ServerLevel level, Vec3 center, Vec3 facing) {
        arc(level, ParticleTypes.SWEEP_ATTACK, center, new Vec3(-facing.z, 0.0D, facing.x),
                0.8D, -0.42D, 0.42D, 5);
    }

    public static void windSweep(ServerLevel level, Vec3 center, Vec3 facing) {
        horizontalArc(level, ParticleTypes.SWEEP_ATTACK, center, facing, 2.5D, 0.55D, 12);
        horizontalArc(level, ParticleTypes.SWEEP_ATTACK, center.add(0.0D, 0.32D, 0.0D),
                facing, 2.9D, 0.48D, 12);
        horizontalArc(level, ParticleTypes.CLOUD, center.add(0.0D, -0.18D, 0.0D), facing, 2.2D, 0.5D, 9);
    }

    public static void windBlade(ServerLevel level, Vec3 start, Vec3 facing, double range) {
        Vec3 side = new Vec3(-facing.z, facing.x, 0.0D);
        if (side.lengthSqr() < 1.0E-8D) side = new Vec3(1.0D, 0.0D, 0.0D);
        side = side.normalize();
        for (double distance = 0.9D; distance <= range; distance += 1.2D) {
            Vec3 center = start.add(facing.scale(distance));
            arc(level, ParticleTypes.SWEEP_ATTACK, center, side, 0.48D,
                    -0.75D, 0.75D, 4);
        }
    }

    public static void earthLanding(ServerLevel level, Vec3 center) {
        burst(level, ParticleTypes.POOF, center, 12, 0.8D);
        burst(level, ParticleTypes.DUST_PLUME, center, 7, 0.55D);
    }

    public static void earthSeismicRing(ServerLevel level, Vec3 center, double radius) {
        ring(level, ParticleTypes.ASH, center, radius, Math.max(12, (int) Math.ceil(radius * 4.0D)));
        if (radius <= 1.0D) groundDecal(level, center, radius, 6);
    }

    public static void earthCone(ServerLevel level, Vec3 start, Vec3 facing, double range) {
        for (double distance = 0.8D; distance <= range; distance += 0.8D) {
            Vec3 center = start.add(facing.scale(distance));
            burst(level, ParticleTypes.POOF, center, 3, 0.2D + distance * 0.035D);
            burst(level, ParticleTypes.DUST_PLUME, center, 2, 0.12D + distance * 0.02D);
        }
    }

    public static void lightningPrimary(ServerLevel level, Vec3 center) {
        burst(level, ParticleTypes.ELECTRIC_SPARK, center, 2, 0.12D);
    }

    public static void lightningProc(ServerLevel level, Vec3 center) {
        burst(level, ParticleTypes.FLASH, center, 1, 0.0D);
        for (int branch = 0; branch < 3; branch++) {
            double side = branch - 1.0D;
            Vec3 end = center.add(side * 0.42D, 0.55D + (branch % 2) * 0.2D, 0.18D * (1 - branch));
            beam(level, ParticleTypes.ELECTRIC_SPARK, center, end, 0.18D);
        }
    }

    public static void lightningSlash(ServerLevel level, Vec3 start, Vec3 end, Vec3 facing) {
        burst(level, ParticleTypes.FLASH, start.add(end).scale(0.5D), 1, 0.0D);
        beam(level, ParticleTypes.ELECTRIC_SPARK, start, end, 0.32D);
        Vec3 offset = new Vec3(-facing.z, 0.0D, facing.x).scale(0.06D);
        beam(level, ParticleTypes.END_ROD, start.add(offset), end.add(offset), 0.65D);
    }

    private static void emit(ServerLevel level, ParticleOptions particle, List<Vec3> points) {
        for (Vec3 point : points) {
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
