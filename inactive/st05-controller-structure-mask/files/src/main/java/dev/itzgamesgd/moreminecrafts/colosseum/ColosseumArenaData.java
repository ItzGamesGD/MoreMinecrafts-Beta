package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Per-dimension, world-persistent registry of colosseum arenas and their controller state.
 * <p>
 * Schema v1 root: {@code {schema: 1, arenas: [...], archived: [...]}}. A pre-schema (v0) world stored a bare list of
 * arenas and is migrated on load. A root with {@code schema > 1} was written by a newer build: it is kept
 * verbatim, never ticked and never rewritten with changes (read-only preservation), so a downgrade cannot lose data.
 */
public final class ColosseumArenaData extends SavedData {
    public static final int SCHEMA = 1;

    private record Archived(String instanceId, RewardLedger ledger) {
        static final Codec<Archived> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("instance_id").forGetter(Archived::instanceId),
                RewardLedger.CODEC.fieldOf("ledger").forGetter(Archived::ledger)
        ).apply(i, Archived::new));
    }

    private record Root(int schema, List<ColosseumArena> arenas, List<Archived> archived) {}

    private static final Codec<Root> V1 = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("schema").forGetter(Root::schema),
            ColosseumArena.CODEC.listOf().fieldOf("arenas").forGetter(Root::arenas),
            Archived.CODEC.listOf().optionalFieldOf("archived", List.of()).forGetter(Root::archived)
    ).apply(i, Root::new));

    private static final Codec<ColosseumArenaData> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ColosseumArenaData, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> dynamic = new Dynamic<>(ops, input);
            if (ops.getStream(input).result().isPresent()) return migrateV0(dynamic).map(data -> Pair.of(data, input));
            var schema = dynamic.get("schema").asNumber().result();
            if (schema.isEmpty()) return DataResult.error(() -> "ST-05 arena data has no schema version");
            int version = schema.get().intValue();
            if (version > SCHEMA) {
                MoreMinecrafts.LOGGER.error("ST-05 arena data uses schema {} but this build supports {}; "
                        + "keeping it read-only and untouched.", version, SCHEMA);
                return DataResult.success(Pair.of(new ColosseumArenaData(dynamic), input));
            }
            if (version != SCHEMA) return DataResult.error(() -> "unsupported ST-05 arena data schema " + version);
            return V1.decode(ops, input).map(pair -> pair.mapFirst(ColosseumArenaData::fromRoot));
        }

        @Override
        public <T> DataResult<T> encode(ColosseumArenaData data, DynamicOps<T> ops, T prefix) {
            if (data.preserved != null) return DataResult.success(data.preserved.convert(ops).getValue());
            return V1.encode(data.toRoot(), ops, prefix);
        }
    };

    public static final SavedDataType<ColosseumArenaData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:st05_colosseum_arenas"), ColosseumArenaData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<String, ColosseumArena> arenas = new LinkedHashMap<>();
    private final Map<String, RewardLedger> archived = new LinkedHashMap<>();
    @Nullable private final Dynamic<?> preserved;

    public ColosseumArenaData() { this.preserved = null; }

    private ColosseumArenaData(Dynamic<?> preserved) { this.preserved = preserved; }

    private static ColosseumArenaData fromRoot(Root root) {
        ColosseumArenaData data = new ColosseumArenaData();
        for (ColosseumArena arena : root.arenas()) data.arenas.putIfAbsent(arena.instanceId(), arena);
        for (Archived a : root.archived()) data.archived.putIfAbsent(a.instanceId(), a.ledger());
        return data;
    }

    private Root toRoot() {
        List<Archived> list = new ArrayList<>();
        archived.forEach((id, ledger) -> list.add(new Archived(id, ledger)));
        return new Root(SCHEMA, new ArrayList<>(arenas.values()), list);
    }

    /**
     * v0 (pre-schema) migration. The arena list is kept as is; each arena gets an id (dimension unknown until
     * {@link #adoptDimension}), the old {@code cleared_waves} become ledger waves with key state {@code NONE}
     * (v0 never issued keys, so none is issued retroactively), and {@code timers.boss_started} becomes
     * {@code content.boss.started}. A missing trial limit means a full 65 minutes.
     */
    static <T> DataResult<ColosseumArenaData> migrateV0(Dynamic<T> list) {
        ColosseumArenaData data = new ColosseumArenaData();
        List<String> errors = new ArrayList<>();
        list.asStream().forEach(dynamic -> {
            var geometry = ColosseumArenaGeometry.CODEC.parse(dynamic.get("geometry").orElseEmptyMap());
            var stateDynamic = dynamic.get("state").orElseEmptyMap();
            var state = ColosseumTrialState.CODEC.parse(stateDynamic);
            if (geometry.error().isPresent() || state.error().isPresent()) {
                errors.add(geometry.error().map(DataResult.Error::message).orElse("")
                        + state.error().map(DataResult.Error::message).orElse(""));
                return;
            }
            ColosseumTrialState trial = state.result().orElseThrow();
            trial.boss.started = stateDynamic.get("timers").get("boss_started").asBoolean(false);
            for (Dynamic<T> wave : stateDynamic.get("content").get("cleared_waves").asStream().toList()) {
                var parsed = WaveRecord.CODEC.parse(wave);
                if (parsed.result().isPresent()) trial.ledger.addWave(parsed.result().get());
                else errors.add(parsed.error().map(DataResult.Error::message).orElse("wave"));
            }
            ColosseumArena arena = new ColosseumArena(ColosseumArena.testInstanceId(ColosseumArena.UNKNOWN_DIMENSION,
                    geometry.result().orElseThrow().center()), geometry.result().orElseThrow(), trial);
            data.arenas.putIfAbsent(arena.instanceId(), arena);
        });
        if (!errors.isEmpty()) return DataResult.error(() -> "ST-05 v0 arena migration failed: " + errors);
        MoreMinecrafts.LOGGER.info("ST-05 migrated {} v0 arena record(s) to schema {}.", data.arenas.size(), SCHEMA);
        return DataResult.success(data);
    }

    /** Replaces the dimension placeholder of migrated ids once the owning level is known. */
    public void adoptDimension(String dimension) {
        boolean changed = false;
        for (ColosseumArena arena : new ArrayList<>(arenas.values())) {
            String id = arena.instanceId();
            if (!id.equals(ColosseumArena.testInstanceId(ColosseumArena.UNKNOWN_DIMENSION, arena.geometry().center()))) continue;
            arenas.remove(id);
            arena.instanceId(ColosseumArena.testInstanceId(dimension, arena.geometry().center()));
            arenas.put(arena.instanceId(), arena);
            changed = true;
        }
        if (changed) setDirty();
    }

    /** True when a newer build wrote this data: it is neither ticked nor modified. */
    public boolean readOnly() { return preserved != null; }

    public Collection<ColosseumArena> arenas() { return Collections.unmodifiableCollection(arenas.values()); }

    @Nullable public ColosseumArena arenaByInstance(String instanceId) { return arenas.get(instanceId); }

    /** Lookup by exact packed center; a legacy-tag fallback and the command/test entry point. */
    @Nullable public ColosseumArena arena(long centerKey) {
        for (ColosseumArena arena : arenas.values()) if (arena.key() == centerKey) return arena;
        return null;
    }

    public Map<String, RewardLedger> archived() { return Collections.unmodifiableMap(archived); }

    /**
     * Idempotent by center: an arena already registered there keeps its geometry and state. A ledger archived under
     * the same instance id is restored into the new arena, so archiving never loses history.
     */
    public ColosseumArena register(ColosseumArenaGeometry geometry, String instanceId) {
        if (readOnly()) throw new IllegalStateException("ST-05 arena data was written by a newer build and is read-only");
        ColosseumArena existing = arena(geometry.center().asLong());
        if (existing != null) return existing;
        ColosseumArena arena = new ColosseumArena(instanceId, geometry, new ColosseumTrialState());
        RewardLedger restored = archived.remove(instanceId);
        if (restored != null) arena.state().ledger.absorb(restored);
        arenas.put(instanceId, arena);
        setDirty();
        return arena;
    }

    /** Removes the arena record only. Callers decide whether anything must be preserved first. */
    boolean remove(ColosseumArena arena) {
        if (arenas.remove(arena.instanceId()) == null) return false;
        setDirty();
        return true;
    }

    /** Moves the arena's ledger to the archive under its instance id and removes the arena. */
    void archiveAndRemove(ColosseumArena arena) {
        if (arenas.remove(arena.instanceId()) == null) return;
        if (!arena.state().ledger.isEmpty()) {
            RewardLedger target = archived.computeIfAbsent(arena.instanceId(), id -> new RewardLedger());
            target.absorb(arena.state().ledger);
        }
        setDirty();
    }
}
