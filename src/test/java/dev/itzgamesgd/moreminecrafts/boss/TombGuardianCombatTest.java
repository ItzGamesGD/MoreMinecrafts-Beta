package dev.itzgamesgd.moreminecrafts.boss;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Pure hit and cadence rules for the three attacks and the 40% enrage. */
class TombGuardianCombatTest {
    @Test void enrageThresholdIsFortyPercentOfMaxHealth() {
        assertFalse(TombGuardianCombat.shouldEnrage(80.01F, 200.0F));
        assertTrue(TombGuardianCombat.shouldEnrage(80.0F, 200.0F));
        assertTrue(TombGuardianCombat.shouldEnrage(1.0F, 200.0F));
        assertFalse(TombGuardianCombat.shouldEnrage(0.0F, 200.0F), "a dead guardian does not enrage");
        assertTrue(TombGuardianCombat.shouldEnrage(40.0F, 100.0F), "derived from max health, not a fixed 80 HP");
    }

    @Test void enrageOnlySpeedsUpTempo() {
        assertEquals(44, TombGuardianCombat.attackInterval(false));
        assertEquals(38, TombGuardianCombat.attackInterval(true));
        assertEquals(26, TombGuardianCombat.slamWindup(false));
        assertEquals(22, TombGuardianCombat.slamWindup(true));
        assertEquals(180, TombGuardianCombat.slamCooldown(false));
        assertEquals(160, TombGuardianCombat.slamCooldown(true));
        assertEquals(1.15D, TombGuardianTuning.ENRAGED_SPEED_MULTIPLIER);
        assertTrue(TombGuardianCombat.recoveryAfterAttack(true) < TombGuardianCombat.recoveryAfterAttack(false));
        // Consecutive punches start exactly one attack interval apart.
        assertEquals(TombGuardianCombat.attackInterval(false),
                TombGuardianTuning.MELEE_WINDUP_TICKS + TombGuardianCombat.recoveryAfterAttack(false));
    }

    @Test void waveFrontMovesOutwardAndHitsOnlyItsBand() {
        assertEquals(0.0D, TombGuardianCombat.waveFront(0));
        assertEquals(3.0D, TombGuardianCombat.waveFront(6));
        assertEquals(TombGuardianTuning.WAVE_RADIUS, TombGuardianCombat.waveFront(1000), "never wider than radius 6");
        assertFalse(TombGuardianCombat.waveHits(3.0D, 0.0D, 0.5D, 0.0D), "not reached yet");
        assertTrue(TombGuardianCombat.waveHits(3.0D, 2.5D, 3.0D, 0.0D), "inside the front band");
        assertFalse(TombGuardianCombat.waveHits(1.0D, 2.5D, 3.0D, 0.0D), "the ring has already passed this target");
        assertFalse(TombGuardianCombat.waveHits(7.5D, 5.5D, 6.0D, 0.0D), "outside the maximum radius");
        assertFalse(TombGuardianCombat.waveHits(3.0D, 2.5D, 3.0D, 0.8D), "jumping over the wave avoids it");
        int travel = TombGuardianCombat.waveTravelTicks();
        boolean everHit = false;
        for (int t = 0; t < travel; t++)
            everHit |= TombGuardianCombat.waveHits(5.9D, TombGuardianCombat.waveFront(t), TombGuardianCombat.waveFront(t + 1), 0.0D);
        assertTrue(everHit, "the swept band cannot tunnel past a grounded target inside the radius");
    }

    @Test void statueHitsInsideTheFixedCircleOnly() {
        assertTrue(TombGuardianCombat.statueHits(0.0D, 0.0D, 0.0D));
        assertTrue(TombGuardianCombat.statueHits(1.0D, 1.0D, 0.0D));
        assertFalse(TombGuardianCombat.statueHits(1.2D, 1.2D, 0.0D), "side-step beyond radius 1.5");
        assertFalse(TombGuardianCombat.statueHits(0.0D, 0.0D, 5.0D), "above the statue column");
    }
}
