package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

/** Production scope around the verified natural biome-decoration entry point. */
@Mixin(ChunkGenerator.class)
abstract class K5ChunkGeneratorMixin {
    @WrapMethod(method = "applyBiomeDecoration(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V")
    private void moreminecrafts$naturalBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
            StructureManager structureManager, Operation<Void> original) {
        K5NaturalTreeProvenance.enterNaturalBiomeDecoration(level.registryAccess());
        try {
            original.call(level, chunk, structureManager);
        } finally {
            K5NaturalTreeProvenance.exitNaturalBiomeDecoration();
        }
    }
}
