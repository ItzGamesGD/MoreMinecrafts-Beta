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

    /** The last interval is seven blocks: preserve the measured far boundary at 95 (96x96) or 111 (112x112). */
    public static int[] sampleOffsets() {
        return sampleOffsets(St03PlacementConfig.EVAL_BOX_SIZE);
    }

    public static int[] sampleOffsets(int boxSize) {
        int steps = boxSize / 8 + 1;
        int[] out = new int[steps];
        for (int i = 0; i < steps - 1; i++) out[i] = i * 8;
        out[steps - 1] = boxSize - 1;
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
        return evaluate(t, originX, originZ, St03PlacementConfig.EVAL_BOX_SIZE, St03PlacementConfig.MAX_WATER_PERCENT);
    }

    public static Result evaluate(Terrain t, int originX, int originZ, int evalBoxSize, int maxWaterPercent) {
        if (columnKind(t, originX + 56, originZ + 56) != 0) return reject(Verdict.REJECT_CENTER_BIOME);
        int margin = (St03PlacementConfig.BOX_SIZE - evalBoxSize) / 2;
        int ox = originX + margin, oz = originZ + margin;
        int good = 0, edge = 0;
        for (int x = ox - 2; x <= ox + evalBoxSize - 2; x += 8) {
            for (int z = oz - 2; z <= oz + evalBoxSize - 2; z += 8) {
                int kind = columnKind(t, x, z);
                if (kind == 0) good++;
                else if (kind == 1) edge++;
            }
        }
        int[] offsets = sampleOffsets(evalBoxSize);
        int totalSamples = offsets.length * offsets.length;
        int denominator = totalSamples - edge;
        if (denominator == 0 || 100L * good < (long) St03PlacementConfig.MIN_BIOME_PERCENT * denominator)
            return reject(Verdict.REJECT_FOOTPRINT_BIOME);

        int[] heights = new int[totalSamples];
        int count = 0, water = 0, floorMin = Integer.MAX_VALUE;
        for (int dx : offsets) {
            for (int dz : offsets) {
                int s = t.surfaceHeight(ox + dx, oz + dz);
                int f = t.oceanFloorHeight(ox + dx, oz + dz);
                heights[count++] = s;
                if (s > f + 1) water++;
                floorMin = Math.min(floorMin, f);
            }
        }
        if (!withinWaterLimit(water, heights.length, maxWaterPercent))
            return reject(Verdict.REJECT_WATER);
        Arrays.sort(heights);
        int min = heights[0], max = heights[totalSamples - 1], median = heights[totalSamples / 2];
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
        return withinWaterLimit(water, samples, St03PlacementConfig.MAX_WATER_PERCENT);
    }

    static boolean withinWaterLimit(int water, int samples, int maxWaterPercent) {
        return samples > 0 && 100L * water <= (long) maxWaterPercent * samples;
    }
}
