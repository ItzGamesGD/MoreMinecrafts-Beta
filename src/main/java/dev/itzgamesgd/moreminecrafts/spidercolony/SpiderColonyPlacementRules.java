package dev.itzgamesgd.moreminecrafts.spidercolony;

import java.util.Collection;

/**
 * Deterministic placement and frequency rules for Spider Colony worldgen:
 * <ul>
 *   <li>Frequency: 1–3 colonies per 1.5 km² ({@code 1,500,000 m²}), modeled on a 77×77 chunk
 *       ({@code 1232 × 1232 = 1,517,824 m² ≈ 1.518 km²}) region grid.</li>
 *   <li>Nest range: 128–256 blocks radius, non-overlapping across all colonies.</li>
 * </ul>
 */
public final class SpiderColonyPlacementRules {
    public static final double TARGET_AREA_KM2 = 1.5;
    public static final int TARGET_AREA_SQ_BLOCKS = 1_500_000;
    public static final int REGION_SIDE_CHUNKS = 77; // 77 * 16 = 1232 blocks -> 1,517,824 blocks^2 (~1.518 km^2)
    public static final int MIN_COLONIES_PER_REGION = 1;
    public static final int MAX_COLONIES_PER_REGION = 3;
    public static final int MIN_NEST_RADIUS_BLOCKS = 128;
    public static final int MAX_NEST_RADIUS_BLOCKS = 256;

    private SpiderColonyPlacementRules() {}

    public static int regionX(int chunkX) {
        return Math.floorDiv(chunkX, REGION_SIDE_CHUNKS);
    }

    public static int regionZ(int chunkZ) {
        return Math.floorDiv(chunkZ, REGION_SIDE_CHUNKS);
    }

    public static int targetColoniesForRegion(long worldSeed, int regionX, int regionZ) {
        long h = mix64(worldSeed ^ (regionX * 0x9E3779B97F4A7C15L) ^ (regionZ * 0xBF58476D1CE4E5B9L));
        int span = MAX_COLONIES_PER_REGION - MIN_COLONIES_PER_REGION + 1;
        return MIN_COLONIES_PER_REGION + Math.floorMod(h, span);
    }

    public static int nestRadiusForSite(long worldSeed, int x, int y, int z) {
        long h = mix64(worldSeed ^ (x * 0x94D049BB133111EBL) ^ (y * 0xD6E8FEB86659FD93L) ^ (z * 0xA0761D6478BD642FL));
        int span = MAX_NEST_RADIUS_BLOCKS - MIN_NEST_RADIUS_BLOCKS + 1;
        return MIN_NEST_RADIUS_BLOCKS + Math.floorMod(h, span);
    }

    public static boolean overlapsAny(int x, int z, int nestRadius, Collection<ColonyLedger.RecordedColony> existing) {
        for (ColonyLedger.RecordedColony colony : existing) {
            long dx = (long) x - colony.x();
            long dz = (long) z - colony.z();
            long minSep = (long) nestRadius + colony.nestRadius();
            if (dx * dx + dz * dz < minSep * minSep) {
                return true;
            }
        }
        return false;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
