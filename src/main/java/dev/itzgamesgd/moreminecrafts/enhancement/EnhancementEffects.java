package dev.itzgamesgd.moreminecrafts.enhancement;

import net.minecraft.world.item.ItemStack;

/** Resolves the additive value of one enhancement effect from the Stage 4A profile metadata. */
public final class EnhancementEffects {
    private EnhancementEffects() {
    }

    /**
     * Pure effect arithmetic, kept separate from the Minecraft-facing overload so it can be
     * verified without bootstrapping registries.
     */
    public static double value(EnhancementDefinition definition, int level, EnhancementEffect.Target target) {
        if (level <= 0) {
            return 0.0D;
        }

        int effectiveLevel = Math.min(level, definition.category().maximumLevel());
        return definition.profile().valueAt(effectiveLevel, target);
    }

    /** Resolves an effect for the item identity and enhancement component carried by a stack. */
    public static double value(ItemStack stack, EnhancementEffect.Target target) {
        return EnhancementCatalog.definition(stack.getItem())
                .map(definition -> value(definition, EnhancementOperation.level(stack), target))
                .orElse(0.0D);
    }

    /**
     * Resolves the flat launch damage for projectile-capable profiles. SPEAR deliberately keeps
     * using its Stage 4A ATTACK_DAMAGE metadata; this adapter prevents that value from making
     * unrelated melee profiles projectile-capable.
     */
    public static double projectileDamage(EnhancementDefinition definition, int level) {
        return switch (definition.profile()) {
            case RANGED -> value(definition, level, EnhancementEffect.Target.RANGED_DAMAGE);
            case SPEAR -> value(definition, level, EnhancementEffect.Target.ATTACK_DAMAGE);
            default -> 0.0D;
        };
    }

    /** Resolves projectile damage from the weapon identity and component on the supplied stack. */
    public static double projectileDamage(ItemStack weapon) {
        return EnhancementCatalog.definition(weapon.getItem())
                .map(definition -> projectileDamage(definition, EnhancementOperation.level(weapon)))
                .orElse(0.0D);
    }

    /** Resolves the fractional pre-armor damage reduction supplied by one armor stack. */
    public static double damageReduction(EnhancementDefinition definition, int level) {
        if (definition.profile() != EnhancementProfile.ARMOR) {
            return 0.0D;
        }

        return value(definition, level, EnhancementEffect.Target.DAMAGE_REDUCTION);
    }

    /** Resolves one equipped stack's fractional pre-armor damage reduction. */
    public static double damageReduction(ItemStack armor) {
        return EnhancementCatalog.definition(armor.getItem())
                .map(definition -> damageReduction(definition, EnhancementOperation.level(armor)))
                .orElse(0.0D);
    }

    /** Pure effective-block guard used by ItemStackMixin and regression tests. */
    public static float toolDestroySpeed(float vanillaSpeed, double enhancementBonus) {
        return vanillaSpeed > 1.0F && enhancementBonus > 0.0D
                ? vanillaSpeed + (float) enhancementBonus
                : vanillaSpeed;
    }
}
