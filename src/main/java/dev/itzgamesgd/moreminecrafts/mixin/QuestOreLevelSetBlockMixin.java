package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import dev.itzgamesgd.moreminecrafts.progression.QuestOreProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Any tracked ore that appears through Level.setBlock after natural decoration is non-natural
 * (player placement, piston arrival, commands, templates). Same-block state changes such as
 * redstone ore lighting up keep the existing verdict.
 */
@Mixin(Level.class)
abstract class QuestOreLevelSetBlockMixin {
    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean moreminecrafts$markNonNaturalOre(BlockPos pos, BlockState state, int updateFlags,
            int updateLimit, Operation<Boolean> original) {
        Level level = (Level) (Object) this;
        BlockState previous = level.isClientSide() ? null : this.getBlockState(pos);
        boolean changed = original.call(pos, state, updateFlags, updateLimit);
        if (changed && previous != null && previous.getBlock() != state.getBlock()
                && QuestOreProvenance.isTrackedOre(state) && !K5NaturalTreeProvenance.isNaturalBiomeDecoration()) {
            QuestOreProvenance.markNonNatural(level, pos);
        }
        return changed;
    }
}
