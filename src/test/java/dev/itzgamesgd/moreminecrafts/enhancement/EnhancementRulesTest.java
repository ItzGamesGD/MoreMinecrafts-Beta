package dev.itzgamesgd.moreminecrafts.enhancement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnhancementRulesTest {
    @Test void ordinaryTierCapsAndFutureTierExclusions() {
        assertTrue(EnhancementCategory.VANILLA.allowsTransitionTo(20));
        assertTrue(EnhancementCategory.ELEMENTAL.allowsTransitionTo(20));
        assertFalse(EnhancementCategory.VANILLA.allowsTransitionTo(21));
        assertFalse(EnhancementCategory.ELEMENTAL.allowsTransitionTo(21));
        assertFalse(EnhancementCategory.SPECIAL.allowsTransitionTo(1));
        assertFalse(EnhancementCategory.ENDGAME.allowsTransitionTo(1));
    }
    @Test void milestonesAreDerivedWithoutPromotionState() {
        for (int level : new int[]{5, 10, 15, 20}) assertTrue(EnhancementRules.isMilestone(level));
        for (int level : new int[]{0, 1, 4, 6, 19, 21}) assertFalse(EnhancementRules.isMilestone(level));
        assertEquals(0, EnhancementRules.milestoneCount(4));
        assertEquals(1, EnhancementRules.milestoneCount(5));
        assertEquals(4, EnhancementRules.milestoneCount(20));
    }
    @Test void provisionalCostPolicyIsMilestoneSensitiveAndDeterministic() {
        assertEquals(new EnhancementRules.Cost(1, 0), EnhancementRules.costFor(4));
        assertEquals(new EnhancementRules.Cost(2, 1), EnhancementRules.costFor(5));
        assertEquals(EnhancementRules.costFor(15), EnhancementRules.costFor(15));
        assertTrue(EnhancementRules.DETERMINISTIC_SUCCESS);
    }
}
