package dev.itzgamesgd.moreminecrafts.special.elemental;

/** Pure horizontal geometry shared by Earth Fracture and Earth Shatter. */
public final class EarthCombatMath {
    private EarthCombatMath() {}

    public static boolean inForwardLane(double forwardX, double forwardZ, double offsetX, double offsetZ,
                                        double length, double halfWidth) {
        double normal = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (normal < 1.0E-12D) return false;
        double fx = forwardX / normal;
        double fz = forwardZ / normal;
        double along = offsetX * fx + offsetZ * fz;
        double lateral = Math.abs(-offsetX * fz + offsetZ * fx);
        return along >= 0.0D && along <= length && lateral <= halfWidth;
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
