package dev.itzgamesgd.moreminecrafts.progression;

/** Pure quest-state transitions shared by server runtime and tests. */
public final class QuestRules {
    private QuestRules() {}
    public static ResearchProgressState addProgress(ResearchProgressState state, QuestDefinition quest, int amount) {
        if (state.questClaimed(quest.id()) || amount <= 0) return state;
        int next = (int) Math.min(quest.objectiveTarget(), (long) state.questProgress(quest.id()) + amount);
        ResearchProgressState updated = state.withQuestProgress(quest.id(), next);
        return next >= quest.objectiveTarget() ? updated.completeQuest(quest.id()) : updated;
    }
    /**
     * Monotonic evidence-based progress: never lowers stored progress, never touches claimed quests,
     * and completes once the observed value reaches the target. Completion is never revoked.
     */
    public static ResearchProgressState raiseProgress(ResearchProgressState state, QuestDefinition quest, long observed) {
        String id = quest.id();
        if (state.questClaimed(id)) return state;
        int current = state.questProgress(id);
        int next = (int) Math.max(current, Math.min(quest.objectiveTarget(), Math.max(0, observed)));
        ResearchProgressState updated = next != current ? state.withQuestProgress(id, next) : state;
        return next >= quest.objectiveTarget() && !updated.questComplete(id) ? updated.completeQuest(id) : updated;
    }
    /** Records one distinct entry under {@code quest.seen.<questId>.<entry>}; repeated entries are ignored. */
    public static ResearchProgressState observeDistinct(ResearchProgressState state, String questId, String entry) {
        if (entry == null || entry.isBlank()) return state;
        String key = seenKey(questId, entry);
        return state.get(key) != 0 ? state : state.with(key, 1);
    }
    public static int distinctCount(ResearchProgressState state, String questId) {
        String prefix = "quest.seen." + questId + ".";
        return (int) state.counters().entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) && e.getValue() != 0).count();
    }
    static String seenKey(String questId, String entry) { return "quest.seen." + questId + "." + entry; }
    /** Uncapped counter that saturates at Integer.MAX_VALUE instead of overflowing. */
    public static ResearchProgressState addCounter(ResearchProgressState state, String key, long amount) {
        if (amount <= 0) return state;
        String stored = counterKey(key);
        int current = Math.max(0, state.get(stored));
        int next = amount >= Integer.MAX_VALUE - current ? Integer.MAX_VALUE : current + (int) amount;
        return next == current ? state : state.with(stored, next);
    }
    /** Seeds a counter from a reliable lower bound (e.g. a capped Knowledge counter) without lowering it. */
    public static ResearchProgressState seedCounter(ResearchProgressState state, String key, int lowerBound) {
        String stored = counterKey(key);
        return lowerBound > state.get(stored) ? state.with(stored, lowerBound) : state;
    }
    public static int counter(ResearchProgressState state, String key) { return Math.max(0, state.get(counterKey(key))); }
    static String counterKey(String key) { return "quest.counter." + key; }
    public static boolean canClaim(ResearchProgressState state, QuestDefinition quest) {
        return state.questComplete(quest.id()) && !state.questClaimed(quest.id());
    }
    public static ResearchProgressState claim(ResearchProgressState state, QuestDefinition quest) {
        return canClaim(state, quest) ? state.claimQuest(quest.id()) : state;
    }
}
