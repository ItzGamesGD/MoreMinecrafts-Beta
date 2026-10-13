package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class K5KnowledgeRulesTest {
    @Test void approvedCatalogUsesTwoHundredDirectMinedNaturalLogs() {
        var k5 = KnowledgeCatalog.byNumber(5);
        assertEquals("knowledge.5", k5.id());
        assertEquals(200, k5.target());
        assertEquals(KnowledgeCatalog.ConditionType.BLOCKS_HARVESTED, k5.conditionType());
        assertEquals("natural_vanilla_logs_mined", k5.conditionKey());
    }

    @Test void counterCapsAtTwoHundredAndSurvivesCodecRoundTrip() {
        int count = 0;
        for (int i = 0; i < 199; i++) count = KnowledgeConditionProgress.advance(count, count + 1, 200);
        assertEquals(199, count);
        assertFalse(KnowledgeRules.conditionMet(count, 200));
        count = KnowledgeConditionProgress.advance(count, count + 1, 200);
        assertEquals(200, count);
        assertTrue(KnowledgeRules.conditionMet(count, 200));
        assertEquals(200, KnowledgeConditionProgress.advance(count, count + 1, 200));
        var state = ResearchProgressState.empty().with("condition.natural_vanilla_logs_mined", count)
                .with("knowledge.5", 1);
        JsonElement encoded = ResearchProgressState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        var restored = ResearchProgressState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(200, restored.get("condition.natural_vanilla_logs_mined"));
        assertTrue(restored.hasKnowledge(5));
    }

    @Test void existingKnowledgeScreenShowsK5ProgressOutOfTwoHundred() {
        var state = ResearchProgressState.empty().with("condition.natural_vanilla_logs_mined", 127);
        var row = ProgressionScreenData.knowledge(state, "resources").stream()
                .filter(entry -> entry.id().equals("knowledge.5")).findFirst().orElseThrow();
        assertEquals(127, row.conditionProgress());
        assertEquals(200, row.conditionTarget());
        assertFalse(row.unlocked());
    }
}
