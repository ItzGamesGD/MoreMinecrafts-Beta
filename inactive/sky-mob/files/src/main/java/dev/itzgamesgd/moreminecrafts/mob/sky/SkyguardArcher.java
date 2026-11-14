package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.mixin.AbstractArrowAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 천궁 사수 (Q08, Q09, D4): Skeleton bow AI with 30 HP and +40 % arrow base damage.
 *
 * <p>It extends AbstractSkeleton (the base of Skeleton, Stray, Bogged, Parched) rather than Skeleton, because the
 * powder-snow Stray conversion lives in Skeleton itself. D4 forbids that conversion, so it is structurally absent.
 */
public final class SkyguardArcher extends AbstractSkeleton {
    public SkyguardArcher(EntityType<? extends AbstractSkeleton> type, Level level) {
        super(type, level);
    }

    @Override
    protected AbstractArrow getArrow(ItemStack ammo, float power, ItemStack weapon) {
        AbstractArrow arrow = super.getArrow(ammo, power, weapon);
        double base = ((AbstractArrowAccessor) arrow).moreminecrafts$getBaseDamage();
        arrow.setBaseDamage(base * SkyMobTraits.ARCHER_ARROW_DAMAGE_MULTIPLIER);
        return arrow;
    }

    /** Q55: fixed bow; no random enchantments, armor, pumpkin, pickup or equipment drops. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        SkyMobTraits.applyFixedLoadout(this, new ItemStack(Items.BOW));
        reassessWeaponGoal();
        return result;
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random,
                                                        DifficultyInstance difficulty) {
        // Q55: the bow is a fixed item.
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return SkyMobTraits.isImmuneDamage(source) || super.isInvulnerableTo(level, source);
    }

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
