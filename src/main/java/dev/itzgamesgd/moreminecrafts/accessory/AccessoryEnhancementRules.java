package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.Optional;

/** Accessory-only enhancement cap and transactional raw-XP/material requirements. */
public final class AccessoryEnhancementRules {
    public static final int MAX_LEVEL = 10;
    public static final boolean DETERMINISTIC_SUCCESS = true;
    public static final int CATALYST_COST_AT_MILESTONE = 1; // provisional quantity per approved authority

    private AccessoryEnhancementRules() {}

    public static Optional<Plan> plan(int currentLevel, int availableStones, int availableCatalysts,
                                      int availableRawExperience) {
        if (currentLevel < 0 || currentLevel >= MAX_LEVEL
                || availableStones < 0 || availableCatalysts < 0 || availableRawExperience < 0) {
            return Optional.empty();
        }
        int targetLevel = currentLevel + 1;
        boolean milestone = targetLevel == 5 || targetLevel == 10;
        int stoneCost = milestone ? 2 : 1;
        int catalystCost = milestone ? CATALYST_COST_AT_MILESTONE : 0;
        int rawExperienceCost = targetLevel <= 5 ? 40 : 70;
        if (availableStones < stoneCost || availableCatalysts < catalystCost
                || availableRawExperience < rawExperienceCost) {
            return Optional.empty();
        }
        return Optional.of(new Plan(targetLevel, rawExperienceCost, stoneCost, catalystCost,
                milestone, DETERMINISTIC_SUCCESS));
    }

    public static int totalRawExperienceCost() { return 5 * 40 + 5 * 70; }
    public static int totalStoneCost() { return 8 + 2 * 2; }
    public static int totalCatalystCost() { return 2 * CATALYST_COST_AT_MILESTONE; }

    public record Plan(int targetLevel, int rawExperienceCost, int stoneCost, int catalystCost,
                       boolean milestone, boolean deterministicSuccess) {}
}
