package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Immutable chunk-scoped map from packed farmland block position to its last valid cultivator. */
public record FarmlandCultivatorData(int schemaVersion, Map<Long, UUID> cultivators) {
    public static final int SCHEMA_VERSION = 1;
    public static final FarmlandCultivatorData EMPTY = new FarmlandCultivatorData(SCHEMA_VERSION, Map.of());

    private record Entry(long position, UUID cultivator) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(Entry::position),
                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::cultivator)
        ).apply(instance, Entry::new));
    }

    public static final Codec<FarmlandCultivatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(FarmlandCultivatorData::schemaVersion),
            Entry.CODEC.listOf().fieldOf("farmland").forGetter(data -> data.cultivators().entrySet().stream()
                    .map(entry -> new Entry(entry.getKey(), entry.getValue())).toList())
    ).apply(instance, (version, entries) -> {
        Map<Long, UUID> map = new HashMap<>();
        for (Entry entry : entries) map.put(entry.position(), entry.cultivator());
        return new FarmlandCultivatorData(version, map);
    }));

    public FarmlandCultivatorData { cultivators = Map.copyOf(cultivators); }

    public boolean isEmpty() { return cultivators.isEmpty(); }

    public FarmlandCultivatorData with(long position, UUID cultivator) {
        Map<Long, UUID> updated = new HashMap<>(cultivators);
        updated.put(position, cultivator);
        return new FarmlandCultivatorData(SCHEMA_VERSION, updated);
    }

    public FarmlandCultivatorData without(long position) {
        if (!cultivators.containsKey(position)) return this;
        Map<Long, UUID> updated = new HashMap<>(cultivators);
        updated.remove(position);
        return new FarmlandCultivatorData(SCHEMA_VERSION, updated);
    }
}
