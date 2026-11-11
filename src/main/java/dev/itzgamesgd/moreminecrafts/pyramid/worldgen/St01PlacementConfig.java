package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

/**
 * ST-01 Pyramid natural-placement parameters.
 *
 * <p>The structure-set and MODERATE terrain values are OWNER-APPROVED AND LOCKED (P8-C2-B brief §2, authority record
 * {@code docs/design/st01-p8c2b/AUTHORITY-AMENDMENT-O14-MODERATE.md}): do not change any of them without explicit owner
 * approval. {@code St01WorldgenResourceTest} asserts that the committed structure-set JSON equals these constants.
 *
 * <p>The "implementation parameters" below are HOW-level definitions, not design values; they are reported in the
 * P8-C2-B report for owner review.
 */
public final class St01PlacementConfig {
    private St01PlacementConfig() {}

    // ---- structure set (data/moreminecrafts/worldgen/structure_set/st01_pyramid.json) ----
    public static final int SPACING_CHUNKS = 32;
    public static final int SEPARATION_CHUNKS = 8;
    public static final String SPREAD_TYPE = "triangular";
    public static final int SALT = 91401;

    // ---- footprint: 81 x 81 blocks, centred on the anchor block (-40..+40) ----
    public static final int FOOTPRINT_HALF_WIDTH = 40;
    public static final int FOOTPRINT_WIDTH = 2 * FOOTPRINT_HALF_WIDTH + 1;

    // ---- MODERATE terrain policy (owner-approved, supersedes G0-A 12/8/6 for worldgen terrain only) ----
    public static final int MAX_HEIGHT_DIFFERENCE = 16;
    public static final int MAX_CUT = 10;
    public static final int MAX_SKIRT = 8;

    // ---- implementation parameters (HOW, not owner-approved design values) ----
    /** Every footprint column is sampled (6,561), so the approved limits hold for every column, not just a grid. */
    public static final int HEIGHT_SAMPLE_STEP = 1;
    /** Plants placed above the natural terrain before the correction runs are cleared up to this many blocks above it. */
    public static final int VEGETATION_CLEARANCE = 8;
}
