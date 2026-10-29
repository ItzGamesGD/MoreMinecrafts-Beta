package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/**
 * Arena combat-zone geometry supplied by the structure (or a test fixture). {@code center} is the feet-level block
 * at the exact arena center above the central Ominous Vault floor. The combat zone is a vertical cylinder of
 * {@code combatRadius} between {@code minY} and {@code maxY}; mob feet positions are searched in the fixed 12-24
 * annulus between {@code spawnMinY} and {@code spawnMaxY}. Exact production values are structure/implementation
 * work; {@link #testFixture} is only for arenas built without the natural structure.
 */
public record ColosseumArenaGeometry(BlockPos center, int combatRadius, int minY, int maxY, double activationRadius,
                                     int spawnMinY, int spawnMaxY) {
    public static final Codec<ColosseumArenaGeometry> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("center").forGetter(ColosseumArenaGeometry::center),
            Codec.INT.fieldOf("combat_radius").forGetter(ColosseumArenaGeometry::combatRadius),
            Codec.INT.fieldOf("min_y").forGetter(ColosseumArenaGeometry::minY),
            Codec.INT.fieldOf("max_y").forGetter(ColosseumArenaGeometry::maxY),
            Codec.DOUBLE.fieldOf("activation_radius").forGetter(ColosseumArenaGeometry::activationRadius),
            Codec.INT.fieldOf("spawn_min_y").forGetter(ColosseumArenaGeometry::spawnMinY),
            Codec.INT.fieldOf("spawn_max_y").forGetter(ColosseumArenaGeometry::spawnMaxY)
    ).apply(i, ColosseumArenaGeometry::new));

    public ColosseumArenaGeometry {
        center = center.immutable();
        if (combatRadius < ColosseumRules.SPAWN_OUTER_RADIUS)
            throw new IllegalArgumentException("the combat zone must contain the 12-24 spawn annulus");
        if (!(activationRadius > 0.0D && activationRadius < ColosseumRules.SPAWN_INNER_RADIUS))
            throw new IllegalArgumentException("activation radius must lie inside the spawn annulus");
        if (!(minY <= spawnMinY && spawnMinY <= spawnMaxY && spawnMaxY <= maxY && minY <= center.getY() && center.getY() <= maxY))
            throw new IllegalArgumentException("vertical bounds must contain the center and the spawn band");
    }

    /** Flat test floor: zone radius 26, 2 blocks below to 10 above center, mob feet on the floor level +/- 0..1. */
    public static ColosseumArenaGeometry testFixture(BlockPos center) {
        return new ColosseumArenaGeometry(center, 26, center.getY() - 2, center.getY() + 10, 2.0D,
                center.getY(), center.getY() + 1);
    }

    public static double horizontalDistanceSqr(BlockPos center, double x, double z) {
        double dx = x - (center.getX() + 0.5D), dz = z - (center.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    public boolean inCombatZone(double x, double y, double z) {
        return y >= minY && y <= maxY + 1 && horizontalDistanceSqr(center, x, z) <= (double) combatRadius * combatRadius;
    }

    public boolean atActivation(double x, double y, double z) {
        return y >= minY && y <= maxY + 1 && horizontalDistanceSqr(center, x, z) <= activationRadius * activationRadius;
    }

    /** Whether a block column's center lies in the fixed 12-24 radial annulus and its feet Y in the spawn band. */
    public boolean inSpawnAnnulus(BlockPos pos) {
        double distance = horizontalDistanceSqr(center, pos.getX() + 0.5D, pos.getZ() + 0.5D);
        return pos.getY() >= spawnMinY && pos.getY() <= spawnMaxY
                && distance >= ColosseumRules.SPAWN_INNER_RADIUS * ColosseumRules.SPAWN_INNER_RADIUS
                && distance <= ColosseumRules.SPAWN_OUTER_RADIUS * ColosseumRules.SPAWN_OUTER_RADIUS;
    }
}
