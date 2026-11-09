package dev.itzgamesgd.moreminecrafts.progression;

/** Approved additional-offspring odds for V3 Livestock. Vanilla already creates the first baby. */
public final class ResearchLivestockRules {
    private ResearchLivestockRules() {}

    public static int additionalOffspring(int level, double roll) {
        if (level <= 0 || level > 3 || !Double.isFinite(roll) || roll < 0.0D || roll >= 1.0D) return 0;
        return switch (level) {
            case 1 -> roll < 0.60D ? 1 : 0;
            case 2 -> 1;
            case 3 -> roll < 0.20D ? 2 : 1;
            default -> 0;
        };
    }
}
