package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import dev.itzgamesgd.moreminecrafts.progression.PrecisionCraftingRules;
import dev.itzgamesgd.moreminecrafts.progression.PrecisionCraftingRuntime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts a server-observed completed crafting result transfer once per recipe action. */
@Mixin(ResultSlot.class)
abstract class KnowledgeRecipeCraftCountMixin {
    @Shadow @Final private Player player;
    @Shadow @Final private CraftingContainer craftSlots;
    @Unique private boolean moreminecrafts$creditedInQuickCraft;
    @Unique private int moreminecrafts$precisionCraftingPreserveSlot = -1;
    @Unique private boolean moreminecrafts$precisionCraftingPreserved;

    /**
     * onTake is reached only after a real result transfer. Resolve one paid candidate while the
     * full pre-consumption grid is still present; preview creation never enters this path.
     */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void moreminecrafts$preparePrecisionCrafting(Player player, ItemStack crafted,
                                                          CallbackInfo ci) {
        moreminecrafts$precisionCraftingPreserveSlot = -1;
        moreminecrafts$precisionCraftingPreserved = false;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        int level = PrecisionCraftingRuntime.purchasedLevel(serverPlayer);
        if (level <= 0) return;

        var positioned = craftSlots.asPositionedCraftInput();
        int candidate = PrecisionCraftingRuntime.selectedContainerSlot(craftSlots,
                positioned.input(), positioned.left(), positioned.top());
        if (candidate < 0) return;

        var materialClass = PrecisionCraftingRuntime.materialClass(craftSlots.getItem(candidate));
        int roll = serverPlayer.getRandom().nextInt(PrecisionCraftingRules.ROLL_RANGE);
        if (PrecisionCraftingRules.succeeds(materialClass, level, roll))
            moreminecrafts$precisionCraftingPreserveSlot = candidate;
    }

    /** Keep the selected unit in the input; vanilla still handles all recipe remainders. */
    @WrapOperation(method = "onTake",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack moreminecrafts$preserveOneEligibleIngredient(CraftingContainer container,
            int slot, int amount, Operation<ItemStack> original) {
        if (slot == moreminecrafts$precisionCraftingPreserveSlot && amount == 1) {
            moreminecrafts$precisionCraftingPreserveSlot = -1;
            moreminecrafts$precisionCraftingPreserved = true;
            return ItemStack.EMPTY;
        }
        return original.call(container, slot, amount);
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void moreminecrafts$clearPrecisionCraftingTransaction(Player player, ItemStack crafted,
                                                                   CallbackInfo ci) {
        boolean preserved = moreminecrafts$precisionCraftingPreserved;
        moreminecrafts$precisionCraftingPreserveSlot = -1;
        moreminecrafts$precisionCraftingPreserved = false;
        if (preserved) craftSlots.setChanged();
    }

    /** Shift-click transfers call Slot.onQuickCraft with the successfully moved output quantity. */
    @Inject(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("TAIL"))
    private void moreminecrafts$countQuickCraftedOutput(ItemStack crafted, int amount, CallbackInfo ci) {
        if (amount <= 0 || !(player instanceof ServerPlayer serverPlayer) || crafted.isEmpty()) return;
        if (dev.itzgamesgd.moreminecrafts.progression.KnowledgeEligibleRosters
                .isEligibleVanillaTool(crafted.getItem())) {
            ItemStack moved = crafted.copy();
            moved.setCount(amount);
            KnowledgeEventCollectors.vanillaToolCrafted(serverPlayer, moved);
        }
        moreminecrafts$recordCompletedRecipe(serverPlayer, crafted);
        moreminecrafts$creditedInQuickCraft = true;
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void moreminecrafts$countCompletedRecipe(Player player, ItemStack crafted,
                                                      CallbackInfo ci) {
        // CraftingMenu.quickMoveStack calls onQuickCraft before onTake, and a full shift-click
        // passes an empty remaining stack to onTake. Consume the paired-callback marker before
        // any early return so it cannot suppress a later, unrelated normal take.
        boolean alreadyCreditedInQuickCraft = moreminecrafts$creditedInQuickCraft;
        moreminecrafts$creditedInQuickCraft = false;
        if (!(player instanceof ServerPlayer serverPlayer) || crafted.isEmpty() || alreadyCreditedInQuickCraft) return;
        if (dev.itzgamesgd.moreminecrafts.progression.KnowledgeEligibleRosters
                .isEligibleVanillaTool(crafted.getItem()))
            KnowledgeEventCollectors.vanillaToolCrafted(serverPlayer, crafted);
        moreminecrafts$recordCompletedRecipe(serverPlayer, crafted);
    }

    @Unique
    private static void moreminecrafts$recordCompletedRecipe(ServerPlayer player, ItemStack crafted) {
        ResearchProgressState state = KnowledgeRuntime.state(player);
        KnowledgeRuntime.recordCondition(player, "recipes_completed",
                state.conditionProgress("recipes_completed") + 1);
        if (crafted.has(net.minecraft.core.component.DataComponents.FOOD))
            KnowledgeRuntime.recordDistinctCondition(player, "foods_crafted_or_cooked",
                    String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crafted.getItem())));
    }
}
