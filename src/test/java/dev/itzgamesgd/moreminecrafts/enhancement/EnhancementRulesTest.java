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
    @Test void approvedRawXpTableAndMilestoneMaterials() {
        int[] expected = {0, 40, 40, 40, 40, 40, 70, 70, 70, 70, 70, 110, 110, 110, 110, 110, 160, 160, 160, 160, 160};
        int total = 0;
        for (int target = 1; target <= 20; target++) {
            EnhancementRules.Cost cost = EnhancementRules.costFor(target);
            assertEquals(expected[target], cost.rawXpCost(), "raw XP for +" + target);
            assertEquals(EnhancementRules.isMilestone(target) ? 2 : 1, cost.materialCount(), "stones for +" + target);
            total += cost.rawXpCost();
        }
        assertEquals(1_900, total);
    }
    @Test void provisionalCostPolicyIsMilestoneSensitiveAndDeterministic() {
        assertEquals(new EnhancementRules.Cost(1, 40), EnhancementRules.costFor(4));
        assertEquals(new EnhancementRules.Cost(2, 40), EnhancementRules.costFor(5));
        assertEquals(EnhancementRules.costFor(15), EnhancementRules.costFor(15));
        assertTrue(EnhancementRules.DETERMINISTIC_SUCCESS);
    }
}
