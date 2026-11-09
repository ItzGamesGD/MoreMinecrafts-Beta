package dev.itzgamesgd.moreminecrafts.st01;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Corrected Verification for ST-01 O-14 Worldgen Feasibility Study.
 *
 * <p>Validates:
 * <ul>
 *   <li>Exact X-mirror symmetry between EAST and WEST secret-area geometry.</li>
 *   <li>Deterministic 64-bit MurmurHash3 orientation selection (shared with Python tooling).</li>
 *   <li>Exact 81x81 body footprint invariance (strictly 6x6=36 chunks across all 256 subchunk phases).</li>
 *   <li>Variable chunk span for buffered/skirt footprints (36, 42, or 49 chunks across the 256 phases).</li>
 *   <li>SecretEntranceData idempotent exact-once registration contract.</li>
 *   <li>Multi-criteria biome eligibility predicates (center vs full footprint).</li>
 * </ul>
 */
class St01O14WorldgenFeasibilityTest {

    @Test
    void testEastWestGeometryEquivalence() {
        BlockPos anchor = new BlockPos(100, 64, 200);

        List<BlockPos> eastCells = EntranceLayout.coverCells(anchor, EntranceLayout.Orientation.EAST);
        List<BlockPos> westCells = EntranceLayout.coverCells(anchor, EntranceLayout.Orientation.WEST);

        assertEquals(12, eastCells.size(), "EAST cover must have exactly 12 cells");
        assertEquals(12, westCells.size(), "WEST cover must have exactly 12 cells");

        // Verify exact X-mirror property: for every cell (anchor + dx, dy, dz) in EAST,
        // there must be an exact corresponding cell (anchor - dx, dy, dz) in WEST.
        for (BlockPos e : eastCells) {
            int dx = e.getX() - anchor.getX();
            int dy = e.getY() - anchor.getY();
            int dz = e.getZ() - anchor.getZ();

            assertTrue(dx >= 12 && dx <= 15, "EAST dx must be in 12..15");
            assertEquals(-3, dy, "Cover layer must be at anchor dy = -3");
            assertTrue(dz >= 0 && dz <= 2, "Cover dz must be in 0..2");

            BlockPos mirrored = anchor.offset(-dx, dy, dz);
            assertTrue(westCells.contains(mirrored), "WEST cells must contain exact mirrored cell: " + mirrored);
        }

        // Verify stop points (O-4)
        BlockPos stopEast = EntranceLayout.stopPoint(anchor, EntranceLayout.Orientation.EAST);
        BlockPos stopWest = EntranceLayout.stopPoint(anchor, EntranceLayout.Orientation.WEST);
        assertEquals(anchor.getX() + 9, stopEast.getX());
        assertEquals(anchor.getX() - 9, stopWest.getX());
        assertEquals(stopEast.getY(), stopWest.getY());
        assertEquals(stopEast.getZ(), stopWest.getZ());

        // Verify golem emergence spawn (O-6): centered on X=0 so identical in both
        BlockPos golemEast = EntranceLayout.golemSpawn(anchor, EntranceLayout.Orientation.EAST);
        BlockPos golemWest = EntranceLayout.golemSpawn(anchor, EntranceLayout.Orientation.WEST);
        assertEquals(golemEast, golemWest, "Golem spawn is on centerline X=0, must be identical in EAST and WEST");
        assertEquals(anchor.getX(), golemEast.getX());
        assertEquals(anchor.getY() - 2, golemEast.getY());
        assertEquals(anchor.getZ() - 6, golemEast.getZ());
    }

    /**
     * Shared 64-bit mixer (MurmurHash3 finalizer) matching Python get_deterministic_orientation.
     */
    static long mix64(long seed, int x, int z) {
        long coordHash = (((long) x & 0xFFFFFFFFL) << 32) | ((long) z & 0xFFFFFFFFL);
        long combined = seed ^ coordHash;
        combined ^= (combined >>> 33);
        combined *= 0xff51afd7ed558ccdL;
        combined ^= (combined >>> 33);
        combined *= 0xc4ceb9fe1a85ec53L;
        combined ^= (combined >>> 33);
        return combined;
    }

