package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.NearbyChestIngredientsRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Syncs nearby chest ingredient summary when a researcher opens a CraftingTableBlock. */
@Mixin(CraftingTableBlock.class)
abstract class ResearchCraftingTableMixin {
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void moreminecrafts$syncNearbyChestsOnOpen(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit,
                                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide() && cir.getReturnValue().consumesAction()
                && player instanceof ServerPlayer serverPlayer) {
            NearbyChestIngredientsRuntime.syncForPlayer(serverPlayer, pos);
        }
    }
}
