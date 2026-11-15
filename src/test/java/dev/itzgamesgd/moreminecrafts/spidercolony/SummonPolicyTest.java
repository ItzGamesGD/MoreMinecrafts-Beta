package dev.itzgamesgd.moreminecrafts.spidercolony;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SummonPolicyTest {
    @Test void combatSummonsRespectRemainingSlots() {
        assertEquals(2, SummonPolicy.allowed(true, 0));
        assertEquals(1, SummonPolicy.allowed(true, 3));
        assertEquals(0, SummonPolicy.allowed(true, 4));
        assertEquals(0, SummonPolicy.allowed(false, 0));
    }
}
