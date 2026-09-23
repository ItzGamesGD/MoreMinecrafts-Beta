package dev.itzgamesgd.moreminecrafts.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZombieVariantRulesTest {
    @Test
    void customChanceHasExclusiveUpperBoundary() {
        assertEquals(ZombieVariant.BOMB, ZombieVariantRules.select(true, null, 0.149999D, 0.0D));
        assertEquals(ZombieVariant.NORMAL, ZombieVariantRules.select(true, null, 0.15D, 0.0D));
    }

    @Test
    void selectedVariantsUseEqualWeightBoundary() {
        assertEquals(ZombieVariant.BOMB, ZombieVariantRules.select(true, null, 0.0D, 0.499999D));
        assertEquals(ZombieVariant.LEAP, ZombieVariantRules.select(true, null, 0.0D, 0.5D));
    }

    @Test
    void nonNaturalAndClassifiedZombiesStayNormal() {
        assertEquals(ZombieVariant.NORMAL, ZombieVariantRules.select(false, null, 0.0D, 0.0D));
        assertEquals(ZombieVariant.LEAP,
                ZombieVariantRules.select(true, ZombieVariant.LEAP, 0.0D, 0.0D));
    }

    @Test
    void bombDamageFallsOffWithinRadiusAndStopsOutside() {
        assertEquals(4.0F, ZombieVariantRules.bombDamage(0.0D), 0.0001F);
        assertEquals(1.2F, ZombieVariantRules.bombDamage(3.0D), 0.0001F);
        assertEquals(0.0F, ZombieVariantRules.bombDamage(3.0001D), 0.0001F);
        assertEquals(ZombieVariantRules.BOMB_DAMAGE, ZombieVariantRules.bombDamage(0.0D));
    }

    @Test
    void leapRangeIncludesBothBoundaries() {
        assertTrue(ZombieVariantRules.canTriggerLeap(3.5D, 120L, 120L));
        assertTrue(ZombieVariantRules.canTriggerLeap(8.0D, 120L, 120L));
        assertFalse(ZombieVariantRules.canTriggerLeap(3.499D, 120L, 120L));
        assertFalse(ZombieVariantRules.canTriggerLeap(8.001D, 120L, 120L));
    }

    @Test
    void leapCooldownAndDamageAreFixed() {
        assertFalse(ZombieVariantRules.canTriggerLeap(5.0D, 119L, 120L));
        assertEquals(220L, ZombieVariantRules.nextLeapTick(100L));
        assertEquals(2.0F, ZombieVariantRules.LEAP_DAMAGE);
    }
}
