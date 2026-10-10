package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies the same server authorization to the 2x2 player inventory crafting grid. */
@Mixin(InventoryMenu.class)
abstract class KnowledgeInventoryCraftingMenuMixin {
    @WrapOperation(method = "slotChangedCraftingGrid",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z"))
    private static boolean moreminecrafts$knowledgeGateInventoryCrafting(ResultContainer output,
            ServerPlayer player, RecipeHolder<?> recipe, Operation<Boolean> original) {
        return KnowledgeRuntime.canCraft(player, recipe) && original.call(output, player, recipe);
    }
}
