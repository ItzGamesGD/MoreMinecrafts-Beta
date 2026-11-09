package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable, localized client-facing data; widgets render these records but own no progression rules. */
public final class ProgressionScreenData {
    public record KnowledgeRow(String id, String discipline, String titleKey, String descriptionKey,
                               KnowledgeCatalog.ConditionType conditionType, String conditionKey,
                               int conditionProgress, int conditionTarget, String rewardKey, boolean unlocked,
                               boolean unresolved) {}
    public record ResearchRow(String id, String discipline, String titleKey, String descriptionKey,
                              int level, int maximumLevel, String catalogVersion, String tier, boolean prerequisitesDefined,
                              java.util.Set<Integer> requiredKnowledge,
                              java.util.Set<String> prerequisiteResearch, ResearchDefinition.LevelCost nextCost,
                              ResearchPurchaseRules.Result availability) {}
    public record QuestRow(String id, String category, String titleKey, String descriptionKey,
                           String objectiveKey, int progress, int target, int rewardRawXp,
                           boolean complete, boolean claimed, List<QuestGroup> groups) {}
    public record QuestTarget(String entityId, String titleKey, boolean complete) {}
    public record QuestGroup(String id, List<QuestTarget> targets, boolean complete) {
        public QuestGroup { targets = List.copyOf(targets); }
    }

    private ProgressionScreenData() {}
    public static List<KnowledgeRow> knowledge(ResearchProgressState state, String discipline) {
        return KnowledgeCatalog.entries().stream()
                .filter(entry -> discipline == null || discipline.equals(entry.discipline()))
                .map(entry -> new KnowledgeRow(entry.id(), entry.discipline(), entry.titleKey(),
                        entry.descriptionKey(), entry.conditionType(), entry.conditionKey(),
                        state.conditionProgress(entry.conditionKey()), entry.target(),
                        entry.rewardKey(), state.hasKnowledge(entry.number()), entry.unresolved()))
                .toList();
    }
    public static List<ResearchRow> research(ResearchProgressState state, String discipline,
                                             Map<String, Integer> inventory, int rawXp) {
        Set<Integer> unlockedKnowledge = java.util.stream.IntStream.rangeClosed(1,70)
                .filter(state::hasKnowledge).boxed().collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> purchased = ResearchCatalog.allCatalogEntries().stream()
                .filter(definition -> state.researchLevel(definition.id()) > 0)
                .map(ResearchDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return ResearchCatalog.allCatalogEntries().stream()
                .filter(entry -> discipline == null || discipline.equals(entry.discipline()))
                .map(entry -> {
                    int level = state.researchLevel(entry.id());
                    ResearchDefinition.LevelCost next = level < entry.maximumLevel() ? entry.cost(level + 1) : null;
                    ResearchPurchaseRules.Result result = next == null
                            ? ResearchPurchaseRules.Result.MAX_LEVEL
                            : ResearchPurchaseRules.validate(entry, level + 1, level, unlockedKnowledge,
                                    purchased, inventory, rawXp);
                    return new ResearchRow(entry.id(), entry.discipline(), entry.titleKey(),
                            entry.descriptionKey(), level, entry.maximumLevel(), entry.catalogVersion(), entry.tier(), entry.prerequisitesDefined(),
                            entry.knowledgePrerequisites(),
                            entry.researchPrerequisites(), next, result);
                }).toList();
    }
    public static List<QuestRow> quests(ResearchProgressState state, String category) {
        return QuestCatalog.entries().stream()
                .filter(quest -> category == null || category.equals(quest.category()))
                .map(quest -> new QuestRow(quest.id(), quest.category(), quest.titleKey(),
                        quest.descriptionKey(), quest.objectiveKey(), state.questProgress(quest.id()),
                        quest.objectiveTarget(), quest.rewardRawXp(), state.questComplete(quest.id()),
                        state.questClaimed(quest.id()), questGroups(state, quest.id())))
                .toList();
    }

    private static List<QuestGroup> questGroups(ResearchProgressState state, String questId) {
        XpQuestCatalogData.Spec spec = XpQuestCatalogData.catalog().quests().stream()
                .filter(entry -> entry.id().equals(questId)).findFirst().orElse(null);
        if (spec == null) return List.of();
        List<String> encodedGroups = switch (spec.tracker().kind()) {
            case "stat_killed_each" -> List.of("species|" + String.join("|", spec.tracker().entries()));
            case "stat_killed_monsters_distinct" -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(type -> type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
                    .filter(type -> spec.tracker().key().isBlank() || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                            .getKey(type).getNamespace().equals(spec.tracker().key()))
                    .map(type -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())
                    .sorted().collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(), values -> List.of("species|" + String.join("|", values))));
            case "stat_killed_groups_each" -> spec.tracker().entries();
            default -> List.of();
        };
        List<QuestGroup> groups = new java.util.ArrayList<>();
        for (String encoded : encodedGroups) {
            String[] parts = encoded.split("\\|");
            List<QuestTarget> targets = new java.util.ArrayList<>();
            for (int index = 1; index < parts.length; index++) {
                String entityId = parts[index];
                String titleKey = "entity." + entityId.replace(':', '.');
                targets.add(new QuestTarget(entityId, titleKey,
                        state.get("quest.target." + questId + "." + entityId) > 0));
            }
            boolean any = targets.stream().anyMatch(QuestTarget::complete);
            boolean all = !targets.isEmpty() && targets.stream().allMatch(QuestTarget::complete);
            groups.add(new QuestGroup(parts[0], targets, "stat_killed_each".equals(spec.tracker().kind())
                    || "stat_killed_monsters_distinct".equals(spec.tracker().kind()) ? all : any));
        }
        return List.copyOf(groups);
    }
}
