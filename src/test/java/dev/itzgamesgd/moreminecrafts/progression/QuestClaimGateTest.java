package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuestClaimGateTest {
    @Test void rejectsIncompleteClaimedAndDuplicateRequestsAndAllowsIndependentQuests() {
        var gate = new QuestClaimGate();
        assertFalse(gate.begin("a", false, false, 0));
        assertFalse(gate.begin("a", true, true, 0));
        assertTrue(gate.begin("a", true, false, 0));
        assertFalse(gate.begin("a", true, false, 1));
        assertTrue(gate.begin("b", true, false, 1));
        gate.result("a");
        assertTrue(gate.begin("a", true, false, 2));
        assertTrue(gate.pending("b", 2));
    }
    @Test void timeoutStateAndReconnectReleasePendingWithoutChangingSavedState() {
        var gate = new QuestClaimGate();
        gate.begin("a", true, false, 10);
        assertTrue(gate.pending("a", 5009));
        assertFalse(gate.pending("a", 5010));
        assertTrue(gate.begin("a", true, false, 5010));
        gate.synchronizedState(Map.of("quest.claimed.a", 1));
        assertFalse(gate.pending("a", 5011));
        gate.begin("b", true, false, 5011);
        gate.reset();
        assertFalse(gate.pending("b", 5012));
    }
    @Test void notifiesOnlyNewCompletionsFromAppliedDeltas() {
        assertEquals(java.util.List.of("a"), QuestClaimGate.newCompletions(Map.of(),
                Map.of("quest.complete.a", 1, "quest.progress.b", 1)));
        assertTrue(QuestClaimGate.newCompletions(Map.of("quest.complete.a", 1),
                Map.of("quest.complete.a", 1)).isEmpty());
        assertTrue(QuestClaimGate.newCompletions(Map.of(), Map.of("quest.complete.a", 0)).isEmpty());
    }
}
