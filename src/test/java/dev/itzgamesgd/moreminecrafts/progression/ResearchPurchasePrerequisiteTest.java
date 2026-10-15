package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ResearchPurchasePrerequisiteTest {
    private static final ResearchDefinition.LevelCost FREE = new ResearchDefinition.LevelCost(0, List.of());

    private static ResearchDefinition fixture(String id, Set<String> prerequisites) {
        return new ResearchDefinition(id, "test", "title", "description", 1,
                Set.of(), prerequisites, List.of(FREE));
    }

    private static ResearchPurchaseRules.Result validate(ResearchDefinition definition,
                                                         ResearchProgressState state, int current, int requested) {
        return ResearchPurchaseRules.validate(definition, requested, current, Set.of(),
                ResearchPurchaseService.purchasedResearch(state), Map.of(), 0);
    }

    @Test void purchasedV3SatisfiesV3PrerequisiteAndMissingPurchaseRejects() {
        String v3 = ResearchCatalog.v3Catalog().getFirst().id();
        ResearchDefinition dependent = fixture("fixture.v3.child", Set.of(v3));
        assertEquals(ResearchPurchaseRules.Result.MISSING_PREREQUISITE,
                validate(dependent, ResearchProgressState.empty(), 0, 1));
        assertEquals(ResearchPurchaseRules.Result.PURCHASED,
                validate(dependent, ResearchProgressState.empty().with("research." + v3, 1), 0, 1));
    }

    @Test void purchasedV3SatisfiesV4PrerequisiteInIsolatedFixture() {
        String v3 = ResearchCatalog.v3Catalog().getFirst().id();
        ResearchDefinition dependent = fixture("fixture.v4.child", Set.of(v3));
        assertEquals(ResearchPurchaseRules.Result.PURCHASED,
                validate(dependent, ResearchProgressState.empty().with("research." + v3, 1), 0, 1));
    }

    @Test void purchasedV4SatisfiesV4PrerequisiteAndMissingPurchaseRejects() {
        ResearchDefinition dependent = fixture("fixture.v4.child", Set.of("combat_root"));
        assertEquals(ResearchPurchaseRules.Result.MISSING_PREREQUISITE,
                validate(dependent, ResearchProgressState.empty(), 0, 1));
        assertEquals(ResearchPurchaseRules.Result.PURCHASED,
                validate(dependent, ResearchProgressState.empty().with("research.combat_root", 1), 0, 1));
    }

    @Test void alreadyPurchasedAndInvalidLevelsStillReject() {
        ResearchDefinition dependent = fixture("fixture.child", Set.of());
        assertEquals(ResearchPurchaseRules.Result.ALREADY_PURCHASED,
                validate(dependent, ResearchProgressState.empty(), 1, 1));
        assertEquals(ResearchPurchaseRules.Result.INVALID_LEVEL,
                validate(dependent, ResearchProgressState.empty(), 0, 0));
        assertEquals(ResearchPurchaseRules.Result.INVALID_LEVEL,
                validate(dependent, ResearchProgressState.empty(), 0, 2));
    }

    @Test void savedAndReloadedV3AndV4PurchasesRemainPrerequisites() {
        String v3 = ResearchCatalog.v3Catalog().getFirst().id();
        ResearchProgressState original = ResearchProgressState.empty()
                .with("research." + v3, 2).with("research.combat_root", 1).with("knowledge.31", 1);
        var saved = ResearchProgressState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ResearchProgressState loaded = ResearchProgressState.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(original, loaded);
        assertEquals(Set.of(v3, "combat_root"), ResearchPurchaseService.purchasedResearch(loaded));
        assertEquals(ResearchPurchaseRules.Result.PURCHASED,
                validate(fixture("fixture.child", Set.of(v3, "combat_root")), loaded, 0, 1));
    }

    @Test void registeredCatalogPreservesIdentitiesAndApprovedTotals() {
        var all = ResearchCatalog.allCatalogEntries();
        assertEquals(87, all.size());
        assertEquals(87, all.stream().map(ResearchDefinition::id).distinct().count());
        assertEquals(195, all.stream().mapToInt(ResearchDefinition::maximumLevel).sum());
        assertEquals(147_270, all.stream().flatMap(entry -> entry.costs().stream())
                .mapToInt(ResearchDefinition.LevelCost::rawXp).sum());
        assertEquals(70, all.stream().filter(entry -> entry.catalogVersion().equals("v3")).count());
        assertEquals(17, all.stream().filter(entry -> entry.catalogVersion().equals("v4")).count());
        for (ResearchDefinition entry : all) assertSame(entry, ResearchCatalog.byId(entry.id()));
    }
}
