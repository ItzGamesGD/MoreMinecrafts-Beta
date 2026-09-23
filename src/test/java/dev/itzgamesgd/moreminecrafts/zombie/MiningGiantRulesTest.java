package dev.itzgamesgd.moreminecrafts.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningGiantRulesTest {
    @Test
    void naturalSpawnRequiresLowAltitudeAndExclusiveChanceBoundary() {
        assertTrue(MiningGiantRules.canSpawn(true, 32, 0.009999D));
        assertFalse(MiningGiantRules.canSpawn(true, 33, 0.0D));
        assertFalse(MiningGiantRules.canSpawn(true, 0, 0.01D));
        assertFalse(MiningGiantRules.canSpawn(false, 0, 0.0D));
    }

    @Test
    void cleaveTriggerIncludesRangeAndCooldownBoundaries() {
        assertTrue(MiningGiantRules.canStartCleave(2.5D, 100L, 100L, true, true));
        assertTrue(MiningGiantRules.canStartCleave(7.0D, 100L, 100L, true, true));
        assertFalse(MiningGiantRules.canStartCleave(2.499D, 100L, 100L, true, true));
        assertFalse(MiningGiantRules.canStartCleave(7.001D, 100L, 100L, true, true));
        assertFalse(MiningGiantRules.canStartCleave(4.0D, 99L, 100L, true, true));
    }

    @Test
    void cleaveRejectsInvalidTargetAndAirborneGiant() {
        assertFalse(MiningGiantRules.canStartCleave(4.0D, 100L, 100L, false, true));
        assertFalse(MiningGiantRules.canStartCleave(4.0D, 100L, 100L, true, false));
    }

    @Test
    void warningCancelsIfTargetOrFootingBecomesInvalid() {
        assertTrue(MiningGiantRules.canContinueCleave(true, true, false, false));
        assertFalse(MiningGiantRules.canContinueCleave(false, true, false, false));
        assertFalse(MiningGiantRules.canContinueCleave(true, false, false, false));
        assertFalse(MiningGiantRules.canContinueCleave(true, true, true, false));
        assertFalse(MiningGiantRules.canContinueCleave(true, true, false, true));
    }

    @Test
    void fixedCorridorPermitsSidewaysCounterplay() {
        assertTrue(MiningGiantRules.isInsideCleave(1.0D, 0.0D, 0.5D, 2.25D));
        assertTrue(MiningGiantRules.isInsideCleave(1.0D, 0.0D, 7.0D, 0.0D));
        assertFalse(MiningGiantRules.isInsideCleave(1.0D, 0.0D, 4.0D, 2.251D));
        assertFalse(MiningGiantRules.isInsideCleave(1.0D, 0.0D, -1.0D, 0.0D));
        assertFalse(MiningGiantRules.isInsideCleave(0.0D, 0.0D, 1.0D, 0.0D));
    }

    @Test
    void cooldownAndPrototypeDamageAreFixed() {
        assertEquals(300L, MiningGiantRules.nextCleaveTick(100L));
        assertEquals(8.0F, MiningGiantRules.CLEAVE_DAMAGE);
    }
}
