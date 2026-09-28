package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FishingEquipmentTest {
    @BeforeAll
    static void initializeMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

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

    @Test void moduleWearReplacementCopiesStateAndChangesOnlySelectedSlot() {
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values()) {
            RodEquipment original = equipmentWithRemaining(5);
            int reelDamage = original.reel().getDamageValue();
            int crankbaitDamage = original.crankbait().getDamageValue();
            int minnowDamage = original.minnow().getDamageValue();

            RodEquipment updated = original.withModuleUpdate(family,
                    stack -> stack.setDamageValue(stack.getDamageValue() + 1));

            assertEquals(reelDamage, original.reel().getDamageValue(), "original reel");
            assertEquals(crankbaitDamage, original.crankbait().getDamageValue(), "original crankbait");
            assertEquals(minnowDamage, original.minnow().getDamageValue(), "original minnow");
            assertEquals(family == FishingEquipmentFamily.REEL ? reelDamage + 1 : reelDamage,
                    updated.reel().getDamageValue(), "updated reel");
            assertEquals(family == FishingEquipmentFamily.CRANKBAIT ? crankbaitDamage + 1 : crankbaitDamage,
                    updated.crankbait().getDamageValue(), "updated crankbait");
            assertEquals(family == FishingEquipmentFamily.MINNOW ? minnowDamage + 1 : minnowDamage,
                    updated.minnow().getDamageValue(), "updated minnow");
        }
    }

    @Test void deniedCaptureDoesNotWearMinnowOrCrankbait() {
        RodEquipment original = equipmentWithRemaining(5);

        RodEquipment updated = FishingChallenges.captureWear(original, false);

        assertEquals(5, ModuleDurability.remaining(original.minnow()));
        assertEquals(5, ModuleDurability.remaining(original.crankbait()));
        assertEquals(5, ModuleDurability.remaining(updated.minnow()));
        assertEquals(5, ModuleDurability.remaining(updated.crankbait()));
    }

    @Test void captureWearCopiesAndUpdatesBothCaptureSlotsOnce() {
        RodEquipment original = equipmentWithRemaining(5);

        RodEquipment updated = original.withModuleUpdate(FishingEquipmentFamily.MINNOW,
                        FishingEquipmentTest::wearOnce)
                .withModuleUpdate(FishingEquipmentFamily.CRANKBAIT, FishingEquipmentTest::wearOnce);

        assertEquals(5, ModuleDurability.remaining(original.reel()));
        assertEquals(5, ModuleDurability.remaining(original.minnow()));
        assertEquals(5, ModuleDurability.remaining(original.crankbait()));
        assertEquals(5, ModuleDurability.remaining(updated.reel()));
        assertEquals(4, ModuleDurability.remaining(updated.minnow()));
        assertEquals(4, ModuleDurability.remaining(updated.crankbait()));
    }

    @Test void brokenThresholdKeepsModulePresentAndAtOneRemaining() {
        RodEquipment original = equipmentWithRemaining(1);
        RodEquipment updated = original.withModuleUpdate(FishingEquipmentFamily.MINNOW, stack ->
                stack.setDamageValue(stack.getMaxDamage()
                        - ModuleDurability.remainingAfterWear(ModuleDurability.remaining(stack))));

        assertEquals(1, ModuleDurability.remaining(updated.minnow()));
        assertEquals(1, ModuleDurability.remaining(original.minnow()));
        assertFalse(updated.minnow().isEmpty());
    }

    private static void wearOnce(ItemStack stack) {
        stack.setDamageValue(stack.getDamageValue() + 1);
    }

    private static RodEquipment equipmentWithRemaining(int remaining) {
        return new RodEquipment(module(remaining), module(remaining), module(remaining));
    }

    private static ItemStack module(int remaining) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.setDamageValue(stack.getMaxDamage() - remaining);
        return stack;
    }
}
