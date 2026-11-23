package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.itzgamesgd.moreminecrafts.trade.PiglinTradeConfig;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PiglinTradeConfigTest {
    @Test void closedByDefaultEvenForMaxedPlayers() {
        assertFalse(PiglinTradeConfig.CLOSED.unlocked(id -> 99));
    }
    @Test void blankIdIsClosed() {
        assertFalse(new PiglinTradeConfig(Optional.of(" ")).unlocked(id -> 99));
    }
    @Test void requiresLevelOfConfiguredResearchOnly() {
        PiglinTradeConfig c = new PiglinTradeConfig(Optional.of("x"));
        assertFalse(c.unlocked(id -> id.equals("x") ? 0 : 5));
        assertTrue(c.unlocked(id -> id.equals("x") ? 1 : 0));
    }

    @Test void approvedResearchNodeMatchesApprovedValues() {
        ResearchDefinition d = ResearchCatalog.byId("piglin_trading");
        assertTrue(d != null && d.catalogVersion().equals("v4") && d.tier().equals("T3") && d.discipline().equals("world"));
        org.junit.jupiter.api.Assertions.assertEquals(1, d.maximumLevel());
        org.junit.jupiter.api.Assertions.assertEquals(Set.of(48), d.knowledgePrerequisites());
        org.junit.jupiter.api.Assertions.assertEquals(Set.of("v3.research.54"), d.researchPrerequisites());
        org.junit.jupiter.api.Assertions.assertEquals(2400, d.cost(1).rawXp());
        org.junit.jupiter.api.Assertions.assertEquals(
                Map.of("minecraft:gold_block", 32, "minecraft:crying_obsidian", 16, "minecraft:emerald", 8),
                d.cost(1).materials().stream().collect(java.util.stream.Collectors.toMap(
                        ResearchDefinition.MaterialCost::itemId, ResearchDefinition.MaterialCost::count)));
    }

    @Test void purchaseRequiresTheBarterStudyAndKnowledge() {
        ResearchDefinition d = ResearchCatalog.byId("piglin_trading");
        Map<String, Integer> mats = Map.of("minecraft:gold_block", 32, "minecraft:crying_obsidian", 16, "minecraft:emerald", 8);
        assertTrue(ResearchPurchaseRules.validate(d, 1, 0, Set.of(48), Set.of("v3.research.54"), mats, 2400)
                == ResearchPurchaseRules.Result.PURCHASED);
        assertTrue(ResearchPurchaseRules.validate(d, 1, 0, Set.of(48), Set.of(), mats, 2400)
                == ResearchPurchaseRules.Result.MISSING_PREREQUISITE);
        assertTrue(ResearchPurchaseRules.validate(d, 1, 0, Set.of(), Set.of("v3.research.54"), mats, 2400)
                == ResearchPurchaseRules.Result.MISSING_KNOWLEDGE);
    }

    @Test void shippedConfigPointsAtARealResearch() throws Exception {
        try (var in = PiglinTradeConfigTest.class.getResourceAsStream("/data/moreminecrafts/piglin_trade/config.json")) {
            var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject();
            assertTrue(ResearchCatalog.byId(json.get("required_research").getAsString()) != null);
        }
    }

    @Test void shippedOffersReferenceRealResearchAndStayWithinSlotLimits() throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of("src/main/resources/data/moreminecrafts/piglin_trade/offers");
        int tier1 = 0, tier2 = 0;
        try (var files = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path f : files.toList()) {
                var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(f)).getAsJsonObject();
                var offer = json.getAsJsonObject("offer");
                if (json.has("required_research")) {
                    assertTrue(ResearchCatalog.byId(json.get("required_research").getAsString()) != null, f.toString());
                    tier2++;
                } else tier1++;
                assertTrue(offer.getAsJsonObject("buy").get("count").getAsInt() <= 64, f.toString());
                if (offer.has("buyB")) assertTrue(offer.getAsJsonObject("buyB").get("count").getAsInt() <= 64, f.toString());
                assertTrue(offer.get("maxUses").getAsInt() >= 1 && offer.get("xp").getAsInt() == 0
                        && !offer.get("rewardExp").getAsBoolean(), f.toString());
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(10, tier1);
        org.junit.jupiter.api.Assertions.assertEquals(13, tier2);
    }
}
