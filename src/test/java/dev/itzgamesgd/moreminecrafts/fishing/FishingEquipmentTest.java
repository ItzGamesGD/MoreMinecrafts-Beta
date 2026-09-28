package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.world.item.ItemStack;
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

    @Test void copyForUpdatePreservesOriginalAndAppliesWearExactlyOnce() {
        MutableDurability original = new MutableDurability(5);

        MutableDurability updated = RodEquipment.copyForUpdate(
                original, MutableDurability::copy, FishingEquipmentTest::wearOnce);

        assertEquals(5, original.remaining);
        assertEquals(4, updated.remaining);
    }

    @Test void deniedCaptureLeavesAllEquipmentSlotsUnchanged() {
        RodEquipment original = RodEquipment.EMPTY;

        RodEquipment updated = FishingChallenges.captureWear(original, false);

        assertTrue(updated.reel().isEmpty());
        assertTrue(updated.crankbait().isEmpty());
        assertTrue(updated.minnow().isEmpty());
    }

    @Test void equipmentReplacementUpdatesOnlyTheSelectedSlot() {
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values()) {
            int[] updatedSlots = new int[3];
            RodEquipment updated = RodEquipment.EMPTY.withModuleUpdate(family, stack -> {
                int slot = switch (family) {
                    case REEL -> 0;
                    case CRANKBAIT -> 1;
                    case MINNOW -> 2;
                };
                updatedSlots[slot]++;
            });

            assertEquals(family == FishingEquipmentFamily.REEL ? 1 : 0, updatedSlots[0]);
            assertEquals(family == FishingEquipmentFamily.CRANKBAIT ? 1 : 0, updatedSlots[1]);
            assertEquals(family == FishingEquipmentFamily.MINNOW ? 1 : 0, updatedSlots[2]);
            assertTrue(updated.reel().isEmpty());
            assertTrue(updated.crankbait().isEmpty());
            assertTrue(updated.minnow().isEmpty());
        }
    }

    private static void wearOnce(MutableDurability stack) {
        stack.remaining = ModuleDurability.remainingAfterWear(stack.remaining);
    }

    private static final class MutableDurability {
        private int remaining;

        private MutableDurability(int remaining) {
            this.remaining = remaining;
        }

        private MutableDurability copy() {
            return new MutableDurability(remaining);
        }
    }
}
