package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ResearchMovementRulesTest {
    @Test void climbingUsesApprovedOneAndHalfBlocksPerSecond() {
        assertEquals(1.5D / 20.0D, ResearchMovementRules.climbVelocityPerTick(), 1.0e-9);
    }
    @Test void graceWindowsIncludeTheirDeadlineButAreNotArmedByDefaultState() {
        assertFalse(ResearchMovementRules.inGraceWindow(0L, 0L));
        assertTrue(ResearchMovementRules.inGraceWindow(115L, 115L));
        assertFalse(ResearchMovementRules.inGraceWindow(116L, 115L));
    }
}
