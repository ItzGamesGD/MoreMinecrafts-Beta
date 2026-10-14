package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance.ChunkData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;

/** Observes Minecraft's actual server-level unload callback for marked chunks. */
@Mixin(ServerLevel.class)
abstract class ServerLevelUnloadProbeMixin {
    @WrapMethod(method = "unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V")
    private void moreminecrafts$observeMarkedChunkUnload(LevelChunk chunk, Operation<Void> original) {
        ChunkData before = K5NaturalTreeProbe.data(chunk);
        boolean marked = before != null && !before.positions().isEmpty();
        original.call(chunk);
        if (marked) K5NaturalTreeProbe.recordMarkedChunkUnload((ServerLevel) (Object) this, chunk);
    }
}
