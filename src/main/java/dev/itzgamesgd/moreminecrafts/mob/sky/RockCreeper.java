package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * 암석 크리퍼 / Rock Creeper (group B, §5 #3): vanilla Creeper AI and vanilla explosion behaviour.
 * The blast radius is scaled by {@link SkyVariantMobs#ROCK_CREEPER_EXPLOSION_MULTIPLIER} in
 * {@code CaveCreeperExplosionMixin}; health/speed come from {@code CustomMobStats}.
 */
public class RockCreeper extends Creeper {
    public RockCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }
}
