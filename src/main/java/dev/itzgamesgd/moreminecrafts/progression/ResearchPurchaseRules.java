package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import java.util.Set;

/** Pure purchase validation; it never mutates inventory, XP, or persistent progression. */
public final class ResearchPurchaseRules {
    public enum Result {
        PURCHASED, INVALID_LEVEL, MISSING_KNOWLEDGE, MISSING_PREREQUISITE,
        MISSING_MATERIALS, INSUFFICIENT_XP, ALREADY_PURCHASED, MAX_LEVEL, INVALID_STATE
    }
    private ResearchPurchaseRules() {}

    public static Result validate(ResearchDefinition definition, int requestedLevel, int currentLevel,
                                  Set<Integer> knowledge, Set<String> purchasedResearch,
                                  Map<String, Integer> availableMaterials, int rawXp) {
        if (definition == null || currentLevel < 0 || currentLevel > definition.maximumLevel()
                || rawXp < 0) return Result.INVALID_STATE;
        if (requestedLevel < 1 || requestedLevel > definition.maximumLevel()) return Result.INVALID_LEVEL;
        if (currentLevel >= requestedLevel) return Result.ALREADY_PURCHASED;
        if (currentLevel >= definition.maximumLevel()) return Result.MAX_LEVEL;
        if (requestedLevel != currentLevel + 1) return Result.INVALID_LEVEL;
        if (!definition.prerequisitesDefined()) return Result.MISSING_PREREQUISITE;
        if (!definition.knowledgePrerequisites().isEmpty()
                && definition.knowledgePrerequisites().stream().noneMatch(knowledge::contains))
            return Result.MISSING_KNOWLEDGE;
        if (!purchasedResearch.containsAll(definition.researchPrerequisites()))
            return Result.MISSING_PREREQUISITE;
        var cost = definition.cost(requestedLevel);
        for (var material : cost.materials()) {
            if (availableMaterials.getOrDefault(material.itemId(), 0) < material.count())
                return Result.MISSING_MATERIALS;
        }
        return rawXp < cost.rawXp() ? Result.INSUFFICIENT_XP : Result.PURCHASED;
    }
}
