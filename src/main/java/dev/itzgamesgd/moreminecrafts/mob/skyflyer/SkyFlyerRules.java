package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

/**
 * Owner-approved numbers for the Sky Spirit and Storm Hunter
 * (docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md section 3). Nothing here may be changed without a design decision.
 * Values the design leaves open live in {@link SkyFlyerTuning} and are marked PROVISIONAL there.
 */
public final class SkyFlyerRules {
    /** Sky Spirit: 3 % chance of exactly one Sky Feather; Looting does not apply. */
    public static final float SPIRIT_FEATHER_CHANCE = 0.03F;
    public static final int SPIRIT_FEATHER_COUNT = 1;

    /** Storm Hunter: 6 % success roll; on success 1 feather 80 % / 2 feathers 20 %; Looting does not apply. */
    public static final float HUNTER_FEATHER_SUCCESS_CHANCE = 0.06F;
    public static final int HUNTER_ONE_FEATHER_WEIGHT = 80;
    public static final int HUNTER_TWO_FEATHER_WEIGHT = 20;

    /** Overall drop distribution implied by the hunter numbers: 0 = 94 %, 1 = 4.8 %, 2 = 1.2 %. */
    public static double hunterFeatherProbability(int count) {
        double success = HUNTER_FEATHER_SUCCESS_CHANCE;
        double total = HUNTER_ONE_FEATHER_WEIGHT + HUNTER_TWO_FEATHER_WEIGHT;
        return switch (count) {
            case 0 -> 1.0D - success;
            case 1 -> success * HUNTER_ONE_FEATHER_WEIGHT / total;
            case 2 -> success * HUNTER_TWO_FEATHER_WEIGHT / total;
            default -> 0.0D;
        };
    }

    private SkyFlyerRules() {}
}
