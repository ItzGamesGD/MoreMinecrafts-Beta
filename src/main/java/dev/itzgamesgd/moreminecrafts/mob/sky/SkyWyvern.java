package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.mob.bone.BoneSkillMonster;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sky Wyvern (천공의 비룡): an always-airborne elite. HP, the three attacks and their damage and the feather drop come
 * from SKY_ISLANDS_T2_CONTEXT_2026-10-27 §3; the owner (2026-11-26) asked for it to be built with freely designed
 * attack behaviour, so circling flight, ranges and cooldowns are PROVISIONAL in {@link Tuning}. No natural spawn yet.
 */
public final class SkyWyvern extends BoneSkillMonster {
    public static final List<String> SKILLS = List.of("dive", "breath", "wing_buffet");
    private static final int DIVE = 1, BREATH = 2, BUFFET = 3;

    public static final class Tuning {
        private Tuning() {}
        // --- AUTHORITY (SKY_ISLANDS_T2 §3)
        public static final double MAX_HEALTH = 60.0;
        public static final float BREATH_DAMAGE = 6.0F;     // pure damage, no knockback or lift
        public static final float DIVE_DAMAGE = 8.0F;
        public static final float BUFFET_DAMAGE = 3.0F;     // + knockback
        public static final float FEATHER_CHANCE = 0.10F;   // exactly 2 feathers, looting does not apply
        // --- PROVISIONAL
        public static final double CIRCLE_RADIUS = 9.0;
        public static final double CIRCLE_HEIGHT = 5.0;
        public static final double FLY_SPEED = 0.32;
        public static final double BREATH_REACH = 9.0;
        public static final double BREATH_ARC = 50.0;
        public static final double BUFFET_REACH = 4.5;
        public static final double BUFFET_KNOCKBACK = 1.6;
        public static final int DIVE_COOLDOWN = 90, BREATH_COOLDOWN = 140, BUFFET_COOLDOWN = 60;
    }

    private int diveCooldown = 60, breathCooldown = 100, buffetCooldown;
    private double orbit;
    private Vec3 home;
    private Vec3 diveAim = Vec3.ZERO;
    private final Set<UUID> hits = new HashSet<>();

