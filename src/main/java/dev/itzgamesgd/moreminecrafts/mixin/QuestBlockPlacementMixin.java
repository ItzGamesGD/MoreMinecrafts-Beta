package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.XpQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Live XP Quest placement source. BlockItem.place returns SUCCESS only after placeBlock succeeded
 * (26.3 bytecode), and every block-item placement path goes through it, including
 * PlaceOnWaterBlockItem (lily pad, frogspawn) which bypasses the ITEM_USED statistic.
 */
@Mixin(BlockItem.class)
abstract class QuestBlockPlacementMixin {
    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void moreminecrafts$countConfirmedPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() instanceof InteractionResult.Success
                && context.getPlayer() instanceof ServerPlayer player && !context.getLevel().isClientSide()) {
            XpQuestHooks.blockPlaced(player, (BlockItem) (Object) this);
        }
    }
}
