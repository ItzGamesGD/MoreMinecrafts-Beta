package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import java.util.Set;

/** One-time, cost-free grant of discipline roots for research already present in legacy saves. */
public final class ResearchRootMigration {
    public static final String VERSION_KEY = "migration.stage22.research_roots";
    private static final Map<String, String> ROOTS = Map.of(
            "resources", "resources_root", "production", "production_root", "living", "living_root",
            "combat", "combat_root", "ecology", "ecology_root", "world", "world_root", "alchemy", "alchemy_root");
    private static final Set<String> ROOT_IDS = Set.copyOf(ROOTS.values());

    private ResearchRootMigration() {}

    public static ResearchProgressState migrate(ResearchProgressState original) {
        if (original.get(VERSION_KEY) > 0) return original;
        ResearchProgressState migrated = original;
        for (ResearchDefinition definition : ResearchCatalog.allCatalogEntries()) {
            if (ROOT_IDS.contains(definition.id()) || original.researchLevel(definition.id()) <= 0) continue;
            String rootId = ROOTS.get(definition.discipline());
            if (rootId != null && migrated.researchLevel(rootId) == 0)
                migrated = migrated.with("research." + rootId, 1);
        }
        return migrated.with(VERSION_KEY, 1);
    }
}
