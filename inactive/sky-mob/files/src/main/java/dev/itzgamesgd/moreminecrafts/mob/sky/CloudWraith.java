package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** 구름 망령 (Q05-Q07, Q48): Zombie AI; 30 HP / 5 attack / 0.23 speed; never converts to a Drowned. */
public final class CloudWraith extends SkyZombieBase {
    public CloudWraith(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    /** Q48: no drowning conversion. */
    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
