package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure unit tests of the entrance state machine (ported from ST-01 P7-P11, no Minecraft). All fallback times here are TEST-ONLY. */
class EntranceCoreTest {
    static final EntranceConfig CFG = EntranceConfig.testOnly(6, 1.0D);

    /** World stand-in: persistent victory/cover, volatile golem/chunks. */
    static final class FakePorts implements EntranceCore.Ports {
        boolean victory, chunk = true, spawnArea = true, cover, nextSpawnOk = true;
        UUID golem; boolean golemAlive, stalled, atStop; int spawned, openMutations;
        public boolean victory() { return victory; }
        public boolean spawnAreaLoaded() { return spawnArea; }
        public boolean coverChunkLoaded() { return chunk; }
        public boolean coverOpen() { return cover; }
        public void openCover() { if (!cover) openMutations++; cover = true; }
        public int golemCount() { return golem != null && golemAlive ? 1 : 0; }
        public boolean golemAlive(UUID g) { return golem != null && golemAlive && golem.equals(g); }
        public UUID spawnGolem() { if (!nextSpawnOk) return null; golem = UUID.randomUUID(); golemAlive = true; stalled = false; atStop = false; spawned++; return golem; }
        public boolean golemAtStop(UUID g) { return golemAlive(g) && !stalled && atStop; }
        void restart() { golemAlive = false; chunk = true; }
    }

    static EntranceCore core(FakePorts p) { return new EntranceCore(CFG, p, new EntranceCore.Saved(EntranceState.SEALED, 0, false)); }
    static void steps(EntranceCore c, int n) { for (int i = 0; i < n; i++) c.step(); }

    @Test void productionConfigIsInstalledByDefaultAndRejectsInvalidParameters() {
        EntranceConfig.resetToProduction();
        EntranceConfig prod = EntranceConfig.current();
        assertNotNull(prod);
        assertSame(EntranceConfig.PRODUCTION, prod);
        assertEquals(600L, prod.fallbackTicks());
        assertEquals(1.5D, prod.arrivalRadius(), 1.0E-9D);
        assertEquals(1, prod.maxSpawnAttempts());
        assertFalse(prod.respawnAfterRestart());
        assertFalse(prod.testOnly());

        EntranceConfig testCfg = EntranceConfig.testOnly(120L, 1.25D);
        assertTrue(testCfg.testOnly());
        EntranceConfig.install(testCfg);
        assertSame(testCfg, EntranceConfig.current());
        EntranceConfig.resetToProduction();
        assertSame(EntranceConfig.PRODUCTION, EntranceConfig.current());

        assertThrows(IllegalStateException.class, () -> new EntranceCore(new EntranceConfig(-1, 1.0, 1, false, false), new FakePorts(), new EntranceCore.Saved(EntranceState.SEALED, 0, false)));
        assertThrows(IllegalStateException.class, () -> new EntranceCore(new EntranceConfig(600L, 0.0, 1, false, false), new FakePorts(), new EntranceCore.Saved(EntranceState.SEALED, 0, false)));
    }

    @Test void productionConfigRunsFull600TickFallbackAndImmediateVictoryStart() {
        FakePorts p = new FakePorts();
        p.nextSpawnOk = false;
        EntranceCore c = new EntranceCore(EntranceConfig.PRODUCTION, p, new EntranceCore.Saved(EntranceState.SEALED, 0, false));
        p.victory = true;
        c.step(); // tick 1: reconcile sets remaining = 600
        assertEquals(EntranceState.GUARDIAN_DEFEATED, c.state());
        assertEquals(600L, c.remaining(), "O-3: timer starts immediately at 600 ticks on valid victory");
        steps(c, 599);
        assertEquals(EntranceState.GUARDIAN_DEFEATED, c.state(), "still closed at 599 elapsed ticks after timer start");
        assertEquals(1L, c.remaining());
        assertFalse(p.cover);
        c.step(); // 600th countdown tick reaches 0 -> FALLBACK_OPEN
        assertEquals(EntranceState.FALLBACK_OPEN, c.state(), "O-1: opens at 600th countdown tick");
        assertTrue(p.cover);
        assertEquals(1, p.openMutations);
    }

    @Test void approvedGolemTuningIsUnchanged() {
        assertEquals(0.5D, GoldGuideGolemTuning.SCALE, 1.0E-9D);
        assertEquals(0.25D, GoldGuideGolemTuning.MOVEMENT_SPEED, 1.0E-9D);
        assertEquals(0.7D, GoldGuideGolemTuning.BASE_WIDTH * GoldGuideGolemTuning.SCALE, 1.0E-6D);
        assertEquals(1.35D, GoldGuideGolemTuning.BASE_HEIGHT * GoldGuideGolemTuning.SCALE, 1.0E-6D);
    }

