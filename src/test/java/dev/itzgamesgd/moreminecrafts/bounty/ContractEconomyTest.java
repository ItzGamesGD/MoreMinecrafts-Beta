package dev.itzgamesgd.moreminecrafts.bounty;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractEconomyTest {
    @Test void structureTierBoundariesMatchApprovedRows() {
        assertEquals(ContractData.Tier.BEGINNER, ContractEconomy.structureTier(ContractEconomy.Difficulty.EASY, 74));
        assertEquals(ContractData.Tier.INTERMEDIATE, ContractEconomy.structureTier(ContractEconomy.Difficulty.EASY, 75));
        assertEquals(ContractData.Tier.ADVANCED, ContractEconomy.structureTier(ContractEconomy.Difficulty.EASY, 97));
        assertEquals(ContractData.Tier.INTERMEDIATE, ContractEconomy.structureTier(ContractEconomy.Difficulty.MEDIUM, 45));
        assertEquals(ContractData.Tier.ADVANCED, ContractEconomy.structureTier(ContractEconomy.Difficulty.MEDIUM, 85));
        assertEquals(ContractData.Tier.INTERMEDIATE, ContractEconomy.structureTier(ContractEconomy.Difficulty.HARD, 20));
        assertEquals(ContractData.Tier.ADVANCED, ContractEconomy.structureTier(ContractEconomy.Difficulty.HARD, 65));
    }

    @Test void rarityAndSpecialConversionUseApprovedThresholds() {
        assertEquals(ContractEconomy.Rarity.LEGENDARY, ContractEconomy.rarity(ContractData.Tier.BEGINNER, 999));
        assertEquals(ContractEconomy.Rarity.RARE, ContractEconomy.rarity(ContractData.Tier.INTERMEDIATE, 900));
        assertEquals(ContractEconomy.Rarity.LEGENDARY, ContractEconomy.rarity(ContractData.Tier.SPECIAL, 950));
        assertEquals(ContractData.Tier.SPECIAL, ContractEconomy.convertAdvanced(ContractData.Tier.ADVANCED, 4));
        assertEquals(ContractData.Tier.ADVANCED, ContractEconomy.convertAdvanced(ContractData.Tier.ADVANCED, 5));
    }

    @Test void purchaseAndPayoutStayWithinApprovedBands() {
        assertArrayEquals(new int[]{2, 4, 5, 10}, ContractEconomy.band(ContractData.Tier.BEGINNER));
        assertArrayEquals(new int[]{6, 10, 12, 24}, ContractEconomy.band(ContractData.Tier.INTERMEDIATE));
        assertArrayEquals(new int[]{12, 20, 25, 50}, ContractEconomy.band(ContractData.Tier.ADVANCED));
    }

    @Test void eligibleContainerAppearanceRatesAreSeparateFromTierRolls() {
        assertTrue(ContractEconomy.appears(ContractEconomy.Difficulty.EASY, 4));
        assertFalse(ContractEconomy.appears(ContractEconomy.Difficulty.EASY, 5));
        assertTrue(ContractEconomy.appears(ContractEconomy.Difficulty.MEDIUM, 9));
        assertFalse(ContractEconomy.appears(ContractEconomy.Difficulty.MEDIUM, 10));
        assertTrue(ContractEconomy.appears(ContractEconomy.Difficulty.HARD, 17));
        assertFalse(ContractEconomy.appears(ContractEconomy.Difficulty.HARD, 18));
    }
}
