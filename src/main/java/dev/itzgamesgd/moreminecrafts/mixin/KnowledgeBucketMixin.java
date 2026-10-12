package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the actual successful fluid placement performed by a water bucket. */
@Mixin(BucketItem.class)
abstract class KnowledgeBucketMixin {
    @Inject(method = "emptyContents", at = @At("RETURN"))
    private void moreminecrafts$recordSuccessfulWaterPlacement(Player player, Level level, BlockPos pos,
            BlockHitResult hitResult, CallbackInfoReturnable<Boolean> result) {
        if ((Object) this != Items.WATER_BUCKET || level.isClientSide()
                || !(player instanceof ServerPlayer serverPlayer)
                || !Boolean.TRUE.equals(result.getReturnValue())) return;
        if (level.getFluidState(pos).isSource() && irrigatesFarmland(level, pos))
            KnowledgeEventCollectors.successfulFarmlandIrrigation(serverPlayer);
    }

    private static boolean irrigatesFarmland(Level level, BlockPos water) {
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (Math.abs(dx) + Math.abs(dz) > 4) continue;
            for (int dy = -1; dy <= 0; dy++)
                if (level.getBlockState(water.offset(dx, dy, dz)).is(net.minecraft.world.level.block.Blocks.FARMLAND))
                    return true;
        }
        return false;
    }
}
