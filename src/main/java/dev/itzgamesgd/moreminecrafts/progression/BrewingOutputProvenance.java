package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
    private static final Codec<Pending> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(entry -> entry.slot),
            ItemStack.CODEC.fieldOf("output").forGetter(entry -> entry.prototype),
            Codec.INT.fieldOf("count").forGetter(entry -> entry.count)
    ).apply(instance, Pending::new));
    public static final Codec<java.util.List<Pending>> CODEC = ENTRY_CODEC.listOf(0, 3);
    public static final int DATA_VERSION = 1;
    private BrewingOutputProvenance() {}

    public static void completed(ServerLevel level, BlockPos pos, BrewingStandBlockEntity stand,
            ItemStack[] before, ItemStack[] after) {
        Map<Integer, Pending> outputs = OUTPUTS.computeIfAbsent(stand, ignored -> new HashMap<>());
        boolean changed = false;
        for (int slot = 0; slot < 3; slot++) {
            if (!KnowledgeEventCollectors.isPotionItem(after[slot]) || ItemStack.matches(before[slot], after[slot])) continue;
            outputs.put(slot, new Pending(slot, after[slot].copyWithCount(1), after[slot].getCount()));
            changed = true;
        }
        if (!changed && outputs.isEmpty()) OUTPUTS.remove(stand);
        if (changed) stand.setChanged();
    }

    public static int collect(BrewingStandBlockEntity stand, int slot, ItemStack taken) {
        Map<Integer, Pending> outputs = OUTPUTS.get(stand);
        Pending pending = outputs == null ? null : outputs.get(slot);
        if (pending == null || taken.isEmpty() || !ItemStack.isSameItemSameComponents(pending.prototype, taken)) return 0;
        int amount = Math.min(pending.count, taken.getCount());
        pending.count -= amount;
        if (pending.count == 0) outputs.remove(slot);
        if (outputs.isEmpty()) OUTPUTS.remove(stand);
        stand.setChanged();
        return amount;
    }

    /**
     * Reconcile pending output after inventory mutations that do not pass through a player output slot.
     * Hopper extraction is not eligible for player credit; if it removes some output, only the remaining
     * portion stays eligible. A missing, changed, or enlarged replacement invalidates the evidence.
     */
    public static void reconcile(BrewingStandBlockEntity stand) {
        Map<Integer, Pending> outputs = OUTPUTS.get(stand);
        if (outputs == null) return;
        outputs.entrySet().removeIf(entry -> {
            Pending pending = entry.getValue();
            ItemStack current = stand.getItem(entry.getKey());
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(pending.prototype, current)
                    || current.getCount() > pending.count) return true;
            pending.count = current.getCount();
            return false;
        });
        if (outputs.isEmpty()) OUTPUTS.remove(stand);
    }

    /** Discard evidence immediately when a brewing output slot is explicitly replaced. */
    public static void discard(BrewingStandBlockEntity stand, int slot) {
        Map<Integer, Pending> outputs = OUTPUTS.get(stand);
        if (outputs == null) return;
        outputs.remove(slot);
        if (outputs.isEmpty()) OUTPUTS.remove(stand);
    }

    public static java.util.List<Pending> save(BrewingStandBlockEntity stand) {
        Map<Integer, Pending> outputs = OUTPUTS.get(stand);
        return outputs == null ? java.util.List.of() : outputs.values().stream().map(Pending::copy).toList();
    }

    public static void load(BrewingStandBlockEntity stand, java.util.List<Pending> saved) {
        Map<Integer, Pending> valid = new HashMap<>();
        for (Pending entry : saved) {
            if (entry.slot >= 0 && entry.slot <= 2 && entry.count > 0 && !entry.prototype.isEmpty()
                    && KnowledgeEventCollectors.isPotionItem(entry.prototype)) valid.put(entry.slot, entry.copy());
        }
        if (valid.isEmpty()) OUTPUTS.remove(stand);
        else OUTPUTS.put(stand, valid);
    }

    public static final class Pending {
        private final int slot;
        private final ItemStack prototype;
        private int count;
        private Pending(int slot, ItemStack prototype, int count) {
            this.slot = slot;
            this.prototype = prototype.copyWithCount(1);
            this.count = Math.max(0, count);
        }
        private Pending copy() { return new Pending(slot, prototype, count); }
    }
}
