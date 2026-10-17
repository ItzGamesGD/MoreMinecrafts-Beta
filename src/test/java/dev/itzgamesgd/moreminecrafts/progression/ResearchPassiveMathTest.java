package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ResearchPassiveMathTest {
    @Test void percentXpRetainsRemaindersAndNeverCreatesXpFromZero() {
        var first = ResearchPassiveMath.accumulatePercent(49, 0, 1);
        assertEquals(new ResearchPassiveMath.Fraction(0, 49), first);
        var second = ResearchPassiveMath.accumulatePercent(51, first.remainder(), 1);
        assertEquals(new ResearchPassiveMath.Fraction(1, 0), second);
        assertEquals(new ResearchPassiveMath.Fraction(0, 0), ResearchPassiveMath.accumulatePercent(0, 0, 1));
    }

    @Test void cropBonusIsRelativeCappedAndAddsOnlyTheRequiredFailedRollProbability() {
        assertEquals(0.0101D, ResearchPassiveMath.multipliedProbability(0.01D, 0.01D), 1.0E-12);
        assertEquals(1.0D, ResearchPassiveMath.multipliedProbability(0.999D, 0.01D), 1.0E-12);
        double extra = ResearchPassiveMath.conditionalExtraProbability(1.0D / 3.0D, 0.01D);
        assertEquals((1.0D / 3.0D) * 1.01D, (1.0D / 3.0D) + (2.0D / 3.0D) * extra, 1.0E-12);
    }

    @Test void potionDurationRoundsTicksWithoutTouchingInstantOrInfiniteDurationsAtCallSite() {
        assertEquals(101, ResearchPassiveMath.increasedDuration(100, 1));
        assertEquals(1, ResearchPassiveMath.increasedDuration(1, 1));
        assertEquals(0, ResearchPassiveMath.increasedDuration(0, 1));
    }

    @Test void paidCombatSkillsScaleOnlyByTheirConfiguredLevelValue() {
        assertEquals(106.0D, ResearchPassiveMath.scaleByBonus(100.0D, 0.06D), 1.0E-12);
        assertEquals(0.85D, ResearchPassiveMath.reduceByPercent(1.0D, 0.15D), 1.0E-12);
        assertEquals(0.0D, ResearchPassiveMath.reduceByPercent(1.0D, 1.5D), 1.0E-12);
    }

    @Test void shieldPreservationConsumesAtMostOnePointAndHonorsChanceBoundaries() {
        assertEquals(2, ResearchPassiveMath.preventDurabilityLoss(3, 0.099D, 0.10D));
        assertEquals(3, ResearchPassiveMath.preventDurabilityLoss(3, 0.10D, 0.10D));
        assertEquals(0, ResearchPassiveMath.preventDurabilityLoss(0, 0.0D, 0.30D));
    }
}
