package dev.itzgamesgd.moreminecrafts.progression;

/** Pure monotonic condition rule shared by server event adapters and catalog tests. */
public final class KnowledgeRules {
    private KnowledgeRules() {}
    public static boolean conditionMet(int progress, int target) { return target > 0 && progress >= target; }
    public static int monotonicProgress(int previous, int observed) {
        return Math.max(Math.max(0, previous), observed);
    }
}
