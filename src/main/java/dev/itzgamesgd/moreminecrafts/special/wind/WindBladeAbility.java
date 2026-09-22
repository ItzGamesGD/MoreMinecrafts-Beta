package dev.itzgamesgd.moreminecrafts.special.wind;

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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WindBladeAbility implements ActiveEquipmentAbility {
    public static final WindBladeAbility INSTANCE = new WindBladeAbility();
    public static final Identifier ID = ModIds.id("wind_blade");
    public static final int COOLDOWN_TICKS = 60;
    public static final double RANGE = 12.0D;
    public static final double PATH_RADIUS = 0.8D;
    public static final float DAMAGE = 5.0F;

    private WindBladeAbility() {}
    @Override public Identifier id() { return ID; }
    @Override public int cooldownTicks() { return COOLDOWN_TICKS; }

    @Override
    public boolean activate(ServerPlayer player, ItemStack source) {
        ServerLevel level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 facing = player.getLookAngle().normalize();
        Vec3 requestedEnd = start.add(facing.scale(RANGE));
        HitResult blockHit = level.clip(new ClipContext(start, requestedEnd, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double effectiveRange = blockHit.getType() == HitResult.Type.MISS
                ? RANGE : Math.max(0.0D, start.distanceTo(blockHit.getLocation()) - 0.1D);
        Vec3 end = start.add(facing.scale(effectiveRange));
        AABB search = new AABB(start, end).inflate(PATH_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                target -> validTarget(player, target))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(start);
            if (WindCombatMath.isInsidePath(facing.x, facing.y, facing.z, offset.x, offset.y, offset.z,
                    effectiveRange, PATH_RADIUS + target.getBbWidth() * 0.5D)) {
                target.hurtServer(level, player.damageSources().playerAttack(player), DAMAGE);
            }
        }
        for (double distance = 0.75D; distance <= effectiveRange; distance += 0.75D) {
            Vec3 point = start.add(facing.scale(distance));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z,
                    1, 0.12D, 0.12D, 0.12D, 0.0D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.9F, 1.35F);
        return true;
    }

    private static boolean validTarget(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isAlliedTo(player)
                && (!(target instanceof ServerPlayer other) || player.canHarmPlayer(other));
    }
}
