package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ProgressionPurchasePolicyTest {
    @Test void onlyEligibleResearchWithARealNextCostCanRequestPurchase() {
        assertTrue(ProgressionPurchasePolicy.canRequest(true, true, 0, 1, true,
                ResearchPurchaseRules.Result.PURCHASED));
        assertFalse(ProgressionPurchasePolicy.canRequest(false, true, 0, 1, true,
                ResearchPurchaseRules.Result.PURCHASED));
        assertFalse(ProgressionPurchasePolicy.canRequest(true, false, 0, 1, true,
                ResearchPurchaseRules.Result.PURCHASED));
        assertFalse(ProgressionPurchasePolicy.canRequest(true, true, 0, 1, false,
                ResearchPurchaseRules.Result.PURCHASED));
        assertFalse(ProgressionPurchasePolicy.canRequest(true, true, 1, 1, false,
                ResearchPurchaseRules.Result.MAX_LEVEL));
    }

    @Test void unmetKnowledgeMaterialsXpOrResearchPrerequisitesCannotRequestPurchase() {
        for (var reason : new ResearchPurchaseRules.Result[]{ResearchPurchaseRules.Result.MISSING_KNOWLEDGE,
                ResearchPurchaseRules.Result.MISSING_PREREQUISITE, ResearchPurchaseRules.Result.MISSING_MATERIALS,
                ResearchPurchaseRules.Result.INSUFFICIENT_XP, ResearchPurchaseRules.Result.ALREADY_PURCHASED,
                ResearchPurchaseRules.Result.INVALID_STATE})
            assertFalse(ProgressionPurchasePolicy.canRequest(true, true, 0, 1, true, reason), reason.name());
    }
}
