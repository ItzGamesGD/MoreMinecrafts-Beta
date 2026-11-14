package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility.Terrain;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility.Verdict;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * P8-D3 C1: the height-free column prefilter hook may only remove candidates the unchanged Desert stage rejects, never
 * changes a {@link St01Eligibility.Result}, and runs over exactly the cells of the Desert sweep.
 */
class St01EligibilityPrefilterTest {
    private static final int MIN_Y = -64, MAX_Y = 319;

    /** Random synthetic world: per-column surface heights and per-quart-cell Desert flags; optional exact prefilter. */
    private static class World implements Terrain {
        final long seed;
        final double desertShare;
        final int baseY, relief;
        boolean prefilter;
        int heightCalls, prefilterCalls;
        final Set<Long> prefilterCells = new HashSet<>();

        World(long seed, double desertShare, int baseY, int relief) {
            this.seed = seed; this.desertShare = desertShare; this.baseY = baseY; this.relief = relief;
        }

        private long mix(long a, long b, long c) {
            long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (c * 0x165667B19E3779F9L);
            h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
            return h;
        }

        @Override public int surfaceHeight(int x, int z) {
            heightCalls++;
            int h = baseY + (int) Math.floorMod(mix(x >> 3, z >> 3, 7), relief + 1);
            return Math.max(MIN_Y, Math.min(MAX_Y + 1, h));
        }

        /** Per-mille of columns that are Desert only in some vertical bands (Desert below/above the surface only). */
        int bandedPerMille;

        @Override public boolean isDesertCell(int qx, int qy, int qz) {
            if (Math.floorMod(mix(qx, qz, 1), 1000) >= desertShare * 1000) return false;
            if (Math.floorMod(mix(qx, qz, 2), 1000) >= bandedPerMille) return true;
            return Math.floorMod(mix(qx, qy >> 2, qz), 3) != 0;
        }

        @Override public boolean desertPossibleInColumn(int qx, int qz) {
            prefilterCalls++;
            prefilterCells.add(((long) qx << 32) ^ (qz & 0xFFFFFFFFL));
            if (!prefilter) return true;
            int qy0 = Math.floorDiv(MIN_Y - 3, 4), qy1 = Math.floorDiv(MAX_Y - 2, 4) + 1;
            for (int qy = qy0; qy < qy1; qy++) if (isDesertCell(qx, qy, qz) && isDesertCell(qx, qy + 1, qz)) return true;
            return false;
        }
    }

    @Test void exactPrefilterNeverChangesAResult() {
        Random r = new Random(91401);
        int rejectedByPrefilter = 0, eligible = 0, passedPrefilterFailedSweep = 0;
        for (int i = 0; i < 400; i++) {
            long seed = r.nextLong();
            double share = new double[]{0.2, 0.9, 0.97, 1.0}[i % 4];
            int relief = new int[]{0, 8, 16, 24}[(i / 4) % 4];
            int cx = r.nextInt(200_000) - 100_000, cz = r.nextInt(200_000) - 100_000;
            int banded = new int[]{0, 0, 2, 30}[(i / 16) % 4];
            World legacy = new World(seed, share, 70, relief);
            World fast = new World(seed, share, 70, relief);
            legacy.bandedPerMille = banded;
            fast.bandedPerMille = banded;
            fast.prefilter = true;
            var expected = St01Eligibility.evaluate(legacy, cx, cz);
            var actual = St01Eligibility.evaluate(fast, cx, cz);
            assertEquals(expected, actual, "seed=" + seed + " c=" + cx + "," + cz);
            if (expected.eligible()) eligible++;
            if (fast.heightCalls == 0 && expected.verdict() == Verdict.REJECT_BIOME) rejectedByPrefilter++;
            if (fast.heightCalls > 0 && expected.verdict() == Verdict.REJECT_BIOME) passedPrefilterFailedSweep++;
        }
        assertTrue(rejectedByPrefilter > 50, "the prefilter must actually reject before heights: " + rejectedByPrefilter);
        assertTrue(eligible > 10, "eligible candidates must be covered: " + eligible);
        assertTrue(passedPrefilterFailedSweep > 0, "prefilter-pass / sweep-fail columns must be covered: " + passedPrefilterFailedSweep);
    }

    @Test void prefilterRejectionSamplesNoHeight() {
        World w = new World(1, 1.0, 70, 0) {
            @Override public boolean desertPossibleInColumn(int qx, int qz) { prefilterCalls++; return !(qx == Math.floorDiv(8 + 40 - 2, 4) + 1); }
        };
        assertEquals(Verdict.REJECT_BIOME, St01Eligibility.evaluate(w, 8, 8).verdict());
        assertEquals(0, w.heightCalls, "a prefilter rejection must happen before any height is sampled");
        World centre = new World(1, 1.0, 70, 0) {
            @Override public boolean desertPossibleInColumn(int qx, int qz) { prefilterCalls++; return false; }
        };
        assertEquals(Verdict.REJECT_BIOME, St01Eligibility.evaluate(centre, 8, 8).verdict());
        assertEquals(1, centre.prefilterCalls, "the centre cell is tested first");
        assertEquals(0, centre.heightCalls);
    }

    @Test void prefilterCoversExactlyTheDesertSweepCells() {
        for (int[] c : new int[][]{{8, 8}, {-1016, -1008}, {4088, -8}, {-7, 13}}) {
            World w = new World(3, 1.0, 70, 0);
            assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(w, c[0], c[1]).verdict());
            Set<Long> sweep = new HashSet<>();
            int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
            for (int qx = Math.floorDiv(c[0] - half - 2, 4); qx <= Math.floorDiv(c[0] + half - 2, 4) + 1; qx++)
                for (int qz = Math.floorDiv(c[1] - half - 2, 4); qz <= Math.floorDiv(c[1] + half - 2, 4) + 1; qz++)
                    sweep.add(((long) qx << 32) ^ (qz & 0xFFFFFFFFL));
            assertEquals(sweep, w.prefilterCells, "c=" + c[0] + "," + c[1]);
        }
    }

    @Test void defaultHookDecidesNothing() {
        Terrain plain = new Terrain() {
            @Override public int surfaceHeight(int x, int z) { return 100; }
            @Override public boolean isDesertCell(int qx, int qy, int qz) { return true; }
        };
        assertTrue(plain.desertPossibleInColumn(0, 0));
        assertEquals(Verdict.ELIGIBLE, St01Eligibility.evaluate(plain, 8, 8).verdict());
    }
}
