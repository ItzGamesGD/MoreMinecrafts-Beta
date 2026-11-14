package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;

/**
 * 푸른 슬라임 / Azure Slime (group B, §5 #5): vanilla Slime sizes, health, jumping AI and splitting (children keep
 * this type through {@code getType()}).
 *
 * <p>Jump height comes from the {@code JUMP_STRENGTH} default attribute (vanilla x
 * {@link SkyVariantMobs#AZURE_SLIME_JUMP_MULTIPLIER}), which vanilla size changes never rewrite; fall-damage immunity
 * comes from {@code #minecraft:fall_damage_immune}.
 */
public class AzureSlime extends Slime {
    public AzureSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }
}
