package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.Level;

/**
 * Crypt Archer: vanilla Skeleton bow AI, attributes, sounds and freeze rules, without the powder-snow Stray
 * conversion (owner decision 2026-11-14). That conversion lives inside the vanilla Skeleton class, so this extends
 * AbstractSkeleton (the shared base of Skeleton, Stray, Bogged) and restates the few Skeleton-specific members.
 */
public final class CryptArcher extends AbstractSkeleton {
    public CryptArcher(EntityType<? extends AbstractSkeleton> type, Level level) {
        super(type, level);
    }

    /** Vanilla Skeleton.canFreeze() is false. */
    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.SKELETON_STEP;
    }
}
