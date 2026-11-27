package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EffectJournalTest {
    @TempDir Path dir;
    @Test void atomicJournalRoundTripAndCorruptionFailsClosed() throws Exception {
        EffectJournal.write(dir, "arena", "state", false);
        assertEquals("state", EffectJournal.read(dir, "arena").state());
        EffectJournal.write(dir, "arena", "next", true);
        assertTrue(EffectJournal.read(dir, "arena").inFlight());
        Files.writeString(EffectJournal.path(dir, "arena"), "broken");
        assertThrows(java.io.IOException.class, () -> EffectJournal.read(dir, "arena"));
    }
    @Test void terrainConflictIsPermanentAndSurvivesSerialization() {
        TerrainLedger l = new TerrainLedger();
        l.record(1L, "stone[foo=bar]"); l.record(2L, "log[axis=x]");
        assertEquals("stone[foo=bar]", l.restore(1L, true));
        assertNull(l.restore(2L, false)); // occupied by player or liquid
        assertNull(l.restore(2L, true)); // conflict never becomes a delayed overwrite
        TerrainLedger copy = ArenaController.GSON.fromJson(ArenaController.GSON.toJson(l), TerrainLedger.class);
        assertTrue(copy.conflicts.contains(2L)); assertTrue(copy.pending.isEmpty());
    }
}
