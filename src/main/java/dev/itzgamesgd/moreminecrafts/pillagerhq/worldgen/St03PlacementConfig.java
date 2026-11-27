package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

/** Owner-approved V2 site and random-spread parameters (2026-11-16). */
public final class St03PlacementConfig {
    private St03PlacementConfig() {}
    public static final int BOX_SIZE = 112;
    public static final int EVAL_BOX_SIZE = 96;
    public static final int EVAL_MARGIN = (BOX_SIZE - EVAL_BOX_SIZE) / 2;
    public static final int EVAL_SAMPLES = (EVAL_BOX_SIZE / 8 + 1) * (EVAL_BOX_SIZE / 8 + 1);
    public static final int EVAL_COLUMNS = EVAL_BOX_SIZE * EVAL_BOX_SIZE;
    public static final int HEIGHT_SAMPLE_STEP = 8;
    public static final int MAX_HEIGHT_DIFFERENCE = 20;
    public static final int MAX_CUT = 10;
    public static final int MAX_SKIRT = 12;
    public static final int MAX_FOOTING_FILL = 16;
    public static final int MIN_BIOME_PERCENT = 80;
    public static final int MAX_WATER_PERCENT = 15;
    public static final int SEARCH_RADIUS_CHUNKS = 2;
    public static final int SPACING_CHUNKS = 66;
    public static final int SEPARATION_CHUNKS = 22;
    public static final int SALT = 91403;
}
