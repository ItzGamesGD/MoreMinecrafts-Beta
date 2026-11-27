package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.FarmlandCultivators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Drops a cultivator record as soon as its farmland block is replaced by a non-farmland block. */
@Mixin(Level.class)
abstract class SoilStudyFarmlandMutationMixin {
    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Shadow
    public abstract boolean isClientSide();

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean moreminecrafts$clearReplacedFarmland(BlockPos pos, BlockState state, int updateFlags,
            int updateLimit, Operation<Boolean> original) {
        boolean wasFarmland = !this.isClientSide() && this.getBlockState(pos).getBlock() instanceof FarmlandBlock;
        boolean changed = original.call(pos, state, updateFlags, updateLimit);
        if (changed && wasFarmland && !(state.getBlock() instanceof FarmlandBlock))
            FarmlandCultivators.clear((Level) (Object) this, pos);
        return changed;
    }
}
