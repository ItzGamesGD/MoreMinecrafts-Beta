package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import org.spongepowered.asm.mixin.Mixin;

/** Captures successful natural FallenTreeFeature log writes, which do not use TreeFeature's trunk setter. */
@Mixin(FallenTreeFeature.class)
abstract class K5FallenTreeFeatureMixin {
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
