package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import java.util.Arrays;

/**
 * Deterministic 112x112 candidate eligibility evaluator for ST-03 Pillager Headquarters.
 * Uses {@code WORLD_SURFACE_WG} (natural ground excluding tree trunks/leaves) and checks
 * biome eligibility, water exclusion, and <=5 block natural terrain elevation spread.
 */
public final class St03Eligibility {
    private St03Eligibility() {}

    public interface Terrain {
        /** {@code WORLD_SURFACE_WG} height (first air Y above solid terrain, ignoring trees). */
        int surfaceHeight(int x, int z);

        /** {@code OCEAN_FLOOR_WG} height to detect rivers/lakes/oceans. */
        int oceanFloorHeight(int x, int z);

        /** Whether the noise biome of the quart cell is an eligible plains/forest biome. */
        boolean isEligibleBiomeCell(int quartX, int quartY, int quartZ);
    }

    public enum Verdict {
        ELIGIBLE,
        REJECT_BIOME,
        REJECT_WATER,
        REJECT_HEIGHT_DIFFERENCE,
        REJECT_CUT,
        REJECT_SKIRT
    }

    public record Result(Verdict verdict, int minHeight, int maxHeight, int foundationY,
                         int heightDifference, int cut, int skirt) {
        public boolean eligible() {
            return verdict == Verdict.ELIGIBLE;
        }
    }

    private static final Result BIOME_REJECT = new Result(Verdict.REJECT_BIOME, 0, 0, 0, 0, 0, 0);

    public static Result evaluate(Terrain terrain, int originX, int originZ) {
        if (!fullFootprintEligibleBiome(terrain, originX, originZ)) {
            return BIOME_REJECT;
        }

        int size = St03PlacementConfig.BOX_SIZE;
        int step = St03PlacementConfig.HEIGHT_SAMPLE_STEP;
        int perAxis = (size - 1) / step + 2;
        int[] heights = new int[perAxis * perAxis];
        int count = 0;

        for (int dx = 0; dx < size; dx += step) {
            int sx = originX + dx;
            for (int dz = 0; dz < size; dz += step) {
                int sz = originZ + dz;
                int surf = terrain.surfaceHeight(sx, sz);
                int floor = terrain.oceanFloorHeight(sx, sz);
                if (surf > floor + 1) {
                    return new Result(Verdict.REJECT_WATER, floor, surf, surf, surf - floor, 0, 0);
                }
                heights[count++] = surf;
            }
            // Include far edge z = originZ + size - 1
            int ez = originZ + size - 1;
            int surfEdge = terrain.surfaceHeight(sx, ez);
            int floorEdge = terrain.oceanFloorHeight(sx, ez);
            if (surfEdge > floorEdge + 1) {
                return new Result(Verdict.REJECT_WATER, floorEdge, surfEdge, surfEdge, surfEdge - floorEdge, 0, 0);
            }
            heights[count++] = surfEdge;
        }

        // Also sample far X edge (x = originX + size - 1)
        int ex = originX + size - 1;
        for (int dz = 0; dz < size; dz += step) {
            int sz = originZ + dz;
            int surf = terrain.surfaceHeight(ex, sz);
            int floor = terrain.oceanFloorHeight(ex, sz);
            if (surf > floor + 1) {
                return new Result(Verdict.REJECT_WATER, floor, surf, surf, surf - floor, 0, 0);
            }
            heights[count++] = surf;
        }
        int surfCorner = terrain.surfaceHeight(ex, originZ + size - 1);
        int floorCorner = terrain.oceanFloorHeight(ex, originZ + size - 1);
        if (surfCorner > floorCorner + 1) {
            return new Result(Verdict.REJECT_WATER, floorCorner, surfCorner, surfCorner, surfCorner - floorCorner, 0, 0);
        }
        heights[count++] = surfCorner;

        Arrays.sort(heights, 0, count);
        int min = heights[0];
        int max = heights[count - 1];
        int yf = heights[count / 2];
        int diff = max - min;
        int cut = Math.max(0, max - yf);
        int skirt = Math.max(0, yf - min);

        if (diff > St03PlacementConfig.MAX_HEIGHT_DIFFERENCE) {
            return new Result(Verdict.REJECT_HEIGHT_DIFFERENCE, min, max, yf, diff, cut, skirt);
        }
        if (cut > St03PlacementConfig.MAX_CUT) {
            return new Result(Verdict.REJECT_CUT, min, max, yf, diff, cut, skirt);
        }
        if (skirt > St03PlacementConfig.MAX_SKIRT) {
            return new Result(Verdict.REJECT_SKIRT, min, max, yf, diff, cut, skirt);
        }

        int cx = originX + size / 2;
        int cz = originZ + size / 2;
        if (!terrain.isEligibleBiomeCell(Math.floorDiv(cx, 4), Math.floorDiv(yf, 4), Math.floorDiv(cz, 4))) {
            return new Result(Verdict.REJECT_BIOME, min, max, yf, diff, cut, skirt);
        }

        return new Result(Verdict.ELIGIBLE, min, max, yf, diff, cut, skirt);
    }

    public static boolean fullFootprintEligibleBiome(Terrain terrain, int originX, int originZ) {
        int cx = originX + St03PlacementConfig.BOX_SIZE / 2;
        int cz = originZ + St03PlacementConfig.BOX_SIZE / 2;
        if (!cellColumnEligible(terrain, Math.floorDiv(cx, 4), Math.floorDiv(cz, 4))) {
            return false;
        }
        int qx0 = Math.floorDiv(originX - 2, 4);
        int qx1 = Math.floorDiv(originX + St03PlacementConfig.BOX_SIZE - 3, 4) + 1;
        int qz0 = Math.floorDiv(originZ - 2, 4);
        int qz1 = Math.floorDiv(originZ + St03PlacementConfig.BOX_SIZE - 3, 4) + 1;
        // Sample every 2 quart cells (8 blocks) for fast exact-enough perimeter/interior check
        for (int qx = qx0; qx <= qx1; qx += 2) {
            for (int qz = qz0; qz <= qz1; qz += 2) {
                if (!cellColumnEligible(terrain, qx, qz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean cellColumnEligible(Terrain terrain, int qx, int qz) {
        int surfaceBlockY = terrain.surfaceHeight(qx * 4 + 2, qz * 4 + 2) - 1;
        int qy = Math.floorDiv(surfaceBlockY - 2, 4);
        return terrain.isEligibleBiomeCell(qx, qy, qz) && terrain.isEligibleBiomeCell(qx, qy + 1, qz);
    }
}
