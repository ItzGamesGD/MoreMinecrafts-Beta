package dev.itzgamesgd.moreminecrafts.special.wind;

/** Pure geometry shared by Wind Blade's bounded, narrow swept path. */
public final class WindCombatMath {
    private WindCombatMath() {
    }

    public static boolean isInsidePath(double forwardX, double forwardY, double forwardZ,
            double offsetX, double offsetY, double offsetZ, double range, double radius) {
        double lengthSquared = forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ;
        if (lengthSquared < 1.0E-12D || range < 0.0D || radius < 0.0D) return false;
        double scale = 1.0D / Math.sqrt(lengthSquared);
        double projection = (offsetX * forwardX + offsetY * forwardY + offsetZ * forwardZ) * scale;
        if (projection < 0.0D || projection > range) return false;
        double perpendicularSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ
                - projection * projection;
        return perpendicularSquared <= radius * radius + 1.0E-12D;
    }

    public static double[] horizontalStep(double lookX, double lookZ, double strength) {
        double length = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (length < 1.0E-12D || strength <= 0.0D) return new double[] {0.0D, 0.0D};
        return new double[] {lookX / length * strength, lookZ / length * strength};
    }
}
