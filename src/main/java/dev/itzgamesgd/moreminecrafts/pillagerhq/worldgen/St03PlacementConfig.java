package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

/**
 * Fixed ST-03 Pillager Headquarters worldgen placement & terrain thresholds.
 * Outer compound is 112x112 (x=0..111, z=0..111 in local coordinates).
 */
public final class St03PlacementConfig {
    private St03PlacementConfig() {}

    /** Outer compound side length in blocks (112x112). */
    public static final int BOX_SIZE = 112;

    /** Sampling step across the 112x112 footprint during candidate evaluation. */
    public static final int HEIGHT_SAMPLE_STEP = 4;

    /** Approved ~4-5 block maximum natural ground elevation spread across the 112x112 compound. */
    public static final int MAX_HEIGHT_DIFFERENCE = 5;

    /** Maximum cut above foundationY before rejecting high-side terrain protrusion. */
    public static final int MAX_CUT = 3;

    /** Maximum downward footing skirt below foundationY. */
    public static final int MAX_SKIRT = 5;

    /** Maximum downward vertical footing fill depth for wall/building columns. */
    public static final int MAX_FOOTING_FILL = 6;
}
