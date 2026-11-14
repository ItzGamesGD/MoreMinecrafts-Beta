package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrialSpawner.class)
public interface TrialSpawnerConfigAccessor {
    @Accessor("config") TrialSpawner.FullConfig moreminecrafts$config();
    @Accessor("config") void moreminecrafts$setConfig(TrialSpawner.FullConfig config);
}
