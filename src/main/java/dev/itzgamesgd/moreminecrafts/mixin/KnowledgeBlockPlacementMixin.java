package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
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
        if (!result.getReturnValue().consumesAction()
                || !(context.getPlayer() instanceof ServerPlayer player)
                || context.getLevel().isClientSide()) return;
        ItemStack placed = context.getItemInHand();
        if (placed.is(net.minecraft.world.item.Items.HOPPER)
                || KnowledgeEventCollectors.isEligibleRedstoneComponent(placed))
            KnowledgeEventCollectors.blockPlaced(player, placed);
    }
}
