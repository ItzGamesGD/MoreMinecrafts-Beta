package dev.itzgamesgd.moreminecrafts.special;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ElementalHitProgressTest {
    @Test
    void lightningFifthHitAddsBonusToPrimaryDamageThenStartsNextCycle() {
        UUID player = UUID.randomUUID();
        float primaryDamage = 6.0F;

        for (int hit = 1; hit <= 4; hit++) {
            assertEquals(0.0F, ElementalHitProgress.primaryHitBonusDamage(
                    player, ElementalHitProgress.Weapon.LIGHTNING));
            assertFalse(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
            assertEquals(hit, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        }

        float procBonus = ElementalHitProgress.primaryHitBonusDamage(
                player, ElementalHitProgress.Weapon.LIGHTNING);
        assertEquals(4.0F, procBonus);
        assertEquals(10.0F, primaryDamage + procBonus,
                "The fifth hit's bonus must be part of the primary damage amount");
        assertEquals(4, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));

        assertTrue(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));

        assertEquals(0.0F, ElementalHitProgress.primaryHitBonusDamage(
                player, ElementalHitProgress.Weapon.LIGHTNING));
        assertFalse(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
        assertEquals(1, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        ElementalHitProgress.clear(player);
    }

    @Test
    void evaluatingPassiveAndActiveDamageDoesNotAdvanceLightningCounter() {
        UUID player = UUID.randomUUID();
        for (int hit = 0; hit < 4; hit++) {
            assertFalse(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
        }

        for (int i = 0; i < 3; i++) {
            assertEquals(4.0F, ElementalHitProgress.primaryHitBonusDamage(
                    player, ElementalHitProgress.Weapon.LIGHTNING));
        }
        assertEquals(4, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        assertTrue(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        ElementalHitProgress.clear(player);
    }

    @Test
    void eachFamilyUsesItsOwnThresholdAndLightningIsCumulative() {
        UUID player = UUID.randomUUID();

        assertEquals(10, ElementalHitProgress.threshold(ElementalHitProgress.Weapon.FIRE));
        assertEquals(10, ElementalHitProgress.threshold(ElementalHitProgress.Weapon.WIND));
        assertEquals(5, ElementalHitProgress.threshold(ElementalHitProgress.Weapon.LIGHTNING));

        for (int i = 0; i < 9; i++) {
            assertFalse(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.FIRE));
            assertFalse(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.WIND));
        }
        for (int i = 0; i < 4; i++) {
            assertFalse(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
        }
        assertEquals(9, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.FIRE));
        assertEquals(9, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.WIND));
        assertEquals(4, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));

        assertTrue(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.FIRE));
        assertTrue(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.WIND));
        assertTrue(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        ElementalHitProgress.clear(player);
    }

    @Test
    void switchingFamiliesOrLeavingTrackedWeaponsClearsStaleProgress() {
        UUID player = UUID.randomUUID();
        for (int i = 0; i < 4; i++) {
            ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING);
        }

        ElementalHitProgress.observeHeld(player, ElementalHitProgress.Weapon.LIGHTNING);
        ElementalHitProgress.observeHeld(player, ElementalHitProgress.Weapon.FIRE);
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));

        ElementalHitProgress.record(player, ElementalHitProgress.Weapon.FIRE);
        assertEquals(1, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.FIRE));
        ElementalHitProgress.observeHeld(player, null);
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.FIRE));
        ElementalHitProgress.clear(player);
    }

    @Test
    void serverStopClearsEveryCounterAndNewSessionStartsAtZero() {
        UUID player = UUID.randomUUID();
        for (int i = 0; i < 4; i++) {
            ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING);
        }
        ElementalHitProgress.observeHeld(player, ElementalHitProgress.Weapon.LIGHTNING);
        assertTrue(ElementalHitProgress.hasRuntimeState());
        assertEquals(4, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));

        ElementalHitProgress.onServerStopped(null);

        assertFalse(ElementalHitProgress.hasRuntimeState());
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        ElementalHitProgress.onServerStarting(null);
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        assertFalse(ElementalHitProgress.record(player, ElementalHitProgress.Weapon.LIGHTNING));
        assertEquals(1, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.LIGHTNING));
        ElementalHitProgress.clearRuntimeState();
    }

    @Test
    void explicitLifecycleClearResetsPlayerProgress() {
        UUID player = UUID.randomUUID();
        ElementalHitProgress.record(player, ElementalHitProgress.Weapon.FIRE);
        ElementalHitProgress.clear(player);
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.FIRE));
    }
}
