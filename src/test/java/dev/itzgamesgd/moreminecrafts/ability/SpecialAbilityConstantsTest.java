package dev.itzgamesgd.moreminecrafts.ability;

import dev.itzgamesgd.moreminecrafts.special.flame.FlameCleaveAbility;
import dev.itzgamesgd.moreminecrafts.special.wind.WindBladeAbility;
import dev.itzgamesgd.moreminecrafts.special.wind.WindStepAbility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecialAbilityConstantsTest {
    @Test
    void specialAbilitiesHaveDistinctIdsAndExpectedCooldowns() {
        assertNotEquals(FlameCleaveAbility.ID, WindBladeAbility.ID);
        assertNotEquals(FlameCleaveAbility.ID, WindStepAbility.ID);
        assertNotEquals(WindBladeAbility.ID, WindStepAbility.ID);
        assertEquals(100, FlameCleaveAbility.COOLDOWN_TICKS);
        assertEquals(60, WindBladeAbility.COOLDOWN_TICKS);
        assertEquals(80, WindStepAbility.COOLDOWN_TICKS);
    }
}
