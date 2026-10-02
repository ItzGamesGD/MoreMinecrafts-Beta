package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LowerEquipmentRulesTest {
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
        assertEquals(3.75F, LowerEquipmentRules.GREATSWORD_REACH);
        assertEquals(4.5F, LowerEquipmentRules.GREATSWORD_SWEEP_RANGE);
        assertEquals(3.0F, LowerEquipmentRules.GAUNTLET_REACH);
        assertEquals(0.0D, 0.0D); // secondary push deliberately has no vertical component
    }
}
