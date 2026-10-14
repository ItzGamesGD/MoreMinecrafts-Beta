package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5CooperatingMixinProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** Independent GameTest-only wrapper that audits composition with production marker invalidation. */
@Mixin(Level.class)
abstract class K5CooperatingLevelMixin {
    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean moreminecrafts$observeMutation(BlockPos pos, BlockState state, int updateFlags,
            int updateLimit, Operation<Boolean> original) {
        K5CooperatingMixinProbe.levelMutationWrapped();
        return original.call(pos, state, updateFlags, updateLimit);
    }
}
