package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResearchVillagerDiscountTest {
    @Test void villagerDiscountRoundsReductionUpAndNeverDropsBelowOne() {
        assertEquals(0, ResearchVillagerDiscount.discountPercent(0));
        assertEquals(5, ResearchVillagerDiscount.discountPercent(1));
        assertEquals(10, ResearchVillagerDiscount.discountPercent(2));
        assertEquals(15, ResearchVillagerDiscount.discountPercent(3));
        assertEquals(15, ResearchVillagerDiscount.discountPercent(9));
        assertEquals(20, ResearchVillagerDiscount.discountedCost(20, 0));
        assertEquals(19, ResearchVillagerDiscount.discountedCost(20, 1));
        assertEquals(18, ResearchVillagerDiscount.discountedCost(20, 2));
        assertEquals(17, ResearchVillagerDiscount.discountedCost(20, 3));
        assertEquals(9, ResearchVillagerDiscount.discountedCost(10, 1));
        assertEquals(9, ResearchVillagerDiscount.discountedCost(10, 2));
        assertEquals(8, ResearchVillagerDiscount.discountedCost(10, 3));
        assertEquals(54, ResearchVillagerDiscount.discountedCost(64, 3));
        assertEquals(1, ResearchVillagerDiscount.discountedCost(1, 3));
        assertEquals(1, ResearchVillagerDiscount.discountedCost(2, 3));
        for (int cost = 1; cost <= 64; cost++) for (int level = 0; level <= 3; level++) {
            int discounted = ResearchVillagerDiscount.discountedCost(cost, level);
            assertTrue(discounted >= 1 && discounted <= cost, cost + "/" + level);
        }
    }
}
