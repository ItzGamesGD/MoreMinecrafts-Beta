package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

/**
 * Stage 22 §8 Chest Sorting (`v3.research.13`):
 * In-place stable sorting with lossless stack consolidation.
 * Never merges stacks with different data components; never creates or deletes items.
 */
public final class ChestSortRules {
    public static final int MODE_CATEGORY = 0;
    public static final int MODE_NAME = 1;

    private ChestSortRules() {}

    /**
     * Default category precedence per Stage 22 §8:
     * 0 = tools/equipment, 1 = materials/blocks, 2 = food/farming, 3 = potions/consumables, 4 = miscellaneous.
     */
    public static int categoryRank(ItemStack stack) {
        if (stack.isEmpty()) return 99;
        if (stack.getMaxDamage() > 0 || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) {
            return 0;
        }
        if (stack.getItem() instanceof PotionItem || stack.has(DataComponents.POTION_CONTENTS)) {
            return 3;
        }
        if (stack.has(DataComponents.FOOD)) {
            return 2;
        }
        if (stack.getItem() instanceof BlockItem) {
            return 1;
        }
        return 4;
    }

    /** Consolidates identical stacks (same item + same components) and sorts stably into {@code capacity} slots. */
    public static List<ItemStack> sort(List<ItemStack> snapshot, int mode) {
        int capacity = snapshot.size();
        record Indexed(ItemStack stack, int originalSlot, String itemId, int category, int damage, int compHash) {}

        // 1. Merge identical stacks without mutating snapshot entries.
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack raw : snapshot) {
            if (raw == null || raw.isEmpty()) continue;
            ItemStack remaining = raw.copy();
            for (ItemStack existing : merged) {
                if (ItemStack.isSameItemSameComponents(existing, remaining)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    int transfer = Math.min(space, remaining.getCount());
                    existing.grow(transfer);
                    remaining.shrink(transfer);
                    if (remaining.isEmpty()) break;
                }
            }
            if (!remaining.isEmpty()) merged.add(remaining);
        }

        // 2. Build stable sort entries.
        List<Indexed> indexed = new ArrayList<>(merged.size());
        for (int i = 0; i < merged.size(); i++) {
            ItemStack s = merged.get(i);
            String id = String.valueOf(BuiltInRegistries.ITEM.getKey(s.getItem()));
            indexed.add(new Indexed(s, i, id, categoryRank(s), s.getDamageValue(), s.getComponents().hashCode()));
        }

        Comparator<Indexed> cmp = mode == MODE_NAME
                ? Comparator.comparing(Indexed::itemId)
                        .thenComparingInt(Indexed::damage)
                        .thenComparingInt(Indexed::compHash)
                        .thenComparingInt(Indexed::originalSlot)
                : Comparator.comparingInt(Indexed::category)
                        .thenComparing(Indexed::itemId)
                        .thenComparingInt(Indexed::damage)
                        .thenComparingInt(Indexed::compHash)
                        .thenComparingInt(Indexed::originalSlot);
        indexed.sort(cmp);

        List<ItemStack> result = new ArrayList<>(capacity);
        for (Indexed entry : indexed) {
            if (result.size() < capacity) result.add(entry.stack());
        }
        while (result.size() < capacity) result.add(ItemStack.EMPTY);
        return result;
    }
}
