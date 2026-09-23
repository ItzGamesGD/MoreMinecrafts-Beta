package dev.itzgamesgd.moreminecrafts.skeleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingRules.Ability;
import org.junit.jupiter.api.Test;

class SkeletonKingRulesTest {
    @Test
    void persistentIdentityKeysAreStableAndSeparate() {
        assertEquals("MoreMinecraftsSkeletonKing", SkeletonKingRules.IDENTITY_SAVE_KEY);
        assertEquals("MoreMinecraftsSkeletonKingGuardOwner", SkeletonKingRules.GUARD_OWNER_SAVE_KEY);
    }

    @Test
    void cooldownBoundariesAreInclusiveAndAbilitySpecific() {
        assertFalse(SkeletonKingRules.ready(119L, 120L));
        assertTrue(SkeletonKingRules.ready(120L, 120L));
        assertEquals(220L, SkeletonKingRules.nextReady(Ability.BARRAGE, 100L));
        assertEquals(300L, SkeletonKingRules.nextReady(Ability.GUARDS, 100L));
        assertEquals(260L, SkeletonKingRules.nextReady(Ability.CURSE, 100L));
    }

    @Test
    void schedulerRotatesDeterministicallyAndNeverOverlapsSelections() {
        assertEquals(Ability.BARRAGE, SkeletonKingRules.select(0, 200L, 200L, 200L, 200L, true, 0, true));
        assertEquals(Ability.GUARDS, SkeletonKingRules.select(1, 200L, 200L, 200L, 200L, true, 0, true));
        assertEquals(Ability.CURSE, SkeletonKingRules.select(2, 200L, 200L, 200L, 200L, true, 0, true));
        assertNull(SkeletonKingRules.select(0, 199L, 200L, 200L, 200L, true, 0, true));
        assertNull(SkeletonKingRules.select(0, 200L, 200L, 200L, 200L, false, 0, true));
    }

    @Test
    void guardCapSkipsSummonButAllowsAnotherReadyAbility() {
        assertEquals(Ability.GUARDS, SkeletonKingRules.select(1, 200L, 999L, 200L, 999L, true, 3, false));
        assertNull(SkeletonKingRules.select(1, 200L, 999L, 200L, 999L, true, 4, false));
        assertEquals(Ability.BARRAGE, SkeletonKingRules.select(1, 200L, 200L, 200L, 999L, true, 6, false));
    }

    @Test
    void barrageHasEightEvenlySpacedArrowsAcrossThirtyDegrees() {
        assertEquals(8, SkeletonKingRules.BARRAGE_ARROWS);
        assertEquals(-15.0D, SkeletonKingRules.barrageYawOffset(0), 0.0001D);
        assertEquals(15.0D, SkeletonKingRules.barrageYawOffset(7), 0.0001D);
        assertEquals(30.0D / 7.0D,
                SkeletonKingRules.barrageYawOffset(4) - SkeletonKingRules.barrageYawOffset(3), 0.0001D);
        assertThrows(IllegalArgumentException.class, () -> SkeletonKingRules.barrageYawOffset(8));
    }

    @Test
    void curseEligibilityIsBoundedAndRejectsInvalidPlayers() {
        assertTrue(SkeletonKingRules.eligiblePlayer(12.0D, true, false));
        assertFalse(SkeletonKingRules.eligiblePlayer(12.001D, true, false));
        assertFalse(SkeletonKingRules.eligiblePlayer(5.0D, false, false));
        assertFalse(SkeletonKingRules.eligiblePlayer(5.0D, true, true));
        assertNull(SkeletonKingRules.select(2, 200L, 999L, 999L, 200L, true, 0, false));
    }

    @Test
    void cleanupOnlyDestroysOwnedEncounterForTerminalRemoval() {
        assertTrue(SkeletonKingRules.shouldCleanup(true, true));
        assertFalse(SkeletonKingRules.shouldCleanup(true, false));
        assertFalse(SkeletonKingRules.shouldCleanup(false, true));
    }
}
