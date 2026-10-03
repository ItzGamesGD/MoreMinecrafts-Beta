package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WeaponBalanceTest {
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
