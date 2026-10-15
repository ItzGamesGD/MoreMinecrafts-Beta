package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.Arrays;
import java.util.List;
import dev.itzgamesgd.moreminecrafts.progression.Research23AEffectRules;
import net.minecraft.util.RandomSource;

/** Pure server-authoritative rolls. Species, grade, length and weight remain separate. */
public final class FishingLogic {
    private FishingLogic() {}

    public static List<FishSpecies> accessibleSpecies(int reelTier) {
        if (reelTier == 0) return List.of();
        FishingEquipmentTier.requireValid(reelTier);
        return Arrays.stream(FishSpecies.values()).filter(s -> s.requiredReelTier() <= reelTier).toList();
    }

    public static FishSpecies selectSpecies(List<FishSpecies> pool, RandomSource random) {
        if (pool.isEmpty()) throw new IllegalArgumentException("No accessible species");
        int total = pool.stream().mapToInt(FishSpecies::encounterWeight).sum();
        int roll = random.nextInt(total);
        for (FishSpecies species : pool) {
            if (roll < species.encounterWeight()) return species;
            roll -= species.encounterWeight();
        }
        throw new IllegalStateException("Weighted species selection overflow");
    }

    public static boolean canCapture(FishSpecies species, int minnowTier) {
        return minnowTier >= species.requiredMinnowTier();
    }

    /** crankbaitTier zero means no active crankbait and uses the natural distribution. */
    public static int[] gradeWeights(int crankbaitTier) {
        return gradeWeights(crankbaitTier, 0);
    }

    /** Fisheries Study changes only grade odds; a zero research level preserves the old table. */
    public static int[] gradeWeights(int crankbaitTier, int fisheriesStudyLevel) {
        return Research23AEffectRules.fishGradeWeights(crankbaitTier, fisheriesStudyLevel);
    }

    public static int rollGrade(int crankbaitTier, RandomSource random) {
        return rollGrade(crankbaitTier, 0, random);
    }

    public static int rollGrade(int crankbaitTier, int fisheriesStudyLevel, RandomSource random) {
        int roll = random.nextInt(100);
        int[] weights = gradeWeights(crankbaitTier, fisheriesStudyLevel);
        for (int i = 0; i < weights.length; i++) {
            if (roll < weights[i]) return i + 1;
            roll -= weights[i];
        }
        throw new IllegalStateException("Grade distribution must sum to 100");
    }

    public static int rollLength(FishSpecies species, RandomSource random) {
        return species.lengthDistribution().roll(random);
    }

    public static int rollWeight(FishSpecies species, int lengthTenthsCm, RandomSource random) {
        return species.weightForLength(lengthTenthsCm, random.nextDouble() * 2.0 - 1.0);
    }

    /** Rolls every per-fish field from the caller's server RNG without reusing an ItemStack. */
    public static FishRoll rollFish(FishSpecies species, int crankbaitTier, RandomSource random) {
        return rollFish(species, crankbaitTier, 0, random);
    }

    public static FishRoll rollFish(FishSpecies species, int crankbaitTier, int fisheriesStudyLevel,
            RandomSource random) {
        int grade = rollGrade(crankbaitTier, fisheriesStudyLevel, random);
        int length = rollLength(species, random);
        int weight = rollWeight(species, length, random);
        return new FishRoll(species, grade, length, weight);
    }

    /** Selects a new species before rolling its independent quality and measurements. */
    public static FishRoll rollFish(List<FishSpecies> pool, int crankbaitTier, RandomSource random) {
        return rollFish(selectSpecies(pool, random), crankbaitTier, random);
    }

    public static FishRoll rollFish(List<FishSpecies> pool, int crankbaitTier, int fisheriesStudyLevel,
            RandomSource random) {
        return rollFish(selectSpecies(pool, random), crankbaitTier, fisheriesStudyLevel, random);
    }

    public record FishRoll(FishSpecies species, int grade, int lengthTenthsCm, int weightTenthsKg) {}
}
