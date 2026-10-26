package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Server-authoritative accessory-only enhancement transaction. */
public final class AccessoryEnhancementOperation {
    private AccessoryEnhancementOperation() {}

    public static boolean isAccessory(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AccessoryItem accessory
                && !AccessoryEnhancementCatalog.table(accessory.definition()).isEmpty();
    }

    public static Optional<AccessoryEnhancementRules.Plan> requirement(
            ItemStack accessory, int stoneCount, int catalystCount, int availableRawExperience) {
        if (!isAccessory(accessory)) return Optional.empty();
        return AccessoryEnhancementRules.plan(level(accessory), stoneCount, catalystCount, availableRawExperience);
    }

    public static Optional<AccessoryEnhancementRules.Plan> nextRequirement(ItemStack accessory) {
        return requirement(accessory, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** Applies exactly one level; a rejected/failed attempt leaves XP and all three stacks unchanged. */
    public static boolean apply(Player player, ItemStack accessory, ItemStack stones, ItemStack catalysts) {
        return apply(player, accessory, stones, catalysts, () -> {});
    }

    /**
     * Test seam: {@code afterDebits} runs once all costs are debited and before the level is written, so a
     * test can throw there and prove the rollback. Production callers use the four-argument overload.
     */
    public static boolean apply(Player player, ItemStack accessory, ItemStack stones, ItemStack catalysts,
                                Runnable afterDebits) {
        if (player.level().isClientSide() || !isAccessory(accessory)
                || !stones.is(ModItems.ENHANCEMENT_STONE)
                || !catalysts.isEmpty() && !catalysts.is(ModItems.RELIC_CATALYST)) {
            return false;
        }
        Optional<AccessoryEnhancementRules.Plan> planned = AccessoryEnhancementRules.plan(
                level(accessory), stones.getCount(), catalysts.isEmpty() ? 0 : catalysts.getCount(), RawXp.available(player));
        if (planned.isEmpty()) return false;

        AccessoryEnhancementRules.Plan plan = planned.get();
        RawXp.Snapshot xpBefore = RawXp.Snapshot.of(player);
        int oldStoneCount = stones.getCount();
        int oldCatalystCount = catalysts.getCount();
        Integer oldLevel = accessory.get(ModDataComponents.ENHANCEMENT_LEVEL);
        boolean xpDebited = false;
        boolean stonesDebited = false;
        boolean catalystDebited = false;
        try {
            // Exact raw-XP deduction (level, then points, then totalExperience), as approved for every
            // MoreMinecrafts XP payment. Charged in every game mode, as this path always has been.
            RawXp.setTotal(player, (long) RawXp.available(player) - plan.rawExperienceCost());
            xpDebited = true;
            stones.shrink(plan.stoneCost());
            stonesDebited = true;
            if (plan.catalystCost() > 0) {
                catalysts.shrink(plan.catalystCost());
                catalystDebited = true;
            }
            afterDebits.run();
            accessory.set(ModDataComponents.ENHANCEMENT_LEVEL, plan.targetLevel());
            return true;
        } catch (RuntimeException failure) {
            rollback(player, accessory, stones, catalysts, oldLevel, oldStoneCount, oldCatalystCount,
                    xpBefore, xpDebited, stonesDebited, catalystDebited);
            return false;
        } catch (Error failure) {
            rollback(player, accessory, stones, catalysts, oldLevel, oldStoneCount, oldCatalystCount,
                    xpBefore, xpDebited, stonesDebited, catalystDebited);
            throw failure;
        }
    }

    public static int level(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL, 0);
    }

    private static void rollback(Player player, ItemStack accessory, ItemStack stones, ItemStack catalysts,
                                 Integer oldLevel, int oldStoneCount, int oldCatalystCount,
                                 RawXp.Snapshot xpBefore, boolean xpDebited, boolean stonesDebited,
                                 boolean catalystDebited) {
        if (oldLevel == null) accessory.remove(ModDataComponents.ENHANCEMENT_LEVEL);
        else accessory.set(ModDataComponents.ENHANCEMENT_LEVEL, oldLevel);
        if (catalystDebited) catalysts.setCount(oldCatalystCount);
        if (stonesDebited) stones.setCount(oldStoneCount);
        if (xpDebited) xpBefore.restore(player);
    }
}
