package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import java.util.ArrayList;
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
abstract class K5TreeFeatureMixin {
    @WrapOperation(method = "place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/TreeFeature;doPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;)Z"))
    private boolean moreminecrafts$recordNaturalTreeTrunk(TreeFeature self, WorldGenLevel level,
            RandomSource random, BlockPos origin, BiConsumer<BlockPos, BlockState> rootSetter,
            BiConsumer<BlockPos, BlockState> trunkSetter, FoliagePlacer.FoliageSetter foliageSetter,
            Operation<Boolean> original) {
        ArrayList<K5NaturalTreeProvenance.TrunkWrite> trunkWrites = new ArrayList<>();
        BiConsumer<BlockPos, BlockState> probeTrunkSetter = (pos, state) -> {
            trunkSetter.accept(pos, state);
            trunkWrites.add(new K5NaturalTreeProvenance.TrunkWrite(pos.immutable(), state));
        };
        boolean placed = original.call(self, level, random, origin, rootSetter, probeTrunkSetter, foliageSetter);
        K5NaturalTreeProvenance.recordTrunkWrites(self, level, trunkWrites, placed);
        return placed;
    }
}
