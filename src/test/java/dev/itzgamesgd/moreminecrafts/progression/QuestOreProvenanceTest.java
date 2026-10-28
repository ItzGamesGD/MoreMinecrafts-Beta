package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestOreProvenanceTest {
    private static final long POS = 42L;

    @Test void missingOrLegacyChunkDataIsUnknownNeverNatural() {
        assertEquals(QuestOreProvenance.Verdict.UNKNOWN, QuestOreProvenance.verdict(null, POS));
        assertEquals(QuestOreProvenance.Verdict.UNKNOWN, QuestOreProvenance.verdict(
                new QuestOreProvenance.ChunkData(1, Set.of(POS)), POS));
    }

    @Test void onlyPositiveOreWriteEvidenceIsNatural() {
        var empty = new QuestOreProvenance.ChunkData(QuestOreProvenance.SCHEMA_VERSION, Set.of());
        assertEquals(QuestOreProvenance.Verdict.NON_NATURAL, QuestOreProvenance.verdict(empty, POS));
        var marked = empty.withNatural(POS, true);
        assertEquals(QuestOreProvenance.Verdict.NATURAL, QuestOreProvenance.verdict(marked, POS));
        assertEquals(QuestOreProvenance.Verdict.NON_NATURAL, QuestOreProvenance.verdict(marked, POS + 1));
        assertEquals(QuestOreProvenance.Verdict.NON_NATURAL, QuestOreProvenance.verdict(marked.withNatural(POS, false), POS));
    }

    @Test void unchangedMarksReuseTheSameInstance() {
        var empty = new QuestOreProvenance.ChunkData(QuestOreProvenance.SCHEMA_VERSION, Set.of());
        var marked = empty.withNatural(POS, true);
        assertSame(marked, marked.withNatural(POS, true));
        assertSame(empty, empty.withNatural(POS, false));
    }
}
