package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.itzgamesgd.moreminecrafts.progression.QuestOreProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** OreFeature writes sections directly, bypassing both Level and WorldGenRegion setters. */
@Mixin(OreFeature.class)
abstract class QuestOreFeatureMixin {
    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState moreminecrafts$recordOreWrite(LevelChunkSection section, int x, int y, int z,
            BlockState state, boolean lock, Operation<BlockState> original,
            @Local(argsOnly = true) WorldGenLevel level, @Local(index = 22) BlockPos.MutableBlockPos pos) {
        BlockState previous = original.call(section, x, y, z, state, lock);
        QuestOreProvenance.recordGenerated(level, pos, state);
        return previous;
    }
}
