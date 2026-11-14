package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class St03EligibilityTest {
    private static St03Eligibility.Terrain flatTerrain(int y, boolean biomeOk, boolean waterAtCenter) {
        return new St03Eligibility.Terrain() {
            @Override
            public int surfaceHeight(int x, int z) {
                return y;
            }

            @Override
            public int oceanFloorHeight(int x, int z) {
                return (waterAtCenter && x == 56 && z == 56) ? y - 3 : y;
            }

            @Override
            public boolean isEligibleBiomeCell(int quartX, int quartY, int quartZ) {
                return biomeOk;
            }
        };
    }

    @Test
    void flatEligibleTerrainPasses() {
        var res = St03Eligibility.evaluate(flatTerrain(64, true, false), 0, 0);
        assertTrue(res.eligible());
        assertEquals(St03Eligibility.Verdict.ELIGIBLE, res.verdict());
        assertEquals(64, res.foundationY());
        assertEquals(0, res.heightDifference());
    }

    @Test
    void fiveBlockSlopeWithinApprovedLimitPasses() {
        St03Eligibility.Terrain sloped = new St03Eligibility.Terrain() {
            @Override
            public int surfaceHeight(int x, int z) {
                if (x < 20) return 62;
                if (x > 90) return 67;
                return 64;
            }

            @Override
            public int oceanFloorHeight(int x, int z) {
                return surfaceHeight(x, z);
            }

            @Override
            public boolean isEligibleBiomeCell(int quartX, int quartY, int quartZ) {
                return true;
            }
        };
        var res = St03Eligibility.evaluate(sloped, 0, 0);
        assertTrue(res.eligible());
        assertEquals(5, res.heightDifference());
    }

    @Test
    void sixBlockSlopeIsRejected() {
        St03Eligibility.Terrain steep = new St03Eligibility.Terrain() {
            @Override
            public int surfaceHeight(int x, int z) {
                return x < 20 ? 60 : 66;
            }

            @Override
            public int oceanFloorHeight(int x, int z) {
                return surfaceHeight(x, z);
            }

            @Override
            public boolean isEligibleBiomeCell(int quartX, int quartY, int quartZ) {
                return true;
            }
        };
        var res = St03Eligibility.evaluate(steep, 0, 0);
        assertFalse(res.eligible());
        assertEquals(St03Eligibility.Verdict.REJECT_HEIGHT_DIFFERENCE, res.verdict());
    }

    @Test
    void waterInsideFootprintIsRejected() {
        var res = St03Eligibility.evaluate(flatTerrain(64, true, true), 0, 0);
        assertFalse(res.eligible());
        assertEquals(St03Eligibility.Verdict.REJECT_WATER, res.verdict());
    }

    @Test
    void ineligibleBiomeIsRejected() {
        var res = St03Eligibility.evaluate(flatTerrain(64, false, false), 0, 0);
        assertFalse(res.eligible());
        assertEquals(St03Eligibility.Verdict.REJECT_BIOME, res.verdict());
    }
}
