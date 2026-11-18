package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import java.util.function.IntBinaryOperator;

/**
 * Owner decision 2026-11-17: evaluate the central 96x96 (9,216 columns) in a single pass and
 * recompute foundationY to the closest valid integer in [max - MAX_CUT, min(min + MAX_SKIRT, floorMin + MAX_FOOTING_FILL)].
 */
public final class St03FullTerrainCheck {
    private St03FullTerrainCheck() {}
    public enum Verdict { ELIGIBLE, CUT, SKIRT, FOOTING_FILL }
    public record Result(Verdict verdict, int cut, int skirt, int footingFill, int columnsChecked, int foundationY) {
        public Result(Verdict verdict, int cut, int skirt, int footingFill, int columnsChecked) {
            this(verdict, cut, skirt, footingFill, columnsChecked, 0);
        }
        public boolean eligible() { return verdict == Verdict.ELIGIBLE; }
    }

    public static Result evaluate(IntBinaryOperator surface, IntBinaryOperator floor, int x0, int z0, int sampleFoundationY) {
        return evaluate(surface, floor, x0, z0, sampleFoundationY, St03PlacementConfig.EVAL_BOX_SIZE, true);
    }

    public static Result evaluate(IntBinaryOperator surface, IntBinaryOperator floor, int x0, int z0,
                                  int sampleFoundationY, int evalBoxSize, boolean recomputeFoundationY) {
        int margin = (St03PlacementConfig.BOX_SIZE - evalBoxSize) / 2;
        int ox = x0 + margin, oz = z0 + margin;
        if (!recomputeFoundationY) {
            int cut = 0, skirt = 0, fill = 0, n = 0;
            for (int x = ox; x < ox + evalBoxSize; x++) for (int z = oz; z < oz + evalBoxSize; z++) {
                int s = surface.applyAsInt(x, z), f = floor.applyAsInt(x, z); n++;
                cut = Math.max(cut, s - sampleFoundationY);
                skirt = Math.max(skirt, sampleFoundationY - s);
                fill = Math.max(fill, sampleFoundationY - f);
                if (cut > St03PlacementConfig.MAX_CUT) return new Result(Verdict.CUT, cut, skirt, fill, n, sampleFoundationY);
                if (skirt > St03PlacementConfig.MAX_SKIRT) return new Result(Verdict.SKIRT, cut, skirt, fill, n, sampleFoundationY);
                if (fill > St03PlacementConfig.MAX_FOOTING_FILL) return new Result(Verdict.FOOTING_FILL, cut, skirt, fill, n, sampleFoundationY);
            }
            return new Result(Verdict.ELIGIBLE, cut, skirt, fill, n, sampleFoundationY);
        }
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, floorMin = Integer.MAX_VALUE, n = 0;
        for (int x = ox; x < ox + evalBoxSize; x++) {
            for (int z = oz; z < oz + evalBoxSize; z++) {
                int s = surface.applyAsInt(x, z), f = floor.applyAsInt(x, z);
                n++;
                if (s < min) min = s;
                if (s > max) max = s;
                if (f < floorMin) floorMin = f;
            }
        }
        int low = max - St03PlacementConfig.MAX_CUT;
        int highSkirt = min + St03PlacementConfig.MAX_SKIRT;
        int highFill = floorMin + St03PlacementConfig.MAX_FOOTING_FILL;
        int high = Math.min(highSkirt, highFill);
        if (low <= high) {
            int y = Math.max(low, Math.min(sampleFoundationY, high));
            return new Result(Verdict.ELIGIBLE, Math.max(0, max - y), Math.max(0, y - min), Math.max(0, y - floorMin), n, y);
        }
        int cutAtSample = Math.max(0, max - sampleFoundationY);
        int skirtAtSample = Math.max(0, sampleFoundationY - min);
        int fillAtSample = Math.max(0, sampleFoundationY - floorMin);
        if (cutAtSample > St03PlacementConfig.MAX_CUT) {
            return new Result(Verdict.CUT, cutAtSample, skirtAtSample, fillAtSample, n, sampleFoundationY);
        }
        if (skirtAtSample > St03PlacementConfig.MAX_SKIRT || highSkirt <= highFill) {
            return new Result(Verdict.SKIRT, cutAtSample, skirtAtSample, fillAtSample, n, sampleFoundationY);
        }
        return new Result(Verdict.FOOTING_FILL, cutAtSample, skirtAtSample, fillAtSample, n, sampleFoundationY);
    }

    /**
     * Deterministic pre-generation boundary water filter:
     * Rejects a site if any column in the 1-block outer ring surrounding the 112x112 cut box
     * (including the gatehouse entrance perimeter) contains water at or above foundationY + 1.
     */
    public static boolean hasBoundaryWaterCutContact(IntBinaryOperator surface, IntBinaryOperator floor,
                                                     int originX, int originZ, int foundationY) {
        int box = St03PlacementConfig.BOX_SIZE;
        int minWaterSurface = foundationY + 2; // top water block y = s - 1 >= foundationY + 1 <=> s >= foundationY + 2
        for (int i = 0; i < box; i++) {
            int sN = surface.applyAsInt(originX + i, originZ - 1);
            if (sN >= minWaterSurface && sN > floor.applyAsInt(originX + i, originZ - 1) + 1) return true;

            int sS = surface.applyAsInt(originX + i, originZ + box);
            if (sS >= minWaterSurface && sS > floor.applyAsInt(originX + i, originZ + box) + 1) return true;

            int sW = surface.applyAsInt(originX - 1, originZ + i);
            if (sW >= minWaterSurface && sW > floor.applyAsInt(originX - 1, originZ + i) + 1) return true;

            int sE = surface.applyAsInt(originX + box, originZ + i);
            if (sE >= minWaterSurface && sE > floor.applyAsInt(originX + box, originZ + i) + 1) return true;
        }
        return false;
    }
}
