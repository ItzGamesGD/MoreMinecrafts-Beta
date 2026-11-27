package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

/**
 * ST-01 Pyramid natural-placement parameters. OWNER-APPROVED AND LOCKED (Phase 4 brief, section 2): do not change any value
 * without explicit owner approval. {@code St01WorldgenResourceTest} asserts that the committed structure-set JSON equals these constants.
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

    // ---- MODERATE terrain policy ----
    public static final int MAX_HEIGHT_DIFFERENCE = 16;
    public static final int MAX_CUT = 10;
    public static final int MAX_SKIRT = 8;

    // ---- implementation parameters (not owner-approved design values; HOW) ----
    /**
     * Height sampling step. The O-14 diagnostics sampled a 4-block grid (441 columns); Phase 4 samples EVERY footprint column (6561) because a
     * sampled grid cannot guarantee the approved limits for the columns it skips. The definitions (max-min, cut = max-median, skirt = median-min)
     * are unchanged. Cost is bounded: only anchors that already passed the full-footprint Desert stage reach this stage.
     */
    public static final int HEIGHT_SAMPLE_STEP = 1;
    /** Blocks added around the underground volume when testing for fluid (adjacent aquifer/water breach). */
    public static final int FLUID_MARGIN = 1;
}
