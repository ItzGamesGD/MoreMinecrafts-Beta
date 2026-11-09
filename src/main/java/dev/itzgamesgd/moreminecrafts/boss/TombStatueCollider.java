package dev.itzgamesgd.moreminecrafts.boss;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Temporary, never-saved solid body of the risen statue. Its bounding box is the exact slice of the obelisk that
 * currently stands above the floor, synchronised to clients, so the server collision follows the rise and sink of
 * the displayed statue. It edits no blocks and is gone with the display; a chunk unload or restart cannot keep it.
 */
public final class TombStatueCollider extends Entity {
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(TombStatueCollider.class, EntityDataSerializers.FLOAT);

    private int lifetime = 1;
    private int age;

    public TombStatueCollider(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Nullable
    static TombStatueCollider spawn(ServerLevel level, Vec3 base, int lifetime) {
        TombStatueCollider collider = TombGuardianEntities.TOMB_STATUE_COLLIDER.create(level, EntitySpawnReason.TRIGGERED);
        if (collider == null) return null;
        collider.lifetime = Math.max(1, lifetime);
        collider.setPos(base.x, base.y, base.z);
        level.addFreshEntity(collider);
        return collider;
    }

    public int lifetime() { return lifetime; }

    /** Current collision height above the floor; zero while buried (no collision). */
    public float solidHeight() { return this.entityData.get(DATA_HEIGHT); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEIGHT, 0.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_HEIGHT.equals(accessor)) setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        return TombStatueShape.box(position, this.entityData.get(DATA_HEIGHT));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel level)) return;
        age++;
        if (age >= lifetime) {
            discard();
            return;
        }
        double height = TombStatueShape.height(age, lifetime);
        if (height > solidHeight() && !clearOccupants(level, TombStatueShape.box(position(), height))) {
            // A rising statue must never trap someone: with nowhere safe to shove them, drop the collision.
            discard();
            return;
        }
        this.entityData.set(DATA_HEIGHT, (float) height);
        setBoundingBox(makeBoundingBox());
    }

    /** Moves everything inside the growing box to a safe place first; false if anyone has none. */
    private boolean clearOccupants(ServerLevel level, AABB grown) {
        List<LivingEntity> occupants = level.getEntitiesOfClass(LivingEntity.class, grown, entity -> !entity.isSpectator());
        List<Vec3> shifts = new ArrayList<>(occupants.size());
        for (LivingEntity occupant : occupants) {
            Vec3 shift = TombStatuePlacement.findSafeShift(level, occupant, grown);
            if (shift == null) return false;
            shifts.add(shift);
        }
        for (int i = 0; i < occupants.size(); i++) {
            LivingEntity occupant = occupants.get(i);
            occupant.teleportTo(occupant.getX() + shifts.get(i).x, occupant.getY(), occupant.getZ() + shifts.get(i).z);
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) { return isAlive() && solidHeight() > 0.0F; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public PushReaction getPistonPushReaction() { return PushReaction.IMMOVEABLE; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean shouldBeSaved() { return false; }
}
