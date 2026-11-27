package dev.itzgamesgd.moreminecrafts.mob.bone;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared skill/animation plumbing for mobs whose clips come from a converted .bbmodel (see BoneAnimations on the client).
 * The server runs one skill at a time and syncs its pose id; the client restarts the matching {@link AnimationState}.
 * Pose id {@code i + 1} plays {@code skillNames().get(i)}.
 */
public abstract class BoneSkillMonster extends Monster {
    private static final EntityDataAccessor<Integer> POSE =
            SynchedEntityData.defineId(BoneSkillMonster.class, EntityDataSerializers.INT);

    /** Client side: index = pose id (0 unused). */
    public final AnimationState[] poseStates;

    protected int skill;
    protected int skillTick;
    protected int gap;
    protected boolean scripted;
    protected Vec3 facing = new Vec3(0, 0, 1);
    private Vec3 forcedVelocity = Vec3.ZERO;
    private int forcedTicks;

    protected BoneSkillMonster(EntityType<? extends BoneSkillMonster> type, Level level) {
        super(type, level);
        poseStates = new AnimationState[skillNames().size() + 1];
        for (int i = 0; i < poseStates.length; i++) poseStates[i] = new AnimationState();
    }

    /** Clip names, in pose-id order starting at 1. */
    public abstract List<String> skillNames();

    /** Clip length in ticks. */
    protected abstract int skillLength(int id);

    /** One server tick of skill {@code id}; {@code tick} counts from 0. */
    protected abstract void skillTick(ServerLevel level, int id, int tick);

    /** Movement and skill choice while no skill runs (skipped when scripted). */
    protected abstract void think(ServerLevel level);

    /** While scripted and idle: hold position. */
    protected void holdStill(ServerLevel level) {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.5, isNoGravity() ? 0.5 : 1.0, 0.5));
    }

    /** Whether the skill still turns toward its target on this tick. */
    protected boolean tracksTarget(int id, int tick) {
        return false;
    }

    protected void onSkillEnd(int id) {}

    /** Ticks before the corpse is removed (the death clip plays meanwhile). */
    protected int deathTicks() {
        return 40;
    }

    /** Client playback speed of skill clips (e.g. a faster phase). */
    public float animationSpeed() {
        return 1.0F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POSE, 0);
    }

    public int pose() {
        return entityData.get(POSE);
    }

    public String currentSkillName() {
        return skill == 0 ? "" : skillNames().get(skill - 1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor.equals(POSE) && level().isClientSide()) {
            for (AnimationState s : poseStates) s.stop();
            int id = entityData.get(POSE);
            if (id > 0 && id < poseStates.length) poseStates[id].start(tickCount);
        }
    }

    public void setScripted(boolean value) {
        scripted = value;
        if (value) getNavigation().stop();
    }

    public boolean scripted() {
        return scripted;
    }

    /** Debug/capture entry: move at {@code velocity} (blocks/tick) for {@code ticks}, facing the motion, even when scripted. */
    public void forceMove(Vec3 velocity, int ticks) {
        forcedVelocity = velocity;
        forcedTicks = ticks;
    }

    /** Debug/capture entry: start the named skill facing {@code toward}. */
    public boolean forceSkill(String name, Vec3 toward) {
        int id = skillNames().indexOf(name) + 1;
        if (id <= 0 || !isAlive() || !(level() instanceof ServerLevel)) return false;
        begin(id, toward);
        return true;
    }

    protected void begin(int id, Vec3 toward) {
        skill = id;
        skillTick = 0;
        faceToward(toward);
        getNavigation().stop();
        entityData.set(POSE, id);
    }

    protected void faceToward(Vec3 toward) {
        Vec3 d = toward.subtract(position());
        Vec3 flat = new Vec3(d.x, 0, d.z);
        if (flat.lengthSqr() > 1.0E-4) facing = flat.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    protected void finish() {
        int done = skill;
        skill = 0;
        skillTick = 0;
        gap = 6;
        entityData.set(POSE, 0);
        onSkillEnd(done);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (isDeadOrDying()) return;
        if (skill != 0) {
            LivingEntity target = getTarget();
            if (target != null && tracksTarget(skill, skillTick)) faceToward(target.position());
            else faceToward(position().add(facing));
            skillTick(level, skill, skillTick);
            if (skill != 0 && ++skillTick >= skillLength(skill)) finish();
            return;
        }
        if (forcedTicks > 0) {
            forcedTicks--;
            setDeltaMovement(forcedVelocity);
            faceToward(position().add(forcedVelocity));
            return;
        }
        if (gap > 0) {
            gap--;
            return;
        }
        if (scripted) holdStill(level);
        else think(level);
    }

    /** Living entities (not this, not creative/spectator players) within {@code reach} in a forward arc. */
    protected List<LivingEntity> victims(ServerLevel level, Vec3 from, Vec3 dir, double reach, double arcDegrees, double ySpan) {
        double cosHalf = Math.cos(Math.toRadians(arcDegrees / 2.0));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                net.minecraft.world.phys.AABB.ofSize(from, reach * 2 + 2, ySpan * 2 + 2, reach * 2 + 2),
                t -> t != this && t.isAlive() && t.getType() != getType())) {
            if (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 d = e.getBoundingBox().getCenter().subtract(from);
            double len = d.length();
            if (len > reach + e.getBbWidth() * 0.5) continue;
            if (len > 1.0 && d.scale(1.0 / len).dot(dir) < cosHalf) continue;
            out.add(e);
        }
        return out;
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= deathTicks() && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }
}
