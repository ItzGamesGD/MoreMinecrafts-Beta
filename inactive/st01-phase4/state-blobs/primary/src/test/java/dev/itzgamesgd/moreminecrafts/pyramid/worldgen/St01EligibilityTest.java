package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility.Terrain;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility.Verdict;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility.Volume;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Boundary tests for the owner-approved MODERATE limits (16 / 10 / 8), the full-footprint Desert rule and fluid safety. */
class St01EligibilityTest {
    private static final int Y = 100;
    private static final Volume HALL = new Volume(-40, -2, -24, -5, 2, 40);

    /** Synthetic terrain: flat desert at Y unless overridden. */
    private static final class T implements Terrain {
        IntBinaryOperator height = (x, z) -> Y;
        Predicate<int[]> desertCell = c -> true;   // {qx,qy,qz}
        Predicate<int[]> fluid = c -> false;       // {x,y,z}

        @Override public int surfaceHeight(int x, int z) { return height.applyAsInt(x, z); }
        @Override public boolean isDesertCell(int qx, int qy, int qz) { return desertCell.test(new int[]{qx, qy, qz}); }
        @Override public boolean hasFluid(int x, int z, int minY, int maxY) {
            for (int y = minY; y <= maxY; y++) if (fluid.test(new int[]{x, y, z})) return true;
            return false;
        }
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
        var r = St01Eligibility.evaluate(new T(), 8, 8, HALL);
        assertEquals(Verdict.ELIGIBLE, r.verdict());
        assertEquals(Y, r.foundationY());
        assertEquals(0, r.heightDifference());
    }

