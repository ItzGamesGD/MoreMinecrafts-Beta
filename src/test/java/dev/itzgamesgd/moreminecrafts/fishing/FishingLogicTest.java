package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingLogicTest {
    @Test void reelAloneControlsEligibility() {
        assertEquals(List.of(FishSpecies.BLUEGILL), FishingLogic.eligible(5, 1));
        assertEquals(List.of(FishSpecies.BLUEGILL, FishSpecies.TROUT), FishingLogic.eligible(5, 2));
        assertEquals(FishingLogic.LIVE_TEST_SPECIES, FishingLogic.eligible(5, 3));
    }
    @Test void minnowChangesRarityWeightNotEligibility() {
        var eligible = FishingLogic.eligible(5, 2);
        assertFalse(eligible.contains(FishSpecies.PIKE));
        assertEquals(FishingLogic.speciesWeight(FishSpecies.BLUEGILL, 1),
                FishingLogic.speciesWeight(FishSpecies.BLUEGILL, 5));
        assertTrue(FishingLogic.speciesWeight(FishSpecies.TROUT, 5)
                > FishingLogic.speciesWeight(FishSpecies.TROUT, 1));
    }
    @Test void crankbaitChangesGradeNotSpeciesEligibility() {
        assertEquals(FishingLogic.eligible(5, 2), FishingLogic.eligible(5, 2));
        assertEquals(FishingLogic.gradeWeight(1, 1), FishingLogic.gradeWeight(1, 5));
        assertTrue(FishingLogic.gradeWeight(3, 5) > FishingLogic.gradeWeight(3, 1));
    }
    @Test void allThreeGradesAndPersistableLengthsAreReachable() {
        boolean[] seen = new boolean[4];
        for (int seed=0; seed<5000; seed++) {
            var roll = FishingLogic.select(FishingLogic.LIVE_TEST_SPECIES, 5, 5, RandomSource.create(seed));
            seen[roll.grade()] = true;
            assertTrue(roll.lengthCm() >= roll.species().minLengthCm());
            assertTrue(roll.lengthCm() <= roll.species().maxLengthCm());
        }
        assertTrue(seen[1] && seen[2] && seen[3]);
    }
}
