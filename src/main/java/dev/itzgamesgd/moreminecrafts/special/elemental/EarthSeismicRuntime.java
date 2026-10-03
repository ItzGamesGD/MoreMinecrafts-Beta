package dev.itzgamesgd.moreminecrafts.special.elemental;

import dev.itzgamesgd.moreminecrafts.special.vfx.T2ElementalVfx;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Short-lived server-owned Earth smash waves; presentation never controls target eligibility. */
public final class EarthSeismicRuntime {
    public static final double MAX_RADIUS = 6.0D; // PROVISIONAL: live-tunable
    public static final double EXPANSION_PER_TICK = 1.0D; // PROVISIONAL: live-tunable
    public static final double GROUND_CONTACT_TOLERANCE = 0.75D; // PROVISIONAL: live-tunable
    public static final float DAMAGE = 3.0F; // PROVISIONAL: live-tunable
    public static final int SLOW_DURATION_TICKS = 40; // PROVISIONAL: live-tunable
    public static final int SLOW_AMPLIFIER = 0; // PROVISIONAL: live-tunable
    public static final double OUTWARD_IMPULSE = 0.85D; // PROVISIONAL: live-tunable

    private static final List<Wave> WAVES = new ArrayList<>();

    private EarthSeismicRuntime() {}

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
        ServerTickEvents.END_SERVER_TICK.register(EarthSeismicRuntime::tick);
    }

    public static void start(ServerPlayer attacker, Vec3 impact) {
        WAVES.add(new Wave(attacker.getUUID(), attacker.level().dimension(), impact));
        T2ElementalVfx.earthLanding(attacker.level(), impact);
    }

    static int activeWaveCount() { return WAVES.size(); }

    static void clear() { WAVES.clear(); }

    private static void tick(MinecraftServer server) {
        Iterator<Wave> waves = WAVES.iterator();
        while (waves.hasNext()) {
            Wave wave = waves.next();
            ServerPlayer attacker = server.getPlayerList().getPlayer(wave.attackerId);
            if (attacker == null || !attacker.isAlive() || !wave.dimension.equals(attacker.level().dimension())) {
                waves.remove();
                continue;
            }

            double outerRadius = Math.min(MAX_RADIUS, wave.radius + EXPANSION_PER_TICK);
            double innerRadius = wave.radius;
            ServerLevel level = attacker.level();
            AABB search = new AABB(wave.impact.x - outerRadius, wave.impact.y - GROUND_CONTACT_TOLERANCE,
                    wave.impact.z - outerRadius, wave.impact.x + outerRadius,
                    wave.impact.y + GROUND_CONTACT_TOLERANCE + 2.5D, wave.impact.z + outerRadius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                    entity -> ElementalCombat.validTarget(attacker, entity)
                            && !wave.affected.contains(entity.getUUID()))) {
                double dx = target.getX() - wave.impact.x;
                double dz = target.getZ() - wave.impact.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (!EarthCombatMath.isGroundedTarget(target.onGround(), target.getY() - wave.impact.y,
                        GROUND_CONTACT_TOLERANCE)
                        || !EarthCombatMath.inExpandingRing(distance, innerRadius, outerRadius)) continue;
                if (target.hurtServer(level, attacker.damageSources().playerAttack(attacker), DAMAGE)) {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                            SLOW_DURATION_TICKS, SLOW_AMPLIFIER));
                    Vec3 impulse = EarthCombatMath.radialOutwardImpulse(wave.impact, target.position(), OUTWARD_IMPULSE);
                    target.push(impulse.x, 0.0D, impulse.z);
                    wave.affected.add(target.getUUID());
                }
            }
            T2ElementalVfx.earthSeismicRing(level, wave.impact, outerRadius);
            wave.radius = outerRadius;
            if (wave.radius >= MAX_RADIUS) waves.remove();
        }
    }

    private static final class Wave {
        private final UUID attackerId;
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final Vec3 impact;
        private final Set<UUID> affected = new HashSet<>();
        private double radius;

        private Wave(UUID attackerId, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                     Vec3 impact) {
            this.attackerId = attackerId;
            this.dimension = dimension;
            this.impact = impact;
        }
    }
}
