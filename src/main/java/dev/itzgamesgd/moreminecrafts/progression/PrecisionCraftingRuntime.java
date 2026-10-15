package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.Arrays;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/** Applies the paid preservation rule only inside a completed server crafting transaction. */
public final class PrecisionCraftingRuntime {
    private static final String RESEARCH_ID = "v3.research.08";
    private static final TagKey<Item> ORDINARY = tag("ordinary");
    private static final TagKey<Item> PROCESSED = tag("processed");
    private static final TagKey<Item> PRECIOUS = tag("precious");
    private static final TagKey<Item> RARE = tag("rare");
    private static final TagKey<Item> UNIQUE = tag("unique");
    private static final TagKey<Item> COMPRESSED_TOKENS = tag("compressed_tokens");

    private PrecisionCraftingRuntime() {}

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ModIds.id("research/precision_crafting/" + name));
    }

    public static int purchasedLevel(ServerPlayer player) {
        return purchasedLevel(KnowledgeRuntime.state(player));
    }

    static int purchasedLevel(ResearchProgressState state) {
        return Math.min(PrecisionCraftingRules.MAX_LEVEL, state.researchLevel(RESEARCH_ID));
    }

    /**
     * Selects from the recipe's actual, positioned inputs and returns the corresponding full-grid
     * container index. The result-slot hook invokes this before vanilla consumes any ingredients.
     */
    public static int selectedContainerSlot(CraftingContainer container, CraftingInput input,
            int left, int top) {
        var classes = new PrecisionCraftingRules.MaterialClass[container.getContainerSize()];
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int inputSlot = x + y * input.width();
                int containerSlot = x + left + (y + top) * container.getWidth();
                classes[containerSlot] = materialClass(input.getItem(inputSlot));
            }
        }
        return PrecisionCraftingRules.selectedSlot(Arrays.asList(classes));
    }

    /** Unclassified and unique materials are deliberately ineligible. */
    public static PrecisionCraftingRules.MaterialClass materialClass(ItemStack stack) {
        if (stack.isEmpty() || stack.is(COMPRESSED_TOKENS) || stack.is(UNIQUE)) return null;
        if (stack.is(RARE)) return PrecisionCraftingRules.MaterialClass.RARE;
        if (stack.is(PRECIOUS)) return PrecisionCraftingRules.MaterialClass.PRECIOUS;
        if (stack.is(PROCESSED)) return PrecisionCraftingRules.MaterialClass.PROCESSED;
        if (stack.is(ORDINARY)) return PrecisionCraftingRules.MaterialClass.ORDINARY;
        return null;
    }
}
