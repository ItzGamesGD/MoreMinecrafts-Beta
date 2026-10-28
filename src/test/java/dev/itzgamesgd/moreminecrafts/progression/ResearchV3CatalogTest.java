package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

final class ResearchV3CatalogTest {
    @Test void v3CatalogIntegratesEveryApprovedCostAndHasCompletePrerequisites() {
        var entries = ResearchCatalog.v3Catalog();
        assertEquals(70, entries.size());
        assertEquals(178, entries.stream().mapToInt(ResearchDefinition::maximumLevel).sum());
        assertEquals(130_360, entries.stream().flatMap(entry -> entry.costs().stream())
                .mapToInt(ResearchDefinition.LevelCost::rawXp).sum());
        assertTrue(entries.stream().allMatch(entry -> entry.id().startsWith("v3.research.")
                && entry.catalogVersion().equals("v3") && entry.prerequisitesDefined()));

        var allKnowledge = IntStream.rangeClosed(1, 70).boxed().collect(Collectors.toUnmodifiableSet());
        for (var entry : entries) {
            assertEquals(ResearchPurchaseRules.Result.MISSING_PREREQUISITE,
                    ResearchPurchaseRules.validate(entry, 1, 0, allKnowledge, Set.of(), Map.of(), Integer.MAX_VALUE),
                    entry.id());
            assertTrue(entry.researchPrerequisites().contains(entry.discipline() + "_root"), entry.id());
        }
    }

    @Test void approvedGraphHasExistingSameDisciplineEdgesAndIsAcyclic() {
        var all = ResearchCatalog.allCatalogEntries();
        var byId = all.stream().collect(Collectors.toMap(ResearchDefinition::id, e -> e));
        for (var entry : all) for (String prerequisiteId : entry.researchPrerequisites()) {
            var prerequisite = byId.get(prerequisiteId);
            assertNotNull(prerequisite, entry.id() + " -> " + prerequisiteId);
            if (!prerequisiteId.endsWith("_root")) assertEquals(entry.discipline(), prerequisite.discipline(), entry.id());
        }
        Set<String> visited = new java.util.HashSet<>(), visiting = new java.util.HashSet<>();
        for (var entry : all) assertAcyclic(entry.id(), byId, visited, visiting);
    }

    private static void assertAcyclic(String id, Map<String, ResearchDefinition> byId,
                                      Set<String> visited, Set<String> visiting) {
        if (visited.contains(id)) return;
        assertTrue(visiting.add(id), "research prerequisite cycle includes " + id);
        for (String prerequisite : byId.get(id).researchPrerequisites())
            assertAcyclic(prerequisite, byId, visited, visiting);
        visiting.remove(id);
        visited.add(id);
    }

    @Test void v3CompressedPriceUnitsPreserveExactSixtyFourToOneEquivalent() {
        var compressedCosts = ResearchCatalog.v3Catalog().stream().flatMap(entry -> entry.costs().stream())
                .flatMap(cost -> cost.materials().stream()).filter(cost -> cost.compressedUnits() > 0).toList();
        assertFalse(compressedCosts.isEmpty());
        assertTrue(compressedCosts.stream().allMatch(cost -> cost.count() == cost.compressedUnits() * 64));
    }

    @Test void v4AdditionIdentitiesRemainDistinctAndUnchanged() {
        assertEquals(17, ResearchCatalog.v4Additions().size());
        assertEquals(16_910, ResearchCatalog.v4Additions().stream()
                .flatMap(entry -> entry.costs().stream()).mapToInt(ResearchDefinition.LevelCost::rawXp).sum());
        assertTrue(ResearchCatalog.byId("combat_root").catalogVersion().equals("v4"));
        assertTrue(ResearchCatalog.byId("v3.research.01").catalogVersion().equals("v3"));
    }
}
