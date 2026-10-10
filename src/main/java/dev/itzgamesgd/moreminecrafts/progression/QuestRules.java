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
    public static boolean canClaim(ResearchProgressState state, QuestDefinition quest) {
        return state.questComplete(quest.id()) && !state.questClaimed(quest.id());
    }
    public static ResearchProgressState claim(ResearchProgressState state, QuestDefinition quest) {
        return canClaim(state, quest) ? state.claimQuest(quest.id()) : state;
    }
}
