package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ProgressionPurchaseGateTest {
    @Test void sameResearchCannotBeSubmittedAgainUntilResultAndSyncAreCurrent() {
        var gate = new ProgressionPurchaseGate();
        assertTrue(gate.begin("research:nearby_chest_ingredients", 5));
        assertFalse(gate.begin("research:nearby_chest_ingredients", 5));
        gate.synchronizedAt(6);
        assertFalse(gate.begin("research:nearby_chest_ingredients", 6));
        gate.actionResult("research:nearby_chest_ingredients", 6);
        assertTrue(gate.begin("research:nearby_chest_ingredients", 6));
    }

    @Test void unrelatedResearchRequestsHaveIndependentLocks() {
        var gate = new ProgressionPurchaseGate();
        assertTrue(gate.begin("research:a", 5));
        assertTrue(gate.begin("research:b", 5));
        gate.actionResult("research:a", 5);
        gate.synchronizedAt(5);
        assertTrue(gate.begin("research:a", 5));
        assertFalse(gate.begin("research:b", 5));
    }
}
