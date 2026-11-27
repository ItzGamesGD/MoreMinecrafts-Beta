package dev.itzgamesgd.moreminecrafts.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TombGuardianCombatTest {
    @Test void enrageIsOneWayAtOrBelowFiftyPercent() {
        assertFalse(TombGuardianCombat.shouldEnrage(151.0F, 300.0F));
        assertTrue(TombGuardianCombat.shouldEnrage(150.0F, 300.0F));
        assertTrue(TombGuardianCombat.shouldEnrage(1.0F, 300.0F));
        assertFalse(TombGuardianCombat.shouldEnrage(0.0F, 300.0F));
    }

    @Test void specNumbersAreExact() {
        assertEquals(300.0D, TombGuardianTuning.MAX_HEALTH);
        assertEquals(12.0D, TombGuardianTuning.ARMOR);
        assertEquals(0.21D, TombGuardianTuning.MOVEMENT_SPEED);
        assertEquals(0.24D, TombGuardianTuning.ENRAGED_MOVEMENT_SPEED);
        assertEquals(1.35F, TombGuardianTuning.WIDTH);
        assertEquals(4.35F, TombGuardianTuning.HEIGHT);
        assertEquals(3.6975F, TombGuardianTuning.EYE_HEIGHT);
        assertEquals(80, TombGuardianTuning.AWAKENING_TICKS);
        assertEquals(26, TombGuardianTuning.SLAM_WINDUP_TICKS);
        assertEquals(180, TombGuardianTuning.SLAM_COOLDOWN_TICKS);
        assertEquals(15, TombGuardianTuning.SWEEP_WINDUP_TICKS);
        assertEquals(50, TombGuardianTuning.SWEEP_COOLDOWN_TICKS);
        assertEquals(20, TombGuardianTuning.SPIKE_WINDUP_TICKS);
        assertEquals(140, TombGuardianTuning.SPIKE_COOLDOWN_TICKS);
        assertEquals(30, TombGuardianTuning.SUMMON_WINDUP_TICKS);
        assertEquals(400, TombGuardianTuning.SUMMON_COOLDOWN_TICKS);
        assertEquals(3, TombGuardianTuning.SUMMON_MAX_ALIVE);
        assertEquals(40, TombGuardianTuning.ULTIMATE_WINDUP_TICKS);
        assertEquals(500, TombGuardianTuning.ULTIMATE_COOLDOWN_TICKS);
        assertEquals(100, TombGuardianTuning.NO_SURVIVOR_RESET_TICKS);
        assertEquals("EncounterBound", TombGuardianTuning.ENCOUNTER_BOUND_TAG);
    }

    @Test void waveFrontGrowsOutwardAndStopsAtItsRadius() {
        assertEquals(0.0D, TombGuardianCombat.waveFront(0, 6.0D));
        assertEquals(1.5D, TombGuardianCombat.waveFront(3, 6.0D));
        assertEquals(6.0D, TombGuardianCombat.waveFront(100, 6.0D));
        assertEquals(8.0D, TombGuardianCombat.waveFront(100, 8.0D));
    }

    @Test void waveTravelTimeCoversTheWholeRadius() {
        assertEquals(13, TombGuardianCombat.waveTravelTicks(6.0D));
        assertEquals(17, TombGuardianCombat.waveTravelTicks(8.0D));
        assertEquals(25, TombGuardianCombat.waveTravelTicks(12.0D));
    }

    @Test void waveHitsOnlyTheRingBandAndIsJumpable() {
        // Band around radius 3 when the front moves 2.5 -> 3.0.
        assertTrue(TombGuardianCombat.waveHits(2.8D, 2.5D, 3.0D, 0.0D, 6.0D));
        assertFalse(TombGuardianCombat.waveHits(1.0D, 2.5D, 3.0D, 0.0D, 6.0D));
        // Feet at or above 0.6 m hop over the wave.
        assertFalse(TombGuardianCombat.waveHits(2.8D, 2.5D, 3.0D, 0.6D, 6.0D));
        assertTrue(TombGuardianCombat.waveHits(2.8D, 2.5D, 3.0D, 0.59D, 6.0D));
        // Beyond the wave's own maximum radius nothing is hit.
        assertFalse(TombGuardianCombat.waveHits(9.0D, 5.5D, 6.0D, 0.0D, 6.0D));
    }

    @Test void sweepFanIsOneHundredTwentyDegreesWithinReachAndVerticalTolerance() {
        // Yaw 0 faces +z (Minecraft convention: forward = (-sin, cos)).
        assertTrue(TombGuardianCombat.sweepHits(0.0D, 0.0D, 2.0D, 0.0D), "straight ahead");
        assertTrue(TombGuardianCombat.sweepHits(0.0D, 1.7D, 1.0D, 0.0D), "inside the 60 degree half angle");
        assertFalse(TombGuardianCombat.sweepHits(0.0D, 2.0D, 1.0D, 0.0D), "beyond 60 degrees off-axis");
        assertFalse(TombGuardianCombat.sweepHits(0.0D, 0.0D, -2.0D, 0.0D), "behind the guardian");
        assertFalse(TombGuardianCombat.sweepHits(0.0D, 0.0D, 2.6D, 0.0D), "beyond 2.5 m reach");
        assertFalse(TombGuardianCombat.sweepHits(0.0D, 0.0D, 2.0D, 1.6D), "outside the vertical tolerance");
        assertTrue(TombGuardianCombat.sweepHits(0.0D, 0.0D, 2.0D, -1.5D), "at the vertical limit");
    }

    @Test void sweepFollowsTheTurnedBodyNotTheSpawnFacing() {
        // Yaw 90 faces -x. A victim at -x is hit; one at +z is now off the arc.
        assertTrue(TombGuardianCombat.sweepHits(90.0D, -2.0D, 0.0D, 0.0D));
        assertFalse(TombGuardianCombat.sweepHits(90.0D, 0.0D, 2.0D, 0.0D));
    }

    @Test void spikeColumnHasTheSpecRadiusAndHeight() {
        assertTrue(TombGuardianCombat.spikeHits(1.0D, 1.0D, 0.0D));
        assertFalse(TombGuardianCombat.spikeHits(1.5D, 1.0D, 0.0D));
        assertFalse(TombGuardianCombat.spikeHits(0.0D, 0.0D, 4.5D));
    }

    @Test void ultimateStrikesUseTheSpecRadiiAndDamage() {
        assertEquals(8.0D, TombGuardianCombat.ultimateRadius(0));
        assertEquals(12.0D, TombGuardianCombat.ultimateRadius(1));
        assertEquals(10.0F, TombGuardianCombat.ultimateDamage(0));
        assertEquals(10.0F, TombGuardianCombat.ultimateDamage(1));
        assertEquals(12.0F, TombGuardianCombat.ultimateDamage(2));
    }
}
