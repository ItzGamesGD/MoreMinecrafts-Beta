package dev.itzgamesgd.moreminecrafts.enchantment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class EnchantmentMathTest {
    @Test void veinMiningLevelTwoHalvesAndRoundsUp() {
        assertEquals(3, EnchantmentMath.veinExtraDurability(5, 2));
        assertEquals(5, EnchantmentMath.veinExtraDurability(5, 1));
    }
    @Test void lumberjackAppliesUnbreakingOnceArithmetically() {
        assertEquals(10, EnchantmentMath.lumberDurability(10, 0));
        assertEquals(5, EnchantmentMath.lumberDurability(10, 1));
    }
    @Test void jetBootChargesAreLevelBounded() {
        assertEquals(0, EnchantmentMath.airJumps(0));
        assertEquals(3, EnchantmentMath.airJumps(9));
    }
}
