package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.phys.Vec3;

/** Shared wind-projectile launch for the Sky Spirit and Storm Hunter. PROVISIONAL: uses the vanilla Breeze wind charge. */
final class WindShot {
    private WindShot() {}

    /** Launches one wind charge from {@code shooter} toward {@code target}; returns the spawned projectile. */
    static BreezeWindCharge fire(Mob shooter, LivingEntity target) {
        ServerLevel level = (ServerLevel) shooter.level();
        Vec3 from = shooter.getEyePosition();
        Vec3 aim = new Vec3(target.getX(), target.getY(0.5D), target.getZ()).subtract(from);
        BreezeWindCharge charge = new BreezeWindCharge(EntityTypes.BREEZE_WIND_CHARGE, level);
        charge.setOwner(shooter);
        charge.setPos(from.x, from.y, from.z);
        charge.shoot(aim.x, aim.y, aim.z, SkyFlyerTuning.WIND_SHOT_SPEED, SkyFlyerTuning.WIND_SHOT_INACCURACY);
        level.addFreshEntity(charge);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.WIND_CHARGE_THROW,
                SoundSource.HOSTILE, 1.0F, 1.0F);
        return charge;
    }
}
