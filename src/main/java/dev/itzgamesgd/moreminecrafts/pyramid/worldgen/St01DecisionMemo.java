package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * P8-D4 C4: per-world memo of ST-01 candidate decisions.
 *
 * <p>Vanilla evaluates every found candidate twice: {@code StructureCheck.canCreateStructure} on the server thread
 * (locator), then {@code ChunkGenerator.createStructures -> Structure.generate} on a worldgen worker when the locator
 * requests the chunk at {@code STRUCTURE_STARTS}. Both build a fresh {@code GenerationContext} for the same world and chunk
 * and run {@link St01Candidate#decide}, which is a pure function of the world's generator, random state, seed, height
 * range, chunk and the chunk's seeded rotation. This memo returns the first result to every later evaluation.
 *
 * <p><b>Key.</b> One cache per {@link RandomState} (one per dimension of a loaded world, weakly held so an unloaded world
 * releases its cache), keyed by packed chunk position. A cache also records the generator (weakly), seed and height range
 * it was created for; a context that differs in any of them bypasses the memo. A cached decision is used only if its
 * orientation equals the rotation drawn from the current context's seeded random (the RNG is still consumed exactly as
 * before). <b>Lifetime.</b> Worldgen registries do not change while a world is loaded, so entries never go stale; all
 * caches are cleared when the server stops. <b>Size.</b> LRU, at most {@code -Dmm.st01.memo.max} entries per world
 * (default 65,536 = 1.6x a radius-100 search). <b>Threads.</b> Decisions are immutable; the map is accessed under its
 * own lock and a decision is computed outside it (two threads may compute the same key; the results are identical).
 * {@code -Dmm.st01.memo.disable=true} or {@code -Dmm.st01.terrain.legacy=true} bypasses the memo.
 */
public final class St01DecisionMemo {
    private St01DecisionMemo() {}

    public static final AtomicLong HITS = new AtomicLong();
    public static final AtomicLong MISSES = new AtomicLong();
    public static final AtomicLong BYPASSES = new AtomicLong();

    private static final Map<RandomState, Cache> CACHES = new WeakHashMap<>();

    public static boolean enabled() {
        return !St01FastTerrain.legacyForced() && !Boolean.getBoolean("mm.st01.memo.disable");
    }

    static int maxEntries() {
        return Math.max(1, Integer.getInteger("mm.st01.memo.max", 65_536));
    }

    /** The decision for {@code context}'s chunk, from the memo or from {@code compute} (then remembered). */
    public static St01Candidate.Decision decide(Structure.GenerationContext context, Rotation rotation, Supplier<St01Candidate.Decision> compute) {
        if (!enabled()) return compute.get();
        Cache cache = cacheFor(context);
        if (cache == null) {
            BYPASSES.incrementAndGet();
            return compute.get();
        }
        long key = context.chunkPos().pack();
        St01Candidate.Decision cached = cache.get(key);
        if (cached != null && cached.orientation() == St01Candidate.orientationOf(rotation)) {
            HITS.incrementAndGet();
            return cached;
        }
        MISSES.incrementAndGet();
        St01Candidate.Decision fresh = compute.get();
        cache.put(key, fresh);
        return fresh;
    }

    /** Whether a decision for {@code context}'s chunk is already remembered (diagnostics / preparation). */
    public static boolean contains(Structure.GenerationContext context) {
        Cache cache = enabled() ? cacheFor(context) : null;
        return cache != null && cache.get(context.chunkPos().pack()) != null;
    }

    /**
     * Whether a decision for {@code chunk} is remembered for the world described by the arguments (P8-D6: lets the
     * server-thread cache warm-up skip every candidate it would otherwise have to evaluate itself).
     */
    public static boolean contains(RandomState randomState, ChunkGenerator generator, long seed, int minY, int height, net.minecraft.world.level.ChunkPos chunk) {
        if (!enabled()) return false;
        Cache c;
        synchronized (CACHES) {
            c = CACHES.get(randomState);
        }
        return c != null && c.matches(generator, seed, minY, height) && c.get(chunk.pack()) != null;
    }

    public static int size(RandomState randomState) {
        synchronized (CACHES) {
            Cache c = CACHES.get(randomState);
            return c == null ? 0 : c.size();
        }
    }

    public static void clear() {
        synchronized (CACHES) {
            CACHES.clear();
        }
    }

    private static Cache cacheFor(Structure.GenerationContext context) {
        synchronized (CACHES) {
            Cache c = CACHES.get(context.randomState());
            if (c == null) {
                c = new Cache(context.chunkGenerator(), context.seed(), context.heightAccessor().getMinY(), context.heightAccessor().getHeight());
                CACHES.put(context.randomState(), c);
            }
            return c.matches(context) ? c : null;
        }
    }

    static final class Cache {
        private final WeakReference<ChunkGenerator> generator;
        private final long seed;
        private final int minY, height;
        private final Map<Long, St01Candidate.Decision> entries;

        Cache(ChunkGenerator generator, long seed, int minY, int height) {
            this.generator = new WeakReference<>(generator);
            this.seed = seed;
            this.minY = minY;
            this.height = height;
            int max = maxEntries();
            this.entries = new LinkedHashMap<>(1024, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long, St01Candidate.Decision> eldest) { return size() > max; }
            };
        }

        boolean matches(Structure.GenerationContext context) {
            return matches(context.chunkGenerator(), context.seed(), context.heightAccessor().getMinY(), context.heightAccessor().getHeight());
        }

        boolean matches(ChunkGenerator generator, long seed, int minY, int height) {
            return this.generator.get() == generator && this.seed == seed && this.minY == minY && this.height == height;
        }

        synchronized St01Candidate.Decision get(long key) { return entries.get(key); }

        synchronized void put(long key, St01Candidate.Decision decision) { entries.putIfAbsent(key, decision); }

        synchronized int size() { return entries.size(); }
    }
}
