package dev.itzgamesgd.moreminecrafts.special.vfx;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class T2VfxGeometryTest {
    @Test
    void lineIncludesEndpointsAndUsesDeterministicSpacing() {
        List<Vec3> points = T2VfxGeometry.line(Vec3.ZERO, new Vec3(1.0D, 0.0D, 0.0D), 0.4D);
        assertEquals(4, points.size());
        assertEquals(Vec3.ZERO, points.get(0));
        assertEquals(new Vec3(1.0D, 0.0D, 0.0D), points.get(points.size() - 1));
        assertTrue(points.get(0).distanceTo(points.get(1)) <= 0.4D);
    }

    @Test
    void arcAndRingStayOnTheirExpectedWorldSpaceRadius() {
        List<Vec3> arc = T2VfxGeometry.arc(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D), 2.0D,
                -Math.PI / 2.0D, Math.PI / 2.0D, 8);
        assertEquals(9, arc.size());
        assertTrue(arc.stream().allMatch(point -> Math.abs(point.length() - 2.0D) < 1.0E-12D));

        List<Vec3> ring = T2VfxGeometry.ring(new Vec3(4.0D, 10.0D, -2.0D), 3.0D, 16);
        assertEquals(16, ring.size());
        assertTrue(ring.stream().allMatch(point -> Math.abs(
                Math.hypot(point.x - 4.0D, point.z + 2.0D) - 3.0D) < 1.0E-12D));
        assertTrue(ring.stream().allMatch(point -> point.y == 10.0D));
    }

    @Test
    void horizontalArcFollowsHeadingAndBurstSamplingIsDeterministic() {
        List<Vec3> arc = T2VfxGeometry.horizontalArc(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D),
                2.0D, Math.PI / 3.0D, 6);
        assertEquals(new Vec3(0.0D, 0.0D, 2.0D), arc.get(3));
        assertTrue(arc.stream().allMatch(point -> Math.abs(point.y) < 1.0E-12D));

        List<Vec3> burstA = T2VfxGeometry.burst(Vec3.ZERO, 12, 0.8D);
        List<Vec3> burstB = T2VfxGeometry.burst(Vec3.ZERO, 12, 0.8D);
        assertEquals(burstA, burstB);
        assertTrue(burstA.stream().allMatch(point -> point.length() <= 0.8D + 1.0E-12D));
    }
}
