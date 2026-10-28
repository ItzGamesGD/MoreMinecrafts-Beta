package dev.itzgamesgd.moreminecrafts.boss;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.boss.TombEncounterData.GuardianPresence;
import dev.itzgamesgd.moreminecrafts.boss.TombEncounterData.StartDecision;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Option B encounter rules and their world-save round trip. */
class TombEncounterDataTest {
    private static final long TOMB = 123456789L;
    private static final UUID GUARDIAN = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    private static TombEncounterData roundTrip(TombEncounterData data) {
        var encoded = TombEncounterData.TYPE.codec().encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        return TombEncounterData.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }

    @Test void dormantTombStartsAndBindsExactlyOneGuardian() {
        TombEncounterData data = new TombEncounterData();
        assertEquals(StartDecision.START, data.decideStart(TOMB, GuardianPresence.GONE));
        assertTrue(data.bind(TOMB, GUARDIAN, 7L, 100L));
        assertFalse(data.bind(TOMB, UUID.randomUUID(), 7L, 101L), "a second guardian is never bound");
        assertTrue(data.isBound(TOMB, GUARDIAN));
    }

    @Test void activeTombRefusesWhileAliveOrNotLoadedAndRestartsOnlyWhenConfirmedGone() {
        TombEncounterData data = new TombEncounterData();
        data.bind(TOMB, GUARDIAN, 7L, 100L);
        assertEquals(StartDecision.ALREADY_ACTIVE, data.decideStart(TOMB, GuardianPresence.ALIVE));
        assertEquals(StartDecision.GUARDIAN_NOT_LOADED, data.decideStart(TOMB, GuardianPresence.NOT_LOADED));
        assertTrue(data.isBound(TOMB, GUARDIAN), "refusals keep the binding");
        assertEquals(StartDecision.RESTART_AFTER_LOSS, data.decideStart(TOMB, GuardianPresence.GONE));
        assertNull(data.active(TOMB), "a confirmed loss clears the binding for the new encounter");
        assertTrue(data.bind(TOMB, UUID.randomUUID(), 8L, 200L));
    }

    @Test void nonDeathReleaseAllowsRetryButKeepsTheSeal() {
        TombEncounterData data = new TombEncounterData();
        data.bind(TOMB, GUARDIAN, 7L, 100L);
        assertFalse(data.release(TOMB, UUID.randomUUID()), "only the bound guardian can release");
        assertTrue(data.release(TOMB, GUARDIAN));
        assertFalse(data.isDefeated(TOMB));
        assertEquals(StartDecision.START, data.decideStart(TOMB, GuardianPresence.GONE));
    }

    @Test void killDefeatsOnceForeverAndOnlyForTheBoundGuardian() {
        TombEncounterData data = new TombEncounterData();
        data.bind(TOMB, GUARDIAN, 7L, 100L);
        assertFalse(data.markDefeated(TOMB, UUID.randomUUID()), "an orphan kill completes nothing");
        assertTrue(data.markDefeated(TOMB, GUARDIAN));
        assertFalse(data.markDefeated(TOMB, GUARDIAN), "completion is granted once");
        assertTrue(data.isDefeated(TOMB));
        assertNull(data.active(TOMB));
        for (GuardianPresence presence : GuardianPresence.values())
            assertEquals(StartDecision.ALREADY_DEFEATED, data.decideStart(TOMB, presence));
        assertFalse(data.bind(TOMB, UUID.randomUUID(), 7L, 300L), "a defeated tomb never binds again");
        assertFalse(data.release(TOMB, GUARDIAN));
    }

    @Test void defeatedAndActiveRecordsSurviveSaveAndRestart() {
        TombEncounterData data = new TombEncounterData();
        data.bind(TOMB, GUARDIAN, 7L, 100L);
        data.markDefeated(TOMB, GUARDIAN);
        UUID other = UUID.randomUUID();
        data.bind(42L, other, -9L, 500L);
        data.trackChunk(42L, other, 11L);
        TombEncounterData restored = roundTrip(data);
        assertTrue(restored.isDefeated(TOMB));
        assertEquals(StartDecision.ALREADY_DEFEATED, restored.decideStart(TOMB, GuardianPresence.GONE));
        assertTrue(restored.isBound(42L, other));
        assertEquals(11L, restored.active(42L).guardianChunk(), "the last tracked guardian chunk is persisted");
        assertEquals(StartDecision.GUARDIAN_NOT_LOADED, restored.decideStart(42L, GuardianPresence.NOT_LOADED));
        TombEncounterData twice = roundTrip(restored);
        assertTrue(twice.isDefeated(TOMB) && twice.isBound(42L, other), "stable across repeated restarts");
    }

    @Test void inconsistentSaveNeverReopensADefeatedTomb() {
        var json = com.google.gson.JsonParser.parseString("{\"defeated\":[5],\"active\":[{\"tomb\":5,\"guardian\":["
                + "1,2,3,4],\"guardian_chunk\":0,\"started\":0}]}");
        TombEncounterData restored = TombEncounterData.TYPE.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(restored.isDefeated(5L));
        assertNull(restored.active(5L), "an active record for a defeated tomb is dropped on load");
    }

    @Test void mutationsMarkTheSaveDirty() {
        TombEncounterData data = new TombEncounterData();
        assertFalse(data.isDirty());
        data.bind(TOMB, GUARDIAN, 7L, 100L);
        assertTrue(data.isDirty());
    }
}
