package dev.itzgamesgd.moreminecrafts.enhancement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnhancementOperationTest {
    private static final EnhancementDefinition VANILLA_SWORD =
            new EnhancementDefinition(EnhancementCategory.VANILLA, EnhancementProfile.SWORD);
    private static final EnhancementDefinition ELEMENTAL_SWORD =
            new EnhancementDefinition(EnhancementCategory.ELEMENTAL, EnhancementProfile.SWORD);

    @Test
    void advancesSequentiallyWithTargetLevelCosts() {
        var first = EnhancementOperation.plan(VANILLA_SWORD, 0, 1).orElseThrow();
        assertEquals(1, first.targetLevel());
        assertEquals(1, first.experienceLevelCost());
        assertEquals(1, first.materialCost());

        var second = EnhancementOperation.plan(VANILLA_SWORD, first.targetLevel(), 1).orElseThrow();
        assertEquals(2, second.targetLevel());
        assertEquals(1, second.experienceLevelCost());
        assertEquals(1, second.materialCost());
    }

    @Test
    void vanillaStopsAtThirtyAndElementalAtForty() {
        assertEquals(30, EnhancementOperation.plan(VANILLA_SWORD, 29, 1).orElseThrow().targetLevel());
        assertTrue(EnhancementOperation.plan(VANILLA_SWORD, 30, 1).isEmpty());

        assertEquals(40, EnhancementOperation.plan(ELEMENTAL_SWORD, 39, 1).orElseThrow().targetLevel());
        assertTrue(EnhancementOperation.plan(ELEMENTAL_SWORD, 40, 1).isEmpty());
    }

    @Test
    void nonEnhanceableCategoriesAreRejected() {
        for (EnhancementCategory category : new EnhancementCategory[]{
                EnhancementCategory.SPECIAL,
                EnhancementCategory.ENDGAME,
                EnhancementCategory.UNSUPPORTED
        }) {
            var definition = new EnhancementDefinition(category, EnhancementProfile.SWORD);
            assertTrue(EnhancementOperation.plan(definition, 0, 1).isEmpty());
        }
    }

    @Test
    void materialPolicyConsumesExactlyOneFromAnyPositiveStack() {
        assertTrue(EnhancementOperation.plan(VANILLA_SWORD, 0, 0).isEmpty());

        var one = EnhancementOperation.plan(VANILLA_SWORD, 0, 1).orElseThrow();
        var stack = EnhancementOperation.plan(VANILLA_SWORD, 0, 64).orElseThrow();
        assertEquals(1, one.materialCost());
        assertEquals(1, stack.materialCost());
    }

    @Test
    void malformedLevelsFailClosedWithoutOverflow() {
        assertTrue(EnhancementOperation.plan(VANILLA_SWORD, -1, 1).isEmpty());
        assertTrue(EnhancementOperation.plan(VANILLA_SWORD, Integer.MAX_VALUE, 1).isEmpty());
    }
}
