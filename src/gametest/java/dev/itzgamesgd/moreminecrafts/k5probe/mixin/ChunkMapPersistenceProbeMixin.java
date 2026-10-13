package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;

/** Confirms Minecraft's real ChunkMap save path sees a persisted K5 diagnostic attachment. */
@Mixin(ChunkMap.class)
abstract class ChunkMapPersistenceProbeMixin {
    @WrapMethod(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z")
    private boolean moreminecrafts$observeMarkedChunkSave(ChunkAccess chunk, Operation<Boolean> original) {
        K5NaturalTreeProbe.ChunkData data = K5NaturalTreeProbe.data(chunk);
        boolean hasMarkers = data != null && !data.positions().isEmpty();
        boolean saved = original.call(chunk);
        if (saved && hasMarkers) K5NaturalTreeProbe.recordSuccessfulChunkSaveWithMarkers();
        return saved;
    }
}
