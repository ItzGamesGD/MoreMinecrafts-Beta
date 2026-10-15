package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Deny creating output until the respective FREE knowledge is recorded. */
@Mixin(CraftingMenu.class)
abstract class KnowledgeCraftingMenuMixin {
    @WrapOperation(method = "slotChangedCraftingGrid",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z"))
    private static boolean moreminecrafts$knowledgeGate(ResultContainer output, ServerPlayer player,
            RecipeHolder<?> recipe, Operation<Boolean> original) {
        boolean used = KnowledgeRuntime.canCraft(player, recipe) && original.call(output, player, recipe);
        return used;
    }

    @WrapOperation(method = "slotChangedCraftingGrid",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private static void moreminecrafts$applyWoodProcessing(ResultContainer output, int slot, ItemStack result,
            Operation<Void> original, @Local(argsOnly = true) Player player) {
        RecipeHolder<?> usedRecipe = output.getRecipeUsed();
        if (slot == 0 && player instanceof ServerPlayer serverPlayer && usedRecipe != null)
            dev.itzgamesgd.moreminecrafts.progression.ResearchCraftingRuntime
                    .applyWoodProcessing(serverPlayer, result, usedRecipe);
        original.call(output, slot, result);
    }
}
