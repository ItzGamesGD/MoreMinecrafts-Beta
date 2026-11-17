package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class St03FullTerrainCheckTest {
    @Test void acceptsInclusiveCutSkirtAndFootingBoundaries() {
        var r=St03FullTerrainCheck.evaluate((x,z)->x==0?74:52,(x,z)->48,0,0,64);
        assertTrue(r.eligible());assertEquals(12544,r.columnsChecked());
        assertEquals(10,r.cut());assertEquals(12,r.skirt());assertEquals(16,r.footingFill());
    }
    @Test void rejectsACliffBetweenTheApprovedEightBlockSamples() {
        var r=St03FullTerrainCheck.evaluate((x,z)->x==1&&z==1?75:64,(x,z)->64,0,0,64);
        assertEquals(St03FullTerrainCheck.Verdict.CUT,r.verdict());assertEquals(11,r.cut());
    }
    @Test void rejectsUnsampledSkirtAndDeepWaterFloor() {
        assertEquals(St03FullTerrainCheck.Verdict.SKIRT,St03FullTerrainCheck.evaluate(
                (x,z)->x==1&&z==1?51:64,(x,z)->64,0,0,64).verdict());
        assertEquals(St03FullTerrainCheck.Verdict.FOOTING_FILL,St03FullTerrainCheck.evaluate(
                (x,z)->64,(x,z)->x==1&&z==1?47:64,0,0,64).verdict());
    }
    @Test void inspectsLastColumnAndNeverTouchesOutsideFootprint() {
        var r=St03FullTerrainCheck.evaluate((x,z)->{
            assertTrue(x>=-160&&x<=-49&&z>=32&&z<=143);
            return x==-49&&z==143?75:64;
        },(x,z)->64,-160,32,64);
        assertEquals(St03FullTerrainCheck.Verdict.CUT,r.verdict());assertEquals(12544,r.columnsChecked());
    }
}
