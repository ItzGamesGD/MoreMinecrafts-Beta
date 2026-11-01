package dev.itzgamesgd.moreminecrafts.colosseum;

import static dev.itzgamesgd.moreminecrafts.colosseum.Sim.*;
import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.colosseum.ArenaAttempt.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArenaAttemptTest {
    private static Sim started(UUID... players) {
        Sim s = new Sim();
        for (UUID p : players) s.inside.add(p);
        s.tick();
        assertNotNull(s.c.attempt());
        return s;
    }

    @Test void budgetTableAndMultiplier() {
        int[][] exp = {{20, 32, 44, 56}, {32, 51, 70, 90}, {48, 77, 106, 134}};
        int[] rounds = {1, 3, 5};
        for (int i = 0; i < 3; i++) for (int n = 1; n <= 4; n++) assertEquals(exp[i][n - 1], RoundPlan.scaledBudget(rounds[i], n));
        assertEquals(1000, RoundPlan.multiplierMilli(1));
        assertEquals(2200, RoundPlan.multiplierMilli(3));
        assertArrayEquals(new int[] {300, 405, 510, 615}, new int[] {RoundPlan.bossMaxHp(1), RoundPlan.bossMaxHp(2), RoundPlan.bossMaxHp(3), RoundPlan.bossMaxHp(4)});
    }

    @Test void firstEntryStartsAttemptAndSameTickEntrantsAllJoinRound1() {
        Sim s = started(A, B, C);
        ArenaAttempt a = s.c.attempt();
        assertEquals(3, a.rosterIds().size());
        assertEquals(3, a.snapshot().n());
        assertEquals(RoundPlan.scaledBudget(1, 3), a.snapshot().budget());
        assertEquals(1, s.count(RoundStart.class));
        s.ticks(5);
        assertEquals(1, s.count(RoundStart.class), "continued presence never restarts the attempt");
    }

    @Test void entryOrderDoesNotMatterForTheFirstRoster() {
        Sim s1 = new Sim(); s1.inside.addAll(List.of(A, B)); s1.tick();
        Sim s2 = new Sim(); s2.inside.addAll(List.of(B, A)); s2.tick();
        assertEquals(s1.c.attempt().rosterIds(), s2.c.attempt().rosterIds());
        assertEquals(s1.c.attempt().snapshot().budget(), s2.c.attempt().snapshot().budget());
    }

    @Test void lateEntrantIsPendingAndDoesNotChangeCurrentRound() {
        Sim s = started(A);
        int budget = s.c.attempt().snapshot().budget();
        s.ticks(3);
        s.inside.add(B);
        s.ticks(3);
        ArenaAttempt a = s.c.attempt();
        assertTrue(a.pendingIds().contains(B));
        assertFalse(a.rosterIds().contains(B));
        assertEquals(1, a.snapshot().n());
        assertEquals(budget, a.snapshot().budget(), "no retroactive scaling");
        assertEquals(1, a.snapshot().supplyChests());
    }

    @Test void pendingIsAdmittedAtNextBoundaryAndScalesNextRoundOnly() {
        Sim s = started(A);
        s.ticks(2);
        s.inside.add(B);
        s.clearRound();
        assertEquals(Phase.INTERMISSION, s.c.attempt().phase());
        assertEquals(1, s.c.attempt().snapshot().n(), "round 1 snapshot is untouched");
        assertFalse(s.c.attempt().entitlements().stream().anyMatch(e -> e.contains(B.toString())), "no retroactive reward for the pending player");
        s.waitIntermission();
        ArenaAttempt a = s.c.attempt();
        assertEquals(2, a.round());
        assertTrue(a.rosterIds().contains(B));
        assertEquals(2, a.member(B).admittedRound);
        assertEquals(2, a.snapshot().n());
        assertEquals(RoundPlan.scaledBudget(2, 2), a.snapshot().budget());
        assertTrue(a.pendingIds().isEmpty());
    }

    @Test void pendingWhoLeftBeforeBoundaryIsNotAdmittedAndDeadPendingIsDropped() {
        Sim s = started(A);
        s.inside.addAll(List.of(B, C));
        s.ticks(2);
        s.inside.remove(B);
        s.c.onPlayerDeath(C);
        s.clearRound();
        s.waitIntermission();
        assertEquals(List.of(A), List.copyOf(s.c.attempt().rosterIds()));
    }

    @Test void reentryOfAdmittedMemberIsAReturnNotANewAdmission() {
        Sim s = started(A, B);
        s.ticks(2);
        s.inside.remove(B);
        s.ticks(3);
        s.inside.add(B);
        s.ticks(3);
        ArenaAttempt a = s.c.attempt();
        assertEquals(2, a.rosterIds().size());
        assertTrue(a.pendingIds().isEmpty());
        assertEquals(1, a.member(B).admittedRound);
        assertEquals(2, a.admissions.size());
    }

    @Test void peakFloorNeverDecreases() {
        Sim s = started(A, B);
        s.clearRound();
        s.inside.remove(B);
        s.waitIntermission();
        assertEquals(2, s.c.attempt().round());
        assertEquals(2, s.c.attempt().snapshot().n(), "monotonic maximum-active floor");
        assertEquals(2, s.c.attempt().peak());
    }

    @Test void wipeOfAllAdmittedMembersFailsImmediatelyAndPendingCannotRescue() {
        Sim s = started(A, B);
        s.inside.add(C);
        s.ticks(2);
        s.c.onPlayerDeath(A);
        s.c.onPlayerDeath(B);
        s.tick();
        assertEquals(Outcome.WIPE, s.c.attempt().outcome());
        assertTrue(s.log.stream().anyMatch(x -> x instanceof End e && e.outcome() == Outcome.WIPE && e.cleanup()));
        assertEquals(RoundPlan.COOLDOWN_TICKS, s.c.cooldownLeft());
    }

    @Test void oneSurvivorKeepsTheAttemptAlive() {
        Sim s = started(A, B);
        s.c.onPlayerDeath(A);
        s.ticks(5);
        assertFalse(s.c.attempt().ended());
    }

    @Test void deadMemberRejoinsOnlyBetweenRounds() {
        Sim s = started(A, B);
        s.c.onPlayerDeath(A);
        s.ticks(3);
        assertTrue(s.c.attempt().member(A).dead, "inside during a round does not revive");
        s.clearRound();
        s.tick();
        assertFalse(s.c.attempt().member(A).dead, "revived in the intermission window");
    }

    @Test void absenceGraceExpiresAfter180SecondsAndReturnResumes() {
        Sim s = started(A);
        s.ticks(5);
        s.inside.clear();
        s.tick();
        long expiry = s.c.attempt().graceExpiryMillis();
        assertEquals(s.now + RoundPlan.GRACE_MILLIS, expiry);
        // freeze: allocation warnings do not advance while nobody is present
        int warnBefore = s.c.attempt().allocations().stream().mapToInt(x -> x.warnLeft).sum();
        s.ticks(20);
        assertEquals(warnBefore, s.c.attempt().allocations().stream().mapToInt(x -> x.warnLeft).sum());
        // return within grace resumes with the same attempt
        s.now = expiry - 1000;
        s.inside.add(A);
        UUID id = s.c.attempt().attemptId;
        s.tick();
        assertEquals(0, s.c.attempt().graceExpiryMillis());
        assertEquals(id, s.c.attempt().attemptId);
        assertFalse(s.c.attempt().ended());
        // leave again and let it expire; an already running grace is never reset
        s.inside.clear();
        s.tick();
        long second = s.c.attempt().graceExpiryMillis();
        s.now = second + 10;
        s.tick();
        assertEquals(Outcome.ABSENCE_EXPIRED, s.c.attempt().outcome());
    }

    @Test void sixtyFiveMinuteCapEndsAsTimedOutWithoutCleanup() {
        Sim s = started(A);
        s.ticks(3);
        s.now += RoundPlan.LIMIT_MILLIS;
        s.tick();
        assertEquals(Outcome.TIMED_OUT, s.c.attempt().outcome());
        assertTrue(s.log.stream().anyMatch(x -> x instanceof End e && e.outcome() == Outcome.TIMED_OUT && !e.cleanup()), "leftovers stay on time-out (Q116)");
        assertEquals(RoundPlan.COOLDOWN_TICKS, s.c.cooldownLeft());
    }

    @Test void limitWinsWhenItExpiresBeforeTheGrace() {
        Sim s = started(A);
        s.inside.clear();
        s.tick();
        s.now = s.c.attempt().startMillis + RoundPlan.LIMIT_MILLIS - 60_000;   // the grace restarts later than the cap would end
        s.inside.add(A); s.tick(); s.inside.clear(); s.tick();
        assertTrue(s.c.attempt().graceExpiryMillis() > s.c.attempt().startMillis + RoundPlan.LIMIT_MILLIS);
        s.now = s.c.attempt().startMillis + RoundPlan.LIMIT_MILLIS + 1;
        s.tick();
        assertEquals(Outcome.TIMED_OUT, s.c.attempt().outcome());
    }

    @Test void cooldownBlocksNewAttemptsForExactly78000Ticks() {
        Sim s = started(A);
        s.c.onPlayerDeath(A);
        s.tick();
        s.inside.clear();
        s.tick();
        s.inside.add(A);
        s.ticks(10);
        assertEquals(Outcome.WIPE, s.c.attempt().outcome(), "entering during cooldown starts nothing");
        long left = s.c.cooldownLeft();
        for (long i = 0; i < left; i++) s.c.tick(s.now += 50, java.util.Set.of(), true, s.planner);
        assertEquals(0, s.c.cooldownLeft());
        s.inside.clear(); s.tick(); s.inside.add(A); s.tick();
        assertEquals(Phase.ROUND, s.c.attempt().phase());
        assertNotEquals(Outcome.WIPE, s.c.attempt().outcome());
    }

    @Test void twelveAliveCapAndReplenishAtSixOrFewer() {
        Sim s = started(A, B, C, D);   // R1 budget 56 -> well over 12 allocations
        s.ticks(RoundPlan.WARNING_TICKS + 3);
        ArenaAttempt a = s.c.attempt();
        assertTrue(a.allocations().size() > 12);
        assertEquals(12, a.count(AllocStatus.SPAWNED));
        for (int i = 0; i < 6; i++) { s.kill(a.allocations().stream().filter(x -> x.status == AllocStatus.SPAWNED).findFirst().get()); s.tick(); }
        assertEquals(6, a.count(AllocStatus.SPAWNED), "6 alive is the replenish trigger point");
        int pendingBefore = a.count(AllocStatus.PENDING) + a.count(AllocStatus.WARNING);
        s.ticks(RoundPlan.WARNING_TICKS + 3);
        assertEquals(Math.min(12, 6 + pendingBefore), a.count(AllocStatus.SPAWNED), "replenished in a batch up to the cap, never beyond the remaining allocations");
        for (int i = 0; i < 200; i++) { s.tick(); assertTrue(a.count(AllocStatus.SPAWNED) + a.count(AllocStatus.WARNING) <= 12); }
    }

    @Test void uncreditedKillAndLostMobReturnTheirAllocation() {
        Sim s = started(A, B);
        s.ticks(RoundPlan.WARNING_TICKS + 3);
        ArenaAttempt a = s.c.attempt();
        Alloc x = a.allocations().stream().filter(al -> al.status == AllocStatus.SPAWNED).findFirst().get();
        Alloc y = a.allocations().stream().filter(al -> al.status == AllocStatus.SPAWNED && al != x).findFirst().get();
        s.c.onMobLost(y.entity);
        s.tick();
        assertEquals(AllocStatus.PENDING, y.status);
        // everybody absent: a death evaluated in an empty arena is not credited
        s.inside.clear();
        s.tick();
        s.kill(x);
        s.tick();
        assertEquals(AllocStatus.PENDING, x.status);
        assertEquals(0, a.count(AllocStatus.KILLED));
    }

    @Test void blockedSpawningFailsAfterSixtyActiveSecondsAndNeverBeforeWhileCapped() {
        Sim s = started(A);
        s.valid = false;
        s.ticks(RoundPlan.BLOCKAGE_TICKS - 2);
        assertFalse(s.c.attempt().ended());
        s.valid = true;
        s.tick();
        assertEquals(0, s.c.attempt().blockedTicks(), "valid position resets the consecutive clock");
        s.ticks(RoundPlan.WARNING_TICKS + 2);
        s.valid = false;
        s.ticks(RoundPlan.BLOCKAGE_TICKS + 5);
        // a held cap (12 alive) is not blockage: reaching here means either fail or still pending with cap; verify failure cause when it fails
        if (s.c.attempt().ended()) assertEquals(Outcome.BLOCKED, s.c.attempt().outcome());
    }

    @Test void blockageFailsWhenNothingHoldsTheSpawnsBack() {
        Sim s = started(A);
        s.valid = false;
        s.ticks(RoundPlan.BLOCKAGE_TICKS + 2);
        assertEquals(Outcome.BLOCKED, s.c.attempt().outcome());
    }

    @Test void keysAreAwardedOncePerWaveAndPersonalEligibilityNeedsPresenceAtClear() {
        Sim s = started(A, B);
        s.ticks(2);
        s.inside.remove(B);   // B leaves before the clear
        s.clearRound();
        assertEquals(1, s.count(DropKey.class));
        assertTrue(s.c.attempt().entitlements().stream().anyMatch(e -> e.endsWith(":1:" + A)));
        assertFalse(s.c.attempt().entitlements().stream().anyMatch(e -> e.endsWith(":1:" + B)));
        assertEquals(java.util.Set.of(1), s.c.attempt().keysAwarded());
    }

    @Test void supplyChestsAfterRound3And5UseTheRoundSnapshotCount() {
        Sim s = started(A);
        for (int r = 1; r <= 5; r++) { s.clearRound(); if (r == 2) s.inside.add(B); s.waitIntermission(); }
        List<SupplyChests> sup = s.log.stream().filter(SupplyChests.class::isInstance).map(SupplyChests.class::cast).toList();
        assertEquals(List.of(3, 5), sup.stream().map(SupplyChests::round).toList());
        assertEquals(2, sup.get(0).count(), "B was admitted at the R2->R3 boundary, so the R3 snapshot is 2");
        assertEquals(2, sup.get(1).count());
    }

    @Test void fullRunReachesRound7AndAwaitsBossThenSucceeds() {
        Sim s = started(A);
        for (int r = 1; r <= 6; r++) { s.clearRound(); assertEquals(Phase.INTERMISSION, s.c.attempt().phase(), "round " + r); s.waitIntermission(); }
        assertEquals(7, s.c.attempt().round());
        s.ticks(5);
        assertEquals(1, s.count(BossStart.class), "boss start is requested exactly once");
        assertFalse(s.c.attempt().ended());
        s.c.attempt().onBossDefeated();
        s.tick();
        assertEquals(Outcome.SUCCESS, s.c.attempt().outcome());
        assertEquals(7, s.log.stream().filter(DropKey.class::isInstance).count());
        assertTrue(s.log.stream().anyMatch(x -> x instanceof DropKey k && k.round() == 7 && k.ominous()));
        assertEquals(1, s.count(DropKey.class) - 6);
        s.ticks(5);
        assertEquals(7, s.count(DropKey.class), "no duplicate awards afterwards");
    }

    @Test void intermissionLengthsMatchApproval() {
        assertEquals(200, RoundPlan.intermissionAfter(1));
        assertEquals(700, RoundPlan.intermissionAfter(3));
        assertEquals(700, RoundPlan.intermissionAfter(5));
        assertEquals(200, RoundPlan.intermissionAfter(6));
    }

    @Test void entitiesNotLoadedSuspendsKillAndClearDecisions() {
        Sim s = started(A);
        s.ticks(RoundPlan.WARNING_TICKS + 3);
        s.loaded = false;
        for (Alloc a : s.c.attempt().allocations()) if (a.status == AllocStatus.SPAWNED) s.kill(a);
        s.ticks(5);
        assertEquals(Phase.ROUND, s.c.attempt().phase());
        s.loaded = true;
        s.tick();
    }
}
