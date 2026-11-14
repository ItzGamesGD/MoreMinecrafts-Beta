package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;

/**
 * Buried Mummy and Gilded Mummy: a vanilla Husk that never converts to a Zombie in water. Owner decision
 * 2026-11-14: the water conversion is blocked; everything else (AI, attributes, drops, damage rules) stays vanilla.
 */
public final class PyramidHusk extends Husk {
    public PyramidHusk(EntityType<? extends Husk> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
