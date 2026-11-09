package dev.itzgamesgd.moreminecrafts.boss;

import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finds where an entity standing inside a statue's collision box can be shoved to, using real server collision. */
final class TombStatuePlacement {
    private static final int DIRECTIONS = 16;
    private static final double STEP = 0.25D;
    private static final double MAX_SHIFT = 3.0D;
    private static final double CLEARANCE = 0.05D;
    /** A destination must have ground at most this far below the entity's feet. */
    private static final double GROUND_REACH = 1.0D;

    private TombStatuePlacement() {}

    /**
     * Shortest horizontal shift that carries {@code entity} out of {@code statueBox}. The straight path must be free
     * of block and entity collisions all the way, and the destination must be collision-free, dry and supported.
     * Returns {@code null} when no such place exists within reach.
     */
    @Nullable
    static Vec3 findSafeShift(ServerLevel level, LivingEntity entity, AABB statueBox) {
        AABB start = entity.getBoundingBox();
        AABB keepOut = statueBox.inflate(CLEARANCE);
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < DIRECTIONS; i++) {
            double angle = Math.PI * 2.0D * i / DIRECTIONS;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            for (double d = STEP; d <= MAX_SHIFT + 1.0E-9D; d += STEP) {
                AABB moved = start.move(dx * d, 0.0D, dz * d);
                if (!level.noCollision(entity, moved)) break;
                if (moved.intersects(keepOut) || level.containsAnyLiquid(moved)) continue;
                if (level.noCollision(entity, moved.move(0.0D, -GROUND_REACH, 0.0D))) continue;
                if (d < bestDistance) {
                    bestDistance = d;
                    best = new Vec3(dx * d, 0.0D, dz * d);
                }
                break;
            }
        }
        return best;
    }
}
