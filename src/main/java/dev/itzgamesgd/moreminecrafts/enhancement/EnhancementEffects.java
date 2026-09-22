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

        return level * definition.profile().valuePerLevel(target);
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
}
