package dev.itzgamesgd.moreminecrafts.special.flame;

import dev.itzgamesgd.moreminecrafts.ability.ActiveEquipmentAbility;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FlameCleaveAbility implements ActiveEquipmentAbility {
    public static final FlameCleaveAbility INSTANCE = new FlameCleaveAbility();
    public static final Identifier ID = ModIds.id("flame_cleave");
    public static final int COOLDOWN_TICKS = 600; // AUTHORITATIVE 30 seconds
    public static final double RANGE = 4.0D;
    public static final double HALF_ANGLE_DEGREES = 45.0D;
    public static final float DAMAGE = 10.0F; // PROVISIONAL live-test damage
    public static final float IGNITE_SECONDS = 3.0F;

    private FlameCleaveAbility() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack source) {
        ServerLevel level = player.level();
        Vec3 facing = player.getLookAngle();
        AABB search = player.getBoundingBox().inflate(RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                target -> isValidTarget(player, target))) {
            if (!FlameCombatMath.isInsideHorizontalCone(
                    facing.x, facing.z,
                    target.getX() - player.getX(), target.getZ() - player.getZ(),
                    RANGE, HALF_ANGLE_DEGREES)) {
                continue;
            }
            if (target.hurtServer(level, player.damageSources().playerAttack(player), DAMAGE)) {
                target.igniteForSeconds(IGNITE_SECONDS);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(),
                        5, 0.25D, 0.35D, 0.25D, 0.01D);
            }
        }

        // The prototype is a geometric cone; obstacle occlusion is deliberately not applied yet.
        for (int step = 1; step <= 4; step++) {
            double distance = step;
            level.sendParticles(ParticleTypes.FLAME,
                    player.getX() + facing.x * distance, player.getY() + 1.0D,
                    player.getZ() + facing.z * distance, 4,
                    distance * 0.18D, 0.18D, distance * 0.18D, 0.01D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.isAlliedTo(player)) {
            return false;
        }
        return !(target instanceof ServerPlayer targetPlayer) || player.canHarmPlayer(targetPlayer);
    }
}
