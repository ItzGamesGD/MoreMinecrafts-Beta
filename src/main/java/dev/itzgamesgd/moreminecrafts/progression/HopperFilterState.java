package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Stage 22 §8 Hopper Filter (`hopper_filter`) persistent rule model:
 * Whitelist or blacklist, up to 9 item identity entries, optional exact-component matching.
 * Default whitelist with 0 entries passes nothing; blacklist with 0 entries passes all.
 */
public record HopperFilterState(boolean whitelist, List<Entry> entries) {
    public static final int MAX_ENTRIES = 9;

    public record Entry(String itemId, boolean matchComponents, int componentHash) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("item_id").forGetter(Entry::itemId),
                Codec.BOOL.optionalFieldOf("match_components", false).forGetter(Entry::matchComponents),
                Codec.INT.optionalFieldOf("component_hash", 0).forGetter(Entry::componentHash)
        ).apply(instance, Entry::new));

        public boolean matches(String candidateItemId, int candidateComponentHash) {
            if (!itemId.equals(candidateItemId)) return false;
            return !matchComponents || componentHash == candidateComponentHash;
        }
    }

    public static final Codec<HopperFilterState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(HopperFilterState::whitelist),
            Entry.CODEC.listOf(0, MAX_ENTRIES).optionalFieldOf("entries", List.of()).forGetter(HopperFilterState::entries)
    ).apply(instance, HopperFilterState::new));

    public HopperFilterState {
        entries = entries == null ? List.of()
                : entries.size() > MAX_ENTRIES ? List.copyOf(entries.subList(0, MAX_ENTRIES)) : List.copyOf(entries);
    }

    public static HopperFilterState emptyWhitelist() {
        return new HopperFilterState(true, List.of());
    }

    public boolean allows(String itemId, int componentHash) {
        boolean matched = entries.stream().anyMatch(e -> e.matches(itemId, componentHash));
        return whitelist ? matched : !matched;
    }

    public boolean allows(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return allows(id, stack.getComponents().hashCode());
    }
}
