package dev.itzgamesgd.moreminecrafts.accessory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class T1AccessoryRulesTest {
    @Test void sevenT1sUseFiveLevelStoneOnlyEnhancementWithoutChangingT2() {
        var t1 = AccessoryDefinition.ALL.stream().filter(a -> a.registryId().startsWith("t1_")).toList();
        assertEquals(7, t1.size());
        for (var accessory : t1) {
            for (int level = 0; level < 5; level++) {
                var plan = AccessoryEnhancementRules.plan(accessory, level, level + 2, 0, 0).orElseThrow();
                assertEquals(level + 1, plan.targetLevel());
                assertEquals(level + 2, plan.stoneCost());
                assertEquals(0, plan.rawExperienceCost());
                assertEquals(0, plan.catalystCost());
                assertTrue(plan.deterministicSuccess());
                assertTrue(AccessoryEnhancementRules.plan(accessory, level, level + 1, 100, 10000).isEmpty());
            }
            assertTrue(AccessoryEnhancementRules.plan(accessory, 5, 100, 100, 10000).isEmpty());
        }
        assertEquals(40, AccessoryEnhancementRules.plan(AccessoryDefinition.EMBER_RING, 0, 1, 0, 40)
                .orElseThrow().rawExperienceCost());
    }
}
