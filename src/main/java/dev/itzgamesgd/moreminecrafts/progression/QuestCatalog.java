package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registry for approved quest content. No new quest definitions are shipped pending approval. */
public final class QuestCatalog {
    private static final Map<String, QuestDefinition> QUESTS = new ConcurrentHashMap<>();
    private QuestCatalog() {}
    public static void register(QuestDefinition definition) {
        if (QUESTS.putIfAbsent(definition.id(), definition) != null)
            throw new IllegalArgumentException("Duplicate quest id " + definition.id());
    }
    public static QuestDefinition byId(String id) { return QUESTS.get(id); }
    public static Collection<QuestDefinition> entries() {
        return QUESTS.values().stream().sorted(java.util.Comparator.comparing(QuestDefinition::id)).toList();
    }
}
