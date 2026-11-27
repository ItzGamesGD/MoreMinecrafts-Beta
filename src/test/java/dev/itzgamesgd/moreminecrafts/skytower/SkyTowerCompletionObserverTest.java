package dev.itzgamesgd.moreminecrafts.skytower;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkyTowerCompletionObserverTest {
    @Test void aVanillaCombatCycleRecordsOnePermanentCompletionOnlyAfterRewards(){
        var p=new SkyTowerProgress(0);
        var cycle=new TrialSpawnerState[]{TrialSpawnerState.WAITING_FOR_PLAYERS,TrialSpawnerState.ACTIVE,
            TrialSpawnerState.WAITING_FOR_REWARD_EJECTION,TrialSpawnerState.EJECTING_REWARD,TrialSpawnerState.COOLDOWN};
        for(int i=1;i<cycle.length;i++){
            if(SkyTowerCompletionObserver.completionEvidence(cycle[i-1],cycle[i]))p.completeVanillaTrial(0);
            assertEquals(i==cycle.length-1?1:0,p.completedMask());
        }
        assertFalse(p.completeVanillaTrial(0));
    }
    @Test void unloadRestoreAndCooldownExpiryRemainCompletionEvidence(){
        assertTrue(SkyTowerCompletionObserver.completionEvidence(TrialSpawnerState.COOLDOWN,TrialSpawnerState.WAITING_FOR_PLAYERS));
        assertFalse(SkyTowerCompletionObserver.completionEvidence(TrialSpawnerState.ACTIVE,TrialSpawnerState.WAITING_FOR_PLAYERS));
        assertFalse(SkyTowerCompletionObserver.completionEvidence(TrialSpawnerState.WAITING_FOR_REWARD_EJECTION,TrialSpawnerState.EJECTING_REWARD));
    }
}
