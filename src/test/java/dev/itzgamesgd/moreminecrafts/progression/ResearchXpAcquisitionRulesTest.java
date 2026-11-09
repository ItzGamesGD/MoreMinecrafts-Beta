package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

final class ResearchXpAcquisitionRulesTest {
    @Test void vanillaXpIsUnchangedWithoutResearchAndFractionsAccumulatePerLevel() {
        assertEquals(new ResearchXpAcquisitionRules.Award(1, 0),
                ResearchXpAcquisitionRules.apply(1, 0, 0));
        int remainder = 0;
        int totalBonus = 0;
        for (int i = 0; i < 100; i++) {
            var award = ResearchXpAcquisitionRules.apply(1, 3, remainder);
            totalBonus += award.totalXp() - 1;
            remainder = award.remainder();
        }
        assertEquals(9, totalBonus);
        assertEquals(0, remainder);
    }

    @Test void noBonusIsCreatedFromEmptyOrNegativeXp() {
        assertEquals(new ResearchXpAcquisitionRules.Award(0, 17),
                ResearchXpAcquisitionRules.apply(0, 3, 17));
        assertEquals(new ResearchXpAcquisitionRules.Award(0, 17),
                ResearchXpAcquisitionRules.apply(-4, 3, 17));
    }

    @Test void perPlayerFractionKeySurvivesProgressionSerialization() {
        String key = "research.xp_fraction.v3.research.64";
        ResearchProgressState original = ResearchProgressState.empty().with(key, 63);
        var encoded = ResearchProgressState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ResearchProgressState restored = ResearchProgressState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(63, restored.get(key));
    }
}
