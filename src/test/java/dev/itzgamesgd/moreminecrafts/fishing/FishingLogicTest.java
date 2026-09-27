package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.Arrays;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingLogicTest {
    @Test void higherReelsIncludeLowerSpeciesWithoutChangingWeights() {
        var one = FishingLogic.accessibleSpecies(1);
        var three = FishingLogic.accessibleSpecies(3);
        assertEquals(4, one.size());
        assertEquals(12, three.size());
        assertTrue(three.containsAll(one));
        assertEquals(FishSpecies.CRUCIAN_CARP.encounterWeight(), one.getFirst().encounterWeight());
        assertEquals(FishSpecies.CRUCIAN_CARP.encounterWeight(), three.getFirst().encounterWeight());
    }

    @Test void minnowDoesNotFilterEncounterPoolAndValidationComesAfterward() {
        var pool = FishingLogic.accessibleSpecies(3);
        assertTrue(pool.contains(FishSpecies.BLUE_MARLIN));
        assertFalse(FishingLogic.canCapture(FishSpecies.BLUE_MARLIN, 0));
        assertFalse(FishingLogic.canCapture(FishSpecies.BLUE_MARLIN, 2));
        assertTrue(FishingLogic.canCapture(FishSpecies.BLUE_MARLIN, 3));
    }

    @Test void exactCrankbaitDistributionsAreComplete() {
        assertArrayEquals(new int[]{45,30,16,7,2}, FishingLogic.gradeWeights(0));
        assertArrayEquals(new int[]{30,32,22,12,4}, FishingLogic.gradeWeights(1));
        assertArrayEquals(new int[]{10,25,32,24,9}, FishingLogic.gradeWeights(2));
        assertArrayEquals(new int[]{0,8,34,38,20}, FishingLogic.gradeWeights(3));
        for (int tier=0;tier<=3;tier++) assertEquals(100, Arrays.stream(FishingLogic.gradeWeights(tier)).sum());
        assertEquals(0, FishingLogic.gradeWeights(3)[0]);
    }

    @Test void measurementsAreSpeciesSpecificBoundedAndIndependentFromGrade() {
        assertNotEquals(FishSpecies.CRUCIAN_CARP.lengthDistribution(), FishSpecies.SWORDFISH.lengthDistribution());
        assertNotEquals(FishSpecies.CRUCIAN_CARP.weightDistribution(), FishSpecies.SWORDFISH.weightDistribution());
        for (FishSpecies species : FishSpecies.values()) for (int seed=0;seed<100;seed++) {
            int length = FishingLogic.rollLength(species, RandomSource.create(seed));
            int weight = FishingLogic.rollWeight(species, RandomSource.create(seed + 1000));
            assertTrue(length >= species.lengthDistribution().minimum() && length <= species.lengthDistribution().maximum());
            assertTrue(weight >= species.weightDistribution().minimum() && weight <= species.weightDistribution().maximum());
            // Neither API accepts Grade: the identical seed gives identical measurements for any eventual Grade.
            assertEquals(length, FishingLogic.rollLength(species, RandomSource.create(seed)));
            assertEquals(weight, FishingLogic.rollWeight(species, RandomSource.create(seed + 1000)));
        }
    }

    @Test void valueUsesGradeAndAdditiveSpeciesRelativePercentiles() {
        FishSpecies species = FishSpecies.COMMON_CARP;
        int minL=species.lengthDistribution().minimum(), maxL=species.lengthDistribution().maximum();
        int minW=species.weightDistribution().minimum(), maxW=species.weightDistribution().maximum();
        assertEquals(species.baseValue(), FishValue.calculate(species,1,minL,minW), 0.0001);
        assertEquals(species.baseValue()*2.5, FishValue.calculate(species,5,minL,minW), 0.0001);
        assertEquals(species.baseValue()*2.5, FishValue.calculate(species,1,maxL,maxW), 0.0001);
    }
}
