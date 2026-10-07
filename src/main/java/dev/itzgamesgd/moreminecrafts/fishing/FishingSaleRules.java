package dev.itzgamesgd.moreminecrafts.fishing;

/** Provisional conversion at the Fisherman economy boundary: 100 fish-value points per Emerald. */
public final class FishingSaleRules {
    public static final int VALUE_PER_EMERALD = 100;
    public static final int MAX_EMERALDS_PER_FISH = 8;

    private FishingSaleRules() {}

    public static int emeraldPrice(FishSpecies species, int grade, int lengthTenthsCm, int weightTenthsKg) {
        int value = (int) Math.floor(FishValue.calculate(species, grade, lengthTenthsCm, weightTenthsKg));
        return Math.clamp(value / VALUE_PER_EMERALD, 1, MAX_EMERALDS_PER_FISH);
    }
}
