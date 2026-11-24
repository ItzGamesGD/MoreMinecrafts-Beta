package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.BrewingResearchRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Attributes a brewing stand on server-side block interaction when no player has mutated its slots yet. */
@Mixin(BrewingStandBlock.class)
abstract class BrewingStandBlockInteractionMixin {
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void moreminecrafts$attributeBrewingOpener(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide() && cir.getReturnValue().consumesAction()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand) {
            BrewingResearchRuntime.opened(stand, serverPlayer);
        }
    }
}
