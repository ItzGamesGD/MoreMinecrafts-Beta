package dev.itzgamesgd.moreminecrafts.boss;

import static dev.itzgamesgd.moreminecrafts.boss.TombGuardianTuning.*;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardianCombat.Attack;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Royal Tomb Guardian: unique, sight-driven heavy armored boss (not Warden vibration AI). Three attacks with
 * distinct tells and recovery windows, resolved on the server only: heavy melee fist, ground smash with an
 * outward-moving floor shockwave, and a fixed-position Stone Eruption announced by a magic circle. At or
 * below 40% health it enters a one-way enrage (faster movement and cadence, no new attacks, same wave).
 */
public class TombGuardian extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED =
            SynchedEntityData.defineId(TombGuardian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AWAKENING =
            SynchedEntityData.defineId(TombGuardian.class, EntityDataSerializers.INT);
    private static final Identifier ENRAGE_SPEED_ID = ModIds.id("tomb_guardian_enrage_speed");

    /** Entity events driving client animation of server-decided attacks (tells, strike frame, end). */
    public static final byte EVENT_MELEE_WINDUP = 70;
    public static final byte EVENT_SLAM_WINDUP = 71;
    public static final byte EVENT_STATUE_WINDUP = 72;
    public static final byte EVENT_STRIKE = 73;
    public static final byte EVENT_ATTACK_END = 74;
    /** Length of the reused vanilla Warden emerge animation (6.68 s); the client fast-forwards it to the awakening. */
    private static final int EMERGE_ANIMATION_TICKS = 134;

    public final AnimationState emergeAnimationState = new AnimationState();
    public final AnimationState meleeWindupAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState slamAnimationState = new AnimationState();
    public final AnimationState statueAnimationState = new AnimationState();

    @Nullable private BlockPos tombAnchor;
    @Nullable private BlockPos emergePos;
    private Attack attack = Attack.NONE;
    private int attackTicks;
    private long nextAttackTick;
    private long slamReadyTick;
    private long statueReadyTick;
    @Nullable private Vec3 slamOrigin;
    @Nullable private Vec3 statuePoint;
    @Nullable private UUID meleeTarget;
    private final Set<UUID> waveHits = new HashSet<>();
    private long lastTrackedChunk = Long.MIN_VALUE;
    private boolean lootSuppressed;

    public TombGuardian(EntityType<? extends TombGuardian> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, MELEE_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ENRAGED, false);
        builder.define(DATA_AWAKENING, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new ApproachTargetGoal());
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, (float) FOLLOW_RANGE));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Sight-driven: only players the guardian can actually see are acquired.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // Encounter binding.

    /** Prepares a structure-spawned guardian before it is added to the level. */
    void beginEncounter(BlockPos tombAnchor, BlockPos spawnPos, float yaw) {
        this.tombAnchor = tombAnchor.immutable();
        this.emergePos = spawnPos.immutable();
        snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, yaw, 0.0F);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
        setAwakeningTicks(AWAKENING_TICKS);
    }

    @Nullable public BlockPos tombAnchor() { return tombAnchor; }

    public boolean isAwakening() { return entityData.get(DATA_AWAKENING) > 0; }

    public int awakeningTicks() { return entityData.get(DATA_AWAKENING); }

    /** Ends the awakening immediately (developer verification and tests). */
    public void skipAwakening() {
        if (!isAwakening()) return;
        setAwakeningTicks(0);
        nextAttackTick = tickCount;
    }

    private void setAwakeningTicks(int ticks) { entityData.set(DATA_AWAKENING, Math.max(0, ticks)); }

    public boolean isEnraged() { return entityData.get(DATA_ENRAGED); }

    public Attack currentAttack() { return attack; }

    @Nullable public Vec3 statuePoint() { return statuePoint; }

    // Lifecycle.

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // Synced awakening drives the emerge animation, so late-joining clients see the same emergence.
            if (isAwakening()) emergeAnimationState.startIfStopped(tickCount - (EMERGE_ANIMATION_TICKS - awakeningTicks()));
            else emergeAnimationState.stop();
            return;
        }
        if (!(level() instanceof ServerLevel level)) return;
        if (tombAnchor != null && !isDeadOrDying()) {
            TombEncounterData data = TombGuardianEncounters.data(level);
            long tomb = TombGuardianEncounters.tombKey(tombAnchor);
            if (!data.isBound(tomb, getUUID())) {
                // An orphan (the tomb was defeated, or another guardian owns the encounter): never duplicate.
                lootSuppressed = true;
                discardOrphan();
                return;
            }
            long chunk = chunkPosition().pack();
            if (chunk != lastTrackedChunk) {
                lastTrackedChunk = chunk;
                data.trackChunk(tomb, getUUID(), chunk);
            }
        }
        if (isAwakening()) tickAwakening(level);
    }

    private boolean orphanDiscard;

    private void discardOrphan() {
        orphanDiscard = true;
        discard();
    }

    private void tickAwakening(ServerLevel level) {
        int remaining = awakeningTicks();
        getNavigation().stop();
        setDeltaMovement(0.0D, Math.min(0.0D, getDeltaMovement().y), 0.0D);
        BlockState floor = level.getBlockState(blockPosition().below());
        if (remaining % 4 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                    getX(), getY() + 0.1D, getZ(), 24, getBbWidth(), 0.2D, getBbWidth(), 0.1D);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2D, getZ(),
                    2, getBbWidth() / 2.0D, 0.1D, getBbWidth() / 2.0D, 0.01D);
        }
        if (remaining % 20 == 0)
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0F, 0.7F);
        setAwakeningTicks(remaining - 1);
        if (remaining - 1 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
            nextAttackTick = tickCount + MELEE_WINDUP_TICKS;
        }
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return isAwakening() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean isPushable() { return !isAwakening() && super.isPushable(); }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) { return false; }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean canUsePortal(boolean allowPassengers) { return false; }

    @Override
    public void die(DamageSource source) {
        if (!isRemoved() && !dead && level() instanceof ServerLevel level && tombAnchor != null) {
            // Only the bound guardian of a not-yet-defeated tomb completes it and may drop its reward.
            boolean credited = TombGuardianEncounters.data(level).isBound(TombGuardianEncounters.tombKey(tombAnchor), getUUID());
            if (!credited) lootSuppressed = true;
            LivingEntity killer = getKillCredit();
            super.die(source);
            if (credited) TombGuardianEncounters.guardianDied(level, this, tombAnchor, killer);
            return;
        }
        super.die(source);
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) { return !lootSuppressed && super.shouldDropLoot(level); }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!isRemoved() && level() instanceof ServerLevel level && tombAnchor != null && !orphanDiscard
                && reason != Entity.RemovalReason.KILLED && reason.shouldDestroy()) {
            // Discarded (peaceful despawn, command, encounter end) without dying: the tomb can be retried.
            // Chunk unload (UNLOADED_TO_CHUNK / UNLOADED_WITH_PLAYER) does not destroy and keeps the binding.
            TombGuardianEncounters.guardianLost(level, this, tombAnchor);
        }
        super.remove(reason);
    }

    // Combat.

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (isAwakening()) return;
        updateEnrage();
        LivingEntity target = getTarget();
        if (attack != Attack.NONE) {
            tickAttack(level, target);
            return;
        }
        if (target == null || !target.isAlive() || tickCount < nextAttackTick || !getSensing().hasLineOfSight(target)) return;
        double distance = horizontalDistance(target.position());
        if (tickCount >= slamReadyTick && distance <= WAVE_RADIUS) startSlam(level);
        else if (isWithinMeleeReach(target)) startMelee(level, target);
        else if (tickCount >= statueReadyTick && distance <= FOLLOW_RANGE) startStatue(level, target);
    }

    /** Starts one attack immediately if idle (developer verification); still resolved by the normal rules. */
    public boolean forceAttack(ServerLevel level, Attack forced, LivingEntity target) {
        if (isAwakening() || attack != Attack.NONE || !target.isAlive()) return false;
        setTarget(target);
        switch (forced) {
            case MELEE -> startMelee(level, target);
            case SLAM -> startSlam(level);
            case STATUE -> startStatue(level, target);
            case NONE -> { return false; }
        }
        return true;
    }

    void updateEnrage() {
        if (isEnraged() || !TombGuardianCombat.shouldEnrage(getHealth(), getMaxHealth())) return;
        entityData.set(DATA_ENRAGED, true);
        applyEnrageSpeed();
        if (level() instanceof ServerLevel level) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ANGRY, SoundSource.HOSTILE, 3.0F, 0.5F);
            level.sendParticles(new DustParticleOptions(0xFF2010, 2.0F), getX(), getEyeY(), getZ(),
                    30, 0.4D, 0.3D, 0.4D, 0.0D);
        }
    }

    private void applyEnrageSpeed() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(ENRAGE_SPEED_ID))
            speed.addPermanentModifier(new AttributeModifier(ENRAGE_SPEED_ID, ENRAGED_SPEED_MULTIPLIER - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    boolean isWithinMeleeReach(LivingEntity target) {
        AABB reach = getBoundingBox().inflate(MELEE_REACH, 0.0D, MELEE_REACH);
        return reach.intersects(target.getBoundingBox());
    }

    private double horizontalDistance(Vec3 point) {
        double dx = point.x - getX();
        double dz = point.z - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    void startMelee(ServerLevel level, LivingEntity target) {
        beginAttack(Attack.MELEE);
        meleeTarget = target.getUUID();
        level.broadcastEntityEvent(this, EVENT_MELEE_WINDUP);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.5F, 0.5F);
    }

    void startSlam(ServerLevel level) {
        beginAttack(Attack.SLAM);
        slamOrigin = position();
        waveHits.clear();
        level.broadcastEntityEvent(this, EVENT_SLAM_WINDUP);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    void startStatue(ServerLevel level, LivingEntity target) {
        beginAttack(Attack.STATUE);
        // The marked location is fixed when the warning begins and is never re-targeted afterwards.
        statuePoint = floorBelow(level, target.position());
        TombStatueEffects.spawnCircle(level, statuePoint, STATUE_WARNING_TICKS);
        level.broadcastEntityEvent(this, EVENT_STATUE_WINDUP);
        level.playSound(null, statuePoint.x, statuePoint.y, statuePoint.z, SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private void beginAttack(Attack next) {
        attack = next;
        attackTicks = 0;
        getNavigation().stop();
    }

    private void tickAttack(ServerLevel level, @Nullable LivingEntity target) {
        attackTicks++;
        getNavigation().stop();
        switch (attack) {
            case MELEE -> tickMelee(level);
            case SLAM -> tickSlam(level);
            case STATUE -> tickStatue(level);
            case NONE -> {}
        }
        if (target != null && attack != Attack.STATUE) getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void tickMelee(ServerLevel level) {
        Entity target = meleeTarget == null ? null : level.getEntity(meleeTarget);
        if (target instanceof LivingEntity living) getLookControl().setLookAt(living, 30.0F, 30.0F);
        if (attackTicks < MELEE_WINDUP_TICKS) return;
        // Strike frame: only connects if the target is still in reach and visible; vanilla armor/shield apply.
        level.broadcastEntityEvent(this, EVENT_STRIKE);
        if (target instanceof LivingEntity living && living.isAlive() && isWithinMeleeReach(living)
                && getSensing().hasLineOfSight(living)) {
            doHurtTarget(level, living);
        }
        finishAttack();
    }

    private void tickSlam(ServerLevel level) {
        int windup = TombGuardianCombat.slamWindup(isEnraged());
        if (slamOrigin == null) slamOrigin = position();
        if (attackTicks < windup) {
            if (attackTicks % 3 == 0)
                level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.8D, getZ(), 6,
                        getBbWidth() / 2.0D, 0.2D, getBbWidth() / 2.0D, 0.05D);
            return;
        }
        int sinceImpact = attackTicks - windup;
        if (sinceImpact == 0) level.broadcastEntityEvent(this, EVENT_STRIKE);
        if (sinceImpact == 0)
            level.playSound(null, slamOrigin.x, slamOrigin.y, slamOrigin.z, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.5F, 0.6F);
        double previous = TombGuardianCombat.waveFront(sinceImpact);
        double front = TombGuardianCombat.waveFront(sinceImpact + 1);
        TombStatueEffects.waveRing(level, slamOrigin, front);
        resolveWave(level, previous, front);
        if (sinceImpact + 1 >= TombGuardianCombat.waveTravelTicks()) {
            slamReadyTick = tickCount + TombGuardianCombat.slamCooldown(isEnraged());
            finishAttack();
        }
    }

    void resolveWave(ServerLevel level, double previousFront, double front) {
        Vec3 origin = slamOrigin == null ? position() : slamOrigin;
        double reach = WAVE_RADIUS + WAVE_THICKNESS;
        AABB area = new AABB(origin.x - reach, origin.y - WAVE_FLOOR_TOLERANCE - 1.0D, origin.z - reach,
                origin.x + reach, origin.y + 3.0D, origin.z + reach);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::isAreaVictim)) {
            if (waveHits.contains(victim.getUUID())) continue;
            double dx = victim.getX() - origin.x;
            double dz = victim.getZ() - origin.z;
            if (TombGuardianCombat.waveHits(Math.sqrt(dx * dx + dz * dz), previousFront, front, victim.getY() - origin.y)) {
                waveHits.add(victim.getUUID());
                victim.hurtServer(level, damageSources().mobAttack(this), SLAM_DAMAGE);
            }
        }
    }

    private void tickStatue(ServerLevel level) {
        if (statuePoint == null) {
            finishAttack();
            return;
        }
        if (attackTicks < STATUE_WARNING_TICKS) {
            TombStatueEffects.circleParticles(level, statuePoint, attackTicks);
            return;
        }
        level.broadcastEntityEvent(this, EVENT_ATTACK_END);
        resolveStatue(level);
        TombStatueEffects.spawnStatue(level, statuePoint, STATUE_VISIBLE_TICKS);
        statueReadyTick = tickCount + STATUE_COOLDOWN_TICKS;
        finishAttack();
    }

    /** Bounded single damage window at the eruption moment: each entity inside the column is hit once. */
    void resolveStatue(ServerLevel level) {
        Vec3 point = statuePoint;
        if (point == null) return;
        AABB column = new AABB(point.x - STATUE_RADIUS, point.y - 1.0D, point.z - STATUE_RADIUS,
                point.x + STATUE_RADIUS, point.y + STATUE_HEIGHT, point.z + STATUE_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, column, this::isAreaVictim)) {
            if (TombGuardianCombat.statueHits(victim.getX() - point.x, victim.getZ() - point.z, victim.getY() - point.y))
                victim.hurtServer(level, damageSources().mobAttack(this), STATUE_DAMAGE);
        }
        level.playSound(null, point.x, point.y, point.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.5F);
    }

    private boolean isAreaVictim(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof TombGuardian) && !(entity instanceof ArmorStand)
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity);
    }

    private void finishAttack() {
        attack = Attack.NONE;
        attackTicks = 0;
        meleeTarget = null;
        statuePoint = null;
        slamOrigin = null;
        nextAttackTick = tickCount + TombGuardianCombat.recoveryAfterAttack(isEnraged());
    }

    private static Vec3 floorBelow(ServerLevel level, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        for (int i = 0; i < 6; i++) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) break;
            pos = below;
        }
        return new Vec3(position.x, pos.getY(), position.z);
    }

    // Client animation.

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_MELEE_WINDUP -> { stopTells(); meleeWindupAnimationState.start(tickCount); }
            case EVENT_SLAM_WINDUP -> { stopTells(); slamAnimationState.start(tickCount); }
            case EVENT_STATUE_WINDUP -> { stopTells(); statueAnimationState.start(tickCount); }
            case EVENT_STRIKE -> { stopTells(); attackAnimationState.start(tickCount); }
            case EVENT_ATTACK_END -> stopTells();
            default -> super.handleEntityEvent(id);
        }
    }

    private void stopTells() {
        meleeWindupAnimationState.stop();
        slamAnimationState.stop();
        statueAnimationState.stop();
    }

    // Persistence.

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("tomb_anchor", BlockPos.CODEC, tombAnchor);
        output.storeNullable("emerge_pos", BlockPos.CODEC, emergePos);
        output.putInt("awakening", awakeningTicks());
        output.putBoolean("enraged", isEnraged());
        output.putLong("slam_ready", Math.max(0L, slamReadyTick - tickCount));
        output.putLong("statue_ready", Math.max(0L, statueReadyTick - tickCount));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        tombAnchor = input.read("tomb_anchor", BlockPos.CODEC).orElse(null);
        emergePos = input.read("emerge_pos", BlockPos.CODEC).orElse(null);
        setAwakeningTicks(input.getIntOr("awakening", 0));
        if (input.getBooleanOr("enraged", false)) {
            entityData.set(DATA_ENRAGED, true);
            applyEnrageSpeed();
        }
        slamReadyTick = tickCount + input.getLongOr("slam_ready", 0L);
        statueReadyTick = tickCount + input.getLongOr("statue_ready", 0L);
    }

    /** Walks toward the visible target between attacks; never moves while an attack is telegraphing. */
    private final class ApproachTargetGoal extends Goal {
        private int repathDelay;

        ApproachTargetGoal() { setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return !isAwakening() && target != null && target.isAlive();
        }

        @Override
        public void start() { repathDelay = 0; }

        @Override
        public void stop() { getNavigation().stop(); }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attack != Attack.NONE || isWithinMeleeReach(target)) {
                getNavigation().stop();
                return;
            }
            if (--repathDelay <= 0) {
                repathDelay = 4 + getRandom().nextInt(7);
                getNavigation().moveTo(target, 1.0D);
            }
        }
    }
}
