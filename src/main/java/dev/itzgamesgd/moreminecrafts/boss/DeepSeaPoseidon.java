package dev.itzgamesgd.moreminecrafts.boss;

import dev.itzgamesgd.moreminecrafts.mob.bone.BoneSkillMonster;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Deep Sea Poseidon (sea boss). Identity, stats and the three skills come from
 * {@code docs/planning/boss_sync_2026-11-15} (U-01); everything else is PROVISIONAL in {@link Tuning}. Free-swimming:
 * gravity is off while in water and it drifts around its target instead of walking. The summon trigger is deferred by
 * the document, so there is no natural spawn ({@code /summon} or {@code /mmmob}).
 */
public final class DeepSeaPoseidon extends BoneSkillMonster {
    public static final List<String> SKILLS = List.of("attack_thrust", "attack_swing", "skill_pull", "skill_charge", "skill_throw");
    private static final int THRUST = 1, SWING = 2, PULL = 3, CHARGE = 4, THROW = 5;

    public static final class Tuning {
        private Tuning() {}
        // --- AUTHORITY (boss sync 2026-11-15, U-01)
        public static final double MAX_HEALTH = 140.0;
        public static final float BASIC_DAMAGE = 8.0F;
        public static final float DAMAGE_TAKEN = 0.85F;       // 15% reduction
        public static final double KNOCKBACK_RESISTANCE = 0.75;
        public static final int CHARGE_TELEGRAPH_TICKS = 24;  // 1.2 s
        public static final double CHARGE_DISTANCE = 14.0;
        public static final float CHARGE_DAMAGE = 11.0F;
        public static final int CHARGE_SLOW_TICKS = 30;       // 1.5 s
        public static final float THROW_DAMAGE = 8.0F;
        public static final int THROW_RETURN_TICKS = 30;      // 1.5 s
        // --- PROVISIONAL (not defined by the document)
        public static final double SWIM_SPEED = 0.16;
        public static final double BASIC_REACH = 4.0;
        public static final double PULL_REACH = 10.0;
        public static final double PULL_ARC = 70.0;
        public static final double PULL_STRENGTH = 0.14;
        public static final int CHARGE_DASH_TICKS = 14;
        public static final int PULL_COOLDOWN = 200, CHARGE_COOLDOWN = 220, THROW_COOLDOWN = 160;
    }

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
            Component.translatable("entity.moreminecrafts.deep_sea_poseidon"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private int pullCooldown = 80, chargeCooldown = 120, throwCooldown = 60, basicIndex;
    private Vec3 aim = new Vec3(0, 0, 1);
    private final Set<UUID> chargeHits = new HashSet<>();
    private PoseidonTrident thrown;

    public DeepSeaPoseidon(EntityType<? extends DeepSeaPoseidon> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Tuning.MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Tuning.BASIC_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, Tuning.KNOCKBACK_RESISTANCE)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    public List<String> skillNames() {
        return SKILLS;
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected int skillLength(int id) {
        return switch (id) {
            case THRUST -> 20;
            case SWING -> 24;
            case PULL -> 60;
            case CHARGE -> 52;
            case THROW -> 56;
            default -> 1;
        };
    }

    @Override
    protected boolean tracksTarget(int id, int tick) {
        return switch (id) {
            case THRUST -> tick < 10;
            case SWING -> tick < 12;
            case CHARGE -> tick < Tuning.CHARGE_TELEGRAPH_TICKS - 4;
            case THROW -> tick < 14;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ movement

    /** Submerged check from the fluid at mid-body; independent of the fluid-push flag, which this boss disables. */
    public boolean submerged() {
        return level().getFluidState(net.minecraft.core.BlockPos.containing(getX(), getY() + getBbHeight() * 0.5, getZ()))
                .is(net.minecraft.tags.FluidTags.WATER);
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide()) setNoGravity(submerged() && !isDeadOrDying());
        super.aiStep();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false; // immune to current push
    }

    @Override
    protected void holdStill(ServerLevel level) {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().scale(0.6));
    }

    @Override
    protected void think(ServerLevel level) {
        if (pullCooldown > 0) pullCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;
        if (throwCooldown > 0) throwCooldown--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            holdStill(level);
            return;
        }
        faceToward(target.position());
        double dist = distanceTo(target);
        if (submerged()) {
            // Drift to a point level with the target's chest, keeping a short stand-off.
            Vec3 goal = target.position().add(0, target.getBbHeight() * 0.4, 0).subtract(facing.scale(Tuning.BASIC_REACH - 1.5));
            Vec3 d = goal.subtract(position());
            Vec3 want = d.length() > 0.6 ? d.normalize().scale(Tuning.SWIM_SPEED) : Vec3.ZERO;
            setDeltaMovement(getDeltaMovement().lerp(want, 0.18));
        } else if (dist > Tuning.BASIC_REACH - 1.0) {
            getNavigation().moveTo(target, 1.0);
        }
        boolean sees = hasLineOfSight(target);
        if (dist <= Tuning.BASIC_REACH) {
            if (pullCooldown <= 0 && random.nextInt(5) == 0) {
                begin(PULL, target.position());
            } else {
                begin((basicIndex++ & 1) == 0 ? THRUST : SWING, target.position());
            }
        } else if (sees && dist <= Tuning.CHARGE_DISTANCE && chargeCooldown <= 0 && random.nextInt(4) == 0) {
            begin(CHARGE, target.position());
        } else if (sees && dist <= Tuning.PULL_REACH && pullCooldown <= 0 && random.nextInt(4) == 0) {
            begin(PULL, target.position());
        } else if (sees && dist > 5.0 && dist <= 22.0 && throwCooldown <= 0 && random.nextInt(4) == 0) {
            begin(THROW, target.position());
        }
    }

    // ------------------------------------------------------------------ skills

    @Override
    protected void begin(int id, Vec3 toward) {
        super.begin(id, toward);
        chargeHits.clear();
        aim = toward.add(0, 1.0, 0).subtract(position().add(0, getBbHeight() * 0.5, 0));
        aim = aim.lengthSqr() < 1.0E-4 ? facing : aim.normalize();
    }

    @Override
    protected void skillTick(ServerLevel level, int id, int tick) {
        if (id != CHARGE || tick < Tuning.CHARGE_TELEGRAPH_TICKS) setDeltaMovement(getDeltaMovement().scale(0.7));
        Vec3 chest = position().add(0, getBbHeight() * 0.55, 0);
        switch (id) {
            case THRUST -> {
                if (tick == 11) strike(level, chest, Tuning.BASIC_REACH, 60.0);
            }
            case SWING -> {
                if (tick == 13) strike(level, chest, Tuning.BASIC_REACH, 150.0);
            }
            case PULL -> pullTick(level, chest, tick);
            case CHARGE -> chargeTick(level, chest, tick);
            case THROW -> throwTick(level, chest, tick);
            default -> { }
        }
    }

    private void strike(ServerLevel level, Vec3 chest, double reach, double arc) {
        playSound(SoundEvents.TRIDENT_THROW.value(), 1.4F, 0.8F);
        Vec3 tip = chest.add(facing.scale(2.6));
        level.sendParticles(ParticleTypes.BUBBLE, tip.x, tip.y, tip.z, 12, 0.5, 0.3, 0.5, 0.05);
        for (LivingEntity e : victims(level, chest, facing, reach, arc, 2.5)) {
            if (e.hurtServer(level, damageSources().mobAttack(this), Tuning.BASIC_DAMAGE)) {
                e.knockback(0.5, -facing.x, -facing.z, damageSources().mobAttack(this), Tuning.BASIC_DAMAGE);
            }
        }
    }

    private void pullTick(ServerLevel level, Vec3 chest, int tick) {
        if (tick == 12) playSound(SoundEvents.CONDUIT_ACTIVATE, 2.0F, 1.4F);
        if (tick < 12 || tick > 52) return;
        // Whirlpool cone in front of the spinning trident: rings shrink toward the tip and turn with it.
        Vec3 tip = tridentHandPosition().add(facing.scale(2.4));
        Vec3 side = new Vec3(-facing.z, 0, facing.x), up = new Vec3(0, 1, 0);
        for (int ring = 0; ring < 4; ring++) {
            double dist = 1.0 + ring * 2.2, radius = 0.4 + ring * 0.7;
            for (int k = 0; k < 3; k++) {
                double ang = tick * 0.5 - ring * 0.6 + k * 2.094;
                Vec3 p = tip.add(facing.scale(dist)).add(side.scale(Math.cos(ang) * radius)).add(up.scale(Math.sin(ang) * radius));
                level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                if (k == 0) level.sendParticles(ParticleTypes.DOLPHIN, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
            }
        }
        Vec3 into = chest.add(facing.scale(2.2));
        for (LivingEntity e : victims(level, chest, facing, Tuning.PULL_REACH, Tuning.PULL_ARC, 4.0)) {
            Vec3 d = into.subtract(e.position());
            if (d.length() < 1.2) continue;
            e.setDeltaMovement(e.getDeltaMovement().scale(0.6).add(d.normalize().scale(Tuning.PULL_STRENGTH * 2.2)));
            e.needsSync = true;
            if (tick % 3 == 0) {
                Vec3 p = e.position().add(0, 0.8, 0);
                level.sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    private void chargeTick(ServerLevel level, Vec3 chest, int tick) {
        int dashEnd = Tuning.CHARGE_TELEGRAPH_TICKS + Tuning.CHARGE_DASH_TICKS;
        if (tick < Tuning.CHARGE_TELEGRAPH_TICKS) {
            LivingEntity target = getTarget();
            if (target != null && tick < Tuning.CHARGE_TELEGRAPH_TICKS - 4) {
                Vec3 d = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(chest);
                if (d.lengthSqr() > 1.0E-3) aim = d.normalize();
            }
            if (tick % 2 == 0) { // water-current warning line
                for (double r = 1.0; r <= Tuning.CHARGE_DISTANCE; r += 1.0) {
                    Vec3 p = chest.add(aim.scale(r));
                    level.sendParticles(ParticleTypes.DOLPHIN, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                    if (((int) r + tick / 2) % 3 == 0) level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
                }
            }
            if (tick == Tuning.CHARGE_TELEGRAPH_TICKS - 1) playSound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.0F, 1.0F);
            return;
        }
        if (tick < dashEnd) {
            double speed = Tuning.CHARGE_DISTANCE / Tuning.CHARGE_DASH_TICKS;
            setDeltaMovement(aim.scale(speed));
            level.sendParticles(ParticleTypes.BUBBLE, chest.x, chest.y, chest.z, 6, 0.4, 0.4, 0.4, 0.05);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.8),
                    t -> t != this && t.isAlive() && t.getType() != getType())) {
                if (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
                if (!chargeHits.add(e.getUUID())) continue;
                if (e.hurtServer(level, damageSources().mobAttack(this), Tuning.CHARGE_DAMAGE)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Tuning.CHARGE_SLOW_TICKS, 1), this);
                }
            }
            if (horizontalCollision && tick > Tuning.CHARGE_TELEGRAPH_TICKS + 2) {
                skillTick = dashEnd; // stop on walls, go straight to recovery
            }
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.6));
        }
    }

    /** World position of the right hand that holds the trident (throw origin and catch point). */
    public Vec3 tridentHandPosition() {
        Vec3 right = new Vec3(-facing.z, 0, facing.x);
        return position().add(0, getBbHeight() * 0.6, 0).add(right.scale(0.45)).add(facing.scale(0.5));
    }

    void onTridentCaught() {
        thrown = null;
        playSound(SoundEvents.TRIDENT_RETURN, 2.0F, 1.2F);
        Vec3 hand = tridentHandPosition();
        if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.GLOW, hand.x, hand.y, hand.z, 10, 0.2, 0.3, 0.2, 0.02);
    }

    private void throwTick(ServerLevel level, Vec3 chest, int tick) {
        if (tick == 14) {
            Vec3 from = tridentHandPosition().add(0, 0.5, 0);
            LivingEntity target = getTarget();
            Vec3 to = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : from.add(aim.scale(12));
            thrown = PoseidonTrident.launch(level, this, from, to.subtract(from));
            playSound(SoundEvents.TRIDENT_THROW.value(), 2.0F, 0.9F);
        }
    }

    @Override
    protected void onSkillEnd(int id) {
        switch (id) {
            case PULL -> pullCooldown = Tuning.PULL_COOLDOWN;
            case CHARGE -> chargeCooldown = Tuning.CHARGE_COOLDOWN;
            case THROW -> throwCooldown = Tuning.THROW_COOLDOWN;
            default -> { }
        }
    }

    // ------------------------------------------------------------------ damage / boss bar

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof PoseidonTrident) return false;
        return super.hurtServer(level, source, amount * Tuning.DAMAGE_TAKEN);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossBar.setProgress(getHealth() / getMaxHealth());
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
        if (thrown != null && !thrown.isRemoved()) thrown.discard();
        super.remove(reason);
    }

    @Override
    protected int deathTicks() {
        return 50;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DROWNED_AMBIENT_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DROWNED_HURT_WATER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }
}
