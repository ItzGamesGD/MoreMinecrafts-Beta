package dev.itzgamesgd.moreminecrafts.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FishingSaleRulesTest {
    @Test
    void salePriceUsesAllFishFieldsAndHasADeclaredCap() {
        FishSpecies species = FishSpecies.BLUE_MARLIN;
        int smallGradeOne = FishingSaleRules.emeraldPrice(species, 1,
                species.lengthDistribution().minimum(), species.weightDistribution().minimum());
        int largeGradeFive = FishingSaleRules.emeraldPrice(species, 5,
                species.lengthDistribution().maximum(), species.weightDistribution().maximum());
        assertEquals(1, smallGradeOne);
        assertEquals(6, largeGradeFive);
        assertTrue(largeGradeFive > smallGradeOne);
        assertTrue(FishingSaleRules.emeraldPrice(FishSpecies.BLUE_MARLIN, 5,
                FishSpecies.BLUE_MARLIN.lengthDistribution().maximum(),
                FishSpecies.BLUE_MARLIN.weightDistribution().maximum()) <= FishingSaleRules.MAX_EMERALDS_PER_FISH);
    }
}
