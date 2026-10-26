package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ResearchV3LedgerTest {
    private static final Path LEDGER = Path.of("src/main/resources/data/moreminecrafts/research/research_v3.json");

    @Test void v3PriceLedgerHasApprovedNodePurchaseAndRawXpTotals() throws IOException {
        JsonObject root;
        try (var reader = Files.newBufferedReader(LEDGER)) { root = JsonParser.parseReader(reader).getAsJsonObject(); }
        assertEquals("v3", root.get("version").getAsString());
        JsonArray entries = root.getAsJsonArray("entries");
        int purchases = 0;
        int rawXp = 0;
        for (var nodeElement : entries) {
            JsonObject node = nodeElement.getAsJsonObject();
            JsonArray costs = node.getAsJsonArray("costs");
            purchases += costs.size();
            int nodeXp = 0;
            for (var costElement : costs) nodeXp += costElement.getAsJsonObject().get("rawXp").getAsInt();
            rawXp += nodeXp;
            assertEquals(node.get("approvedTotalRawXp").getAsInt(), nodeXp, node.get("id").getAsString());
            assertTrue(node.get("prerequisitesDefined").getAsBoolean(),
                    "Stage 22 requires every V3 node to have explicit prerequisites");
            assertFalse(node.getAsJsonArray("knowledgePrerequisites").isEmpty());
            assertTrue(node.getAsJsonArray("researchPrerequisites").size() >= 1,
                    "each V3 node requires its discipline root");
        }
        assertEquals(70, entries.size());
        assertEquals(178, purchases);
        assertEquals(130_360, rawXp);
    }
}
