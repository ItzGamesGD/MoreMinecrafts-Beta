package dev.itzgamesgd.moreminecrafts.st01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Per-dimension persistent entrance state, keyed by the tomb anchor (same key as {@code TombEncounterData}); one record per STRUCTURE INSTANCE.
 * Persists: layout (anchor + orientation), state, remaining fallback time, whether the cover was applied, and the participants of the valid
 * victory (used only to choose whom the golem leads). A restart restores the OPEN state; nothing here ever moves back to SEALED.
 */
public final class SecretEntranceData extends SavedData {
    public record Instance(BlockPos anchor, EntranceLayout.Orientation orientation, EntranceState state, long remaining, boolean coverApplied, List<UUID> participants) {
        public long key() { return anchor.asLong(); }
        public Instance withSaved(EntranceCore.Saved s) { return new Instance(anchor, orientation, s.state(), s.remaining(), s.coverApplied(), participants); }
        public Instance withParticipants(List<UUID> p) { return new Instance(anchor, orientation, state, remaining, coverApplied, List.copyOf(p)); }
    }

    private static final Codec<EntranceLayout.Orientation> ORIENTATION = Codec.STRING.xmap(EntranceLayout.Orientation::valueOf, EntranceLayout.Orientation::name);
    private static final Codec<EntranceState> STATE = Codec.STRING.xmap(EntranceState::valueOf, EntranceState::name);
    private static final Codec<Instance> INSTANCE = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("anchor").forGetter(Instance::anchor),
            ORIENTATION.fieldOf("orientation").forGetter(Instance::orientation),
            STATE.fieldOf("state").forGetter(Instance::state),
            Codec.LONG.fieldOf("remaining").forGetter(Instance::remaining),
            Codec.BOOL.fieldOf("cover_applied").forGetter(Instance::coverApplied),
            UUIDUtil.CODEC.listOf().optionalFieldOf("participants", List.of()).forGetter(Instance::participants)).apply(i, Instance::new));
    private static final Codec<SecretEntranceData> CODEC = INSTANCE.listOf().fieldOf("instances").codec().xmap(SecretEntranceData::fromList, SecretEntranceData::toList);

    public static final SavedDataType<SecretEntranceData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:st01_secret_entrance"), SecretEntranceData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<Long, Instance> instances = new LinkedHashMap<>();

    public SecretEntranceData() {}

    private static SecretEntranceData fromList(List<Instance> list) {
        SecretEntranceData d = new SecretEntranceData();
        for (Instance i : list) d.instances.put(i.key(), i);
        return d;
    }

    private List<Instance> toList() { return new ArrayList<>(instances.values()); }

    @Nullable public Instance get(long key) { return instances.get(key); }
    public Iterable<Instance> all() { return List.copyOf(instances.values()); }
    public boolean isEmpty() { return instances.isEmpty(); }

    /** Registers an instance once; an existing record (possibly OPEN) is never overwritten. */
    public boolean register(BlockPos anchor, EntranceLayout.Orientation o) {
        if (instances.containsKey(anchor.asLong())) return false;
        instances.put(anchor.asLong(), new Instance(anchor.immutable(), o, EntranceState.SEALED, 0L, false, List.of()));
        setDirty();
        return true;
    }

    /** Stores a changed record; refuses any state regression so the OPEN state is permanent in normal play. */
    public boolean put(Instance next) {
        Instance old = instances.get(next.key());
        if (old != null && old.state().ordinal() > next.state().ordinal() && old.state() != EntranceState.SEALED) return false;
        if (old != null && old.state().isOpen() && !next.state().isOpen()) return false;
        if (next.equals(old)) return false;
        instances.put(next.key(), next);
        setDirty();
        return true;
    }
}
