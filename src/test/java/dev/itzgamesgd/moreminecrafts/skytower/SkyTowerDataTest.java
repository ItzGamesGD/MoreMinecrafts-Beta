package dev.itzgamesgd.moreminecrafts.skytower;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkyTowerDataTest {
    @Test void saveRestorePreservesTwoIndependentInstances() {
        var d=new SkyTowerData();
        var a=new SkyTowerLayout.Site("minecraft:overworld",0,70,0,0);
        var b=new SkyTowerLayout.Site("minecraft:overworld",200,70,0,0);
        assertTrue(d.register(a));assertTrue(d.register(b));
        assertTrue(d.recordVanillaCompletion(a.id(),a.spawner(0)));
        assertFalse(d.recordVanillaCompletion(a.id(),b.spawner(0)));
        assertFalse(d.recordVanillaCompletion(a.id(),a.spawner(0)));
        var json=SkyTowerData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow();
        var restored=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        assertEquals(1,restored.completedMask(a.id()));assertEquals(0,restored.completedMask(b.id()));
        assertFalse(restored.register(a));assertEquals(1,restored.completedMask(a.id()));
    }
    @Test void conflictingAndFutureSaveRecordsQuarantineInsteadOfUnlocking() {
        var json=JsonParser.parseString("""
          {"version":1,"quarantined":false,"sites":[
           {"dimension":"minecraft:overworld","x":0,"y":70,"z":0,"rotation":0,"completed_spawners":1},
           {"dimension":"minecraft:overworld","x":0,"y":70,"z":0,"rotation":0,"completed_spawners":65535}]}
          """);
        var d=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        assertTrue(d.quarantined());assertEquals(1,d.completedMask("minecraft:overworld|0,70,0|0"));
        assertFalse(d.recordVanillaCompletion("minecraft:overworld|0,70,0|0",new SkyTowerLayout.Point(17,82,31)));
        var again=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,SkyTowerData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow()).getOrThrow();
        assertTrue(again.quarantined());
        var future=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"version\":2,\"quarantined\":false,\"sites\":[]}")).getOrThrow();
        assertTrue(future.quarantined());
    }
    @Test void overlappingRegistrationsAreRejected() {
        var d=new SkyTowerData();d.register(new SkyTowerLayout.Site("minecraft:overworld",0,70,0,0));
        assertThrows(IllegalArgumentException.class,()->d.register(new SkyTowerLayout.Site("minecraft:overworld",1,70,1,0)));
    }
    @Test void malformedFieldsAndHeadersNeverFallBackToWritableEmptyRegistry() {
        for(String raw:new String[]{"{}", "{\"version\":\"broken\",\"quarantined\":false,\"sites\":[]}",
            "{\"version\":1,\"quarantined\":false,\"sites\":[{\"dimension\":\"minecraft:overworld\",\"x\":0,\"y\":70,\"z\":0,\"rotation\":0,\"completed_spawners\":\"broken\"}]}"}) {
            var d=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(raw)).resultOrPartial(e->{}).orElseGet(SkyTowerData::new);
            assertTrue(d.quarantined(),raw);
            assertThrows(IllegalStateException.class,()->d.register(new SkyTowerLayout.Site("minecraft:overworld",0,70,0,0)));
            var json=SkyTowerData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow();
            assertFalse(json.getAsJsonObject().get("quarantine_payload").getAsString().isBlank());
            var again=SkyTowerData.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
            assertTrue(again.quarantined());
        }
    }
}
