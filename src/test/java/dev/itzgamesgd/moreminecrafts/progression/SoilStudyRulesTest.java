package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SoilStudyRulesTest {
    @Test void trampleResistanceNeedsAtLeastLevelOne() {
        assertFalse(SoilStudyRules.tramplingResisted(0));
        assertTrue(SoilStudyRules.tramplingResisted(1));
        assertTrue(SoilStudyRules.tramplingResisted(3));
    }

    @Test void hydrationReachIsSixOnlyAtLevelThree() {
        assertEquals(4, SoilStudyRules.hydrationReach(0));
        assertEquals(4, SoilStudyRules.hydrationReach(2));
        assertEquals(6, SoilStudyRules.hydrationReach(3));
    }

    @Test void seedYieldIsThirtyPercentOfEligibleSeedsFromLevelTwo() {
        assertEquals(0, SoilStudyRules.seedYieldBonus(3, 1, 0.0D));
        assertEquals(0, SoilStudyRules.seedYieldBonus(0, 2, 0.0D));
        assertEquals(1, SoilStudyRules.seedYieldBonus(3, 2, 0.8999D)); // 0.9 seeds: 90% for one
        assertEquals(0, SoilStudyRules.seedYieldBonus(3, 2, 0.9D));
        assertEquals(1, SoilStudyRules.seedYieldBonus(4, 2, 0.9999D)); // 1.2 seeds: one guaranteed
        assertEquals(2, SoilStudyRules.seedYieldBonus(4, 2, 0.1999D));
        assertEquals(3, SoilStudyRules.seedYieldBonus(10, 3, 0.0D));
        assertEquals(3, SoilStudyRules.seedYieldBonus(10, 3, 0.9999D));
    }

    @Test void growthBonusIsTwentyPercentRelativeAndNeverAnUnconditionalStage() {
        assertEquals(0.0D, SoilStudyRules.growthRelativeBonus(2));
        assertEquals(0.20D, SoilStudyRules.growthRelativeBonus(3));
        double base = 1.0D / 7.0D;
        assertEquals(base * 1.2D, ResearchPassiveMath.multipliedProbability(base, 0.20D), 1.0e-12);
        double extra = ResearchPassiveMath.conditionalExtraProbability(base, 0.20D);
        assertTrue(extra > 0.0D && extra < 1.0D);
        assertEquals(base * 1.2D, base + (1.0D - base) * extra, 1.0e-12);
        assertEquals(1.0D, base + (1.0D - base) * ResearchPassiveMath.conditionalExtraProbability(0.9D, 0.20D),
                1.0e-12);
    }

    @Test void independentBonusesComposeOnceAndSingleBonusIsExact() {
        assertEquals(0.0D, SoilStudyRules.composeRelativeBonuses(0.0D, 0.0D));
        assertEquals(0.01D, SoilStudyRules.composeRelativeBonuses(0.01D, 0.0D));
        assertEquals(0.20D, SoilStudyRules.composeRelativeBonuses(0.0D, 0.20D));
        assertEquals(1.01D * 1.2D - 1.0D, SoilStudyRules.composeRelativeBonuses(0.01D, 0.20D), 1.0e-12);
    }

    @Test void cultivatorDataRoundTripsAndEditsAreImmutable() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        FarmlandCultivatorData data = FarmlandCultivatorData.EMPTY.with(5L, a).with(-7L, b).with(5L, b);
        assertEquals(b, data.cultivators().get(5L));
        assertEquals(2, data.cultivators().size());
        assertTrue(FarmlandCultivatorData.EMPTY.isEmpty());
        var json = FarmlandCultivatorData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        assertEquals(data, FarmlandCultivatorData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertSame(data, data.without(99L));
        assertEquals(1, data.without(5L).cultivators().size());
        assertEquals(2, data.cultivators().size());
    }
}
