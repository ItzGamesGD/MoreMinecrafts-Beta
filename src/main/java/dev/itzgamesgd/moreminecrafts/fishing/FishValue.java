package dev.itzgamesgd.moreminecrafts.fishing;

public final class FishValue {
    private static final double[] GRADE_MULTIPLIER = {0, 1.00, 1.15, 1.40, 1.80, 2.50};
    private FishValue() {}

    public static double gradeMultiplier(int grade) {
        return GRADE_MULTIPLIER[FishingGrade.requireValid(grade)];
    }

    public static double calculate(FishSpecies species, int grade, int lengthTenthsCm, int weightTenthsKg) {
        double lengthBonus = species.lengthDistribution().percentile(lengthTenthsCm) * 0.75;
        double weightBonus = species.weightDistribution().percentile(weightTenthsKg) * 0.75;
        return species.baseValue() * gradeMultiplier(grade) * (1.0 + lengthBonus + weightBonus);
    }
}
