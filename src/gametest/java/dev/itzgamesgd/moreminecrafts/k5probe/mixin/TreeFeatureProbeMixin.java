package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Captures only TreeFeature's trunk callback, never tree shape or arbitrary log writes. */
@Mixin(TreeFeature.class)
abstract class TreeFeatureProbeMixin {
    @WrapOperation(method = "place", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/TreeFeature;doPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;)Z"))
    private boolean moreminecrafts$recordNaturalTreeTrunk(TreeFeature self, WorldGenLevel level,
            RandomSource random, BlockPos origin, BiConsumer<BlockPos, BlockState> rootSetter,
            BiConsumer<BlockPos, BlockState> trunkSetter, FoliagePlacer.FoliageSetter foliageSetter,
            Operation<Boolean> original) {
        BiConsumer<BlockPos, BlockState> probeTrunkSetter = (pos, state) -> {
            trunkSetter.accept(pos, state);
            K5NaturalTreeProbe.recordTrunkWrite(level, pos, state);
        };
        return original.call(self, level, random, origin, rootSetter, probeTrunkSetter, foliageSetter);
    }
}