    public SkyWyvern(EntityType<? extends SkyWyvern> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Tuning.MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Tuning.DIVE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.FLYING_SPEED, Tuning.FLY_SPEED);
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
            case DIVE -> 34;
            case BREATH -> 50;
            case BUFFET -> 20;
            default -> 1;
        };
    }

    @Override
    protected boolean tracksTarget(int id, int tick) {
        return (id == DIVE && tick < 14) || (id == BREATH && tick < 30) || (id == BUFFET && tick < 8);
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide()) setNoGravity(!isDeadOrDying());
        super.aiStep();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
    }

    @Override
    protected void holdStill(ServerLevel level) {
        setDeltaMovement(getDeltaMovement().scale(0.5));
    }

    private void steer(Vec3 goal, double speed, double blend) {
        Vec3 d = goal.subtract(position());
        Vec3 want = d.length() > 0.5 ? d.normalize().scale(speed) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().lerp(want, blend));
    }

    @Override
    protected void think(ServerLevel level) {
        if (diveCooldown > 0) diveCooldown--;
        if (breathCooldown > 0) breathCooldown--;
        if (buffetCooldown > 0) buffetCooldown--;
        if (home == null) home = position();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            orbit += 0.02;
            steer(home.add(Math.cos(orbit) * 6, 2 + Math.sin(orbit * 0.7), Math.sin(orbit) * 6), Tuning.FLY_SPEED * 0.5, 0.1);
            faceToward(position().add(getDeltaMovement()));
            return;
        }
        double dist = distanceTo(target);
        if (dist <= Tuning.BUFFET_REACH && buffetCooldown <= 0) {
            begin(BUFFET, target.position());
            return;
        }
        boolean sees = hasLineOfSight(target);
        if (sees && diveCooldown <= 0 && random.nextInt(30) == 0) {
            begin(DIVE, target.position());
            return;
        }
        if (sees && dist <= Tuning.BREATH_REACH + 3 && breathCooldown <= 0 && random.nextInt(25) == 0) {
            begin(BREATH, target.position());
            return;
        }
        // Circle the target above head height.
        orbit += 0.045;
        Vec3 goal = target.position().add(Math.cos(orbit) * Tuning.CIRCLE_RADIUS, Tuning.CIRCLE_HEIGHT, Math.sin(orbit) * Tuning.CIRCLE_RADIUS);
        steer(goal, Tuning.FLY_SPEED, 0.12);
        faceToward(position().add(getDeltaMovement()));
    }

    @Override
    protected void begin(int id, Vec3 toward) {
        super.begin(id, toward);
        hits.clear();
    }

    @Override
    protected void skillTick(ServerLevel level, int id, int tick) {
        Vec3 head = position().add(facing.scale(1.6)).add(0, 1.2, 0);
        LivingEntity target = getTarget();
        switch (id) {
            case DIVE -> {
                if (tick < 14) { // rear up and climb a little
                    setDeltaMovement(getDeltaMovement().scale(0.6).add(0, 0.06, 0));
                    if (target != null) diveAim = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    else diveAim = position().add(facing.scale(8)).add(0, -4, 0);
                } else if (tick < 26) { // plunge
                    Vec3 d = diveAim.subtract(position());
                    setDeltaMovement(d.length() > 0.5 ? d.normalize().scale(0.95) : getDeltaMovement().scale(0.5));
                    if (tick == 14) playSound(SoundEvents.PHANTOM_SWOOP, 2.0F, 0.7F);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6),
                            t -> t != this && t.isAlive() && t.getType() != getType())) {
                        if (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
                        if (hits.add(e.getUUID())) e.hurtServer(level, damageSources().mobAttack(this), Tuning.DIVE_DAMAGE);
                    }
                } else { // pull up
                    setDeltaMovement(getDeltaMovement().scale(0.7).add(0, 0.09, 0));
                }
            }
            case BREATH -> {
                setDeltaMovement(getDeltaMovement().scale(0.6));
                if (tick == 18) playSound(SoundEvents.BREEZE_INHALE, 2.0F, 0.6F);
                if (tick >= 24 && tick < 40) {
                    Vec3 dir = target != null
                            ? target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(head).normalize()
                            : facing.add(0, -0.4, 0).normalize();
                    if (tick == 24) playSound(SoundEvents.BREEZE_SHOOT, 2.0F, 0.5F);
                    for (double r = 1.0; r <= Tuning.BREATH_REACH; r += 0.8) {
                        Vec3 p = head.add(dir.scale(r));
                        level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, r * 0.06, r * 0.06, r * 0.06, 0.01);
                        if (r > 3 && tick % 2 == 0) level.sendParticles(ParticleTypes.GUST, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
                    }
                    for (LivingEntity e : victims(level, head, dir, Tuning.BREATH_REACH, Tuning.BREATH_ARC, Tuning.BREATH_REACH)) {
                        // Pure damage: bypasses armour, no knockback or lift; each target is hit once per breath.
                        if (hits.add(e.getUUID())) {
                            Vec3 v = e.getDeltaMovement();
                            e.hurtServer(level, damageSources().indirectMagic(this, this), Tuning.BREATH_DAMAGE);
                            e.setDeltaMovement(v);
                            e.needsSync = true;
                        }
                    }
                }
            }
            case BUFFET -> {
                setDeltaMovement(getDeltaMovement().scale(0.5));
                if (tick == 9) {
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 1.2F);
                    level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, head.x, head.y - 0.5, head.z, 1, 0, 0, 0, 0);
                    for (LivingEntity e : victims(level, position().add(0, 0.8, 0), facing, Tuning.BUFFET_REACH, 200.0, 3.0)) {
                        if (e.hurtServer(level, damageSources().mobAttack(this), Tuning.BUFFET_DAMAGE)) {
                            e.knockback(Tuning.BUFFET_KNOCKBACK, -facing.x, -facing.z, damageSources().mobAttack(this), Tuning.BUFFET_DAMAGE);
                            e.needsSync = true;
                        }
                    }
                }
            }
            default -> { }
        }
    }

    @Override
    protected void onSkillEnd(int id) {
        switch (id) {
            case DIVE -> diveCooldown = Tuning.DIVE_COOLDOWN;
            case BREATH -> breathCooldown = Tuning.BREATH_COOLDOWN;
            case BUFFET -> buffetCooldown = Tuning.BUFFET_COOLDOWN;
            default -> { }
        }
    }

    /** Feathers: 10% for exactly two, no looting (§3). The extra common loot (phantom membrane, bones) has no list yet. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (random.nextFloat() < Tuning.FEATHER_CHANCE) spawnAtLocation(level, new ItemStack(ModItems.SKY_FEATHER, 2));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected int deathTicks() {
        return 40;
    }
}
