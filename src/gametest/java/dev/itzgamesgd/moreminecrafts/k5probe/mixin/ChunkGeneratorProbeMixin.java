package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

/** Test-mod-only scope around the verified natural biome-decoration entry point. */
@Mixin(ChunkGenerator.class)
abstract class ChunkGeneratorProbeMixin {
    @WrapMethod(method = "applyBiomeDecoration(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V")
    private void moreminecrafts$probeNaturalBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
            StructureManager structureManager, Operation<Void> original) {
        K5NaturalTreeProbe.enterNaturalBiomeDecoration();
        try {
            original.call((ChunkGenerator) (Object) this, level, chunk, structureManager);
        } finally {
            K5NaturalTreeProbe.exitNaturalBiomeDecoration();
        }
    }
}
