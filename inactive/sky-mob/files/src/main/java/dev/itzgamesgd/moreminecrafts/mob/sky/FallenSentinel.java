package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * 타락한 파수병 (Q19-Q23): Husk-line melee mob, 40 HP / 6 attack / 0.23 speed, the only undead of the five.
 *
 * <p>It extends Zombie and mirrors the Husk's sounds, sun immunity and water behaviour instead of extending Husk:
 * Husk.doHurtTarget applies Hunger (Q23 replaces it) and Husk.finalizeSpawn can add camel-husk / parched jockeys,
 * i.e. extra entities that P1 must not create.
 */
public final class FallenSentinel extends SkyZombieBase {
    public FallenSentinel(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    /** Q21 / Q23 / Q76: 40 % chance, Weakness I, 3 seconds, independent of difficulty. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living && SkyMobTraits.rollWeakness(getRandom())) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SkyMobTraits.SENTINEL_WEAKNESS_TICKS, 0), this);
        }
        return hit;
    }

    /** Vanilla Husk water behaviour is inherited (Husk -> Zombie, never Drowned: Q48 / V1 report). */
    @Override
    protected boolean convertsInWater() {
        return true;
    }

    @Override
    protected EntityType<? extends Zombie> convertsToWhenDrowning() {
        return EntityTypes.ZOMBIE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.HUSK_STEP;
    }
}
