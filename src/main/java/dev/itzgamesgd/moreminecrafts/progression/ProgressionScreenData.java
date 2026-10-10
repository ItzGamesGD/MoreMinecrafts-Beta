package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable, localized client-facing data; widgets render these records but own no progression rules. */
public final class ProgressionScreenData {
    public record KnowledgeRow(String id, String discipline, String titleKey, String descriptionKey,
                               KnowledgeCatalog.ConditionType conditionType, String conditionKey,
                               int conditionProgress, int conditionTarget, String rewardKey, boolean unlocked) {}
    public record ResearchRow(String id, String discipline, String titleKey, String descriptionKey,
                              int level, int maximumLevel, java.util.Set<Integer> requiredKnowledge,
                              java.util.Set<String> prerequisiteResearch, ResearchDefinition.LevelCost nextCost,
                              ResearchPurchaseRules.Result availability) {}
    public record QuestRow(String id, String category, String titleKey, String descriptionKey,
                           String objectiveKey, int progress, int target, int rewardRawXp,
                           boolean complete, boolean claimed) {}

    private ProgressionScreenData() {}
    public static List<KnowledgeRow> knowledge(ResearchProgressState state, String discipline) {
        return KnowledgeCatalog.entries().stream()
                .filter(entry -> discipline == null || discipline.equals(entry.discipline()))
                .map(entry -> new KnowledgeRow(entry.id(), entry.discipline(), entry.titleKey(),
                        entry.descriptionKey(), entry.conditionType(), entry.conditionKey(),
                        state.conditionProgress(entry.conditionKey()), entry.target(),
                        entry.rewardKey(), state.hasKnowledge(entry.number())))
                .toList();
    }
    public static List<ResearchRow> research(ResearchProgressState state, String discipline,
                                             Map<String, Integer> inventory, int rawXp) {
        Set<Integer> unlockedKnowledge = java.util.stream.IntStream.rangeClosed(1,70)
                .filter(state::hasKnowledge).boxed().collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> purchased = ResearchCatalog.v4Additions().stream()
                .filter(definition -> state.researchLevel(definition.id()) > 0)
                .map(ResearchDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return ResearchCatalog.v4Additions().stream()
                .filter(entry -> discipline == null || discipline.equals(entry.discipline()))
                .map(entry -> {
                    int level = state.researchLevel(entry.id());
                    ResearchDefinition.LevelCost next = level < entry.maximumLevel() ? entry.cost(level + 1) : null;
                    ResearchPurchaseRules.Result result = next == null
                            ? ResearchPurchaseRules.Result.MAX_LEVEL
                            : ResearchPurchaseRules.validate(entry, level + 1, level, unlockedKnowledge,
                                    purchased, inventory, rawXp);
                    return new ResearchRow(entry.id(), entry.discipline(), entry.titleKey(),
                            entry.descriptionKey(), level, entry.maximumLevel(), entry.knowledgePrerequisites(),
                            entry.researchPrerequisites(), next, result);
                }).toList();
    }
    public static List<QuestRow> quests(ResearchProgressState state, String category) {
        return QuestCatalog.entries().stream()
                .filter(quest -> category == null || category.equals(quest.category()))
                .map(quest -> new QuestRow(quest.id(), quest.category(), quest.titleKey(),
                        quest.descriptionKey(), quest.objectiveKey(), state.questProgress(quest.id()),
                        quest.objectiveTarget(), quest.rewardRawXp(), state.questComplete(quest.id()),
                        state.questClaimed(quest.id())))
                .toList();
    }
}
