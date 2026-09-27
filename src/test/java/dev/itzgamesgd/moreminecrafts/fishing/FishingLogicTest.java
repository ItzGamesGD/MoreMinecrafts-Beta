package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.Arrays;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FishingLogicTest {
    @Test
    void higherReelsIncludeLowerSpeciesWithoutChangingWeights() {
        var one = FishingLogic.accessibleSpecies(1);
        var three = FishingLogic.accessibleSpecies(3);
        assertEquals(4, one.size());
        assertEquals(12, three.size());
        assertTrue(three.containsAll(one));
        assertEquals(FishSpecies.CRUCIAN_CARP.encounterWeight(), one.getFirst().encounterWeight());
        assertEquals(FishSpecies.CRUCIAN_CARP.encounterWeight(), three.getFirst().encounterWeight());
    }

    @Test
    void insufficientMinnowDoesNotBlockEncounterButDeniesCapture() {
        var pool = FishingLogic.accessibleSpecies(3);
        assertTrue(pool.contains(FishSpecies.BLUE_MARLIN));
        assertFalse(FishingLogic.canCapture(FishSpecies.BLUE_MARLIN, 0));
        assertFalse(FishingLogic.canCapture(FishSpecies.BLUE_MARLIN, 2));
        assertTrue(FishingLogic.canCapture(FishSpecies.BLUE_MARLIN, 3));
    }

    @Test
    void exactCrankbaitDistributionsAreComplete() {
        assertArrayEquals(new int[]{45, 30, 16, 7, 2}, FishingLogic.gradeWeights(0));
        assertArrayEquals(new int[]{30, 32, 22, 12, 4}, FishingLogic.gradeWeights(1));
        assertArrayEquals(new int[]{10, 25, 32, 24, 9}, FishingLogic.gradeWeights(2));
        assertArrayEquals(new int[]{0, 8, 34, 38, 20}, FishingLogic.gradeWeights(3));
        for (int tier = 0; tier <= 3; tier++) {
            assertEquals(100, Arrays.stream(FishingLogic.gradeWeights(tier)).sum());
        }
    }

    @Test
    void expectedWeightIsSpeciesSpecificMonotonicAndBounded() {
        for (FishSpecies species : FishSpecies.values()) {
            int minLength = species.lengthDistribution().minimum();
            int maxLength = species.lengthDistribution().maximum();
            int minWeight = species.weightDistribution().minimum();
            int maxWeight = species.weightDistribution().maximum();
            int previous = minWeight;
            for (int length = minLength; length <= maxLength; length++) {
                int expected = species.expectedWeightForLength(length);
                assertTrue(expected >= previous, species.id());
                assertTrue(expected >= minWeight && expected <= maxWeight);
                previous = expected;
            }
            assertEquals(minWeight, species.expectedWeightForLength(minLength));
            assertEquals(maxWeight, species.expectedWeightForLength(maxLength));
        }
    }

    @Test
    void sameLengthAllowsBoundedIndividualVariation() {
        for (FishSpecies species : FishSpecies.values()) {
            int length = species.lengthDistribution().minimum()
                    + (species.lengthDistribution().maximum() - species.lengthDistribution().minimum()) / 2;
            int light = species.weightForLength(length, -1.0);
            int heavy = species.weightForLength(length, 1.0);
            assertTrue(light >= species.weightDistribution().minimum());
            assertTrue(heavy <= species.weightDistribution().maximum());
            assertTrue(heavy > light, species.id());
        }
    }

    @Test
    void randomSizeRollsStayBoundedAndIgnoreGradeAndModules() {
        for (FishSpecies species : FishSpecies.values()) {
            for (int seed = 0; seed < 100; seed++) {
                int length = FishingLogic.rollLength(species, RandomSource.create(seed));
                int weight = FishingLogic.rollWeight(species, length, RandomSource.create(seed + 1000));
                assertTrue(length >= species.lengthDistribution().minimum());
                assertTrue(length <= species.lengthDistribution().maximum());
                assertTrue(weight >= species.weightDistribution().minimum());
                assertTrue(weight <= species.weightDistribution().maximum());

                for (int grade = 1; grade <= FishingGrade.MAX; grade++) {
                    for (int tier = 0; tier <= FishingEquipmentTier.MAX; tier++) {
                        assertEquals(length, FishingLogic.rollLength(species, RandomSource.create(seed)));
                        assertEquals(weight,
                                FishingLogic.rollWeight(species, length, RandomSource.create(seed + 1000)));
                    }
                }
            }
        }
    }

    @Test
    void gradeMultipliersAndSizeValueRemainBounded() {
        FishSpecies species = FishSpecies.COMMON_CARP;
        double[] multipliers = {0, 1.00, 1.15, 1.40, 1.80, 2.50};
        for (int grade = 1; grade <= FishingGrade.MAX; grade++) {
            assertEquals(multipliers[grade], FishValue.gradeMultiplier(grade), 0.0001);
        }
        int minLength = species.lengthDistribution().minimum();
        int maxLength = species.lengthDistribution().maximum();
        int minWeight = species.weightDistribution().minimum();
        int maxWeight = species.weightDistribution().maximum();
        assertEquals(species.baseValue(), FishValue.calculate(species, 1, minLength, minWeight), 0.0001);
        assertEquals(species.baseValue() * 2.5, FishValue.calculate(species, 5, minLength, minWeight), 0.0001);
        assertEquals(species.baseValue() * 1.75, FishValue.calculate(species, 1, maxLength, maxWeight), 0.0001);
        assertEquals(species.baseValue() * 2.5 * 1.75,
                FishValue.calculate(species, 5, maxLength, maxWeight), 0.0001);
    }
}
