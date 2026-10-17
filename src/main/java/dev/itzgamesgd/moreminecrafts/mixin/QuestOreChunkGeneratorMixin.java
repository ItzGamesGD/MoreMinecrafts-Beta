package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.QuestOreProvenance;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

/** Marks a chunk as ore-provenance tracked once its natural decoration (including ore features) ran. */
@Mixin(ChunkGenerator.class)
abstract class QuestOreChunkGeneratorMixin {
    @WrapMethod(method = "applyBiomeDecoration(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V")
    private void moreminecrafts$trackOreProvenance(WorldGenLevel level, ChunkAccess chunk,
            StructureManager structureManager, Operation<Void> original) {
        original.call(level, chunk, structureManager);
        QuestOreProvenance.markTrackedAfterNaturalDecoration(chunk);
    }
}