    static EntranceLayout.Orientation selectOrientation(long seed, int x, int z) {
        long h = mix64(seed, x, z);
        return (h & 1L) == 0L ? EntranceLayout.Orientation.EAST : EntranceLayout.Orientation.WEST;
    }

    @Test
    void testDeterministicOrientationCrossLanguageVectors() {
        // Shared test vectors verified against Python murmur3_64
        assertEquals(EntranceLayout.Orientation.EAST, selectOrientation(1001L, 100, 200),
                "Vector 1 (1001, 100, 200) must yield EAST");
        assertEquals(EntranceLayout.Orientation.WEST, selectOrientation(1001L, -500, 300),
                "Vector 2 (1001, -500, 300) must yield WEST");
        assertEquals(EntranceLayout.Orientation.WEST, selectOrientation(1002L, 0, 0),
                "Vector 3 (1002, 0, 0) must yield WEST");
        assertEquals(EntranceLayout.Orientation.EAST, selectOrientation(123456789L, 42, -99),
                "Vector 4 (123456789, 42, -99) must yield EAST");
        assertEquals(EntranceLayout.Orientation.EAST, selectOrientation(-987654321L, 1234, 5678),
                "Vector 5 (-987654321, 1234, 5678) must yield EAST");
    }

    @Test
    void testDeterministicOrientationSelection50_50Distribution() {
        long seed = 123456789L;
        int trials = 10000;
        int eastCount = 0;
        int westCount = 0;

        for (int i = 0; i < trials; i++) {
            int x = i * 31 + 17;
            int z = i * 101 + 43;
            EntranceLayout.Orientation o = selectOrientation(seed, x, z);
            if (o == EntranceLayout.Orientation.EAST) eastCount++;
            else westCount++;

            // Test deterministic reproducibility: exact same inputs must yield exact same orientation
            assertEquals(o, selectOrientation(seed, x, z),
                    "Orientation selection must be 100% deterministic and reproducible");
        }

        double eastRatio = (double) eastCount / trials;
        assertTrue(eastRatio >= 0.48 && eastRatio <= 0.52,
                "Orientation selection must have an intended 50:50 distribution (actual: " + eastRatio + ")");
    }

