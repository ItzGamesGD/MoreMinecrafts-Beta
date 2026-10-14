package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.grower.TreeGrower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Test-only trace of vanilla's feature placement calls on the sapling TreeGrower path. */
@Mixin(TreeGrower.class)
abstract class TreeGrowerProbeMixin {
    @WrapOperation(
            method = "growTree(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/Feature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean moreminecrafts$traceSaplingFeaturePlacement(Feature feature, WorldGenLevel level,
            ChunkGenerator generator, RandomSource random, BlockPos origin, Operation<Boolean> original) {
        boolean succeeded = original.call(feature, level, generator, random, origin);
        K5NaturalTreeProbe.recordTreeGrowerFeatureAttempt(level, feature, origin, succeeded);
        return succeeded;
    }
}
