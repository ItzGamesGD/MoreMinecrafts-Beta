package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class KnowledgeToolCraftProgressTest {
    @Test void fiveCopiesOfOneToolMeetTheQuantityTarget() {
        int current = 0;
        for (int i = 0; i < 5; i++) current = KnowledgeToolCraftProgress.advance(
                current, Identifier.parse("minecraft:iron_pickaxe"), 1);
        assertEquals(5, current);
    }

    @Test void quantitiesFromMixedFamiliesAccumulateAndCapAtTarget() {
        int current = 0;
        current = KnowledgeToolCraftProgress.advance(current, Identifier.parse("minecraft:wooden_axe"), 1);
        current = KnowledgeToolCraftProgress.advance(current, Identifier.parse("minecraft:wooden_axe"), 1);
        current = KnowledgeToolCraftProgress.advance(current, Identifier.parse("minecraft:diamond_hoe"), 1);
        current = KnowledgeToolCraftProgress.advance(current, Identifier.parse("minecraft:stone_sword"), 1);
        current = KnowledgeToolCraftProgress.advance(current, Identifier.parse("minecraft:stone_sword"), 1);
        assertEquals(5, current);
    }

    @Test void everyRegisteredMaterialAndApprovedFamilyIsAccepted() {
        assertTrue(KnowledgeEligibleRosters.vanillaToolItems().size() >= 25);
        for (var item : KnowledgeEligibleRosters.vanillaToolItems())
            assertEquals(1, KnowledgeToolCraftProgress.advance(0,
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item), 1));
    }

    @Test void unrelatedAndModdedItemsDoNotAdvanceTheCounter() {
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, Identifier.parse("minecraft:bow"), 1));
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, Identifier.parse("minecraft:trident"), 1));
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, Identifier.parse("minecraft:mace"), 1));
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, Identifier.parse("minecraft:air"), 1));
    }

    @Test void savedK14CounterSurvivesResearchProgressAttachmentCodec() {
        var state = ResearchProgressState.empty().with("condition.tool_types_crafted", 4)
                .with("knowledge.14", 1);
        var encoded = ResearchProgressState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, state)
                .result().orElseThrow();
        var decoded = ResearchProgressState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .result().orElseThrow();
        assertEquals(4, decoded.conditionProgress("tool_types_crafted"));
        assertEquals(1, decoded.get("knowledge.14"), "an unlocked K14 remains persisted independently of statistics");
    }
}
