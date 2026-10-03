package dev.itzgamesgd.moreminecrafts.special.elemental;

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
