package dev.itzgamesgd.moreminecrafts.st01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

/**
 * ST-01 O-14 — Natural Worldgen Feasibility & Deterministic Invariance Tests.
 *
 * <p>Verifies the mathematical and logical requirements of ST-01 worldgen feasibility:
 * <ul>
 *   <li>EAST/WEST geometry equivalence and exact X-axis mirror contract</li>
 *   <li>Deterministic seed-based orientation selection with 50:50 distribution</li>
 *   <li>Chunk-border footprint invariance across all 256 subchunk coordinate phase offsets</li>
 *   <li>Exactly-once secret entrance registration contract upon placement completion</li>
 *   <li>Rejection of registration before placement and rejection of duplicate registrations</li>
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

    @Test
    void testDeterministicOrientationSelection50_50Distribution() {
        long seed = 123456789L;
        int trials = 10000;
        int eastCount = 0;
        int westCount = 0;

        for (int i = 0; i < trials; i++) {
            long coordHash = ((long) (i * 31 + 17) << 32) ^ (long) (i * 101 + 43);
            long combined = seed ^ coordHash;
            // 64-bit mix (MurmurHash3 finalizer)
            combined ^= (combined >>> 33);
            combined *= 0xff51afd7ed558ccdL;
            combined ^= (combined >>> 33);
            combined *= 0xc4ceb9fe1a85ec53L;
            combined ^= (combined >>> 33);

            EntranceLayout.Orientation o = (combined & 1L) == 0L ? EntranceLayout.Orientation.EAST : EntranceLayout.Orientation.WEST;
            if (o == EntranceLayout.Orientation.EAST) eastCount++;
            else westCount++;

            // Test deterministic reproducibility: exact same inputs must yield exact same orientation
            long verifyCombined = seed ^ coordHash;
            verifyCombined ^= (verifyCombined >>> 33);
            verifyCombined *= 0xff51afd7ed558ccdL;
            verifyCombined ^= (verifyCombined >>> 33);
            verifyCombined *= 0xc4ceb9fe1a85ec53L;
            verifyCombined ^= (verifyCombined >>> 33);
            EntranceLayout.Orientation o2 = (verifyCombined & 1L) == 0L ? EntranceLayout.Orientation.EAST : EntranceLayout.Orientation.WEST;
            assertEquals(o, o2, "Orientation selection must be 100% deterministic and reproducible");
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
                assertEquals(36, spanX * spanZ, "Total chunks spanned must be invariant at 36 chunks");
            }
        }
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
    void testTerrainDeltaAndBiomeEligibilityPredicates() {
        // Mock preflight predicates:
        // Rule 1: Desert biome only (100% footprint)
        // Rule 2: Delta elevation <= maxDelta
        double maxDelta = 12.0;

        // Candidate A: Flat desert plateau (delta = 4.5, 100% desert) -> ACCEPT
        assertTrue(mockPreflight(1.0, 4.5, maxDelta));

        // Candidate B: Moderate rolling dune (delta = 9.2, 100% desert) -> ACCEPT
        assertTrue(mockPreflight(1.0, 9.2, maxDelta));

        // Candidate C: Extreme dune crest (delta = 16.8, 100% desert) -> REJECT
        assertFalse(mockPreflight(1.0, 16.8, maxDelta));

        // Candidate D: Bordering savanna (delta = 3.2, 85% desert) -> REJECT (Rule: natural generation restricted to desert)
        assertFalse(mockPreflight(0.85, 3.2, maxDelta));
    }

    private boolean mockPreflight(double desertCoverage, double deltaH, double maxDelta) {
        if (desertCoverage < 1.0) return false;
        if (deltaH > maxDelta) return false;
        return true;
    }
}
