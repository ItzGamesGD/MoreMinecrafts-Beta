package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-D5 C6 feasibility bench (inert unless {@code -Dmm.st01.d5.out} is set; experiment only, not production C6).
 *
 * <p>Prototype pipeline, all phases recorded tick by tick on a ticking dedicated server:
 * <ol>
 *   <li><b>prepare</b> (one low-priority background thread, de-duplicated by origin region and radius, cancellable): the
 *       vanilla locator's ring order is evaluated through the production C1+C2+C4 path with vanilla-equivalent
 *       generation contexts; only immutable/concurrent worldgen state is read (no StructureCheck, chunk or level state);</li>
 *   <li><b>validate</b> ({@code mm.st01.d5.validate=offthread}): the target chunk is requested at {@code STRUCTURE_STARTS}
 *       through {@code ServerChunkCache.getChunkFuture} from the background thread (vanilla's off-thread path, which only
 *       schedules on the server thread);</li>
 *   <li><b>warm</b> ({@code mm.st01.d5.warm=K}): on the server thread, K candidates per tick through the vanilla
 *       {@code StructureManager.checkStructurePresence}, filling vanilla's own StructureCheck cache from memo hits;</li>
 *   <li><b>issue</b>: the unchanged vanilla explorer-map calls ({@code findNearestMapStructure} + map creation), timed.</li>
 * </ol>
 * {@code mm.st01.d5.split=true} times the target chunk's {@code STRUCTURE_STARTS} generation separately before the vanilla
 * call. {@code mm.st01.d5.origins=N} issues N cartographer-like requests in the same tick. {@code mm.st01.d5.cancelAfterMs}
 * stops the server gracefully during preparation and records cancellation latency.
 */
