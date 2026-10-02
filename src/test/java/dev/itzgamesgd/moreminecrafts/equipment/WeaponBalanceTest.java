package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class WeaponBalanceTest {
    @Test void portableFurnaceUsesVanillaFuelComponentsAndRemainders() {
        assertEquals(1_600, PortableFurnaceMenu.vanillaFuelTicks(new ItemStack(Items.COAL)));
        assertEquals(16_000, PortableFurnaceMenu.vanillaFuelTicks(new ItemStack(Items.COAL_BLOCK)));
        assertEquals(20_000, PortableFurnaceMenu.vanillaFuelTicks(new ItemStack(Items.LAVA_BUCKET)));
        assertEquals(Items.BUCKET, Items.LAVA_BUCKET.getCraftingRemainder().getItem());
        var slots = new PortableFurnaceMenu.PortableContainer();
        assertEquals(16, slots.getMaxStackSize(new ItemStack(Items.COAL)));
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
