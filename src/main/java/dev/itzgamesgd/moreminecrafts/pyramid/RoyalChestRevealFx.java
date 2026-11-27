package dev.itzgamesgd.moreminecrafts.pyramid;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * D13-03 Q39-Q41: the short particle + sound shown when a royal chest's first-open generation produces an accessory.
 * No chat or title. Visible/audible to players within {@link #RADIUS} blocks; the sound attenuates with distance
 * (vanilla playSound at volume 1.0 reaches 16 blocks and fades). It is triggered only by the persisted first-generation
 * transaction in {@link RoyalChestRuntime#generate}, never by opening, item movement, chunk reload or restart.
 * Choices (implementation detail): END_ROD particles and the amethyst chime.
 */
public final class RoyalChestRevealFx {
    public static final double RADIUS = 16.0D;
    private static final double RADIUS_SQR = RADIUS * RADIUS;

    /** One played reveal, for tests. */
    public record Reveal(ServerLevel level, String instanceId, RoyalChestRole role, Vec3 center, List<ServerPlayer> audience) {}

    /** Test observer; null in production. */
    static volatile Consumer<Reveal> observer;

    private RoyalChestRevealFx() {}

    /** Pure range rule shared by the audience filter and unit tests. */
    public static boolean inRange(Vec3 center, Vec3 viewer) { return center.distanceToSqr(viewer) <= RADIUS_SQR; }

    /** Centre of the chest; PRIMARY is a double chest, so the midpoint of its two halves, raised to the lid. */
    static Vec3 center(RoyalChestData.Instance instance, RoyalChestRole role) {
        BlockPos pos = instance.chestPos(role).orElseThrow();
        Vec3 base = Vec3.atCenterOf(pos);
        if (role == RoyalChestRole.PRIMARY && instance.primaryRight().isPresent())
            base = base.add(Vec3.atCenterOf(instance.primaryRight().get())).scale(0.5D);
        return base.add(0.0D, 0.6D, 0.0D);
    }

    static List<ServerPlayer> audience(ServerLevel level, Vec3 center) {
        return level.players().stream().filter(p -> inRange(center, p.position())).toList();
    }

    /** Plays the reveal once for the caller's first generation. Callers guarantee the "once" (see class comment). */
    static void play(ServerLevel level, RoyalChestData.Instance instance, RoyalChestRole role) {
        Vec3 c = center(instance, role);
        List<ServerPlayer> audience = audience(level, c);
        for (ServerPlayer player : audience)
            level.sendParticles(player, ParticleTypes.END_ROD, false, false, c.x, c.y, c.z, 12, 0.35D, 0.25D, 0.35D, 0.02D);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
        Consumer<Reveal> seen = observer;
        if (seen != null) seen.accept(new Reveal(level, instance.id(), role, c, audience));
    }
}
