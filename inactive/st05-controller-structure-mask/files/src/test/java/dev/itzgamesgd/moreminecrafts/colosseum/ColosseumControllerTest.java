package dev.itzgamesgd.moreminecrafts.colosseum;

import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus.ALIVE;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus.KILLED;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus.PENDING;
import static dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Outcome;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Phase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ColosseumControllerTest {
    private final FakeColosseumWorld world = new FakeColosseumWorld();
    private final ColosseumTrialState state = new ColosseumTrialState();

    private UUID start(int players) {
        UUID first = world.join();
        for (int i = 1; i < players; i++) world.join();
        world.atActivation = true;
        world.tick(state, 1);
        world.atActivation = false;
        return first;
    }

    /** Big rounds so the 12 cap matters: R5 with four players allocates far more than 12 mobs. */
    private void advanceToRound(int round) {
        while (state.round() < round) {
            world.clearRound(state, 4000);
            world.tick(state, state.intermissionRemaining() + 1);
        }
    }

    @Test
    void centerApproachStartsRoundOneWithLockedParticipantsAndBudget() {
        world.join();
        world.tick(state, 5);
        assertEquals(Phase.READY, state.phase(), "presence alone does not start; the center does");
        world.join();
        world.atActivation = true;
        world.tick(state, 1);
        assertEquals(Phase.ROUND, state.phase());
        assertEquals(1, state.round());
        assertEquals(2, state.lockedCount());
        assertNotNull(state.trialId());
        assertTrue(state.allocatedThreat() >= ColosseumRules.normalFillMin(32) && state.allocatedThreat() <= 32);
        assertTrue(state.allocations().stream().allMatch(a -> a.status() == PENDING || a.status() == WARNING));
    }

    @Test
    void neverMoreThanTwelveAliveAndReplenishesOnlyAtSixOrFewer() {
        start(4);
        advanceToRound(5);
        int allocated = state.allocations().size();
        assertTrue(allocated > ColosseumRules.MAX_ALIVE, "R5 with 4 players allocates " + allocated);
        world.tick(state, 1);
        assertEquals(12, state.count(WARNING), "initial batch warns up to the cap");
        world.tick(state, ColosseumRules.SPAWN_WARNING_TICKS);
        assertEquals(12, state.count(ALIVE));
        world.kill(state, 5);
        world.tick(state, 60);
        assertEquals(7, state.count(ALIVE), "7 alive: no replenishment yet");
        assertEquals(0, state.count(WARNING));
        world.kill(state, 1);
        world.tick(state, ColosseumRules.BATCH_CYCLE_TICKS + ColosseumRules.SPAWN_WARNING_TICKS + 1);
        assertEquals(12, state.count(ALIVE), "at 6 alive the batch refills to 12");
        assertEquals(allocated, state.allocations().size(), "allocations are never consumed or duplicated");
        assertEquals(6, state.count(KILLED));
        world.clearRound(state, 4000);
        assertEquals(12, world.maxAliveSeen);
        assertEquals(Phase.INTERMISSION, state.phase());
    }

    @Test
    void roundCompletesOnceAndPaysEachWaveOnce() {
        start(1);
        world.tick(state, 30);
        var allocations = state.allocations();
        world.clearRound(state, 4000);
        assertEquals(Phase.INTERMISSION, state.phase());
        assertEquals(1, world.waves.size());
        // Replayed death events for already-counted mobs change nothing.
        for (var allocation : allocations)
            assertFalse(ColosseumController.onTrialMobDeath(state, state.trialId(), allocation.index(), UUID.randomUUID(), true));
        assertEquals(ColosseumRules.ORDINARY_INTERMISSION_TICKS, state.intermissionRemaining());
        world.tick(state, 50);
        assertEquals(1, world.waves.size());
        assertEquals(ColosseumRules.ORDINARY_INTERMISSION_TICKS - 50, state.intermissionRemaining());
        world.tick(state, state.intermissionRemaining());
        assertEquals(Phase.ROUND, state.phase());
        assertEquals(2, state.round());
        world.clearRound(state, 4000);
        assertEquals(List.of(1, 2), world.waves.stream().map(WaveRecord::round).toList());
    }

    @Test
    void suppliesFollowRoundsThreeAndFiveWithTheLockedCountAndAThirtyFiveSecondInterval() {
        start(3);
        advanceToRound(3);
        world.clearRound(state, 4000);
        assertEquals(ColosseumRules.SUPPLY_INTERMISSION_TICKS, state.intermissionRemaining());
        assertEquals(1, world.supplies.size());
        assertEquals(3, world.supplies.get(0)[0]);
        assertEquals(3, world.supplies.get(0)[1], "one chest per participating player");
        assertEquals(3, state.supplyPositions().size());
        world.tick(state, ColosseumRules.SUPPLY_INTERMISSION_TICKS);
        assertEquals(4, state.round());
    }

    @Test
    void q110FailsAfterSixtyActiveSecondsOfTotalBlockageOnly() {
        start(1);
        world.positionsValid = false;
        world.tick(state, ColosseumRules.SPAWN_BLOCKED_FAIL_TICKS - 1);
        assertEquals(Phase.ROUND, state.phase(), "just under 60 seconds blocked is still running");
        int heldAt = state.blockedTicks();
        // Absence pause holds the clock.
        var players = List.copyOf(world.present);
        world.present.clear();
        world.tick(state, 600);
        assertEquals(Phase.ROUND, state.phase());
        assertEquals(heldAt, state.blockedTicks(), "the absence pause holds the clock");
        world.present.addAll(players);
        // A valid position resets the consecutive clock.
        world.positionsValid = true;
        world.tick(state, ColosseumRules.BATCH_CYCLE_TICKS);
        assertEquals(0, state.blockedTicks());
        world.positionsValid = false;
        int ticks = 0; // warned mobs cannot materialize either; the clock restarts from zero
        while (state.phase() == Phase.ROUND && ticks < 5000) { world.tick(state, 1); ticks++; }
        assertTrue(ticks >= ColosseumRules.SPAWN_BLOCKED_FAIL_TICKS
                && ticks <= ColosseumRules.SPAWN_BLOCKED_FAIL_TICKS + 2 * ColosseumRules.BATCH_CYCLE_TICKS, "failed after " + ticks);
        assertEquals(ColosseumRules.COOLDOWN_TICKS, state.cooldownRemaining());
        assertEquals(Phase.COOLDOWN, state.phase());
        assertEquals(Outcome.FAILED_SPAWN_BLOCKED, state.lastOutcome());
        assertEquals(ColosseumRules.COOLDOWN_TICKS, state.cooldownRemaining());
    }


    @Test
    void q110ClockHoldsWhileTheCapHoldsSpawning() {
        start(4);
        advanceToRound(5);
        world.tick(state, ColosseumRules.SPAWN_WARNING_TICKS + 2);
        assertEquals(12, state.count(ALIVE));
        assertTrue(state.count(PENDING) > 0);
        world.positionsValid = false;
        world.tick(state, ColosseumRules.SPAWN_BLOCKED_FAIL_TICKS * 2);
        assertEquals(Phase.ROUND, state.phase(), "12 alive: spawning is legitimately held");
        assertEquals(0, state.blockedTicks());
    }

    @Test
    void absencePausesProgressionAndExpiryResetsWithCleanupAndCooldown() {
        start(2);
        world.tick(state, 30);
        int alive = state.count(ALIVE);
        assertTrue(alive > 0);
        var players = List.copyOf(world.present);
        world.present.clear();
        world.tick(state, ColosseumRules.ABSENCE_GRACE_TICKS - 1);
        assertTrue(state.paused());
        assertEquals(Phase.ROUND, state.phase());
        world.present.add(players.get(1));
        world.tick(state, 1);
        assertFalse(state.paused(), "one participant returning within grace resumes");
        world.present.clear();
        world.tick(state, ColosseumRules.ABSENCE_GRACE_TICKS);
        assertEquals(Phase.COOLDOWN, state.phase());
        assertEquals(Outcome.FAILED_ABSENCE_RESET, state.lastOutcome());
        assertEquals(alive, world.discarded.size(), "only this trial's mobs are removed");
        assertTrue(world.entities.isEmpty());
        assertNull(state.trialId());
        world.tick(state, ColosseumRules.COOLDOWN_TICKS - 1);
        assertEquals(Phase.COOLDOWN, state.phase());
        world.tick(state, 1);
        assertEquals(Phase.READY, state.phase());
    }

    @Test
    void intermissionDoesNotAdvanceWhileEveryoneIsAway() {
        start(1);
        world.clearRound(state, 4000);
        int remaining = state.intermissionRemaining();
        var players = List.copyOf(world.present);
        world.present.clear();
        world.tick(state, 300);
        assertEquals(remaining, state.intermissionRemaining());
        world.present.addAll(players);
        world.tick(state, remaining);
        assertEquals(2, state.round());
    }

    @Test
    void deathRulesTotalPartyDefeatAndRejoinAfterClear() {
        UUID first = start(2);
        UUID second = world.present.stream().filter(p -> !p.equals(first)).findFirst().orElseThrow();
        world.tick(state, 30);
        ColosseumController.onPlayerDeath(state, first);
        world.present.remove(first);
        world.tick(state, 5);
        assertEquals(Phase.ROUND, state.phase(), "a surviving teammate continues");
        world.present.add(first); // respawned and walked back in mid-round: no rejoin yet
        world.present.remove(second);
        world.tick(state, 5);
        assertTrue(state.paused(), "a dead participant does not count as present before the round clears");
        world.present.add(second);
        world.clearRound(state, 4000);
        assertTrue(state.deadThisRound().isEmpty(), "rejoin opens once the round clears");
        world.tick(state, state.intermissionRemaining());
        assertEquals(2, state.lockedCount(), "the dead participant rejoined at the next round start");
        world.tick(state, 30);
        ColosseumController.onPlayerDeath(state, first);
        ColosseumController.onPlayerDeath(state, second);
        world.tick(state, 1);
        assertEquals(Outcome.FAILED_PARTY_DEFEAT, state.lastOutcome());
        assertEquals(Phase.COOLDOWN, state.phase());
    }

    @Test
    void lateJoinerCountsOnlyFromTheNextRound() {
        start(1);
        world.tick(state, 30);
        UUID late = world.join();
        world.tick(state, 30);
        assertEquals(1, state.lockedCount());
        assertFalse(state.lockedParticipants().contains(late));
        world.clearRound(state, 4000);
        assertEquals(1, world.waves.get(0).lockedParticipants().size());
        assertTrue(world.waves.get(0).presentAtClear().contains(late));
        world.tick(state, state.intermissionRemaining());
        assertEquals(2, state.lockedCount());
        int budget = ColosseumRules.budget(2, 2);
        assertTrue(state.allocatedThreat() >= ColosseumRules.normalFillMin(budget) && state.allocatedThreat() <= budget);
    }

    @Test
    void killsWithNoPlayerInsideAndLostMobsReturnToPending() {
        start(1);
        world.tick(state, 30);
        var victim = state.allocations().stream().filter(a -> a.status() == ALIVE).findFirst().orElseThrow();
        assertFalse(ColosseumController.onTrialMobDeath(state, state.trialId(), victim.index(), victim.entity(), false),
                "an empty-arena kill is not credited");
        assertEquals(PENDING, state.allocation(victim.index()).status());
        var lost = state.allocations().stream().filter(a -> a.status() == ALIVE).findFirst().orElseThrow();
        world.entities.remove(lost.entity());
        world.chunksLoaded = false;
        world.tick(state, 1);
        assertEquals(ALIVE, state.allocation(lost.index()).status(), "an unloaded chunk may still hold it: keep it");
        world.chunksLoaded = true;
        world.tick(state, 1);
        assertEquals(PENDING, state.allocation(lost.index()).status(), "confirmed gone: requeued, not counted");
        assertEquals(1, state.allocation(lost.index()).requeues());
        assertEquals(0, state.count(KILLED));
    }

    @Test
    void ownershipRejectsOtherTrialsAndStaleCopies() {
        start(1);
        world.tick(state, 30);
        var alive = state.allocations().stream().filter(a -> a.status() == ALIVE).findFirst().orElseThrow();
        UUID trial = state.trialId();
        assertTrue(ColosseumController.ownsEntity(state, trial, alive.index(), alive.entity()));
        assertFalse(ColosseumController.ownsEntity(state, UUID.randomUUID(), alive.index(), alive.entity()));
        assertFalse(ColosseumController.ownsEntity(state, trial, alive.index(), UUID.randomUUID()));
        assertFalse(ColosseumController.ownsEntity(state, trial, 999, alive.entity()));
        ColosseumController.forceReset(state, world);
        assertFalse(ColosseumController.ownsEntity(state, trial, alive.index(), alive.entity()), "ended trial owns nothing");
    }

    @Test
    void stateSurvivesSerializationMidRoundIncludingWarningsAndTimers() {
        start(2);
        world.tick(state, 25);
        world.kill(state, 2);
        world.tick(state, 2);
        var players = List.copyOf(world.present);
        world.present.clear();
        world.tick(state, 100);
        JsonElement saved = TestArenas.encode(state);
        ColosseumTrialState restored = TestArenas.decode(saved);
        assertEquals(saved, TestArenas.encode(restored));
        assertEquals(state.trialId(), restored.trialId());
        assertEquals(100, restored.absenceTicks());
        assertEquals(state.lockedParticipants(), restored.lockedParticipants());
        assertEquals(state.count(KILLED), restored.count(KILLED));
        assertEquals(state.count(ALIVE), restored.count(ALIVE));
        world.present.addAll(players);
        world.clearRound(restored, 4000);
        assertEquals(Phase.INTERMISSION, restored.phase());
        assertEquals(1, world.waves.size());
        assertTrue(restored.waveCleared(restored.trialId(), 1));
        ColosseumTrialState reloaded = TestArenas.roundTrip(restored);
        assertTrue(reloaded.waveCleared(restored.trialId(), 1), "payout record persists: no second payout after restart");
    }

    @Test
    void warningInProgressSurvivesSerializationWithoutDuplicates() {
        start(1);
        world.tick(state, 5);
        int warned = state.count(WARNING);
        assertTrue(warned > 0);
        ColosseumTrialState restored = TestArenas.roundTrip(state);
        assertEquals(warned, restored.count(WARNING));
        world.tick(restored, ColosseumRules.SPAWN_WARNING_TICKS);
        assertEquals(warned, restored.count(ALIVE));
        assertEquals(warned, world.entities.size(), "each warned allocation materialized exactly once");
    }

    @Test
    void bossPhaseWaitsForIntegrationAndSucceedsOnlyOnBossDefeat() {
        start(1);
        for (int round = 1; round <= 6; round++) {
            world.clearRound(state, 6000);
            assertEquals(round, world.waves.size());
            world.tick(state, state.intermissionRemaining());
        }
        assertEquals(Phase.BOSS, state.phase());
        assertEquals(7, state.round());
        world.tick(state, 200);
        assertEquals(Phase.BOSS, state.phase(), "no boss integration: never a fake success");
        assertFalse(ColosseumController.onBossDefeated(state, world, state.trialId()));
        world.bossAvailable = true;
        world.tick(state, 1);
        assertEquals(1, world.bossStarts);
        UUID trial = state.trialId();
        assertFalse(ColosseumController.onBossDefeated(state, world, UUID.randomUUID()));
        assertTrue(ColosseumController.onBossDefeated(state, world, trial));
        assertEquals(Outcome.SUCCESS, state.lastOutcome());
        assertEquals(Phase.COOLDOWN, state.phase());
        assertEquals(ColosseumRules.COOLDOWN_TICKS, state.cooldownRemaining());
        assertEquals(List.of(Outcome.SUCCESS), world.outcomes);
        assertEquals(1, world.cleanedSupplies.size());
        assertEquals(2, world.cleanedSupplies.get(0).size(), "R3 and R5 supplies removed on success");
        assertFalse(ColosseumController.onBossDefeated(state, world, trial), "success is once");
    }
}
