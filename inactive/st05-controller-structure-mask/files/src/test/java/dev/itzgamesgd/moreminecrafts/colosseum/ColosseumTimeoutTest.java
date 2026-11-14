package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumController.LoadDecision;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumOwnership.Kind;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Outcome;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Phase;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumWorldPort.RemovalCause;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Q116 (65-minute limit, ordinary mobs stay), Q117 (boss vanishes, not a kill), same-tick ordering, load decisions. */
class ColosseumTimeoutTest {
    private final Scenario s = new Scenario();

    private List<UUID> aliveMobs() {
        return s.state.allocations().stream().filter(a -> a.status() == AllocationStatus.ALIVE)
                .map(ColosseumTrialState.Allocation::entity).toList();
    }

    @Test
    void theLimitIsExactly65MinutesOfOnlineTicksAndSeparateFromTheCooldown() {
        s.start(1);
        assertEquals(ColosseumRules.TRIAL_LIMIT_TICKS, s.state.trialRemaining());
        s.world.tick(s.state, ColosseumRules.TRIAL_LIMIT_TICKS - 1);
        assertEquals(Phase.ROUND, s.state.phase());
        assertEquals(1, s.state.trialRemaining());
        s.world.tick(s.state, 1);
        assertEquals(Phase.COOLDOWN, s.state.phase());
        assertEquals(Outcome.TIMED_OUT, s.state.lastOutcome());
        assertEquals(0, s.state.trialRemaining());
        assertEquals(ColosseumRules.COOLDOWN_TICKS, s.state.cooldownRemaining(), "its own 78,000-tick cooldown starts");
        // Two independent timers: the cooldown counts down while the limit stays at zero, then a new trial gets a full limit.
        s.world.tick(s.state, ColosseumRules.COOLDOWN_TICKS);
        assertEquals(Phase.READY, s.state.phase());
        assertEquals(0, s.state.trialRemaining());
        s.world.atActivation = true;
        s.world.tick(s.state, 1);
        assertEquals(ColosseumRules.TRIAL_LIMIT_TICKS, s.state.trialRemaining());
    }

    @Test
    void timeOutLeavesOrdinaryMobsAndSuppliesAndReleasesTheTrial() {
        s.start(1);
        s.clearThrough(3);
        s.world.tick(s.state, s.state.intermissionRemaining()); // R4 starts; supplies from R3 exist
        s.spawnMobs();
        UUID trial = s.state.trialId();
        List<UUID> mobs = aliveMobs();
        assertFalse(mobs.isEmpty());
        assertEquals(1, s.world.supplies.size());
        s.state.trialRemaining = 2;
        s.world.tick(s.state, 2);
        assertEquals(Outcome.TIMED_OUT, s.state.lastOutcome());
        assertTrue(s.world.discarded.isEmpty(), "no ordinary mob is removed");
        assertTrue(s.world.released.containsAll(mobs), "every living mob is released to external");
        assertTrue(s.world.entities.keySet().containsAll(mobs));
        assertTrue(s.world.cleanedSupplies.isEmpty(), "supply chests stay");
        assertTrue(s.state.supplyPositions().isEmpty(), "and are forgotten, not tracked for cleanup");
        assertTrue(s.state.ledger().isReleased(trial));
        assertTrue(s.state.allocations().isEmpty());
        assertTrue(s.state.bossVanish().isEmpty() && s.world.crystalEndings.isEmpty(), "no boss, so no vanishing and no ending");
        assertEquals(List.of(Outcome.TIMED_OUT), s.world.outcomes);
    }

    @Test
    void failureStillRemovesMobsAndSuppliesAndTheBoss() {
        s.start(1);
        s.clearThrough(3);
        s.world.tick(s.state, s.state.intermissionRemaining());
        s.spawnMobs();
        List<UUID> mobs = aliveMobs();
        ColosseumController.forceReset(s.state, s.world);
        assertTrue(s.world.discarded.containsAll(mobs), "failure removes this trial's mobs");
        assertEquals(1, s.world.cleanedSupplies.size(), "and its supply chests");
        assertFalse(s.state.ledger().isReleased(s.state.lastTrialId()), "a failed trial is not released");
        assertTrue(s.world.released.isEmpty() && s.state.bossVanish().isEmpty() && s.world.crystalEndings.isEmpty());
        assertEquals(Outcome.FAILED_FORCED_RESET, s.state.lastOutcome());
    }

