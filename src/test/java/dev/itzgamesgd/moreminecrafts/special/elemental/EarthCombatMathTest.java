package dev.itzgamesgd.moreminecrafts.special.elemental;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthCombatMathTest {
    @Test void fractureIsNarrowForwardLane() {
        assertTrue(EarthCombatMath.inForwardLane(1, 0, 5, 1, 7, 1.1));
        assertFalse(EarthCombatMath.inForwardLane(1, 0, -1, 0, 7, 1.1));
        assertFalse(EarthCombatMath.inForwardLane(1, 0, 5, 2, 7, 1.1));
    }

    @Test void shatterIsBroadButForwardOnly() {
        assertTrue(EarthCombatMath.inForwardCone(1, 0, 4, 3, 8, 48));
        assertFalse(EarthCombatMath.inForwardCone(1, 0, -2, 0, 8, 48));
    }
}
