package dev.itzgamesgd.moreminecrafts.mixin;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** SKY-MOB C2: lets a config swap drop the spawn pick made from the previous (vanilla zombie) config. */
@Mixin(TrialSpawnerStateData.class)
public interface TrialSpawnerStateDataAccessor {
    @Accessor("nextSpawnData") Optional<SpawnData> moreminecrafts$nextSpawnData();
    @Accessor("nextSpawnData") void moreminecrafts$setNextSpawnData(Optional<SpawnData> data);
    @Accessor("displayEntity") void moreminecrafts$setDisplayEntity(Entity entity);
}
