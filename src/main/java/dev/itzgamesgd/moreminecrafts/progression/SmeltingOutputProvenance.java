package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Tracks only outputs created by a completed furnace burn until a player takes them. */
public final class SmeltingOutputProvenance {
    private static final Map<AbstractFurnaceBlockEntity, List<PendingOutput>> PENDING = new WeakHashMap<>();

    private SmeltingOutputProvenance() {}

    public static void record(AbstractFurnaceBlockEntity furnace, ItemStack input, ItemStack output) {
        if (furnace == null || input.isEmpty() || output.isEmpty()) return;
        var oreType = KnowledgeEventRules.oreType(input);
        if (oreType.isEmpty()) return;
        List<PendingOutput> pending = PENDING.computeIfAbsent(furnace, ignored -> new ArrayList<>());
        for (PendingOutput entry : pending) {
            if (entry.oreType.equals(oreType.get()) && ItemStack.isSameItemSameComponents(entry.prototype, output)) {
                entry.count += output.getCount();
                return;
            }
        }
        pending.add(new PendingOutput(output.copyWithCount(1), oreType.get(), output.getCount()));
    }

    public static Set<String> collect(AbstractFurnaceBlockEntity furnace, ItemStack taken) {
        if (furnace == null || taken.isEmpty()) return Set.of();
        List<PendingOutput> pending = PENDING.get(furnace);
        if (pending == null) return Set.of();
        int remaining = taken.getCount();
        Set<String> oreTypes = new LinkedHashSet<>();
        for (var iterator = pending.iterator(); iterator.hasNext() && remaining > 0;) {
            PendingOutput output = iterator.next();
            if (!ItemStack.isSameItemSameComponents(output.prototype, taken)) continue;
            int used = Math.min(remaining, output.count);
            if (used > 0) oreTypes.add(output.oreType);
            output.count -= used;
            remaining -= used;
            if (output.count == 0) iterator.remove();
        }
        if (pending.isEmpty()) PENDING.remove(furnace);
        return Set.copyOf(oreTypes);
    }

    private static final class PendingOutput {
        private final ItemStack prototype;
        private final String oreType;
        private int count;

        private PendingOutput(ItemStack prototype, String oreType, int count) {
            this.prototype = prototype;
            this.oreType = oreType;
            this.count = count;
        }
    }
}
