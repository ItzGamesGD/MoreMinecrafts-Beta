package dev.itzgamesgd.moreminecrafts.furnace;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FurnaceResearchRulesTest {
    @Test void fuelSavingsConvergeForEveryApprovedLevel() {
        assertEquals(200, FurnaceResearchRules.fuelDuration(200, 0, 0).burnTicks());
        for (int level = 1; level <= 3; level++) {
            double carry = 0;
            long ticks = 0;
            for (int i = 0; i < 10000; i++) {
                var step = FurnaceResearchRules.fuelDuration(200, level, carry);
                ticks += step.burnTicks();
                carry = step.remainder();
            }
            assertEquals(2000000.0 / (1 - level * 0.085), ticks + carry, 0.001);
        }
        assertTrue(FurnaceResearchRules.fuelDuration(30000, 3, 0).burnTicks() > Short.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE,
                FurnaceResearchRules.fuelDuration(Integer.MAX_VALUE, 3, 0).burnTicks());
    }

    @Test void cookingSpeedPreservesCarryAndVanillaLevelZero() {
        for (int level = 0; level <= 3; level++) {
            double carry = 0;
            int bonusTicks = 0;
            for (int i = 0; i < 100; i++) {
                var step = FurnaceResearchRules.cookingTick(1, 200, level, carry, 0);
                bonusTicks += step.timer() - 1;
                carry = step.remainder();
            }
            assertEquals(level * 10, bonusTicks);
            assertEquals(0, carry, 1.0E-8);
        }
        assertEquals(0.4, FurnaceResearchRules.cookingTick(1, 200, 0, 0.4, 0).remainder());
        assertEquals(200, FurnaceResearchRules.cookingTick(199, 200, 3, 0.9, 0).timer());
        assertEquals(0, FurnaceResearchRules.cookingTick(1, 1, 3, 0.9, 1).credit());
    }

    @Test void precisionChanceAndOutputCapacityAreBounded() {
        for (int level = 0; level <= 5; level++) {
            int successes = 0;
            for (int roll = 0; roll < 10000; roll++)
                if (FurnaceResearchRules.precisionBonus(level, roll)) successes++;
            assertEquals(level * 750, successes);
        }
        assertTrue(FurnaceResearchRules.fitsPossibleBonus(62, 1, 64));
        assertFalse(FurnaceResearchRules.fitsPossibleBonus(63, 1, 64));
        assertFalse(FurnaceResearchRules.fitsPossibleBonus(60, 4, 64));
    }
}