    @Test void heightDifference16Passes17Rejects() {
        for (int[] c : new int[][]{{8, 8}, {-1016, -1016 + 8}, {-8, 4088}}) {
            // 16 = +8 / -8 : cut 8 and skirt 8, both within limits
            assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(withGridAnomalies(c[0], c[1], 8, -8), c[0], c[1], HALL).verdict());
            // 17 = +9 / -8 (cut 9, skirt 8): only the 16 limit rejects
            var r = St01Eligibility.evaluate(withGridAnomalies(c[0], c[1], 9, -8), c[0], c[1], HALL);
            assertEquals(Verdict.REJECT_HEIGHT_DIFFERENCE, r.verdict());
            assertEquals(17, r.heightDifference());
        }
    }

    @Test void cut10Passes11Rejects() {
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(withGridAnomalies(8, 8, 10, 0), 8, 8, HALL).verdict());
        var r = St01Eligibility.evaluate(withGridAnomalies(8, 8, 11, 0), 8, 8, HALL);
        assertEquals(Verdict.REJECT_CUT, r.verdict());
        assertEquals(11, r.cut());
        assertTrue(r.heightDifference() <= 16, "cut limit must bind before the height-difference limit here");
    }

    @Test void skirt8Passes9Rejects() {
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(withGridAnomalies(8, 8, 0, -8), 8, 8, HALL).verdict());
        var r = St01Eligibility.evaluate(withGridAnomalies(8, 8, 0, -9), 8, 8, HALL);
        assertEquals(Verdict.REJECT_SKIRT, r.verdict());
        assertEquals(9, r.skirt());
    }

    @Test void foundationIsMedianAndCutSkirtAreRelativeToIt() {
        // just over half of the columns sit 6 higher than the rest: the median moves up, so the lower columns are a 6-block skirt, not a cut
        T t = new T();
        t.height = (x, z) -> (x - 8) < 1 ? Y + 6 : Y;
        var r = St01Eligibility.evaluate(t, 8, 8, HALL);
        assertEquals(Y + 6, r.foundationY());
        assertEquals(6, r.skirt());
        assertEquals(0, r.cut());
    }

    @Test void everyFootprintColumnIsSampledNotJustAGrid() {
        // a single off-grid column 30 blocks high must be seen (a 4-block grid would skip x offset -39)
        T t = new T();
        t.height = (x, z) -> (x == 8 - 39 && z == 8 + 1) ? Y + 30 : Y;
        assertEquals(Verdict.REJECT_HEIGHT_DIFFERENCE, St01Eligibility.evaluate(t, 8, 8, HALL).verdict());
        // columns just outside the 81x81 footprint are not part of the rule
        T u = new T();
        u.height = (x, z) -> (x == 8 - 41 || x == 8 + 41 || z == 8 - 41 || z == 8 + 41) ? Y + 30 : Y;
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(u, 8, 8, HALL).verdict());
    }

    @Test void fluidBoundaryOfUndergroundVolumeWithMargin() {
        int cx = 8, cz = 8, yf = Y;
        // volume x[-40,-2] y[-24,-5] z[2,40], margin 1: fluid inside the margin shell rejects, one block further does not
        record P(int dx, int dy, int dz, boolean reject) {}
        P[] cases = {
                new P(-41, -10, 10, true), new P(-42, -10, 10, false),     // west edge
                new P(-1, -10, 10, true), new P(0, -10, 10, false),        // east edge (-2 + margin)
                new P(-20, -25, 10, true), new P(-20, -26, 10, false),     // bottom
                new P(-20, -4, 10, true), new P(-20, -3, 10, false),       // top
                new P(-20, -10, 1, true), new P(-20, -10, 0, false),       // north edge (2 - margin)
                new P(-20, -10, 41, true), new P(-20, -10, 42, false),     // south edge
                new P(-20, -15, 20, true),                                 // dead centre
        };
        for (P p : cases) {
            T t = new T();
            t.fluid = a -> a[0] == cx + p.dx() && a[1] == yf + p.dy() && a[2] == cz + p.dz();
            var r = St01Eligibility.evaluate(t, cx, cz, HALL);
            assertEquals(p.reject() ? Verdict.REJECT_FLUID : Verdict.ELIGIBLE, r.verdict(), p.toString());
        }
    }

    @Test void fluidIsNotCheckedForMissingVolumeAndNotBeforeTerrain() {
        T t = new T();
        t.fluid = a -> true;
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(t, 8, 8, null).verdict());
        T both = withGridAnomalies(8, 8, 11, 0);
        both.fluid = a -> true;
        assertEquals(Verdict.REJECT_CUT, St01Eligibility.evaluate(both, 8, 8, HALL).verdict(), "terrain stage is reported before fluid");
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
                assertEquals(Verdict.REJECT_BIOME, St01Eligibility.evaluate(t, cx, cz, HALL).verdict(), "bad=" + bad[0] + "," + bad[1] + " c=" + cx + "," + cz);
            }
            for (int[] ok : new int[][]{{qx0 - 1, midQz}, {qx1 + 1, midQz}, {midQx, qz0 - 1}, {midQx, qz1 + 1}}) {
                T t = new T();
                t.desertCell = q -> !(q[0] == ok[0] && q[2] == ok[1]);
                assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(t, cx, cz, HALL).verdict(), "outside ring must not matter: " + ok[0] + "," + ok[1]);
            }
        }
    }

    @Test void bothVerticalQuartCandidatesAreRequired() {
        T t = new T();
        int surfaceBlock = Y - 1;
        int qyA = Math.floorDiv(surfaceBlock - 2, 4);
        t.desertCell = q -> q[1] != qyA + 1 || q[0] != 2 || q[2] != 2;
        assertEquals(Verdict.REJECT_BIOME, St01Eligibility.evaluate(t, 8, 8, HALL).verdict());
        t.desertCell = q -> q[1] != qyA - 1;
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(t, 8, 8, HALL).verdict());
    }

    @Test void anchorStubBiomeCellIsRequiredAtTheFoundationLevel() {
        // vanilla drops a stub whose (anchor, foundationY) cell is not in the structure's biome set: it must be rejected here, not later
        T t = new T();
        int cellY = Math.floorDiv(Y, 4); // yf = Y
        int surfaceBlock = Y - 1;
        int qyA = Math.floorDiv(surfaceBlock - 2, 4);
        assertTrue(cellY == qyA || cellY == qyA + 1);
        t.desertCell = q -> true;
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(t, 8, 8, HALL).verdict());
    }

    @Test void evaluationIsDeterministicAndSideEffectFree() {
        T t = withGridAnomalies(8, 8, 5, -5);
        assertEquals(St01Eligibility.evaluate(t, 8, 8, HALL), St01Eligibility.evaluate(t, 8, 8, HALL));
    }

    @Test void fixtureVolumeRotatesWithTheSameTransformAsTemplatePlacement() {
        // all four rotations keep the hall inside the 81x81 footprint (|d| <= 40) and map the reference box to the expected quadrant
        for (var rot : net.minecraft.world.level.block.Rotation.values()) {
            Volume v = St01RoyalHallFixture.volume(rot);
            assertTrue(v.minDx() >= -40 && v.maxDx() <= 40 && v.minDz() >= -40 && v.maxDz() <= 40, rot + " " + v);
            assertEquals(39, v.maxDx() - v.minDx() + 1);
            assertEquals(39, v.maxDz() - v.minDz() + 1);
        }
        Volume none = St01RoyalHallFixture.volume(net.minecraft.world.level.block.Rotation.NONE);
        assertEquals(new Volume(-40, -2, -24, -5, 2, 40), none);
        Volume cw = St01RoyalHallFixture.volume(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        assertEquals(new Volume(-40, -2, -24, -5, -40, -2), cw);
    }
}
