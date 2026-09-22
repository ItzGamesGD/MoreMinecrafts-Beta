package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

/**
 * The authoritative, UI-independent eligibility and enhancement state transition.
 */
public final class EnhancementOperation {
    private EnhancementOperation() {
    }

    public static boolean matches(ItemStack equipment, ItemStack material) {
        return preview(equipment, material).isPresent();
    }

    public static Optional<Result> preview(ItemStack equipment, ItemStack material) {
        if (!isEnhancementMaterial(material) || material.getCount() < EnhancementRules.MATERIAL_COST) {
            return Optional.empty();
        }

        Optional<EnhancementDefinition> definition = EnhancementCatalog.definition(equipment.getItem());
        if (definition.isEmpty()) return Optional.empty();

        int targetLevel = level(equipment) + 1;
        if (!definition.get().category().allowsTransitionTo(targetLevel)) return Optional.empty();

        ItemStack result = equipment.copyWithCount(1);
        result.set(ModDataComponents.ENHANCEMENT_LEVEL, targetLevel);
        return Optional.of(new Result(result, targetLevel,
                EnhancementRules.experienceLevelCost(targetLevel), EnhancementRules.MATERIAL_COST));
    }

    public static Optional<ItemStack> createResult(ItemStack equipment, ItemStack material) {
        return preview(equipment, material).map(Result::stack);
    }

    public static boolean isEnhancementMaterial(ItemStack material) {
        return material.is(ModItems.ENHANCEMENT_STONE);
    }

    public static int level(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL, 0);
    }

    public static double attackDamageBonus(ItemStack stack) {
        return EnhancementCatalog.definition(stack.getItem())
                .filter(definition -> definition.profile() == EnhancementProfile.SWORD)
                .map(definition -> level(stack) * definition.profile()
                        .valuePerLevel(EnhancementEffect.Target.ATTACK_DAMAGE))
                .orElse(0.0D);
    }

    public record Result(ItemStack stack, int targetLevel, int experienceLevelCost, int materialCost) {
    }
}
