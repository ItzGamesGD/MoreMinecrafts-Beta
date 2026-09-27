package dev.itzgamesgd.moreminecrafts.special.flame;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlameCombatMathTest {
    @Test
    void includesTargetsInsideCone() {
        assertTrue(FlameCombatMath.isInsideHorizontalCone(0, 1, 1, 2, 4, 45));
    }

    @Test
    void rejectsTargetOutsideAngle() {
        assertFalse(FlameCombatMath.isInsideHorizontalCone(0, 1, 2, 1, 4, 45));
    }

    @Test
    void rejectsTargetOutsideRange() {
        assertFalse(FlameCombatMath.isInsideHorizontalCone(0, 1, 0, 4.01, 4, 45));
    }

    @Test
    void includesRangeAndAngleBoundaries() {
        double diagonal = 4.0 / Math.sqrt(2.0);
        assertTrue(FlameCombatMath.isInsideHorizontalCone(0, 1, diagonal, diagonal, 4, 45));
    }

    @Test
    void calculatesSplashFraction() {
        assertEquals(3.15F, FlameCombatMath.splashDamage(9.0F, 0.35F), 0.0001F);
    }

    @Test
    void rejectsInvalidSplashInputs() {
        assertEquals(0.0F, FlameCombatMath.splashDamage(0.0F, 0.35F));
        assertEquals(0.0F, FlameCombatMath.splashDamage(-1.0F, 0.35F));
        assertEquals(0.0F, FlameCombatMath.splashDamage(9.0F, -0.1F));
        assertEquals(0.0F, FlameCombatMath.splashDamage(Float.NaN, 0.35F));
    }

    @Test
    void abilityContractUsesStablePrototypeValues() {
        assertEquals("moreminecrafts:flame_cleave", FlameCleaveAbility.ID.toString());
        assertEquals(600, FlameCleaveAbility.COOLDOWN_TICKS);
    }
}
