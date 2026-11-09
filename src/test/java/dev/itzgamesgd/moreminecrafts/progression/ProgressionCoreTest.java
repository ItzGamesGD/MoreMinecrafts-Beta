package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ProgressionCoreTest {
    @Test void approvedKnowledgeCatalogAccountsForAllSeventyStableIds() {
        assertEquals(70, KnowledgeCatalog.entries().size());
        for (int id = 1; id <= 70; id++) {
            assertNotNull(KnowledgeCatalog.byNumber(id), "Missing knowledge #" + id);
            assertEquals("knowledge." + id, KnowledgeCatalog.byNumber(id).stateKey());
        }
        assertEquals(java.util.List.of(51), KnowledgeCatalog.entries().stream()
                .filter(KnowledgeCatalog.Entry::unresolved).map(KnowledgeCatalog.Entry::number).toList());
    }

    @Test void knowledgeConditionsDoNotUnlockEarlyAndProgressNeverMovesBackward() {
        assertFalse(KnowledgeRules.conditionMet(49, 50));
        assertTrue(KnowledgeRules.conditionMet(50, 50));
        assertEquals(50, KnowledgeRules.monotonicProgress(50, 2));
    }

    @Test void v4ResearchCatalogContainsSeventeenApprovedPurchasesAndCaps() {
        assertEquals(17, ResearchCatalog.v4Additions().size());
        assertEquals(1, ResearchCatalog.byId("throwable_pickaxe").maximumLevel());
        assertEquals(900, ResearchCatalog.byId("throwable_pickaxe").cost(1).rawXp());
        assertEquals(2800, ResearchCatalog.byId("mob_capture").cost(1).rawXp());
    }

    @Test void purchaseRulesRejectEveryUnmetConditionWithoutMutatingResources() {
        ResearchDefinition definition = ResearchCatalog.byId("throwable_pickaxe");
        var allKnowledge = java.util.Set.of(9);
        var ownedParents = java.util.Set.of("resources_root", "v3.research.01");
        var inventory = Map.of("minecraft:iron_block", 4, "minecraft:redstone", 64, "minecraft:diamond", 4);
        Map<String,Integer> originalInventory = Map.copyOf(inventory);
        assertEquals(ResearchPurchaseRules.Result.PURCHASED,
                ResearchPurchaseRules.validate(definition, 1, 0, allKnowledge, ownedParents, inventory, 900));
        assertEquals(ResearchPurchaseRules.Result.MISSING_KNOWLEDGE,
                ResearchPurchaseRules.validate(definition, 1, 0, java.util.Set.of(), java.util.Set.of(), inventory, 900));
        ResearchDefinition dependent = new ResearchDefinition("dependent", "combat", "", "", 1,
                java.util.Set.of(9), java.util.Set.of("parent"), java.util.List.of(definition.cost(1)));
        assertEquals(ResearchPurchaseRules.Result.MISSING_PREREQUISITE,
                ResearchPurchaseRules.validate(dependent, 1, 0, allKnowledge, java.util.Set.of(), inventory, 900));
        assertEquals(ResearchPurchaseRules.Result.MISSING_MATERIALS,
                ResearchPurchaseRules.validate(definition, 1, 0, allKnowledge, ownedParents, Map.of(), 900));
        assertEquals(ResearchPurchaseRules.Result.INSUFFICIENT_XP,
                ResearchPurchaseRules.validate(definition, 1, 0, allKnowledge, ownedParents, inventory, 899));
        assertEquals(ResearchPurchaseRules.Result.ALREADY_PURCHASED,
                ResearchPurchaseRules.validate(definition, 1, 1, allKnowledge, ownedParents, inventory, 900));
        assertEquals(ResearchPurchaseRules.Result.INVALID_LEVEL,
                ResearchPurchaseRules.validate(definition, 2, 0, allKnowledge, ownedParents, inventory, 900));
        assertEquals(originalInventory, inventory, "Validation must not consume materials");
        assertNull(ResearchCatalog.byId("unknown_research"));
    }

    @Test void progressionStateKeepsExistingKeysAndPersistsQuestClaimKeys() {
        ResearchProgressState old = new ResearchProgressState(Map.of("knowledge.57", 1, "research.wall_jump", 1));
        ResearchProgressState updated = old.withQuestProgress("first_diamond", 1)
                .completeQuest("first_diamond")
                .claimQuest("first_diamond");
        assertTrue(updated.hasKnowledge(57));
        assertEquals(1, updated.researchLevel("wall_jump"));
        assertEquals(1, updated.questProgress("first_diamond"));
        assertTrue(updated.questComplete("first_diamond"));
        assertTrue(updated.questClaimed("first_diamond"));
        assertFalse(old.questComplete("first_diamond"));
        JsonElement saved = ResearchProgressState.CODEC.encodeStart(JsonOps.INSTANCE, updated).getOrThrow();
        ResearchProgressState loaded = ResearchProgressState.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(updated, loaded);
    }

    @Test void questProgressCompletesAndClaimIsOneTime() {
        QuestDefinition quest = new QuestDefinition("test.quest", "activities", "title", "description",
                "objective", 3, 25);
        ResearchProgressState state = QuestRules.addProgress(ResearchProgressState.empty(), quest, 2);
        assertFalse(state.questComplete(quest.id()));
        state = QuestRules.addProgress(state, quest, 5);
        assertEquals(3, state.questProgress(quest.id()));
        assertTrue(QuestRules.canClaim(state, quest));
        state = QuestRules.claim(state, quest);
        assertTrue(state.questClaimed(quest.id()));
        assertFalse(QuestRules.canClaim(state, quest));
        assertSame(state, QuestRules.addProgress(state, quest, 1));
    }

    @Test void syncRejectsOutOfOrderDeltasAndIgnoresStaleSnapshots() {
        var current = Map.of("knowledge.1", 1);
        var gap = ProgressionSyncRules.delta(2, current, 4, Map.of("knowledge.2", 1));
        assertEquals(ProgressionSyncRules.Status.NEED_SNAPSHOT, gap.status());
        var applied = ProgressionSyncRules.delta(2, current, 3, Map.of("knowledge.2", 1));
        assertEquals(ProgressionSyncRules.Status.APPLIED, applied.status());
        assertEquals(1, applied.values().get("knowledge.2"));
        var stale = ProgressionSyncRules.snapshot(3, applied.values(), 2, Map.of());
        assertEquals(ProgressionSyncRules.Status.STALE, stale.status());
        assertTrue(stale.values().containsKey("knowledge.1"));
    }
}
