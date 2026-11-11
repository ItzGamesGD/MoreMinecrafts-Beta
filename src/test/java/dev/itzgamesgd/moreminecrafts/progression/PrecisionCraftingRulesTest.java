package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrecisionCraftingRulesTest {
    @Test
    void everyPaidLevelUsesTheApprovedClassChancesAndLinearIntermediateLevels() {
        int[][] expected = {
                {0, 100, 150, 200, 250, 300},
                {0, 70, 105, 140, 175, 210},
                {0, 40, 60, 80, 100, 120},
                {0, 20, 30, 40, 50, 60}
        };
        var classes = PrecisionCraftingRules.MaterialClass.values();
        for (int classIndex = 0; classIndex < classes.length; classIndex++) {
            for (int level = 0; level <= PrecisionCraftingRules.MAX_LEVEL; level++) {
                assertEquals(expected[classIndex][level], classes[classIndex].chancePermille(level),
                        classes[classIndex] + " level " + level);
            }
        }
    }

    @Test
    void exactProbabilityBoundariesAreInclusiveBelowAndExclusiveAtTheThreshold() {
        for (var materialClass : PrecisionCraftingRules.MaterialClass.values()) {
            for (int level = 0; level <= PrecisionCraftingRules.MAX_LEVEL; level++) {
                int chance = materialClass.chancePermille(level);
                assertFalse(PrecisionCraftingRules.succeeds(materialClass, level, chance),
                        materialClass + " level " + level + " fails at its threshold");
                if (chance > 0) {
                    assertTrue(PrecisionCraftingRules.succeeds(materialClass, level, chance - 1),
                            materialClass + " level " + level + " succeeds immediately below its threshold");
                }
            }
        }
        assertFalse(PrecisionCraftingRules.succeeds(null, 5, 0), "unclassified inputs are never preserved");
    }

    @Test
    void selectionUsesRarestClassThenLowestSlotIndexAndReturnsAtMostOneSlot() {
        var slots = Arrays.asList(
                PrecisionCraftingRules.MaterialClass.ORDINARY,
                PrecisionCraftingRules.MaterialClass.PRECIOUS,
                PrecisionCraftingRules.MaterialClass.PROCESSED,
                PrecisionCraftingRules.MaterialClass.RARE,
                PrecisionCraftingRules.MaterialClass.RARE,
                null);
        assertEquals(3, PrecisionCraftingRules.selectedSlot(slots));
        assertEquals(1, PrecisionCraftingRules.selectedSlot(List.of(
                PrecisionCraftingRules.MaterialClass.ORDINARY,
                PrecisionCraftingRules.MaterialClass.PRECIOUS,
                PrecisionCraftingRules.MaterialClass.PROCESSED)));
        assertEquals(-1, PrecisionCraftingRules.selectedSlot(Arrays.asList(null, null)));
    }

    @Test
    void purchaseLevelIsReadFromResearchStateAndCappedAtTheNodeMaximum() {
        assertEquals(0, PrecisionCraftingRuntime.purchasedLevel(ResearchProgressState.empty()));
        assertEquals(1, PrecisionCraftingRuntime.purchasedLevel(ResearchProgressState.empty()
                .with("research.v3.research.08", 1)));
        assertEquals(5, PrecisionCraftingRuntime.purchasedLevel(ResearchProgressState.empty()
                .with("research.v3.research.08", 9)));
    }

    @Test
    void rollValuesOutsideTheDeterministicRangeAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PrecisionCraftingRules.succeeds(PrecisionCraftingRules.MaterialClass.ORDINARY, 5, -1));
        assertThrows(IllegalArgumentException.class,
                () -> PrecisionCraftingRules.succeeds(PrecisionCraftingRules.MaterialClass.ORDINARY, 5, 1000));
    }
}
