package dev.itzgamesgd.moreminecrafts.enhancement;

/** Progression rules shared by every interaction adapter. */
public final class EnhancementRules {
    public static final int MATERIAL_COST = 1;
    public static final boolean DETERMINISTIC_SUCCESS = true;

    private EnhancementRules() {
    }

    public static int experienceLevelCost(int targetLevel) {
        if (targetLevel < 1 || targetLevel > 40) {
            throw new IllegalArgumentException("Target enhancement level must be between 1 and 40: " + targetLevel);
        }
        if (targetLevel <= 5) return 1;
        if (targetLevel <= 10) return 2;
        if (targetLevel <= 20) return 3;
        if (targetLevel <= 30) return 4;
        return 5;
    }
}