    @Test
    void theLimitKeepsRunningWhileEveryoneIsAbsentAndBeatsTheAbsenceReset() {
        s.start(1);
        s.spawnMobs();
        s.state.trialRemaining = 500;
        var players = List.copyOf(s.world.present);
        s.world.present.clear();
        s.world.tick(s.state, 200);
        assertEquals(Phase.ROUND, s.state.phase());
        assertTrue(s.state.paused());
        assertEquals(300, s.state.trialRemaining(), "Q116: the limit runs through the pause");
        s.state.trialRemaining = 100;
        s.world.tick(s.state, 100);
        assertEquals(Outcome.TIMED_OUT, s.state.lastOutcome(), "not an absence reset: the grace had not expired");
        assertFalse(players.isEmpty());
    }

    @Test
    void leftoverMobsAreNotCountedAfterTheTimeOutAndPayNothing() {
        s.start(1);
        s.spawnMobs();
        UUID trial = s.state.trialId();
        var mob = s.state.allocations().stream().filter(a -> a.status() == AllocationStatus.ALIVE).findFirst().orElseThrow();
        int wavesBefore = s.state.ledger().waves().size();
        s.state.trialRemaining = 1;
        s.world.tick(s.state, 1);
        assertFalse(ColosseumController.onTrialMobDeath(s.state, trial, mob.index(), mob.entity(), true),
                "a released mob's death is not a trial kill");
        assertEquals(wavesBefore, s.state.ledger().waves().size());
        assertTrue(s.world.waves.isEmpty());
        assertEquals(0, s.world.keysSpawned);
        // A later trial does not count the leftovers in its allocations, budget or alive total.
        ColosseumController.clearCooldown(s.state);
        s.world.atActivation = true;
        s.world.tick(s.state, 1);
        assertEquals(0, s.state.count(AllocationStatus.ALIVE));
        assertEquals(s.state.allocations().size(), s.state.count(AllocationStatus.PENDING) + s.state.count(AllocationStatus.WARNING));
    }

    @Test
    void timeOutInsideTheBossPhaseVanishesTheBossWithoutADefeat() {
        s.start(1);
        s.toBoss();
        assertEquals(Phase.BOSS, s.state.phase());
        UUID boss = s.world.bossEntity;
        UUID trial = s.state.trialId();
        int keysBefore = s.world.keysSpawned;
        s.state.trialRemaining = 3;
        s.world.tick(s.state, 3);
        assertEquals(Outcome.TIMED_OUT, s.state.lastOutcome());
        assertTrue(s.world.discarded.contains(boss), "removed without dying");
        assertFalse(s.world.entities.containsKey(boss));
        assertEquals(List.of(RemovalCause.VANISHED), s.world.bossRemovals);
        assertEquals(List.of(trial), s.world.crystalEndings);
        assertEquals(List.of(trial), s.world.bossVanishedEvents);
        assertEquals(List.of("boss:VANISHED", "vanished", "crystal", "ended:TIMED_OUT"), s.world.order.subList(s.world.order.size() - 4, s.world.order.size()));
        assertTrue(s.state.bossVanish().isEmpty(), "the work item is dropped once the boss is gone");
        assertFalse(ColosseumController.onBossDefeated(s.state, s.world, trial), "vanishing is not a defeat");
        assertEquals(6, s.state.ledger().waves().size(), "no R7 record");
        assertEquals(keysBefore, s.world.keysSpawned, "no boss key or any reward");
        assertEquals(ColosseumRules.COOLDOWN_TICKS, s.state.cooldownRemaining());
    }

