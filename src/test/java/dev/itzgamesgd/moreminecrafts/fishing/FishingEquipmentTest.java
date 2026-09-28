package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
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

    @Test void reelMinnowAndCrankbaitWearReturnUpdatedEquipmentWithoutMutatingOriginal() {
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values()) {
            RodEquipment original = equipmentWithRemaining(5);
            int reelDamage = original.reel().getDamageValue();
            int crankbaitDamage = original.crankbait().getDamageValue();
            int minnowDamage = original.minnow().getDamageValue();

            RodEquipment updated = original.withWear(family);

            assertEquals(reelDamage, original.reel().getDamageValue(), "original reel");
            assertEquals(crankbaitDamage, original.crankbait().getDamageValue(), "original crankbait");
            assertEquals(minnowDamage, original.minnow().getDamageValue(), "original minnow");
            assertEquals(family == FishingEquipmentFamily.REEL ? reelDamage + 1 : reelDamage,
                    updated.reel().getDamageValue(), "updated reel");
            assertEquals(family == FishingEquipmentFamily.CRANKBAIT ? crankbaitDamage + 1 : crankbaitDamage,
                    updated.crankbait().getDamageValue(), "updated crankbait");
            assertEquals(family == FishingEquipmentFamily.MINNOW ? minnowDamage + 1 : minnowDamage,
                    updated.minnow().getDamageValue(), "updated minnow");
            assertEquals(1, updated.tier(family), "module tier is unchanged");
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

    @Test void successfulCaptureWearUpdatesBothCaptureModulesOnlyOnce() {
        RodEquipment original = equipmentWithRemaining(5);

        RodEquipment updated = FishingChallenges.captureWear(original, true);

        assertEquals(5, ModuleDurability.remaining(original.reel()));
        assertEquals(5, ModuleDurability.remaining(original.minnow()));
        assertEquals(5, ModuleDurability.remaining(original.crankbait()));
        assertEquals(5, ModuleDurability.remaining(updated.reel()));
        assertEquals(4, ModuleDurability.remaining(updated.minnow()));
        assertEquals(4, ModuleDurability.remaining(updated.crankbait()));
    }

    @Test void brokenAndWrongFamilyModulesKeepExistingRemovalAndFamilySemantics() {
        RodEquipment atBrokenThreshold = equipmentWithRemaining(1);
        RodEquipment unchanged = atBrokenThreshold.withWear(FishingEquipmentFamily.MINNOW);
        assertEquals(1, ModuleDurability.remaining(unchanged.minnow()));
        assertFalse(unchanged.minnow().isEmpty());

        RodEquipment wrongFamily = new RodEquipment(
                module(FishingEquipmentFamily.CRANKBAIT, 10, 5),
                ItemStack.EMPTY,
                module(FishingEquipmentFamily.MINNOW, 10, 5));
        RodEquipment afterWrongFamilyWear = wrongFamily.withWear(FishingEquipmentFamily.REEL);
        assertEquals(5, ModuleDurability.remaining(afterWrongFamilyWear.reel()));
        assertEquals(5, ModuleDurability.remaining(afterWrongFamilyWear.minnow()));
        assertEquals(0, afterWrongFamilyWear.tier(FishingEquipmentFamily.REEL));
    }

    private static RodEquipment equipmentWithRemaining(int remaining) {
        return new RodEquipment(
                module(FishingEquipmentFamily.REEL, 10, remaining),
                module(FishingEquipmentFamily.CRANKBAIT, 10, remaining),
                module(FishingEquipmentFamily.MINNOW, 10, remaining));
    }

    private static ItemStack module(FishingEquipmentFamily family, int maxDamage, int remaining) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("moreminecrafts", "test_" + family.name().toLowerCase()));
        Item item = new FishingEquipmentItem(family, 1,
                new Item.Properties().durability(maxDamage).setId(key));
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(maxDamage - remaining);
        return stack;
    }
}
