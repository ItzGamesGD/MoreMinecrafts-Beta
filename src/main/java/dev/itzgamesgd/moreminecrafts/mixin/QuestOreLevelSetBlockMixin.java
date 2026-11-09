package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.QuestOreProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Every changed block identity consumes positive ore provenance, including piston departure,
 * explosions and replacement. State-only changes such as redstone ore lighting preserve it.
 */
@Mixin(Level.class)
abstract class QuestOreLevelSetBlockMixin {
    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean moreminecrafts$invalidateOre(BlockPos pos, BlockState state, int updateFlags,
            int updateLimit, Operation<Boolean> original) {
        Level level = (Level) (Object) this;
        BlockState previous = level.isClientSide() ? null : this.getBlockState(pos);
        boolean changed = original.call(pos, state, updateFlags, updateLimit);
        if (changed && previous != null && previous.getBlock() != state.getBlock())
            QuestOreProvenance.invalidate(level, pos);
        return changed;
    }
}
