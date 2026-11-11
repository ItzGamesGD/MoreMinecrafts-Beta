package dev.itzgamesgd.moreminecrafts.accessory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessoryEnhancementRulesTest {
    @Test void allLevelsUseApprovedRawXpAndMaterialCosts() {
        for (int level = 0; level < 10; level++) {
            var plan = AccessoryEnhancementRules.plan(level, 12, 2, 1_000).orElseThrow();
            assertEquals(level + 1, plan.targetLevel());
            assertEquals(level < 5 ? 40 : 70, plan.rawExperienceCost());
            assertEquals(level == 4 || level == 9 ? 2 : 1, plan.stoneCost());
            assertEquals(level == 4 || level == 9 ? 1 : 0, plan.catalystCost());
            assertTrue(plan.deterministicSuccess());
        }
        assertEquals(550, AccessoryEnhancementRules.totalRawExperienceCost());
        assertEquals(12, AccessoryEnhancementRules.totalStoneCost());
        assertEquals(2, AccessoryEnhancementRules.totalCatalystCost());
    }

    @Test void rejectsInvalidTransitionsAndInsufficientInputsWithoutChangingPlan() {
        assertTrue(AccessoryEnhancementRules.plan(-1, 99, 99, 99_999).isEmpty());
        assertTrue(AccessoryEnhancementRules.plan(10, 99, 99, 99_999).isEmpty());
        assertTrue(AccessoryEnhancementRules.plan(4, 1, 1, 40).isEmpty());
        assertTrue(AccessoryEnhancementRules.plan(4, 2, 0, 40).isEmpty());
        assertTrue(AccessoryEnhancementRules.plan(4, 2, 1, 39).isEmpty());
        assertTrue(AccessoryEnhancementRules.plan(5, 0, 0, 70).isEmpty());
        assertTrue(AccessoryEnhancementRules.plan(9, 2, 1, 69).isEmpty());
        assertTrue(AccessoryEnhancementRules.plan(9, 2, 1, -1).isEmpty());
    }
}
