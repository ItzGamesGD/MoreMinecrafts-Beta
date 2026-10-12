package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeConditionSemantics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts only server-side BlockItem placements whose placement method reports success. */
@Mixin(BlockItem.class)
abstract class KnowledgeBlockPlacementMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void moreminecrafts$recordSuccessfulPlacement(BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        if (!KnowledgeConditionSemantics.successfulServerAction(!context.getLevel().isClientSide(),
                result.getReturnValue().consumesAction())
                || !(context.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack placed = context.getItemInHand();
        // Redstone dust is handled by KnowledgeItemUseMixin after the wire appears.
        // It is also a BlockItem, so counting it here would credit one placement twice.
        if (placed.is(net.minecraft.world.item.Items.HOPPER)
                || (placed.getItem() != net.minecraft.world.item.Items.REDSTONE
                        && KnowledgeEventCollectors.isEligibleRedstoneComponent(placed.getItem())))
            KnowledgeEventCollectors.blockPlaced(player, placed);
    }
}
