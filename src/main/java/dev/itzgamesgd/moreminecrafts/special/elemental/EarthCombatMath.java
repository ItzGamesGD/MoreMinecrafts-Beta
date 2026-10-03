package dev.itzgamesgd.moreminecrafts.special.elemental;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/** Pure horizontal geometry shared by Earth's radial smash passive and forward cone active. */
public final class EarthCombatMath {
    private EarthCombatMath() {}

    public static boolean inExpandingRing(double distance, double innerRadius, double outerRadius) {
        return distance >= Math.max(0.0D, innerRadius) && distance <= outerRadius;
    }

    public static boolean isGroundedTarget(boolean onGround, double verticalDelta, double tolerance) {
        return onGround && Math.abs(verticalDelta) <= tolerance;
    }

    /** Picks the nearest valid sampled surface without allowing a remote or excessively low anchor. */
    public static Optional<Vec3> selectGroundAnchor(Vec3 impactReference, List<Vec3> groundHits,
                                                     double maxHorizontalDistance, double maxVerticalDrop) {
        Vec3 nearest = null;
        double nearestHorizontalDistanceSquared = Double.POSITIVE_INFINITY;
        for (Vec3 hit : groundHits) {
            double dx = hit.x - impactReference.x;
            double dz = hit.z - impactReference.z;
            double horizontalDistanceSquared = dx * dx + dz * dz;
            double verticalDrop = impactReference.y - hit.y;
            if (horizontalDistanceSquared > maxHorizontalDistance * maxHorizontalDistance
                    || verticalDrop < 0.0D || verticalDrop > maxVerticalDrop) continue;
            if (horizontalDistanceSquared < nearestHorizontalDistanceSquared) {
                nearest = hit;
                nearestHorizontalDistanceSquared = horizontalDistanceSquared;
            }
        }
        return Optional.ofNullable(nearest);
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
