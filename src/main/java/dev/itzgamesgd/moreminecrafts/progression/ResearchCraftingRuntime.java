package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.inventory.ResultContainer;

/** Applies Wood Processing to the server-created recipe result before either take path runs. */
public final class ResearchCraftingRuntime {
    private static final List<TagKey<Item>> WOOD_MATERIAL_TAGS = List.of(
            tag("logs"), tag("planks"), tag("wooden_buttons"), tag("wooden_doors"),
            tag("wooden_fences"), tag("wooden_fence_gates"), tag("wooden_pressure_plates"),
            tag("wooden_rods"), tag("wooden_slabs"), tag("wooden_stairs"), tag("wooden_trapdoors"),
            tag("wooden_signs"), tag("wooden_hanging_signs"), tag("boats"), tag("chest_boats"));

    private ResearchCraftingRuntime() {}

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(path));
    }

    public static void applyWoodProcessing(ServerPlayer player, ResultContainer result,
                                           RecipeHolder<?> recipe) {
        if (player == null || result == null || recipe == null || !hasWoodIngredient(recipe)) return;
        int level = ResearchRewards.level(player, "v3.research.09", "wood_processing");
        if (level <= 0) return;
        ItemStack original = result.getItem(0);
        if (original.isEmpty()) return;
        ItemStack updated = original.copy();
        updated.setCount(ResearchContentRules.woodCraftOutputCount(
                original.getCount(), level, original.getMaxStackSize()));
        result.setItem(0, updated);
    }

    static boolean hasWoodIngredient(RecipeHolder<?> recipe) {
        if (!(recipe.value() instanceof CraftingRecipe craftingRecipe)) return false;
        for (Ingredient ingredient : craftingRecipe.placementInfo().ingredients()) {
            if (ingredient.items().anyMatch(item -> item.value() == Items.STICK
                    || item.value() == Items.BAMBOO
                    || WOOD_MATERIAL_TAGS.stream().anyMatch(item::is))) return true;
        }
        return false;
    }
}
