package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeConditionSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.WaterBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts a successfully placed source-water bucket only when it can irrigate nearby farmland. */
@Mixin(WaterBucketItem.class)
abstract class KnowledgeWaterBucketMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void moreminecrafts$recordIrrigation(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        Level level = context.getLevel();
        if (!KnowledgeConditionSemantics.successfulServerAction(!level.isClientSide(),
                result.getReturnValue().consumesAction())
                || !(context.getPlayer() instanceof ServerPlayer player)) return;
        BlockPos clicked = context.getClickedPos();
        BlockPos adjacent = clicked.relative(context.getClickedFace());
        if ((level.getFluidState(clicked).isSource() && irrigatesFarmland(level, clicked))
                || (level.getFluidState(adjacent).isSource() && irrigatesFarmland(level, adjacent)))
            KnowledgeEventCollectors.successfulFarmlandIrrigation(player);
    }

    private static boolean irrigatesFarmland(Level level, BlockPos water) {
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (Math.abs(dx) + Math.abs(dz) > 4) continue;
            for (int dy = -1; dy <= 0; dy++)
                if (level.getBlockState(water.offset(dx, dy, dz)).is(Blocks.FARMLAND)) return true;
        }
        return false;
    }
}
