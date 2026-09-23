package dev.itzgamesgd.moreminecrafts.zombie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Owns only the short-lived, post-death Bomb Zombie countdowns. */
public final class BombZombieEffects {
    private static final List<PendingExplosion> PENDING = new ArrayList<>();

    private BombZombieEffects() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(BombZombieEffects::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    public static void schedule(ServerLevel level, Vec3 position) {
        PENDING.add(new PendingExplosion(level, position, level.getGameTime()));
        level.playSound(null, position.x, position.y, position.z, SoundEvents.LEVER_CLICK,
                SoundSource.HOSTILE, 0.6F, 0.8F);
    }

    private static void tick(MinecraftServer server) {
        Iterator<PendingExplosion> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingExplosion pending = iterator.next();
            if (pending.level.getServer() != server
                    || !pending.level.hasChunkAt(BlockPos.containing(pending.position))) {
                iterator.remove();
                continue;
            }
            long elapsed = pending.level.getGameTime() - pending.startedAt;
            if (elapsed == 10) {
                pending.level.sendParticles(ParticleTypes.SMALL_FLAME, pending.position.x, pending.position.y + 0.5D,
                        pending.position.z, 12, 0.4D, 0.4D, 0.4D, 0.01D);
            } else if (elapsed == 20) {
                pending.level.playSound(null, pending.position.x, pending.position.y, pending.position.z,
                        SoundEvents.LEVER_CLICK, SoundSource.HOSTILE, 0.8F, 1.4F);
            } else if (elapsed >= 30) {
                explode(pending);
                iterator.remove();
            }
        }
    }

    private static void explode(PendingExplosion pending) {
        ServerLevel level = pending.level;
        Vec3 center = pending.position;
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
        AABB area = new AABB(center, center).inflate(ZombieVariantRules.BOMB_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, ServerPlayer::isAlive)) {
            double distance = player.position().distanceTo(center);
            float damage = ZombieVariantRules.bombDamage(distance);
            if (damage <= 0.0F) {
                continue;
            }
            player.hurtServer(level, level.damageSources().generic(), damage);
            knockAway(player, center, 0.6D);
        }
    }

    public static void knockAway(ServerPlayer player, Vec3 center, double strength) {
        Vec3 horizontal = player.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 push = horizontal.normalize().scale(strength);
        player.push(push.x, 0.2D, push.z);
    }

    private record PendingExplosion(ServerLevel level, Vec3 position, long startedAt) {
    }
}
