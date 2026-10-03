package dev.itzgamesgd.moreminecrafts.special.vfx;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Deterministic world-space sampling shared by the T2 elemental VFX primitives. */
public final class T2VfxGeometry {
    private T2VfxGeometry() {}

    public static List<Vec3> line(Vec3 start, Vec3 end, double maxSpacing) {
        double length = start.distanceTo(end);
        int segments = Math.max(1, (int) Math.ceil(length / Math.max(0.05D, maxSpacing)));
        List<Vec3> points = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            points.add(start.lerp(end, i / (double) segments));
        }
        return List.copyOf(points);
    }

    /** Samples a circular sector in the plane whose normal is {@code normal}. */
    public static List<Vec3> arc(Vec3 center, Vec3 normal, double radius,
                                 double startRadians, double endRadians, int samples) {
        Vec3 n = normalized(normal);
        if (n.lengthSqr() < 1.0E-12D || samples < 1) return List.of();
        Vec3 u = Math.abs(n.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        u = normalized(u.subtract(n.scale(u.dot(n))));
        Vec3 v = normalized(n.cross(u));
        List<Vec3> points = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            double angle = startRadians + (endRadians - startRadians) * i / samples;
            points.add(center.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius)));
        }
        return List.copyOf(points);
    }

    /** Samples a horizontal XZ ring centered at the supplied world position. */
    public static List<Vec3> ring(Vec3 center, double radius, int samples) {
        if (radius < 0.0D || samples < 3) return List.of();
        List<Vec3> points = new ArrayList<>(samples);
        for (int i = 0; i < samples; i++) {
            double angle = Math.PI * 2.0D * i / samples;
            points.add(center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius));
        }
        return List.copyOf(points);
    }

    /** Samples a horizontal forward-facing arc, centered on the supplied heading. */
    public static List<Vec3> horizontalArc(Vec3 center, Vec3 facing, double radius,
                                          double halfAngleRadians, int samples) {
        Vec3 forward = normalized(new Vec3(facing.x, 0.0D, facing.z));
        if (forward.lengthSqr() < 1.0E-12D || samples < 1) return List.of();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        List<Vec3> points = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            double angle = -halfAngleRadians + 2.0D * halfAngleRadians * i / samples;
            Vec3 offset = forward.scale(Math.cos(angle) * radius).add(right.scale(Math.sin(angle) * radius));
            points.add(center.add(offset));
        }
        return List.copyOf(points);
    }

    /** Evenly-spaced deterministic points in a compact burst volume. */
    public static List<Vec3> burst(Vec3 center, int count, double radius) {
        if (count < 1 || radius < 0.0D) return List.of();
        List<Vec3> points = new ArrayList<>(count);
        double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < count; i++) {
            double y = count == 1 ? 0.0D : 1.0D - 2.0D * (i + 0.5D) / count;
            double planar = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = goldenAngle * i;
            points.add(center.add(Math.cos(angle) * planar * radius, y * radius,
                    Math.sin(angle) * planar * radius));
        }
        return List.copyOf(points);
    }

    private static Vec3 normalized(Vec3 vector) {
        return vector.lengthSqr() < 1.0E-12D ? Vec3.ZERO : vector.normalize();
    }
}
