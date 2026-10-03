package dev.itzgamesgd.moreminecrafts.special.elemental;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthCombatMathTest {
    @Test
    void seismicWaveIncludesRadialTargetsAndExcludesTargetsOutsideItsRing() {
        assertTrue(EarthCombatMath.inExpandingRing(2.0D, 1.0D, 2.0D));
        assertTrue(EarthCombatMath.inExpandingRing(1.0D, 1.0D, 2.0D));
        assertFalse(EarthCombatMath.inExpandingRing(0.99D, 1.0D, 2.0D));
        assertFalse(EarthCombatMath.inExpandingRing(2.01D, 1.0D, 2.0D));
        assertFalse(EarthCombatMath.inExpandingRing(3.0D, 0.0D, 2.0D));
    }

    @Test
    void onlyGroundedTargetsWithinVerticalContactToleranceQualify() {
        assertTrue(EarthCombatMath.isGroundedTarget(true, 0.75D, 0.75D));
        assertTrue(EarthCombatMath.isGroundedTarget(true, -0.4D, 0.75D));
        assertFalse(EarthCombatMath.isGroundedTarget(false, 0.0D, 0.75D));
        assertFalse(EarthCombatMath.isGroundedTarget(true, 0.751D, 0.75D));
    }

    @Test
    void groundAnchorChoosesNearestLocalSurfaceForGroundedOrElevatedImpact() {
        Vec3 groundedImpact = new Vec3(2.0D, 64.25D, 3.0D);
        Vec3 centerColumn = new Vec3(2.0D, 64.0D, 3.0D);
        Vec3 nearbyColumn = new Vec3(2.5D, 64.5D, 3.0D);

        assertEquals(centerColumn, EarthCombatMath.selectGroundAnchor(groundedImpact,
                List.of(centerColumn, nearbyColumn), EarthGroundAnchor.MAX_HORIZONTAL_DISTANCE,
                EarthGroundAnchor.MAX_VERTICAL_DROP).orElseThrow());
        Vec3 elevatedImpact = new Vec3(2.0D, 65.25D, 3.0D);
        assertEquals(nearbyColumn, EarthCombatMath.selectGroundAnchor(elevatedImpact,
                List.of(nearbyColumn), EarthGroundAnchor.MAX_HORIZONTAL_DISTANCE,
                EarthGroundAnchor.MAX_VERTICAL_DROP).orElseThrow());
        Vec3 airborneImpact = new Vec3(2.0D, 70.25D, 3.0D);
        assertEquals(new Vec3(2.0D, 64.0D, 3.0D), EarthCombatMath.selectGroundAnchor(airborneImpact,
                List.of(centerColumn), EarthGroundAnchor.MAX_HORIZONTAL_DISTANCE,
                EarthGroundAnchor.MAX_VERTICAL_DROP).orElseThrow());
    }

    @Test
    void groundAnchorRejectsSurfacesOutsideLocalOrVerticalSearchBounds() {
        Vec3 impact = new Vec3(0.0D, 20.25D, 0.0D);
        assertTrue(EarthCombatMath.selectGroundAnchor(impact,
                List.of(new Vec3(0.0D, 20.5D, 0.0D)), 0.75D, 12.0D).isEmpty());
        assertTrue(EarthCombatMath.selectGroundAnchor(impact,
                List.of(new Vec3(0.76D, 19.0D, 0.0D)), 0.75D, 12.0D).isEmpty());
        assertTrue(EarthCombatMath.selectGroundAnchor(impact,
                List.of(new Vec3(0.0D, 8.0D, 0.0D)), 0.75D, 12.0D).isEmpty());
    }

    @Test
    void outwardImpulseIsRadialHorizontalAndDegenerateSafe() {
        Vec3 impulse = EarthCombatMath.radialOutwardImpulse(Vec3.ZERO, new Vec3(3.0D, 8.0D, 4.0D), 0.85D);
        assertEquals(0.51D, impulse.x, 1.0E-12D);
        assertEquals(0.0D, impulse.y, 0.0D);
        assertEquals(0.68D, impulse.z, 1.0E-12D);
        assertEquals(Vec3.ZERO, EarthCombatMath.radialOutwardImpulse(Vec3.ZERO, Vec3.ZERO, 0.85D));
    }

    @Test
    void activeShockwaveRemainsAForwardCone() {
        assertTrue(EarthCombatMath.inForwardCone(1, 0, 4, 3, 8, 48));
        assertFalse(EarthCombatMath.inForwardCone(1, 0, -2, 0, 8, 48));
        assertFalse(EarthCombatMath.inForwardCone(1, 0, 4, 5, 8, 48));
        assertFalse(EarthCombatMath.inForwardCone(1, 0, 8.01D, 0, 8, 48));
    }
}