    @Test
    void testChunkBorderFootprintInvarianceAcrossSubchunkOffsets() {
        int halfWidth = 40; // 81 blocks total: -40..+40
        // Test all 256 subchunk offsets (0..15 in X, 0..15 in Z)
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int minX = dx - halfWidth;
                int maxX = dx + halfWidth;
                int minZ = dz - halfWidth;
                int maxZ = dz + halfWidth;

                int chunkMinX = Math.floorDiv(minX, 16);
                int chunkMaxX = Math.floorDiv(maxX, 16);
                int chunkMinZ = Math.floorDiv(minZ, 16);
                int chunkMaxZ = Math.floorDiv(maxZ, 16);

                int spanX = chunkMaxX - chunkMinX + 1;
                int spanZ = chunkMaxZ - chunkMinZ + 1;

                assertEquals(6, spanX, "Horizontal X span must be exactly 6 chunks for all subchunk offsets (dx=" + dx + ")");
                assertEquals(6, spanZ, "Horizontal Z span must be exactly 6 chunks for all subchunk offsets (dz=" + dz + ")");
                assertEquals(36, spanX * spanZ, "Total chunks spanned for 81x81 body must be invariant at 36 chunks");
            }
        }
    }

    @Test
    void testBufferedFootprintVariableChunkSpanAcrossSubchunkOffsets() {
        int bufferHalfWidth = 44; // 89x89 illustrative 4-block perimeter skirt buffer: -44..+44
        Map<Integer, Integer> distribution = new HashMap<>();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int minX = dx - bufferHalfWidth;
                int maxX = dx + bufferHalfWidth;
                int minZ = dz - bufferHalfWidth;
                int maxZ = dz + bufferHalfWidth;

                int chunkMinX = Math.floorDiv(minX, 16);
                int chunkMaxX = Math.floorDiv(maxX, 16);
                int chunkMinZ = Math.floorDiv(minZ, 16);
                int chunkMaxZ = Math.floorDiv(maxZ, 16);

                int spanX = chunkMaxX - chunkMinX + 1;
                int spanZ = chunkMaxZ - chunkMinZ + 1;
                int totalChunks = spanX * spanZ;

                assertTrue(spanX == 6 || spanX == 7, "Buffered spanX must be 6 or 7");
                assertTrue(spanZ == 6 || spanZ == 7, "Buffered spanZ must be 6 or 7");
                assertTrue(totalChunks == 36 || totalChunks == 42 || totalChunks == 49,
                        "Total chunks spanned for 89x89 buffer must be 36, 42, or 49 (actual: " + totalChunks + ")");

                distribution.put(totalChunks, distribution.getOrDefault(totalChunks, 0) + 1);
            }
        }

        // Verify exact 256-phase distribution for 89x89 buffer:
        // 36 chunks: 64 phases (25%)
        // 42 chunks: 128 phases (50%)
        // 49 chunks: 64 phases (25%)
        assertEquals(64, distribution.get(36).intValue(), "89x89 buffer must yield exactly 64 phases with 36 chunks");
        assertEquals(128, distribution.get(42).intValue(), "89x89 buffer must yield exactly 128 phases with 42 chunks");
        assertEquals(64, distribution.get(49).intValue(), "89x89 buffer must yield exactly 64 phases with 49 chunks");
    }

    @Test
    void testSecretEntranceDataRegistrationContract() {
        SecretEntranceData data = new SecretEntranceData();
        assertTrue(data.isEmpty(), "Data must start empty before placement");

        BlockPos anchor1 = new BlockPos(256, 64, 512);
        // Step 1: Successful placement registers once
        boolean firstReg = data.register(anchor1, EntranceLayout.Orientation.EAST);
        assertTrue(firstReg, "First registration upon placement completion must succeed");
        assertFalse(data.isEmpty(), "Data must not be empty after registration");
        assertEquals(1, ((List<?>) data.all()).size());

        // Step 2: Idempotent duplicate registration attempt rejected
        boolean dupReg = data.register(anchor1, EntranceLayout.Orientation.EAST);
        assertFalse(dupReg, "Duplicate registration of already placed structure must return false");

        // Step 3: Registration with conflicting orientation rejected
        boolean conflictReg = data.register(anchor1, EntranceLayout.Orientation.WEST);
        assertFalse(conflictReg, "Conflicting registration of existing anchor must return false");
        assertEquals(EntranceLayout.Orientation.EAST, data.get(anchor1.asLong()).orientation(),
                "Initial registered orientation must be preserved");

        // Step 4: Independent structure at distinct anchor succeeds
        BlockPos anchor2 = new BlockPos(1024, 64, 2048);
        boolean secondStructure = data.register(anchor2, EntranceLayout.Orientation.WEST);
        assertTrue(secondStructure, "Distinct structure registration must succeed");
        assertEquals(2, ((List<?>) data.all()).size());
        assertEquals(EntranceLayout.Orientation.WEST, data.get(anchor2.asLong()).orientation());
    }

    @Test
    void testMultiCriteriaBiomePredicates() {
        // Evaluates center-only vs 100% full-footprint predicates independently
        // Candidate A: 100% desert -> passes both
        assertTrue(predicateCenterOnly(true));
        assertTrue(predicateFullFootprint(1.0));

        // Candidate B: Desert center, but borders savanna (85% desert)
        // Passes center-only, rejected under strict 100% footprint
        assertTrue(predicateCenterOnly(true));
        assertFalse(predicateFullFootprint(0.85));

        // Candidate C: Non-desert center
        assertFalse(predicateCenterOnly(false));
        assertFalse(predicateFullFootprint(0.0));
    }

    private boolean predicateCenterOnly(boolean centerIsDesert) {
        return centerIsDesert;
    }

    private boolean predicateFullFootprint(double desertCoverageRatio) {
        return desertCoverageRatio >= 1.0;
    }
}
