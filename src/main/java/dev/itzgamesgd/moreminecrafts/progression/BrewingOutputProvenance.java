package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/** Tracks successful vanilla brew outputs until the collecting player takes them. */
public final class BrewingOutputProvenance {
    private static final Map<BrewingStandBlockEntity, Map<Integer, Pending>> OUTPUTS = new WeakHashMap<>();
    private BrewingOutputProvenance() {}

    public static void completed(ServerLevel level, BlockPos pos, BrewingStandBlockEntity stand,
            ItemStack[] before, ItemStack[] after) {
        Map<Integer, Pending> outputs = OUTPUTS.computeIfAbsent(stand, ignored -> new HashMap<>());
        boolean changed = false;
        for (int slot = 0; slot < 3; slot++) {
            if (!KnowledgeEventCollectors.isPotionItem(after[slot]) || ItemStack.matches(before[slot], after[slot])) continue;
            outputs.put(slot, new Pending(after[slot].copyWithCount(1), after[slot].getCount()));
            changed = true;
        }
        if (!changed && outputs.isEmpty()) OUTPUTS.remove(stand);
    }

    public static int collect(BrewingStandBlockEntity stand, int slot, ItemStack taken) {
        Map<Integer, Pending> outputs = OUTPUTS.get(stand);
        Pending pending = outputs == null ? null : outputs.get(slot);
        if (pending == null || taken.isEmpty() || !ItemStack.isSameItemSameComponents(pending.prototype, taken)) return 0;
        int amount = Math.min(pending.count, taken.getCount());
        pending.count -= amount;
        if (pending.count == 0) outputs.remove(slot);
        if (outputs.isEmpty()) OUTPUTS.remove(stand);
        return amount;
    }

    private static final class Pending {
        private final ItemStack prototype;
        private int count;
        private Pending(ItemStack prototype, int count) { this.prototype = prototype; this.count = count; }
    }
}
