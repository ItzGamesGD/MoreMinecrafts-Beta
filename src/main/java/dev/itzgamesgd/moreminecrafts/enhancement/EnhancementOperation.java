package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * The UI-independent Stage 3 enhancement rule and state transition.
 *
 * <p>This intentionally models only the single Iron Sword probe. A later menu can invoke this
 * operation without depending on the temporary vanilla-anvil adapter.</p>
 */
public final class EnhancementOperation {
    public static final int RESULT_LEVEL = 1;
    public static final int MATERIAL_COST = 1;
    public static final int EXPERIENCE_LEVEL_COST = 1;
    public static final double ATTACK_DAMAGE_BONUS = 1.0D;

    private EnhancementOperation() {
    }

    public static boolean matches(ItemStack equipment, ItemStack material) {
        return equipment.is(Items.IRON_SWORD)
                && material.is(ModItems.ENHANCEMENT_STONE)
                && material.getCount() >= MATERIAL_COST
                && level(equipment) == 0;
    }

    public static Optional<ItemStack> createResult(ItemStack equipment, ItemStack material) {
        if (!matches(equipment, material)) {
            return Optional.empty();
        }

        ItemStack result = equipment.copyWithCount(1);
        result.set(ModDataComponents.ENHANCEMENT_LEVEL, RESULT_LEVEL);
        return Optional.of(result);
    }

    public static int level(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL, 0);
    }

    public static double attackDamageBonus(ItemStack stack) {
        return stack.is(Items.IRON_SWORD) ? level(stack) * ATTACK_DAMAGE_BONUS : 0.0D;
    }
}
