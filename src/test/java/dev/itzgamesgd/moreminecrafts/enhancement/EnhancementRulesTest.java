package dev.itzgamesgd.moreminecrafts.enhancement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnhancementRulesTest {
    @Test
    void categoryCapsMatchPolicy() {
        assertTrue(EnhancementCategory.VANILLA.allowsTransitionTo(30));
        assertFalse(EnhancementCategory.VANILLA.allowsTransitionTo(31));
        assertTrue(EnhancementCategory.ELEMENTAL.allowsTransitionTo(40));
        assertFalse(EnhancementCategory.ELEMENTAL.allowsTransitionTo(41));
        assertFalse(EnhancementCategory.SPECIAL.allowsTransitionTo(1));
        assertFalse(EnhancementCategory.ENDGAME.allowsTransitionTo(1));
        assertFalse(EnhancementCategory.UNSUPPORTED.allowsTransitionTo(1));
    }

    @Test
    void xpCostsUseTargetLevelBoundaries() {
        assertEquals(1, EnhancementRules.experienceLevelCost(1));
        assertEquals(1, EnhancementRules.experienceLevelCost(5));
        assertEquals(2, EnhancementRules.experienceLevelCost(6));
        assertEquals(2, EnhancementRules.experienceLevelCost(10));
        assertEquals(3, EnhancementRules.experienceLevelCost(11));
        assertEquals(3, EnhancementRules.experienceLevelCost(20));
        assertEquals(4, EnhancementRules.experienceLevelCost(21));
        assertEquals(4, EnhancementRules.experienceLevelCost(30));
        assertEquals(5, EnhancementRules.experienceLevelCost(31));
        assertEquals(5, EnhancementRules.experienceLevelCost(40));
    }

    @Test
    void transactionConstantsRemainDeterministic() {
        assertEquals(1, EnhancementRules.MATERIAL_COST);
        assertTrue(EnhancementRules.DETERMINISTIC_SUCCESS);
    }
}
