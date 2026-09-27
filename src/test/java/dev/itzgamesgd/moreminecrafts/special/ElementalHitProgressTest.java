package dev.itzgamesgd.moreminecrafts.special;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ElementalHitProgressTest {
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
    void explicitLifecycleClearResetsPlayerProgress() {
        UUID player = UUID.randomUUID();
        ElementalHitProgress.record(player, ElementalHitProgress.Weapon.FIRE);
        ElementalHitProgress.clear(player);
        assertEquals(0, ElementalHitProgress.progress(player, ElementalHitProgress.Weapon.FIRE));
    }
}