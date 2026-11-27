package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Pure research predicates over owned research levels (policy 6: owned value, not purchase history). */
final class XpQuestResearchValue {
    private XpQuestResearchValue() {}

    static long value(ResearchProgressState state, XpQuestCatalogData.Tracker tracker) {
        return value(state, tracker, ResearchCatalog.allCatalogEntries());
    }

    static long value(ResearchProgressState state, XpQuestCatalogData.Tracker tracker, List<ResearchDefinition> catalog) {
        return switch (tracker.kind()) {
            case "research_nodes" -> catalog.stream()
                    .filter(d -> !"non_root".equals(tracker.key()) || !isRoot(d))
                    .filter(d -> owned(state, d) > 0).count();
            case "research_roots" -> catalog.stream().filter(XpQuestResearchValue::isRoot)
                    .filter(d -> owned(state, d) > 0).count();
            case "research_tier" -> catalog.stream().filter(d -> tracker.key().equals(d.tier()))
                    .filter(d -> owned(state, d) > 0).count();
            case "research_breadth" -> catalog.stream().filter(d -> !isRoot(d)).filter(d -> owned(state, d) > 0)
                    .map(ResearchDefinition::discipline).distinct().count();
            case "research_discipline_complete" -> {
                Map<String, List<ResearchDefinition>> byDiscipline = catalog.stream()
                        .collect(Collectors.groupingBy(ResearchDefinition::discipline));
                yield byDiscipline.values().stream().anyMatch(defs -> defs.stream()
                        .allMatch(d -> owned(state, d) >= d.maximumLevel())) ? 1 : 0;
            }
            case "research_value" -> {
                long sum = 0;
                for (ResearchDefinition d : catalog)
                    for (int level = 1; level <= owned(state, d); level++) sum += d.cost(level).rawXp();
                yield sum;
            }
            default -> throw new IllegalArgumentException("Not a research tracker: " + tracker.kind());
        };
    }

    static boolean isRoot(ResearchDefinition definition) { return definition.id().endsWith("_root"); }

    private static int owned(ResearchProgressState state, ResearchDefinition definition) {
        return Math.min(state.researchLevel(definition.id()), definition.maximumLevel());
    }
}
