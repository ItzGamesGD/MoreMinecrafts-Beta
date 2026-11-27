package dev.itzgamesgd.moreminecrafts.boss;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Lava Forgemaster (piglin fire boss). Stats, skill identities and damage numbers come from
 * {@code docs/planning/boss_sync_2026-11-15} (U-01). Everything the document leaves open (reach, cadence, fan angles, pillar
 * count, flame-pulse damage) is isolated in {@link Tuning} as PROVISIONAL values for live tuning. The summon trigger and
 * arena are deferred by the document, so this entity has no natural spawn: use {@code /summon} or {@code /mmforge}.
 */
public final class PiglinForgemaster extends Monster {
    /** Synced pose id, 0 = none. Ids match {@link Skill#id}. */
    private static final EntityDataAccessor<Integer> POSE =
            SynchedEntityData.defineId(PiglinForgemaster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PHASE_TWO =
            SynchedEntityData.defineId(PiglinForgemaster.class, EntityDataSerializers.BOOLEAN);

    /** Skills and their server timing. {@code animation} is the clip name in the converted bone model. */
    public enum Skill {
        NONE(0, "", 0, 0),
        ATTACK_OVERHEAD(1, "attack_overhead", 26, 15),
        ATTACK_CLEAVE(2, "attack_cleave", 22, 12),
        SLASH(3, "skill_slash", 50, 28),
        QUAKE(4, "skill_quake", 66, 52),
        FURNACE(5, "skill_furnace", 80, 20),
        OVERHEAT(6, "overheat", 60, 60),
        STAGGER(7, "stagger", 50, 50),
        LEAP(8, "leap_slam", 48, 32);

        public final int id;
        public final String animation;
        /** Length of the animation clip in ticks (20/s). */
        public final int ticks;
        /** Tick of the clip at which the skill takes effect (impact / emission start). */
        public final int hit;

        Skill(int id, String animation, int ticks, int hit) {
            this.id = id;
            this.animation = animation;
            this.ticks = ticks;
            this.hit = hit;
        }

        public static Skill byId(int id) {
            for (Skill s : values()) {
                if (s.id == id) return s;
            }
            return NONE;
        }
    }

    /** Values written in the authority document are marked AUTHORITY; the rest are PROVISIONAL tuning. */
    public static final class Tuning {
        private Tuning() {}
        // --- AUTHORITY (boss sync 2026-11-15, U-01)
        public static final double MAX_HEALTH = 240.0;
        public static final double ARMOR = 12.0;
        public static final double KNOCKBACK_RESISTANCE = 0.85;
        public static final float BASIC_DAMAGE = 12.0F;
        public static final float SLASH_DAMAGE = 10.0F;
        public static final float QUAKE_DAMAGE = 11.0F;
        public static final double FURNACE_RADIUS = 5.0;
        public static final int FURNACE_TICKS = 40;                 // 2 s emission
        public static final double PHASE_TWO_HEALTH_FRACTION = 0.45;
        public static final double PHASE_TWO_ATTACK_SPEED = 1.20;
        public static final double PHASE_TWO_MOVE_SPEED = 1.15;
        public static final int STAGGER_TICKS = 50;                 // 2.5 s core exposure
        public static final float STAGGER_DAMAGE_TAKEN = 1.5F;
        // --- PROVISIONAL (not defined by the document)
        public static final double MOVE_SPEED = 0.26;
        public static final double BASIC_REACH = 5.0;
        public static final double SLASH_REACH = 7.0;
        public static final double SLASH_ARC_DEGREES = 140.0;
        public static final double QUAKE_REACH = 9.0;
        public static final double QUAKE_ARC_DEGREES = 90.0;
        public static final int QUAKE_PILLARS = 9;
        public static final double QUAKE_PILLAR_RADIUS = 1.6;
        public static final int SLASH_FIRE_SECONDS = 5;
        public static final int FURNACE_FIRE_SECONDS = 3;
        public static final int SLASH_COOLDOWN = 160;
        public static final int QUAKE_COOLDOWN = 240;
        public static final int FURNACE_COOLDOWN = 300;
        public static final int GAP_TICKS = 6;
    }

    private final ServerBossEvent bossBar = new ServerBossEvent(
            java.util.UUID.randomUUID(), Component.translatable("entity.moreminecrafts.piglin_forgemaster"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    /** Client side: one animation state per skill id (index = {@link Skill#id}). */
    public final AnimationState[] poseStates = new AnimationState[Skill.values().length];

    private Skill skill = Skill.NONE;
    private int skillTick;
    private int gap;
    private int slashCooldown = 60;
    private int quakeCooldown = 100;
    private int furnaceCooldown = 120;
    private int basicIndex;
    private boolean scripted;
    private Vec3 facing = Vec3.ZERO;
    private final List<Vec3> pillars = new ArrayList<>();

    public PiglinForgemaster(EntityType<? extends PiglinForgemaster> type, Level level) {
        super(type, level);
        for (int i = 0; i < poseStates.length; i++) poseStates[i] = new AnimationState();
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Tuning.MAX_HEALTH)
                .add(Attributes.ARMOR, Tuning.ARMOR)
                .add(Attributes.ATTACK_DAMAGE, Tuning.BASIC_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, Tuning.KNOCKBACK_RESISTANCE)
                .add(Attributes.MOVEMENT_SPEED, Tuning.MOVE_SPEED)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POSE, 0);
        builder.define(PHASE_TWO, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public Skill currentSkill() {
        return Skill.byId(entityData.get(POSE));
    }

    public boolean phaseTwo() {
        return entityData.get(PHASE_TWO);
    }

    public boolean isStaggered() {
        return skill == Skill.STAGGER;
    }

    // ------------------------------------------------------------------ client animation sync

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor.equals(POSE) && level().isClientSide()) {
            for (AnimationState state : poseStates) state.stop();
            int id = entityData.get(POSE);
            if (id > 0 && id < poseStates.length) poseStates[id].start(tickCount);
        }
    }

    // ------------------------------------------------------------------ developer controls

    /** Stops autonomous skill selection and chasing so a single animation can be filmed; skills still run when forced. */
    public void setScripted(boolean value) {
        scripted = value;
        if (value) getNavigation().stop();
    }

    public boolean scripted() {
        return scripted;
    }

    /** Starts {@code requested} now, facing {@code toward}. Used by the debug command. */
    public boolean force(Skill requested, Vec3 toward) {
        if (!(level() instanceof ServerLevel) || requested == Skill.NONE || !isAlive()) return false;
        begin(requested, toward);
        return true;
    }

    public void enterPhaseTwo() {
        if (phaseTwo() || !(level() instanceof ServerLevel)) return;
        entityData.set(PHASE_TWO, true);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath("moreminecrafts", "forgemaster_overheat"));
        speed.addPermanentModifier(new AttributeModifier(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("moreminecrafts", "forgemaster_overheat"),
                Tuning.PHASE_TWO_MOVE_SPEED - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        begin(Skill.OVERHEAT, position().add(getLookAngle()));
    }

    // ------------------------------------------------------------------ server tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (isDeadOrDying()) return;
        if (!phaseTwo() && getHealth() <= getMaxHealth() * Tuning.PHASE_TWO_HEALTH_FRACTION && skill != Skill.OVERHEAT) {
            enterPhaseTwo();
        }
        if (skill != Skill.NONE) {
            runSkill(level);
            return;
        }
        if (slashCooldown > 0) slashCooldown--;
        if (quakeCooldown > 0) quakeCooldown--;
        if (furnaceCooldown > 0) furnaceCooldown--;
        if (gap > 0) {
            gap--;
            return;
        }
        if (scripted) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double dist = distanceTo(target);
        getLookControl().setLookAt(target, 40.0F, 40.0F);
        if (dist > Tuning.BASIC_REACH - 1.0) {
            getNavigation().moveTo(target, 1.0);
        } else {
            getNavigation().stop();
        }
        Vec3 toward = target.position();
        if (dist <= Tuning.BASIC_REACH - 0.5 && furnaceCooldown <= 0 && random.nextInt(4) == 0) {
            begin(Skill.FURNACE, toward);
        } else if (dist <= Tuning.QUAKE_REACH && quakeCooldown <= 0 && random.nextInt(3) == 0) {
            begin(Skill.QUAKE, toward);
        } else if (dist <= Tuning.SLASH_REACH && slashCooldown <= 0 && random.nextInt(3) == 0) {
            begin(Skill.SLASH, toward);
        } else if (dist <= Tuning.BASIC_REACH - 0.5) {
            begin((basicIndex++ & 1) == 0 ? Skill.ATTACK_OVERHEAD : Skill.ATTACK_CLEAVE, toward);
        }
    }

    private double speedFactor() {
        return phaseTwo() ? Tuning.PHASE_TWO_ATTACK_SPEED : 1.0;
    }

    private void begin(Skill next, Vec3 toward) {
        skill = next;
        skillTick = 0;
        pillars.clear();
        Vec3 dir = toward.subtract(position());
        facing = new Vec3(dir.x, 0, dir.z).lengthSqr() < 1.0E-4 ? getLookAngle() : new Vec3(dir.x, 0, dir.z).normalize();
        float yaw = (float) (Math.atan2(-facing.x, facing.z) * 180.0 / Math.PI);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        entityData.set(POSE, next.id);
        if (next == Skill.OVERHEAT) {
            playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.6F);
        } else if (next == Skill.LEAP) {
            setDeltaMovement(facing.x * 0.9, 0.95, facing.z * 0.9);
        }
    }

