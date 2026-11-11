package dev.itzgamesgd.moreminecrafts.skytower;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Server-thread reconciliation. No chunk forcing, foreign block deletion, protection, or rewards. */
public final class SkyTowerBarriers {
    private SkyTowerBarriers() {}
    private static final Map<String,String> conflicts=new HashMap<>();
    public static void clearDiagnostics(){conflicts.clear();}
    public static void tick(MinecraftServer server){
        if(server.getTickCount()%20!=0)return;
        var data=server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        if(data.quarantined() || data.sites().isEmpty())return;
        for(var level:server.getAllLevels())for(var entry:data.sites().entrySet())
            if(entry.getValue().dimension().equals(level.dimension().identifier().toString()))
                for(int floor=0;floor<4;floor++)reconcile(level,data,entry.getValue(),floor);
    }
    public static boolean reconcile(ServerLevel level,SkyTowerData data,SkyTowerLayout.Site site,int floor){
        if(data.quarantined())return false;
        String key=site.id()+"/"+floor;
        boolean complete=data.floorCleared(site.id(),floor);
        for(int i=0;i<18;i++){
            var cell=site.barrier(floor,i);var p=pos(cell);
            if(!level.hasChunkAt(p))return false;
            if(!complete && !entityQueryReady(cell,chunk->level.areEntitiesLoaded(chunk)
                && level.getChunkSource().hasChunk(net.minecraft.world.level.ChunkPos.getX(chunk),net.minecraft.world.level.ChunkPos.getZ(chunk))))return false;
        }
        // Revoke stale ownership rather than adopting an unexpected block.
        for(int i=0;i<18;i++){
            var p=pos(site.barrier(floor,i));
            if(data.ownsBarrier(site.id(),floor,i) && !level.getBlockState(p).equals(Blocks.BARRIER.defaultBlockState()))
                data.setBarrierOwned(site.id(),floor,i,false);
        }
        if(complete){
            boolean ok=true;
            for(int i=0;i<18;i++)if(data.ownsBarrier(site.id(),floor,i)){
                var p=pos(site.barrier(floor,i));
                if(level.getBlockState(p).equals(Blocks.BARRIER.defaultBlockState())){
                    // Hook revokes ownership only on successful replacement.
                    if(!level.setBlock(p,Blocks.AIR.defaultBlockState(),3))ok=false;
                }
            }
            for(int i=0;i<18;i++)if(!level.getBlockState(pos(site.barrier(floor,i))).isAir())
                return conflict(key,"released floor contains foreign block at "+pos(site.barrier(floor,i)));
            if(ok)conflicts.remove(key);return ok;
        }
        // Preflight the entire floor: never install a partial wall around an entity or foreign block.
        for(int i=0;i<18;i++){
            var p=pos(site.barrier(floor,i));var state=level.getBlockState(p);
            if(data.ownsBarrier(site.id(),floor,i) && state.equals(Blocks.BARRIER.defaultBlockState()))continue;
            if(!state.isAir() || level.getBlockEntity(p)!=null)return conflict(key,"foreign block at "+p);
            if(!level.getEntities(null,new AABB(p)).isEmpty())return conflict(key,"entity at "+p);
        }
        for(int i=0;i<18;i++){
            var p=pos(site.barrier(floor,i));
            if(data.ownsBarrier(site.id(),floor,i))continue;
            if(!level.setBlock(p,Blocks.BARRIER.defaultBlockState(),3))return conflict(key,"placement failed at "+p);
            data.setBarrierOwned(site.id(),floor,i,true);
        }
        conflicts.remove(key);return true;
    }
    /** Mirrors 26.3 EntitySectionStorage's horizontal query padding (min-2, max+2).
     * A body may cross a chunk edge even though its centre belongs to the neighbor.
     */
    static boolean entityQueryReady(SkyTowerLayout.Point cell,java.util.function.LongPredicate ready){
        for(int x=Math.floorDiv(cell.x()-2,16);x<=Math.floorDiv(cell.x()+3,16);x++)
            for(int z=Math.floorDiv(cell.z()-2,16);z<=Math.floorDiv(cell.z()+3,16);z++)
                if(!ready.test(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;
        return true;
    }
    private static boolean conflict(String key,String reason){
        if(!reason.equals(conflicts.put(key,reason)))MoreMinecrafts.LOGGER.warn("[sky-tower barrier] {} deferred: {}",key,reason);
        return false;
    }
    public static BlockPos pos(SkyTowerLayout.Point p){return new BlockPos(p.x(),p.y(),p.z());}
}
