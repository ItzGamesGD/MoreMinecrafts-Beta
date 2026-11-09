package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ResearchRootMigrationTest {
    @Test void legacyPurchasesGrantOnlyTheirDisciplineRootsWithoutChangingExistingProgress() {
        ResearchProgressState legacy = ResearchProgressState.empty()
                .with("research.v3.research.04", 2)
                .with("research.v3.research.35", 1)
                .with("knowledge.31", 1)
                .with("condition.ore_types_mined", 5);

        ResearchProgressState migrated = ResearchRootMigration.migrate(legacy);

        assertEquals(2, migrated.researchLevel("v3.research.04"));
        assertEquals(1, migrated.researchLevel("v3.research.35"));
        assertEquals(1, migrated.researchLevel("resources_root"));
        assertEquals(1, migrated.researchLevel("combat_root"));
        assertEquals(0, migrated.researchLevel("production_root"));
        assertEquals(1, migrated.get("knowledge.31"));
        assertEquals(5, migrated.conditionProgress("ore_types_mined"));
        assertEquals(1, migrated.get(ResearchRootMigration.VERSION_KEY));
    }

    @Test void migrationIsIdempotentAndDoesNotOverwriteAnExistingRootPurchase() {
        ResearchProgressState legacy = ResearchProgressState.empty()
                .with("research.v3.research.08", 1)
                .with("research.production_root", 1);
        ResearchProgressState migrated = ResearchRootMigration.migrate(legacy);
        assertEquals(1, migrated.researchLevel("production_root"));
        assertSame(migrated, ResearchRootMigration.migrate(migrated));
    }

    @Test void aNewPlayerReceivesMigrationMarkerButNoFreeRoots() {
        ResearchProgressState migrated = ResearchRootMigration.migrate(ResearchProgressState.empty());
        for (String root : new String[]{"resources_root", "production_root", "living_root", "combat_root",
                "ecology_root", "world_root", "alchemy_root"})
            assertEquals(0, migrated.researchLevel(root), root);
        assertEquals(1, migrated.get(ResearchRootMigration.VERSION_KEY));
    }
}