public final class St01D5C6Bench implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-d5-c6bench");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    private enum Phase { WARMUP, PREPARE, VALIDATE, WARM, SPLIT, ISSUE, GAP, REPEAT, DONE }

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.d5.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> new Run(server, out).start());
    }

    /** Result of preparing one (origin region, radius) request. */
    record Prepared(List<ChunkPos> candidates, ChunkPos target, int eligibleRing, double cpuMs, double wallMs) {}

    /** Experimental preparer: one background thread, de-duplication, bounded queue, cooperative cancellation. */
    static final class Preparer {
        final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "st01-c6-prepare");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        final Map<String, CompletableFuture<Prepared>> inflight = new ConcurrentHashMap<>();
        final int maxPending = Integer.getInteger("mm.st01.d5.maxPending", 8);
        volatile boolean cancelled;
        volatile long cancelObservedNanos;
        int rejected;

        CompletableFuture<Prepared> request(ServerLevel level, Holder<Structure> st01, BlockPos origin, int radius, RandomSpreadStructurePlacement placement) {
            int cx = SectionPos.blockToSectionCoord(origin.getX()), cz = SectionPos.blockToSectionCoord(origin.getZ());
            String key = Math.floorDiv(cx, placement.spacing()) + "," + Math.floorDiv(cz, placement.spacing()) + "/" + radius;
            CompletableFuture<Prepared> existing = inflight.get(key);
            if (existing != null) return existing;
            if (inflight.values().stream().filter(f -> !f.isDone()).count() >= maxPending) { rejected++; return null; }
            return inflight.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> prepare(level, st01, cx, cz, radius, placement), exec));
        }

        Prepared prepare(ServerLevel level, Holder<Structure> st01, int cx, int cz, int radius, RandomSpreadStructurePlacement placement) {
            long cpu0 = THREADS.getCurrentThreadCpuTime(), t0 = System.nanoTime();
            var generator = level.getChunkSource().getGenerator();
            RandomState randomState = level.getChunkSource().randomState();
            var sampler = randomState.createClimateSampler(SamplerContext.builder().enableCaches().build());
            long seed = level.getSeed();
            List<ChunkPos> list = new ArrayList<>();
            ChunkPos target = null;
            int eligibleRing = -1;
            // vanilla ring order; one extra ring after the first eligible one tolerates a villager moving to a neighbouring region
            for (int r = 0; r <= radius && (eligibleRing < 0 || r <= eligibleRing + 1); r++) {
                for (int k = -r; k <= r; k++) {
                    boolean edgeX = k == -r || k == r;
                    for (int l = -r; l <= r; l++) {
                        if (!edgeX && l != -r && l != r) continue;
                        if (cancelled) { cancelObservedNanos = System.nanoTime(); throw new CancellationException("server stopping"); }
                        ChunkPos c = placement.getPotentialStructureChunk(seed, cx + placement.spacing() * k, cz + placement.spacing() * l);
                        Structure.GenerationContext ctx = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
                                sampler, randomState, level.getStructureTemplateManager(), seed, c, level, h -> st01.value().biomes().contains(h));
                        Rotation rotation = Rotation.getRandom(ctx.random());
                        St01Candidate.Decision d = St01DecisionMemo.decide(ctx, rotation,
                                () -> St01Candidate.decide(new St01PyramidStructure.GeneratorTerrain(ctx), c, rotation, seed));
                        list.add(c);
                        if (d.result().eligible() && eligibleRing < 0) { eligibleRing = r; target = c; }
                    }
                }
            }
            return new Prepared(list, target, eligibleRing, (THREADS.getCurrentThreadCpuTime() - cpu0) / 1e6, (System.nanoTime() - t0) / 1e6);
        }

        void shutdown() {
            cancelled = true;
            exec.shutdownNow();
        }
    }

    private static final class Run {
        final MinecraftServer server;
        final String out;
        final ServerLevel level;
        final Holder<Structure> st01;
        final RandomSpreadStructurePlacement placement;
        final int radius = Integer.getInteger("mm.st01.d5.radius", 20);
        final boolean prepare = Boolean.parseBoolean(System.getProperty("mm.st01.d5.prepare", "true"));
        final String validate = System.getProperty("mm.st01.d5.validate", "none");
        final int warm = Integer.getInteger("mm.st01.d5.warm", 0);
        final boolean split = Boolean.getBoolean("mm.st01.d5.split");
        final long cancelAfterMs = Long.getLong("mm.st01.d5.cancelAfterMs", -1);
        final List<BlockPos> origins = new ArrayList<>();
        final Map<String, Object> result = new LinkedHashMap<>();
        final List<Double> tickMs = new ArrayList<>();
        final Map<String, Double> phaseMax = new LinkedHashMap<>();
        final Map<String, Integer> phaseTicks = new LinkedHashMap<>();
        final Preparer preparer = new Preparer();
        final List<CompletableFuture<Prepared>> jobs = new ArrayList<>();
        final List<ChunkPos> warmList = new ArrayList<>();
        final List<Map<String, Object>> calls = new ArrayList<>();
        CompletableFuture<List<String>> validation;
        Phase phase = Phase.WARMUP;
        int phaseTick, warmIndex, issueIndex;
        long tickStart, phaseStart, heapBefore;
        boolean finished;
        String tickLabel;

        Run(MinecraftServer server, String out) {
            this.server = server;
            this.out = out;
            this.level = server.overworld();
            this.st01 = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID).orElseThrow();
            this.placement = (RandomSpreadStructurePlacement) level.getChunkSource().getGeneratorState().getPlacementsForStructure(st01).getFirst();
            int n = Integer.getInteger("mm.st01.d5.origins", 1);
            int[][] offsets = {{0, 0}, {40, 40}, {-60, 30}, {600, 0}, {0, 600}, {-600, -600}, {1200, 1200}, {2000, -2000}};
            for (int i = 0; i < n; i++) origins.add(new BlockPos(offsets[i % offsets.length][0], 64, offsets[i % offsets.length][1]));
        }

        void start() {
            result.put("seed", level.getSeed());
            result.put("radius", radius);
            result.put("prepare", prepare);
            result.put("validate", validate);
            result.put("warm_per_tick", warm);
            result.put("split", split);
            result.put("origins", origins.size());
            result.put("memo_size_at_start", St01DecisionMemo.size(level.getChunkSource().randomState()));
            ServerTickEvents.START_SERVER_TICK.register(s -> tickStart = System.nanoTime());
            ServerTickEvents.END_SERVER_TICK.register(s -> {
                if (finished) return;
                try { onTickEnd(); } catch (Throwable t) { LOG.error("c6 bench failed", t); result.put("error", t.toString()); finish(); }
            });
            ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
                long t = System.nanoTime();
                preparer.shutdown();
                try { preparer.exec.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
                result.put("stop_requested_to_worker_terminated_ms", (System.nanoTime() - t) / 1e6);
                result.put("worker_observed_cancel", preparer.cancelObservedNanos != 0);
                result.put("cancel_observe_latency_ms", preparer.cancelObservedNanos == 0 ? null : (preparer.cancelObservedNanos - t) / 1e6);
                result.put("jobs_cancelled", jobs.stream().filter(CompletableFuture::isCompletedExceptionally).count());
            });
            ServerLifecycleEvents.SERVER_STOPPED.register(s -> { if (!finished) { finished = true; write(); } });
        }

        void next(Phase p) {
            phase = p;
            phaseTick = 0;
            phaseStart = System.nanoTime();
        }

        void onTickEnd() {
            phaseTick++;
            tickLabel = phase.name();
            switch (phase) {
                case WARMUP -> {
                    if (phaseTick == 30) { System.gc(); heapBefore = usedHeap(); tickLabel = "HEAP_GC"; }
                    if (phaseTick >= 40) {
                        if (prepare) {
                            for (BlockPos o : origins) jobs.add(preparer.request(level, st01, o, radius, placement));
                            result.put("jobs_created", jobs.stream().filter(j -> j != null).distinct().count());
                            result.put("requests_rejected_by_queue_cap", preparer.rejected);
                            next(Phase.PREPARE);
                        } else next(Phase.ISSUE);
                    }
                }
                case PREPARE -> {
                    if (cancelAfterMs >= 0 && (System.nanoTime() - phaseStart) / 1e6 >= cancelAfterMs) {
                        result.put("cancel_requested_after_ms", (System.nanoTime() - phaseStart) / 1e6);
                        server.halt(false); // graceful stop: SERVER_STOPPING cancels the background work
                        phase = Phase.DONE;
                    } else if (jobs.stream().filter(j -> j != null).allMatch(CompletableFuture::isDone)) {
                        List<Map<String, Object>> prep = new ArrayList<>();
                        for (CompletableFuture<Prepared> j : jobs.stream().filter(j -> j != null).distinct().toList()) {
                            Prepared p = j.join();
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("candidates", p.candidates().size());
                            m.put("eligible_ring", p.eligibleRing());
                            m.put("target", p.target() == null ? null : p.target().x() + "," + p.target().z());
                            m.put("worker_cpu_ms", p.cpuMs());
                            m.put("worker_wall_ms", p.wallMs());
                            prep.add(m);
                            for (ChunkPos c : p.candidates()) if (!warmList.contains(c)) warmList.add(c);
                        }
                        result.put("preparation", prep);
                        result.put("preparation_phase_wall_ms", (System.nanoTime() - phaseStart) / 1e6);
                        System.gc();
                        tickLabel = "HEAP_GC";
                        result.put("retained_heap_delta_mb", (usedHeap() - heapBefore) / 1048576.0);
                        result.put("memo_size_after_prepare", St01DecisionMemo.size(level.getChunkSource().randomState()));
                        if (validate.equals("offthread")) {
                            List<ChunkPos> targets = jobs.stream().filter(j -> j != null).distinct().map(CompletableFuture::join).map(Prepared::target).filter(t -> t != null).distinct().toList();
                            validation = CompletableFuture.supplyAsync(() -> validateOffThread(targets), preparer.exec);
                            next(Phase.VALIDATE);
                        } else next(warm > 0 ? Phase.WARM : (split ? Phase.SPLIT : Phase.ISSUE));
                    }
                }
                case VALIDATE -> {
                    if (validation.isDone()) {
                        result.put("validation", validation.join());
                        result.put("validation_phase_wall_ms", (System.nanoTime() - phaseStart) / 1e6);
                        next(warm > 0 ? Phase.WARM : (split ? Phase.SPLIT : Phase.ISSUE));
                    }
                }
                case WARM -> {
                    long t = System.nanoTime();
                    for (int i = 0; i < warm && warmIndex < warmList.size(); i++, warmIndex++)
                        level.structureManager().checkStructurePresence(warmList.get(warmIndex), st01.value(), placement, false);
                    phaseMax.merge("warm_work_ms", (System.nanoTime() - t) / 1e6, Math::max);
                    if (warmIndex >= warmList.size()) {
                        result.put("warm_candidates", warmList.size());
                        result.put("warm_phase_wall_ms", (System.nanoTime() - phaseStart) / 1e6);
                        next(split ? Phase.SPLIT : Phase.ISSUE);
                    }
                }
                case SPLIT -> {
                    ChunkPos target = jobs.isEmpty() || jobs.getFirst() == null ? null : jobs.getFirst().join().target();
                    if (target != null) {
                        long t = System.nanoTime();
                        level.getChunk(target.x(), target.z(), ChunkStatus.STRUCTURE_STARTS);
                        result.put("split_target_structure_starts_ms", (System.nanoTime() - t) / 1e6);
                    }
                    next(Phase.ISSUE);
                }
                case ISSUE -> {
                    BlockPos o = origins.get(issueIndex);
                    Map<String, Object> m = St01D3Probe.mapCall(level, HolderSet.direct(st01), st01.value(), o, radius, "issue-" + issueIndex);
                    m.put("tick_ms_including_call", (System.nanoTime() - tickStart) / 1e6);
                    calls.add(m);
                    if (++issueIndex >= origins.size()) next(Phase.GAP);
                }
                case GAP -> { if (phaseTick >= 20) next(Phase.REPEAT); }
                case REPEAT -> {
                    Map<String, Object> m = St01D3Probe.mapCall(level, HolderSet.direct(st01), st01.value(), origins.getFirst(), radius, "repeat");
                    m.put("tick_ms_including_call", (System.nanoTime() - tickStart) / 1e6);
                    calls.add(m);
                    next(Phase.DONE);
                    finish();
                }
                case DONE -> {}
            }
            double ms = (System.nanoTime() - tickStart) / 1e6;
            tickMs.add(ms);
            String key = tickLabel;
            phaseMax.merge(key, ms, Math::max);
            phaseTicks.merge(key, 1, Integer::sum);
        }

        List<String> validateOffThread(List<ChunkPos> targets) {
            List<String> out = new ArrayList<>();
            for (ChunkPos t : targets) {
                long t0 = System.nanoTime();
                String status;
                try {
                    ChunkResult<?> r = level.getChunkSource().getChunkFuture(t.x(), t.z(), ChunkStatus.STRUCTURE_STARTS, true).get(30, TimeUnit.SECONDS);
                    status = r.isSuccess() ? "ok" : "error";
                } catch (Exception e) {
                    status = "exception " + e.getClass().getSimpleName();
                }
                out.add(t.x() + "," + t.z() + " " + status + " " + String.format("%.1f", (System.nanoTime() - t0) / 1e6) + " ms");
            }
            return out;
        }

        static long usedHeap() {
            Runtime r = Runtime.getRuntime();
            return r.totalMemory() - r.freeMemory();
        }

        void finish() {
            finished = true;
            write();
            if (Boolean.getBoolean("mm.st01.d5.save")) server.saveEverything(true, true, true);
            Runtime.getRuntime().halt(0);
        }

        void write() {
            result.put("calls", calls);
            result.put("phase_max_tick_ms", phaseMax);
            result.put("phase_ticks", phaseTicks);
            double max = 0;
            for (var e : phaseMax.entrySet()) if (!e.getKey().equals("HEAP_GC") && !e.getKey().equals("WARMUP") && !e.getKey().endsWith("_work_ms")) max = Math.max(max, e.getValue());
            result.put("max_tick_ms_excluding_warmup_and_gc", max);
            result.put("ticks", tickMs.size());
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
            } catch (Throwable t) {
                LOG.error("could not write", t);
            }
        }
    }
}
