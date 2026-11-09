package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.Level;

/** Piglin variant that retains its identity outside the Nether. */
public final class WildPiglin extends Piglin {
    public WildPiglin(EntityType<? extends Piglin> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        // Piglin's vanilla overworld timer zombifies the entity after 300 ticks.
        // Keep all inherited AI and behavior while ensuring that timer never reaches it.
        if (!level().isClientSide()) setTimeInOverworld(0);
        super.tick();
    }
}
