package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ResearchAxeCombatRulesTest {
    @Test void firstStrikeRequiresTheFullSixtyTickQuietPeriod() {
        assertTrue(ResearchAxeCombatRules.isFirstStrike(200, Long.MIN_VALUE));
        assertFalse(ResearchAxeCombatRules.isFirstStrike(159, 100));
        assertTrue(ResearchAxeCombatRules.isFirstStrike(160, 100));
    }

    @Test void bonusScalesByPurchasedLevelAndAppliesOnlyToFirstStrike() {
        assertEquals(0.0D, ResearchAxeCombatRules.bonus(0, true));
        assertEquals(0.03D, ResearchAxeCombatRules.bonus(1, true));
        assertEquals(0.06D, ResearchAxeCombatRules.bonus(2, true));
        assertEquals(0.09D, ResearchAxeCombatRules.bonus(3, true));
        assertEquals(0.0D, ResearchAxeCombatRules.bonus(3, false));
    }
}
