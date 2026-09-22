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
}
