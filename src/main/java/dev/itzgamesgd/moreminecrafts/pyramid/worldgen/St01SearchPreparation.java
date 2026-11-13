package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import dev.itzgamesgd.moreminecrafts.mixin.St01MerchantOffersAccessor;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P8-D6 C6: background preparation of ST-01 explorer-map searches.
 *
 * <p><b>What it does.</b> When a villager that can still be offered an ST-01 explorer map (per {@link St01MapTradeIndex})
 * is loaded, changes profession or level, or opens its trading screen, the candidates that vanilla's
 * {@code findNearestMapStructure} would visit around the villager are decided ahead of time on one low-priority worker
 * thread, in vanilla ring order, through the unchanged C1+C2 path into the C4 {@link St01DecisionMemo}; it stops one ring
 * ({@code -Dmm.st01.c6.extraRings}) after the first ring that holds an eligible start. The server thread then feeds those
 * decisions into vanilla's own {@code StructureCheck} cache through {@code StructureManager.checkStructurePresence}, at most
 * {@code -Dmm.st01.c6.warmBudgetUs} (default 2000 µs) per tick. When the trade is later generated, vanilla's unchanged
 * locator finds every candidate already decided.
 *
 * <p><b>What it does not do.</b> It never searches on the server thread, never evaluates a candidate there (warm-up skips
 * any candidate the memo does not hold), never loads or generates a chunk, and never changes trades, placement, eligibility,
 * {@code search_radius} or {@code skip_existing_chunks}. If preparation is not finished when the trade is generated, vanilla
 * behaves exactly as before (the trade policy is unchanged; see the P8-D6 report).
 *
 * <p><b>Threads.</b> The worker reads only immutable or concurrent worldgen state (generator, biome source, random state,
 * its own climate sampler and generation context, the memo). {@code StructureCheck}, chunks, entities and level data are
 * touched only on the server thread. Requests are de-duplicated per (level, placement region, radius); at most
 * {@link #QUEUE_CAP} are queued or running, further requests are rejected and retried by the next trigger. Unloading a
 * level cancels its work; stopping the server cancels everything and stops the thread. Cancellation is checked between
 * candidates and inside a decision (each chunk of heights, each prefilter column).
 *
 * <p>{@code -Dmm.st01.c6.disable=true} (or disabling the memo) makes the service inert.
 */
public final class St01SearchPreparation {
    private St01SearchPreparation() {}

    private static final Logger LOG = LoggerFactory.getLogger("moreminecrafts/st01-search-preparation");
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    /** Maximum number of preparation jobs queued or running at once. */
    public static final int QUEUE_CAP = 8;
    static final int READY_MAX = 256;

    public static final AtomicLong REQUESTS = new AtomicLong();
    public static final AtomicLong QUEUED = new AtomicLong();
    public static final AtomicLong REJECTED = new AtomicLong();
    public static final AtomicLong COMPLETED = new AtomicLong();
    public static final AtomicLong CANCELLED = new AtomicLong();
    public static final AtomicLong FAILED = new AtomicLong();
    public static final AtomicLong CANDIDATES = new AtomicLong();
    public static final AtomicLong WARMED = new AtomicLong();
    public static final AtomicLong WARM_SKIPPED = new AtomicLong();
    public static final AtomicLong WORKER_CPU_NANOS = new AtomicLong();
    /** Longest single server-thread warm-up pass (diagnostics). */
    public static final AtomicLong PUMP_MAX_NANOS = new AtomicLong();

    public enum Status {
        /** Disabled, no ST-01 placement in this level, or not on the server thread: nothing is done. */
        INERT,
        /** A new job was queued. */
        QUEUED,
        /** The same job is already queued or running. */
        PENDING,
        /** The same job has completed; its decisions are in the memo. */
        READY,
        /** The queue is full or the service is shutting down; a later trigger will retry. */
        REJECTED
    }

    /** Immutable snapshot of what a preparation needs from one level (no level, chunk or entity access). */
    public record Target(Object owner, RegistryAccess access, ChunkGenerator generator, BiomeSource biomeSource, RandomState randomState,
            StructureTemplateManager templates, long seed, LevelHeightAccessor height, Holder<Structure> structure,
            RandomSpreadStructurePlacement placement) {}

    /** Requests with the same key visit the same candidates (vanilla's rings depend on the origin's placement region only). */
    public record Key(Object owner, int regionX, int regionZ, int radius, int minEligible) {
        static Key of(Target t, BlockPos origin, int radius) {
            return of(t, origin, radius, 1);
        }

        static Key of(Target t, BlockPos origin, int radius, int minEligible) {
            int spacing = t.placement().spacing();
            return new Key(t.owner(), Math.floorDiv(SectionPos.blockToSectionCoord(origin.getX()), spacing),
                    Math.floorDiv(SectionPos.blockToSectionCoord(origin.getZ()), spacing), radius, Math.max(1, minEligible));
        }
    }

    /** An eligible ST-01 start found by a preparation, with its vanilla search ring. */
    public record Eligible(ChunkPos chunk, int ring) {}

    /** A completed preparation: candidates decided, the first eligible candidate in ring order, worker CPU and wall time. */
    public record Result(int candidates, Optional<ChunkPos> firstEligible, int eligibleRing, List<Eligible> eligible, long cpuNanos, long wallNanos) {}

    /** Server-thread action that feeds one prepared candidate into vanilla's StructureCheck. */
    @FunctionalInterface
    public interface WarmSink {
        void warm(Target target, ChunkPos chunk);
    }

    record WarmItem(Key key, Target target, ChunkPos chunk) {}

    private static final class Job {
        final Key key;
        final Target target;
        final BlockPos origin;
        volatile boolean cancelled;

        Job(Key key, Target target, BlockPos origin) {
            this.key = key;
            this.target = target;
            this.origin = origin;
        }
    }

    /** One worker thread, de-duplication, queue cap, cancellation and the server-thread warm-up queue. */
    public static final class Preparer {
        private final ThreadPoolExecutor executor;
        private final int cap;
        private final int extraRings;
        private final Consumer<ChunkPos> probe;
        private final Map<Key, Job> pending = new HashMap<>();
        private final Map<Key, Result> ready = new LinkedHashMap<>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, Result> eldest) { return size() > READY_MAX; }
        };
        private final Set<Object> deadOwners = Collections.newSetFromMap(new WeakHashMap<>());
        private final ConcurrentLinkedQueue<WarmItem> warm = new ConcurrentLinkedQueue<>();
        private volatile boolean shutdown;
        private volatile Thread worker;

        /** {@code probe} (tests only, may be null) runs on the worker before each candidate. */
        public Preparer(int cap, int extraRings, Consumer<ChunkPos> probe) {
            this.cap = cap;
            this.extraRings = extraRings;
            this.probe = probe;
            this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> {
                Thread t = new Thread(r, "MoreMinecrafts ST-01 search preparation");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                worker = t;
                return t;
            });
        }

        public synchronized Status request(Target target, BlockPos origin, int radius) {
            return request(target, origin, radius, 1);
        }

        /** As {@link #request(Target, BlockPos, int)}, continuing until at least {@code minEligible} eligible starts are found. */
        public synchronized Status request(Target target, BlockPos origin, int radius, int minEligible) {
            REQUESTS.incrementAndGet();
            if (shutdown) return reject();
            Key key = Key.of(target, origin, radius, minEligible);
            if (ready.get(key) != null) return Status.READY;
            if (pending.containsKey(key)) return Status.PENDING;
            if (pending.size() >= cap) return reject();
            deadOwners.remove(target.owner());
            Job job = new Job(key, target, origin.immutable());
            pending.put(key, job);
            try {
                executor.execute(() -> run(job));
            } catch (RejectedExecutionException e) {
                pending.remove(key);
                return reject();
            }
            QUEUED.incrementAndGet();
            return Status.QUEUED;
        }

        private static Status reject() {
            REJECTED.incrementAndGet();
            return Status.REJECTED;
        }

        public synchronized Status status(Target target, BlockPos origin, int radius) {
            Key key = Key.of(target, origin, radius);
            if (ready.containsKey(key)) return Status.READY;
            return pending.containsKey(key) ? Status.PENDING : Status.INERT;
        }

        public synchronized Optional<Result> result(Target target, BlockPos origin, int radius) {
            return result(target, origin, radius, 1);
        }

        public synchronized Optional<Result> result(Target target, BlockPos origin, int radius, int minEligible) {
            return Optional.ofNullable(ready.get(Key.of(target, origin, radius, minEligible)));
        }

        public synchronized Status status(Target target, BlockPos origin, int radius, int minEligible) {
            Key key = Key.of(target, origin, radius, minEligible);
            if (ready.containsKey(key)) return Status.READY;
            return pending.containsKey(key) ? Status.PENDING : Status.INERT;
        }

        public synchronized int pendingCount() { return pending.size(); }

        public boolean warmIdle() { return warm.isEmpty(); }

        public Thread workerThread() { return worker; }

        /** Cancels and forgets everything prepared for {@code owner} (a level being unloaded). */
        public synchronized void cancelOwner(Object owner) {
            deadOwners.add(owner);
            for (Job j : pending.values()) if (j.key.owner() == owner) j.cancelled = true;
            ready.keySet().removeIf(k -> k.owner() == owner);
            warm.removeIf(w -> w.key().owner() == owner);
        }

        /** Cancels all work and stops the worker; waits at most {@code awaitMillis}. Returns whether the worker stopped. */
        public boolean shutdown(long awaitMillis) {
            synchronized (this) {
                shutdown = true;
                for (Job j : pending.values()) j.cancelled = true;
                ready.clear();
                warm.clear();
            }
            executor.shutdownNow();
            try {
                return executor.awaitTermination(awaitMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        /** Blocks until no job is queued or running (tests and benchmarks only). */
        public boolean awaitIdle(long timeoutMillis) {
            long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            while (System.nanoTime() < end) {
                if (pendingCount() == 0) return true;
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return pendingCount() == 0;
        }

        /**
         * Server thread: feeds prepared candidates to {@code sink} for at most {@code budgetNanos}. A candidate whose decision
         * is not in the memo (evicted, or memo disabled) is dropped, never evaluated here. Returns the number fed.
         */
        public int pump(long budgetNanos, WarmSink sink) {
            if (warm.isEmpty()) return 0;
            long start = System.nanoTime();
            int fed = 0;
            WarmItem w;
            while (System.nanoTime() - start < budgetNanos && (w = warm.poll()) != null) {
                Target t = w.target();
                if (!St01DecisionMemo.contains(t.randomState(), t.generator(), t.seed(), t.height().getMinY(), t.height().getHeight(), w.chunk())) {
                    WARM_SKIPPED.incrementAndGet();
                    continue;
                }
                sink.warm(t, w.chunk());
                WARMED.incrementAndGet();
                fed++;
            }
            long spent = System.nanoTime() - start;
            PUMP_MAX_NANOS.accumulateAndGet(spent, Math::max);
            return fed;
        }

        private void run(Job job) {
            long cpu0 = cpuNanos(), wall0 = System.nanoTime();
            List<ChunkPos> candidates = new ArrayList<>();
            List<Eligible> eligible = new ArrayList<>();
            int[] eligibleRing = {-1};
            ChunkPos[] first = {null};
            Throwable failure = null;
            boolean cancelled = false;
            St01Cancellation.enter(() -> job.cancelled || shutdown);
            try {
                prepare(job, candidates, eligible, eligibleRing, first);
            } catch (CancellationException e) {
                cancelled = true;
            } catch (Throwable t) {
                failure = t;
            } finally {
                St01Cancellation.exit();
            }
            long cpu = cpuNanos() - cpu0;
            WORKER_CPU_NANOS.addAndGet(cpu);
            synchronized (this) {
                pending.remove(job.key);
                if (cancelled || job.cancelled || shutdown || deadOwners.contains(job.key.owner())) {
                    CANCELLED.incrementAndGet();
                } else if (failure != null) {
                    FAILED.incrementAndGet();
                    LOG.warn("ST-01 search preparation failed for {}; vanilla search is unaffected", job.key, failure);
                } else {
                    ready.put(job.key, new Result(candidates.size(), Optional.ofNullable(first[0]), eligibleRing[0], List.copyOf(eligible), cpu, System.nanoTime() - wall0));
                    for (ChunkPos c : candidates) warm.add(new WarmItem(job.key, job.target, c));
                    COMPLETED.incrementAndGet();
                }
            }
        }

        private void prepare(Job job, List<ChunkPos> candidates, List<Eligible> eligible, int[] eligibleRing, ChunkPos[] first) {
            Target t = job.target;
            RandomSpreadStructurePlacement placement = t.placement();
            Climate.Sampler sampler = t.randomState().createClimateSampler(SamplerContext.builder().enableCaches().build());
            int cx = SectionPos.blockToSectionCoord(job.origin.getX()), cz = SectionPos.blockToSectionCoord(job.origin.getZ());
            int radius = job.key.radius();
            // the candidate order of vanilla's ChunkGenerator.getNearestGeneratedStructure for a random-spread placement
            int wanted = job.key.minEligible();
            // stop extraRings rings after the ring holding the wanted-th eligible start
            for (int r = 0; r <= radius && (eligible.size() < wanted || r <= eligible.get(wanted - 1).ring() + extraRings); r++) {
                for (int k = -r; k <= r; k++) {
                    boolean edgeX = k == -r || k == r;
                    for (int l = -r; l <= r; l++) {
                        if (!edgeX && l != -r && l != r) continue;
                        St01Cancellation.check();
                        ChunkPos c = placement.getPotentialStructureChunk(t.seed(), cx + placement.spacing() * k, cz + placement.spacing() * l);
                        if (probe != null) probe.accept(c);
                        Structure.GenerationContext ctx = new Structure.GenerationContext(t.access(), t.generator(), t.biomeSource(), sampler,
                                t.randomState(), t.templates(), t.seed(), c, t.height(), h -> t.structure().value().biomes().contains(h));
                        Rotation rotation = Rotation.getRandom(ctx.random());
                        St01Candidate.Decision d = St01DecisionMemo.decide(ctx, rotation,
                                () -> St01Candidate.decide(new St01PyramidStructure.GeneratorTerrain(ctx), c, rotation, t.seed()));
                        candidates.add(c);
                        CANDIDATES.incrementAndGet();
                        if (d.result().eligible()) eligible.add(new Eligible(c, r));
                        if (d.result().eligible() && eligibleRing[0] < 0) {
                            eligibleRing[0] = r;
                            first[0] = c;
                        }
                    }
                }
            }
        }

        private static long cpuNanos() {
            return THREADS.isCurrentThreadCpuTimeSupported() ? THREADS.getCurrentThreadCpuTime() : 0L;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Server wiring

    private static volatile Preparer service;
    private static final Map<ServerLevel, Optional<Target>> TARGETS = new WeakHashMap<>();

    public static boolean disabled() {
        return Boolean.getBoolean("mm.st01.c6.disable") || !St01DecisionMemo.enabled();
    }

    static long warmBudgetNanos() {
        return TimeUnit.MICROSECONDS.toNanos(Math.max(0, Integer.getInteger("mm.st01.c6.warmBudgetUs", 2000)));
    }

    /** The running service, or null (disabled, or no server running). */
    public static Preparer service() { return service; }

    static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            St01MapTradeIndex.rebuild(server.registryAccess());
            if (!disabled()) service = new Preparer(QUEUE_CAP, Math.max(0, Integer.getInteger("mm.st01.c6.extraRings", 1)), null);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> St01MapTradeIndex.rebuild(server.registryAccess()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Preparer p = service;
            service = null;
            if (p != null && !p.shutdown(1000)) LOG.warn("ST-01 search preparation worker did not stop within 1 s");
            synchronized (TARGETS) {
                TARGETS.clear();
            }
            St01MapTradeIndex.clear();
        });
        ServerLevelEvents.UNLOAD.register((server, level) -> {
            Preparer p = service;
            if (p != null) p.cancelOwner(level);
            synchronized (TARGETS) {
                TARGETS.remove(level);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Preparer p = service;
            if (p != null) p.pump(warmBudgetNanos(), St01SearchPreparation::warmInLevel);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Villager v) onVillager(v, level);
        });
    }

    private static void warmInLevel(Target t, ChunkPos chunk) {
        ((ServerLevel) t.owner()).structureManager().checkStructurePresence(chunk, t.structure().value(), t.placement(), false);
    }

    /** The preparation snapshot for {@code level}, or empty if ST-01 has no single random-spread placement there. */
    public static Optional<Target> targetFor(ServerLevel level) {
        synchronized (TARGETS) {
            return TARGETS.computeIfAbsent(level, St01SearchPreparation::createTarget);
        }
    }

    private static Optional<Target> createTarget(ServerLevel level) {
        Optional<? extends Holder<Structure>> st01 = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID);
        if (st01.isEmpty() || !(st01.get().value() instanceof St01PyramidStructure)) return Optional.empty();
        List<StructurePlacement> placements = level.getChunkSource().getGeneratorState().getPlacementsForStructure(st01.get());
        if (placements.size() != 1 || !(placements.getFirst() instanceof RandomSpreadStructurePlacement placement)) return Optional.empty();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return Optional.of(new Target(level, level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                level.getStructureTemplateManager(), level.getSeed(), LevelHeightAccessor.create(level.getMinY(), level.getHeight()),
                st01.get(), placement));
    }

    /** Server thread: queue preparation of a {@code radius} ST-01 explorer-map search from {@code origin}. */
    public static Status requestFor(ServerLevel level, BlockPos origin, int radius) {
        return requestFor(level, origin, radius, 1);
    }

    /** Server thread: as {@link #requestFor(ServerLevel, BlockPos, int)}, until at least {@code minEligible} eligible starts are found. */
    public static Status requestFor(ServerLevel level, BlockPos origin, int radius, int minEligible) {
        Preparer p = service;
        if (p == null || disabled() || !level.getServer().isSameThread()) return Status.INERT;
        Optional<Target> target = targetFor(level);
        return target.isEmpty() ? Status.INERT : p.request(target.get(), origin, radius, minEligible);
    }

    /** Server thread: a villager's data changed or its trading screen opened. */
    public static void onVillagerChanged(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level) || villager.isRemoved()) return;
        // only villagers already in the level (not during construction or loading; ENTITY_LOAD covers loading)
        if (level.getEntity(villager.getUUID()) != villager) return;
        onVillager(villager, level);
    }

    private static void onVillager(Villager villager, ServerLevel level) {
        if (service == null) return;
        List<St01MapTradeIndex.Entry> entries = St01MapTradeIndex.entries();
        if (entries.isEmpty()) return;
        for (int radius : wantedRadii(entries, villager.getVillagerData(), ((St01MerchantOffersAccessor) villager).moreminecrafts$offers() == null)) {
            requestFor(level, villager.blockPosition(), radius);
        }
    }

    /**
     * Radii of the ST-01 explorer-map trades this villager can still generate: those of a higher level than its current one,
     * and those of its current level while its offers have not been generated yet.
     */
    public static List<Integer> wantedRadii(List<St01MapTradeIndex.Entry> entries, VillagerData data, boolean offersUngenerated) {
        Optional<ResourceKey<VillagerProfession>> profession = data.profession().unwrapKey();
        if (profession.isEmpty()) return List.of();
        List<Integer> out = new ArrayList<>();
        for (St01MapTradeIndex.Entry e : entries) {
            if (!e.profession().equals(profession.get())) continue;
            if ((e.level() > data.level() || (e.level() == data.level() && offersUngenerated)) && !out.contains(e.radius())) out.add(e.radius());
        }
        return out;
    }
}
