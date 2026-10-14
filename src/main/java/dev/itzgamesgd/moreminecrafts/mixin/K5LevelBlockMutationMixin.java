package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Production removal hook ensures stale markers cannot survive a block replacement. */
@Mixin(Level.class)
abstract class K5LevelBlockMutationMixin {
    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean moreminecrafts$invalidateChangedBlock(BlockPos pos, BlockState state, int updateFlags,
            int updateLimit, Operation<Boolean> original) {
        BlockState previous = this.getBlockState(pos);
        boolean changed = original.call(pos, state, updateFlags, updateLimit);
        if (changed && !previous.equals(state)) {
            K5NaturalTreeProvenance.invalidatePosition((Level) (Object) this, pos);
        }
        return changed;
    }
}
