package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.XpQuestHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records right-click sweet berry bush harvests when mature berries are picked. */
@Mixin(SweetBerryBushBlock.class)
abstract class QuestSweetBerryMixin {
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void moreminecrafts$recordSweetBerryHarvest(BlockState before, Level level, BlockPos pos,
            Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                || !cir.getReturnValue().consumesAction()) return;
        BlockState after = level.getBlockState(pos);
        if (before.hasProperty(SweetBerryBushBlock.AGE)
                && before.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE
                && after.hasProperty(SweetBerryBushBlock.AGE)
                && after.getValue(SweetBerryBushBlock.AGE) == 1) {
            XpQuestHooks.cropHarvested(serverPlayer, "minecraft:sweet_berry_bush");
        }
    }
}
