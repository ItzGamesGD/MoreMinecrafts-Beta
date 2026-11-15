package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumIntegration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records only successful server-side player placements in the ST-05 arena protection mask (Q91, Q105). */
@Mixin(BlockItem.class)
abstract class ColosseumPlacementMixin {
    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void moreminecrafts$recordPlayerPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() instanceof InteractionResult.Success
                && context.getPlayer() instanceof ServerPlayer
                && context.getLevel() instanceof ServerLevel level) {
            ColosseumIntegration.onPlayerPlaced(level, context.getClickedPos());
        }
    }
}
