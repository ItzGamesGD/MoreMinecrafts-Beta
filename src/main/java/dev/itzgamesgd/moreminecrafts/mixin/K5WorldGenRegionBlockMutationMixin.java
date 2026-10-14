package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Later world-generation writes must not make a recorded trunk position stale. */
@Mixin(WorldGenRegion.class)
abstract class K5WorldGenRegionBlockMutationMixin {
    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean moreminecrafts$invalidateWorldgenReplacement(BlockPos pos, BlockState state,
            int updateFlags, int updateLimit, Operation<Boolean> original) {
        BlockState previous = getBlockState(pos);
        boolean changed = original.call(pos, state, updateFlags, updateLimit);
        if (changed && !previous.equals(state)) {
            K5NaturalTreeProvenance.captureFallenTreeWrite(pos, state);
            WorldGenRegion region = (WorldGenRegion) (Object) this;
            K5NaturalTreeProvenance.invalidatePosition(region.getChunk(pos.getX() >> 4, pos.getZ() >> 4), pos);
        }
        return changed;
    }
}
