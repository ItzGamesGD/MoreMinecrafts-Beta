package dev.itzgamesgd.moreminecrafts.skytower;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Receives committed chunk mutations, including player breaks, commands and explosions.
 * Attempts/cancelled changes never reach this path. No missing-block polling or reward synthesis.
 */
public final class SkyTowerBlockChanges {
    private SkyTowerBlockChanges() {}
    public static void committed(Level world,BlockPos pos,BlockState before,BlockState after){
        if(!(world instanceof ServerLevel level) || !level.getServer().isSameThread() || before==null || before.equals(after))return;
        if(!before.is(Blocks.TRIAL_SPAWNER) && !before.is(Blocks.BARRIER))return;
        var d=level.getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        if(d.quarantined() || d.sites().isEmpty())return;
        var point=new SkyTowerLayout.Point(pos.getX(),pos.getY(),pos.getZ());
        String dimension=level.dimension().identifier().toString();
        for(var e:d.sites().entrySet()){
            var site=e.getValue();if(!site.dimension().equals(dimension))continue;
            if(before.is(Blocks.TRIAL_SPAWNER) && !after.is(Blocks.TRIAL_SPAWNER))d.recordDestruction(e.getKey(),point);
            if(before.is(Blocks.BARRIER) && !after.is(Blocks.BARRIER))
                for(int f=0;f<4;f++)for(int i=0;i<18;i++)
                    if(site.barrier(f,i).equals(point) && d.ownsBarrier(e.getKey(),f,i))d.setBarrierOwned(e.getKey(),f,i,false);
        }
    }
}
