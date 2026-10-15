package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResearchContentRulesTest {
    @Test
    void oreAndLumberLevelsUseTheApprovedNonlinearTable() {
        assertEquals(0.10D, ResearchContentRules.gatheringChance(1));
        assertEquals(0.20D, ResearchContentRules.gatheringChance(2));
        assertEquals(0.20D, ResearchContentRules.gatheringChance(3));
        assertEquals(0.30D, ResearchContentRules.gatheringChance(4));
        assertEquals(0.30D, ResearchContentRules.gatheringChance(5));
        assertEquals(0.0D, ResearchContentRules.gatheringChance(0));
        assertEquals(0.30D, ResearchContentRules.gatheringChance(99));
        assertEquals(1, ResearchContentRules.maximumGatheringExtra(1));
        assertEquals(1, ResearchContentRules.maximumGatheringExtra(2));
        assertEquals(2, ResearchContentRules.maximumGatheringExtra(3));
        assertEquals(2, ResearchContentRules.maximumGatheringExtra(4));
        assertEquals(3, ResearchContentRules.maximumGatheringExtra(5));
    }

    @Test
    void gatherRollAddsUniformOneThroughTheLevelMaximum() {
        assertEquals(0, ResearchContentRules.gatheringExtraCount(3, 0.20D, 0.0D));
        assertEquals(1, ResearchContentRules.gatheringExtraCount(3, 0.199999D, 0.0D));
        assertEquals(2, ResearchContentRules.gatheringExtraCount(3, 0.0D, 0.999999D));
        assertEquals(3, ResearchContentRules.gatheringExtraCount(5, 0.299999D, 0.999999D));
    }

    @Test
    void durabilityConservationIsOneIndependentFivePercentPerLevelCheck() {
        assertEquals(3, ResearchContentRules.preservedMiningDamage(3, 0.10D, 0));
        assertEquals(2, ResearchContentRules.preservedMiningDamage(3, 0.0D, 1));
        assertEquals(2, ResearchContentRules.preservedMiningDamage(3, 0.049999D, 5));
        assertEquals(3, ResearchContentRules.preservedMiningDamage(3, 0.25D, 5));
        assertEquals(0, ResearchContentRules.preservedMiningDamage(1, 0.0D, 5));
        assertEquals(3, ResearchContentRules.preservedMiningDamage(3, 0.0D, 0));
    }

    @Test
    void seedAndHoneycombLevelsUseTheirApprovedChances() {
        assertEquals(0.15D, ResearchContentRules.seedChance(1));
        assertEquals(0.30D, ResearchContentRules.seedChance(2));
        assertEquals(0.45D, ResearchContentRules.seedChance(3));
        assertEquals(0.20D, ResearchContentRules.honeycombChance(1));
        assertEquals(0.40D, ResearchContentRules.honeycombChance(2));
        assertEquals(0.60D, ResearchContentRules.honeycombChance(3));
        assertTrue(ResearchContentRules.extraItemSucceeds(0.599999D, 0.60D));
        assertFalse(ResearchContentRules.extraItemSucceeds(0.60D, 0.60D));
    }

    @Test
    void woodCraftingAddsPerLevelWithoutExceedingOutputStackLimit() {
        assertEquals(4, ResearchContentRules.woodCraftOutputCount(2, 2, 64));
        assertEquals(64, ResearchContentRules.woodCraftOutputCount(63, 2, 64));
        assertEquals(1, ResearchContentRules.woodCraftOutputCount(1, 2, 1));
        assertEquals(2, ResearchContentRules.woodCraftOutputCount(2, 0, 64));
    }

    @Test
    void equipmentMaintenanceAlternatesRepairAndXpBenefits() {
        assertEquals(0, ResearchContentRules.repairBonusPercent(0));
        assertEquals(10, ResearchContentRules.repairBonusPercent(1));
        assertEquals(10, ResearchContentRules.repairBonusPercent(2));
        assertEquals(20, ResearchContentRules.repairBonusPercent(3));
        assertEquals(20, ResearchContentRules.repairBonusPercent(4));
        assertEquals(30, ResearchContentRules.repairBonusPercent(5));
        assertEquals(10, ResearchContentRules.additionalRepairDurability(100, 1, 1));
        assertEquals(40, ResearchContentRules.additionalRepairDurability(100, 2, 3));
        assertEquals(0, ResearchContentRules.additionalRepairDurability(0, 2, 5));
        assertEquals(0.00D, ResearchContentRules.anvilXpDiscount(1));
        assertEquals(0.045D, ResearchContentRules.anvilXpDiscount(2));
        assertEquals(0.045D, ResearchContentRules.anvilXpDiscount(3));
        assertEquals(0.09D, ResearchContentRules.anvilXpDiscount(4));
        assertEquals(0.09D, ResearchContentRules.anvilXpDiscount(5));
        assertEquals(1, ResearchContentRules.discountAnvilLevels(1, 5));
        assertEquals(10, ResearchContentRules.discountAnvilLevels(11, 5));
        assertEquals(1, ResearchContentRules.discountAnvilLevels(1, 2));
        assertEquals(0, ResearchContentRules.discountAnvilLevels(0, 5));
    }

    @Test
    void fishingTechniqueWidensOnlyTheCenteredPerfectZone() {
        assertEquals(0.08D, ResearchContentRules.fishingGreenHalfWidth(0));
        assertEquals(0.09D, ResearchContentRules.fishingGreenHalfWidth(1));
        assertEquals(0.10D, ResearchContentRules.fishingGreenHalfWidth(2));
        assertEquals(0.11D, ResearchContentRules.fishingGreenHalfWidth(3));
        assertTrue(ResearchContentRules.inFishingGreenZone(0.415D, 1));
        assertFalse(ResearchContentRules.inFishingGreenZone(0.40D, 1));
        assertTrue(ResearchContentRules.inFishingGreenZone(0.395D, 3));
        assertFalse(ResearchContentRules.inFishingGreenZone(0.385D, 3));
    }

    @Test
    void purchasedLevelDescriptorsBoundEachNode() {
        assertEquals(5, ResearchRewards.effects("v3.research.01").getFirst().maximumLevel());
        assertEquals(5, ResearchRewards.effects("v3.research.03").getFirst().maximumLevel());
        assertEquals(2, ResearchRewards.effects("v3.research.09").getFirst().maximumLevel());
        assertEquals(5, ResearchRewards.effects("v3.research.10").getFirst().maximumLevel());
        assertEquals(3, ResearchRewards.effects("v3.research.15").getFirst().maximumLevel());
        assertEquals(3, ResearchRewards.effects("v3.research.18").getFirst().maximumLevel());
        assertEquals(3, ResearchRewards.effects("v3.research.23").getFirst().maximumLevel());
        var owned = ResearchProgressState.empty().with("research.v3.research.09", 8);
        assertEquals(2, ResearchRewards.level(owned, "v3.research.09", "wood_processing"));
        assertEquals(0, ResearchRewards.level(ResearchProgressState.empty(), "v3.research.09", "wood_processing"));
    }
}
