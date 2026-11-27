package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerCompletionObserver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrialSpawner.class)
public abstract class SkyTowerTrialCompletionMixin {
    @Shadow public abstract TrialSpawnerState getState();
    @Unique private TrialSpawnerState moreminecrafts$beforeTrialTick;
    @Inject(method="tickServer",at=@At("HEAD"))
    private void moreminecrafts$before(ServerLevel level,BlockPos pos,boolean ominous,CallbackInfo ci){
        moreminecrafts$beforeTrialTick=getState();
    }
    @Inject(method="tickServer",at=@At("TAIL"))
    private void moreminecrafts$after(ServerLevel level,BlockPos pos,boolean ominous,CallbackInfo ci){
        if(SkyTowerCompletionObserver.completionEvidence(moreminecrafts$beforeTrialTick,getState()))
            SkyTowerCompletionObserver.observe(level,pos);
    }
}
