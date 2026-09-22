package dev.itzgamesgd.moreminecrafts.special.wind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindCombatMathTest {
    @Test
    void pathIncludesForwardBoundaryButNotOffPathOrBeyondRange() {
        assertTrue(WindCombatMath.isInsidePath(1, 0, 0, 12, 0, 0, 12, 0.8));
        assertTrue(WindCombatMath.isInsidePath(1, 0, 0, 6, 0.8, 0, 12, 0.8));
        assertFalse(WindCombatMath.isInsidePath(1, 0, 0, 12.01, 0, 0, 12, 0.8));
        assertFalse(WindCombatMath.isInsidePath(1, 0, 0, 6, 0.81, 0, 12, 0.8));
        assertFalse(WindCombatMath.isInsidePath(1, 0, 0, -1, 0, 0, 12, 0.8));
    }

    @Test
    void horizontalStepIsNormalizedAndDegenerateSafe() {
        assertArrayEquals(new double[] {3, 4}, WindCombatMath.horizontalStep(0.6, 0.8, 5), 1.0E-12);
        assertArrayEquals(new double[] {0, 0}, WindCombatMath.horizontalStep(0, 0, 5), 0);
    }
}
