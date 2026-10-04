package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * The authoritative, UI-independent eligibility and enhancement state transition.
 *
 * <p>Minecraft/registry-facing resolution stays at the edge in {@link #preview(ItemStack, ItemStack)}.
 * The transition policy itself is exposed as a package-private pure planner so ordinary JUnit tests
 * do not try to emulate Fabric's registry lifecycle.</p>
 */
public final class EnhancementOperation {
    private EnhancementOperation() {
    }

    public static boolean matches(ItemStack equipment, ItemStack material) {
        return preview(equipment, material).isPresent();
    }

    public static Optional<Result> preview(ItemStack equipment, ItemStack material) {
        if (!isEnhancementMaterial(material)) {
            return Optional.empty();
        }

        Optional<EnhancementDefinition> definition = EnhancementCatalog.definition(equipment.getItem());
        if (definition.isEmpty()) {
            return Optional.empty();
        }

        return plan(definition.get(), level(equipment), material.getCount()).map(plan -> result(equipment, plan));
    }

    /** Requirement shown by the UI even when the inserted stone stack is too small. */
    public static Optional<Result> requirement(ItemStack equipment) {
        Optional<EnhancementDefinition> definition = EnhancementCatalog.definition(equipment.getItem());
        if (equipment.isEmpty() || definition.isEmpty()) {
            return Optional.empty();
        }
        return plan(definition.get(), level(equipment), Integer.MAX_VALUE).map(plan -> result(equipment, plan));
    }

    /** Server-authoritative one-step transaction; inputs stay in their workstation slots. */
    public static boolean apply(Player player, ItemStack equipment, ItemStack material) {
        if (player.level().isClientSide()) {
            return false;
        }
        Optional<Result> preview = preview(equipment, material);
        if (preview.isEmpty() || !RawXp.pay(player, preview.get().experienceCost())) {
            return false;
        }
        Result result = preview.get();
        equipment.set(ModDataComponents.ENHANCEMENT_LEVEL, result.targetLevel());
        material.shrink(result.materialCost());
        return true;
    }

    private static Result result(ItemStack equipment, Plan plan) {
        ItemStack result = equipment.copyWithCount(1);
        result.set(ModDataComponents.ENHANCEMENT_LEVEL, plan.targetLevel());
        return new Result(result, plan.targetLevel(), plan.experienceCost(), plan.materialCost(),
                plan.milestone(), plan.milestoneCount());
    }

    /**
     * Pure enhancement transition policy. This intentionally contains no ItemStack, registry,
     * Data Component, Fabric, or menu interaction so it is safe to exercise with plain JUnit.
     */
    static Optional<Plan> plan(EnhancementDefinition definition, int currentLevel, int materialCount) {
        if (currentLevel < 0) {
            return Optional.empty();
        }

        // Check the current level before incrementing so malformed/max-int component values cannot overflow.
        if (!definition.category().enhanceable() || currentLevel >= definition.category().maximumLevel()) {
            return Optional.empty();
        }

        int targetLevel = currentLevel + 1;
        if (!definition.category().allowsTransitionTo(targetLevel)) {
            return Optional.empty();
        }

        EnhancementRules.Cost cost = EnhancementRules.costFor(targetLevel);
        if (materialCount < cost.materialCount()) {
            return Optional.empty();
        }
        return Optional.of(new Plan(
                targetLevel,
                cost.experienceCost(),
                cost.materialCount(),
                EnhancementRules.isMilestone(targetLevel),
                EnhancementRules.milestoneCount(targetLevel)
        ));
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
        return EnhancementEffects.value(stack, EnhancementEffect.Target.ATTACK_DAMAGE);
    }

    record Plan(int targetLevel, int experienceCost, int materialCost,
                boolean milestone, int milestoneCount) {
    }

    public record Result(ItemStack stack, int targetLevel, int experienceCost, int materialCost,
                         boolean milestone, int milestoneCount) {
    }
}
