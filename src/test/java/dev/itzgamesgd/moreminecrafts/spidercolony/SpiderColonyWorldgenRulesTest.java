package dev.itzgamesgd.moreminecrafts.spidercolony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpiderColonyWorldgenRulesTest {
    private static boolean[][][] emptyGrid(int side) {
        return new boolean[side][side][SpiderColonyDetector.Y_SIZE];
    }

    private static void carveSphere(boolean[][][] air, int cx, int cyLocal, int cz, int radius) {
        int side = air.length;
        int r2 = radius * radius;
        for (int x = 0; x < side; x++) {
            for (int z = 0; z < side; z++) {
                for (int y = 0; y < SpiderColonyDetector.Y_SIZE; y++) {
                    int dx = x - cx;
                    int dy = y - cyLocal;
                    int dz = z - cz;
                    if (dx * dx + dy * dy + dz * dz <= r2) {
                        air[x][z][y] = true;
                    }
                }
            }
        }
    }

    @Test
    void detectsQualifyingInscribedSphereAndExcludes16BlockStructureBuffer() {
        int side = 48;
        boolean[][][] air = emptyGrid(side);
        // Carve a sphere of radius 12 at local (24, 48, 24) -> world (24, 0, 24)
        carveSphere(air, 24, 48, 24, 12);

        List<SpiderColonyDetector.CoreCandidate> found = SpiderColonyDetector.detect(air, side, 0, 0, List.of());
        assertEquals(1, found.size());
        SpiderColonyDetector.CoreCandidate c = found.getFirst();
        assertEquals(24, c.worldX());
        assertEquals(0, c.worldY());
        assertEquals(24, c.worldZ());
        assertTrue(c.diameterBlocks() >= 20.0, "expected diameter >= 20, got " + c.diameterBlocks());

        // Structure box 15 blocks outside sphere radius should still intersect with 16-block buffer
        var nearbyBox = new SpiderColonyDetector.Box3i(24 + 12 + 15, -2, 23, 24 + 12 + 20, 2, 25);
        List<SpiderColonyDetector.CoreCandidate> blocked = SpiderColonyDetector.detect(air, side, 0, 0, List.of(nearbyBox));
        assertTrue(blocked.isEmpty(), "16-block structure buffer must exclude overlapping core");
    }

    @Test
    void boundaryTruncationClampsClearanceNearSampleEdge() {
        int side = 48;
        boolean[][][] air = emptyGrid(side);
        // Carve a sphere of radius 12 centered only 6 blocks from x=0 boundary -> truncated by sample edge
        carveSphere(air, 6, 48, 24, 12);
        List<SpiderColonyDetector.CoreCandidate> found = SpiderColonyDetector.detect(air, side, 0, 0, List.of());
        assertTrue(found.isEmpty(), "truncated sphere near boundary must not reach radius 10 clearance");
    }

    @Test
    void placementRulesEnforce15Km2FrequencyAndNonOverlapping128To256Range() {
        assertEquals(77, SpiderColonyPlacementRules.REGION_SIDE_CHUNKS);
        for (int rx = -3; rx <= 3; rx++) {
            for (int rz = -3; rz <= 3; rz++) {
                int count = SpiderColonyPlacementRules.targetColoniesForRegion(12345L, rx, rz);
                assertTrue(count >= 1 && count <= 3, "expected 1-3 colonies per 1.5 km^2, got " + count);
            }
        }
        int r = SpiderColonyPlacementRules.nestRadiusForSite(12345L, 100, -10, 200);
        assertTrue(r >= 128 && r <= 256, "expected nest radius in [128, 256], got " + r);

        var existing = List.of(new ColonyLedger.RecordedColony("c1", 0, -10, 0, 150, 0, 0));
        assertTrue(SpiderColonyPlacementRules.overlapsAny(200, 0, 128, existing));
        assertFalse(SpiderColonyPlacementRules.overlapsAny(300, 0, 128, existing));
    }

    @Test
    void stage3TaperAndStructureBufferPointCheck() {
        assertEquals(1.0, SpiderColonyWorldgen.taperWeight(10.0, 20.0, 56.0), 1e-9);
        assertEquals(0.5, SpiderColonyWorldgen.taperWeight(38.0, 20.0, 56.0), 1e-9);
        assertEquals(0.0, SpiderColonyWorldgen.taperWeight(56.0, 20.0, 56.0), 1e-9);

        var box = new SpiderColonyDetector.Box3i(0, -10, 0, 10, 10, 10).expand(16);
        assertTrue(box.containsPoint(-16, 0, 0));
        assertFalse(box.containsPoint(-17, 0, 0));
    }
}
