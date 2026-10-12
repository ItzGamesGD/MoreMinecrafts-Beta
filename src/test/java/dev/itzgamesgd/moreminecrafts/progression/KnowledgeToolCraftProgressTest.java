package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

final class KnowledgeToolCraftProgressTest {
    @Test void fiveCopiesOfOneToolMeetTheQuantityTarget() {
        int current = 0;
        for (int i = 0; i < 5; i++) current = KnowledgeToolCraftProgress.advance(
                current, new ItemStack(Items.IRON_PICKAXE));
        assertEquals(5, current);
    }

    @Test void quantitiesFromMixedFamiliesAccumulateAndCapAtTarget() {
        int current = 0;
        current = KnowledgeToolCraftProgress.advance(current, new ItemStack(Items.WOODEN_AXE));
        current = KnowledgeToolCraftProgress.advance(current, new ItemStack(Items.WOODEN_AXE));
        current = KnowledgeToolCraftProgress.advance(current, new ItemStack(Items.DIAMOND_HOE));
        current = KnowledgeToolCraftProgress.advance(current, new ItemStack(Items.STONE_SWORD));
        current = KnowledgeToolCraftProgress.advance(current, new ItemStack(Items.STONE_SWORD));
        assertEquals(5, current);
    }

    @Test void everyRegisteredMaterialAndApprovedFamilyIsAccepted() {
        assertTrue(KnowledgeEligibleRosters.vanillaToolItems().size() >= 25);
        for (var item : KnowledgeEligibleRosters.vanillaToolItems())
            assertEquals(1, KnowledgeToolCraftProgress.advance(0, new ItemStack(item)));
    }

    @Test void unrelatedAndModdedItemsDoNotAdvanceTheCounter() {
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, new ItemStack(Items.BOW)));
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, new ItemStack(Items.TRIDENT)));
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, new ItemStack(Items.MACE)));
        assertEquals(2, KnowledgeToolCraftProgress.advance(2, new ItemStack(Items.AIR)));
    }

    @Test void savedK14CounterSurvivesResearchProgressAttachmentCodec() {
        var state = ResearchProgressState.empty().with("condition.tool_types_crafted", 4)
                .with("knowledge.14", 0);
        var encoded = ResearchProgressState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, state)
                .result().orElseThrow();
        var decoded = ResearchProgressState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .result().orElseThrow();
        assertEquals(4, decoded.conditionProgress("tool_types_crafted"));
        assertEquals(0, decoded.get("knowledge.14"));
    }
}
