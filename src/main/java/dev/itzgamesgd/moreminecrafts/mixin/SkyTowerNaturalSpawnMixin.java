package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.mob.sky.SkyIslandRegion;
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

/** SKY-MOB Q33 B: no natural hostile spawning (sky or vanilla) inside a Sky Tower; trial spawners are unaffected. */
@Mixin(NaturalSpawner.class)
public abstract class SkyTowerNaturalSpawnMixin {
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void moreminecrafts$blockSkyTowerInterior(ServerLevel level, MobCategory category,
            StructureManager structures, ChunkGenerator generator, MobSpawnSettings.SpawnerData data,
            BlockPos.MutableBlockPos pos, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (category == MobCategory.MONSTER && SkyIslandRegion.inSkyTower(level, pos)) cir.setReturnValue(false);
    }
}
