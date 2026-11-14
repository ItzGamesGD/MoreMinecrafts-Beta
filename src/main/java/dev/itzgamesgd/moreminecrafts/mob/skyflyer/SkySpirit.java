package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * 하늘의 정령 / Sky Spirit: Allay flight, neutral. It never picks a target; when something hurts it, it answers with
 * wind shots and follows the attacker until sight is lost or the attacker is far away (design section 3).
 * It is a separate entity from the Allay: no brain, no item pickup, no duplication, no vibration listening.
 */
public final class SkySpirit extends PathfinderMob {
    private int shotCooldown;
    private int unseenTicks;

    public SkySpirit(EntityType<? extends SkySpirit> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        setCanPickUpLoot(false);
    }

    /**
     * PROVISIONAL stats: the vanilla Allay attribute set (20 HP, 0.1 flying speed). FOLLOW_RANGE is raised to the
     * disengage distance because the vanilla target goal ends a chase beyond it, which would otherwise override
     * {@link SkyFlyerTuning#SPIRIT_DISENGAGE_DISTANCE}.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Allay.createAttributes().add(Attributes.FOLLOW_RANGE, SkyFlyerTuning.SPIRIT_DISENGAGE_DISTANCE);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setRequiredPathLength(48.0F);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new RetaliateGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 input) {
        travelFlying(input, getSpeed());
    }

    @Override
    protected void checkFallDamage(double ySpeed, boolean onGround, BlockState state, BlockPos pos) {
        // Flier: no fall damage, as for the Allay.
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    /** Chase and shoot the current target; ends (and clears the target) per {@link SpiritPursuit}. */
    private static final class RetaliateGoal extends Goal {
        private final SkySpirit spirit;

        RetaliateGoal(SkySpirit spirit) {
            this.spirit = spirit;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = spirit.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = spirit.getTarget();
            if (target == null) return false;
            double distSqr = spirit.distanceToSqr(target);
            return !SpiritPursuit.shouldDisengage(target.isAlive(), distSqr, spirit.unseenTicks);
        }

        @Override
        public void start() {
            spirit.unseenTicks = 0;
        }

        @Override
        public void stop() {
            spirit.setTarget(null);
            spirit.getNavigation().stop();
            spirit.unseenTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = spirit.getTarget();
            if (target == null) return;
            boolean sight = spirit.getSensing().hasLineOfSight(target);
            spirit.unseenTicks = sight ? 0 : spirit.unseenTicks + 1;
            spirit.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSqr = spirit.distanceToSqr(target);
            double standoff = SkyFlyerTuning.SPIRIT_STANDOFF_DISTANCE;
            if (distSqr > standoff * standoff || !sight) {
                spirit.getNavigation().moveTo(target, SkyFlyerTuning.SPIRIT_CHASE_SPEED);
            } else {
                spirit.getNavigation().stop();
            }
            if (spirit.shotCooldown > 0) spirit.shotCooldown--;
            double range = SkyFlyerTuning.SPIRIT_FIRE_RANGE;
            if (sight && spirit.shotCooldown <= 0 && distSqr <= range * range) {
                WindShot.fire(spirit, target);
                spirit.shotCooldown = SkyFlyerTuning.SPIRIT_SHOT_COOLDOWN_TICKS;
            }
        }
    }
}
