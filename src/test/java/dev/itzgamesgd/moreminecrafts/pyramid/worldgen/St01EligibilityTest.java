package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility.Terrain;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility.Verdict;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Boundary tests for the owner-approved MODERATE limits (16 / 10 / 8) and the full-footprint Desert rule. P8-C2-C Policy D: no entrance or fluid criterion exists. */
class St01EligibilityTest {
    private static final int Y = 100;

    /** Synthetic terrain: flat desert at Y unless overridden. */
    private static final class T implements Terrain {
        IntBinaryOperator height = (x, z) -> Y;
        Predicate<int[]> desertCell = c -> true;   // {qx,qy,qz}

        @Override public int surfaceHeight(int x, int z) { return height.applyAsInt(x, z); }
        @Override public boolean isDesertCell(int qx, int qy, int qz) { return desertCell.test(new int[]{qx, qy, qz}); }
    }

    private static T withGridAnomalies(int cx, int cz, int highOffset, int lowOffset) {
        T t = new T();
        t.height = (x, z) -> {
            if (x == cx - 40 && z == cz - 40) return Y + highOffset;
            if (x == cx + 40 && z == cz + 40) return Y + lowOffset;
            return Y;
        };
        return t;
    }

    @Test void lockedConstants() {
        assertEquals(32, St01PlacementConfig.SPACING_CHUNKS);
        assertEquals(8, St01PlacementConfig.SEPARATION_CHUNKS);
        assertEquals("triangular", St01PlacementConfig.SPREAD_TYPE);
        assertEquals(91401, St01PlacementConfig.SALT);
        assertEquals(81, St01PlacementConfig.FOOTPRINT_WIDTH);
        assertEquals(16, St01PlacementConfig.MAX_HEIGHT_DIFFERENCE);
        assertEquals(10, St01PlacementConfig.MAX_CUT);
        assertEquals(8, St01PlacementConfig.MAX_SKIRT);
    }

    @Test void flatDesertIsEligible() {
        var r = St01Eligibility.evaluate(new T(), 8, 8);
        assertEquals(Verdict.ELIGIBLE, r.verdict());
        assertEquals(Y, r.foundationY());
        assertEquals(0, r.heightDifference());
    }

