package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.equipment.LowerEquipmentRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResearchStage23BHandlerRulesTest {
    @Test
    void researchRewardsRegisterStage1AndStage2Effects() {
        assertEquals("auto_farm_operation", ResearchRewards.effects("v3.research.24").getFirst().id());
        assertEquals("fish_trap_interval", ResearchRewards.effects("v3.research.26").getFirst().id());
        assertEquals("slime_split_prevention", ResearchRewards.effects("v3.research.41").getFirst().id());
        assertEquals("villager_trading", ResearchRewards.effects("v3.research.53").getFirst().id());
        assertEquals("brewing_speed", ResearchRewards.effects("v3.research.59").getFirst().id());
        assertEquals("potion_study", ResearchRewards.effects("v3.research.60").getFirst().id());
        assertEquals("advanced_brewing_fuel", ResearchRewards.effects("v3.research.66").getFirst().id());
    }

    @Test
    void blockEntityAttributionPrecedenceAndCodecRoundTrip() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        var empty = BlockEntityResearchAttribution.State.empty();
        assertTrue(empty.attributedPlayer().isEmpty());
        assertFalse(empty.actualInteraction());

        var opened = empty.openedBy(first);
        assertEquals(first, opened.attributedPlayer().orElseThrow());
        assertFalse(opened.actualInteraction());

        var reopened = opened.openedBy(second);
        assertEquals(second, reopened.attributedPlayer().orElseThrow());

        var interacted = reopened.interactedBy(first);
        assertEquals(first, interacted.attributedPlayer().orElseThrow());
        assertTrue(interacted.actualInteraction());

        var openedAfterInteract = interacted.openedBy(second);
        assertEquals(first, openedAfterInteract.attributedPlayer().orElseThrow());

        var encoded = BlockEntityResearchAttribution.State.CODEC
                .encodeStart(JsonOps.INSTANCE, interacted).getOrThrow();
        var decoded = BlockEntityResearchAttribution.State.CODEC
                .parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(interacted, decoded);
    }

    @Test
    void fishTrapImprovedIntervalIs3m30s() {
        assertEquals(6000, LowerEquipmentRules.fishTrapIntervalTicks(0));
        assertEquals(4200, LowerEquipmentRules.fishTrapIntervalTicks(1));
        assertEquals(4200, LowerEquipmentRules.advanceFishTrapTimer(4199, false, 4200));
        assertEquals(4199, LowerEquipmentRules.advanceFishTrapTimer(4199, true, 4200));
    }

    @Test
    void slimeSplitRulesCalculateRecursiveDescendantsAndXp() {
        assertFalse(ResearchSlimeSplitRules.shouldPreventSplit(0, 4, true, true));
        assertFalse(ResearchSlimeSplitRules.shouldPreventSplit(1, 1, true, true));
        assertFalse(ResearchSlimeSplitRules.shouldPreventSplit(1, 4, false, true));
        assertTrue(ResearchSlimeSplitRules.shouldPreventSplit(1, 4, true, true));

        // nextIntWithBound returning 0 -> 2 children per split:
        // size 4 -> 2x size 2 -> 4x size 1 => [2, 1, 1, 2, 1, 1], total XP = 8
        List<Integer> minChildren = ResearchSlimeSplitRules.descendantSizes(4, bound -> 0);
        assertEquals(List.of(2, 1, 1, 2, 1, 1), minChildren);
        assertEquals(8, ResearchSlimeSplitRules.totalDescendantXp(minChildren));

        // nextIntWithBound returning 2 -> 4 children per split:
        // size 4 -> 4x size 2 -> 16x size 1 => 4*2 + 16*1 = 24 XP
        List<Integer> maxChildren = ResearchSlimeSplitRules.descendantSizes(4, bound -> 2);
        assertEquals(20, maxChildren.size());
        assertEquals(24, ResearchSlimeSplitRules.totalDescendantXp(maxChildren));
    }

    @Test
    void brewingBasicsAccelerates400TickBrewToExactTargetTicks() {
        int[] expectedTicks = {400, 340, 280, 220};
        for (int level = 0; level <= 3; level++) {
            int brewTime = 400;
            double remainder = 0.0D;
            int ticksElapsed = 0;
            while (brewTime > 0) {
                var step = BrewingResearchRules.advanceBrewTime(brewTime, level, remainder);
                brewTime = step.nextBrewTime();
                remainder = step.nextRemainder();
                brewTime--; // vanilla's per-tick decrement
                ticksElapsed++;
                assertTrue(ticksElapsed <= 400, "brew must finish within vanilla time");
            }
            assertEquals(expectedTicks[level], ticksElapsed, "level=" + level);
        }
    }

    @Test
    void potionStudyAndAdvancedBrewingArithmeticMatchSpec() {
        assertEquals(20, BrewingResearchRules.fuelUsesPerBlazePowder(0));
        assertEquals(25, BrewingResearchRules.fuelUsesPerBlazePowder(1));

        // Redstone: 9600 ticks -> +8/+16/+24%
        assertEquals(9600, BrewingResearchRules.redstoneExtendedDuration(9600, 0));
        assertEquals(10368, BrewingResearchRules.redstoneExtendedDuration(9600, 1));
        assertEquals(11136, BrewingResearchRules.redstoneExtendedDuration(9600, 2));
        assertEquals(11904, BrewingResearchRules.redstoneExtendedDuration(9600, 3));

        // Glowstone: 3600 -> 1800 penalty = 1800; 10/20/30% restored = 180/360/540
        assertEquals(1800, BrewingResearchRules.glowstoneMitigatedDuration(3600, 1800, 0));
        assertEquals(1980, BrewingResearchRules.glowstoneMitigatedDuration(3600, 1800, 1));
        assertEquals(2160, BrewingResearchRules.glowstoneMitigatedDuration(3600, 1800, 2));
        assertEquals(2340, BrewingResearchRules.glowstoneMitigatedDuration(3600, 1800, 3));
    }

    @Test
    void autoFarmRecipeExistsUnderResearchGatePath() throws IOException {
        Path recipe = Path.of("src/main/resources/data/moreminecrafts/recipe/research/v3.research.24/level_1/auto_farm.json");
        assertTrue(Files.isRegularFile(recipe), "auto_farm research recipe must exist");
        try (var reader = Files.newBufferedReader(recipe)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("moreminecrafts:auto_farm", json.getAsJsonObject("result").get("id").getAsString());
        }
    }
}
