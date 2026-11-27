package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestData;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import dev.itzgamesgd.moreminecrafts.pyramid.entrance.SecretEntranceRuntime;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Server-thread completion handoff from natural ST-01 generation to the unchanged P8-B2 royal-chest runtime (P8-C2-B §7).
 *
 * <p>Vanilla offers no atomic cross-chunk "structure finished" event, so this is a persisted, recoverable protocol:
 * <ol>
 *   <li><b>Commit.</b> A chunk that reaches FULL status has run its FEATURES step, i.e. every ST-01 piece intersecting it has
 *       been written. On {@code CHUNK_LOAD} (newly generated or loaded from disk), each ST-01 start the chunk references is
 *       recorded with that chunk as committed. The start's own chunk additionally records the start's metadata (ground,
 *       orientation, route, bounding box), read from its {@link St01AssemblyPiece}s.</li>
 *   <li><b>Complete.</b> A start is complete once its metadata is known and every chunk intersecting its bounding box is
 *       committed. Only then, on the server thread at the end of a tick and only while both PRIMARY chunks are loaded (no
 *       chunk is ever force-loaded), {@link St01RoyalChestPlacement#registerPlaced} is called with the D1 origin and settings
 *       derived from the actual placed transform ({@link St01PyramidAssembly#describe}).</li>
 *   <li><b>Idempotence and recovery.</b> All records are persisted per dimension. Registration is idempotent in P8-B2 (an
 *       existing instance keeps its catalyst, Guardian and reward state). A REGISTERED record whose P8-B2 instance is missing
 *       (e.g. one saved-data file lost in a crash between two saves) is re-registered from the same deterministic inputs,
 *       which yields the identical canonical id and catalyst draw; nothing is ever re-rolled or reset.</li>
 * </ol>
 * Rejected candidates have no structure start and therefore no references, so they can never create a record.
 */
public final class St01PyramidCompletion extends SavedData {
    public enum State { PENDING, REGISTERED, REJECTED }

    /** Placement metadata read from the actual structure start. */
    public record Meta(BlockPos ground, Orientation orientation, AccessRoute route, BoundingBox box) {}

    /** One natural ST-01 start in this dimension, keyed by its start chunk. */
    public static final class Entry {
        final long startChunk;
        final LongSet committed = new LongOpenHashSet();
        Meta meta;
        State state = State.PENDING;
        String instanceId;
        String detail;

        Entry(long startChunk) { this.startChunk = startChunk; }

        public long startChunk() { return startChunk; }
        public Optional<Meta> meta() { return Optional.ofNullable(meta); }
        public State state() { return state; }
        public Optional<String> instanceId() { return Optional.ofNullable(instanceId); }
        public Optional<String> detail() { return Optional.ofNullable(detail); }
        public int committedCount() { return committed.size(); }
        public boolean isCommitted(long chunk) { return committed.contains(chunk); }
    }

    // ---- persistence ----

    private record MetaSave(long ground, String orientation, String route, List<Integer> box) {}

    private record EntrySave(long start, List<Long> committed, Optional<MetaSave> meta, String state,
                             Optional<String> instance, Optional<String> detail) {}

    private static final Codec<MetaSave> META = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("ground").forGetter(MetaSave::ground),
            Codec.STRING.fieldOf("orientation").forGetter(MetaSave::orientation),
            Codec.STRING.fieldOf("route").forGetter(MetaSave::route),
            Codec.INT.listOf().fieldOf("box").forGetter(MetaSave::box)).apply(i, MetaSave::new));
    private static final Codec<EntrySave> ENTRY = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("start").forGetter(EntrySave::start),
            Codec.LONG.listOf().fieldOf("committed").forGetter(EntrySave::committed),
            META.optionalFieldOf("meta").forGetter(EntrySave::meta),
            Codec.STRING.fieldOf("state").forGetter(EntrySave::state),
            Codec.STRING.optionalFieldOf("instance").forGetter(EntrySave::instance),
            Codec.STRING.optionalFieldOf("detail").forGetter(EntrySave::detail)).apply(i, EntrySave::new));
    private static final Codec<St01PyramidCompletion> CODEC = ENTRY.listOf().optionalFieldOf("starts", List.of()).codec()
            .xmap(St01PyramidCompletion::fromSaved, St01PyramidCompletion::toSaved);

    public static final SavedDataType<St01PyramidCompletion> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:st01_pyramid_completion"), St01PyramidCompletion::new, CODEC, DataFixTypes.LEVEL);

    private final Map<Long, Entry> entries = new LinkedHashMap<>();

    public St01PyramidCompletion() {}

    private static St01PyramidCompletion fromSaved(List<EntrySave> saved) {
        St01PyramidCompletion data = new St01PyramidCompletion();
        for (EntrySave s : saved) {
            Entry e = new Entry(s.start());
            s.committed().forEach(e.committed::add);
            e.meta = s.meta().map(m -> new Meta(BlockPos.of(m.ground()), Orientation.valueOf(m.orientation()), AccessRoute.valueOf(m.route()),
                    new BoundingBox(m.box().get(0), m.box().get(1), m.box().get(2), m.box().get(3), m.box().get(4), m.box().get(5))))
                    .orElse(null);
            e.state = State.valueOf(s.state());
            e.instanceId = s.instance().orElse(null);
            e.detail = s.detail().orElse(null);
            data.entries.put(e.startChunk, e);
        }
        return data;
    }

    private List<EntrySave> toSaved() {
        List<EntrySave> out = new ArrayList<>();
        for (Entry e : entries.values()) {
            List<Long> committed = new ArrayList<>(e.committed);
            committed.sort(Long::compare);
            Optional<MetaSave> meta = e.meta().map(m -> new MetaSave(m.ground().asLong(), m.orientation().name(), m.route().name(),
                    List.of(m.box().minX(), m.box().minY(), m.box().minZ(), m.box().maxX(), m.box().maxY(), m.box().maxZ())));
            out.add(new EntrySave(e.startChunk, committed, meta, e.state.name(), Optional.ofNullable(e.instanceId), Optional.ofNullable(e.detail)));
        }
        return out;
    }

    // ---- pure protocol state (unit-testable) ----

    public Optional<Entry> entry(long startChunk) { return Optional.ofNullable(entries.get(startChunk)); }

    public List<Entry> entries() { return List.copyOf(entries.values()); }

    private Entry entryFor(long startChunk) { return entries.computeIfAbsent(startChunk, Entry::new); }

    /** Records that {@code chunk} reached FULL while referencing {@code startChunk}. Returns whether anything changed. */
    public boolean commit(long startChunk, long chunk) {
        boolean changed = entryFor(startChunk).committed.add(chunk);
        if (changed) setDirty();
        return changed;
    }

    /** Records the start's metadata (first sighting of the start chunk). Never replaces existing metadata. */
    public boolean describe(long startChunk, Meta meta) {
        Entry e = entryFor(startChunk);
        if (e.meta != null) return false;
        e.meta = meta;
        setDirty();
        return true;
    }

    /** Every chunk intersecting {@code box}, as packed chunk positions. */
    public static long[] requiredChunks(BoundingBox box) {
        int x0 = box.minX() >> 4, x1 = box.maxX() >> 4, z0 = box.minZ() >> 4, z1 = box.maxZ() >> 4;
        long[] out = new long[(x1 - x0 + 1) * (z1 - z0 + 1)];
        int n = 0;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) out[n++] = ChunkPos.pack(x, z);
        return out;
    }

    /** The whole structure is committed: metadata known and every chunk of its bounding box at FULL at least once. */
    public static boolean isComplete(Entry e) {
        if (e.meta == null) return false;
        for (long c : requiredChunks(e.meta.box())) if (!e.committed.contains(c)) return false;
        return true;
    }

    void markRegistered(Entry e, String id) {
        e.state = State.REGISTERED;
        e.instanceId = id;
        e.detail = null;
        setDirty();
    }

    void markRejected(Entry e, String why) {
        e.state = State.REJECTED;
        e.detail = why;
        setDirty();
    }

    // ---- runtime wiring ----

    private record Sighting(ServerLevel level, long chunk, long[] startRefs, List<Map.Entry<Long, Meta>> starts) {}

    private static final Queue<Sighting> QUEUE = new ConcurrentLinkedQueue<>();
    private static boolean initialized;

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) -> onChunkLoad(level, chunk));
        ServerTickEvents.END_SERVER_TICK.register(St01PyramidCompletion::processPending);
    }

    public static St01PyramidCompletion data(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }

    /** Snapshot only; all state changes happen on the server thread in {@link #processPending}. */
    static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        LongSet refs = null;
        for (Map.Entry<Structure, LongSet> ref : chunk.getAllReferences().entrySet()) {
            if (ref.getKey() instanceof St01PyramidStructure && !ref.getValue().isEmpty()) {
                if (refs == null) refs = new LongOpenHashSet();
                refs.addAll(ref.getValue());
            }
        }
        if (refs == null) return;
        List<Map.Entry<Long, Meta>> starts = new ArrayList<>();
        for (Map.Entry<Structure, StructureStart> s : chunk.getAllStarts().entrySet()) {
            if (s.getKey() instanceof St01PyramidStructure && s.getValue().isValid()) {
                metaOf(s.getValue()).ifPresent(m -> starts.add(Map.entry(chunk.getPos().pack(), m)));
            }
        }
        QUEUE.add(new Sighting(level, chunk.getPos().pack(), refs.toLongArray(), starts));
    }

    /** Metadata of an ST-01 start, from its assembly piece and its bounding box. */
    public static Optional<Meta> metaOf(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof St01AssemblyPiece a && a.pieceIndex() == 0) {
                return Optional.of(new Meta(a.ground(), a.orientation(), a.route(), start.getBoundingBox()));
            }
        }
        return Optional.empty();
    }

    /** Drains queued chunk sightings and attempts completion. Server thread only (END_SERVER_TICK, or a probe on that thread). */
    public static void processPending(MinecraftServer server) {
        Map<ServerLevel, LongSet> touched = new LinkedHashMap<>();
        for (Sighting s; (s = QUEUE.poll()) != null; ) {
            St01PyramidCompletion data = data(s.level());
            for (Map.Entry<Long, Meta> start : s.starts()) data.describe(start.getKey(), start.getValue());
            LongSet set = touched.computeIfAbsent(s.level(), k -> new LongOpenHashSet());
            for (long ref : s.startRefs()) {
                data.commit(ref, s.chunk());
                set.add(ref);
            }
        }
        touched.forEach((level, starts) -> {
            for (LongIterator it = starts.iterator(); it.hasNext(); ) tryComplete(level, it.nextLong());
        });
    }

    /** Registers the start's PRIMARY chest with P8-B2 once complete. Safe to call repeatedly. */
    public static Optional<RoyalChestData.Instance> tryComplete(ServerLevel level, long startChunk) {
        St01PyramidCompletion data = data(level);
        Entry e = data.entries.get(startChunk);
        if (e == null || e.state == State.REJECTED || !isComplete(e)) return Optional.empty();
        if (e.state == State.REGISTERED && e.instanceId != null
                && RoyalChestRuntime.data(level).instance(e.instanceId).isPresent()) {
            SecretEntranceRuntime.register(level, e.meta.ground(), e.meta.orientation()); // idempotent; covers starts registered earlier
            return RoyalChestRuntime.data(level).instance(e.instanceId);
        }
        St01PyramidAssembly.Placement p = St01PyramidAssembly.describe(e.meta.ground(), e.meta.orientation(), e.meta.route());
        if (!loaded(level, p.primaryLeft()) || !loaded(level, p.primaryRight())) return Optional.empty(); // retried on their next load
        Optional<RoyalChestData.Instance> instance = St01RoyalChestPlacement.registerPlaced(level, p.d1PlacementOrigin(), p.d1Settings());
        if (instance.isPresent()) {
            data.markRegistered(e, instance.get().id());
            SecretEntranceRuntime.register(level, p); // same ground/orientation, keyed by the same tomb anchor as the PRIMARY chest
        }
        else data.markRejected(e, "P8-B2 refused the physical PRIMARY pair at " + p.primaryLeft().toShortString()
                + " / " + p.primaryRight().toShortString() + ": " + level.getBlockState(p.primaryLeft()) + " / " + level.getBlockState(p.primaryRight()));
        return instance;
    }

    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }
}
