package dev.itzgamesgd.moreminecrafts.pyramid;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01DecisionMemo;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01MapTradeIndex;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation.Preparer;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation.Status;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation.Target;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d3.St01D3Support.World;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;

/** ST-01 P8-D6 C6: background search preparation is exact, de-duplicated, bounded, cancellable and never evaluates on the server thread. */
public final class St01SearchPreparationGameTests {

    private static Target target(World w) {
        return new Target(w, w.access, w.generator, w.biomeSource, w.randomState, null, w.seed, w.height, Holder.direct(w.st01), w.placement);
    }

    /** The first eligible candidate in vanilla ring order, computed independently with fresh (memo-free) decisions. */
    private static ChunkPos firstEligibleInRingOrder(World w, BlockPos origin, int radius, List<ChunkPos> visited) {
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4, s = w.placement.spacing();
        for (int r = 0; r <= radius; r++) {
            ChunkPos found = null;
            for (int k = -r; k <= r; k++) for (int l = -r; l <= r; l++) {
                if (k != -r && k != r && l != -r && l != r) continue;
                ChunkPos c = w.placement.getPotentialStructureChunk(w.seed, cx + s * k, cz + s * l);
                visited.add(c);
                if (found == null && St01D3Support.fastDecision(w, c).result().eligible()) found = c;
            }
            if (found != null) return found;
        }
        return null;
    }

