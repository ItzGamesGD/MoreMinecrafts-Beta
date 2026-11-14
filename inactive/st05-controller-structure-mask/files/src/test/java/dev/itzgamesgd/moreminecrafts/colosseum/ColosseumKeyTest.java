package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyRecord;
import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Q113-B/Q97: exactly one shared key per wave, no duplicate issue, and a key lost to a crash is reissued once. */
class ColosseumKeyTest {
    private final Scenario s = new Scenario();

    private KeyRecord clearRoundOne() {
        s.start(1);
        s.world.tick(s.state, 30);
        s.world.clearRound(s.state, 4000);
        return s.state.ledger().wave(s.state.trialId(), 1).key();
    }

    @Test
    void oneKeyIsIssuedWhenTheWaveClearsAndNeverAgain() {
        KeyRecord key = clearRoundOne();
        assertEquals(KeyState.SPAWNED, key.state());
        assertEquals(1, key.issueCount());
        assertEquals(1, s.world.keysSpawned);
        s.world.tick(s.state, 500);
        assertEquals(1, s.world.keysSpawned, "idle ticks never issue another key");
        assertEquals(KeyState.SPAWNED, key.state());
    }

    @Test
    void eachWaveGetsItsOwnSingleKey() {
        s.start(2);
        s.world.tick(s.state, 30);
        s.clearThrough(2);
        assertEquals(2, s.world.keysSpawned, "two waves cleared, two keys, regardless of two players");
        var waves = s.state.ledger().waves();
        assertEquals(2, waves.size());
        assertFalse(waves.get(0).key().entity().equals(waves.get(1).key().entity()));
    }

    @Test
    void aKeyWaitsForTheCenterChunkAndIsIssuedOnceWhenItLoads() {
        s.world.centerLoaded = false;
        KeyRecord key = clearRoundOne();
        assertEquals(KeyState.PENDING, key.state());
        s.world.tick(s.state, 50);
        assertEquals(0, s.world.keysSpawned);
        s.world.centerLoaded = true;
        s.world.tick(s.state, 5);
        assertEquals(KeyState.SPAWNED, key.state());
        assertEquals(1, s.world.keysSpawned);
        s.world.tick(s.state, 200);
        assertEquals(1, s.world.keysSpawned);
    }

    @Test
    void aKeyThatLeavesTheWorldNormallyIsConsumedAndNeverReissued() {
        KeyRecord key = clearRoundOne();
        s.world.removeKey(s.state, key.entity()); // picked up, merged or despawned
        assertEquals(KeyState.CONSUMED, key.state());
        s.world.tick(s.state, 500);
        assertEquals(1, s.world.keysSpawned);
        assertEquals(KeyState.CONSUMED, key.state());
    }

    @Test
    void aKeyStoredToDiskIsConfirmedAndTreatedAsConsumedIfLaterMissing() {
        KeyRecord key = clearRoundOne();
        UUID entity = key.entity();
        s.world.unloadKeyToStorage(s.state, entity);
        assertEquals(KeyState.CONFIRMED, key.state());
        // Unloaded: it may still be on disk, so nothing is reissued.
        s.world.chunksLoaded = false;
        s.world.tick(s.state, 100);
        assertEquals(1, s.world.keysSpawned);
        // The chunk is loaded and the stored key is simply not there (taken in a way no event saw): consumed, not lost.
        s.world.chunksLoaded = true;
        s.world.tick(s.state, 5);
        assertEquals(KeyState.CONSUMED, key.state());
        assertEquals(1, s.world.keysSpawned, "a confirmed key is never reissued");
    }

