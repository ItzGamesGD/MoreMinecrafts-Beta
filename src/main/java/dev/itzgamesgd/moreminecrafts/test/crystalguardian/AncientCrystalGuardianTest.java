package dev.itzgamesgd.moreminecrafts.test.crystalguardian;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * POC-only test entity for the converted Blockbench model. It has no targeting, no attack, no drops, no natural spawn and is
 * not saved. Its only behaviours are the operator-triggered test controls below (see {@code AncientCrystalGuardianTestCommand}):
 * a forced straight walk and a forced right-arm swing. The swing is purely visual: no damage, no target search, no collision attack.
 */
public final class AncientCrystalGuardianTest extends PathfinderMob {
    /** Entity event: start the right-arm swing animation on clients. */
    public static final byte EVENT_ARM_SWING = 111;

    /** Client-side animation clock for the swing (driven by {@link #EVENT_ARM_SWING}). */
    public final AnimationState swingAnimationState = new AnimationState();

    private int walkTicks;
    private double walkSpeed = 1.0D;
    private int swingTicksLeft;
    private int swingRepeatsLeft;
    private int swingGapTicks;
    private int swingGapLeft;

    public AncientCrystalGuardianTest(EntityType<? extends AncientCrystalGuardianTest> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new ForcedWalkGoal());
    }

    // ---- test controls (server side) ----

    /** Walk straight ahead for {@code ticks} ticks at {@code speedMultiplier} x base speed. Replaces any running forced walk. */
    public void startWalk(int ticks, double speedMultiplier) {
        this.walkTicks = Math.max(0, ticks);
        this.walkSpeed = Math.max(0.05D, speedMultiplier);
    }

    public void stopWalk() {
        this.walkTicks = 0;
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
        setZza(0.0F);
    }

    /** Starts one arm swing unless one is already running (prevents overlapping/stacked swings). */
    public boolean requestSwing() {
        if (level().isClientSide() || swingTicksLeft > 0) return false;
        swingTicksLeft = GuardianPoseMath.SWING_TICKS;
        level().broadcastEntityEvent(this, EVENT_ARM_SWING);
        return true;
    }

    /** Runs {@code count} swings back to back with {@code gapTicks} of rest between them. */
    public void startSwingLoop(int count, int gapTicks) {
        this.swingRepeatsLeft = Math.max(0, count);
        this.swingGapTicks = Math.max(0, gapTicks);
        this.swingGapLeft = 0;
    }

    /** Cancels walking, swing loops and any pending swing request (an in-flight swing still plays out). */
    public void resetTest() {
        stopWalk();
        swingRepeatsLeft = 0;
    }

    public boolean isForcedWalking() { return walkTicks > 0; }
    public boolean isSwinging() { return swingTicksLeft > 0; }
    public int swingRepeatsLeft() { return swingRepeatsLeft; }
    /** Verification hooks: the entity must only ever carry its single test goal and no target goals. */
    public int goalCount() { return goalSelector.getAvailableGoals().size(); }
    public int targetGoalCount() { return targetSelector.getAvailableGoals().size(); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (swingTicksLeft > 0) swingTicksLeft--;
        if (swingTicksLeft == 0 && swingRepeatsLeft > 0) {
            if (swingGapLeft > 0) swingGapLeft--;
            else if (requestSwing()) { swingRepeatsLeft--; swingGapLeft = swingGapTicks; }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_ARM_SWING) swingAnimationState.start(tickCount);
        else super.handleEntityEvent(id);
    }

    // ---- inert-by-design overrides ----

    @Override public boolean removeWhenFarAway(double distanceSquared) { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(level, source);
    }
    @Override protected boolean shouldDropLoot(ServerLevel level) { return false; }
    @Override protected int getBaseExperienceReward(ServerLevel level) { return 0; }

    /** Test-only locomotion: keeps asking the move control for a point straight ahead; never targets anything. */
    private final class ForcedWalkGoal extends Goal {
        ForcedWalkGoal() { setFlags(EnumSet.of(Goal.Flag.MOVE)); }

        @Override public boolean canUse() { return walkTicks > 0; }
        @Override public boolean canContinueToUse() { return walkTicks > 0; }

        @Override
        public void tick() {
            walkTicks--;
            Vec3 ahead = Vec3.directionFromRotation(0.0F, getYRot()).scale(3.0D).add(position());
            getMoveControl().setWantedPosition(ahead.x, ahead.y, ahead.z, walkSpeed);
        }

        @Override public void stop() { getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D); }
    }
}
