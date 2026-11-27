package dev.itzgamesgd.moreminecrafts.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.worldgen.structure.AdaptivePlacementMath;
import org.junit.jupiter.api.Test;

class AdaptivePlacementMathTest {
    private static final int MAX_Y = 319, PAD = 4, TOWER = 97, MIN = 18, MAX = 70;

    @Test void towerEquationIs219MinusT() {
        for (int t : new int[]{80, 120, 140, 160, 180, 200, 201, 202, 218, 219, 220, 240, 260})
            assertEquals(219 - t, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, t, TOWER), "T=" + t);
    }

    @Test void minimumOffset18IsFeasibleUpToT201AndInfeasibleFromT202() {
        assertTrue(AdaptivePlacementMath.feasible(MIN, MAX, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, 201, TOWER)));
        for (int t : new int[]{202, 218, 219, 220, 240, 260})
            assertFalse(AdaptivePlacementMath.feasible(MIN, MAX, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, t, TOWER)), "T=" + t);
    }

    @Test void preferredRangeIsKeptWhereFeasible() {
        for (int t : new int[]{80, 120, 140, 149})
            assertEquals(MAX, AdaptivePlacementMath.effectiveMaxOffset(MAX, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, t, TOWER)), "T=" + t);
        assertEquals(59, AdaptivePlacementMath.effectiveMaxOffset(MAX, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, 160, TOWER)));
        assertEquals(19, AdaptivePlacementMath.effectiveMaxOffset(MAX, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, 200, TOWER)));
    }

    @Test void shorterElementsHaveMoreRoom() { // temple 45, plain_medium 27: root maxY = T + h + size - 1
        assertEquals(315 - 240 - 44, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, 240, 45));
        assertEquals(315 - 240 - 26, AdaptivePlacementMath.feasibleMaxOffset(MAX_Y, PAD, 240, 27));
    }
}
