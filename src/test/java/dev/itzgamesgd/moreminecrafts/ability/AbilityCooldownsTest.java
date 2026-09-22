package dev.itzgamesgd.moreminecrafts.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityCooldownsTest {
    private static final String ABILITY_A = "moreminecrafts:ability_a";
    private static final String ABILITY_B = "moreminecrafts:ability_b";

    @Test
    void absentCooldownIsReady() {
        assertTrue(AbilityCooldowns.EMPTY.isReady(ABILITY_A, 100));
        assertEquals(0, AbilityCooldowns.EMPTY.remainingTicks(ABILITY_A, 100));
    }

    @Test
    void deadlineAndRemainingTicksUseExclusiveReadyAtBoundary() {
        AbilityCooldowns cooldowns = AbilityCooldowns.EMPTY.start(ABILITY_A, 100, 40);

        assertEquals(140, cooldowns.readyAtTicks().get(ABILITY_A));
        assertFalse(cooldowns.isReady(ABILITY_A, 100));
        assertEquals(40, cooldowns.remainingTicks(ABILITY_A, 100));
        assertFalse(cooldowns.isReady(ABILITY_A, 139));
        assertEquals(1, cooldowns.remainingTicks(ABILITY_A, 139));
        assertTrue(cooldowns.isReady(ABILITY_A, 140));
        assertEquals(0, cooldowns.remainingTicks(ABILITY_A, 140));
    }

    @Test
    void nonPositiveDurationLeavesAbilityReady() {
        assertTrue(AbilityCooldowns.EMPTY.start(ABILITY_A, 100, 0).isReady(ABILITY_A, 100));
        assertTrue(AbilityCooldowns.EMPTY.start(ABILITY_A, 100, -5).isReady(ABILITY_A, 100));
    }

    @Test
    void differentAbilityIdsAreIndependent() {
        AbilityCooldowns cooldowns = AbilityCooldowns.EMPTY.start(ABILITY_A, 100, 40);
        assertFalse(cooldowns.isReady(ABILITY_A, 100));
        assertTrue(cooldowns.isReady(ABILITY_B, 100));
    }

    @Test
    void restartingSameAbilityReplacesDeadline() {
        AbilityCooldowns cooldowns = AbilityCooldowns.EMPTY
                .start(ABILITY_A, 100, 40)
                .start(ABILITY_A, 110, 10);
        assertEquals(120, cooldowns.readyAtTicks().get(ABILITY_A));
    }

    @Test
    void cooldownKeyContainsNoItemIdentity() {
        AbilityCooldowns cooldowns = AbilityCooldowns.EMPTY.start(ABILITY_A, 100, 40);
        String duplicateProbeA = ABILITY_A;
        String duplicateProbeB = ABILITY_A;
        assertFalse(cooldowns.isReady(duplicateProbeA, 101));
        assertFalse(cooldowns.isReady(duplicateProbeB, 101));
        assertEquals(1, cooldowns.readyAtTicks().size());
    }
}