    @Test
    void aBossInAnUnloadedChunkVanishesWhenItLoadsAndOnlyOnce() {
        s.start(1);
        s.toBoss();
        UUID boss = s.world.bossEntity;
        UUID trial = s.state.trialId();
        s.world.chunksLoaded = false;
        s.state.trialRemaining = 2;
        s.world.tick(s.state, 2);
        assertEquals(Outcome.TIMED_OUT, s.state.lastOutcome());
        assertEquals(1, s.state.bossVanish().size(), "unfinished: waiting for the chunk");
        assertEquals(BossVanish.State.PENDING, s.state.bossVanish().get(0).state());
        assertEquals(1, s.world.crystalEndings.size(), "the ending presentation is not held back by the chunk");
        assertTrue(s.world.bossVanishedEvents.isEmpty());
        s.world.tick(s.state, 200);
        assertEquals(1, s.state.bossVanish().size());
        // The boss' chunk loads and the load is rejected (I-ID2): that completes the vanishing.
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(s.state, trial, Kind.BOSS, ColosseumOwnership.BOSS_ALLOCATION, boss));
        ColosseumController.onBossLoadRejected(s.state, s.world, trial, boss);
        assertTrue(s.state.bossVanish().isEmpty());
        assertEquals(1, s.world.bossVanishedEvents.size());
        ColosseumController.onBossLoadRejected(s.state, s.world, trial, boss);
        s.world.tick(s.state, 50);
        assertEquals(1, s.world.bossVanishedEvents.size(), "never twice");
        assertEquals(1, s.world.crystalEndings.size());
    }

    @Test
    void aBossFoundLoadedOrConfirmedGoneAfterwardsAlsoCompletesTheVanish() {
        for (boolean stillThere : new boolean[]{true, false}) {
            Scenario t = new Scenario();
            t.start(1);
            t.toBoss();
            UUID boss = t.world.bossEntity;
            t.world.chunksLoaded = false;
            t.state.trialRemaining = 1;
            t.world.tick(t.state, 1);
            assertEquals(1, t.state.bossVanish().size());
            if (!stillThere) t.world.entities.remove(boss);
            t.world.chunksLoaded = true;
            t.world.tick(t.state, 2);
            assertTrue(t.state.bossVanish().isEmpty());
            assertEquals(stillThere, t.world.discarded.contains(boss), stillThere ? "found loaded: discarded" : "gone: nothing to discard");
            assertEquals(1, t.world.bossVanishedEvents.size());
        }
    }

    @Test
    void aTimeOutBeforeTheBossExistsHasNothingToVanish() {
        s.start(1);
        s.toBoss();
        // A trial waiting in BOSS whose boss integration never started one.
        Scenario t = new Scenario();
        t.start(1);
        t.clearThrough(6);
        t.world.tick(t.state, t.state.intermissionRemaining());
        t.world.tick(t.state, 3);
        assertEquals(Phase.BOSS, t.state.phase());
        assertFalse(t.state.boss().started());
        t.state.trialRemaining = 1;
        t.world.tick(t.state, 1);
        assertEquals(Outcome.TIMED_OUT, t.state.lastOutcome());
        assertTrue(t.state.bossVanish().isEmpty() && t.world.crystalEndings.isEmpty() && t.world.bossRemovals.isEmpty());
    }

    @Test
    void theLoadDecisionTableSeparatesMobsAndBossesByHowTheTrialEnded() {
        s.start(1);
        s.spawnMobs();
        UUID trial = s.state.trialId();
        var mob = s.state.allocations().stream().filter(a -> a.status() == AllocationStatus.ALIVE).findFirst().orElseThrow();
        // Active trial: only the recorded mob.
        assertEquals(LoadDecision.ACCEPT, ColosseumController.decideLoad(s.state, trial, Kind.MOB, mob.index(), mob.entity()));
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(s.state, trial, Kind.MOB, mob.index(), UUID.randomUUID()));
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(s.state, UUID.randomUUID(), Kind.MOB, 0, UUID.randomUUID()));
        // Failed trial: everything rejected.
        Scenario failed = new Scenario();
        failed.start(1);
        failed.spawnMobs();
        UUID failedTrial = failed.state.trialId();
        var failedMob = failed.state.allocations().stream().filter(a -> a.status() == AllocationStatus.ALIVE).findFirst().orElseThrow();
        ColosseumController.forceReset(failed.state, failed.world);
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(failed.state, failedTrial, Kind.MOB, failedMob.index(), failedMob.entity()));
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(failed.state, failedTrial, Kind.BOSS, -1, UUID.randomUUID()));
        // Timed-out trial: ordinary mobs are accepted as external, the boss is never accepted.
        s.state.trialRemaining = 1;
        s.world.tick(s.state, 1);
        assertEquals(LoadDecision.ACCEPT_RELEASED, ColosseumController.decideLoad(s.state, trial, Kind.MOB, mob.index(), mob.entity()));
        assertEquals(LoadDecision.ACCEPT_RELEASED, ColosseumController.decideLoad(s.state, trial, Kind.MOB, 99, UUID.randomUUID()));
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(s.state, trial, Kind.BOSS, -1, UUID.randomUUID()),
                "any boss copy of a timed-out trial is removed, whatever its id");
    }

    @Test
    void successRejectsStaleCopiesOfItsMobsAndBoss() {
        s.start(1);
        s.toBoss();
        UUID trial = s.state.trialId();
        UUID boss = s.world.bossEntity;
        assertEquals(LoadDecision.ACCEPT, ColosseumController.decideLoad(s.state, trial, Kind.BOSS, -1, boss));
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(s.state, trial, Kind.BOSS, -1, UUID.randomUUID()));
        assertTrue(ColosseumController.onBossDefeated(s.state, s.world, trial));
        assertEquals(Outcome.SUCCESS, s.state.lastOutcome());
        assertEquals(List.of(RemovalCause.DEFEATED), s.world.bossRemovals);
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(s.state, trial, Kind.BOSS, -1, boss));
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(s.state, trial, Kind.MOB, 0, UUID.randomUUID()));
        assertTrue(s.world.crystalEndings.isEmpty(), "a success has no time-out ending");
    }

    @Test
    void failureRemovesTheBossButDoesNotVanishItOrPlayTheEnding() {
        s.start(1);
        s.toBoss();
        UUID boss = s.world.bossEntity;
        ColosseumController.forceReset(s.state, s.world);
        assertTrue(s.world.discarded.contains(boss));
        assertEquals(List.of(RemovalCause.REMOVED), s.world.bossRemovals);
        assertTrue(s.world.crystalEndings.isEmpty() && s.state.bossVanish().isEmpty());
    }

    @Test
    void sameTickOrderingIsDeterministic() {
        // A defeat reported before the controller tick wins over the same-tick expiry.
        s.start(1);
        s.toBoss();
        UUID trial = s.state.trialId();
        s.state.trialRemaining = 1;
        assertTrue(ColosseumController.onBossDefeated(s.state, s.world, trial));
        s.world.tick(s.state, 1);
        assertEquals(Outcome.SUCCESS, s.state.lastOutcome());
        assertEquals(List.of(Outcome.SUCCESS), s.world.outcomes);
        assertTrue(s.world.crystalEndings.isEmpty());

        // Expiry is evaluated before total-party defeat.
        Scenario party = new Scenario();
        UUID p = party.start(1);
        party.spawnMobs();
        party.state.trialRemaining = 1;
        ColosseumController.onPlayerDeath(party.state, p);
        party.world.tick(party.state, 1);
        assertEquals(Outcome.TIMED_OUT, party.state.lastOutcome());

        // ... and before the absence reset that would fire on the same tick.
        Scenario absent = new Scenario();
        absent.start(1);
        absent.spawnMobs();
        absent.world.present.clear();
        absent.state.absenceTicks = ColosseumRules.ABSENCE_GRACE_TICKS - 1;
        absent.state.trialRemaining = 1;
        absent.world.tick(absent.state, 1);
        assertEquals(Outcome.TIMED_OUT, absent.state.lastOutcome());

        // After the expiry processed, a defeat report is rejected.
        Scenario late = new Scenario();
        late.start(1);
        late.toBoss();
        UUID lateTrial = late.state.trialId();
        late.state.trialRemaining = 1;
        late.world.tick(late.state, 1);
        assertFalse(ColosseumController.onBossDefeated(late.state, late.world, lateTrial));
        assertEquals(Outcome.TIMED_OUT, late.state.lastOutcome());
    }

    @Test
    void timeOutStateSurvivesASaveAndLoadIncludingThePendingVanish() {
        s.start(1);
        s.toBoss();
        UUID trial = s.state.trialId();
        UUID boss = s.world.bossEntity;
        ColosseumTrialState mid = TestArenas.roundTrip(s.state);
        assertEquals(s.state.trialRemaining(), mid.trialRemaining(), "the remaining limit is saved exactly");
        assertEquals(boss, mid.boss().entity());
        s.world.chunksLoaded = false;
        s.state.trialRemaining = 1;
        s.world.tick(s.state, 1);
        ColosseumTrialState after = TestArenas.roundTrip(s.state);
        assertEquals(1, after.bossVanish().size());
        assertEquals(boss, after.bossVanish().get(0).entity());
        assertTrue(after.ledger().isReleased(trial));
        assertEquals(LoadDecision.REJECT, ColosseumController.decideLoad(after, trial, Kind.BOSS, -1, boss));
        assertNull(after.trialId());
        assertEquals(Outcome.TIMED_OUT, after.lastOutcome());
        // Completing it on the reloaded state works exactly as on the original.
        FakeColosseumWorld w = new FakeColosseumWorld();
        ColosseumController.onBossLoadRejected(after, w, trial, boss);
        assertTrue(after.bossVanish().isEmpty());
        assertEquals(1, w.bossVanishedEvents.size());
    }
}
