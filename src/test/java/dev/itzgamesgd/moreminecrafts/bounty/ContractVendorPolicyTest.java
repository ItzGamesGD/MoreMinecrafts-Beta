package dev.itzgamesgd.moreminecrafts.bounty;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractVendorPolicyTest {
    @Test void onlyApprovedProfessionsUnlockAtApprovedLevels() {
        for (String profession : ContractVendorPolicy.ELIGIBLE_PROFESSIONS) {
            assertFalse(ContractVendorPolicy.unlocked(profession, 1, ContractData.Tier.BEGINNER));
            assertTrue(ContractVendorPolicy.unlocked(profession, 2, ContractData.Tier.BEGINNER));
            assertFalse(ContractVendorPolicy.unlocked(profession, 2, ContractData.Tier.INTERMEDIATE));
            assertTrue(ContractVendorPolicy.unlocked(profession, 3, ContractData.Tier.INTERMEDIATE));
            assertFalse(ContractVendorPolicy.unlocked(profession, 3, ContractData.Tier.ADVANCED));
            assertTrue(ContractVendorPolicy.unlocked(profession, 4, ContractData.Tier.ADVANCED));
        }
        assertFalse(ContractVendorPolicy.unlocked("minecraft:armorer", 5, ContractData.Tier.BEGINNER));
    }

    @Test void stockMatchesApprovedSellerRules() {
        assertArrayEquals(new int[]{1, 2}, ContractVendorPolicy.wanderingStock(ContractData.Tier.BEGINNER));
        assertArrayEquals(new int[]{0, 1}, ContractVendorPolicy.wanderingStock(ContractData.Tier.INTERMEDIATE));
        assertArrayEquals(new int[]{0, 1}, ContractVendorPolicy.wanderingStock(ContractData.Tier.ADVANCED));
        assertEquals(2, ContractVendorPolicy.villagerStock(ContractData.Tier.BEGINNER));
        assertEquals(1, ContractVendorPolicy.villagerStock(ContractData.Tier.INTERMEDIATE));
        assertEquals(1, ContractVendorPolicy.villagerStock(ContractData.Tier.ADVANCED));
    }
}
