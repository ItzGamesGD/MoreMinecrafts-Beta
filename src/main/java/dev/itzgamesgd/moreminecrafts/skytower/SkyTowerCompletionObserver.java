package dev.itzgamesgd.moreminecrafts.skytower;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;

/** Observes vanilla's post-reward COOLDOWN; never changes spawning, loot, or trial state.
 * Observing COOLDOWN on entry also reconciles a saved vanilla cycle after unload/restart,
 * including its expiry on this tick. Permanent bits make repeated observations no-ops.
 */
public final class SkyTowerCompletionObserver {
    private SkyTowerCompletionObserver() {}
    public static boolean completionEvidence(TrialSpawnerState before,TrialSpawnerState after){
        return before==TrialSpawnerState.COOLDOWN || after==TrialSpawnerState.COOLDOWN;
    }
    public static void observe(ServerLevel level,BlockPos position){
        var data=level.getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        if(data.quarantined() || data.sites().isEmpty())return;
        var point=new SkyTowerLayout.Point(position.getX(),position.getY(),position.getZ());
        String dimension=level.dimension().identifier().toString();
        for(var entry:data.sites().entrySet()){
            var site=entry.getValue();
            if(site.dimension().equals(dimension) && site.spawnerIndex(point)>=0){
                data.recordVanillaCompletion(entry.getKey(),point);return;
            }
        }
    }
}
