package dev.itzgamesgd.moreminecrafts.st01;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The small gold guide golem (separate entity, not an IronGolem: no village/defence/attack behaviour, no boss code). It only walks to the
 * instance's stop point and waits for its leader. It is not saved (restart or chunk unload removes it); the entrance state machine handles
 * that through the fallback timer. Arrival is decided by the SERVER from this entity's position (see McPorts), never by the client.
 */
public class GoldGuideGolem extends PathfinderMob {
    private long tomb;
    @Nullable private BlockPos stop;
    @Nullable private UUID leader;

    public GoldGuideGolem(EntityType<? extends GoldGuideGolem> type, Level level) {
        super(type, level);
        refreshDimensions();   // the SCALE attribute only exists after the super constructor; re-derive hitbox/eye height from it
    }

    /** The guide cannot be hurt by ordinary damage (void and /kill still remove it; the fallback timer covers that). */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(level, source);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, GoldGuideGolemTuning.MOVEMENT_SPEED)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.SCALE, GoldGuideGolemTuning.SCALE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new GuideGoal());
        goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public void configureGuide(long tomb, BlockPos stop) { this.tomb = tomb; this.stop = stop.immutable(); }
    public long tomb() { return tomb; }
    @Nullable public BlockPos stop() { return stop; }
    public void setLeader(@Nullable UUID leader) { this.leader = leader; }
    @Nullable public UUID leader() { return leader; }

    @Override public boolean removeWhenFarAway(double distanceSquared) { return false; }
    @Override public boolean canBeLeashed() { return false; }

    private final class GuideGoal extends Goal {
        private int repath;

        GuideGoal() { setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override public boolean canUse() { return stop != null; }
        @Override public boolean canContinueToUse() { return stop != null; }

        @Override
        public void tick() {
            if (stop == null || !(level() instanceof ServerLevel level)) return;
            net.minecraft.world.entity.player.Player lead = leader == null ? null : level.getPlayerByUUID(leader);
            if (lead != null) getLookControl().setLookAt(lead, 30.0F, 30.0F);
            double distToLeader = lead == null ? Double.MAX_VALUE : distanceTo(lead);
            if (leader != null && distToLeader > GoldGuideGolemTuning.WAIT_DISTANCE) { getNavigation().stop(); return; }  // waits for its leader
            if (--repath > 0) return;
            repath = 10;
            getNavigation().moveTo(stop.getX() + 0.5D, stop.getY(), stop.getZ() + 0.5D, 1.0D);
        }
    }
}
