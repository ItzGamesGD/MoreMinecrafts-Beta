package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01MapTradeIndex;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-D6 benchmark of the production C6 search preparation on a ticking dedicated server (inert unless
 * {@code -Dmm.st01.d6.out} is set; validation only). Scenarios ({@code -Dmm.st01.d6.scenario}):
 * <ul>
 *   <li>{@code none}: no preparation, the unchanged vanilla explorer-map call (baseline = PR #281);</li>
 *   <li>{@code ready}: request, wait until the worker and the server-thread warm-up are idle, then the vanilla call;</li>
 *   <li>{@code notready}: request, then the vanilla call on the next tick while the worker is still running;</li>
 *   <li>{@code multi}: nine requests from nine regions in one tick (queue cap 8), retry of the rejected one, nine calls;</li>
 *   <li>{@code cancel}: a long request, then a graceful server stop while it runs;</li>
 *   <li>{@code villager} / {@code villager-notready}: a cartographer is spawned (the bench data pack makes level 2 offer the
 *       ST-01 map), the C6 triggers fire, then the vanilla level-up runs ({@code increaseMerchantCareer}) after preparation /
 *       on the next tick;</li>
 *   <li>{@code villager-reload}: a world saved by {@code villager} is reopened; the reloaded villager's offers are inspected.</li>
 * </ul>
 */
public final class St01D6PrepareBench implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-d6-bench");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private enum Phase { WARMUP, START, WAIT, ACT, GAP, REPEAT, DONE }

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.d6.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> new Run(server, out).start());
    }

    private static final class Run {
        final MinecraftServer server;
        final String out;
        final ServerLevel level;
        final Holder<Structure> st01;
        final String scenario = System.getProperty("mm.st01.d6.scenario", "ready");
        final int radius = Integer.getInteger("mm.st01.d6.radius", 20);
        final long cancelAfterMs = Long.getLong("mm.st01.d6.cancelAfterMs", 400);
        final boolean save = Boolean.getBoolean("mm.st01.d6.save");
        final List<BlockPos> origins = new ArrayList<>();
        final Map<String, Object> result = new LinkedHashMap<>();
        final Map<String, Double> phaseMax = new LinkedHashMap<>();
        final Map<String, Integer> phaseTicks = new LinkedHashMap<>();
        final List<Map<String, Object>> calls = new ArrayList<>();
        final Map<String, Long> base = new LinkedHashMap<>();
        Phase phase = Phase.WARMUP;
        String label = "WARMUP";
        int phaseTick, actIndex;
        long tickStart, phaseStart, heapBefore;
        boolean finished;
        Villager villager;
        volatile long workerDiedNanos;

        Run(MinecraftServer server, String out) {
            this.server = server;
            this.out = out;
            this.level = server.overworld();
            this.st01 = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID).orElseThrow();
            int[][] offsets = {{0, 0}, {600, 0}, {0, 600}, {-600, -600}, {1200, 1200}, {2000, -2000}, {-2000, 2000}, {3000, 0}, {0, -3000}};
            int n = scenario.equals("multi") ? 9 : 1;
            for (int i = 0; i < n; i++) origins.add(new BlockPos(offsets[i][0], 64, offsets[i][1]));
            if (scenario.equals("cancel")) { origins.clear(); origins.add(new BlockPos(1_000_000, 64, 1_000_000)); }
        }

        void start() {
            result.put("seed", level.getSeed());
            result.put("scenario", scenario);
            result.put("radius", radius);
            result.put("c6_service_running", St01SearchPreparation.service() != null);
            result.put("map_trade_index", St01MapTradeIndex.entries().toString());
            result.put("warm_budget_us", Integer.getInteger("mm.st01.c6.warmBudgetUs", 2000));
            result.put("requests_before_start", St01SearchPreparation.REQUESTS.get());
            snapshot();
            ServerTickEvents.START_SERVER_TICK.register(s -> tickStart = System.nanoTime());
            ServerTickEvents.END_SERVER_TICK.register(s -> {
                if (finished) return;
                try { onTickEnd(); } catch (Throwable t) { LOG.error("d6 bench failed", t); result.put("error", t.toString()); finish(); }
            });
            ServerLifecycleEvents.SERVER_STOPPED.register(s -> { if (!finished) { finished = true; write(); } });
        }

        void snapshot() {
            base.put("requests", St01SearchPreparation.REQUESTS.get());
            base.put("queued", St01SearchPreparation.QUEUED.get());
            base.put("rejected", St01SearchPreparation.REJECTED.get());
            base.put("completed", St01SearchPreparation.COMPLETED.get());
            base.put("cancelled", St01SearchPreparation.CANCELLED.get());
            base.put("failed", St01SearchPreparation.FAILED.get());
            base.put("candidates", St01SearchPreparation.CANDIDATES.get());
            base.put("warmed", St01SearchPreparation.WARMED.get());
            base.put("warm_skipped", St01SearchPreparation.WARM_SKIPPED.get());
            base.put("worker_cpu_ns", St01SearchPreparation.WORKER_CPU_NANOS.get());
            base.put("memo_misses", St01DecisionMemo.MISSES.get());
            base.put("memo_hits", St01DecisionMemo.HITS.get());
        }

        Map<String, Object> counters() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("requests", St01SearchPreparation.REQUESTS.get() - base.get("requests"));
            m.put("queued", St01SearchPreparation.QUEUED.get() - base.get("queued"));
            m.put("rejected", St01SearchPreparation.REJECTED.get() - base.get("rejected"));
            m.put("completed", St01SearchPreparation.COMPLETED.get() - base.get("completed"));
            m.put("cancelled", St01SearchPreparation.CANCELLED.get() - base.get("cancelled"));
            m.put("failed", St01SearchPreparation.FAILED.get() - base.get("failed"));
            m.put("candidates_prepared", St01SearchPreparation.CANDIDATES.get() - base.get("candidates"));
            m.put("warmed", St01SearchPreparation.WARMED.get() - base.get("warmed"));
            m.put("warm_skipped", St01SearchPreparation.WARM_SKIPPED.get() - base.get("warm_skipped"));
            m.put("worker_cpu_ms", (St01SearchPreparation.WORKER_CPU_NANOS.get() - base.get("worker_cpu_ns")) / 1e6);
            m.put("max_warm_pass_ms_since_start", St01SearchPreparation.PUMP_MAX_NANOS.get() / 1e6);
            return m;
        }

        void next(Phase p) {
            phase = p;
            phaseTick = 0;
            phaseStart = System.nanoTime();
        }

        boolean preparationIdle() {
            var s = St01SearchPreparation.service();
            return s == null || (s.pendingCount() == 0 && s.warmIdle());
        }

        void onTickEnd() throws Exception {
            phaseTick++;
            label = phase.name();
            switch (phase) {
                case WARMUP -> {
                    if (phaseTick == 30) { System.gc(); heapBefore = usedHeap(); label = "HEAP_GC"; }
                    if (phaseTick >= 40) next(Phase.START);
                }
                case START -> {
                    snapshot();
                    if (scenario.equals("villager-reload")) { inspectReloadedVillager(); next(Phase.GAP); break; }
                    if (scenario.startsWith("villager")) spawnCartographer();
                    else if (!scenario.equals("none")) {
                        List<String> statuses = new ArrayList<>();
                        for (BlockPos o : origins) statuses.add(St01SearchPreparation.requestFor(level, o, scenario.equals("cancel") ? 100 : radius).name());
                        result.put("request_statuses", statuses);
                    }
                    result.put("start_tick_ms", (System.nanoTime() - tickStart) / 1e6);
                    next(scenario.equals("none") ? Phase.ACT : scenario.equals("notready") ? Phase.ACT : Phase.WAIT);
                }
                case WAIT -> {
                    if (scenario.equals("cancel") && (System.nanoTime() - phaseStart) / 1e6 >= cancelAfterMs) {
                        var s = St01SearchPreparation.service();
                        Thread worker = s == null ? null : s.workerThread();
                        result.put("cancel_requested_after_ms", (System.nanoTime() - phaseStart) / 1e6);
                        result.put("counters_at_cancel", counters());
                        long t0 = System.nanoTime();
                        if (worker != null) {
                            Thread watcher = new Thread(() -> {
                                try { worker.join(10_000); } catch (InterruptedException ignored) {}
                                workerDiedNanos = worker.isAlive() ? -1 : System.nanoTime() - t0;
                            });
                            watcher.setDaemon(true);
                            watcher.start();
                        }
                        server.halt(false);
                        phase = Phase.DONE;
                        break;
                    }
                    if (villager != null && !result.containsKey("villager_visible_after_ticks")) {
                        // a fresh entity joins the level (and fires ENTITY_LOAD) once its section is entity-ticking
                        if (level.getEntity(villager.getUUID()) != villager) { if (phaseTick > 200) throw new IllegalStateException("villager never joined"); break; }
                        result.put("villager_visible_after_ticks", phaseTick);
                        result.put("counters_when_visible", counters());
                        if (scenario.equals("villager-notready")) { next(Phase.ACT); break; }
                    }
                    if (preparationIdle()) {
                        if (scenario.equals("multi") && !result.containsKey("retry_status")) {
                            result.put("retry_status", St01SearchPreparation.requestFor(level, origins.getLast(), radius).name());
                            break;
                        }
                        result.put("preparation_wall_ms", (System.nanoTime() - phaseStart) / 1e6);
                        result.put("counters_after_preparation", counters());
                        System.gc();
                        label = "HEAP_GC";
                        result.put("retained_heap_delta_mb", (usedHeap() - heapBefore) / 1048576.0);
                        result.put("memo_size", St01DecisionMemo.size(level.getChunkSource().randomState()));
                        next(Phase.ACT);
                    }
                }
                case ACT -> {
                    if (scenario.startsWith("villager")) {
                        levelUp();
                        next(Phase.GAP);
                    } else {
                        long misses = St01DecisionMemo.MISSES.get();
                        Map<String, Object> m = St01D3Probe.mapCall(level, HolderSet.direct(st01), st01.value(), origins.get(actIndex), radius, "call-" + actIndex);
                        m.put("server_thread_memo_misses", St01DecisionMemo.MISSES.get() - misses);
                        m.put("tick_ms_including_call", (System.nanoTime() - tickStart) / 1e6);
                        calls.add(m);
                        if (++actIndex >= origins.size()) next(Phase.GAP);
                    }
                }
                case GAP -> {
                    if (phaseTick >= 20) {
                        if (scenario.startsWith("villager")) { next(Phase.DONE); finish(); }
                        else next(Phase.REPEAT);
                    }
                }
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
            phaseMax.merge(label, ms, Math::max);
            phaseTicks.merge(label, 1, Integer::sum);
        }

        void spawnCartographer() {
            level.setChunkForced(0, 0, true); // the villager must be in a loaded chunk to join the level
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(0, 0, 0));
            Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            v.setPos(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
            v.setNoAi(true);
            v.setPersistenceRequired();
            level.addFreshEntity(v);
            v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
                    .getOrThrow(VillagerProfession.CARTOGRAPHER)));
            v.setVillagerXp(1);
            v.getOffers(); // level-1 trades, as when a player first opens the trading screen
            villager = v;
            result.put("villager_pos", v.blockPosition().toShortString());
            result.put("level1_offers", v.getOffers().size());
            result.put("counters_after_spawn", counters());
        }

        void levelUp() throws Exception {
            long misses = St01DecisionMemo.MISSES.get();
            int before = villager.getOffers().size();
            villager.setVillagerXp(10);
            Method m = Villager.class.getDeclaredMethod("increaseMerchantCareer", ServerLevel.class);
            m.setAccessible(true);
            long t0 = System.nanoTime();
            m.invoke(villager, level);
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("level_up_ms", (System.nanoTime() - t0) / 1e6);
            c.put("tick_ms_including_level_up", (System.nanoTime() - tickStart) / 1e6);
            c.put("server_thread_memo_misses", St01DecisionMemo.MISSES.get() - misses);
            c.put("villager_level", villager.getVillagerData().level());
            c.put("offers_before", before);
            c.put("offers_after", villager.getOffers().size());
            c.putAll(describeMapOffers(villager));
            calls.add(c);
        }

        static Map<String, Object> describeMapOffers(Villager v) {
            Map<String, Object> m = new LinkedHashMap<>();
            List<String> maps = new ArrayList<>();
            for (MerchantOffer o : v.getOffers()) {
                if (!o.getResult().is(Items.FILLED_MAP)) continue;
                var decorations = o.getResult().get(DataComponents.MAP_DECORATIONS);
                if (decorations == null) { maps.add("map without decorations"); continue; }
                decorations.decorations().values().forEach(e -> maps.add("target chunk " + (((int) Math.floor(e.x())) >> 4) + "," + (((int) Math.floor(e.z())) >> 4)));
            }
            m.put("map_offers", maps);
            return m;
        }

        void inspectReloadedVillager() {
            List<? extends Villager> vs = level.getEntities(EntityTypes.VILLAGER, e -> true);
            result.put("reloaded_villagers", vs.size());
            if (vs.isEmpty()) return;
            Villager v = vs.getFirst();
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("villager_level", v.getVillagerData().level());
            c.put("offers", v.getOffers().size());
            c.putAll(describeMapOffers(v));
            c.put("c6_requests_since_server_start", St01SearchPreparation.REQUESTS.get() - (long) result.get("requests_before_start"));
            calls.add(c);
        }

        static long usedHeap() {
            Runtime r = Runtime.getRuntime();
            return r.totalMemory() - r.freeMemory();
        }

        void finish() {
            finished = true;
            if (save) server.saveEverything(true, true, true);
            write();
            Runtime.getRuntime().halt(0);
        }

        void write() {
            result.put("calls", calls);
            result.put("counters_final", counters());
            result.put("phase_max_tick_ms", phaseMax);
            result.put("phase_ticks", phaseTicks);
            if (scenario.equals("cancel")) result.put("worker_stopped_after_halt_ms", workerDiedNanos == 0 ? null : workerDiedNanos / 1e6);
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
