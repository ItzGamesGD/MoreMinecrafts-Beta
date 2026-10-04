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

    /** Existing numeric XP costs: every transaction costs at least 1 point; milestones cost extra material. */
    public static Cost costFor(int targetLevel) {
        if (targetLevel < 1 || targetLevel > ORDINARY_MAX_LEVEL) {
            throw new IllegalArgumentException("Target enhancement level must be between 1 and 20: " + targetLevel);
        }
        return isMilestone(targetLevel) ? new Cost(2, 1) : new Cost(1, 1);
    }

    public record Cost(int materialCount, int experienceCost) {}
}
