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
    public static final String ANY_SMELT_TYPE = "_any";
    public record CollectResult(Set<String> oreTypes, int takenCount) {}
    private static final Map<AbstractFurnaceBlockEntity, List<PendingOutput>> PENDING = new WeakHashMap<>();

    private SmeltingOutputProvenance() {}

    public static void record(AbstractFurnaceBlockEntity furnace, ItemStack input, ItemStack output) {
        if (furnace == null || input.isEmpty() || output.isEmpty()) return;
        String oreType = KnowledgeEventRules.oreType(input).orElse(ANY_SMELT_TYPE);
        List<PendingOutput> pending = PENDING.computeIfAbsent(furnace, ignored -> new ArrayList<>());
        for (PendingOutput entry : pending) {
            if (entry.oreType.equals(oreType) && ItemStack.isSameItemSameComponents(entry.prototype, output)) {
                entry.count += output.getCount();
                furnace.setChanged();
                return;
            }
        }
        pending.add(new PendingOutput(output.copyWithCount(1), oreType, output.getCount()));
        furnace.setChanged();
    }

    public static Set<String> collect(AbstractFurnaceBlockEntity furnace, ItemStack taken) {
        return collectWithCount(furnace, taken).oreTypes();
    }

    public static CollectResult collectWithCount(AbstractFurnaceBlockEntity furnace, ItemStack taken) {
        if (furnace == null || taken.isEmpty()) return new CollectResult(Set.of(), 0);
        List<PendingOutput> pending = PENDING.get(furnace);
        if (pending == null) return new CollectResult(Set.of(), 0);
        int remaining = taken.getCount();
        int totalTaken = 0;
        Set<String> oreTypes = new LinkedHashSet<>();
        for (var iterator = pending.iterator(); iterator.hasNext() && remaining > 0;) {
            PendingOutput output = iterator.next();
            if (!ItemStack.isSameItemSameComponents(output.prototype, taken)) continue;
            int used = Math.min(remaining, output.count);
            if (used > 0) {
                totalTaken += used;
                if (KnowledgeEventRules.isKnownOreType(output.oreType)) oreTypes.add(output.oreType);
            }
            output.count -= used;
            remaining -= used;
            if (output.count == 0) iterator.remove();
        }
        if (pending.isEmpty()) PENDING.remove(furnace);
        furnace.setChanged();
        return new CollectResult(Set.copyOf(oreTypes), totalTaken);
    }

    public static List<PendingOutput> save(AbstractFurnaceBlockEntity furnace) {
        List<PendingOutput> pending = PENDING.get(furnace);
        return pending == null ? List.of() : pending.stream().map(PendingOutput::copy).toList();
    }

    public static void load(AbstractFurnaceBlockEntity furnace, List<PendingOutput> saved) {
        List<PendingOutput> valid = saved.stream().filter(entry -> entry.count > 0 && !entry.prototype.isEmpty()
                && (ANY_SMELT_TYPE.equals(entry.oreType) || KnowledgeEventRules.isKnownOreType(entry.oreType)))
                .map(PendingOutput::copy).toList();
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
