package dev.itzgamesgd.moreminecrafts.pyramid;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support.KnownStart;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;

/** ST-01 P8-D4 C4: the decision memo returns exactly what a fresh evaluation returns, per world, bounded and thread-safe. */
public final class St01DecisionMemoGameTests {

    @GameTest(maxTicks = 400)
    public void secondEvaluationOfAFoundCandidateIsAMemoHit(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1004L);
        for (KnownStart s : St01D3Support.KNOWN_STARTS) {
            if (s.seed() != 1004L) continue;
            ChunkPos c = new ChunkPos(s.chunkX(), s.chunkZ());
            long hits = St01DecisionMemo.HITS.get(), misses = St01DecisionMemo.MISSES.get();
            // the locator's evaluation, then the chunk pipeline's evaluation of the same candidate (two fresh contexts)
            Optional<Structure.GenerationStub> first = w.st01.findValidGenerationPoint(w.context(c));
            Optional<Structure.GenerationStub> second = w.st01.findValidGenerationPoint(w.context(c));
            ctx.assertTrue(St01DecisionMemo.MISSES.get() - misses == 1 && St01DecisionMemo.HITS.get() - hits == 1,
                    "expected one miss then one hit for " + s);
            ctx.assertTrue(first.isPresent() && second.isPresent() && first.get().position().equals(second.get().position()),
                    "both evaluations must yield the same stub for " + s);
            ctx.assertTrue(St01D3Support.memoDecision(w, c).equals(St01D3Support.legacyDecision(w, c)), "memo decision differs from legacy for " + s);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void memoDecisionsEqualLegacyIncludingRejections(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1005L);
        int compared = 0;
        for (int rx = -6; rx < 6; rx++) for (int rz = -6; rz < 6; rz++) {
            ChunkPos c = w.candidate(rx, rz);
            St01Candidate.Decision legacy = St01D3Support.legacyDecision(w, c);
            St01Candidate.Decision miss = St01D3Support.memoDecision(w, c), hit = St01D3Support.memoDecision(w, c);
            ctx.assertTrue(miss.equals(legacy) && hit.equals(legacy), "chunk " + c.x() + "," + c.z() + ": " + St01D3Support.describe(hit));
            compared++;
        }
        ctx.assertTrue(St01DecisionMemo.size(w.randomState) == compared, "every decision must be remembered: " + St01DecisionMemo.size(w.randomState));
        ctx.succeed();
    }

    @GameTest(maxTicks = 200)
    public void memoIsPerWorldAndBypassesForeignContexts(GameTestHelper ctx) {
        World a = new World(ctx.getLevel().registryAccess(), 1004L), b = new World(ctx.getLevel().registryAccess(), 1004L);
        ChunkPos c = a.candidate(0, 0);
        St01D3Support.memoDecision(a, c);
        ctx.assertTrue(St01D3Support.memoHit(a, c) && !St01D3Support.memoHit(b, c), "a second world with the same seed must not see the first world's entries");
        // same random state, different height range: must not use (or pollute) the world's cache
        Structure.GenerationContext foreign = new Structure.GenerationContext(a.access, a.generator, a.biomeSource, a.newSampler(), a.randomState,
                null, a.seed, c, LevelHeightAccessor.create(0, 128), h -> a.st01.biomes().contains(h));
        long bypasses = St01DecisionMemo.BYPASSES.get();
        St01DecisionMemo.decide(foreign, Rotation.getRandom(foreign.random()),
                () -> St01Candidate.decide(new St01PyramidStructure.GeneratorTerrain(foreign), c, Rotation.NONE, a.seed));
        ctx.assertTrue(St01DecisionMemo.BYPASSES.get() == bypasses + 1, "a context with another height range must bypass the memo");
        String previous = System.getProperty("mm.st01.memo.disable");
        System.setProperty("mm.st01.memo.disable", "true");
        try {
            ctx.assertTrue(!St01DecisionMemo.enabled() && !St01D3Support.memoHit(a, c), "the disable switch must bypass the memo");
        } finally {
            if (previous == null) System.clearProperty("mm.st01.memo.disable"); else System.setProperty("mm.st01.memo.disable", previous);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void memoIsBoundedAndThreadSafe(GameTestHelper ctx) throws Exception {
        String previous = System.getProperty("mm.st01.memo.max");
        System.setProperty("mm.st01.memo.max", "16");
        World w;
        try {
            w = new World(ctx.getLevel().registryAccess(), 1001L);
            for (int i = 0; i < 40; i++) St01D3Support.memoDecision(w, w.candidate(i - 20, 7));
        } finally {
            if (previous == null) System.clearProperty("mm.st01.memo.max"); else System.setProperty("mm.st01.memo.max", previous);
        }
        ctx.assertTrue(St01DecisionMemo.size(w.randomState) == 16, "LRU cap not applied: " + St01DecisionMemo.size(w.randomState));
        // concurrent evaluations of the same candidates (as locator thread and worldgen workers do) agree with legacy
        World v = new World(ctx.getLevel().registryAccess(), 407538027L);
        List<ChunkPos> chunks = new ArrayList<>();
        for (int rx = -4; rx < 4; rx++) for (int rz = -4; rz < 4; rz++) chunks.add(v.candidate(rx, rz));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<List<St01Candidate.Decision>>> jobs = new ArrayList<>();
            for (int t = 0; t < 4; t++) jobs.add(pool.submit(() -> { List<St01Candidate.Decision> out = new ArrayList<>(); for (ChunkPos c : chunks) out.add(St01D3Support.memoDecision(v, c)); return out; }));
            List<St01Candidate.Decision> reference = new ArrayList<>();
            for (ChunkPos c : chunks) reference.add(St01D3Support.legacyDecision(v, c));
            for (Future<List<St01Candidate.Decision>> f : jobs) ctx.assertTrue(f.get().equals(reference), "a concurrent memo result differs from legacy");
        } finally {
            pool.shutdown();
        }
        ctx.succeed();
    }
}