    @GameTest(maxTicks = 400)
    public void preparationDecidesVanillaRingOrderExactly(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1004L);
        Preparer p = new Preparer(St01SearchPreparation.QUEUE_CAP, 1, null);
        try {
            BlockPos origin = new BlockPos(0, 64, 0);
            ctx.assertTrue(p.request(target(w), origin, 20) == Status.QUEUED, "first request must queue");
            ctx.assertTrue(p.awaitIdle(60_000), "preparation did not finish");
            var result = p.result(target(w), origin, 20).orElseThrow();
            List<ChunkPos> visited = new ArrayList<>();
            ChunkPos expected = firstEligibleInRingOrder(w, origin, 20, visited);
            ctx.assertTrue(expected != null && result.firstEligible().orElseThrow().equals(expected),
                    "first eligible " + result.firstEligible() + " != independent ring order " + expected);
            ctx.assertTrue(expected.equals(new ChunkPos(-111, 168)), "seed 1004 r20 target moved: " + expected);
            ctx.assertTrue(St01D3Support.KNOWN_STARTS.stream().anyMatch(s -> s.seed() == 1004L && s.chunkX() == -111 && s.chunkZ() == 168),
                    "target must be a known real start");
            ctx.assertTrue(result.candidates() >= visited.size(), "preparation must cover at least the rings up to the target");
            for (ChunkPos c : visited) {
                ctx.assertTrue(St01D3Support.memoHit(w, c), "prepared candidate missing from memo: " + c);
                St01Candidate.Decision fresh = St01D3Support.fastDecision(w, c);
                ctx.assertTrue(St01D3Support.memoDecision(w, c).equals(fresh), "memo differs from a fresh decision at " + c);
                if (fresh.result().eligible())
                    ctx.assertTrue(fresh.equals(St01D3Support.legacyDecision(w, c)), "eligible decision differs from legacy at " + c);
            }
            ctx.assertTrue(p.request(target(w), origin.offset(40, 0, 40), 20) == Status.READY, "an origin in the same region must reuse the job");
        } finally {
            p.shutdown(2000);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void duplicateRequestsShareOneJobAndTheQueueIsCappedAtEight(GameTestHelper ctx) throws Exception {
        World w = new World(ctx.getLevel().registryAccess(), 1001L);
        CountDownLatch gate = new CountDownLatch(1);
        Preparer p = new Preparer(St01SearchPreparation.QUEUE_CAP, 1, c -> {
            try { gate.await(20, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        try {
            Target t = target(w);
            List<BlockPos> origins = new ArrayList<>();
            for (int i = 0; i < 9; i++) origins.add(new BlockPos(i * 100_000, 64, 0));
            ctx.assertTrue(p.request(t, origins.get(0), 0) == Status.QUEUED, "first request must queue");
            ctx.assertTrue(p.request(t, origins.get(0).offset(16, 0, 16), 0) == Status.PENDING, "same region and radius must be de-duplicated");
            ctx.assertTrue(p.request(t, origins.get(0), 1) == Status.QUEUED, "another radius is another job");
            for (int i = 1; i < 7; i++) ctx.assertTrue(p.request(t, origins.get(i), 0) == Status.QUEUED, "request " + i + " must queue");
            ctx.assertTrue(p.pendingCount() == 8, "eight jobs expected, got " + p.pendingCount());
            ctx.assertTrue(p.request(t, origins.get(8), 0) == Status.REJECTED, "the ninth job must be rejected");
            gate.countDown();
            ctx.assertTrue(p.awaitIdle(60_000), "jobs did not finish");
            ctx.assertTrue(p.request(t, origins.get(8), 0) == Status.QUEUED, "a rejected request is accepted once there is room");
            ctx.assertTrue(p.awaitIdle(60_000), "retried job did not finish");
            for (int i = 0; i < 9; i++) if (i != 7) ctx.assertTrue(p.status(t, origins.get(i), 0) == Status.READY, "origin " + i + " not ready");
            ctx.assertTrue(p.status(t, origins.get(7), 0) == Status.INERT, "an origin never requested must not be prepared");
        } finally {
            gate.countDown();
            p.shutdown(2000);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void cancellationIsPromptAndLeavesOnlyExactDecisions(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1001L);
        AtomicInteger seen = new AtomicInteger();
        List<ChunkPos> visited = java.util.Collections.synchronizedList(new ArrayList<>());
        Preparer p = new Preparer(St01SearchPreparation.QUEUE_CAP, 1, c -> { seen.incrementAndGet(); visited.add(c); });
        long cancelNanos;
        BlockPos origin = new BlockPos(0, 64, 0);
        try {
            ctx.assertTrue(p.request(target(w), origin, 100) == Status.QUEUED, "request must queue");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (seen.get() < 12 && System.nanoTime() < deadline) Thread.onSpinWait();
            long t0 = System.nanoTime();
            p.cancelOwner(w);
            ctx.assertTrue(p.awaitIdle(5_000), "cancelled job did not stop");
            cancelNanos = System.nanoTime() - t0;
            ctx.assertTrue(p.status(target(w), origin, 100) == Status.INERT, "a cancelled job must not become ready");
            ctx.assertTrue(p.warmIdle(), "a cancelled job must not queue warm-up work");
            ctx.assertTrue(cancelNanos < TimeUnit.MILLISECONDS.toNanos(1000), "cancellation took " + cancelNanos / 1e6 + " ms");
            List<ChunkPos> snapshot;
            synchronized (visited) { snapshot = new ArrayList<>(visited); }
            for (ChunkPos c : snapshot) {
                if (!St01D3Support.memoHit(w, c)) continue; // the candidate interrupted mid-decision is not stored
                ctx.assertTrue(St01D3Support.memoDecision(w, c).equals(St01D3Support.fastDecision(w, c)), "partial decision stored for " + c);
            }
        } finally {
            p.shutdown(2000);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void failedPreparationIsReportedAndCanBeRetried(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1004L);
        AtomicBoolean fail = new AtomicBoolean(true);
        Preparer p = new Preparer(St01SearchPreparation.QUEUE_CAP, 1, c -> { if (fail.getAndSet(false)) throw new IllegalStateException("injected failure"); });
        try {
            BlockPos origin = new BlockPos(0, 64, 0);
            long failed = St01SearchPreparation.FAILED.get();
            ctx.assertTrue(p.request(target(w), origin, 2) == Status.QUEUED, "request must queue");
            ctx.assertTrue(p.awaitIdle(30_000), "failed job did not finish");
            ctx.assertTrue(St01SearchPreparation.FAILED.get() == failed + 1, "failure must be counted");
            ctx.assertTrue(p.status(target(w), origin, 2) == Status.INERT && p.warmIdle(), "a failed job is neither ready nor warmed");
            ctx.assertTrue(p.request(target(w), origin, 2) == Status.QUEUED, "a failed job can be requested again");
            ctx.assertTrue(p.awaitIdle(30_000) && p.status(target(w), origin, 2) == Status.READY, "retry must succeed");
        } finally {
            p.shutdown(2000);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void shutdownStopsTheWorkerAndAFreshServiceWorks(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1001L);
        AtomicInteger seen = new AtomicInteger();
        Preparer p = new Preparer(St01SearchPreparation.QUEUE_CAP, 1, c -> seen.incrementAndGet());
        BlockPos origin = new BlockPos(0, 64, 0);
        ctx.assertTrue(p.request(target(w), origin, 100) == Status.QUEUED, "request must queue");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (seen.get() < 4 && System.nanoTime() < deadline) Thread.onSpinWait();
        long t0 = System.nanoTime();
        boolean stopped = p.shutdown(2000);
        double ms = (System.nanoTime() - t0) / 1e6;
        ctx.assertTrue(stopped && (p.workerThread() == null || !p.workerThread().isAlive()), "worker still running after shutdown");
        ctx.assertTrue(ms < 1000, "shutdown took " + ms + " ms");
        ctx.assertTrue(p.request(target(w), origin, 2) == Status.REJECTED, "a stopped service must reject");
        Preparer next = new Preparer(St01SearchPreparation.QUEUE_CAP, 1, null);
        try {
            ctx.assertTrue(next.request(target(w), origin, 2) == Status.QUEUED && next.awaitIdle(30_000)
                    && next.status(target(w), origin, 2) == Status.READY, "a fresh service (server restart) must work");
        } finally {
            next.shutdown(2000);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void warmUpRespectsItsBudgetAndNeverEvaluatesOnTheServerThread(GameTestHelper ctx) {
        World w = new World(ctx.getLevel().registryAccess(), 1003L);
        Preparer p = new Preparer(St01SearchPreparation.QUEUE_CAP, 1, null);
        try {
            BlockPos origin = new BlockPos(0, 64, 0);
            p.request(target(w), origin, 2);
            ctx.assertTrue(p.awaitIdle(30_000), "preparation did not finish");
            int candidates = p.result(target(w), origin, 2).orElseThrow().candidates();
            List<ChunkPos> fed = new ArrayList<>();
            long misses = St01DecisionMemo.MISSES.get();
            ctx.assertTrue(p.pump(0, (t, c) -> fed.add(c)) == 0, "a zero budget must feed nothing");
            int n = p.pump(TimeUnit.SECONDS.toNanos(5), (t, c) -> {
                ctx.assertTrue(St01D3Support.memoHit(w, c), "fed a candidate the memo does not hold");
                fed.add(c);
            });
            ctx.assertTrue(n == candidates && fed.size() == candidates && p.warmIdle(), "fed " + n + " of " + candidates);
            ctx.assertTrue(St01DecisionMemo.MISSES.get() == misses, "warm-up must not evaluate a candidate");
            // a candidate that is no longer remembered is dropped, not evaluated
            p.request(target(w), new BlockPos(400_000, 64, 0), 0);
            ctx.assertTrue(p.awaitIdle(30_000), "second preparation did not finish");
            St01DecisionMemo.clear();
            long skipped = St01SearchPreparation.WARM_SKIPPED.get();
            ctx.assertTrue(p.pump(TimeUnit.SECONDS.toNanos(5), (t, c) -> ctx.fail("evaluated an unremembered candidate")) == 0
                    && St01SearchPreparation.WARM_SKIPPED.get() > skipped, "unremembered candidates must be skipped");
        } finally {
            p.shutdown(2000);
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void tradeIndexReadsExplorationMapsFromData(GameTestHelper ctx) {
        var access = ctx.getLevel().registryAccess();
        var scanned = St01MapTradeIndex.scan(access);
        ctx.assertTrue(scanned.equals(List.of(new St01MapTradeIndex.Entry(VillagerProfession.CARTOGRAPHER, 4, 100, true))),
                "production data: only the P8-D8 cartographer level-4 map (radius 100, vanilla skip_existing_chunks): " + scanned);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        Holder<Structure> st01 = access.lookupOrThrow(Registries.STRUCTURE).get(dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen.STRUCTURE_ID).orElseThrow();
        String fixture = "[{\"type\":\"minecraft:exploration_map\",\"decoration\":\"minecraft:target_x\",\"destination\":[\"moreminecrafts:st01_pyramid\"],\"search_radius\":20},"
                + "{\"type\":\"minecraft:filtered\",\"item_filter\":{\"predicates\":{\"minecraft:map_id\":{}}},\"on_fail\":{\"type\":\"minecraft:discard\"}}]";
        var function = LootItemFunctions.CODEC.parse(ops, JsonParser.parseString(fixture)).getOrThrow().value();
        var found = St01MapTradeIndex.searchesOf(function, ops, st01);
        ctx.assertTrue(found.equals(List.of(new St01MapTradeIndex.MapSearch(20, true))), "fixture map trade: " + found);
        var defaults = LootItemFunctions.CODEC.parse(ops, JsonParser.parseString(
                "{\"type\":\"minecraft:exploration_map\",\"destination\":[\"moreminecrafts:st01_pyramid\"],\"skip_existing_chunks\":false}")).getOrThrow().value();
        ctx.assertTrue(St01MapTradeIndex.searchesOf(defaults, ops, st01).equals(List.of(new St01MapTradeIndex.MapSearch(50, false))), "vanilla defaults");
        var ocean = LootItemFunctions.CODEC.parse(ops, JsonParser.parseString(
                "{\"type\":\"minecraft:exploration_map\",\"destination\":[\"minecraft:monument\"]}")).getOrThrow().value();
        ctx.assertTrue(St01MapTradeIndex.searchesOf(ocean, ops, st01).isEmpty(), "a vanilla explorer map is not an ST-01 map");
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void wantedRadiiFollowProfessionLevelAndUngeneratedOffers(GameTestHelper ctx) {
        var access = ctx.getLevel().registryAccess();
        var professions = access.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        var type = access.lookupOrThrow(Registries.VILLAGER_TYPE).getOrThrow(VillagerType.PLAINS);
        var cartographer = professions.getOrThrow(VillagerProfession.CARTOGRAPHER);
        List<St01MapTradeIndex.Entry> entries = List.of(new St01MapTradeIndex.Entry(VillagerProfession.CARTOGRAPHER, 2, 20, true),
                new St01MapTradeIndex.Entry(VillagerProfession.CARTOGRAPHER, 3, 50, true));
        ctx.assertTrue(St01SearchPreparation.wantedRadii(entries, new VillagerData(type, cartographer, 1), false).equals(List.of(20, 50)), "level 1");
        ctx.assertTrue(St01SearchPreparation.wantedRadii(entries, new VillagerData(type, cartographer, 2), false).equals(List.of(50)), "level 2 generated");
        ctx.assertTrue(St01SearchPreparation.wantedRadii(entries, new VillagerData(type, cartographer, 2), true).equals(List.of(20, 50)), "level 2 ungenerated");
        ctx.assertTrue(St01SearchPreparation.wantedRadii(entries, new VillagerData(type, cartographer, 3), false).isEmpty(), "level 3 generated");
        ctx.assertTrue(St01SearchPreparation.wantedRadii(entries, new VillagerData(type, professions.getOrThrow(VillagerProfession.FARMER), 1), false).isEmpty(), "farmer");
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void onlyVillagersThatCanStillBeOfferedTheMapRequestPreparation(GameTestHelper ctx) {
        var professions = ctx.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION);
        long requests = St01SearchPreparation.REQUESTS.get();
        Villager farmer = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        BlockPos at = ctx.absolutePos(new BlockPos(1, 2, 1));
        farmer.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        ctx.getLevel().addFreshEntity(farmer);
        farmer.setVillagerData(farmer.getVillagerData().withProfession(professions.getOrThrow(VillagerProfession.FARMER)));
        ctx.assertTrue(St01SearchPreparation.REQUESTS.get() == requests, "a farmer must not request preparation");
        Villager master = EntityTypes.VILLAGER.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        master.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        ctx.getLevel().addFreshEntity(master);
        master.setVillagerData(master.getVillagerData().withProfession(professions.getOrThrow(VillagerProfession.CARTOGRAPHER)).withLevel(5));
        master.getOffers();
        long before = St01SearchPreparation.REQUESTS.get();
        master.setVillagerData(master.getVillagerData().withLevel(5));
        ctx.assertTrue(St01SearchPreparation.REQUESTS.get() == before, "a level-5 cartographer with generated offers must not request preparation");
        farmer.discard();
        master.discard();
        ctx.succeed();
    }

}
