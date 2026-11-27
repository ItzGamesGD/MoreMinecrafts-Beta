package dev.itzgamesgd.moreminecrafts.skytower;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkyTowerLifecycleTest {
    private static SkyTowerLayout.Site site(int x,int r){return new SkyTowerLayout.Site("minecraft:overworld",x,65,0,r);}
    @Test void barrierCellsRotateAndBelongToExactlyOneFloor() {
        for(int r=0;r<4;r++){
            var s=site(200,r);var cells=new java.util.HashSet<SkyTowerLayout.Point>();
            for(int f=0;f<4;f++)for(int i=0;i<18;i++)assertTrue(cells.add(s.barrier(f,i)));
            assertEquals(72,cells.size());
            assertEquals(s.at(39,11,27),s.barrier(0,0));assertEquals(s.at(44,50,27),s.barrier(3,17));
        }
    }
    @Test void destructionAndCooldownRaceIsPermanentAndRecordsFirstCauseWithoutRewards() {
        var d=new SkyTowerData();var s=site(0,0);d.register(s);
        assertTrue(d.recordDestruction(s.id(),s.spawner(0)));
        assertFalse(d.recordVanillaCompletion(s.id(),s.spawner(0)));
        assertTrue(d.recordVanillaCompletion(s.id(),s.spawner(1)));
        assertFalse(d.recordDestruction(s.id(),s.spawner(1)));
        assertFalse(d.recordDestruction(s.id(),site(500,0).spawner(2)));
        assertEquals(3,d.completedMask(s.id()));assertEquals(1,d.destroyedMask(s.id()));
        for(int i=2;i<4;i++)d.recordDestruction(s.id(),s.spawner(i));
        assertTrue(d.floorCleared(s.id(),0));assertFalse(d.floorCleared(s.id(),1));
        var restored=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,SkyTowerData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow()).getOrThrow();
        assertEquals(15,restored.completedMask(s.id()));assertEquals(13,restored.destroyedMask(s.id()));
        assertFalse(restored.recordDestruction(s.id(),s.spawner(0)));
    }
    @Test void ownershipPersistsAndForeignBarrierIsNotAdopted() {
        var d=new SkyTowerData();var a=site(0,0);var b=site(500,1);d.register(a);d.register(b);
        assertFalse(d.ownsBarrier(a.id(),0,0));d.setBarrierOwned(a.id(),0,0,true);
        assertTrue(d.ownsBarrier(a.id(),0,0));assertFalse(d.ownsBarrier(b.id(),0,0));
        var restored=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,SkyTowerData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow()).getOrThrow();
        assertTrue(restored.ownsBarrier(a.id(),0,0));
        restored.setBarrierOwned(a.id(),0,0,false);assertFalse(restored.ownsBarrier(a.id(),0,0));
    }
    @Test void oldSchemaDefaultsCauseAndOwnershipWithoutResettingCompletedBits() {
        var input=com.google.gson.JsonParser.parseString("""
          {"version":1,"quarantined":false,"sites":[{"dimension":"minecraft:overworld","x":0,"y":65,"z":0,"rotation":0,"completed_spawners":15}]}
          """);
        var d=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,input).getOrThrow();
        assertFalse(d.quarantined());assertEquals(15,d.completedMask(site(0,0).id()));
        assertEquals(0,d.destroyedMask(site(0,0).id()));assertFalse(d.ownsBarrier(site(0,0).id(),0,0));
    }
    @Test void adjacentUnreadyEntityStorageDefersNativeQueryFootprintAtChunkBorder() {
        var p=new SkyTowerLayout.Point(16,90,16);
        long absent=net.minecraft.world.level.ChunkPos.pack(0,1);
        assertFalse(SkyTowerBarriers.entityQueryReady(p,chunk->chunk!=absent));
        var queried=new java.util.HashSet<Long>();
        assertTrue(SkyTowerBarriers.entityQueryReady(p,chunk->{queried.add(chunk);return true;}));
        assertEquals(java.util.Set.of(net.minecraft.world.level.ChunkPos.pack(0,0),
            net.minecraft.world.level.ChunkPos.pack(0,1),net.minecraft.world.level.ChunkPos.pack(1,0),
            net.minecraft.world.level.ChunkPos.pack(1,1)),queried);
    }
}
