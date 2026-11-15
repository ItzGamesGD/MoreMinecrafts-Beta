package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Per-dimension persistent entrance state, one record per ST-01 STRUCTURE INSTANCE, keyed by the transformed tomb anchor
 * (the D1 PRIMARY left half: the same key as {@code TombEncounterData} and the same anchor as the royal-chest canonical id).
 * Persists the placement (ground + whole-pyramid orientation), state, remaining fallback time and whether the cover was
 * applied. A restart restores the OPEN state; nothing here ever moves back to SEALED.
 */
public final class SecretEntranceData extends SavedData {
    public record Instance(BlockPos tombAnchor, BlockPos ground, St01PyramidAssembly.Orientation orientation,
                           EntranceState state, long remaining, boolean coverApplied) {
        public long key() { return tombAnchor.asLong(); }
        public Instance withSaved(EntranceCore.Saved s) { return new Instance(tombAnchor, ground, orientation, s.state(), s.remaining(), s.coverApplied()); }
    }

    private static final Codec<St01PyramidAssembly.Orientation> ORIENTATION =
            Codec.STRING.xmap(St01PyramidAssembly.Orientation::valueOf, St01PyramidAssembly.Orientation::name);
    private static final Codec<EntranceState> STATE = Codec.STRING.xmap(EntranceState::valueOf, EntranceState::name);
    private static final Codec<Instance> INSTANCE = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("tomb_anchor").forGetter(Instance::tombAnchor),
            BlockPos.CODEC.fieldOf("ground").forGetter(Instance::ground),
            ORIENTATION.fieldOf("orientation").forGetter(Instance::orientation),
            STATE.fieldOf("state").forGetter(Instance::state),
            Codec.LONG.fieldOf("remaining").forGetter(Instance::remaining),
            Codec.BOOL.fieldOf("cover_applied").forGetter(Instance::coverApplied)).apply(i, Instance::new));
    public static final Codec<SecretEntranceData> CODEC =
            INSTANCE.listOf().optionalFieldOf("instances", List.of()).codec().xmap(SecretEntranceData::fromList, SecretEntranceData::toList);

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
    public List<Instance> all() { return List.copyOf(instances.values()); }
    public boolean isEmpty() { return instances.isEmpty(); }

    /** Registers an instance once; an existing record (possibly OPEN) is never overwritten. */
    public boolean register(BlockPos ground, St01PyramidAssembly.Orientation o) {
        BlockPos anchor = EntranceLayout.tombAnchor(ground, o);
        if (instances.containsKey(anchor.asLong())) return false;
        instances.put(anchor.asLong(), new Instance(anchor.immutable(), ground.immutable(), o, EntranceState.SEALED, 0L, false));
        setDirty();
        return true;
    }

    /** Administrative / test cleanup only. */
    public boolean discard(long key) {
        if (instances.remove(key) == null) return false;
        setDirty();
        return true;
    }

    /** Stores a changed record; refuses any state regression so the OPEN state is permanent in normal play. */
    public boolean put(Instance next) {
        Instance old = instances.get(next.key());
        if (old == null) return false;
        if (old.state().ordinal() > next.state().ordinal() && old.state() != EntranceState.SEALED) return false;
        if (old.state().isOpen() && !next.state().isOpen()) return false;
        if (next.equals(old)) return false;
        instances.put(next.key(), next);
        setDirty();
        return true;
    }
}
