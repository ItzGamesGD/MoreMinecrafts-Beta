package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 폭풍의 사냥꾼 / Storm Hunter: Phantom body and flight physics with its own aggressive loop
 * (circle, wind shot, dive, rise; design section 3). The vanilla Phantom circle/sweep goals are not used; the loop is
 * driven by {@link HunterPattern}. It is a separate entity type, so the vanilla insomnia spawner never sees it.
 */
public final class StormHunter extends Phantom {
    private final HunterPattern pattern = new HunterPattern();
    private final HunterMoveControl steering;
    private Vec3 patrolCenter;
    private double orbitAngle;
    private boolean diveHit;

    public StormHunter(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        this.steering = new HunterMoveControl(this);
        this.moveControl = steering;
    }

    /**
     * PROVISIONAL: the vanilla Phantom attribute set ({@code Monster.createMonsterAttributes()}: 20 HP, attack
     * damage 2). Measured in GameTest: a vanilla Phantom of size 0 also keeps attack damage 2, because the 6 + size
     * update only runs when the size changes. FOLLOW_RANGE is set to {@link SkyFlyerTuning#HUNTER_TARGET_RANGE}
     * (the vanilla Phantom's 64-block attack range) so target acquisition uses that constant.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, SkyFlyerTuning.HUNTER_TARGET_RANGE);
    }

    public HunterPattern pattern() {
        return pattern;
    }

    @Override
    protected void registerGoals() {
        // Deliberately not calling super: the vanilla circle/sweep goals are replaced by the hunter loop.
        goalSelector.addGoal(1, new EngageGoal(this));
        goalSelector.addGoal(2, new PatrolGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private Vec3 center() {
        if (patrolCenter == null) patrolCenter = position().add(0.0D, 5.0D, 0.0D);
        return patrolCenter;
    }

    /** Steering for the hunter: accelerate toward a point, turn the body and pitch toward the motion. */
    private static final class HunterMoveControl extends MoveControl<StormHunter> {
        private Vec3 target;
        private double speed = SkyFlyerTuning.HUNTER_CIRCLE_SPEED;

        HunterMoveControl(StormHunter mob) {
            super(mob);
        }

        void steer(Vec3 point, double speed) {
            this.target = point;
            this.speed = speed;
        }

        @Override
        public boolean hasWanted() {
            return target != null;
        }

        @Override
        public void tick() {
            if (target == null) return;
            Vec3 delta = target.subtract(mob.position());
            double dist = delta.length();
            if (dist < 1.0E-4D) return;
            Vec3 dir = delta.scale(1.0D / dist);
            mob.setDeltaMovement(mob.getDeltaMovement().add(dir.scale(speed * 0.5D)));
            float yawGoal = (float) (Mth.atan2(delta.z, delta.x) * 57.2957763671875D) - 90.0F;
            mob.setYRot(Mth.approachDegrees(mob.getYRot(), yawGoal, 10.0F));
            mob.yBodyRot = mob.getYRot();
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float pitchGoal = (float) -(Mth.atan2(delta.y, horizontal) * 57.2957763671875D);
            mob.setXRot(Mth.approachDegrees(mob.getXRot(), pitchGoal, 6.0F));
            if (mob.horizontalCollision) mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, 0.04D, 0.0D));
        }
    }

    /** Wander in a slow orbit around the point where the hunter appeared while nothing is targeted. */
    private static final class PatrolGoal extends Goal {
        private final StormHunter hunter;

        PatrolGoal(StormHunter hunter) {
            this.hunter = hunter;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return hunter.getTarget() == null;
        }

        @Override
        public void tick() {
            hunter.orbitAngle += SkyFlyerTuning.HUNTER_CIRCLE_RATE;
            Vec3 c = hunter.center();
            double r = SkyFlyerTuning.HUNTER_CIRCLE_RADIUS;
            hunter.steering.steer(new Vec3(c.x + Math.cos(hunter.orbitAngle) * r, c.y, c.z + Math.sin(hunter.orbitAngle) * r),
                    SkyFlyerTuning.HUNTER_CIRCLE_SPEED);
        }
    }

    /** The aggressive loop. State lives in {@link HunterPattern}; this goal turns each phase into movement/attacks. */
    private static final class EngageGoal extends Goal {
        private final StormHunter hunter;

        EngageGoal(StormHunter hunter) {
            this.hunter = hunter;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = hunter.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            hunter.pattern.reset();
            hunter.diveHit = false;
            Vec3 offset = hunter.position().subtract(hunter.getTarget().position());
            hunter.orbitAngle = Mth.atan2(offset.z, offset.x);
        }

        @Override
        public void stop() {
            hunter.pattern.reset();
            hunter.steering.target = null;
        }

        @Override
        public void tick() {
            LivingEntity target = hunter.getTarget();
            if (target == null) return;
            Vec3 tp = target.position();
            boolean diveFinished = false;
            boolean risenEnough = false;
            switch (hunter.pattern.phase()) {
                case CIRCLE -> {
                    hunter.orbitAngle += SkyFlyerTuning.HUNTER_CIRCLE_RATE;
                    double r = SkyFlyerTuning.HUNTER_CIRCLE_RADIUS;
                    hunter.steering.steer(new Vec3(tp.x + Math.cos(hunter.orbitAngle) * r,
                            tp.y + SkyFlyerTuning.HUNTER_CIRCLE_HEIGHT, tp.z + Math.sin(hunter.orbitAngle) * r),
                            SkyFlyerTuning.HUNTER_CIRCLE_SPEED);
                    hunter.diveHit = false;
                }
                case SHOOT -> hunter.steering.steer(hunter.position(), SkyFlyerTuning.HUNTER_CIRCLE_SPEED);
                case DIVE -> {
                    hunter.steering.steer(new Vec3(tp.x, target.getY(0.5D), tp.z), SkyFlyerTuning.HUNTER_DIVE_SPEED);
                    if (!hunter.diveHit && hunter.getBoundingBox().inflate(0.2D).intersects(target.getBoundingBox())
                            && hunter.level() instanceof ServerLevel level) {
                        hunter.doHurtTarget(level, target);
                        hunter.diveHit = true;
                    }
                    diveFinished = hunter.diveHit || hunter.verticalCollision;
                }
                case RISE -> {
                    double goalY = tp.y + SkyFlyerTuning.HUNTER_RISE_HEIGHT;
                    hunter.steering.steer(new Vec3(hunter.getX(), goalY, hunter.getZ()), SkyFlyerTuning.HUNTER_DIVE_SPEED);
                    risenEnough = hunter.getY() >= goalY - 1.0D;
                }
            }
            hunter.pattern.tick(diveFinished, risenEnough);
            if (hunter.pattern.shotDue()) WindShot.fire(hunter, target);
        }
    }
}
