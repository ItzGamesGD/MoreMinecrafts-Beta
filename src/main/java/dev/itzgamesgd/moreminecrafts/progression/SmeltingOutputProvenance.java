package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
    private static final Codec<PendingOutput> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("output").forGetter(entry -> entry.prototype),
            Codec.STRING.fieldOf("ore_type").forGetter(entry -> entry.oreType),
            Codec.INT.fieldOf("count").forGetter(entry -> entry.count)
    ).apply(instance, PendingOutput::new));
    public static final Codec<List<PendingOutput>> CODEC = ENTRY_CODEC.listOf(0, 64);
    public static final int DATA_VERSION = 1;
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
                furnace.setChanged();
                return;
            }
        }
        pending.add(new PendingOutput(output.copyWithCount(1), oreType.get(), output.getCount()));
        furnace.setChanged();
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
        furnace.setChanged();
        return Set.copyOf(oreTypes);
    }

    public static List<PendingOutput> save(AbstractFurnaceBlockEntity furnace) {
        List<PendingOutput> pending = PENDING.get(furnace);
        return pending == null ? List.of() : pending.stream().map(PendingOutput::copy).toList();
    }

    public static void load(AbstractFurnaceBlockEntity furnace, List<PendingOutput> saved) {
        List<PendingOutput> valid = saved.stream().filter(entry -> entry.count > 0 && !entry.prototype.isEmpty()
                && KnowledgeEventRules.isKnownOreType(entry.oreType)).map(PendingOutput::copy).toList();
        if (valid.isEmpty()) PENDING.remove(furnace);
        else PENDING.put(furnace, new ArrayList<>(valid));
    }

    /** Discard evidence when another action replaces the furnace output slot. */
    public static void discard(AbstractFurnaceBlockEntity furnace) {
        PENDING.remove(furnace);
    }

    public static final class PendingOutput {
        private final ItemStack prototype;
        private final String oreType;
        private int count;

        private PendingOutput(ItemStack prototype, String oreType, int count) {
            this.prototype = prototype.copyWithCount(1);
            this.oreType = oreType;
            this.count = Math.max(0, count);
        }
        private PendingOutput copy() { return new PendingOutput(prototype, oreType, count); }
    }
}
