package dev.itzgamesgd.moreminecrafts.boss;

import static dev.itzgamesgd.moreminecrafts.boss.TombGuardianTuning.*;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardianCombat.Attack;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.pyramid.PyramidMobs;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 * Royal Tomb Guardian: unique, sight-driven armored boss (not Warden vibration AI). Patterns, per the 2026-11-14 spec:
 * Ground Slam, Sweep, Tomb Spikes, Royal Guard Summon, and the Royal Judgment Earthwave ultimate. Every attack
 * is decided and resolved on the server; the client only plays tells and strike animations. At or below 50% health
 * it enters a one-way enrage (speed 0.24) and the ultimate becomes available.
 */
public class TombGuardian extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED =
            SynchedEntityData.defineId(TombGuardian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AWAKENING =
            SynchedEntityData.defineId(TombGuardian.class, EntityDataSerializers.INT);
    private static final Identifier ENRAGE_SPEED_ID = ModIds.id("tomb_guardian_enrage_speed");

    /** Entity events driving client animation of server-decided attacks (tells, strike frame, end). */
    public static final byte EVENT_SWEEP_WINDUP = 70;
    public static final byte EVENT_SLAM_WINDUP = 71;
    public static final byte EVENT_SPIKE_WINDUP = 72;
    public static final byte EVENT_STRIKE = 73;
    public static final byte EVENT_ATTACK_END = 74;
    public static final byte EVENT_SUMMON_WINDUP = 75;
    public static final byte EVENT_ULTIMATE_WINDUP = 76;
    /** Length of the reused vanilla Warden emerge animation (6.68 s); the client fast-forwards it to the awakening. */
    private static final int EMERGE_ANIMATION_TICKS = 134;

    public final AnimationState emergeAnimationState = new AnimationState();
    /** One client animation state per pattern; the renderer plays the matching Tomb Guardian keyframe animation. */
    public final AnimationState sweepAnimationState = new AnimationState();
    public final AnimationState slamAnimationState = new AnimationState();
    public final AnimationState spikeAnimationState = new AnimationState();
    public final AnimationState summonAnimationState = new AnimationState();
    public final AnimationState ultimateAnimationState = new AnimationState();

    /** One moving shockwave (slam, or one strike of the ultimate). Each victim is hit once per wave. */
    private static final class Wave {
        final Vec3 origin;
        final double radius;
        final float damage;
        final int startTick;
        final Set<UUID> hit = new HashSet<>();
        Wave(Vec3 origin, double radius, float damage, int startTick) {
            this.origin = origin;
            this.radius = radius;
            this.damage = damage;
            this.startTick = startTick;
        }
    }

    @Nullable private BlockPos tombAnchor;
    @Nullable private BlockPos emergePos;
    private Attack attack = Attack.NONE;
    private int attackTicks;
    private long sweepReadyTick;
    private long slamReadyTick;
    private long spikeReadyTick;
    private long summonReadyTick;
    private long ultimateReadyTick;
    @Nullable private Vec3 spikePoint;
    @Nullable private UUID attackTarget;
    private final List<Wave> waves = new ArrayList<>();
    private final Set<UUID> summons = new HashSet<>();
    private int ultimateStrikes;
    private long lastTrackedChunk = Long.MIN_VALUE;
    private boolean lootSuppressed;
    private int noSurvivorTicks;
    private boolean orphanDiscard;

    public TombGuardian(EntityType<? extends TombGuardian> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
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
        addTag(ENCOUNTER_BOUND_TAG);
        setAwakeningTicks(AWAKENING_TICKS);
    }

    @Nullable public BlockPos tombAnchor() { return tombAnchor; }

    public boolean isAwakening() { return entityData.get(DATA_AWAKENING) > 0; }

    public int awakeningTicks() { return entityData.get(DATA_AWAKENING); }

    /** Ends the awakening immediately (developer verification and tests). */
    public void skipAwakening() {
        if (!isAwakening()) return;
        setAwakeningTicks(0);
    }

    private void setAwakeningTicks(int ticks) { entityData.set(DATA_AWAKENING, Math.max(0, ticks)); }

    public boolean isEnraged() { return entityData.get(DATA_ENRAGED); }

    public Attack currentAttack() { return attack; }

    /** Fixed spike point of the current Tomb Spikes windup, or null when none is active. */
    @Nullable public Vec3 spikePoint() { return spikePoint; }

    /** Ticks until the given pattern may start again; zero when ready. */
    long cooldownRemaining(Attack pattern) { return Math.max(0L, readyTick(pattern) - tickCount); }

    private long readyTick(Attack pattern) {
        return switch (pattern) {
            case SWEEP -> sweepReadyTick;
            case SLAM -> slamReadyTick;
            case SPIKES -> spikeReadyTick;
            case SUMMON -> summonReadyTick;
            case ULTIMATE -> ultimateReadyTick;
            case NONE -> 0L;
        };
    }

    private void setReadyTick(Attack pattern, long tick) {
        switch (pattern) {
            case SWEEP -> sweepReadyTick = tick;
            case SLAM -> slamReadyTick = tick;
            case SPIKES -> spikeReadyTick = tick;
            case SUMMON -> summonReadyTick = tick;
            case ULTIMATE -> ultimateReadyTick = tick;
            case NONE -> {}
        }
    }

    /** Living summons currently attributed to this guardian. */
    int aliveSummons(ServerLevel level) {
        summons.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });
        return summons.size();
    }

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
            if (!isAwakening() && tickNoSurvivors(level)) {
                // P1-BUG-02: no living player near the guardian for 5 s; the encounter returns to IDLE and the guardian despawns.
                TombGuardianEncounters.endEncounter(level, tombAnchor);
                return;
            }
        }
        if (isAwakening()) tickAwakening(level);
        else if (isEnraged() && tickCount % 8 == 0) sandBurst(level, 6);
        tickWaves(level);
    }

    /** Counts ticks without a living player in range; true once the reset threshold is reached. */
    private boolean tickNoSurvivors(ServerLevel level) {
        double range = FOLLOW_RANGE * FOLLOW_RANGE;
        boolean anyone = false;
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= range) {
                anyone = true;
                break;
            }
        }
        noSurvivorTicks = anyone ? 0 : noSurvivorTicks + 1;
        return noSurvivorTicks >= NO_SURVIVOR_RESET_TICKS;
    }

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
        if (!isRemoved() && !dead && level() instanceof ServerLevel level) {
            // Only the bound guardian of a not-yet-defeated tomb completes it and may drop its reward (P1-BUG-03).
            // Debug or free-spawned guardians carry no EncounterBound tag and never drop.
            boolean credited = tombAnchor != null && entityTags().contains(ENCOUNTER_BOUND_TAG)
                    && TombGuardianEncounters.data(level).isBound(TombGuardianEncounters.tombKey(tombAnchor), getUUID());
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
        if (target == null || !target.isAlive() || !getSensing().hasLineOfSight(target)) return;
        Attack next = choose(level, target);
        if (next != Attack.NONE) start(level, next, target);
    }

    /** Priority among ready patterns: ultimate, then the close-range patterns, then ranged and summons. */
    private Attack choose(ServerLevel level, LivingEntity target) {
        if (isEnraged() && tickCount >= ultimateReadyTick) return Attack.ULTIMATE;
        if (tickCount >= sweepReadyTick && isWithinSweepReach(target)) return Attack.SWEEP;
        if (tickCount >= slamReadyTick && horizontalDistance(target.position()) <= WAVE_RADIUS) return Attack.SLAM;
        if (tickCount >= spikeReadyTick && horizontalDistance(target.position()) <= FOLLOW_RANGE) return Attack.SPIKES;
        if (tickCount >= summonReadyTick && aliveSummons(level) < SUMMON_MAX_ALIVE) return Attack.SUMMON;
        return Attack.NONE;
    }

    /** Starts one pattern immediately if idle (developer verification); still resolved by the normal rules. */
    public boolean forceAttack(ServerLevel level, Attack forced, LivingEntity target) {
        if (isAwakening() || attack != Attack.NONE || !target.isAlive() || forced == Attack.NONE) return false;
        setTarget(target);
        start(level, forced, target);
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
            sandBurst(level, 60);
        }
    }

    /** Sand dust and stone chips shaken loose by the cracked chest core (enrage presentation only). */
    private void sandBurst(ServerLevel level, int count) {
        BlockState floor = level.getBlockState(blockPosition().below());
        if (floor.isAir()) floor = net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), getX(), getY() + getBbHeight() * 0.62D, getZ(),
                count, getBbWidth() * 0.45D, getBbHeight() * 0.18D, getBbWidth() * 0.45D, 0.12D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + getBbHeight() * 0.62D, getZ(),
                count / 6, getBbWidth() * 0.4D, getBbHeight() * 0.15D, getBbWidth() * 0.4D, 0.02D);
    }

    private void applyEnrageSpeed() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(ENRAGE_SPEED_ID))
            speed.addPermanentModifier(new AttributeModifier(ENRAGE_SPEED_ID, ENRAGED_MOVEMENT_SPEED - MOVEMENT_SPEED,
                    AttributeModifier.Operation.ADD_VALUE));
    }

    boolean isWithinSweepReach(LivingEntity target) {
        return horizontalDistanceToBox(target.getBoundingBox()) <= SWEEP_REACH;
    }

    private double horizontalDistance(Vec3 point) {
        double dx = point.x - getX();
        double dz = point.z - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Horizontal distance from the guardian centre to the nearest point of a box. */
    private double horizontalDistanceToBox(AABB box) {
        double dx = Math.max(0.0D, Math.max(box.minX - getX(), getX() - box.maxX));
        double dz = Math.max(0.0D, Math.max(box.minZ - getZ(), getZ() - box.maxZ));
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void start(ServerLevel level, Attack next, LivingEntity target) {
        beginAttack(next);
        // Cooldowns are counted from the start of each pattern.
        switch (next) {
            case SWEEP -> {
                attackTarget = target.getUUID();
                setReadyTick(next, tickCount + SWEEP_COOLDOWN_TICKS);
                level.broadcastEntityEvent(this, EVENT_SWEEP_WINDUP);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.5F, 0.5F);
            }
            case SLAM -> {
                setReadyTick(next, tickCount + SLAM_COOLDOWN_TICKS);
                level.broadcastEntityEvent(this, EVENT_SLAM_WINDUP);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            case SPIKES -> {
                // The marked point is fixed when the warning begins and is never re-targeted afterwards.
                spikePoint = floorBelow(level, target.position());
                setReadyTick(next, tickCount + SPIKE_COOLDOWN_TICKS);
                TombStatueEffects.spawnCircle(level, spikePoint, SPIKE_WINDUP_TICKS);
                level.broadcastEntityEvent(this, EVENT_SPIKE_WINDUP);
                level.playSound(null, spikePoint.x, spikePoint.y, spikePoint.z, SoundEvents.EVOKER_PREPARE_SUMMON,
                        SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            case SUMMON -> {
                setReadyTick(next, tickCount + SUMMON_COOLDOWN_TICKS);
                level.broadcastEntityEvent(this, EVENT_SUMMON_WINDUP);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.8F);
            }
            case ULTIMATE -> {
                ultimateStrikes = 0;
                setReadyTick(next, tickCount + ULTIMATE_COOLDOWN_TICKS);
                level.broadcastEntityEvent(this, EVENT_ULTIMATE_WINDUP);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.5F);
            }
            case NONE -> {}
        }
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
            case SWEEP -> tickSweep(level);
            case SLAM -> tickSlam(level);
            case SPIKES -> tickSpikes(level);
            case SUMMON -> tickSummon(level);
            case ULTIMATE -> tickUltimate(level);
            case NONE -> {}
        }
        if (target != null && attack != Attack.SPIKES) getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    /** Sweep windup keeps tracking the target's direction (P1-BUG-01 "1-A"); the strike tests the fan at impact. */
    private void tickSweep(ServerLevel level) {
        Entity target = attackTarget == null ? null : level.getEntity(attackTarget);
        if (target instanceof LivingEntity living) {
            getLookControl().setLookAt(living, 30.0F, 30.0F);
            double dx = living.getX() - getX();
            double dz = living.getZ() - getZ();
            if (Math.abs(dx) + Math.abs(dz) > 1.0E-6D) setYRot((float) (Math.toDegrees(Math.atan2(-dx, dz))));
        }
        if (attackTicks < SWEEP_WINDUP_TICKS) {
            TombStatueEffects.sweepTell(level, position(), getYRot(), attackTicks / (double) SWEEP_WINDUP_TICKS);
            return;
        }
        level.broadcastEntityEvent(this, EVENT_STRIKE);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.7F);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, sweepArea(), this::isAreaVictim)) {
            if (TombGuardianCombat.sweepHits(getYRot(), victim.getX() - getX(), victim.getZ() - getZ(),
                    victim.getY() - getY()))
                victim.hurtServer(level, damageSources().mobAttack(this), SWEEP_DAMAGE);
        }
        finishAttack();
    }

    private AABB sweepArea() {
        return getBoundingBox().inflate(SWEEP_REACH, SWEEP_VERTICAL_TOLERANCE, SWEEP_REACH);
    }

    private void tickSlam(ServerLevel level) {
        if (attackTicks < SLAM_WINDUP_TICKS) {
            TombStatueEffects.directStrikeTell(level, position(), attackTicks / (double) SLAM_WINDUP_TICKS);
            return;
        }
        Vec3 origin = position();
        if (attackTicks == SLAM_WINDUP_TICKS) {
            level.broadcastEntityEvent(this, EVENT_STRIKE);
            level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.5F, 0.6F);
            waves.add(new Wave(origin, WAVE_RADIUS, SLAM_DAMAGE, tickCount));
        }
        finishAttack();
    }

    private void tickSpikes(ServerLevel level) {
        if (spikePoint == null) {
            finishAttack();
            return;
        }
        if (attackTicks < SPIKE_WINDUP_TICKS) {
            TombStatueEffects.circleParticles(level, spikePoint, attackTicks);
            return;
        }
        level.broadcastEntityEvent(this, EVENT_ATTACK_END);
        // A cancelled eruption (nowhere safe to shove an occupant) is a failed attack: no spikes, no damage.
        if (resolveSpikes(level)) TombStatueEffects.spawnStatue(level, spikePoint, SPIKE_VISIBLE_TICKS);
        finishAttack();
    }

    /**
     * Summons the royal guard (spec: 2 mummy zombies and 1 tomb skeleton) at the end of the windup. Only the number
     * that keeps the living total at or below {@link TombGuardianTuning#SUMMON_MAX_ALIVE} is spawned.
     */
    private void tickSummon(ServerLevel level) {
        if (attackTicks < SUMMON_WINDUP_TICKS) return;
        level.broadcastEntityEvent(this, EVENT_ATTACK_END);
        int room = SUMMON_MAX_ALIVE - aliveSummons(level);
        List<EntityType<? extends Mob>> roster = new ArrayList<>();
        for (int i = 0; i < SUMMON_MUMMY_COUNT; i++) roster.add(PyramidMobs.BURIED_MUMMY);
        for (int i = 0; i < SUMMON_SKELETON_COUNT; i++) roster.add(PyramidMobs.CRYPT_ARCHER);
        LivingEntity target = getTarget();
        for (int i = 0; i < roster.size() && i < room; i++) {
            Mob summon = roster.get(i).create(level, EntitySpawnReason.EVENT);
            if (summon == null) continue;
            double angle = Math.PI * 2.0D * i / roster.size();
            double x = getX() + Math.cos(angle) * SUMMON_RING_RADIUS;
            double z = getZ() + Math.sin(angle) * SUMMON_RING_RADIUS;
            summon.snapTo(x, getY(), z, getYRot(), 0.0F);
            if (target != null) summon.setTarget(target);
            if (level.addFreshEntity(summon)) summons.add(summon.getUUID());
        }
        finishAttack();
    }

    /** Three consecutive strikes: 8 m, 12 m, then the full-range strike plus one spike column under the nearest target. */
    private void tickUltimate(ServerLevel level) {
        int windupEnd = ULTIMATE_WINDUP_TICKS;
        if (attackTicks < windupEnd) {
            TombStatueEffects.directStrikeTell(level, position(), attackTicks / (double) windupEnd);
            return;
        }
        int sinceStart = attackTicks - windupEnd;
        if (ultimateStrikes < 3 && sinceStart == ultimateStrikes * ULTIMATE_HIT_INTERVAL_TICKS) {
            Vec3 origin = position();
            level.broadcastEntityEvent(this, EVENT_STRIKE);
            level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 2.0F, 0.5F);
            waves.add(new Wave(origin, TombGuardianCombat.ultimateRadius(ultimateStrikes),
                    TombGuardianCombat.ultimateDamage(ultimateStrikes), tickCount));
            if (ultimateStrikes == 2) {
                LivingEntity target = getTarget();
                if (target != null && target.isAlive()) resolveSpikes(level, floorBelow(level, target.position()));
            }
            ultimateStrikes++;
        }
        if (ultimateStrikes >= 3 && sinceStart >= 2 * ULTIMATE_HIT_INTERVAL_TICKS) finishAttack();
    }

    /** Advances every live wave by one tick and hits the victims its front crosses. */
    private void tickWaves(ServerLevel level) {
        for (var iterator = waves.iterator(); iterator.hasNext(); ) {
            Wave wave = iterator.next();
            int age = tickCount - wave.startTick;
            double previous = TombGuardianCombat.waveFront(age - 1, wave.radius);
            double front = TombGuardianCombat.waveFront(age, wave.radius);
            TombStatueEffects.waveRing(level, wave.origin, front);
            resolveWave(level, wave, previous, front);
            if (age >= TombGuardianCombat.waveTravelTicks(wave.radius)) iterator.remove();
        }
    }

    void resolveWave(ServerLevel level, Wave wave, double previousFront, double front) {
        double reach = wave.radius + WAVE_THICKNESS;
        AABB area = new AABB(wave.origin.x - reach, wave.origin.y - WAVE_FLOOR_TOLERANCE - 1.0D, wave.origin.z - reach,
                wave.origin.x + reach, wave.origin.y + 3.0D, wave.origin.z + reach);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::isAreaVictim)) {
            if (wave.hit.contains(victim.getUUID())) continue;
            double dx = victim.getX() - wave.origin.x;
            double dz = victim.getZ() - wave.origin.z;
            if (TombGuardianCombat.waveHits(Math.sqrt(dx * dx + dz * dz), previousFront, front,
                    victim.getY() - wave.origin.y, wave.radius)) {
                wave.hit.add(victim.getUUID());
                victim.hurtServer(level, damageSources().mobAttack(this), wave.damage);
            }
        }
    }

    /**
     * Spike eruption moment. Everything standing where the spikes will be solid is first shoved to a safe place; if
     * any occupant has none the whole eruption is cancelled before anything changes. Otherwise each entity inside the
     * spike column (measured before the shove) is hit once and lifted into the air.
     *
     * @return whether the eruption happened
     */
    boolean resolveSpikes(ServerLevel level) {
        return spikePoint != null && resolveSpikes(level, spikePoint);
    }

    boolean resolveSpikes(ServerLevel level, Vec3 point) {
        AABB body = TombStatueShape.fullBox(point);
        List<LivingEntity> occupants = level.getEntitiesOfClass(LivingEntity.class, body, entity -> !entity.isSpectator());
        List<Vec3> shifts = new ArrayList<>(occupants.size());
        for (LivingEntity occupant : occupants) {
            Vec3 shift = TombStatuePlacement.findSafeShift(level, occupant, body);
            if (shift == null) return false;
            shifts.add(shift);
        }
        AABB column = new AABB(point.x - SPIKE_RADIUS, point.y - 1.0D, point.z - SPIKE_RADIUS,
                point.x + SPIKE_RADIUS, point.y + SPIKE_HEIGHT, point.z + SPIKE_RADIUS);
        List<LivingEntity> victims = new ArrayList<>();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, column, this::isAreaVictim)) {
            if (TombGuardianCombat.spikeHits(victim.getX() - point.x, victim.getZ() - point.z, victim.getY() - point.y))
                victims.add(victim);
        }
        for (int i = 0; i < occupants.size(); i++) {
            LivingEntity occupant = occupants.get(i);
            occupant.teleportTo(occupant.getX() + shifts.get(i).x, occupant.getY(), occupant.getZ() + shifts.get(i).z);
        }
        for (LivingEntity victim : victims) {
            victim.hurtServer(level, damageSources().mobAttack(this), SPIKE_DAMAGE);
            Vec3 motion = victim.getDeltaMovement();
            victim.setDeltaMovement(motion.x, SPIKE_AIRBORNE_LIFT, motion.z);
        }
        level.playSound(null, point.x, point.y, point.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.5F);
        return true;
    }

    private boolean isAreaVictim(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof TombGuardian) && !(entity instanceof ArmorStand)
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity);
    }

    private void finishAttack() {
        attack = Attack.NONE;
        attackTicks = 0;
        attackTarget = null;
        spikePoint = null;
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
            case EVENT_SWEEP_WINDUP -> { stopTells(); sweepAnimationState.start(tickCount); }
            case EVENT_SLAM_WINDUP -> { stopTells(); slamAnimationState.start(tickCount); }
            case EVENT_SPIKE_WINDUP -> { stopTells(); spikeAnimationState.start(tickCount); }
            case EVENT_SUMMON_WINDUP -> { stopTells(); summonAnimationState.start(tickCount); }
            case EVENT_ULTIMATE_WINDUP -> { stopTells(); ultimateAnimationState.start(tickCount); }
            // The strike and end markers need no client reaction: each keyframe animation already contains its own
            // strike pose and recovery, timed to the server's windup length.
            case EVENT_STRIKE, EVENT_ATTACK_END -> {}
            default -> super.handleEntityEvent(id);
        }
    }

    private void stopTells() {
        sweepAnimationState.stop();
        slamAnimationState.stop();
        spikeAnimationState.stop();
        summonAnimationState.stop();
        ultimateAnimationState.stop();
    }

    // Persistence.

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("tomb_anchor", BlockPos.CODEC, tombAnchor);
        output.storeNullable("emerge_pos", BlockPos.CODEC, emergePos);
        output.putInt("awakening", awakeningTicks());
        output.putBoolean("enraged", isEnraged());
        output.putLong("sweep_ready", Math.max(0L, sweepReadyTick - tickCount));
        output.putLong("slam_ready", Math.max(0L, slamReadyTick - tickCount));
        output.putLong("spike_ready", Math.max(0L, spikeReadyTick - tickCount));
        output.putLong("summon_ready", Math.max(0L, summonReadyTick - tickCount));
        output.putLong("ultimate_ready", Math.max(0L, ultimateReadyTick - tickCount));
        int index = 0;
        for (UUID uuid : summons) output.putString("summon_" + index++, uuid.toString());
        output.putInt("summon_count", index);
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
        sweepReadyTick = tickCount + input.getLongOr("sweep_ready", 0L);
        slamReadyTick = tickCount + input.getLongOr("slam_ready", 0L);
        spikeReadyTick = tickCount + input.getLongOr("spike_ready", 0L);
        summonReadyTick = tickCount + input.getLongOr("summon_ready", 0L);
        ultimateReadyTick = tickCount + input.getLongOr("ultimate_ready", 0L);
        summons.clear();
        int count = input.getIntOr("summon_count", 0);
        for (int i = 0; i < count; i++) {
            try {
                summons.add(UUID.fromString(input.getStringOr("summon_" + i, "")));
            } catch (IllegalArgumentException ignored) {
                // A malformed entry is dropped rather than failing the whole guardian load.
            }
        }
    }

    /** Walks toward the visible target between attacks; never moves while a pattern is telegraphing. */
    private final class ApproachTargetGoal extends Goal {
        private int repathDelay;

        ApproachTargetGoal() { setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

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
            if (attack != Attack.NONE || isWithinSweepReach(target)) {
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
