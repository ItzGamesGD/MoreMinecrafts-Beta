package dev.itzgamesgd.moreminecrafts.pillagerhq;

import java.util.Arrays;
import java.util.Set;

/** Measurement-only alternatives. No production caller and no writes to worldgen policy. */
public final class St03SiteV2Rules {
    private St03SiteV2Rules() {}
    public static final Set<String> S0 = Set.of("plains", "sunflower_plains", "meadow", "forest", "flower_forest",
            "birch_forest", "old_growth_birch_forest", "dark_forest");
    public static final Set<String> S1_EXTRA = Set.of("taiga", "cherry_grove", "savanna");
    public static final Set<String> S2_EXTRA = Set.of("snowy_plains", "desert");
    public static final Set<String> EDGE = Set.of("beach", "snowy_beach", "stony_shore", "river", "frozen_river");
    public static boolean allowed(String id, int set) {
        return S0.contains(id) || set >= 1 && S1_EXTRA.contains(id) || set >= 2 && S2_EXTRA.contains(id);
    }
    public record Limits(String name, int span, int cut, int skirt, int fill) {}
    public static final Limits[] LIMITS = {
        new Limits("L0", 12, 3, 5, 6), new Limits("L1", 12, 6, 8, 10),
        new Limits("L2", 20, 10, 12, 16), new Limits("L3", 32, 16, 20, 24)
    };
    public record Sample(int centerSet, int[] good, int edge, int biomeSamples, int water, int heightSamples,
                         int min, int p05, int median, int p95, int max, int floorMin, int[] surfaceHeights) {
        public int rawSpread() { return max - min; }
        public int robustSpread() { return p95 - p05; }
    }
    public record Decision(String verdict, int foundation, int cut, int skirt, int fill) {
        public boolean pass() { return verdict.equals("PASS"); }
    }
    /** Edge cells are neutral: excluded from eligible/(non-edge) coverage, never eligible centers. */
    public static String biomeVerdict(Sample s, int set, int coverage, boolean neutralEdges) {
        if (s.centerSet > set) return "CENTER_BIOME";
        int denominator = s.biomeSamples - (neutralEdges ? s.edge : 0);
        if (denominator == 0 || 100L * s.good[set] < (long) coverage * denominator) return "FOOTPRINT_BIOME";
        return "PASS";
    }
    /** Full sampled extrema still constrain earthwork; percentile trimming cannot hide an unfillable outlier. */
    public static Decision evaluate(Sample s, int set, int coverage, int waterPercent, boolean neutralEdges,
                                    boolean robust, Limits limits) {
        String b = biomeVerdict(s, set, coverage, neutralEdges);
        if (!b.equals("PASS")) return new Decision(b, 0, 0, 0, 0);
        if (100L * s.water > (long) waterPercent * s.heightSamples) return new Decision("WATER", 0, 0, 0, 0);
        if ((robust ? s.robustSpread() : s.rawSpread()) > limits.span) return new Decision("HEIGHT", 0, 0, 0, 0);
        // Feasible foundation interval, then choose median clamped into it (minimises absolute cut/fill work).
        int low = s.max - limits.cut;
        int highSkirt = s.min + limits.skirt;
        int highFill = s.floorMin + limits.fill;
        if (low > highSkirt) return new Decision("CUT_SKIRT", 0, 0, 0, 0);
        if (low > highFill) return new Decision("FOOTING_FILL", 0, 0, 0, 0);
        int y = Math.max(low, Math.min(s.median, Math.min(highSkirt, highFill)));
        return new Decision("PASS", y, Math.max(0, s.max - y), Math.max(0, y - s.min), Math.max(0, y - s.floorMin));
    }
    public static int percentile(int[] sorted, double fraction) {
        if (sorted.length == 0) throw new IllegalArgumentException("empty population");
        return sorted[Math.max(0, (int) Math.ceil(sorted.length * fraction) - 1)];
    }
    public static int[] sorted(int[] values) { int[] out = values.clone(); Arrays.sort(out); return out; }
}
