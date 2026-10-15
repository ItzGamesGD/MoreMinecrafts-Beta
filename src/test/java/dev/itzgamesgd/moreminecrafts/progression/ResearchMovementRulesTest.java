package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ResearchMovementRulesTest {
    @Test void graceWindowsIncludeTheirDeadlineButAreNotArmedByDefaultState() {
        assertFalse(ResearchMovementRules.inGraceWindow(0L, 0L));
        assertTrue(ResearchMovementRules.inGraceWindow(115L, 115L));
        assertFalse(ResearchMovementRules.inGraceWindow(116L, 115L));
    }
}
