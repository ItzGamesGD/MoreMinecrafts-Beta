package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ProgressionScreenDataTest {
    @Test void knowledgeScreenDataListsEveryStableEntryWithAcquisitionAndRewardMetadata() {
        var rows = ProgressionScreenData.knowledge(new ResearchProgressState(Map.of()), null);
        assertEquals(70, rows.size());
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            assertEquals("knowledge." + (i + 1), row.id());
            assertNotNull(row.conditionType());
            assertFalse(row.conditionKey().isBlank());
            assertFalse(row.rewardKey().isBlank());
            assertFalse(row.unlocked());
        }
        assertTrue(rows.get(50).unresolved(), "the rare-mob definition stays visibly unresolved");
        assertFalse(rows.get(49).unresolved());
    }

    @Test void researchScreenDataExposesServerRuleAvailabilityAndRequirements() {
        var state = new ResearchProgressState(Map.of());
        var rows = ProgressionScreenData.research(state, null, Map.of(), 0);
        assertEquals(ResearchCatalog.v3Catalog().size() + ResearchCatalog.v4Additions().size(), rows.size());
        for (var row : rows) {
            assertEquals(0, row.level());
            assertTrue(row.maximumLevel() > 0);
            assertNotNull(row.nextCost());
            assertNotNull(row.requiredKnowledge());
            assertNotNull(row.prerequisiteResearch());
            assertEquals(row.prerequisitesDefined() ? ResearchPurchaseRules.Result.MISSING_KNOWLEDGE
                    : ResearchPurchaseRules.Result.MISSING_PREREQUISITE, row.availability());
        }
    }
}
