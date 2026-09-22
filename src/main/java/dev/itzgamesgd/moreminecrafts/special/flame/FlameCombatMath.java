package dev.itzgamesgd.moreminecrafts.special.flame;

/** Pure prototype combat calculations, kept independent from Minecraft registry bootstrap. */
public final class FlameCombatMath {
    private FlameCombatMath() {
    }

    public static boolean isInsideHorizontalCone(
            double forwardX, double forwardZ,
            double offsetX, double offsetZ,
            double range, double halfAngleDegrees
    ) {
        double distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
        if (range < 0.0D || distanceSquared > range * range) {
            return false;
        }
        if (distanceSquared == 0.0D) {
            return true;
        }

        double forwardLength = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (forwardLength == 0.0D) {
            return false;
        }
        double dot = (forwardX * offsetX + forwardZ * offsetZ)
                / (forwardLength * Math.sqrt(distanceSquared));
        return dot + 1.0E-12D >= Math.cos(Math.toRadians(halfAngleDegrees));
    }

    public static float splashDamage(float directDamage, float fraction) {
        if (!Float.isFinite(directDamage) || !Float.isFinite(fraction)
                || directDamage <= 0.0F || fraction <= 0.0F) {
            return 0.0F;
        }
        return directDamage * fraction;
    }
}
