package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.QuestNaturalCropProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records positive provenance when a TorchflowerCropBlock matures into Blocks.TORCHFLOWER. */
@Mixin(CropBlock.class)
abstract class QuestCropGrowthMixin {
    @Inject(method = "randomTick", at = @At("RETURN"))
    private void moreminecrafts$markMaturedTorchflowerTick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random, CallbackInfo ci) {
        if ((Object) this instanceof TorchflowerCropBlock && level.getBlockState(pos).is(Blocks.TORCHFLOWER)) {
            QuestNaturalCropProvenance.mark(level, pos);
        }
    }

    @Inject(method = "growCrops", at = @At("RETURN"))
    private void moreminecrafts$markMaturedTorchflowerBonemeal(Level level, BlockPos pos, BlockState state,
            CallbackInfo ci) {
        if (!level.isClientSide() && (Object) this instanceof TorchflowerCropBlock
                && level.getBlockState(pos).is(Blocks.TORCHFLOWER)) {
            QuestNaturalCropProvenance.mark(level, pos);
        }
    }
}
