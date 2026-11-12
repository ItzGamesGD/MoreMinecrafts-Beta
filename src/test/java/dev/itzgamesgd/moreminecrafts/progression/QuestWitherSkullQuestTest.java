package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Counter semantics behind {@code xpq.cmb.wither_skulls_3}; the provenance hooks themselves are GameTests. */
final class QuestWitherSkullQuestTest {
    private static final XpQuestCatalogData.Spec SPEC = XpQuestCatalogData.catalog().quests().stream()
            .filter(q -> q.id().equals("xpq.cmb.wither_skulls_3")).findFirst().orElseThrow();
    private static final QuestDefinition QUEST = SPEC.definition();

    private static ResearchProgressState credit(ResearchProgressState state, int skulls) {
        return QuestRules.addCounter(state, QUEST_KEY, skulls);
    }

    private static final String QUEST_KEY = "wither_skull_drops";

    @Test void approvedIdentityAndRewardAreUnchanged() {
        assertEquals(3, SPEC.target());
        assertEquals(1_100, SPEC.rewardRawXp());
        assertEquals("NOW", SPEC.status());
        assertEquals("counter", SPEC.tracker().kind());
        assertEquals(QUEST_KEY, SPEC.tracker().key());
    }

    @Test void threeDistinctCreditedDropsCompleteAndFewerDoNot() {
        ResearchProgressState state = ResearchProgressState.empty();
        state = QuestRules.raiseProgress(credit(state, 1), QUEST, QuestRules.counter(credit(state, 1), QUEST_KEY));
        assertEquals(1, state.questProgress(QUEST.id()));
        state = credit(state, 1);
        state = QuestRules.raiseProgress(state, QUEST, QuestRules.counter(state, QUEST_KEY));
        assertEquals(2, state.questProgress(QUEST.id()));
        assertFalse(state.questComplete(QUEST.id()));
        state = credit(state, 1);
        state = QuestRules.raiseProgress(state, QUEST, QuestRules.counter(state, QUEST_KEY));
        assertTrue(state.questComplete(QUEST.id()));
    }

    @Test void noCreditMeansNoProgress() {
        assertEquals(0, QuestRules.counter(ResearchProgressState.empty(), QUEST_KEY));
    }
}
