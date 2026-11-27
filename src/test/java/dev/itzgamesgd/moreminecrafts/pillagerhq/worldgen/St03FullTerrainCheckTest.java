package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class St03FullTerrainCheckTest {
    @Test void acceptsInclusiveCutSkirtAndFootingBoundariesAndPreservesValidSampleY() {
        var r = St03FullTerrainCheck.evaluate((x, z) -> x == 8 ? 74 : 52, (x, z) -> 48, 0, 0, 64);
        assertTrue(r.eligible()); assertEquals(9216, r.columnsChecked());
        assertEquals(64, r.foundationY());
        assertEquals(10, r.cut()); assertEquals(12, r.skirt()); assertEquals(16, r.footingFill());
    }
    @Test void recomputesFoundationYClosestToSampleWhenSpanFitsButSampleYWasOffByOne() {
        // Dense max=75, min=53, floorMin=50 -> valid Y range is [65, 65]. Sample Y was 64 -> recomputed Y is 65.
        var r = St03FullTerrainCheck.evaluate((x, z) -> x == 9 && z == 9 ? 75 : (x == 10 && z == 10 ? 53 : 64), (x, z) -> 50, 0, 0, 64);
        assertTrue(r.eligible());
        assertEquals(65, r.foundationY());
        assertEquals(10, r.cut()); assertEquals(12, r.skirt()); assertEquals(15, r.footingFill());
    }
    @Test void rejectsACliffBetweenTheApprovedEightBlockSamplesWhenSpanExceeds22() {
        var r = St03FullTerrainCheck.evaluate((x, z) -> x == 9 && z == 9 ? 75 : (x == 10 && z == 10 ? 52 : 64), (x, z) -> 64, 0, 0, 64);
        assertEquals(St03FullTerrainCheck.Verdict.CUT, r.verdict()); assertEquals(11, r.cut());
    }
    @Test void rejectsUnsampledSkirtAndDeepWaterFloorWhenSpanIsImpossible() {
        assertEquals(St03FullTerrainCheck.Verdict.SKIRT, St03FullTerrainCheck.evaluate(
                (x, z) -> x == 9 && z == 9 ? 51 : (x == 10 && z == 10 ? 74 : 64), (x, z) -> 64, 0, 0, 64).verdict());
        assertEquals(St03FullTerrainCheck.Verdict.FOOTING_FILL, St03FullTerrainCheck.evaluate(
                (x, z) -> x == 10 && z == 10 ? 74 : 64, (x, z) -> x == 9 && z == 9 ? 47 : 64, 0, 0, 64).verdict());
    }
    @Test void inspectsCentral96x96AndNeverTouchesOuterEightBlockRing() {
        var r = St03FullTerrainCheck.evaluate((x, z) -> {
            assertTrue(x >= -152 && x <= -57 && z >= 40 && z <= 135);
            return x == -57 && z == 135 ? 75 : (x == -152 && z == 40 ? 52 : 64);
        }, (x, z) -> 64, -160, 32, 64);
        assertEquals(St03FullTerrainCheck.Verdict.CUT, r.verdict()); assertEquals(9216, r.columnsChecked());
    }
    @Test void detectsBoundaryWaterCutContactOnAllFourEdgesAndGatehouse() {
        // Dry land or water below foundationY+1 (surface <= foundationY+1) does not trigger rejection
        assertFalse(St03FullTerrainCheck.hasBoundaryWaterCutContact((x, z) -> 63, (x, z) -> 63, 0, 0, 62));
        assertFalse(St03FullTerrainCheck.hasBoundaryWaterCutContact((x, z) -> 63, (x, z) -> 55, 0, 0, 62));
        // Water with surface >= foundationY+2 (top water block y = s-1 >= foundationY+1) on North, South (gatehouse), West, East
        assertTrue(St03FullTerrainCheck.hasBoundaryWaterCutContact(
                (x, z) -> (x == 50 && z == -1) ? 63 : 64, (x, z) -> (x == 50 && z == -1) ? 58 : 64, 0, 0, 61));
        assertTrue(St03FullTerrainCheck.hasBoundaryWaterCutContact(
                (x, z) -> (x == 55 && z == 112) ? 63 : 64, (x, z) -> (x == 55 && z == 112) ? 58 : 64, 0, 0, 61));
        assertTrue(St03FullTerrainCheck.hasBoundaryWaterCutContact(
                (x, z) -> (x == -1 && z == 40) ? 63 : 64, (x, z) -> (x == -1 && z == 40) ? 58 : 64, 0, 0, 61));
        assertTrue(St03FullTerrainCheck.hasBoundaryWaterCutContact(
                (x, z) -> (x == 112 && z == 80) ? 63 : 64, (x, z) -> (x == 112 && z == 80) ? 58 : 64, 0, 0, 61));
    }
}
