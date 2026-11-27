package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;

/** Vanilla bee behavior with offspring that keep the Giant Bee variant. */
public final class GiantBeeEntity extends Bee {
    public GiantBeeEntity(EntityType<? extends Bee> type, Level level) {
        super(type, level);
    }

    @Override
    public GiantBeeEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        GiantBeeEntity child = (GiantBeeEntity) CustomMobCatalog.GIANT_BEE.create(level, EntitySpawnReason.BREEDING);
        if (child != null) CustomMobStats.apply(child);
        return child;
    }
}
