package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DamageCreditTest {
    private final UUID mob = UUID.randomUUID();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test void qualifyingAssistAndFinalBlowBothCount() {
        DamageCredit credit = new DamageCredit();
        credit.record(mob, first, 10, 4, 20);
        credit.record(mob, second, 11, 1, 20);
        assertEquals(Set.of(first, second), credit.onDeath(mob, second, 12, 20));
    }

    @Test void insufficientAndExpiredDamageDoNotCount() {
        DamageCredit credit = new DamageCredit();
        credit.record(mob, first, 10, 3.99f, 20);
        assertEquals(Set.of(), credit.onDeath(mob, null, 11, 20));
        credit.record(mob, first, 10, 20, 20);
        assertEquals(Set.of(), credit.onDeath(mob, null, 611, 20));
    }

    @Test void healedAndRepeatedHitsUseOnlyActualDamageInWindow() {
        DamageCredit credit = new DamageCredit();
        credit.record(mob, first, 10, 2, 20);
        credit.record(mob, first, 20, 2, 20);
        assertEquals(Set.of(first), credit.onDeath(mob, null, 30, 20));
        assertEquals(Set.of(), credit.onDeath(mob, null, 30, 20));
    }
}
