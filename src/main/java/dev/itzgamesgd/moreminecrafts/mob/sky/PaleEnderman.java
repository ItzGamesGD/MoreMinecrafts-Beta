package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;

/**
 * 창백한 엔더맨 / Pale Enderman (group B, §5 #4): vanilla Enderman teleport, block carrying and hostility rules.
 * Only the look and the {@code CustomMobStats} multipliers differ.
 */
public class PaleEnderman extends Enderman {
    public PaleEnderman(EntityType<? extends Enderman> type, Level level) {
        super(type, level);
    }
}
