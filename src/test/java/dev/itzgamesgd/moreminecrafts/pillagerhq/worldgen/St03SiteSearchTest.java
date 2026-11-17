package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class St03SiteSearchTest {
    private static St03Eligibility.Terrain terrain(Set<String> centers,boolean hill) {
        return new St03Eligibility.Terrain() {
            public int surfaceHeight(int x,int z) { return hill && x<32 ? 74 : 64; }
            public int oceanFloorHeight(int x,int z) { return surfaceHeight(x,z); }
            public boolean isEligibleBiomeCell(int qx,int qy,int qz) {
                if(Math.floorMod(qx,2)==0 && Math.floorMod(qz,2)==0)return centers.contains(qx+","+qz);
                return true;
            }
        };
    }
    @Test void canMoveTwoChunksAndDoesNotUseThreeChunkSites() {
        var r=St03SiteSearch.select(terrain(Set.of("22,14","26,14"),false),0,0).orElseThrow();
        assertEquals(2,r.dxChunks());assertEquals(0,r.dzChunks());assertEquals(32,r.originX());
        assertTrue(St03SiteSearch.select(terrain(Set.of("26,14"),false),0,0).isEmpty());
    }
    @Test void measuredNegativeDxThenDzBreaksEqualQualityDistanceTies() {
        var r=St03SiteSearch.select(terrain(Set.of("10,14","14,18"),false),0,0).orElseThrow();
        assertEquals(-1,r.dxChunks());assertEquals(0,r.dzChunks());
    }
    @Test void lowerEarthworkWinsBeforeCloserDistance() {
        var r=St03SiteSearch.select(terrain(Set.of("10,14","22,14"),true),0,0).orElseThrow();
        assertEquals(2,r.dxChunks());assertEquals(0,r.result().cut());
    }
    @Test void repeatedSearchHasTheSameResultAtPositiveAndNegativeOrigins() {
        for(int origin:new int[]{0,16000,-16000}) {
            int q=origin/4;
            var terrain=terrain(Set.of((q+10)+","+(q+14),(q+14)+","+(q+18)),false);
            var expected=St03SiteSearch.select(terrain,origin,origin).orElseThrow();
            assertEquals(-1,expected.dxChunks());assertEquals(0,expected.dzChunks());
            for(int repeat=0;repeat<25;repeat++)
                assertEquals(expected,St03SiteSearch.select(terrain,origin,origin).orElseThrow());
        }
    }
    @Test void rejectsTheBestUnsampledCliffAndUsesTheNextMeasuredCandidate() {
        var biomes=terrain(Set.of("10,14","14,18"),false);
        var t=new St03Eligibility.Terrain() {
            public int surfaceHeight(int x,int z){return x==-15&&z==1?75:64;}
            public int oceanFloorHeight(int x,int z){return 64;}
            public boolean isEligibleBiomeCell(int x,int y,int z){return biomes.isEligibleBiomeCell(x,y,z);}
        };
        var first=St03SiteSearch.select(t,0,0).orElseThrow();assertEquals(-1,first.dxChunks());
        var next=St03SiteSearch.select(t,0,0,site->St03FullTerrainCheck.evaluate(t::surfaceHeight,t::oceanFloorHeight,
                site.originX(),site.originZ(),site.result().foundationY()).eligible()).orElseThrow();
        assertEquals(0,next.dxChunks());assertEquals(1,next.dzChunks());
        assertTrue(St03SiteSearch.select(t,0,0,site->false).isEmpty());
    }
}
