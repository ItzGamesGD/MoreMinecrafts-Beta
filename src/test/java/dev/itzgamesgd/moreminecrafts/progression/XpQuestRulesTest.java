package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class XpQuestRulesTest {
    private static final QuestDefinition QUEST = new QuestDefinition("xpq.test.q", "test", "t", "d", "o", 1_000, 500);

    @Test void progressIsMonotonicCappedAndCompletesOnce() {
        ResearchProgressState state = ResearchProgressState.empty();
        state = QuestRules.raiseProgress(state, QUEST, 400);
        assertEquals(400, state.questProgress(QUEST.id()));
        assertSame(state, QuestRules.raiseProgress(state, QUEST, 300), "lower evidence never lowers progress");
        state = QuestRules.raiseProgress(state, QUEST, 5_000);
        assertEquals(1_000, state.questProgress(QUEST.id()));
        assertTrue(state.questComplete(QUEST.id()));
        assertSame(state, QuestRules.raiseProgress(state, QUEST, 9_999), "re-evaluation after completion is a no-op");
    }

    @Test void claimedQuestIsNeverModified() {
        ResearchProgressState state = QuestRules.raiseProgress(ResearchProgressState.empty(), QUEST, 1_000).claimQuest(QUEST.id());
        assertSame(state, QuestRules.raiseProgress(state, QUEST, 2_000));
    }

    @Test void storedProgressAtTargetCompletesEvenWithoutNewEvidence() {
        ResearchProgressState state = ResearchProgressState.empty().withQuestProgress(QUEST.id(), 1_000);
        assertTrue(QuestRules.raiseProgress(state, QUEST, 0).questComplete(QUEST.id()));
    }

    @Test void distinctEntriesCountOnce() {
        ResearchProgressState state = ResearchProgressState.empty();
        for (String entry : List.of("a", "b", "a", "c", "b", "", " ")) state = QuestRules.observeDistinct(state, "xpq.test.q", entry);
        assertEquals(3, QuestRules.distinctCount(state, "xpq.test.q"));
        assertEquals(0, QuestRules.distinctCount(state, "xpq.test.other"));
    }

    @Test void countersAreUncappedSaturatingAndSeedOnlyRaises() {
        ResearchProgressState state = QuestRules.addCounter(ResearchProgressState.empty(), "k", 1_000_000);
        assertEquals(1_000_000, QuestRules.counter(state, "k"), "far above any Knowledge cap");
        state = QuestRules.addCounter(state, "k", Long.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, QuestRules.counter(state, "k"), "saturates instead of overflowing");
        assertSame(state, QuestRules.addCounter(state, "k", 0));
        assertSame(state, QuestRules.addCounter(state, "k", -5));
        ResearchProgressState seeded = QuestRules.seedCounter(ResearchProgressState.empty(), "c", 100);
        assertEquals(100, QuestRules.counter(seeded, "c"));
        assertSame(seeded, QuestRules.seedCounter(seeded, "c", 40), "seeding never lowers a counter");
        assertFalse(QuestRules.counter(seeded, "c") > 100);
    }

    @Test void migrationStampsSchemaOnce() {
        ResearchProgressState migrated = XpQuestTracking.migrate(ResearchProgressState.empty());
        assertEquals(XpQuestTracking.SCHEMA_VERSION, migrated.get(XpQuestTracking.SCHEMA_KEY));
        assertSame(migrated, XpQuestTracking.migrate(migrated));
    }

    @Test void researchValueIsOwnedApprovedCost() {
        var catalog = ResearchCatalog.allCatalogEntries();
        ResearchDefinition first = catalog.stream().filter(d -> d.maximumLevel() >= 2).findFirst().orElseThrow();
        ResearchProgressState state = ResearchProgressState.empty().with("research." + first.id(), 2);
        long expected = first.cost(1).rawXp() + first.cost(2).rawXp();
        var tracker = new XpQuestCatalogData.Tracker("research_value", "", List.of(), 1, null);
        assertEquals(expected, XpQuestResearchValue.value(state, tracker, catalog));
        long all = catalog.stream().mapToLong(d -> d.costs().stream().mapToLong(ResearchDefinition.LevelCost::rawXp).sum()).sum();
        assertEquals(147_670, all, "research ledger total");
    }
}
