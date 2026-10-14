package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Captures successful natural FallenTreeFeature log writes, which do not use TreeFeature's trunk setter. */
@Mixin(FallenTreeFeature.class)
abstract class K5FallenTreeFeatureMixin {
    @WrapOperation(method = "placeLogBlock(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos$MutableBlockPos;Ljava/util/function/Function;)Lnet/minecraft/core/BlockPos;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean moreminecrafts$captureSuccessfulLogWrite(WorldGenLevel level, BlockPos pos,
            BlockState state, Operation<Boolean> original) {
        boolean changed = original.call(level, pos, state);
        if (changed) K5NaturalTreeProvenance.captureFallenTreeWrite(pos, state);
        return changed;
    }

    @WrapMethod(method = "place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z")
    private boolean moreminecrafts$captureFallenTreeLogs(WorldGenLevel level, ChunkGenerator generator,
            RandomSource random, BlockPos origin, Operation<Boolean> original) {
        ArrayList<K5NaturalTreeProvenance.TrunkWrite> writes = new ArrayList<>();
        K5NaturalTreeProvenance.enterFallenTreeCapture(writes);
        boolean placed;
        try {
            placed = original.call(level, generator, random, origin);
        } finally {
            K5NaturalTreeProvenance.exitFallenTreeCapture(writes);
        }
        K5NaturalTreeProvenance.recordTrunkWrites((FallenTreeFeature) (Object) this, level, writes, placed);
        return placed;
    }
}
