package dev.itzgamesgd.moreminecrafts.special;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementalHitProgressTest {
    @Test
    void weaponThresholdsMatchCheckpointTwelveContract() {
        assertEquals(10, ElementalHitProgress.threshold(ElementalHitProgress.Weapon.FIRE));
        assertEquals(10, ElementalHitProgress.threshold(ElementalHitProgress.Weapon.WIND));
        assertEquals(5, ElementalHitProgress.threshold(ElementalHitProgress.Weapon.LIGHTNING));
    }

    @Test
    void lightningProgressHasNoTimeInputOrDecayTransition() {
        SuccessfulHitCounter progress = SuccessfulHitCounter.EMPTY;
        for (int hit = 1; hit <= 4; hit++) {
            progress = progress.recordSuccessfulHit(ElementalHitProgress.LIGHTNING_THRESHOLD).counter();
            assertEquals(hit, progress.hitsTowardTrigger());
        }
        SuccessfulHitCounter.Advance proc = progress.recordSuccessfulHit(ElementalHitProgress.LIGHTNING_THRESHOLD);
        assertEquals(true, proc.triggered());
        assertEquals(SuccessfulHitCounter.EMPTY, proc.counter());
    }
}
