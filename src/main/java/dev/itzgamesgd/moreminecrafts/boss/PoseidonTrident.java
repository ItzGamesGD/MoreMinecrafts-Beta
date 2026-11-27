package dev.itzgamesgd.moreminecrafts.boss;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Poseidon's thrown trident (skill 3). One entity owns the whole flight: hit detection, damage and the return. It has
 * no physics of its own: the server moves it every tick and the client draws the trident model along its motion, so
 * what the player sees and what deals damage are the same thing.
 * <p>Timeline from release: flies out (stops at the first block or entity hit), turns back at {@link #TURN_TICK} at the
 * latest, and reaches the hand exactly at {@link #RETURN_TICK} = 1.5 s (document: returns after 1.5 s).
 */
public final class PoseidonTrident extends Entity {
    public static final int TURN_TICK = 16;
    public static final int RETURN_TICK = DeepSeaPoseidon.Tuning.THROW_RETURN_TICKS;
    private static final double SPEED = 1.3;
    private static final EntityDataAccessor<Boolean> RETURNING =
            SynchedEntityData.defineId(PoseidonTrident.class, EntityDataSerializers.BOOLEAN);

    private DeepSeaPoseidon owner;
    private Vec3 velocity = Vec3.ZERO;
    private boolean stopped;
    private int age;
    private final Set<UUID> hits = new HashSet<>();
    /** Client: last non-zero direction of travel, for drawing. */
    public Vec3 clientDirection = new Vec3(0, 0, 1);

    public PoseidonTrident(EntityType<? extends PoseidonTrident> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    static PoseidonTrident launch(ServerLevel level, DeepSeaPoseidon owner, Vec3 from, Vec3 dir) {
        PoseidonTrident t = new PoseidonTrident(dev.itzgamesgd.moreminecrafts.mob.bone.BoneMobs.POSEIDON_TRIDENT, level);
        t.owner = owner;
        t.velocity = dir.normalize().scale(SPEED);
        t.setPos(from.x, from.y, from.z);
        t.setDeltaMovement(t.velocity);
        level.addFreshEntity(t);
        return t;
    }

    public boolean returning() {
        return entityData.get(RETURNING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RETURNING, false);
    }

    @Override
    public void tick() {
        Vec3 before = position();
        super.tick();
        if (level().isClientSide()) {
            Vec3 d = position().subtract(xo, yo, zo);
            if (d.lengthSqr() > 1.0E-4) clientDirection = d.normalize();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        age++;
        if (!returning()) {
            if (!stopped) flyOut(level, before);
            if (stopped || age >= TURN_TICK) {
                entityData.set(RETURNING, true);
                playSound(SoundEvents.TRIDENT_RETURN, 1.5F, 1.0F);
            }
        } else {
            int left = RETURN_TICK - age;
            Vec3 hand = owner.tridentHandPosition();
            if (left <= 0) {
                owner.onTridentCaught();
                discard();
                return;
            }
            velocity = hand.subtract(position()).scale(1.0 / left);
            setPos(position().add(velocity));
        }
        setDeltaMovement(velocity);
        level.sendParticles(ParticleTypes.GLOW, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.0);
        level.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.02);
    }

    private void flyOut(ServerLevel level, Vec3 from) {
        Vec3 to = from.add(velocity);
        HitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) to = block.getLocation();
        // Sweep the segment against living entities; the trident stops in the first one it hits.
        LivingEntity first = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(0.6),
                t -> t != owner && t.isAlive() && !hits.contains(t.getUUID()))) {
            if (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
            var clip = e.getBoundingBox().inflate(0.3).clip(from, to);
            if (clip.isPresent() && clip.get().distanceToSqr(from) < best) {
                best = clip.get().distanceToSqr(from);
                first = e;
            }
        }
        if (first != null) {
            hits.add(first.getUUID());
            first.hurtServer(level, damageSources().trident(this, owner), DeepSeaPoseidon.Tuning.THROW_DAMAGE);
            playSound(SoundEvents.TRIDENT_HIT, 1.5F, 1.0F);
            to = from.add(to.subtract(from).normalize().scale(Math.sqrt(best)));
            stopped = true;
        } else if (block.getType() != HitResult.Type.MISS) {
            playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.5F, 1.0F);
            stopped = true;
        }
        setPos(to);
        if (stopped) velocity = to.subtract(from).normalize().scale(1.0E-3); // keep pointing the way it flew
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }
}
