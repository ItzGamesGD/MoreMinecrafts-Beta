package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

class LowerEquipmentRulesTest {
    @Test void injectorAcceptsOnlyOrdinaryDrinkablePotionStacks() {
        assertTrue(PotionInjectorItem.isUsablePotion(new ItemStack(Items.POTION)));
        assertFalse(PotionInjectorItem.isUsablePotion(new ItemStack(Items.SPLASH_POTION)));
        assertFalse(PotionInjectorItem.isUsablePotion(new ItemStack(Items.LINGERING_POTION)));
        assertFalse(PotionInjectorItem.isUsablePotion(new ItemStack(Items.TIPPED_ARROW)));
    }

    @Test void drillClassificationMatchesPickaxeMineableTag() {
        assertTrue(LowerEquipmentRules.isPickaxeMineable(Blocks.DEEPSLATE.defaultBlockState()));
        assertFalse(LowerEquipmentRules.isPickaxeMineable(Blocks.DIRT.defaultBlockState()));
    }

    @Test void injectorSkipsEmptySlotsInOrder() {
        assertEquals(3, LowerEquipmentRules.firstOccupiedPotionSlot(
                new boolean[]{false, false, false, true, false, true, false, false, false}));
        assertEquals(-1, LowerEquipmentRules.firstOccupiedPotionSlot(new boolean[9]));
    }

    @Test void portableFurnaceIsThreeTimesAsFastWithoutFuelEfficiency() {
        assertEquals(67, LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME);
        assertEquals(3, LowerEquipmentRules.PORTABLE_FURNACE_FUEL_TICK_MULTIPLIER);
        assertEquals(16, LowerEquipmentRules.PORTABLE_FURNACE_STACK_LIMIT);
    }

    @Test void drillFuelAccountingMatchesAuthority() {
        assertEquals(512, 64 * LowerEquipmentRules.drillFuelPoints(false));
        assertEquals(4608, 64 * LowerEquipmentRules.drillFuelPoints(true));
    }

    @Test void backpackBoundariesPreserveOneFiftyFourSlotStore() {
        assertArrayEquals(new int[]{9, 27, 54}, LowerEquipmentRules.BACKPACK_CAPACITIES);
    }

    @Test void fishTrapTimerPersistsProgressAndPausesWhileFull() {
        assertEquals(121, LowerEquipmentRules.advanceFishTrapTimer(120, false));
        assertEquals(120, LowerEquipmentRules.advanceFishTrapTimer(120, true));
        assertEquals(LowerEquipmentRules.FISH_TRAP_INTERVAL_TICKS,
                LowerEquipmentRules.advanceFishTrapTimer(LowerEquipmentRules.FISH_TRAP_INTERVAL_TICKS - 1, false));
    }

    @Test void fishTrapStopsOnlyWhenAllNineSlotsAreOccupied() {
        boolean[] slots = new boolean[9];
        assertTrue(LowerEquipmentRules.fishTrapCanProduce(slots));
        java.util.Arrays.fill(slots, true);
        assertFalse(LowerEquipmentRules.fishTrapCanProduce(slots));
        slots[8] = false;
        assertTrue(LowerEquipmentRules.fishTrapCanProduce(slots));
        assertEquals(6000, LowerEquipmentRules.FISH_TRAP_INTERVAL_TICKS);
    }

    @Test void weaponConstantsMatchApprovedGeometry() {
        assertEquals(2.5F, LowerEquipmentRules.DAGGER_REACH);
        assertEquals(0.2F, LowerEquipmentRules.greatswordDamageMultiplier(0.0F));
        assertEquals(1.0F, LowerEquipmentRules.greatswordDamageMultiplier(1.0F));
        assertTrue(LowerEquipmentRules.isWithinGreatswordSweep(new net.minecraft.world.phys.Vec3(3.5D,9,1.49D), new net.minecraft.world.phys.Vec3(1,0,0)));
        assertFalse(LowerEquipmentRules.isWithinGreatswordSweep(new net.minecraft.world.phys.Vec3(3.5D,0,1.51D), new net.minecraft.world.phys.Vec3(1,0,0)));
        assertFalse(LowerEquipmentRules.isWithinGreatswordSweep(new net.minecraft.world.phys.Vec3(-0.1D,0,0), new net.minecraft.world.phys.Vec3(1,0,0)));
        assertEquals(3.75F, LowerEquipmentRules.GREATSWORD_REACH);
        assertEquals(1.5D, LowerEquipmentRules.GREATSWORD_SWEEP_HALF_WIDTH);
        assertEquals(3.0F, LowerEquipmentRules.GAUNTLET_REACH);
        assertTrue(LowerEquipmentRules.isInGauntletSecondaryRegion(-2.0D, 0.4D));
        assertFalse(LowerEquipmentRules.isInGauntletSecondaryRegion(2.0D, 0.4D));
        assertEquals(0.8D, LowerEquipmentRules.GAUNTLET_HORIZONTAL_KNOCKBACK_SPEED);
    }
}
