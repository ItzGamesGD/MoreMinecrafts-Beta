package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.level.Level;

/**
 * Magma-cube roster variant whose modest stat multipliers are reapplied after every vanilla
 * size-derived attribute rewrite, so split children retain the approved profile.
 */
public final class CatalogMagmaCube extends MagmaCube {
    private final double healthMultiplier;
    private final double speedMultiplier;
    private final double damageMultiplier;

    public CatalogMagmaCube(EntityType<? extends MagmaCube> type, Level level,
                            double healthMultiplier, double speedMultiplier, double damageMultiplier) {
        super(type, level);
        this.healthMultiplier = healthMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void setSize(int size, boolean updateHealth) {
        super.setSize(size, updateHealth);
        multiply(Attributes.MAX_HEALTH, healthMultiplier);
        multiply(Attributes.MOVEMENT_SPEED, speedMultiplier);
        multiply(Attributes.ATTACK_DAMAGE, damageMultiplier);
        if (updateHealth) setHealth(getMaxHealth());
    }

    private void multiply(net.minecraft.core.Holder<Attribute> attribute, double multiplier) {
        if (multiplier == 1.0) return;
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) instance.setBaseValue(instance.getBaseValue() * multiplier);
    }
}
