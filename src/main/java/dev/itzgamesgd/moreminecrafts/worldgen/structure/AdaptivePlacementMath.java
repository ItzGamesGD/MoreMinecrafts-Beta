package dev.itzgamesgd.moreminecrafts.worldgen.structure;

/** Pure arithmetic of the terrain-adaptive Sky Island placement (no engine classes, unit-testable). */
public final class AdaptivePlacementMath {
    private AdaptivePlacementMath() {}

    /**
     * Highest terrain-relative offset h for which the root piece's maxY stays within {@code levelMaxYInclusive - paddingTop}.
     * Engine: rootMinY = h + T (T = top terrain block), rootMaxY = rootMinY + sizeY - 1.
     * 97-block tower, Overworld (319), padding 4: 315 - T - 96 = 219 - T.
     */
    public static int feasibleMaxOffset(int levelMaxYInclusive, int paddingTop, int topY, int sizeY) {
        return levelMaxYInclusive - paddingTop - topY - (sizeY - 1);
    }

    /** min(preferredMax, feasibleMax); a result below {@code minOffset} means the candidate must be rejected. */
    public static int effectiveMaxOffset(int preferredMax, int feasibleMax) {
        return Math.min(preferredMax, feasibleMax);
    }

    public static boolean feasible(int minOffset, int preferredMax, int feasibleMax) {
        return effectiveMaxOffset(preferredMax, feasibleMax) >= minOffset;
    }
}
