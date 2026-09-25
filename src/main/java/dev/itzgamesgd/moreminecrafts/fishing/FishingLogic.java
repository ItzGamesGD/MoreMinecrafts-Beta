package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Provisional three-species/three-grade live-test sampler.
 * Reel controls eligibility, Minnow only species weighting, and Crankbait only grade weighting.
 */
public final class FishingLogic {
    public static final List<FishSpecies> LIVE_TEST_SPECIES =
            List.of(FishSpecies.BLUEGILL, FishSpecies.TROUT, FishSpecies.PIKE);
    public static final int LIVE_TEST_GRADE_COUNT = 3;

    private FishingLogic() {}

    public static int accessibleTier(int measuredDepth, int reelGrade) {
        FishingGrade.requireValid(reelGrade);
        return Math.min(Math.max(1, measuredDepth), Math.min(3, reelGrade));
    }

    public static List<FishSpecies> eligible(int measuredDepth, int reelGrade) {
        int tier = accessibleTier(measuredDepth, reelGrade);
        return LIVE_TEST_SPECIES.subList(0, tier);
    }

    /** Provisional monotonic rarity weight; it cannot add a species to the supplied pool. */
    public static int speciesWeight(FishSpecies species, int minnowGrade) {
        FishingGrade.requireValid(minnowGrade);
        int rarity = LIVE_TEST_SPECIES.indexOf(species);
        if (rarity < 0) throw new IllegalArgumentException("Species is not in live-test pool: " + species);
        return species.weight() * (100 + rarity * (minnowGrade - 1) * 25) / 100;
    }

    /** Provisional grade weighting, deliberately independent of species. */
    public static int gradeWeight(int grade, int crankbaitGrade) {
        if (grade < 1 || grade > LIVE_TEST_GRADE_COUNT) throw new IllegalArgumentException("Grade must be 1-3");
        FishingGrade.requireValid(crankbaitGrade);
        int base = switch (grade) { case 1 -> 100; case 2 -> 45; case 3 -> 15; default -> throw new AssertionError(); };
        return base * (100 + (grade - 1) * (crankbaitGrade - 1) * 20) / 100;
    }

    public static CatchRoll select(List<FishSpecies> eligible, int minnowGrade, int crankbaitGrade,
                                   RandomSource random) {
        if (eligible.isEmpty()) throw new IllegalArgumentException("No eligible fish");
        record Weighted(FishSpecies species, int grade, int weight) {}
        List<Weighted> values = new ArrayList<>();
        int total = 0;
        for (FishSpecies species : eligible) {
            for (int grade = 1; grade <= LIVE_TEST_GRADE_COUNT; grade++) {
                int weight = speciesWeight(species, minnowGrade) * gradeWeight(grade, crankbaitGrade);
                values.add(new Weighted(species, grade, weight));
                total += weight;
            }
        }
        int roll = random.nextInt(total);
        for (Weighted value : values) {
            if (roll < value.weight()) {
                int length = value.species().lengthFromRoll(random.nextInt());
                return new CatchRoll(value.species(), value.grade(), length);
            }
            roll -= value.weight();
        }
        throw new IllegalStateException("Weighted selection exhausted");
    }

    public record CatchRoll(FishSpecies species, int grade, int lengthCm) {}
}