    @Test
    void aKeyLostToACrashBeforeItReachedStorageIsReissuedExactlyOnce() {
        KeyRecord key = clearRoundOne();
        assertEquals(KeyState.SPAWNED, key.state());
        // Crash and restart: the saved state says SPAWNED, but the entity never made it to disk.
        ColosseumTrialState restarted = TestArenas.roundTrip(s.state);
        FakeColosseumWorld after = new FakeColosseumWorld();
        after.present.addAll(s.world.present);
        after.tick(restarted, 5);
        KeyRecord reissued = restarted.ledger().wave(restarted.trialId(), 1).key();
        assertEquals(1, after.keysSpawned, "exactly one replacement");
        assertEquals(KeyState.SPAWNED, reissued.state());
        assertEquals(2, reissued.issueCount(), "the diagnostic count shows a crash reissue");
        after.tick(restarted, 500);
        assertEquals(1, after.keysSpawned, "and no further keys afterwards");
    }

    @Test
    void aConfirmedKeyMissingAfterARestartIsNotReissued() {
        KeyRecord key = clearRoundOne();
        s.world.unloadKeyToStorage(s.state, key.entity());
        ColosseumTrialState restarted = TestArenas.roundTrip(s.state);
        FakeColosseumWorld after = new FakeColosseumWorld();
        after.present.addAll(s.world.present);
        after.tick(restarted, 5);
        assertEquals(0, after.keysSpawned);
        assertEquals(KeyState.CONSUMED, restarted.ledger().wave(restarted.trialId(), 1).key().state());
    }

    @Test
    void loadDecisionsAcceptOnlyTheRecordedKeyAndConfirmItFromStorage() {
        KeyRecord key = clearRoundOne();
        UUID trial = s.state.trialId();
        assertFalse(ColosseumController.decideKeyLoad(s.state, trial, 1, UUID.randomUUID(), true), "a duplicate copy");
        assertFalse(ColosseumController.decideKeyLoad(s.state, UUID.randomUUID(), 1, key.entity(), true), "unknown trial");
        assertFalse(ColosseumController.decideKeyLoad(s.state, trial, 2, key.entity(), true), "no wave record");
        assertTrue(ColosseumController.decideKeyLoad(s.state, trial, 1, key.entity(), false), "a fresh add is accepted");
        assertEquals(KeyState.SPAWNED, key.state());
        assertTrue(ColosseumController.decideKeyLoad(s.state, trial, 1, key.entity(), true));
        assertEquals(KeyState.CONFIRMED, key.state(), "loading from storage proves it was saved");
        // After a pickup nothing may come back, even a stale saved copy of the same entity.
        s.world.removeKey(s.state, key.entity());
        assertFalse(ColosseumController.decideKeyLoad(s.state, trial, 1, key.entity(), true));
    }

    @Test
    void failureResetAndTimeoutNeverReclaimAnIssuedKey() {
        for (String ending : new String[]{"reset", "timeout"}) {
            Scenario t = new Scenario();
            t.start(1);
            t.world.tick(t.state, 30);
            t.world.clearRound(t.state, 4000);
            UUID key = t.state.ledger().waves().get(0).key().entity();
            if (ending.equals("reset")) ColosseumController.forceReset(t.state, t.world);
            else {
                t.state.trialRemaining = 1;
                t.world.tick(t.state, 1);
            }
            assertTrue(t.world.entities.containsKey(key), ending + ": the key entity is still in the world");
            assertFalse(t.world.discarded.contains(key), ending + ": never discarded");
            assertEquals(KeyState.SPAWNED, t.state.ledger().waves().get(0).key().state());
        }
    }

    @Test
    void migratedWavesWithNoKeySystemAreNeverIssuedAKey() {
        s.start(1);
        UUID trial = s.state.trialId();
        s.state.ledger().addWave(WaveRecord.cleared(trial, 1, 5, java.util.List.of(), java.util.List.of(), 1L, KeyState.NONE));
        s.world.tick(s.state, 300);
        assertEquals(0, s.world.keysSpawned);
        assertNull(s.state.ledger().wave(trial, 5).key().entity());
        assertNotNull(s.state.ledger().wave(trial, 5));
    }
}