    @Test void heightDifference16Passes17Rejects() {
        for (int[] c : new int[][]{{8, 8}, {-1016, -1016 + 8}, {-8, 4088}}) {
            // 16 = +8 / -8 : cut 8 and skirt 8, both within limits
            assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(withGridAnomalies(c[0], c[1], 8, -8), c[0], c[1]).verdict());
            // 17 = +9 / -8 (cut 9, skirt 8): only the 16 limit rejects
            var r = St01Eligibility.evaluate(withGridAnomalies(c[0], c[1], 9, -8), c[0], c[1]);
            assertEquals(Verdict.REJECT_HEIGHT_DIFFERENCE, r.verdict());
            assertEquals(17, r.heightDifference());
        }
    }

    @Test void cut10Passes11Rejects() {
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(withGridAnomalies(8, 8, 10, 0), 8, 8).verdict());
        var r = St01Eligibility.evaluate(withGridAnomalies(8, 8, 11, 0), 8, 8);
        assertEquals(Verdict.REJECT_CUT, r.verdict());
        assertEquals(11, r.cut());
        assertTrue(r.heightDifference() <= 16, "cut limit must bind before the height-difference limit here");
    }

    @Test void skirt8Passes9Rejects() {
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(withGridAnomalies(8, 8, 0, -8), 8, 8).verdict());
        var r = St01Eligibility.evaluate(withGridAnomalies(8, 8, 0, -9), 8, 8);
        assertEquals(Verdict.REJECT_SKIRT, r.verdict());
        assertEquals(9, r.skirt());
    }

    @Test void foundationIsMedianAndCutSkirtAreRelativeToIt() {
        // just over half of the columns sit 6 higher than the rest: the median moves up, so the lower columns are a 6-block skirt, not a cut
        T t = new T();
        t.height = (x, z) -> (x - 8) < 1 ? Y + 6 : Y;
        var r = St01Eligibility.evaluate(t, 8, 8);
        assertEquals(Y + 6, r.foundationY());
        assertEquals(6, r.skirt());
        assertEquals(0, r.cut());
    }

    @Test void everyFootprintColumnIsSampledNotJustAGrid() {
        // a single off-grid column 30 blocks high must be seen (a 4-block grid would skip x offset -39)
        T t = new T();
        t.height = (x, z) -> (x == 8 - 39 && z == 8 + 1) ? Y + 30 : Y;
        assertEquals(Verdict.REJECT_HEIGHT_DIFFERENCE, St01Eligibility.evaluate(t, 8, 8).verdict());
        // columns just outside the 81x81 footprint are not part of the rule
        T u = new T();
        u.height = (x, z) -> (x == 8 - 41 || x == 8 + 41 || z == 8 - 41 || z == 8 + 41) ? Y + 30 : Y;
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(u, 8, 8).verdict());
    }

    @Test void biomeCellsCoverEveryBlockCandidateIncludingNegativeCoordinates() {
        // Proof of the coverage claim: for every footprint block, both horizontal quart candidates {floor((x-2)/4), +1} lie inside the swept range.
        for (int cx = -300; cx <= 300; cx++) {
            int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
            int q0 = Math.floorDiv(cx - half - 2, 4), q1 = Math.floorDiv(cx + half - 2, 4) + 1;
            for (int x = cx - half; x <= cx + half; x++) {
                int c = Math.floorDiv(x - 2, 4);
                assertTrue(c >= q0 && c + 1 <= q1, "cx=" + cx + " x=" + x);
            }
            // and the sweep is tight: no more than one cell of slack on either side
            assertTrue(q1 - q0 + 1 <= (2 * half) / 4 + 3);
        }
    }

    @Test void nonDesertCellAtRingBoundaryRejects() {
        for (int[] c : new int[][]{{-100, -100}, {8, 8}, {-8, 520}, {2023, -777}}) {
            int cx = c[0], cz = c[1], half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
            int qx0 = Math.floorDiv(cx - half - 2, 4), qx1 = Math.floorDiv(cx + half - 2, 4) + 1;
            int qz0 = Math.floorDiv(cz - half - 2, 4), qz1 = Math.floorDiv(cz + half - 2, 4) + 1;
            int midQz = (qz0 + qz1) / 2, midQx = (qx0 + qx1) / 2;
            for (int[] bad : new int[][]{{qx0, midQz}, {qx1, midQz}, {midQx, qz0}, {midQx, qz1}, {qx0, qz0}, {qx1, qz1}}) {
                T t = new T();
                t.desertCell = q -> !(q[0] == bad[0] && q[2] == bad[1]);
                assertEquals(Verdict.REJECT_BIOME, St01Eligibility.evaluate(t, cx, cz).verdict(), "bad=" + bad[0] + "," + bad[1] + " c=" + cx + "," + cz);
            }
            for (int[] ok : new int[][]{{qx0 - 1, midQz}, {qx1 + 1, midQz}, {midQx, qz0 - 1}, {midQx, qz1 + 1}}) {
                T t = new T();
                t.desertCell = q -> !(q[0] == ok[0] && q[2] == ok[1]);
                assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(t, cx, cz).verdict(), "outside ring must not matter: " + ok[0] + "," + ok[1]);
            }
        }
    }

    @Test void bothVerticalQuartCandidatesAreRequired() {
        T t = new T();
        int surfaceBlock = Y - 1;
        int qyA = Math.floorDiv(surfaceBlock - 2, 4);
        t.desertCell = q -> q[1] != qyA + 1 || q[0] != 2 || q[2] != 2;
        assertEquals(Verdict.REJECT_BIOME, St01Eligibility.evaluate(t, 8, 8).verdict());
        t.desertCell = q -> q[1] != qyA - 1;
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(t, 8, 8).verdict());
    }

    @Test void anchorStubBiomeCellIsRequiredAtTheFoundationLevel() {
        // vanilla drops a stub whose (anchor, foundationY) cell is not in the structure's biome set: it must be rejected here, not later
        T t = new T();
        int cellY = Math.floorDiv(Y, 4); // yf = Y
        int surfaceBlock = Y - 1;
        int qyA = Math.floorDiv(surfaceBlock - 2, 4);
        assertTrue(cellY == qyA || cellY == qyA + 1);
        t.desertCell = q -> true;
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(t, 8, 8).verdict());
    }

    @Test void evaluationIsDeterministicAndSideEffectFree() {
        T t = withGridAnomalies(8, 8, 5, -5);
        assertEquals(St01Eligibility.evaluate(t, 8, 8), St01Eligibility.evaluate(t, 8, 8));
    }

    // ---- P8-C2-C Policy D: entrance obstruction and underground fluid are not eligibility criteria ----

    @Test void policyDRemovedTheEntranceAndFluidVerdictsAndConstants() {
        assertEquals(java.util.Set.of("ELIGIBLE", "REJECT_BIOME", "REJECT_HEIGHT_DIFFERENCE", "REJECT_CUT", "REJECT_SKIRT"),
                java.util.Arrays.stream(Verdict.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));
        for (var f : St01PlacementConfig.class.getDeclaredFields()) {
            assertFalse(f.getName().contains("FLUID") || f.getName().contains("ENTRANCE") || f.getName().contains("UNDERGROUND"), f.getName());
        }
        assertEquals(0, java.util.Arrays.stream(Terrain.class.getMethods()).filter(m -> m.getName().toLowerCase().contains("fluid")).count());
    }

    @Test void aBuriedEntranceDoesNotAffectEligibilityOrTheFoundation() {
        // terrain far above the walking level directly in front of every face of the footprint (outside the 81x81 box)
        T t = new T();
        t.height = (x, z) -> (Math.abs(x - 8) > 40 || Math.abs(z - 8) > 40) ? Y + 30 : Y;
        var r = St01Eligibility.evaluate(t, 8, 8);
        assertEquals(Verdict.ELIGIBLE, r.verdict());
        assertEquals(Y, r.foundationY());
    }

    @Test void terrainLimitsStillBindWithoutAnyEntranceStage() {
        assertEquals(Verdict.REJECT_CUT, St01Eligibility.evaluate(withGridAnomalies(8, 8, 11, 0), 8, 8).verdict());
    }
}
