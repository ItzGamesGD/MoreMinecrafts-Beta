package dev.itzgamesgd.moreminecrafts.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchEcologyDamageTest {
    @Test void hoglinAndAquaticLevelsUseApprovedBonuses() {
        for (int number : new int[]{44, 45}) {
            assertEquals(0, ResearchEcologyDamage.paidBonus(number, 0));
            assertEquals(0.04, ResearchEcologyDamage.paidBonus(number, 1));
            assertEquals(0.06, ResearchEcologyDamage.paidBonus(number, 2));
            assertEquals(0.08, ResearchEcologyDamage.paidBonus(number, 3));
        }
    }

    @Test void bossLevelsUseApprovedBonuses() {
        assertEquals(0, ResearchEcologyDamage.paidBonus(47, 0));
        assertEquals(0.03, ResearchEcologyDamage.paidBonus(47, 1));
        assertEquals(0.05, ResearchEcologyDamage.paidBonus(47, 2));
        assertEquals(0.07, ResearchEcologyDamage.paidBonus(47, 3));
    }
}
