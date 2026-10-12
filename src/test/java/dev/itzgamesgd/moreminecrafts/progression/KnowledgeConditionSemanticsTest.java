package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class KnowledgeConditionSemanticsTest {
    @Test void successfulServerActionsRejectFailedAndClientObservations() {
        assertTrue(KnowledgeConditionSemantics.successfulServerAction(true, true));
        assertFalse(KnowledgeConditionSemantics.successfulServerAction(true, false));
        assertFalse(KnowledgeConditionSemantics.successfulServerAction(false, true));
    }

    @Test void animalFeedRequiresActualItemConsumption() {
        assertTrue(KnowledgeConditionSemantics.consumedItem(2, 1));
        assertTrue(KnowledgeConditionSemantics.consumedItem(1, 0));
        assertFalse(KnowledgeConditionSemantics.consumedItem(1, 1));
        assertFalse(KnowledgeConditionSemantics.consumedItem(0, 0));
    }

    @Test void combatKillKeysPreserveBowCrossbowAndTridentDistinction() {
        assertEquals("bow_hostile_kills", KnowledgeConditionSemantics.projectileKillCondition(false));
        assertEquals("crossbow_hostile_kills", KnowledgeConditionSemantics.projectileKillCondition(true));
        assertNotEquals("bow_hostile_kills", "trident_hostile_kills");
    }

    @Test void knowledgeProgressAndDistinctFoodKeysSurviveAttachmentCodecRoundTrip() {
        ResearchProgressState state = new ResearchProgressState(Map.of(
                "condition.foods_crafted_or_cooked", 2,
                "condition.foods_crafted_or_cooked.seen.minecraft.bread", 1,
                "condition.foods_crafted_or_cooked.seen.minecraft.cooked_beef", 1,
                "knowledge.30", 0));
        var encoded = ResearchProgressState.CODEC.encodeStart(JsonOps.INSTANCE, state).result().orElseThrow();
        ResearchProgressState decoded = ResearchProgressState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();
        assertEquals(state, decoded);
        assertEquals(2, decoded.conditionProgress("foods_crafted_or_cooked"));
        assertEquals(1, decoded.get("condition.foods_crafted_or_cooked.seen.minecraft.bread"));
        assertFalse(KnowledgeRules.conditionMet(9, 10));
        assertTrue(KnowledgeRules.conditionMet(10, 10));
    }

    @Test void repeatedFoodObservationIsDeduplicatedByPersistentTypeKey() {
        ResearchProgressState state = ResearchProgressState.empty()
                .with(KnowledgeConditionSemantics.distinctObservationKey(
                        "foods_crafted_or_cooked", "minecraft:bread"), 1);
        assertTrue(KnowledgeConditionSemantics.alreadyObserved(state.counters(),
                "foods_crafted_or_cooked", "minecraft:bread"));
        assertFalse(KnowledgeConditionSemantics.alreadyObserved(state.counters(),
                "foods_crafted_or_cooked", "minecraft:cooked_beef"));
    }

    @Test void blastFurnaceRequiresBothCraftingAndCollectedOutput() {
        assertTrue(KnowledgeConditionSemantics.blastFurnaceUseQualifies(true, true));
        assertFalse(KnowledgeConditionSemantics.blastFurnaceUseQualifies(false, true));
        assertFalse(KnowledgeConditionSemantics.blastFurnaceUseQualifies(true, false));
    }
}
