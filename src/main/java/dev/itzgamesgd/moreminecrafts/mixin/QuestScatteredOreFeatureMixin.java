package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.QuestOreProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The scattered ore variant writes through WorldGenLevel.setBlock. */
@Mixin(ScatteredOreFeature.class)
abstract class QuestScatteredOreFeatureMixin {
    @WrapOperation(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean moreminecrafts$recordOreWrite(WorldGenLevel level, BlockPos pos,
            BlockState state, int flags, Operation<Boolean> original) {
        boolean changed = original.call(level, pos, state, flags);
        if (changed) QuestOreProvenance.recordGenerated(level, pos, state);
        return changed;
    }
}
