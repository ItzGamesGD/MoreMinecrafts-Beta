package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import java.util.Set;

/** Immutable cost and dependency definition for one research node. XP is raw points. */
public record ResearchDefinition(String id, String discipline, String titleKey, String descriptionKey,
                                 int maximumLevel, Set<Integer> knowledgePrerequisites,
                                 Set<String> researchPrerequisites, List<LevelCost> costs) {
    public record MaterialCost(String itemId, int count) {
        public MaterialCost {
            if (itemId == null || itemId.isBlank() || count <= 0) throw new IllegalArgumentException("Invalid material cost");
        }
    }
    public record LevelCost(int rawXp, List<MaterialCost> materials) {
        public LevelCost {
            if (rawXp < 0) throw new IllegalArgumentException("Negative XP cost");
            materials = List.copyOf(materials);
        }
    }

    public ResearchDefinition {
        if (id == null || id.isBlank() || maximumLevel <= 0) throw new IllegalArgumentException("Invalid research definition");
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
