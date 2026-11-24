package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03ExteriorFeatureGuard;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ST-03: marks the span in which a headquarters start places its own pieces (see St03ExteriorFeatureGuard). */
@Mixin(StructureStart.class)
abstract class St03StructureStartPlacementMixin {
    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void moreminecrafts$beginSt03Placement(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, CallbackInfo ci) {
        St03ExteriorFeatureGuard.beginPlacement(((StructureStart) (Object) this).getStructure());
    }

    @Inject(method = "placeInChunk", at = @At("RETURN"))
    private void moreminecrafts$endSt03Placement(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, CallbackInfo ci) {
        St03ExteriorFeatureGuard.endPlacement();
    }
}
