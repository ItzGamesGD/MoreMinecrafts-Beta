package dev.itzgamesgd.moreminecrafts.special.elemental;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.itzgamesgd.moreminecrafts.special.vfx.T2ElementalVfx;

/** Minimal server-owned launch/dive/landing state machine; no permanent traversal state. */
public final class WaterDiveRuntime {
    public static final float PILLAR_DAMAGE = 7.0F; // PROVISIONAL
    private static final int TIMEOUT_TICKS = 80;
    private static final Map<UUID, ActiveDive> DIVES = new HashMap<>();

    private WaterDiveRuntime() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(WaterDiveRuntime::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(WaterDiveRuntime::onServerStopped);
        ServerTickEvents.END_SERVER_TICK.register(WaterDiveRuntime::tick);
    }

    static void onServerStarting(MinecraftServer server) { clearRuntimeState(); }

    static void onServerStopped(MinecraftServer server) { clearRuntimeState(); }

    static void clearRuntimeState() { DIVES.clear(); }

    static void trackDive(UUID playerId, ResourceKey<Level> originLevel) {
        DIVES.put(playerId, new ActiveDive(WaterDiveState.launched(), 0, originLevel));
    }

    static int activeDiveCount() { return DIVES.size(); }

    static boolean isDiveInOriginLevel(UUID playerId, ResourceKey<Level> currentLevel) {
        ActiveDive active = DIVES.get(playerId);
        return active != null && active.originLevel.equals(currentLevel);
    }

    public static boolean launch(ServerPlayer player) {
        if (DIVES.containsKey(player.getUUID()) || player.isPassenger()) return false;
        Vec3 look = player.getLookAngle();
        Vec3 launchVelocity = WaterDiveMotion.launchVelocity(look);
        player.setDeltaMovement(launchVelocity);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        trackDive(player.getUUID(), player.level().dimension());
        T2ElementalVfx.waterLaunch(player.level(), player.position());
        return true;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ActiveDive>> iterator = DIVES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveDive> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ActiveDive active = entry.getValue();
            if (WaterDiveState.shouldExpire(player != null, player != null && player.isAlive(),
                    player != null && active.originLevel.equals(player.level().dimension()),
                    active.totalTicks, TIMEOUT_TICKS)) {
                iterator.remove();
                continue;
            }
            if (active.state.phase() == WaterDiveState.Phase.WAITING_FOR_GROUND) {
                Vec3 end = player.position();
                T2ElementalVfx.waterDiveTrail(player.level(), end.subtract(player.getDeltaMovement()), end);
            }
            WaterDiveState next = active.state.tick(player.onGround());
            if (next.phase() == WaterDiveState.Phase.DIVING) beginDive(player);
            if (next.phase() == WaterDiveState.Phase.RESOLVE) {
                next = next.withImpactCenter(resolve(player));
            }
            if (next.phase() == WaterDiveState.Phase.PILLAR && next.impactCenter() != null) {
                pillar(player.level(), next.impactCenter());
            }
            if (next.phase() == WaterDiveState.Phase.CLEANUP) iterator.remove();
            else entry.setValue(new ActiveDive(next, active.totalTicks + 1, active.originLevel));
        }
    }

    private static void beginDive(ServerPlayer player) {
        player.setDeltaMovement(WaterDiveMotion.diveVelocity(player.getLookAngle()));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static Vec3 resolve(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 horizontal = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (horizontal.lengthSqr() > 1.0E-8D) horizontal = horizontal.normalize();
        Vec3 center = player.position().add(horizontal.scale(2.0D));
        AABB area = new AABB(center.subtract(1.5D, 0.5D, 1.5D), center.add(1.5D, 3.5D, 1.5D));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> ElementalCombat.validTarget(player, target))) {
            target.hurtServer(level, player.damageSources().playerAttack(player), PILLAR_DAMAGE);
        }
        T2ElementalVfx.waterLanding(level, center);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        return center;
    }

    private static void pillar(ServerLevel level, Vec3 center) {
        T2ElementalVfx.waterPillar(level, center);
    }

    private record ActiveDive(WaterDiveState state, int totalTicks, ResourceKey<Level> originLevel) {}
}
