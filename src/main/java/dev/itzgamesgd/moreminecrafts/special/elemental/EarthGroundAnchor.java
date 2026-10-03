package dev.itzgamesgd.moreminecrafts.special.elemental;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Resolves a bounded ground surface near the valid Smash's struck entity. */
final class EarthGroundAnchor {
    static final double MAX_HORIZONTAL_DISTANCE = 0.75D;
    static final double MAX_VERTICAL_DROP = 12.0D;
    private static final double SAMPLE_OFFSET = 0.5D;
    private static final List<Vec3> SAMPLE_OFFSETS = List.of(
            Vec3.ZERO,
            new Vec3(SAMPLE_OFFSET, 0.0D, 0.0D), new Vec3(-SAMPLE_OFFSET, 0.0D, 0.0D),
            new Vec3(0.0D, 0.0D, SAMPLE_OFFSET), new Vec3(0.0D, 0.0D, -SAMPLE_OFFSET),
            new Vec3(SAMPLE_OFFSET, 0.0D, SAMPLE_OFFSET), new Vec3(SAMPLE_OFFSET, 0.0D, -SAMPLE_OFFSET),
            new Vec3(-SAMPLE_OFFSET, 0.0D, SAMPLE_OFFSET), new Vec3(-SAMPLE_OFFSET, 0.0D, -SAMPLE_OFFSET));

    private EarthGroundAnchor() {}

    static Optional<Vec3> resolve(ServerPlayer attacker, LivingEntity struck) {
        ServerLevel level = attacker.level();
        Vec3 impactReference = new Vec3(struck.getX(), struck.getY() + 0.25D, struck.getZ());
        List<Vec3> groundHits = new ArrayList<>(SAMPLE_OFFSETS.size());
        for (Vec3 offset : SAMPLE_OFFSETS) {
            Vec3 start = impactReference.add(offset);
            Vec3 end = start.add(0.0D, -MAX_VERTICAL_DROP, 0.0D);
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, attacker));
            if (hit.getType() == HitResult.Type.BLOCK) groundHits.add(hit.getLocation());
        }
        return EarthCombatMath.selectGroundAnchor(impactReference, groundHits,
                MAX_HORIZONTAL_DISTANCE, MAX_VERTICAL_DROP);
    }
}
