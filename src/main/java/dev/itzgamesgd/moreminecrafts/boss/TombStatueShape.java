package dev.itzgamesgd.moreminecrafts.boss;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Single source of truth for the risen statue's footprint and for how much of it stands above the floor at a
 * given age, so the server collision shape follows the displayed obelisk instead of staying fixed.
 */
final class TombStatueShape {
    /** Ticks the display takes to slide fully up (or back down) through its transformation interpolation. */
    static final int RISE_TICKS = 5;
    /** How far below the floor the buried display's top edge sits. */
    static final double BURY_DEPTH = 0.2D;
    /** The obelisk model is 6 of 16 units wide and is scaled by STATUE_HEIGHT / 2 (see TombVfxDisplay). */
    static final double WIDTH = 6.0D / 16.0D * (TombGuardianTuning.STATUE_HEIGHT / 2.0D);

    private TombStatueShape() {}

    /** Height of the statue above the floor, {@code age} ticks after it spawned and {@code lifetime} ticks in total. */
    static double height(int age, int lifetime) {
        double full = TombGuardianTuning.STATUE_HEIGHT;
        int sinkStart = lifetime - RISE_TICKS;
        double top;
        if (age >= sinkStart) {
            top = lerp(full, -BURY_DEPTH, clamp01((age - sinkStart) / (double) RISE_TICKS));
        } else {
            top = lerp(-BURY_DEPTH, full, clamp01((age - 1) / (double) RISE_TICKS));
        }
        return Math.max(0.0D, Math.min(full, top));
    }

    static AABB box(Vec3 base, double height) {
        double half = WIDTH / 2.0D;
        return new AABB(base.x - half, base.y, base.z - half, base.x + half, base.y + height, base.z + half);
    }

    /** The collision box of the fully risen statue. */
    static AABB fullBox(Vec3 base) {
        return box(base, TombGuardianTuning.STATUE_HEIGHT);
    }

    private static double lerp(double from, double to, double t) { return from + (to - from) * t; }

    private static double clamp01(double value) { return Math.max(0.0D, Math.min(1.0D, value)); }
}
