package dev.itzgamesgd.moreminecrafts.enhancement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnhancementOperationTest {
    private static final EnhancementDefinition T1 = new EnhancementDefinition(EnhancementCategory.VANILLA, EnhancementProfile.SWORD);
    private static final EnhancementDefinition T2 = new EnhancementDefinition(EnhancementCategory.ELEMENTAL, EnhancementProfile.SWORD);

    @Test void ordinaryTiersStopAtTwenty() {
        assertEquals(20, EnhancementOperation.plan(T1, 19, 2).orElseThrow().targetLevel());
        assertEquals(20, EnhancementOperation.plan(T2, 19, 2).orElseThrow().targetLevel());
        assertTrue(EnhancementOperation.plan(T1, 20, 64).isEmpty());
        assertTrue(EnhancementOperation.plan(T2, 20, 64).isEmpty());
    }
    @Test void transactionPlanExposesMilestoneStateAndCosts() {
        var ordinary = EnhancementOperation.plan(T1, 3, 1).orElseThrow();
        assertFalse(ordinary.milestone()); assertEquals(1, ordinary.experienceLevelCost());
        var milestone = EnhancementOperation.plan(T1, 4, 2).orElseThrow();
        assertTrue(milestone.milestone()); assertEquals(1, milestone.milestoneCount());
        assertEquals(2, milestone.materialCost()); assertEquals(1, milestone.experienceLevelCost());
        assertTrue(EnhancementOperation.plan(T1, 4, 1).isEmpty());
    }
    @Test void futureAndUnsupportedCategoriesRemainUnavailable() {
        for (var category : new EnhancementCategory[]{EnhancementCategory.SPECIAL, EnhancementCategory.ENDGAME, EnhancementCategory.UNSUPPORTED})
            assertTrue(EnhancementOperation.plan(new EnhancementDefinition(category, EnhancementProfile.SWORD), 0, 64).isEmpty());
    }
    @Test void malformedLevelsFailClosedWithoutOverflow() {
        assertTrue(EnhancementOperation.plan(T1, -1, 64).isEmpty());
        assertTrue(EnhancementOperation.plan(T1, Integer.MAX_VALUE, 64).isEmpty());
    }
}