    @Test void nothingOpensBeforeAValidVictory() {
        FakePorts p = new FakePorts(); EntranceCore c = core(p);
        steps(c, 100); c.reconcile(); c.pollArrival();
        assertEquals(EntranceState.SEALED, c.state()); assertFalse(p.cover); assertEquals(0, p.spawned);
    }

    @Test void victoryStartsTheTimerEvenWhenTheGolemCannotBeCreated() {
        FakePorts p = new FakePorts(); p.nextSpawnOk = false; EntranceCore c = core(p);
        p.victory = true; c.step();
        assertEquals(EntranceState.GUARDIAN_DEFEATED, c.state());
        assertTrue(c.remaining() < CFG.fallbackTicks() + 1 && c.remaining() >= CFG.fallbackTicks() - 1, "timer started at the victory");
        steps(c, 10);
        assertEquals(EntranceState.FALLBACK_OPEN, c.state()); assertTrue(p.cover); assertEquals(1, p.openMutations);
    }

    @Test void arrivalOpensOnceAndIsIdempotent() {
        FakePorts p = new FakePorts(); EntranceCore c = core(p); p.victory = true;
        for (int i = 0; i < 3; i++) { c.reconcile(); c.pumpSpawn(); }
        assertEquals(EntranceState.GOLEM_GUIDING, c.state()); assertEquals(1, p.spawned, "one golem");
        p.atStop = true; for (int i = 0; i < 3; i++) { c.pollArrival(); c.reconcile(); }
        assertEquals(EntranceState.ENTRANCE_OPEN, c.state()); assertEquals(1, p.openMutations);
        steps(c, 30); assertEquals(EntranceState.ENTRANCE_OPEN, c.state(), "terminal, no restore");
    }

    @Test void stalledOrLostGolemFallsBackToTheTimer() {
        FakePorts p = new FakePorts(); EntranceCore c = core(p); p.victory = true; c.step(); p.stalled = true; p.atStop = true; steps(c, 10);
        assertEquals(EntranceState.FALLBACK_OPEN, c.state());
        FakePorts q = new FakePorts(); EntranceCore d = core(q); q.victory = true; d.step(); q.golemAlive = false; steps(d, 10);
        assertEquals(EntranceState.FALLBACK_OPEN, d.state());
    }

    @Test void restartKeepsStateAndRemainingTimeAndOpenSurvives() {
        FakePorts p = new FakePorts(); EntranceCore c = core(p); p.victory = true; c.step(); steps(c, 2);
        long before = c.remaining(); EntranceCore.Saved saved = c.saved(); p.restart();
        EntranceCore r = new EntranceCore(CFG, p, saved);
        assertEquals(before, r.remaining()); assertEquals(EntranceState.GOLEM_GUIDING, r.state()); assertEquals(0, p.golemCount());
        steps(r, 10); assertEquals(EntranceState.FALLBACK_OPEN, r.state());
        EntranceCore.Saved open = r.saved(); p.restart(); EntranceCore r2 = new EntranceCore(CFG, p, open); steps(r2, 5);
        assertEquals(EntranceState.FALLBACK_OPEN, r2.state()); assertEquals(1, p.openMutations, "no second world change after another restart");
    }

    @Test void unloadedCoverChunkIsAppliedOnceOnReload() {
        FakePorts p = new FakePorts(); EntranceCore c = core(p); p.victory = true; c.step(); p.chunk = false; steps(c, 10);
        assertEquals(EntranceState.FALLBACK_OPEN, c.state()); assertFalse(p.cover);
        p.chunk = true; c.reconcile(); assertTrue(p.cover); assertEquals(1, p.openMutations);
    }

    @Test void revertedBlocksAreRepairedAndAnOpenRecordWithoutVictoryNeverOpens() {
        FakePorts p = new FakePorts(); EntranceCore c = core(p); p.victory = true; c.step(); p.atStop = true; c.pollArrival();
        p.cover = false; c.reconcile(); assertTrue(p.cover, "the OPEN record is authoritative");
        FakePorts q = new FakePorts(); EntranceCore bad = new EntranceCore(CFG, q, new EntranceCore.Saved(EntranceState.ENTRANCE_OPEN, 3, true));
        bad.reconcile(); assertTrue(bad.anomaly()); assertFalse(q.cover, "inconsistent save: not opened");
    }

