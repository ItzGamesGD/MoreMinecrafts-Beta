package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void moreminecrafts$applyWoodProcessing(
            net.minecraft.world.inventory.AbstractContainerMenu menu, ServerLevel level, Player player,
            CraftingContainer input, ResultContainer output, RecipeHolder<?> recipeHint, CallbackInfo callback) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        RecipeHolder<?> usedRecipe = output.getRecipeUsed();
        if (usedRecipe != null) dev.itzgamesgd.moreminecrafts.progression.ResearchCraftingRuntime
                .applyWoodProcessing(serverPlayer, output, usedRecipe);
    }
}
