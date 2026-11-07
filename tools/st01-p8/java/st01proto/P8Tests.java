package st01proto;

import java.nio.file.*;
import java.util.*;
import st01proto.EntranceCoordinator.State;
import st01proto.ParticipationTracker.*;

/** Self-contained prototype tests (no JUnit, no Minecraft). All timer values here are TEST-ONLY. Usage: java st01proto.P8Tests <conformance.txt> */
public final class P8Tests {
    static int fails = 0, passes = 0;
    static void check(String name, boolean ok, String msg) { if (ok) { passes++; System.out.println("PASS " + name); } else { fails++; System.out.println("FAIL " + name + ": " + msg); } }
    static final Config T = Config.testOnly(6);                    // TEST-ONLY
    static final InstanceId A = new InstanceId("overworld", 1001L), B = new InstanceId("overworld", 2002L);
    static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-000000000001"), P2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static DummyBoss boss(FakeWorld w, InstanceId id, EntranceCoordinator[] holder, Policy pol) {
        DummyBoss b = new DummyBoss(id, w, w, pol, Params.testOnly(), 200);
        b.setListener(r -> holder[0].onDefeatSignal()); return b;
    }
    static EntranceCoordinator coord(FakeWorld w, InstanceId id) { return new EntranceCoordinator(id, T, w, w, w); }
    static void ticks(EntranceCoordinator c, FakeWorld w, int n) { for (int i = 0; i < n; i++) { c.tick(); c.pumpSpawn(); c.pollArrival(); } }
    static EntranceCoordinator restart(EntranceCoordinator c, FakeWorld w) { String s = c.snapshot(); w.restart(); return EntranceCoordinator.restore(s, T, w, w, w); }

    public static void main(String[] a) throws Exception {
        // T0 timer has no production default
        boolean threw = false; try { new EntranceCoordinator(A, Config.productionDefaults(), new FakeWorld(), new FakeWorld(), new FakeWorld()); } catch (IllegalStateException e) { threw = true; }
        check("T0 no production default for the fallback timer (TEST-ONLY values never become defaults)", threw, "coordinator ran without an explicit timer");

        // T1 nothing opens before the boss is defeated
        { FakeWorld w = new FakeWorld(); EntranceCoordinator c = coord(w, A);
          c.onChestInteraction(); c.requestOpen(); ticks(c, w, 50); c.onDefeatSignal();
          check("T1 entrance cannot open before a valid defeat (timer, requests, spoofed signals)", c.state() == State.SEALED && !w.isCoverOpen(A) && w.aliveCount(A) == 0 && w.spawnCount.getOrDefault(A, 0) == 0, c.state() + " cover=" + w.isCoverOpen(A)); }

        // T2/T3 invalid kills
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByOther(200, 10); b.die(DeathCause.ENVIRONMENT, 10); ticks(h[0], w, 40);
          check("T2 environment-only death is not a victory: no entrance progress, chest sealed, no reward, retry allowed", h[0].state() == State.SEALED && w.isChestSealed(A) && w.rewards.getOrDefault(A, 0) == 0 && b.retryAllowed && w.hasDied(A) && !w.isVictory(A), h[0].state() + ""); }
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 150, 5); b.die(DeathCause.COMMAND_KILL, 6); ticks(h[0], w, 40);
          check("T3 administrative kill after participation is not a victory", h[0].state() == State.SEALED && w.rewards.getOrDefault(A, 0) == 0, h[0].state() + ""); }
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 20, 5); b.hitByOther(180, 6); b.die(DeathCause.ENVIRONMENT, 400); ticks(h[0], w, 40);
          check("T3b a token player hit followed by an environmental death (share 10%, window long past) is not a victory (COMBINED)", h[0].state() == State.SEALED && w.rewards.getOrDefault(A, 0) == 0, h[0].state() + ""); }

        // T4 valid kill -> timer starts even if golem spawn fails -> fallback
        { FakeWorld w = new FakeWorld(); w.nextSpawnOk = false; EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 200, 5); b.die(DeathCause.PLAYER, 6);
          check("T4a valid victory starts the timer immediately at the defeat", h[0].state() == State.GUARDIAN_DEFEATED && h[0].remaining() == T.timerTicks(), h[0].state() + " " + h[0].remaining());
          ticks(h[0], w, 10);
          check("T4b golem spawn failure never stops the timer: FALLBACK_OPEN, cover applied once, reward granted once", h[0].state() == State.FALLBACK_OPEN && w.isCoverOpen(A) && w.openMutations.get(A) == 1 && w.rewards.get(A) == 1, h[0].state() + " " + w.rewards); }

        // T5 normal + duplicates idempotent, one golem
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 120, 1); b.hitByPlayer(P2, 80, 2); b.die(DeathCause.PLAYER, 3);
          for (int i = 0; i < 3; i++) { h[0].onDefeatSignal(); h[0].reconcile(); h[0].pumpSpawn(); }
          check("T5a duplicate defeat signals create exactly one golem and one guiding state", h[0].state() == State.GOLEM_GUIDING && w.spawnCount.get(A) == 1, h[0].state() + " spawns=" + w.spawnCount);
          ticks(h[0], w, 2); w.golem(A).atStop = true; for (int i = 0; i < 3; i++) { h[0].pollArrival(); h[0].requestOpen(); }
          check("T5b arrival opens (ENTRANCE_OPEN); duplicate arrival/open requests mutate the world exactly once; reward once", h[0].state() == State.ENTRANCE_OPEN && w.openMutations.get(A) == 1 && w.rewards.get(A) == 1, h[0].state() + " " + w.openMutations);
          ticks(h[0], w, 30);
          check("T5c OPEN is terminal: no timer effect, no auto-restore", h[0].state() == State.ENTRANCE_OPEN && w.isCoverOpen(A), h[0].state() + ""); }

        // T6 stalled / lost golem
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 200, 1); b.die(DeathCause.PLAYER, 2); h[0].pumpSpawn(); w.golem(A).stalled = true; w.golem(A).atStop = true; ticks(h[0], w, 10);
          check("T6a stalled golem (path failure) never opens by arrival; fallback opens", h[0].state() == State.FALLBACK_OPEN, h[0].state() + ""); }
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 200, 1); b.die(DeathCause.PLAYER, 2); h[0].pumpSpawn(); w.golem(A).alive = false; ticks(h[0], w, 10);
          check("T6b golem lost/killed while guiding: fallback opens", h[0].state() == State.FALLBACK_OPEN, h[0].state() + ""); }

        // T7 restart
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 200, 1); b.die(DeathCause.PLAYER, 2); h[0].pumpSpawn(); ticks(h[0], w, 2); long before = h[0].remaining();
          EntranceCoordinator c2 = restart(h[0], w);
          check("T7a restart while guiding keeps state and remaining time; no golem is assumed", c2.state() == State.GOLEM_GUIDING && c2.remaining() == before && w.aliveCount(A) == 0, c2.state() + " " + c2.remaining());
          ticks(c2, w, 10);
          check("T7b after restart the fallback still opens it", c2.state() == State.FALLBACK_OPEN && w.isCoverOpen(A), c2.state() + "");
          EntranceCoordinator c3 = restart(c2, w); ticks(c3, w, 5);
          check("T7c OPEN survives another restart, cover stays open, no second world mutation", c3.state() == State.FALLBACK_OPEN && w.openMutations.get(A) == 1, c3.state() + " " + w.openMutations); }
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 200, 1); b.die(DeathCause.PLAYER, 2); h[0].pumpSpawn(); w.golem(A).atStop = true; h[0].pollArrival();
          w.coverOpen.clear();     // simulate a lost/regenerated chunk whose blocks revert while the record says OPEN
          EntranceCoordinator c2 = restart(h[0], w);
          check("T7d OPEN record with reverted cover blocks (lost/regenerated chunk): the OPEN record is authoritative and the cover is re-opened idempotently (never closed)", c2.state() == State.ENTRANCE_OPEN && w.isCoverOpen(A), c2.state() + " cover=" + w.isCoverOpen(A)); }

        // T14 optional respawn after restart (O-7 candidate; OFF by default)
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 200, 1); b.die(DeathCause.PLAYER, 2); h[0].pumpSpawn(); String s = h[0].snapshot(); w.restart();
          EntranceCoordinator c2 = EntranceCoordinator.restore(s, T.withRespawn(true), w, w, w); c2.pumpSpawn();
          check("T14 respawn-after-restart option: exactly one new golem, state GUIDING, remaining time unchanged", c2.state() == State.GOLEM_GUIDING && w.aliveCount(A) == 1 && w.spawnCount.get(A) == 2 && c2.remaining() == T.timerTicks(), c2.state() + " spawns=" + w.spawnCount); }
        // T15 saved-data mismatch: OPEN record without a persisted victory
        { FakeWorld w = new FakeWorld(); EntranceCoordinator c = EntranceCoordinator.restore("overworld|1001|ENTRANCE_OPEN|3|1", T, w, w, w);
          check("T15 OPEN record but no persisted victory (inconsistent save): cover is NOT opened, anomaly is flagged for reporting", c.anomaly() && !w.isCoverOpen(A) && !w.isCoverOpen(new InstanceId("overworld", 1001L)), "anomaly=" + c.anomaly()); }

        // T8 chunk unloaded at the deadline
        { FakeWorld w = new FakeWorld(); EntranceCoordinator[] h = {coord(w, A)}; DummyBoss b = boss(w, A, h, Policy.COMBINED);
          b.hitByPlayer(P1, 200, 1); b.die(DeathCause.PLAYER, 2); h[0].pumpSpawn(); w.chunkLoaded.put(A, false); ticks(h[0], w, 10);
          boolean pending = h[0].state() == State.FALLBACK_OPEN && !w.isCoverOpen(A);
          w.chunkLoaded.put(A, true); h[0].reconcile();
          check("T8 unloaded cover chunk: record opens, blocks change only when the chunk loads, exactly once", pending && w.isCoverOpen(A) && w.openMutations.get(A) == 1, "pending=" + pending); }

        // T9 lost signal
        { FakeWorld w = new FakeWorld(); EntranceCoordinator c = coord(w, A); w.markDeath(A); w.markVictory(A);   // the defeat signal never reaches the coordinator
          ticks(c, w, 1);
          check("T9 lost defeat signal: the persisted victory record alone creates the entrance record and the timer", c.state() == State.GOLEM_GUIDING || c.state() == State.GUARDIAN_DEFEATED, c.state() + ""); }

        // T10 two instances at once
        { FakeWorld w = new FakeWorld(); InstanceRegistry reg = new InstanceRegistry(T, w, w, w);
          EntranceCoordinator[] ha = {reg.of(A)}, hb = {reg.of(B)}; DummyBoss ba = boss(w, A, ha, Policy.COMBINED), bb = boss(w, B, hb, Policy.COMBINED);
          ba.setListener(reg::onDefeatReport); bb.setListener(reg::onDefeatReport);
          ba.hitByPlayer(P1, 200, 1); bb.hitByPlayer(P2, 200, 1); ba.die(DeathCause.PLAYER, 2); bb.die(DeathCause.PLAYER, 2);
          reg.tickAll(); w.golem(A).atStop = true; reg.tickAll();
          check("T10a two simultaneous defeats: A opens by arrival while B keeps guiding (no cross-talk)", reg.of(A).state() == State.ENTRANCE_OPEN && reg.of(B).state() == State.GOLEM_GUIDING && !w.isCoverOpen(B), reg.of(A).state() + "/" + reg.of(B).state());
          for (int i = 0; i < 10; i++) reg.tickAll();
          check("T10b B opens by fallback on its own timer; one golem per instance; one reward per instance", reg.of(B).state() == State.FALLBACK_OPEN && w.spawnCount.get(A) == 1 && w.spawnCount.get(B) == 1 && w.rewards.get(A) == 1 && w.rewards.get(B) == 1, reg.of(B).state() + "");
          FakeWorld w2 = new FakeWorld(); InstanceRegistry r2 = new InstanceRegistry(T, w2, w2, w2); r2.of(A); r2.of(B); DummyBoss only = new DummyBoss(A, w2, w2, Policy.COMBINED, Params.testOnly(), 200); only.setListener(r2::onDefeatReport); only.hitByPlayer(P1, 200, 1); only.die(DeathCause.PLAYER, 2);
          for (int i = 0; i < 20; i++) r2.tickAll();
          check("T10c a defeat in instance A never moves instance B", r2.of(B).state() == State.SEALED && !w2.isCoverOpen(B) && w2.aliveCount(B) == 0, r2.of(B).state() + ""); }

        // T11 selectors
        selectorTests();

        // T12 validity matrix
        matrix();

        // T13 conformance against the P7 reference model
        if (a.length > 0) conformance(a[0]);
        System.out.println("SUMMARY " + passes + " passed, " + fails + " failed");
        System.exit(fails == 0 ? 0 : 1);
    }

    static void selectorTests() {
        List<GuideTargetSelector> sels = List.of(new GuideTargetSelector.Nearest(), new GuideTargetSelector.StickyNearest(3), new GuideTargetSelector.Farthest());
        String[] names = {"Nearest", "StickyNearest", "Farthest"};
        for (int i = 0; i < sels.size(); i++) {
            GuideTargetSelector s = sels.get(i); int switches = 0; UUID last = null; boolean offlineChosen = false; boolean emptyOk = true;
            for (int t = 0; t < 60; t++) {           // two players dispersing around the golem; P1 jitters
                double d1 = 5 + (t % 2 == 0 ? 0.6 : -0.6), d2 = 5 + (t > 30 ? 0.0 : 0.0);
                boolean p2on = t < 40 || t > 50;
                List<GuideTargetSelector.P> ps = List.of(new GuideTargetSelector.P(P1, d1, 0, true), new GuideTargetSelector.P(P2, 0, d2, p2on));
                Optional<UUID> c = s.choose(0, 0, ps);
                if (c.isPresent() && c.get().equals(P2) && !p2on) offlineChosen = true;
                if (c.isPresent() && last != null && !c.get().equals(last)) switches++;
                if (c.isPresent()) last = c.get();
            }
            Optional<UUID> none = s.choose(0, 0, List.of(new GuideTargetSelector.P(P1, 1, 1, false), new GuideTargetSelector.P(P2, 2, 2, false)));
            emptyOk = none.isEmpty();
            System.out.println("SELECTOR " + names[i] + " switches=" + switches + " offlineChosen=" + offlineChosen + " allOfflineEmpty=" + emptyOk);
            check("T11 selector " + names[i] + ": never follows an offline player; empty when everyone is offline", !offlineChosen && emptyOk, "offline chosen or non-empty");
        }
    }

    static void matrix() {
        record Case(String name, boolean playerHit, double share, DeathCause cause, long gap) {}
        Case[] cases = {
            new Case("player kills (full damage)", true, 1.0, DeathCause.PLAYER, 0), new Case("player 60% then lava kills within window", true, 0.6, DeathCause.ENVIRONMENT, 50),
            new Case("player 60% then lava kills long after", true, 0.6, DeathCause.ENVIRONMENT, 500), new Case("player 10% then lava kills quickly", true, 0.1, DeathCause.ENVIRONMENT, 20),
            new Case("no player damage, environment", false, 0, DeathCause.ENVIRONMENT, 0), new Case("no player damage, other mob", false, 0, DeathCause.MOB, 0),
            new Case("player 100% then /kill", true, 1.0, DeathCause.COMMAND_KILL, 0), new Case("player 10% then void", true, 0.1, DeathCause.VOID, 30)};
        for (Policy pol : Policy.values()) {
            StringBuilder sb = new StringBuilder("MATRIX " + pol + ":");
            for (Case c : cases) {
                ParticipationTracker t = new ParticipationTracker(200, pol, Params.testOnly());
                if (c.playerHit) t.onPlayerDamage(P1, c.share * 200, 100);
                if (c.share < 1.0) t.onOtherDamage(200 * (1 - c.share), 100 + c.gap);
                sb.append(" [").append(c.name).append(" => ").append(t.judge(c.cause, 100 + c.gap).valid() ? "VALID" : "invalid").append("]");
            }
            System.out.println(sb);
        }
        ParticipationTracker t = new ParticipationTracker(200, Policy.COMBINED, Params.testOnly()); t.onPlayerDamage(P1, 200, 1);
        check("T12 matrix: COMBINED accepts a full player kill and rejects /kill", t.judge(DeathCause.PLAYER, 2).valid() && !t.judge(DeathCause.COMMAND_KILL, 2).valid(), "matrix");
    }

    static void conformance(String path) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(path)); int seqs = 0, steps = 0; String bad = null;
        FakeWorld w = null; EntranceCoordinator c = null; Config cc = Config.testOnly(2); boolean online = true;
        for (String line : lines) {
            if (line.equals("S")) { w = new FakeWorld(); c = new EntranceCoordinator(A, cc, w, w, w); seqs++; continue; }
            String[] f = line.split(" "); String ev = f[1];
            switch (ev) {
                case "BOSS_KILL" -> { w.markDeath(A); w.markVictory(A); }
                case "DEFEATED_EVENT", "FAKE_DEFEATED_EVENT" -> c.onDefeatSignal();
                case "SPAWN_OK" -> { w.nextSpawnOk = true; c.pumpSpawn(); }
                case "SPAWN_FAIL" -> { w.nextSpawnOk = false; c.pumpSpawn(); }
                case "GOLEM_PATH_FAIL" -> { FakeWorld.Golem g = w.golem(A); if (g != null && g.alive) g.stalled = true; }
                case "GOLEM_LOST" -> { FakeWorld.Golem g = w.golem(A); if (g != null) g.alive = false; }
                case "GOLEM_ARRIVE" -> { FakeWorld.Golem g = w.golem(A); if (g != null && g.alive && !g.stalled) g.atStop = true; c.pollArrival(); }
                case "TICK" -> c.tick();
                case "RESTART" -> { String s = c.snapshot(); w.restart(); c = EntranceCoordinator.restore(s, cc, w, w, w); }
                case "CHUNK_UNLOAD" -> w.chunkLoaded.put(A, false);
                case "CHUNK_LOAD" -> w.chunkLoaded.put(A, true);
                case "OPEN_REQUEST_DUP" -> c.requestOpen();
                case "CHEST_INTERACT_EARLY" -> c.onChestInteraction();
                default -> { /* PLAYER_* events: no effect on instance progress */ }
            }
            c.reconcile(); steps++;
            String got = c.state() + " " + (w.isCoverOpen(A) ? 1 : 0) + " " + w.aliveCount(A) + " " + (w.isVictory(A) ? 1 : 0) + " " + c.remaining() + " " + (c.spawnPending() ? 1 : 0);
            String exp = f[2] + " " + f[3] + " " + f[4] + " " + f[5] + " " + f[6] + " " + f[7];
            if (!got.equals(exp) && bad == null) bad = "seq " + seqs + " step " + steps + " event " + ev + ": expected [" + exp + "] got [" + got + "]";
        }
        check("T13 differential conformance: " + seqs + " random event sequences / " + steps + " steps replayed against the P7 reference model give identical (state, cover, golems, victory, remaining, spawn-pending)", bad == null, bad);
    }
}
