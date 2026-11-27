package dev.itzgamesgd.moreminecrafts.pyramid;

import net.minecraft.core.BlockPos;

/**
 * Approved 50:50 EAST/WEST secret-access route selection for one naturally generated ST-01 instance (P8-C2-B).
 *
 * <p>Derivation, documented and stable: {@code bit63( mix(worldSeed, "moreminecrafts:st01_route/" + x + "," + y + "," + z, SALT) )},
 * where (x, y, z) is the instance's transformed world-space tomb anchor and {@code mix} is the existing P8-B2 SplitMix64-style
 * hash ({@link RoyalChestRuntime#mix}). 0 selects EAST, 1 selects WEST.
 *
 * <p>The tomb anchor (D1 PRIMARY left, authored (-21,-20,33)) is fixed by the ground anchor and orientation and does not
 * depend on the route, so the selection is a pure function of (world seed, canonical structure identity): independent of
 * chunk generation order, process-global RNG state, restart and regeneration. The dimension, the other half of the canonical
 * identity, is not visible to {@code Structure#findGenerationPoint}; omitting it keeps the selection a function of the
 * identity (two identical anchors in different dimensions simply select the same route).
 */
public final class St01RouteSelection {
    /** Domain-separating salt; distinct from the P8-B2 catalyst (0x1) and chest (0x10+) salts. */
    static final long SALT = 0x53543031524F5554L; // "ST01ROUT"

    private St01RouteSelection() {}

    public static St01PyramidAssembly.AccessRoute select(long worldSeed, BlockPos tombAnchor) {
        long h = RoyalChestRuntime.mix(worldSeed,
                "moreminecrafts:st01_route/" + tombAnchor.getX() + "," + tombAnchor.getY() + "," + tombAnchor.getZ(), SALT);
        return (h >>> 63) == 0 ? St01PyramidAssembly.AccessRoute.EAST : St01PyramidAssembly.AccessRoute.WEST;
    }
}
