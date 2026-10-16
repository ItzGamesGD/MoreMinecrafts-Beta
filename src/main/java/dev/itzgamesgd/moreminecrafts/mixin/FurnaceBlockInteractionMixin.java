package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchAttribution;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Attributes a furnace only after its server-side block interaction opens the menu. */
@Mixin(AbstractFurnaceBlock.class)
abstract class FurnaceBlockInteractionMixin {
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void moreminecrafts$attributeMenuOpener(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide() && cir.getReturnValue().consumesAction()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) {
            FurnaceResearchAttribution.opened(furnace, serverPlayer);
        }
    }
}
