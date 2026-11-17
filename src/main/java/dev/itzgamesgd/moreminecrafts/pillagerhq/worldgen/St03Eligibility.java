package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import java.util.Arrays;

/** Exact S0/B80/W5/E1/P/L2 rule from the V2 measurement (owner approved 2026-11-16). */
public final class St03Eligibility {
    private St03Eligibility() {}

    public interface Terrain {
        int surfaceHeight(int x, int z);
        int oceanFloorHeight(int x, int z);
        boolean isEligibleBiomeCell(int quartX, int quartY, int quartZ);
        default boolean isEdgeBiomeCell(int quartX, int quartY, int quartZ) { return false; }
    }

    public enum Verdict {
        ELIGIBLE, REJECT_CENTER_BIOME, REJECT_FOOTPRINT_BIOME, REJECT_WATER,
        REJECT_HEIGHT_DIFFERENCE, REJECT_CUT_SKIRT, REJECT_FOOTING_FILL
    }

    public record Result(Verdict verdict, int minHeight, int maxHeight, int foundationY,
                         int heightDifference, int cut, int skirt, int footingFill) {
        public boolean eligible() { return verdict == Verdict.ELIGIBLE; }
        public int rawHeightDifference() { return maxHeight - minHeight; }
    }

    /** The last interval is seven blocks: preserve the measured far boundary at 111. */
    public static int[] sampleOffsets() {
        int[] out = new int[15];
        for (int i = 0; i < 14; i++) out[i] = i * 8;
        out[14] = 111;
        return out;
    }

    private static Result reject(Verdict verdict) { return new Result(verdict, 0, 0, 0, 0, 0, 0, 0); }

    // A column must pass in both vertically adjacent quart cells, exactly as St03SiteV2Probe.columnSet.
    private static int columnKind(Terrain t, int x, int z) {
        int qx = Math.floorDiv(x, 4), qz = Math.floorDiv(z, 4);
        int qy = Math.floorDiv(t.surfaceHeight(x, z) - 3, 4);
        if (t.isEligibleBiomeCell(qx, qy, qz) && t.isEligibleBiomeCell(qx, qy + 1, qz)) return 0;
        if (t.isEdgeBiomeCell(qx, qy, qz) && t.isEdgeBiomeCell(qx, qy + 1, qz)) return 1;
        return 2;
    }

    public static Result evaluate(Terrain t, int originX, int originZ) {
        if (columnKind(t, originX + 56, originZ + 56) != 0) return reject(Verdict.REJECT_CENTER_BIOME);
        int good = 0, edge = 0;
        for (int x = originX - 2; x <= originX + 110; x += 8) {
            for (int z = originZ - 2; z <= originZ + 110; z += 8) {
                int kind = columnKind(t, x, z);
                if (kind == 0) good++;
                else if (kind == 1) edge++;
            }
        }
        int denominator = 225 - edge;
        if (denominator == 0 || 100L * good < (long) St03PlacementConfig.MIN_BIOME_PERCENT * denominator)
            return reject(Verdict.REJECT_FOOTPRINT_BIOME);

        int[] heights = new int[225];
        int count = 0, water = 0, floorMin = Integer.MAX_VALUE;
        for (int dx : sampleOffsets()) {
            for (int dz : sampleOffsets()) {
                int s = t.surfaceHeight(originX + dx, originZ + dz);
                int f = t.oceanFloorHeight(originX + dx, originZ + dz);
                heights[count++] = s;
                if (s > f + 1) water++;
                floorMin = Math.min(floorMin, f);
            }
        }
        if (!withinWaterLimit(water, heights.length))
            return reject(Verdict.REJECT_WATER);
        Arrays.sort(heights);
        int min = heights[0], max = heights[224], median = heights[112];
        int width = nearestRank(heights, .95) - nearestRank(heights, .05);
        if (width > St03PlacementConfig.MAX_HEIGHT_DIFFERENCE)
            return new Result(Verdict.REJECT_HEIGHT_DIFFERENCE, min, max, 0, width, 0, 0, 0);
        int low = max - St03PlacementConfig.MAX_CUT;
        int highSkirt = min + St03PlacementConfig.MAX_SKIRT;
        int highFill = floorMin + St03PlacementConfig.MAX_FOOTING_FILL;
        if (low > highSkirt)
            return new Result(Verdict.REJECT_CUT_SKIRT, min, max, 0, width, 0, 0, 0);
        if (low > highFill)
            return new Result(Verdict.REJECT_FOOTING_FILL, min, max, 0, width, 0, 0, 0);
        int y = Math.max(low, Math.min(median, Math.min(highSkirt, highFill)));
        return new Result(Verdict.ELIGIBLE, min, max, y, width,
                Math.max(0, max - y), Math.max(0, y - min), Math.max(0, y - floorMin));
    }

    public static int nearestRank(int[] sorted, double fraction) {
        if (sorted.length == 0) throw new IllegalArgumentException("Empty height population");
        return sorted[Math.max(0, (int) Math.ceil(sorted.length * fraction) - 1)];
    }

    static boolean withinWaterLimit(int water, int samples) {
        return samples > 0 && 100L * water <= (long) St03PlacementConfig.MAX_WATER_PERCENT * samples;
    }
}