    @Test void spawnIsDeferredWhileTheAreaIsUnloadedAndNeverDuplicates() {
        FakePorts p = new FakePorts(); p.spawnArea = false; EntranceCore c = core(p); p.victory = true; c.reconcile(); c.pumpSpawn();
        assertEquals(0, p.spawned); assertTrue(c.spawnPending());
        p.spawnArea = true; c.pumpSpawn(); c.pumpSpawn(); assertEquals(1, p.spawned);
    }

    @Test void aGolemThatAppearsLateIsNeverDuplicatedEvenWhileASpawnIsPending() {
        FakePorts p = new FakePorts(); p.victory = true;
        EntranceConfig respawn = EntranceConfig.testOnly(6, 1.0D).withRespawnAfterRestart(true);
        EntranceCore c = new EntranceCore(respawn, p, new EntranceCore.Saved(EntranceState.GOLEM_GUIDING, 4, false));
        assertTrue(c.spawnPending(), "no golem was alive when the record was restored");
        p.golem = UUID.randomUUID(); p.golemAlive = true;          // e.g. an entity that finishes loading just before the spawn step
        c.pumpSpawn(); c.pumpSpawn();
        assertEquals(0, p.spawned, "the existing golem is never joined by a second one"); assertFalse(c.spawnPending());
    }

    /** Differential test: random event sequences generated from the P7 Python reference model must be reproduced step for step. */
    @Test void followsTheP7ReferenceModelOnRecordedSequences() throws Exception {
        EntranceConfig cfg = EntranceConfig.testOnly(2, 1.0D);
        int seqs = 0, steps = 0; FakePorts p = null; EntranceCore c = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/st01/p7_reference_sequences.txt")))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.equals("S")) { p = new FakePorts(); c = new EntranceCore(cfg, p, new EntranceCore.Saved(EntranceState.SEALED, 0, false)); seqs++; continue; }
                String[] f = line.split(" "); String ev = f[1];
                switch (ev) {
                    case "BOSS_KILL" -> p.victory = true;
                    case "DEFEATED_EVENT", "FAKE_DEFEATED_EVENT", "OPEN_REQUEST_DUP" -> c.reconcile();
                    case "SPAWN_OK" -> { p.nextSpawnOk = true; c.pumpSpawn(); }
                    case "SPAWN_FAIL" -> { p.nextSpawnOk = false; c.pumpSpawn(); }
                    case "GOLEM_PATH_FAIL" -> { if (p.golemAlive) p.stalled = true; }
                    case "GOLEM_LOST" -> p.golemAlive = false;
                    case "GOLEM_ARRIVE" -> { if (p.golemAlive && !p.stalled) p.atStop = true; c.pollArrival(); }
                    case "TICK" -> c.tick();
                    case "RESTART" -> { EntranceCore.Saved s = c.saved(); p.restart(); c = new EntranceCore(cfg, p, s); }
                    case "CHUNK_UNLOAD" -> p.chunk = false;
                    case "CHUNK_LOAD" -> p.chunk = true;
                    default -> { }
                }
                c.reconcile(); steps++;
                String got = c.state() + " " + (p.cover ? 1 : 0) + " " + p.golemCount() + " " + (p.victory ? 1 : 0) + " " + c.remaining() + " " + (c.spawnPending() ? 1 : 0);
                String exp = f[2] + " " + f[3] + " " + f[4] + " " + f[5] + " " + f[6] + " " + f[7];
                assertEquals(exp, got, "sequence " + seqs + " step " + steps + " event " + ev);
            }
        }
        assertTrue(seqs >= 400 && steps >= 3000, "replayed " + seqs + " sequences / " + steps + " steps");
    }

    @Test void guideSelectorIsStickyAndIgnoresNobodyOnline() {
        GuideTargetSelector s = new GuideTargetSelector(3.0D);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertEquals(a, s.choose(java.util.List.of(new GuideTargetSelector.Candidate(a, 5), new GuideTargetSelector.Candidate(b, 6))).get());
        assertEquals(a, s.choose(java.util.List.of(new GuideTargetSelector.Candidate(a, 6), new GuideTargetSelector.Candidate(b, 5))).get(), "within the margin: stays");
        assertEquals(b, s.choose(java.util.List.of(new GuideTargetSelector.Candidate(a, 12), new GuideTargetSelector.Candidate(b, 5))).get(), "clearly closer: switches");
        assertTrue(s.choose(java.util.List.of()).isEmpty());
    }
}
