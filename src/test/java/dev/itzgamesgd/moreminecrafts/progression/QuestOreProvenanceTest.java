package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestOreProvenanceTest {
    private static final long POS = 42L;

    @Test void missingOrUntrackedChunkDataIsUnknownNeverNatural() {
        assertEquals(QuestOreProvenance.Verdict.UNKNOWN, QuestOreProvenance.verdict(null, POS));
        assertEquals(QuestOreProvenance.Verdict.UNKNOWN, QuestOreProvenance.verdict(
                new QuestOreProvenance.ChunkData(QuestOreProvenance.SCHEMA_VERSION, false, Set.of()), POS));
    }

    @Test void trackedChunkIsNaturalUntilMarked() {
        var tracked = QuestOreProvenance.ChunkData.TRACKED;
        assertEquals(QuestOreProvenance.Verdict.NATURAL, QuestOreProvenance.verdict(tracked, POS));
        var marked = tracked.withNonNatural(POS, true);
        assertEquals(QuestOreProvenance.Verdict.NON_NATURAL, QuestOreProvenance.verdict(marked, POS));
        assertEquals(QuestOreProvenance.Verdict.NATURAL, QuestOreProvenance.verdict(marked, POS + 1));
        assertEquals(QuestOreProvenance.Verdict.NATURAL, QuestOreProvenance.verdict(marked.withNonNatural(POS, false), POS));
    }

    @Test void unchangedMarksReuseTheSameInstance() {
        var marked = QuestOreProvenance.ChunkData.TRACKED.withNonNatural(POS, true);
        assertSame(marked, marked.withNonNatural(POS, true));
        assertSame(QuestOreProvenance.ChunkData.TRACKED, QuestOreProvenance.ChunkData.TRACKED.withNonNatural(POS, false));
    }
}
