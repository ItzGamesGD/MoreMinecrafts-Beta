package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.mob.CustomMobStats;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class CustomMobStatsMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void moreminecrafts$applyCatalogStats(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                   EntitySpawnReason spawnReason, SpawnGroupData spawnData,
                                                   CallbackInfoReturnable<SpawnGroupData> cir) {
        CustomMobStats.apply((Mob) (Object) this);
    }
}
