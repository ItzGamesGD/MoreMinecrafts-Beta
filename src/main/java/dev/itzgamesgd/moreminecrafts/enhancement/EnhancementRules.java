package dev.itzgamesgd.moreminecrafts.enhancement;

/** Locked progression rules and isolated provisional transaction costs shared by all adapters. */
public final class EnhancementRules {
    public static final int ORDINARY_MAX_LEVEL = 20;
    public static final int MILESTONE_INTERVAL = 5;
    public static final boolean DETERMINISTIC_SUCCESS = true;

    private EnhancementRules() {}

    public static boolean isMilestone(int level) {
        return level > 0 && level <= ORDINARY_MAX_LEVEL && level % MILESTONE_INTERVAL == 0;
    }

    public static int milestoneCount(int level) {
        if (level <= 0) return 0;
        return Math.min(level, ORDINARY_MAX_LEVEL) / MILESTONE_INTERVAL;
    }

    /**
     * Approved 2026-10-17 provisional raw-XP table, shared by T1 and T2: +1–5 = 40, +6–10 = 70,
     * +11–15 = 110, +16–20 = 160 (+0→+20 totals 1,900). Milestones cost extra material, not extra XP.
     */
    public static Cost costFor(int targetLevel) {
        if (targetLevel < 1 || targetLevel > ORDINARY_MAX_LEVEL) {
            throw new IllegalArgumentException("Target enhancement level must be between 1 and 20: " + targetLevel);
        }
        int rawXp = targetLevel <= 5 ? 40 : targetLevel <= 10 ? 70 : targetLevel <= 15 ? 110 : 160;
        return new Cost(isMilestone(targetLevel) ? 2 : 1, rawXp);
    }

    public record Cost(int materialCount, int rawXpCost) {}
}
