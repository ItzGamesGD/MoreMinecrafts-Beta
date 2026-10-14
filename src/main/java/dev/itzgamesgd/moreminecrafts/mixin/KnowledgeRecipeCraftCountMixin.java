package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts an actual player take from the crafting output, once per completed recipe action. */
@Mixin(ResultSlot.class)
abstract class KnowledgeRecipeCraftCountMixin {
    @Shadow @Final private Player player;
    @Unique private boolean moreminecrafts$k14CreditedInQuickCraft;

    /** Shift-click transfers call Slot.onQuickCraft with the successfully moved output quantity. */
    @Inject(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("TAIL"))
    private void moreminecrafts$countQuickCraftedTools(ItemStack crafted, int amount, CallbackInfo ci) {
        if (amount <= 0 || !(player instanceof ServerPlayer serverPlayer) || crafted.isEmpty()
                || !dev.itzgamesgd.moreminecrafts.progression.KnowledgeEligibleRosters
                        .isEligibleVanillaTool(crafted.getItem())) return;
        ItemStack moved = crafted.copy();
        moved.setCount(amount);
        KnowledgeEventCollectors.vanillaToolCrafted(serverPlayer, moved);
        moreminecrafts$k14CreditedInQuickCraft = true;
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void moreminecrafts$countCompletedRecipe(Player player, ItemStack crafted,
                                                      CallbackInfo ci) {
        // CraftingMenu.quickMoveStack calls onQuickCraft before onTake, and a full shift-click
        // passes an empty remaining stack to onTake. Consume the paired-callback marker before
        // any early return so it cannot suppress a later, unrelated normal take.
        boolean alreadyCreditedInQuickCraft = moreminecrafts$k14CreditedInQuickCraft;
        moreminecrafts$k14CreditedInQuickCraft = false;
        if (!(player instanceof ServerPlayer serverPlayer) || crafted.isEmpty()) return;
        if (!alreadyCreditedInQuickCraft) KnowledgeEventCollectors.vanillaToolCrafted(serverPlayer, crafted);
        ResearchProgressState state = KnowledgeRuntime.state(serverPlayer);
        KnowledgeRuntime.recordCondition(serverPlayer, "recipes_completed",
                state.conditionProgress("recipes_completed") + 1);
        if (crafted.has(net.minecraft.core.component.DataComponents.FOOD))
            KnowledgeRuntime.recordDistinctCondition(serverPlayer, "foods_crafted_or_cooked",
                    String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crafted.getItem())));
    }
}
