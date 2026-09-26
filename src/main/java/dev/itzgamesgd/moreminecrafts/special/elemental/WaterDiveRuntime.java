package dev.itzgamesgd.moreminecrafts.special.elemental;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Minimal server-owned launch/dive/landing state machine; no permanent traversal state. */
public final class WaterDiveRuntime {
    public static final float PILLAR_DAMAGE = 7.0F; // PROVISIONAL
    private static final int TIMEOUT_TICKS = 80;
    private static final Map<UUID, ActiveDive> DIVES = new HashMap<>();

    private WaterDiveRuntime() {}

    public static void initialize() { ServerTickEvents.END_SERVER_TICK.register(WaterDiveRuntime::tick); }

    public static boolean launch(ServerPlayer player) {
        if (DIVES.containsKey(player.getUUID()) || player.isPassenger()) return false;
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 0.35D, 0.85D, look.z * 0.35D);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        DIVES.put(player.getUUID(), new ActiveDive(WaterDiveState.launched(), 0));
        return true;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ActiveDive>> iterator = DIVES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveDive> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ActiveDive active = entry.getValue();
            if (player == null || !player.isAlive() || active.totalTicks >= TIMEOUT_TICKS) {
                iterator.remove();
                continue;
            }
            WaterDiveState next = active.state.tick(player.onGround());
            if (next.phase() == WaterDiveState.Phase.DIVING) beginDive(player);
            if (next.phase() == WaterDiveState.Phase.RESOLVE) resolve(player);
            if (next.phase() == WaterDiveState.Phase.CLEANUP) iterator.remove();
            else entry.setValue(new ActiveDive(next, active.totalTicks + 1));
        }
    }

    private static void beginDive(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-8D) horizontal = horizontal.normalize().scale(0.9D);
        player.setDeltaMovement(horizontal.x, Math.min(-0.9D, look.y * 1.1D), horizontal.z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void resolve(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 horizontal = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (horizontal.lengthSqr() > 1.0E-8D) horizontal = horizontal.normalize();
        Vec3 center = player.position().add(horizontal.scale(2.0D));
        AABB area = new AABB(center.subtract(1.5D, 0.5D, 1.5D), center.add(1.5D, 3.5D, 1.5D));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> ElementalCombat.validTarget(player, target))) {
            target.hurtServer(level, player.damageSources().playerAttack(player), PILLAR_DAMAGE);
        }
        for (double y = 0.2D; y <= 3.8D; y += 0.4D) {
            level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + y, center.z,
                    6, 0.45D, 0.12D, 0.45D, 0.12D);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private record ActiveDive(WaterDiveState state, int totalTicks) {}
}
