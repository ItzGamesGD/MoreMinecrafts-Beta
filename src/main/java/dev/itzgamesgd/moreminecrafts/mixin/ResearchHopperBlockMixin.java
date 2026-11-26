package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.HopperFilterRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Attributes and syncs Hopper filter state when a player opens a HopperBlock. */
@Mixin(HopperBlock.class)
abstract class ResearchHopperBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void moreminecrafts$onHopperOpen(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit,
                                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide() && cir.getReturnValue().consumesAction()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof HopperBlockEntity hopper) {
            HopperFilterRuntime.onHopperOpened(hopper, serverPlayer);
        }
    }
}
