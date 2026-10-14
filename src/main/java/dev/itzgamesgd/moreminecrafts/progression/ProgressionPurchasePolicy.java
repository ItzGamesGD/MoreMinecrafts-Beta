package dev.itzgamesgd.moreminecrafts.progression;

/** Client-side request gate only; the server purchase service remains authoritative. */
public final class ProgressionPurchasePolicy {
    private ProgressionPurchasePolicy() {}

    public static boolean canRequest(boolean researchScreen, boolean prerequisitesDefined,
                                     int level, int maximumLevel, boolean hasNextCost,
                                     ResearchPurchaseRules.Result availability) {
        return researchScreen && prerequisitesDefined && level >= 0 && level < maximumLevel
                && hasNextCost && availability == ResearchPurchaseRules.Result.PURCHASED;
    }
}
