package dev.itzgamesgd.moreminecrafts.special.elemental;

import net.minecraft.world.phys.Vec3;

/** Provisional, isolated movement policy for the Water Sword launch and committed dive. */
public final class WaterDiveMotion {
    public static final double LAUNCH_VERTICAL_SPEED = 1.25D; // PROVISIONAL: live-tunable
    public static final double LAUNCH_HORIZONTAL_SPEED = 0.35D;
    public static final double DIVE_HORIZONTAL_SPEED = 1.35D; // PROVISIONAL: live-tunable
    public static final double DIVE_MIN_DOWNWARD_SPEED = 1.4D; // PROVISIONAL: live-tunable
    public static final double DIVE_LOOK_DOWN_SCALE = 1.65D;

    private WaterDiveMotion() {}

    public static Vec3 launchVelocity(Vec3 look) {
        return new Vec3(look.x * LAUNCH_HORIZONTAL_SPEED, LAUNCH_VERTICAL_SPEED,
                look.z * LAUNCH_HORIZONTAL_SPEED);
    }

    public static Vec3 diveVelocity(Vec3 look) {
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-8D) horizontal = horizontal.normalize().scale(DIVE_HORIZONTAL_SPEED);
        return new Vec3(horizontal.x, Math.min(-DIVE_MIN_DOWNWARD_SPEED, look.y * DIVE_LOOK_DOWN_SCALE), horizontal.z);
    }
}