    private int scaled(int ticks) {
        return (int) Math.round(ticks / speedFactor());
    }

    private void runSkill(ServerLevel level) {
        // Locked on the committed direction; the boss only tracks its target before the strike lands.
        LivingEntity target = getTarget();
        if (skillTick < scaled(skill.hit) && target != null && skill != Skill.QUAKE && skill != Skill.FURNACE && skill != Skill.OVERHEAT) {
            Vec3 d = target.position().subtract(position());
            Vec3 flat = new Vec3(d.x, 0, d.z);
            if (flat.lengthSqr() > 1.0E-4) facing = flat.normalize();
        }
        float yaw = (float) (Math.atan2(-facing.x, facing.z) * 180.0 / Math.PI);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        if (skill != Skill.LEAP) setDeltaMovement(0, getDeltaMovement().y, 0);
        getNavigation().stop();

        int hit = scaled(skill.hit);
        switch (skill) {
            case ATTACK_OVERHEAD -> {
                if (skillTick == hit) basicStrike(level, Tuning.BASIC_REACH, 100.0, 1.5, 0.45);
            }
            case ATTACK_CLEAVE -> {
                if (skillTick == hit) basicStrike(level, Tuning.BASIC_REACH, 150.0, 1.2, 0.35);
            }
            case SLASH -> {
                if (skillTick == hit) slash(level);
            }
            case QUAKE -> quakeTick(level, hit);
            case FURNACE -> furnaceTick(level, hit);
            case OVERHEAT -> overheatTick(level);
            case LEAP -> {
                if (skillTick == hit) {
                    level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, 1.4, 0.1, 1.4, 0.0);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
                }
            }
            default -> { }
        }
        if (++skillTick >= scaled(skill.ticks)) finishSkill();
    }

    private void finishSkill() {
        Skill done = skill;
        skill = Skill.NONE;
        skillTick = 0;
        gap = Tuning.GAP_TICKS;
        entityData.set(POSE, 0);
        switch (done) {
            case SLASH -> slashCooldown = Tuning.SLASH_COOLDOWN;
            case QUAKE -> quakeCooldown = Tuning.QUAKE_COOLDOWN;
            case FURNACE -> furnaceCooldown = Tuning.FURNACE_COOLDOWN;
            default -> { }
        }
        // Phase 2: skills 1 and 2 leave the core exposed for 2.5 s (the boss takes 1.5x damage).
        if (phaseTwo() && (done == Skill.SLASH || done == Skill.QUAKE) && !scripted) {
            begin(Skill.STAGGER, position().add(facing));
        }
    }

    // ------------------------------------------------------------------ skills

    private List<LivingEntity> victims(ServerLevel level, double reach, double arcDegrees) {
        double cosHalf = Math.cos(Math.toRadians(arcDegrees / 2.0));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach, 2.0, reach),
                t -> t != this && t.isAlive() && !(t instanceof PiglinForgemaster))) {
            if (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 d = e.position().subtract(position());
            Vec3 flat = new Vec3(d.x, 0, d.z);
            double len = flat.length();
            if (len > reach + e.getBbWidth() * 0.5) continue;
            if (len > 0.8 && flat.scale(1.0 / len).dot(facing) < cosHalf) continue;
            out.add(e);
        }
        return out;
    }

    private void basicStrike(ServerLevel level, double reach, double arc, double knock, double lift) {
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 1.1F);
        Vec3 front = position().add(facing.scale(2.6));
        level.sendParticles(ParticleTypes.LAVA, front.x, getY() + 0.3, front.z, 10, 0.8, 0.1, 0.8, 0.0);
        level.sendParticles(ParticleTypes.EXPLOSION, front.x, getY() + 0.3, front.z, 2, 0.6, 0.1, 0.6, 0.0);
        for (LivingEntity e : victims(level, reach, arc)) {
            if (e.hurtServer(level, damageSources().mobAttack(this), Tuning.BASIC_DAMAGE)) {
                e.knockback(knock, -facing.x, -facing.z, damageSources().mobAttack(this), Tuning.BASIC_DAMAGE);
                e.push(0, lift, 0);
            }
        }
    }

    private void slash(ServerLevel level) {
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
        for (int i = -4; i <= 4; i++) {
            double a = Math.toRadians(i * (Tuning.SLASH_ARC_DEGREES / 8.0));
            Vec3 dir = new Vec3(facing.x * Math.cos(a) - facing.z * Math.sin(a), 0, facing.x * Math.sin(a) + facing.z * Math.cos(a));
            for (double r = 2.0; r <= Tuning.SLASH_REACH; r += 1.0) {
                level.sendParticles(ParticleTypes.FLAME, getX() + dir.x * r, getY() + 1.0 + (r * 0.05), getZ() + dir.z * r, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        for (LivingEntity e : victims(level, Tuning.SLASH_REACH, Tuning.SLASH_ARC_DEGREES)) {
            if (e.hurtServer(level, damageSources().mobAttack(this), Tuning.SLASH_DAMAGE)) {
                e.igniteForSeconds(Tuning.SLASH_FIRE_SECONDS);
                e.knockback(0.8, -facing.x, -facing.z, damageSources().mobAttack(this), Tuning.SLASH_DAMAGE);
            }
        }
    }

    private void quakeTick(ServerLevel level, int hit) {
        if (skillTick == hit) {
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.5F);
            // Choose the pillar positions up front so the fissure telegraph can mark them.
            for (int i = 0; i < Tuning.QUAKE_PILLARS; i++) {
                double a = Math.toRadians((random.nextDouble() - 0.5) * Tuning.QUAKE_ARC_DEGREES);
                double r = 2.5 + random.nextDouble() * (Tuning.QUAKE_REACH - 2.5);
                Vec3 dir = new Vec3(facing.x * Math.cos(a) - facing.z * Math.sin(a), 0, facing.x * Math.sin(a) + facing.z * Math.cos(a));
                pillars.add(position().add(dir.scale(r)));
            }
        }
        int end = scaled(52);
        if (skillTick > hit && skillTick < end && skillTick % 3 == 0) {
            // Fissure telegraph: cracks spreading across the fan, brighter near the pillar spots.
            for (int i = -3; i <= 3; i++) {
                double a = Math.toRadians(i * (Tuning.QUAKE_ARC_DEGREES / 6.0));
                Vec3 dir = new Vec3(facing.x * Math.cos(a) - facing.z * Math.sin(a), 0, facing.x * Math.sin(a) + facing.z * Math.cos(a));
                double r = 1.5 + (skillTick - hit) * (Tuning.QUAKE_REACH / Math.max(1.0, end - hit));
                level.sendParticles(ParticleTypes.LAVA, getX() + dir.x * r, getY() + 0.1, getZ() + dir.z * r, 1, 0.1, 0.0, 0.1, 0.0);
            }
            for (Vec3 p : pillars) level.sendParticles(ParticleTypes.SMOKE, p.x, getY() + 0.1, p.z, 1, 0.3, 0.0, 0.3, 0.0);
        }
        if (skillTick == end) {
            playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
            for (Vec3 p : pillars) {
                for (int h = 0; h < 12; h++) {
                    level.sendParticles(ParticleTypes.FLAME, p.x, getY() + h * 0.35, p.z, 3, 0.35, 0.05, 0.35, 0.02);
                }
                level.sendParticles(ParticleTypes.LAVA, p.x, getY() + 0.5, p.z, 4, 0.4, 0.2, 0.4, 0.0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        net.minecraft.world.phys.AABB.ofSize(new Vec3(p.x, getY() + 1.0, p.z), Tuning.QUAKE_PILLAR_RADIUS * 2, 4.0, Tuning.QUAKE_PILLAR_RADIUS * 2),
                        t -> t != this && t.isAlive() && !(t instanceof PiglinForgemaster))) {
                    if (e instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) continue;
                    if (e.hurtServer(level, damageSources().mobAttack(this), Tuning.QUAKE_DAMAGE)) {
                        e.push(0, 0.9, 0);
                    }
                }
            }
        }
    }

    private void furnaceTick(ServerLevel level, int hit) {
        int end = hit + scaled(Tuning.FURNACE_TICKS);
        if (skillTick == hit) playSound(SoundEvents.BLAZE_SHOOT, 2.5F, 0.4F);
        if (skillTick >= hit && skillTick < end) {
            // Furnace core on the chest: a pulse every 4 ticks out to the 5 block radius in front.
            double chestY = getY() + 2.1;
            for (int i = -2; i <= 2; i++) {
                double a = Math.toRadians(i * 30.0);
                Vec3 dir = new Vec3(facing.x * Math.cos(a) - facing.z * Math.sin(a), 0, facing.x * Math.sin(a) + facing.z * Math.cos(a));
                double r = 1.0 + ((skillTick - hit) % 8) * (Tuning.FURNACE_RADIUS - 1.0) / 8.0;
                level.sendParticles(ParticleTypes.FLAME, getX() + dir.x * r, chestY - r * 0.12, getZ() + dir.z * r, 3, 0.15, 0.15, 0.15, 0.02);
                level.sendParticles(ParticleTypes.SMOKE, getX() + dir.x * r, chestY - r * 0.12, getZ() + dir.z * r, 1, 0.1, 0.1, 0.1, 0.0);
            }
            if ((skillTick - hit) % 4 == 0) {
                // No damage figure is given for this skill in the authority document: it only ignites (placeholder 0 damage).
                for (LivingEntity e : victims(level, Tuning.FURNACE_RADIUS, 150.0)) e.igniteForSeconds(Tuning.FURNACE_FIRE_SECONDS);
            }
        }
    }

    private void overheatTick(ServerLevel level) {
        if (skillTick % 2 == 0) {
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.8, getZ(), 2, 0.8, 1.2, 0.8, 0.0);
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.8, getZ(), 3, 0.9, 1.4, 0.9, 0.03);
        }
    }

    // ------------------------------------------------------------------ damage / death / boss bar

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (skill == Skill.STAGGER) amount *= Tuning.STAGGER_DAMAGE_TAKEN;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    /** The death clip is 2.2 s, longer than the vanilla 1 s corpse timer. */
    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= 50 && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_BRUTE_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("PhaseTwo", phaseTwo());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        if (in.getBooleanOr("PhaseTwo", false)) {
            entityData.set(PHASE_TWO, true);
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("moreminecrafts", "forgemaster_overheat");
            if (speed.getModifier(id) == null) {
                speed.addPermanentModifier(new AttributeModifier(id, Tuning.PHASE_TWO_MOVE_SPEED - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }
}
