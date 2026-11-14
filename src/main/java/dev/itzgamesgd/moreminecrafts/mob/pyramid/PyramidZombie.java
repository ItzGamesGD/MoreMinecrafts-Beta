package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * Funeral Servant: a vanilla Zombie that never converts to a Drowned in water. Owner decision 2026-11-14: the water
 * conversion is blocked; everything else (AI, attributes, drops, damage rules) stays vanilla.
 */
public final class PyramidZombie extends Zombie {
    public PyramidZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
