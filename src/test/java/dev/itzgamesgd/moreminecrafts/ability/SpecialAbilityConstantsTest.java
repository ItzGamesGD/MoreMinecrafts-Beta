package dev.itzgamesgd.moreminecrafts.ability;

import dev.itzgamesgd.moreminecrafts.special.flame.FlameCleaveAbility;
import dev.itzgamesgd.moreminecrafts.special.wind.WindBladeAbility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecialAbilityConstantsTest {
    @Test
    void activeAbilitiesHaveDistinctIdsAndExpectedCooldowns() {
        assertNotEquals(FlameCleaveAbility.ID, WindBladeAbility.ID);
        assertEquals(600, FlameCleaveAbility.COOLDOWN_TICKS);
        assertEquals(60, WindBladeAbility.COOLDOWN_TICKS);
    }
}
