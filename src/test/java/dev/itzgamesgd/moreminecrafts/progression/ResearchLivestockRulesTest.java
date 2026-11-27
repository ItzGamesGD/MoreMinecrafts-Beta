package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class ResearchLivestockRulesTest {
    @Test void approvedLevelsAddOnlyTheSpecifiedNumberOfBabies() {
        assertEquals(0, ResearchLivestockRules.additionalOffspring(0, 0.0D));
        assertEquals(1, ResearchLivestockRules.additionalOffspring(1, 0.599D));
        assertEquals(0, ResearchLivestockRules.additionalOffspring(1, 0.60D));
        assertEquals(1, ResearchLivestockRules.additionalOffspring(2, 0.999D));
        assertEquals(2, ResearchLivestockRules.additionalOffspring(3, 0.199D));
        assertEquals(1, ResearchLivestockRules.additionalOffspring(3, 0.20D));
        assertEquals(0, ResearchLivestockRules.additionalOffspring(4, 0.0D));
    }

    @Test void invalidRollsCannotCreateAnOffspring() {
        assertEquals(0, ResearchLivestockRules.additionalOffspring(3, Double.NaN));
        assertEquals(0, ResearchLivestockRules.additionalOffspring(3, 1.0D));
    }
}
