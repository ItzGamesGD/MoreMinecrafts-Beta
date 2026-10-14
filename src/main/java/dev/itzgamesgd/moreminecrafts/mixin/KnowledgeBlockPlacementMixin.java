package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeConditionSemantics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts only server-side BlockItem placements whose placement method reports success. */
@Mixin(BlockItem.class)
abstract class KnowledgeBlockPlacementMixin {
    @Unique private static final ThreadLocal<Item> MOREMINECRAFTS$PLACED_ITEM = new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"))
    private void moreminecrafts$snapshotPlacedItem(BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        MOREMINECRAFTS$PLACED_ITEM.set(context.getItemInHand().getItem());
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void moreminecrafts$recordSuccessfulPlacement(BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        Item placedItem = MOREMINECRAFTS$PLACED_ITEM.get();
        MOREMINECRAFTS$PLACED_ITEM.remove();
        if (!KnowledgeConditionSemantics.successfulServerAction(!context.getLevel().isClientSide(),
                result.getReturnValue().consumesAction())
                || !(context.getPlayer() instanceof ServerPlayer player) || placedItem == null) return;
        // Redstone dust is handled by KnowledgeItemUseMixin after the wire appears.
        // It is also a BlockItem, so counting it here would credit one placement twice.
        if (placedItem == net.minecraft.world.item.Items.HOPPER
                || (placedItem != net.minecraft.world.item.Items.REDSTONE
                        && KnowledgeEventCollectors.isEligibleRedstoneComponent(placedItem)))
            KnowledgeEventCollectors.blockPlaced(player, new ItemStack(placedItem));
    }
}
