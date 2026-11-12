package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01FastTerrain;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-D4 tick benchmark (inert unless {@code -Dmm.st01.d4.out} is set; validation only, not production code).
 *
 * <p>Runs on a fresh dedicated server with ticks running. The explorer-map call (vanilla {@code findNearestMapStructure}
 * plus the vanilla map-creation calls, as in {@code St01D3Probe}) is issued inside {@code END_SERVER_TICK}, and every
 * tick's duration is recorded, so the reported stall is the actual longest tick. {@code -Dmm.st01.d4.prepare=worker}
 * first fills the C4 memo on one background thread, in the vanilla locator's ring order, from vanilla-equivalent
 * generation contexts (own climate sampler; no StructureCheck, chunk or level state is touched), while the server keeps
 * ticking; the vanilla call is then issued unchanged. This is the C6 feasibility experiment, not a C6 implementation.
 */
public final class St01D4TickBench implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-d4-tickbench");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private enum Phase { WARMUP, PREPARE, COLD, GAP1, WARM, GAP2, DONE }

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.d4.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> new Run(server, out).start());
    }

    private static final class Run {
        final MinecraftServer server;
        final String out;
        final ServerLevel level;
        final Holder<Structure> st01;
        final int radius = Integer.getInteger("mm.st01.d4.radius", 20);
        final String prepare = System.getProperty("mm.st01.d4.prepare", "none");
        final BlockPos origin = new BlockPos(0, 64, 0);
        final Map<String, Object> result = new LinkedHashMap<>();
        final List<Double> tickMs = new ArrayList<>();
        Phase phase = Phase.WARMUP;
        int tick, phaseTick;
        long tickStart;
        CompletableFuture<Map<String, Object>> preparation;
        long prepareStart;
        double prepareMaxTick;
        int prepareTicks;

        Run(MinecraftServer server, String out) {
            this.server = server;
            this.out = out;
            this.level = server.overworld();
            this.st01 = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID).orElseThrow();
        }

        void start() {
            result.put("seed", level.getSeed());
            result.put("radius", radius);
            result.put("prepare", prepare);
            result.put("legacy_forced", St01FastTerrain.legacyForced());
            result.put("memo_enabled", St01DecisionMemo.enabled());
            result.put("available_processors", Runtime.getRuntime().availableProcessors());
            ServerTickEvents.START_SERVER_TICK.register(s -> tickStart = System.nanoTime());
            ServerTickEvents.END_SERVER_TICK.register(s -> {
                try { onTickEnd(); } catch (Throwable t) { LOG.error("tick bench failed", t); result.put("error", t.toString()); finish(); }
            });
        }

        void onTickEnd() {
            tick++; phaseTick++;
            switch (phase) {
                case WARMUP -> { if (phaseTick >= 40) next(prepare.equals("worker") ? Phase.PREPARE : Phase.COLD); }
                case PREPARE -> {
                    if (preparation == null) { prepareStart = System.nanoTime(); preparation = CompletableFuture.supplyAsync(this::prepare); }
                    else if (preparation.isDone()) {
                        Map<String, Object> p = preparation.join();
                        p.put("wall_ms", (System.nanoTime() - prepareStart) / 1e6);
                        p.put("ticks_while_preparing", prepareTicks);
                        p.put("max_tick_while_preparing_ms", prepareMaxTick);
                        result.put("preparation", p);
                        next(Phase.COLD);
                    }
                }
                case COLD -> { result.put("cold", call("cold")); next(Phase.GAP1); }
                case GAP1 -> { if (phaseTick >= 20) next(Phase.WARM); }
                case WARM -> { result.put("warm", call("warm")); next(Phase.GAP2); }
                case GAP2 -> { if (phaseTick >= 20) { next(Phase.DONE); finish(); } }
                case DONE -> {}
            }
            double ms = (System.nanoTime() - tickStart) / 1e6;
            tickMs.add(ms);
            if (phase == Phase.PREPARE && preparation != null) { prepareTicks++; prepareMaxTick = Math.max(prepareMaxTick, ms); }
        }

        void next(Phase p) { phase = p; phaseTick = 0; }

        Map<String, Object> call(String label) {
            long hits = St01DecisionMemo.HITS.get(), misses = St01DecisionMemo.MISSES.get();
            Map<String, Object> m = St01D3Probe.mapCall(level, HolderSet.direct(st01), st01.value(), origin, radius, label);
            m.put("tick_ms_including_call", (System.nanoTime() - tickStart) / 1e6);
            m.put("memo_hits", St01DecisionMemo.HITS.get() - hits);
            m.put("memo_misses", St01DecisionMemo.MISSES.get() - misses);
            return m;
        }

        /** Background thread: vanilla ring order around the origin chunk; stops after the first ring that holds an eligible start. */
        Map<String, Object> prepare() {
            var generator = level.getChunkSource().getGenerator();
            RandomState randomState = level.getChunkSource().randomState();
            var sampler = randomState.createClimateSampler(SamplerContext.builder().enableCaches().build());
            RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) level.getChunkSource().getGeneratorState()
                    .getPlacementsForStructure(st01).getFirst();
            long seed = level.getSeed();
            int cx = SectionPos.blockToSectionCoord(origin.getX()), cz = SectionPos.blockToSectionCoord(origin.getZ());
            int evaluated = 0, eligibleRing = -1;
            for (int r = 0; r <= radius && eligibleRing < 0; r++) {
                for (int k = -r; k <= r; k++) {
                    boolean edgeX = k == -r || k == r;
                    for (int l = -r; l <= r; l++) {
                        if (!edgeX && l != -r && l != r) continue;
                        ChunkPos c = placement.getPotentialStructureChunk(seed, cx + placement.spacing() * k, cz + placement.spacing() * l);
                        Structure.GenerationContext ctx = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
                                sampler, randomState, level.getStructureTemplateManager(), seed, c, level, h -> st01.value().biomes().contains(h));
                        Rotation rotation = Rotation.getRandom(ctx.random());
                        St01Candidate.Decision d = St01DecisionMemo.decide(ctx, rotation,
                                () -> St01Candidate.decide(new St01PyramidStructure.GeneratorTerrain(ctx), c, rotation, seed));
                        evaluated++;
                        if (d.result().eligible()) eligibleRing = r;
                    }
                }
            }
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("candidates_prepared", evaluated);
            p.put("eligible_ring", eligibleRing);
            p.put("memo_size", St01DecisionMemo.size(randomState));
            return p;
        }

        void finish() {
            double max = 0;
            List<Double> steady = new ArrayList<>();
            for (double t : tickMs) max = Math.max(max, t);
            result.put("ticks", tickMs.size());
            result.put("max_tick_ms", max);
            for (int i = 5; i < Math.min(40, tickMs.size()); i++) steady.add(tickMs.get(i));
            result.put("warmup_tick_median_ms", steady.isEmpty() ? null : steady.stream().sorted().toList().get(steady.size() / 2));
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
            } catch (Throwable t) {
                LOG.error("could not write", t);
            }
            Runtime.getRuntime().halt(0);
        }
    }
}
