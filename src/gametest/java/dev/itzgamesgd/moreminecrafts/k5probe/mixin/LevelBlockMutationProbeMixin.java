package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Test-only removal hook ensures stale markers cannot survive a block replacement. */
@Mixin(Level.class)
abstract class LevelBlockMutationProbeMixin {
    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean moreminecrafts$invalidateChangedBlock(BlockPos pos, BlockState state, int updateFlags,
            int updateLimit, Operation<Boolean> original) {
        BlockState previous = this.getBlockState(pos);
        boolean changed = original.call(pos, state, updateFlags, updateLimit);
        if (changed && !previous.equals(state)) {
            K5NaturalTreeProbe.invalidatePosition((Level) (Object) this, pos);
        }
        return changed;
    }
}
