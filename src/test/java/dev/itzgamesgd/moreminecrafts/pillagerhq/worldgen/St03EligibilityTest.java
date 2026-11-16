package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.*;
import dev.itzgamesgd.moreminecrafts.pillagerhq.St03SiteV2Rules;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class St03EligibilityTest {
    /** The frozen measurement evaluator is copied byte-for-byte from 68cde591 as a test oracle. */
    private static final class Terrain implements St03Eligibility.Terrain {
        int good = 225, edge, water;
        boolean center = true;
        int[] heights = new int[225], floors = new int[225];
        Terrain() { Arrays.fill(heights, 64); Arrays.fill(floors, 64); }
        int index(int x, int z) {
            int a = x == 111 ? 14 : x >= 0 && x <= 104 && x % 8 == 0 ? x / 8 : -1;
            int b = z == 111 ? 14 : z >= 0 && z <= 104 && z % 8 == 0 ? z / 8 : -1;
            return a < 0 || b < 0 ? -1 : a * 15 + b;
        }
        public int surfaceHeight(int x, int z) { int i = index(x,z); return i < 0 ? 64 : heights[i]; }
        public int oceanFloorHeight(int x, int z) { return floors[index(x,z)]; }
        int biomeIndex(int qx,int qz) { return ((qx + 1) / 2) * 15 + (qz + 1) / 2; }
        public boolean isEligibleBiomeCell(int qx,int qy,int qz) {
            if (qx == 14 && qz == 14) return center;
            return biomeIndex(qx,qz) < good;
        }
        public boolean isEdgeBiomeCell(int qx,int qy,int qz) {
            if (qx == 14 && qz == 14) return !center;
            int i = biomeIndex(qx,qz); return i >= good && i < good + edge;
        }
        St03Eligibility.Result evaluate() { return St03Eligibility.evaluate(this,0,0); }
        St03SiteV2Rules.Decision measured() {
            int[] sorted = St03SiteV2Rules.sorted(heights);
            int wet = 0; for(int i=0;i<225;i++) if(heights[i]>floors[i]+1) wet++;
            var sample = new St03SiteV2Rules.Sample(center ? 0 : 3,new int[]{good,good,good},edge,225,wet,225,
                    sorted[0],St03SiteV2Rules.percentile(sorted,.05),sorted[112],St03SiteV2Rules.percentile(sorted,.95),
                    sorted[224],Arrays.stream(floors).min().orElseThrow(),heights);
            return St03SiteV2Rules.evaluate(sample,0,80,5,true,true,St03SiteV2Rules.LIMITS[2]);
        }
    }
    @Test void offsetsIncludeEveryApprovedSampleAndFarBoundary() {
        assertArrayEquals(new int[]{0,8,16,24,32,40,48,56,64,72,80,88,96,104,111},St03Eligibility.sampleOffsets());
    }
    @Test void exactEightyPercentAndNeutralDenominator() {
        Terrain t=new Terrain();t.good=180;assertTrue(t.evaluate().eligible());
        t.good=179;assertEquals(St03Eligibility.Verdict.REJECT_FOOTPRINT_BIOME,t.evaluate().verdict());
        t.good=160;t.edge=25;assertTrue(t.evaluate().eligible());
        t.good=159;assertFalse(t.evaluate().eligible());
        t.good=0;t.edge=225;assertEquals(St03Eligibility.Verdict.REJECT_FOOTPRINT_BIOME,t.evaluate().verdict());
    }
    @Test void edgeCenterNeverBecomesEligible() {
        Terrain t=new Terrain();t.center=false;
        assertEquals(St03Eligibility.Verdict.REJECT_CENTER_BIOME,t.evaluate().verdict());
    }
    @Test void elevenWaterSamplesPassButTwelveExceedFivePercentOf225() {
        Terrain t=new Terrain();for(int i=0;i<11;i++)t.floors[i]=62;
        assertTrue(t.evaluate().eligible());
        t.floors[11]=62;assertEquals(St03Eligibility.Verdict.REJECT_WATER,t.evaluate().verdict());
    }
    @Test void oneBlockWaterRetainsMeasuredDefinition() {
        Terrain t=new Terrain();Arrays.fill(t.floors,63);assertTrue(t.evaluate().eligible());
    }
    @Test void percentileBoundaryAndOptimisedFoundation() {
        Terrain t=new Terrain();for(int i=180;i<225;i++)t.heights[i]=t.floors[i]=84;
        var r=t.evaluate();assertTrue(r.eligible());assertEquals(20,r.heightDifference());assertEquals(74,r.foundationY());
        for(int i=180;i<225;i++)t.heights[i]=t.floors[i]=85;
        assertEquals(St03Eligibility.Verdict.REJECT_HEIGHT_DIFFERENCE,t.evaluate().verdict());
    }
    @Test void trimmedOutlierMustStillFitFullExtremaBounds() {
        Terrain t=new Terrain();t.heights[0]=t.floors[0]=86;
        var r=t.evaluate();assertTrue(r.eligible());assertEquals(0,r.heightDifference());assertEquals(76,r.foundationY());
        t.heights[0]=t.floors[0]=87;
        assertEquals(St03Eligibility.Verdict.REJECT_CUT_SKIRT,t.evaluate().verdict());
    }
    @Test void waterFillCanLowerFoundationButCannotIgnoreDeepFloor() {
        Terrain t=new Terrain();t.floors[0]=47;
        var r=t.evaluate();assertTrue(r.eligible());assertEquals(63,r.foundationY());assertEquals(16,r.footingFill());
        t.floors[0]=33;assertEquals(St03Eligibility.Verdict.REJECT_FOOTING_FILL,t.evaluate().verdict());
    }
    @Test void matchesFrozenMeasurementEvaluatorAcrossDeterministicMixedInputs() {
        Random random=new Random(91403);int accepted=0;
        for(int test=0;test<1000;test++) {
            Terrain t=new Terrain();t.center=random.nextInt(10)!=0;t.good=125+random.nextInt(101);
            t.edge=random.nextInt(226-t.good);int span=random.nextInt(26);
            for(int i=0;i<225;i++)t.heights[i]=t.floors[i]=64+random.nextInt(span+1);
            if(test%7==0)t.heights[0]=t.floors[0]=100;
            int wet=random.nextInt(16);for(int i=0;i<wet;i++)t.floors[i]=t.heights[i]-2-random.nextInt(25);
            var expected=t.measured();var actual=t.evaluate();
            String verdict=actual.eligible()?"PASS":actual.verdict().name().replace("REJECT_","").replace("HEIGHT_DIFFERENCE","HEIGHT");
            assertEquals(expected.verdict(),verdict,"case "+test);
            if(actual.eligible()) {
                accepted++;assertEquals(expected.foundation(),actual.foundationY());assertEquals(expected.cut(),actual.cut());
                assertEquals(expected.skirt(),actual.skirt());assertEquals(expected.fill(),actual.footingFill());
            }
        }
        assertTrue(accepted>0,"Parity suite must exercise successful sites too");
    }
}
