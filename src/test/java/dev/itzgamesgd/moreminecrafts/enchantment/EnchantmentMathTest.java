package dev.itzgamesgd.moreminecrafts.enchantment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class EnchantmentMathTest {
    @Test void veinMiningLevelTwoHalvesAndRoundsUp() {
        assertEquals(3, EnchantmentMath.veinDurability(5, 2));
        assertEquals(5, EnchantmentMath.veinDurability(5, 1));
        assertEquals(1, EnchantmentMath.veinDurability(1, 2));
    }
    @Test void lumberjackVerticalSpanIsInclusiveAndCappedAtThirtyFour() {
        assertEquals(true, EnchantmentMath.withinTreeHeight(10, 43));
        assertEquals(false, EnchantmentMath.withinTreeHeight(10, 44));
    }
    @Test void veinMiningUsesAUniqueChunkLimitRatherThanABlockLimit() {
        assertEquals(4, EnchantmentMath.MAX_VEIN_CHUNKS);
    }
    @Test void lumberjackAppliesUnbreakingOnceArithmetically() {
        assertEquals(10, EnchantmentMath.lumberDurability(10, 0));
        assertEquals(5, EnchantmentMath.lumberDurability(10, 1));
        assertEquals(4, EnchantmentMath.lumberDurability(10, 2));
    }
    @Test void lumberjackAcceptsOneByOneAndTwoByTwoTreeEnvelopes() {
        assertEquals(true, EnchantmentMath.withinTreeEnvelope(4, 4, 10, 43, 8, 8));
        assertEquals(true, EnchantmentMath.withinTreeEnvelope(4, 5, 10, 43, 8, 9));
    }
    @Test void lumberjackRejectsOversizeStructuresBeforeProcessing() {
        assertEquals(false, EnchantmentMath.withinTreeEnvelope(4, 6, 10, 20, 8, 9));
        assertEquals(false, EnchantmentMath.withinTreeEnvelope(4, 5, 10, 44, 8, 9));
    }
    @Test void jetBootChargesAreLevelBounded() {
        assertEquals(0, EnchantmentMath.airJumps(0));
        assertEquals(3, EnchantmentMath.airJumps(9));
    }
}
