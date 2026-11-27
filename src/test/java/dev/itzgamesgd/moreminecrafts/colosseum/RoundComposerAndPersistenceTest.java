package dev.itzgamesgd.moreminecrafts.colosseum;

import static dev.itzgamesgd.moreminecrafts.colosseum.Sim.*;
import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.colosseum.ArenaAttempt.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoundComposerAndPersistenceTest {
    @Test void rosterHasExactlyFifteenActiveMobs() { assertEquals(15, MobRoster.SCORES.size()); }

    @Test void everyRoundNAndSeedFitsTheApprovedBandAndConstraints() {
        for (int r0 = 1; r0 <= 6; r0++) for (int n = 1; n <= 4; n++) for (long seed = 0; seed < 150; seed++) {
            final int round = r0;
            int budget = RoundPlan.scaledBudget(round, n);
            List<String> c = RoundComposer.compose(seed, round, budget);
            int total = c.stream().mapToInt(MobRoster::score).sum();
            int lo = (int) Math.ceil(0.85 * budget), ex = (int) Math.floor(1.05 * budget);
            String ctx = "round " + round + " n " + n + " seed " + seed + " " + c;
            assertTrue(total >= lo && total <= ex, ctx + " total " + total);
            assertTrue(c.stream().allMatch(m -> MobRoster.score(m) <= RoundPlan.scoreCap(round)), ctx);
            if (round == 3) {
                assertEquals(1, c.stream().filter(m -> MobRoster.score(m) >= 7).count(), ctx + " exactly one focal elite");
                assertTrue(RoundComposer.rolesCovered(c), ctx);
            }
            if (round >= 5) assertTrue(c.stream().anyMatch(m -> MobRoster.score(m) >= 7), ctx + " elite round");
        }
    }

    @Test void compositionIsDeterministic() { assertEquals(RoundComposer.compose(42, 4, 51), RoundComposer.compose(42, 4, 51)); }

    private static Sim midRun() {
        Sim s = new Sim();
        s.inside.addAll(List.of(A, B));
        s.tick();
        s.ticks(30);
        s.inside.add(C);
        s.ticks(3);
        return s;
    }

    @Test void persistenceRoundTripPreservesEverythingNeededToResume() {
        Sim s = midRun();
        String json = s.c.toJson();
        ArenaController r = ArenaController.restore("arena-1", json, ArenaController.checksum(json), s.now + 1000);
        assertFalse(r.quarantined(), r.quarantineReason());
        assertEquals(json, r.toJson(), "state survives a save/load cycle byte for byte");
        ArenaAttempt a = r.attempt(), o = s.c.attempt();
        assertEquals(o.attemptId, a.attemptId);
        assertEquals(o.rosterIds(), a.rosterIds());
        assertEquals(o.pendingIds(), a.pendingIds());
        assertEquals(o.snapshot(), a.snapshot());
        assertEquals(o.peak(), a.peak());
        assertEquals(o.allocations().size(), a.allocations().size());
    }

    @Test void structureMaskPersistenceAndProtectionSurvivesRoundTrip() {
        ArenaController c = new ArenaController("mask-test");
        net.minecraft.core.BlockPos templatePos = new net.minecraft.core.BlockPos(10, 64, 20);
        net.minecraft.core.BlockPos playerPos = new net.minecraft.core.BlockPos(12, 65, 22);
        net.minecraft.core.BlockPos unplaced = new net.minecraft.core.BlockPos(30, 70, 30);

        c.recordStructureMask(List.of(templatePos));
        assertTrue(c.structureMask().complete());
        assertTrue(c.structureMask().protects(templatePos));
        assertFalse(c.structureMask().playerPlaced(templatePos));

        c.protectBlock(playerPos);
        assertTrue(c.structureMask().protects(playerPos));
        assertTrue(c.structureMask().playerPlaced(playerPos));
        assertFalse(c.structureMask().protects(unplaced));

        String json = c.toJson();
        ArenaController restored = ArenaController.restore("mask-test", json, ArenaController.checksum(json), System.currentTimeMillis());
        assertFalse(restored.quarantined(), restored.quarantineReason());
        assertTrue(restored.structureMask().complete());
        assertTrue(restored.structureMask().protects(templatePos));
        assertFalse(restored.structureMask().playerPlaced(templatePos));
        assertTrue(restored.structureMask().protects(playerPos));
        assertTrue(restored.structureMask().playerPlaced(playerPos));
        assertFalse(restored.structureMask().protects(unplaced));

        // Player block released
        restored.releaseBlock(playerPos);
        assertFalse(restored.structureMask().protects(playerPos));
        // Template block never released
        assertFalse(restored.releaseBlock(templatePos));
        assertTrue(restored.structureMask().protects(templatePos));
    }

    @Test void restoredControllerContinuesWithoutDuplicatingRoundStartOrSpawns() {
        Sim s = midRun();
        int spawned = s.c.attempt().count(AllocStatus.SPAWNED);
        String json = s.c.toJson();
        Sim t = new Sim();
        t.now = s.now;
        t.c = ArenaController.restore("arena-1", json, ArenaController.checksum(json), t.now + 1);
        t.inside.addAll(List.of(A, B, C));
        t.ticks(10);
        assertEquals(0, t.count(RoundStart.class), "restart never starts the round again");
        assertEquals(0, t.count(WarnSpawn.class) , "no new warnings for already warned/spawned allocations beyond the 12 cap logic");
        assertEquals(spawned, t.c.attempt().count(AllocStatus.SPAWNED));
        assertTrue(t.c.attempt().pendingIds().contains(C), "pending joiners survive restart");
        assertFalse(t.c.attempt().member(C) != null, "a pending joiner is still not on the roster after restart");
    }

    @Test void graceSurvivesRestartOnTheWallClock() {
        Sim s = new Sim();
        s.inside.add(A);
        s.ticks(5);
        s.inside.clear();
        s.tick();
        long expiry = s.c.attempt().graceExpiryMillis();
        String json = s.c.toJson();
        ArenaController r = ArenaController.restore("arena-1", json, ArenaController.checksum(json), s.now + 60_000);
        assertEquals(expiry, r.attempt().graceExpiryMillis(), "an already running grace is not reset");
        r.tick(expiry + 1, java.util.Set.of(), true, al -> null);
        assertEquals(Outcome.ABSENCE_EXPIRED, r.attempt().outcome());
    }

    @Test void checksumMismatchUnreadableAndInconsistentStatesAreQuarantined() {
        Sim s = midRun();
        String json = s.c.toJson();
        assertTrue(ArenaController.restore("arena-1", json, "deadbeef", s.now).quarantined());
        assertTrue(ArenaController.restore("arena-1", "{not json", null, s.now).quarantined());
        assertTrue(ArenaController.restore("other", json, null, s.now).quarantined(), "arena id mismatch");
        String tampered = json.replace("\"peak\":2", "\"peak\":1");
        assertNotEquals(json, tampered);
        ArenaController q = ArenaController.restore("arena-1", tampered, null, s.now);
        assertTrue(q.quarantined(), "snapshot/peak contradiction");
        String badPlan = json.replaceFirst("\"mob\":\"[a-z_:#]+\"", "\"mob\":\"minecraft:zombie\"");
        if (!badPlan.equals(json)) {
            ArenaController q2 = ArenaController.restore("arena-1", badPlan, null, s.now);
            assertTrue(q2.quarantined() || q2.toJson().equals(json), "a plan that no longer matches the deterministic composition is isolated");
        }
    }

    @Test void clockRegressionOnRestoreQuarantinesAndNothingIsRewarded() {
        Sim s = midRun();
        String json = s.c.toJson();
        ArenaController r = ArenaController.restore("arena-1", json, ArenaController.checksum(json), s.now - 10_000);
        assertTrue(r.quarantined());
        assertTrue(r.tick(s.now, java.util.Set.of(A), true, al -> new int[] {0, 0, 0}).isEmpty());
        assertEquals(0, r.attempt().keysAwarded().size());
        r.release();
        assertFalse(r.quarantined());
    }

    @Test void entitlementsAndCleanupRecordsAreKeptAcrossAttempts() {
        Sim s = new Sim();
        s.inside.add(A);
        s.tick();
        s.clearRound();
        assertFalse(s.c.entitlements().isEmpty());
        s.c.onPlayerDeath(A);
        s.inside.remove(A);
        s.tick();
        String json = s.c.toJson();
        ArenaController r = ArenaController.restore("arena-1", json, ArenaController.checksum(json), s.now);
        assertEquals(s.c.entitlements(), r.entitlements());
        assertTrue(r.cleanupAttempts().contains(s.c.attempt().attemptId));
        assertEquals(RoundPlan.COOLDOWN_TICKS, r.cooldownLeft());
    }

    @Test void geometryInteriorExcludesWallDoorwaysCorridorsAndCaves() {
        ArenaGeometry g = new ArenaGeometry(1000, -2000, 40);
        assertTrue(g.contains(1000, 40, -2000));
        assertTrue(g.contains(1000 + 29, 40, -2000));
        assertFalse(g.contains(1000 + 30, 40, -2000), "wall ring / entrance arch cells are outside the interior");
        assertFalse(g.contains(1000 + 34, 40, -2000));
        assertFalse(g.contains(1000 + 40, 40, -2000), "corridor");
        assertFalse(g.contains(1000, 39, -2000), "below the combat floor");
        assertFalse(g.contains(1000, 40 + 23, -2000), "above the dome");
        assertFalse(g.contains(1000 + 25, 40, -2000 + 25), "diagonal beyond radius 29");
        assertTrue(g.contains(1000 + 20, 41, -2000 + 20));
    }
}
