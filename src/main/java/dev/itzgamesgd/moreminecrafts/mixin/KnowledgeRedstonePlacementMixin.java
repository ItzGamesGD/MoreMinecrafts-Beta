package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeConditionSemantics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.RedstoneItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Redstone dust uses RedstoneItem rather than BlockItem; count only successful wire placement. */
@Mixin(RedstoneItem.class)
abstract class KnowledgeRedstonePlacementMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void moreminecrafts$recordRedstoneWire(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        if (!KnowledgeConditionSemantics.successfulServerAction(!context.getLevel().isClientSide(),
                result.getReturnValue().consumesAction())
                || !(context.getPlayer() instanceof ServerPlayer player)) return;
        var clicked = context.getClickedPos();
        var placed = clicked.relative(context.getClickedFace());
        if (context.getLevel().getBlockState(clicked).is(Blocks.REDSTONE_WIRE)
                || context.getLevel().getBlockState(placed).is(Blocks.REDSTONE_WIRE))
            KnowledgeEventCollectors.blockPlaced(player, context.getItemInHand());
    }
}
