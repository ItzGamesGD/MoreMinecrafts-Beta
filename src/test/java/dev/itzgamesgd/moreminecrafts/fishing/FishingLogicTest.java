package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingLogicTest {
    @Test void reelExpandsDepthWithoutChangingExistingWeights() {
        assertEquals(1, FishingLogic.accessibleDepth(5, 1));
        assertEquals(4, FishingLogic.accessibleDepth(5, 4));
        List<FishSpecies> low = FishingLogic.eligible(5, 1, 5);
        List<FishSpecies> high = FishingLogic.eligible(5, 4, 5);
        assertFalse(low.contains(FishSpecies.STURGEON));
        assertTrue(high.contains(FishSpecies.STURGEON));
        assertTrue(high.containsAll(low));
        assertEquals(FishingLogic.select(List.of(FishSpecies.PERCH), 2, 0, RandomSource.create(91)),
                FishingLogic.select(high.stream().filter(s -> s == FishSpecies.PERCH).toList(),
                        2, 0, RandomSource.create(91)));
    }

    @Test void minnowIsHardGateAndNotWeightModifier() {
        assertFalse(FishingLogic.eligible(5, 5, 3).contains(FishSpecies.STURGEON));
        assertTrue(FishingLogic.eligible(5, 5, 4).contains(FishSpecies.STURGEON));
        List<FishSpecies> atFour = FishingLogic.eligible(5, 5, 4);
        List<FishSpecies> atFive = FishingLogic.eligible(5, 5, 5);
        assertTrue(atFour.contains(FishSpecies.PERCH));
        assertTrue(atFive.contains(FishSpecies.PERCH));
        assertEquals(FishingLogic.select(atFour.stream().filter(s -> s == FishSpecies.PERCH).toList(),
                        2, 0, RandomSource.create(17)),
                FishingLogic.select(atFive.stream().filter(s -> s == FishSpecies.PERCH).toList(),
                        2, 0, RandomSource.create(17)));
    }

    @Test void crankbaitChangesUpperGradeRatioButNotEligibility() {
        List<FishSpecies> pool = FishingLogic.eligible(2, 2, 1);
        assertFalse(pool.contains(FishSpecies.STURGEON));
        int lowGrade = FishingLogic.weight(FishSpecies.PERCH, 1, 1, 0);
        int highBefore = FishingLogic.weight(FishSpecies.PERCH, 5, 1, 0);
        int highAfter = FishingLogic.weight(FishSpecies.PERCH, 5, 5, 0);
        assertEquals(lowGrade, FishingLogic.weight(FishSpecies.PERCH, 1, 5, 0));
        assertTrue(highAfter > highBefore);
        assertEquals(FishSpecies.STURGEON.depthTier(), 4);
        assertFalse(FishingLogic.eligible(2, 2, 5).contains(FishSpecies.STURGEON));
        assertEquals(FishingLogic.select(pool, 3, 0, RandomSource.create(42)),
                FishingLogic.select(List.copyOf(pool), 3, 0, RandomSource.create(42)));
    }

    @Test void lengthsAndQualityAreBoundedAndDeterministic() {
        for (FishSpecies species : FishSpecies.values()) {
            for (int roll : new int[] {Integer.MIN_VALUE, 0, 1, Integer.MAX_VALUE}) {
                int length = species.lengthFromRoll(roll);
                assertTrue(length >= species.minLengthCm() && length <= species.maxLengthCm());
                assertEquals(species.quality(3, length), species.quality(3, length));
            }
            assertTrue(species.quality(3, species.maxLengthCm())
                    >= species.quality(3, species.minLengthCm()));
        }
    }

    @Test void exactlyFiveGrades() {
        for (int i = 1; i <= 5; i++) assertEquals(i, FishingGrade.requireValid(i));
        assertThrows(IllegalArgumentException.class, () -> FishingGrade.requireValid(0));
        assertThrows(IllegalArgumentException.class, () -> FishingGrade.requireValid(6));
    }
}
