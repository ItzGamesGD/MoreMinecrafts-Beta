package dev.itzgamesgd.moreminecrafts.progression;

/** Pure shared-counter progression rule for Knowledge entries with a common condition key. */
public final class KnowledgeConditionProgress {
    private KnowledgeConditionProgress() {}

    public static int advance(int current, int observedTotal, int sharedMaximumTarget) {
        if (sharedMaximumTarget <= 0 || observedTotal <= 0) return Math.max(0, Math.min(current, sharedMaximumTarget));
        int monotonic = KnowledgeRules.monotonicProgress(Math.max(0, current), observedTotal);
        return Math.min(sharedMaximumTarget, monotonic);
    }
}
