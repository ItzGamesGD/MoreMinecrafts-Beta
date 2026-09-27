package dev.itzgamesgd.moreminecrafts.fishing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingEquipmentTest {
    @Test void equipmentTierAndFishGradeAreSeparateDomains() {
        assertEquals(3, FishingEquipmentTier.requireValid(3));
        assertThrows(IllegalArgumentException.class, () -> FishingEquipmentTier.requireValid(4));
        assertEquals(5, FishingGrade.requireValid(5));
    }

    @Test void absentModulesHaveExplicitZeroTierSemantics() {
        assertEquals(0, RodEquipment.EMPTY.tier(FishingEquipmentFamily.REEL));
        assertEquals(0, RodEquipment.EMPTY.tier(FishingEquipmentFamily.CRANKBAIT));
        assertEquals(0, RodEquipment.EMPTY.tier(FishingEquipmentFamily.MINNOW));
        assertTrue(FishingLogic.accessibleSpecies(0).isEmpty());
    }

    @Test void durabilityBottomsOutAtBrokenOneAndNeverZero() {
        assertEquals(9, ModuleDurability.remainingAfterWear(10));
        assertEquals(1, ModuleDurability.remainingAfterWear(2));
        assertEquals(1, ModuleDurability.remainingAfterWear(1));
    }
}
