package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WeaponBalanceTest {
    @Test void portableFurnaceMapsMinecraft263FuelProviderValues() {
        assertEquals(20_000, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_lava_bucket"));
        assertEquals(16_000, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_coal_block"));
        assertEquals(4_001, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_dried_kelp_block"));
        assertEquals(2_400, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_blaze_rod"));
        assertEquals(1_600, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_coal"));
        assertEquals(1_200, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_boats"));
        assertEquals(300, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_wood_blocks"));
        assertEquals(150, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_wood_slabs"));
        assertEquals(67, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_wool_carpets"));
        assertEquals(50, PortableFurnaceMenu.vanillaFuelTicksForProvider("time_bamboo"));
        assertEquals(0, PortableFurnaceMenu.vanillaFuelTicksForProvider("unknown"));
        assertEquals(67, LowerEquipmentRules.PORTABLE_FURNACE_COOK_TIME);
        assertEquals(3, LowerEquipmentRules.PORTABLE_FURNACE_FUEL_TICK_MULTIPLIER);
        assertEquals(16, LowerEquipmentRules.PORTABLE_FURNACE_STACK_LIMIT);
    }

    @Test void selectedDamageValuesStayInsideAuthorityWindows() {
        for (var value : WeaponBalance.VALUES) {
            assertTrue(value.daggerDamage() >= value.swordDamage() * 0.60F);
            assertTrue(value.daggerDamage() <= value.swordDamage() * 0.70F);
            assertTrue(value.greatswordDamage() >= value.swordDamage() * 1.30F);
            assertTrue(value.greatswordDamage() <= value.swordDamage() * 1.50F);
        }
        assertEquals(0.8F, WeaponBalance.GREATSWORD_ATTACK_SPEED);
        assertEquals(2.4F, WeaponBalance.GAUNTLET_ATTACK_SPEED);
    }
}
