package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.mob.bone.BoneSkillMonster;
import java.util.List;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Floating Jellyfish (부유 해파리): a translucent, slowly drifting neutral creature (SKY_ISLANDS_T2 §5). It never
 * starts a fight; when hurt it drifts after the attacker for a while and stings on contact. The owner (2026-11-26)
 * asked for a light, freely designed attack, so HP, sting damage and timings are PROVISIONAL. No feathers (§5), no
 * other drops or natural spawn defined yet.
 */
public final class FloatingJellyfish extends BoneSkillMonster {
    public static final List<String> SKILLS = List.of("sting");
    private static final int STING = 1;

    public static final class Tuning {
        private Tuning() {}
        // --- PROVISIONAL
        public static final double MAX_HEALTH = 10.0;
        public static final float STING_DAMAGE = 2.0F;
        public static final int ANGER_TICKS = 200;
        public static final double DRIFT_SPEED = 0.035;
        public static final double CHASE_SPEED = 0.07;
        public static final int STING_COOLDOWN = 30;
    }

    private LivingEntity attacker;
    private int anger;
    private int stingCooldown;
    private Vec3 home;
    private Vec3 wander;

    public FloatingJellyfish(EntityType<? extends FloatingJellyfish> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Tuning.MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Tuning.STING_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public List<String> skillNames() {
        return SKILLS;
    }

    @Override
    protected void registerGoals() {
        // Neutral: no target goals; it only reacts to whoever hurt it.
    }

    @Override
    protected int skillLength(int id) {
        return 20;
    }

    @Override
    protected boolean tracksTarget(int id, int tick) {
        return tick < 6;
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity living && living != this) {
            attacker = living;
            anger = Tuning.ANGER_TICKS;
            setTarget(living);
        }
        return hurt;
    }

    @Override
    protected void think(ServerLevel level) {
        if (home == null) home = position();
        if (stingCooldown > 0) stingCooldown--;
        // Pulse propulsion matching the 2 s "move" clip: a push at the start of each bell contraction, then drag.
        boolean pulse = tickCount % 40 == 0;
        if (anger > 0 && attacker != null && attacker.isAlive() && !(attacker instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()))) {
            anger--;
            Vec3 aim = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0);
            Vec3 d = aim.subtract(position());
            faceToward(attacker.position());
            if (pulse || tickCount % 20 == 0) setDeltaMovement(getDeltaMovement().add(d.normalize().scale(Tuning.CHASE_SPEED * 2.5)));
            if (stingCooldown <= 0 && getBoundingBox().inflate(0.4).intersects(attacker.getBoundingBox())) begin(STING, attacker.position());
        } else {
            anger = 0;
            attacker = null;
            setTarget(null);
            if (wander == null || tickCount % 160 == 0 || position().distanceTo(wander) < 1.0) {
                wander = home.add((random.nextDouble() - 0.5) * 14, (random.nextDouble() - 0.5) * 4, (random.nextDouble() - 0.5) * 14);
            }
            if (pulse) {
                Vec3 d = wander.subtract(position());
                if (d.lengthSqr() > 1.0E-3) setDeltaMovement(getDeltaMovement().add(d.normalize().scale(Tuning.DRIFT_SPEED * 3.0)));
            }
        }
        setDeltaMovement(getDeltaMovement().scale(0.94));
    }

    @Override
    protected void holdStill(ServerLevel level) {
        setDeltaMovement(getDeltaMovement().scale(0.8));
    }

    @Override
    protected void skillTick(ServerLevel level, int id, int tick) {
        setDeltaMovement(getDeltaMovement().scale(0.8));
        if (tick == 8) {
            playSound(SoundEvents.PUFFER_FISH_STING, 1.0F, 1.3F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.3, getZ(), 8, 0.5, 0.4, 0.5, 0.05);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.8),
                    t -> t != this && t.isAlive() && t.getType() != getType())) {
                if (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
                if (scripted || e == attacker) e.hurtServer(level, damageSources().mobAttack(this), Tuning.STING_DAMAGE);
            }
        }
    }

    @Override
    protected void onSkillEnd(int id) {
        stingCooldown = Tuning.STING_COOLDOWN;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    protected int deathTicks() {
        return 30;
    }
}
