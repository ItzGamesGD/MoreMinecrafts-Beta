package dev.itzgamesgd.moreminecrafts.special.elemental;

/** Pure horizontal geometry shared by Earth's radial smash passive and forward cone active. */
public final class EarthCombatMath {
    private EarthCombatMath() {}

    public static boolean inExpandingRing(double distance, double innerRadius, double outerRadius) {
        return distance >= Math.max(0.0D, innerRadius) && distance <= outerRadius;
    }

    public static boolean isGroundedTarget(boolean onGround, double verticalDelta, double tolerance) {
        return onGround && Math.abs(verticalDelta) <= tolerance;
    }

    public static Vec3 radialOutwardImpulse(Vec3 center, Vec3 target, double strength) {
        double dx = target.x - center.x;
        double dz = target.z - center.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        return length < 1.0E-8D ? Vec3.ZERO : new Vec3(dx / length * strength, 0.0D, dz / length * strength);
    }

    public static boolean inForwardCone(double forwardX, double forwardZ, double offsetX, double offsetZ,
                                        double range, double halfAngleDegrees) {
        double distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
        if (distanceSquared > range * range || distanceSquared < 1.0E-12D) return false;
        double normal = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (normal < 1.0E-12D) return false;
        double dot = (forwardX * offsetX + forwardZ * offsetZ) / (normal * Math.sqrt(distanceSquared));
        return dot + 1.0E-12D >= Math.cos(Math.toRadians(halfAngleDegrees));
    }
}
