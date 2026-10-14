package dev.itzgamesgd.moreminecrafts.fishing;

public final class FishValue {
    private static final double[] GRADE_MULTIPLIER = {0, 1.00, 1.15, 1.40, 1.80, 2.50};
    private FishValue() {}

    public static double gradeMultiplier(int grade) {
        return GRADE_MULTIPLIER[FishingGrade.requireValid(grade)];
    }

    public static double calculate(FishSpecies species, int grade, int lengthTenthsCm, int weightTenthsKg) {
        double lengthPercentile = species.lengthDistribution().percentile(lengthTenthsCm);
        double weightPercentile = species.weightDistribution().percentile(weightTenthsKg);
        // Length and correlated Weight are one size signal; average their bounded percentiles
        // so correlated values do not double the maximum size bonus.
        double sizeBonus = 0.75 * (lengthPercentile + weightPercentile) / 2.0;
        return species.baseValue() * gradeMultiplier(grade) * (1.0 + sizeBonus);
    }
}
