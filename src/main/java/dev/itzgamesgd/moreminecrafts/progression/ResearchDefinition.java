package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import java.util.Set;

/** Immutable catalog entry. Tier, catalog version, and purchase level remain independent. */
public record ResearchDefinition(String id, String discipline, String titleKey, String descriptionKey,
                                 int maximumLevel, Set<Integer> knowledgePrerequisites,
                                 Set<String> researchPrerequisites, List<LevelCost> costs,
                                 String catalogVersion, String tier, boolean prerequisitesDefined) {
    public record MaterialCost(String itemId, int count, int compressedUnits) {
        public MaterialCost(String itemId, int count) { this(itemId, count, 0); }
        public MaterialCost {
            if (itemId == null || itemId.isBlank() || count <= 0 || compressedUnits < 0)
                throw new IllegalArgumentException("Invalid material cost");
            if (compressedUnits > 0 && (long) compressedUnits * 64 > count)
                throw new IllegalArgumentException("Compressed units exceed the original-item equivalent");
        }
    }
    public record LevelCost(int rawXp, List<MaterialCost> materials) {
        public LevelCost {
            if (rawXp < 0) throw new IllegalArgumentException("Negative XP cost");
            materials = List.copyOf(materials);
        }
    }

    /** Existing V4 constructor remains source-compatible; its catalog identity is explicit. */
    public ResearchDefinition(String id, String discipline, String titleKey, String descriptionKey,
                              int maximumLevel, Set<Integer> knowledgePrerequisites,
                              Set<String> researchPrerequisites, List<LevelCost> costs) {
        this(id, discipline, titleKey, descriptionKey, maximumLevel, knowledgePrerequisites,
                researchPrerequisites, costs, "v4", "UNSPECIFIED", true);
    }

    public ResearchDefinition {
        if (id == null || id.isBlank() || maximumLevel <= 0) throw new IllegalArgumentException("Invalid research definition");
        if (catalogVersion == null || catalogVersion.isBlank()) throw new IllegalArgumentException("Missing catalog version");
        tier = tier == null || tier.isBlank() ? "UNSPECIFIED" : tier;
        knowledgePrerequisites = Set.copyOf(knowledgePrerequisites);
        researchPrerequisites = Set.copyOf(researchPrerequisites);
        costs = List.copyOf(costs);
        if (costs.size() != maximumLevel) throw new IllegalArgumentException("One price is required per level for " + id);
    }
    public LevelCost cost(int level) {
        if (level < 1 || level > maximumLevel) throw new IllegalArgumentException("Invalid level " + level);
        return costs.get(level - 1);
    }
}
