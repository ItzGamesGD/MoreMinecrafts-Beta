package dev.itzgamesgd.moreminecrafts.fishing;

public final class FishingGrade {
    public static final int MIN = 1;
    public static final int MAX = 5;
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};
    private FishingGrade() {}
    public static int requireValid(int grade) {
        if (grade < MIN || grade > MAX) throw new IllegalArgumentException("Fish grade must be 1-5");
        return grade;
    }
    public static String roman(int grade) { return ROMAN[requireValid(grade)]; }
}
