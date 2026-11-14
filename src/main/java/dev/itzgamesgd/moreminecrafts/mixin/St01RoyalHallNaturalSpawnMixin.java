package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01RoyalHallRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ST-01 boss room: no natural monster spawning inside the royal hall box. Everywhere else is vanilla. */
@Mixin(NaturalSpawner.class)
public abstract class St01RoyalHallNaturalSpawnMixin {
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void moreminecrafts$blockRoyalHallSpawns(ServerLevel level, MobCategory category,
            StructureManager structures, ChunkGenerator generator, MobSpawnSettings.SpawnerData data,
            BlockPos.MutableBlockPos pos, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (category == MobCategory.MONSTER && St01RoyalHallRegion.inRoyalHall(level, pos)) cir.setReturnValue(false);
    }
}
