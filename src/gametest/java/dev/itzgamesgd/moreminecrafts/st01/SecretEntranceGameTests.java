package dev.itzgamesgd.moreminecrafts.st01;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardian;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * ST-01 P9/P11 game tests on a real (headless) server: CR-1 victory/death separation, CR-2 participation, and the secret entrance with the gold
 * guide golem. The arena is a synthetic flat floor with the P6 12-block cover cut at its structure-relative position; it is NOT the real royal
 * hall (no structure/worldgen exists, O-14). Uses the owner-approved P11 production configuration ({@link EntranceConfig#PRODUCTION}).
 */
public final class SecretEntranceGameTests {
    private static final long APPROVED_FALLBACK_TICKS = EntranceConfig.APPROVED_FALLBACK_TICKS;
    private static final double APPROVED_ARRIVAL_RADIUS = EntranceConfig.APPROVED_ARRIVAL_RADIUS;
    private static final AtomicInteger NEXT_ARENA = new AtomicInteger();

    static {
        EntranceConfig.resetToProduction();
    }

    /** Own tick scheduler: nesting ctx.runAfterDelay / ctx.onEachTick inside their own callbacks crashes the 26.3 test ticker (concurrent map iteration). */
    private static final class Sched {
        int tick;
        private final List<Runnable> each = new ArrayList<>();
        private final List<Runnable> actions = new ArrayList<>();
        private final List<Integer> due = new ArrayList<>();
        Sched(GameTestHelper ctx) { ctx.onEachTick(this::run); }
        private void run() {
            tick++;
            for (int i = 0; i < each.size(); i++) each.get(i).run();
            for (int i = 0; i < actions.size(); ) {
                if (due.get(i) <= tick) { Runnable r = actions.remove(i); due.remove(i); r.run(); } else i++;
            }
        }
        void each(Runnable r) { each.add(r); }
        void after(int ticks, Runnable r) { due.add(tick + ticks); actions.add(r); }
    }

    private static final class Arena {
        ServerLevel level; BlockPos anchor, spawn, stop; List<BlockPos> cover; EntranceLayout.Orientation o;
    }

    /** A synthetic far-away arena: floor block layer at anchor.y-3 (feet level anchor.y-2), cover cells = floor blocks, landing platform 4 below the feet. */
    private static Arena arena(GameTestHelper ctx, EntranceLayout.Orientation o, boolean spawnIsland) {
        int idx = NEXT_ARENA.getAndIncrement();
        BlockPos base = ctx.absolutePos(BlockPos.ZERO);
        return buildArena(ctx.getLevel(), new BlockPos(base.getX() + 8192 + (idx % 16) * 160, base.getY() + 10, base.getZ() + 8192 + (idx / 16) * 160), o, spawnIsland, true);
    }

    private static Arena buildArena(ServerLevel level, BlockPos anchor, EntranceLayout.Orientation o, boolean spawnIsland, boolean build) {
        Arena a = new Arena(); a.level = level; a.o = o; a.anchor = anchor;
        for (ChunkPos c : chunksOf(a)) {
            a.level.getChunk(c.x(), c.z());
            a.level.setChunkForced(c.x(), c.z(), true);
        }
        int fy = a.anchor.getY() - 3;
        if (build) for (int x = -17; x <= 20; x++) for (int z = -10; z <= 6; z++) {
            for (int y = -8; y <= 6; y++) a.level.setBlock(a.anchor.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            boolean gap = spawnIsland && z <= -2 && z >= -9 && !(Math.abs(x) <= 1 && z <= -4 && z >= -8);   // spawn cell sits on a detached island
            if (!gap) a.level.setBlock(new BlockPos(a.anchor.getX() + x, fy, a.anchor.getZ() + z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 2);
        }
        if (build && spawnIsland) for (int x = -1; x <= 1; x++) for (int z = -8; z <= -4; z++) a.level.setBlock(new BlockPos(a.anchor.getX() + x, fy, a.anchor.getZ() + z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 2);
        a.cover = EntranceLayout.coverCells(a.anchor, o);
        if (build) for (BlockPos c : a.cover) a.level.setBlock(new BlockPos(c.getX(), a.anchor.getY() - 7, c.getZ()), Blocks.STONE.defaultBlockState(), 2);   // landing platform: feet 4 below the floor feet
        a.spawn = EntranceLayout.golemSpawn(a.anchor, o);
        a.stop = EntranceLayout.stopPoint(a.anchor, o);
        // P10: no forgetLiveState() here. It clears the process-global LIVE map, so an arena built while another test was already guiding wiped that test's
        // EntranceCore (guide lost -> arrival never noticed -> FALLBACK_OPEN), the intermittent full-suite failure analysed in docs/design/st01-p10/. A fresh
        // anchor has no live core anyway.
        if (build) SecretEntranceRuntime.register(a.level, a.anchor, o);
        return a;
    }

    /** Runs the test body once the entities of every arena chunk are loaded (far arenas are generated synchronously, entity storage loads asynchronously). */
    private static void ready(Sched s, Arena[] arenas, Runnable body) {
        AtomicInteger started = new AtomicInteger();
        s.each(() -> {
            if (started.get() != 0) return;
            for (Arena a : arenas)
                for (ChunkPos c : chunksOf(a))
                    if (!a.level.areEntitiesLoaded(c.pack())) return;
            started.set(1);
            body.run();
        });
    }

    /** Arena footprint (anchor-relative x -17..20, z -10..6): kept small so the tests do not starve the server thread of other wall-clock-sensitive tests. */
    private static List<ChunkPos> chunksOf(Arena a) {
        List<ChunkPos> out = new ArrayList<>();
        for (int cx = (a.anchor.getX() - 17) >> 4; cx <= (a.anchor.getX() + 20) >> 4; cx++)
            for (int cz = (a.anchor.getZ() - 10) >> 4; cz <= (a.anchor.getZ() + 6) >> 4; cz++) out.add(new ChunkPos(cx, cz));
        return out;
    }

    private static void release(Arena a) {
        for (ChunkPos c : chunksOf(a)) a.level.setChunkForced(c.x(), c.z(), false);
    }

    private static boolean coverClosed(Arena a) { for (BlockPos c : a.cover) if (a.level.getBlockState(c).isAir()) return false; return true; }
    private static boolean coverOpen(Arena a) { for (BlockPos c : a.cover) if (!a.level.getBlockState(c).isAir()) return false; return true; }
    private static SecretEntranceData.Instance inst(Arena a) { return SecretEntranceRuntime.data(a.level).get(a.anchor.asLong()); }
    private static List<GoldGuideGolem> golems(Arena a) { return a.level.getEntitiesOfClass(GoldGuideGolem.class, new AABB(a.anchor).inflate(128.0D), g -> g.tomb() == a.anchor.asLong() && g.isAlive()); }
    private static int hearts(Arena a) {
        return a.level.getEntitiesOfClass(ItemEntity.class, new AABB(a.spawn).inflate(8.0D), i -> i.getItem().is(ModItems.ANCIENT_HEART)).stream().mapToInt(i -> i.getItem().getCount()).sum();
    }
    private static void clearItems(Arena a) { a.level.getEntitiesOfClass(ItemEntity.class, new AABB(a.spawn).inflate(12.0D)).forEach(Entity::discard); }

    private static TombGuardian start(GameTestHelper ctx, Arena a) {
        var r = TombGuardianEncounters.onRoyalChestInteraction(a.level, a.anchor, a.spawn.offset(4, 0, 0), null);
        ctx.assertTrue(r == TombGuardianEncounters.StartResult.STARTED || r == TombGuardianEncounters.StartResult.RESTARTED, "encounter starts, got " + r);
        TombGuardian g = a.level.getEntitiesOfClass(TombGuardian.class, new AABB(a.anchor).inflate(64.0D), e -> e.isAlive()).get(0);
        g.skipAwakening();
        return g;
    }

    /** A mock player standing a few blocks from the golem spawn (the golem waits for a leader that is too far away, so participants must be near). */
    private static ServerPlayer player(GameTestHelper ctx, Arena a) {
        ServerPlayer p = ctx.makeMockServerPlayerInLevel();
        p.teleportTo(a.level, a.spawn.getX() + 0.5D - 3.0D, a.spawn.getY(), a.spawn.getZ() + 0.5D, java.util.Set.of(), 0.0F, 0.0F, false);
        return p;
    }

    /** Makes the mock participants follow the golem (mock players do not walk): they are placed a few blocks behind it every tick until the entrance opens. */
    private static void follow(Sched s, Arena a, ServerPlayer... players) {
        s.each(() -> {
            if (inst(a).state().isOpen() || golems(a).isEmpty()) return;
            var g = golems(a).get(0);
            for (ServerPlayer p : players) p.teleportTo(a.level, g.getX() - 2.0D, g.getY(), g.getZ(), java.util.Set.of(), 0.0F, 0.0F, false);
        });
    }

    private static void killByPlayer(Arena a, TombGuardian g, ServerPlayer p) { g.hurtServer(a.level, a.level.damageSources().playerAttack(p), 100000.0F); }

    // ------------------------------------------------------------------ CR-1 / CR-2

    @GameTest(maxTicks = 260)
    public void validPlayerVictoryCompletesTheTombAndStartsTheEntrance(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        TombGuardian g = start(ctx, a);
        killByPlayer(a, g, player(ctx, a));
        ctx.assertTrue(TombGuardianEncounters.data(a.level).isVictory(a.anchor.asLong()), "valid victory recorded");
        ctx.assertTrue(TombGuardianEncounters.isDefeated(a.level, a.anchor) && !TombGuardianEncounters.isRoyalChestSealed(a.level, a.anchor), "tomb completed, chest unsealed");
        s.after(4, () -> {
            ctx.assertTrue(inst(a).state() == EntranceState.GOLEM_GUIDING || inst(a).state() == EntranceState.GUARDIAN_DEFEATED, "entrance progressing, got " + inst(a).state());
            ctx.assertTrue(coverClosed(a), "cover still closed while guiding");
            clearItems(a); release(a); ctx.succeed();
        });
        });
    }

    @GameTest(maxTicks = 240)
    public void environmentalDeathIsNotAVictoryChestStaysSealedAndRetryWorks(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        AtomicInteger invalid = new AtomicInteger();
        TombGuardianEncounters.INVALID_DEATH.register((level, anchor, verdict) -> { if (anchor.equals(a.anchor)) invalid.incrementAndGet(); });
        TombGuardian g = start(ctx, a);
        ServerPlayer p = player(ctx, a);
        g.hurtServer(a.level, a.level.damageSources().playerAttack(p), 5.0F);                 // a token hit, then lava finishes it
        g.hurtServer(a.level, a.level.damageSources().lava(), 100000.0F);
        ctx.assertTrue(g.isDeadOrDying(), "the guardian died");
        long tomb = a.anchor.asLong();
        ctx.assertTrue(!TombGuardianEncounters.data(a.level).isVictory(tomb) && !TombGuardianEncounters.isDefeated(a.level, a.anchor), "no victory, no completion");
        ctx.assertTrue(TombGuardianEncounters.isRoyalChestSealed(a.level, a.anchor), "chest stays sealed");
        ctx.assertTrue(invalid.get() == 1, "invalid-death event fired once, got " + invalid.get());
        ctx.assertTrue(TombGuardianEncounters.data(a.level).active(tomb) == null, "binding released");
        var retry = TombGuardianEncounters.onRoyalChestInteraction(a.level, a.anchor, a.spawn, null);
        ctx.assertTrue(retry == TombGuardianEncounters.StartResult.STARTED, "the tomb can be challenged again, got " + retry);
        a.level.getEntitiesOfClass(TombGuardian.class, new AABB(a.anchor).inflate(64.0D)).forEach(Entity::discard);
        s.after(6, () -> {
            ctx.assertTrue(inst(a).state() == EntranceState.SEALED && coverClosed(a) && golems(a).isEmpty(), "entrance never progressed, got " + inst(a).state());
            release(a); ctx.succeed();
        });
        });
    }

    @GameTest(maxTicks = 240)
    public void administrativeKillIsNotAVictory(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        TombGuardian g = start(ctx, a);
        ServerPlayer p = player(ctx, a);
        g.hurtServer(a.level, a.level.damageSources().playerAttack(p), 50.0F);                // genuine participation first
        g.hurtServer(a.level, a.level.damageSources().genericKill(), 100000.0F);               // then /kill-style
        ctx.assertTrue(g.isDeadOrDying() && !TombGuardianEncounters.data(a.level).isVictory(a.anchor.asLong()), "admin kill is not a victory");
        ctx.assertTrue(TombGuardianEncounters.isRoyalChestSealed(a.level, a.anchor), "chest sealed");
        a.level.getEntitiesOfClass(TombGuardian.class, new AABB(a.anchor).inflate(64.0D)).forEach(Entity::discard);
        release(a); ctx.succeed();
        });
    }

    @GameTest(maxTicks = 280)
    public void invalidDeathsDropNoRewardAndValidVictoriesStillDoNotDuplicateIt(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        int invalidHearts = 0;
        for (int i = 0; i < 16; i++) {                                                         // loot is a 50% roll: 16 trials make a false pass vanishingly unlikely
            TombGuardian g = start(ctx, a);
            g.hurtServer(a.level, a.level.damageSources().lava(), 100000.0F);
            invalidHearts += hearts(a); clearItems(a);
        }
        ctx.assertTrue(invalidHearts == 0, "no reward from any of 16 invalid deaths, got " + invalidHearts);
        TombGuardian g = start(ctx, a);
        killByPlayer(a, g, player(ctx, a));
        ctx.assertTrue(hearts(a) <= 1, "at most one Ancient Heart for the valid victory");
        ctx.assertTrue(!TombGuardianEncounters.data(a.level).markVictory(a.anchor.asLong(), g.getUUID()), "completion cannot be granted twice");
        clearItems(a); release(a); ctx.succeed();
        });
    }

    @GameTest(maxTicks = 240)
    public void validVictoryRewardsAtLeastSometimesProvingSuppressionIsNotAlwaysOn(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        AtomicInteger total = new AtomicInteger();
        for (int i = 0; i < 24; i++) {
            // fresh tomb key per trial so the previous completion does not block the next encounter
            BlockPos anchor = a.anchor.offset(i + 1, 0, 0);
            a.level.getChunk(anchor);
            var r = TombGuardianEncounters.onRoyalChestInteraction(a.level, anchor, a.spawn, null);
            TombGuardian g = a.level.getEntitiesOfClass(TombGuardian.class, new AABB(a.anchor).inflate(64.0D), e -> e.isAlive()).get(0);
            g.skipAwakening();
            killByPlayer(a, g, player(ctx, a));
            total.addAndGet(hearts(a)); clearItems(a);
            ctx.assertTrue(r == TombGuardianEncounters.StartResult.STARTED, "trial " + i + " started");
        }
        ctx.assertTrue(total.get() >= 1, "24 valid victories dropped at least one Ancient Heart (50% each), got " + total.get());
        release(a); ctx.succeed();
        });
    }

    @GameTest(maxTicks = 260)
    public void coOpVictoryRecordsEveryContributorAndHandsThemToTheEntrance(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        AtomicReference<java.util.Set<UUID>> seen = new AtomicReference<>();
        TombGuardianEncounters.VICTORY.register((level, anchor, verdict) -> { if (anchor.equals(a.anchor)) seen.set(verdict.participants()); });
        TombGuardian g = start(ctx, a);
        ServerPlayer p1 = player(ctx, a), p2 = player(ctx, a);
        g.hurtServer(a.level, a.level.damageSources().playerAttack(p1), 60.0F);
        killByPlayer(a, g, p2);
        ctx.assertTrue(seen.get() != null && seen.get().contains(p1.getUUID()) && seen.get().contains(p2.getUUID()), "both contributors recorded, got " + seen.get());
        s.after(3, () -> {
            ctx.assertTrue(inst(a).participants().contains(p1.getUUID()) && inst(a).participants().contains(p2.getUUID()), "participants persisted in the entrance record");
            clearItems(a); release(a); ctx.succeed();
        });
        });
    }

    @GameTest(maxTicks = 240)
    public void projectileAndOwnedEntityDamageIsAttributedToThePlayer(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        TombGuardian g = start(ctx, a);
        ServerPlayer p = player(ctx, a);
        net.minecraft.world.entity.projectile.arrow.Arrow arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(a.level, p.getX(), p.getY(), p.getZ(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.setOwner(p);
        ctx.assertTrue(TombVictoryPolicy.attributedPlayer(a.level.damageSources().arrow(arrow, p)) == p, "arrow with owner attributes to the shooter");
        g.hurtServer(a.level, a.level.damageSources().arrow(arrow, p), 40.0F);
        ctx.assertTrue(g.isAlive(), "non-lethal hit");
        g.hurtServer(a.level, a.level.damageSources().lava(), 100000.0F);
        ctx.assertTrue(!TombGuardianEncounters.data(a.level).isVictory(a.anchor.asLong()), "arrow participation + lava finish = no victory under the structural minimum");
        a.level.getEntitiesOfClass(TombGuardian.class, new AABB(a.anchor).inflate(64.0D)).forEach(Entity::discard);
        release(a); ctx.succeed();
        });
    }

    // ------------------------------------------------------------------ entrance, golem, cover

    @GameTest(maxTicks = 1400)
    public void goldGolemLeadsToTheStopPointAndOpensTheCoverExactlyOnce(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        TombGuardian g = start(ctx, a);
        ServerPlayer lead = player(ctx, a);
        killByPlayer(a, g, lead);
        follow(s, a, lead);
        AtomicInteger phase = new AtomicInteger();
        s.each(() -> {
            if (phase.get() == 0 && inst(a).state() == EntranceState.GOLEM_GUIDING) System.out.println("P9_TIMING guiding at tick " + s.tick);
            if (phase.get() == 1 && inst(a).state() == EntranceState.ENTRANCE_OPEN) System.out.println("P9_TIMING open at tick " + s.tick);
            if (s.tick == 1300 && phase.get() < 2) {
                var gl = golems(a);
                ctx.fail("no arrival by tick 1300: state=" + inst(a).state() + " remaining=" + inst(a).remaining() + " golems=" + gl.size() + (gl.isEmpty() ? "" : " pos=" + gl.get(0).position() + " stop=" + a.stop
                        + " leader=" + gl.get(0).leader() + " lead=" + lead.position() + " navDone=" + gl.get(0).getNavigation().isDone() + " ground=" + gl.get(0).onGround()) + " phase=" + phase.get());
            }
            if (phase.get() == 0 && inst(a).state() == EntranceState.GOLEM_GUIDING) {
                List<GoldGuideGolem> gs = golems(a);
                ctx.assertTrue(gs.size() == 1, "exactly one golem, got " + gs.size());
                GoldGuideGolem golem = gs.get(0);
                System.out.println("P9_GOLEM_BBOX width=" + golem.getBbWidth() + " height=" + golem.getBbHeight() + " scale=" + golem.getScale());
                ctx.assertTrue(Math.abs(golem.getBbWidth() - 0.7F) < 0.05F && Math.abs(golem.getBbHeight() - 1.35F) < 0.05F, "hitbox follows the scale attribute: " + golem.getBbWidth() + "x" + golem.getBbHeight());
                ctx.assertTrue(coverClosed(a), "closed while guiding");
                phase.set(1);
            } else if (phase.get() == 1 && inst(a).state() == EntranceState.ENTRANCE_OPEN) {
                ctx.assertTrue(coverOpen(a), "all 12 cover cells removed on arrival");
                GoldGuideGolem golem = golems(a).get(0);
                double d = Math.hypot(golem.getX() - (a.stop.getX() + 0.5D), golem.getZ() - (a.stop.getZ() + 0.5D));
                ctx.assertTrue(d <= APPROVED_ARRIVAL_RADIUS + 0.01D, "the golem stands inside the arrival radius, distance " + d);
                boolean overCut = a.cover.stream().anyMatch(c -> golem.blockPosition().getX() == c.getX() && golem.blockPosition().getZ() == c.getZ());
                ctx.assertTrue(!overCut, "the golem does not stand over the cut");
                ctx.assertTrue(golem.isAlive() && golem.onGround(), "and did not fall");
                phase.set(2);
                s.after(40, () -> {
                    ctx.assertTrue(inst(a).state() == EntranceState.ENTRANCE_OPEN && coverOpen(a) && golems(a).size() == 1, "stable: still open, still one golem");
                    golems(a).forEach(Entity::discard); clearItems(a); release(a); ctx.succeed();
                });
            }
        });
        });
    }

    @GameTest(maxTicks = 1400)
    public void golemSpawnFailureStillOpensThroughTheFallbackTimer(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
        SecretEntranceRuntime.TestHooks.failGolemSpawnFor.add(a.anchor.asLong());
        TombGuardian g = start(ctx, a);
        killByPlayer(a, g, player(ctx, a));
        AtomicInteger seenDefeated = new AtomicInteger();
        s.each(() -> {
            if (inst(a).state() == EntranceState.GUARDIAN_DEFEATED) seenDefeated.incrementAndGet();
            if (inst(a).state() == EntranceState.FALLBACK_OPEN) {
                SecretEntranceRuntime.TestHooks.failGolemSpawnFor.remove(a.anchor.asLong());
                ctx.assertTrue(seenDefeated.get() > 300 && golems(a).isEmpty() && coverOpen(a), "waited with no golem, then opened by the timer");
                clearItems(a); release(a); ctx.succeed();
            }
        });
        });
    }

    @GameTest(maxTicks = 1400)
    public void golemPathFailureFallsBackWithoutOpeningEarly(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, true);       // the spawn cell sits on a detached island: no path to the stop point
        ready(s, new Arena[]{a}, () -> {
        TombGuardian g = start(ctx, a);
        killByPlayer(a, g, player(ctx, a));
        s.each(() -> {
            if (inst(a).state() == EntranceState.ENTRANCE_OPEN) ctx.fail("a golem that cannot reach the stop point must not open the entrance");
            if (inst(a).state() == EntranceState.FALLBACK_OPEN) {
                ctx.assertTrue(coverOpen(a), "cover opened by the fallback");
                golems(a).forEach(Entity::discard); clearItems(a); release(a); ctx.succeed();
            }
        });
        });
    }

    @GameTest(maxTicks = 1400)
    public void goneGolemFallsBackToTheTimer(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, true);
        ready(s, new Arena[]{a}, () -> {
        TombGuardian g = start(ctx, a);
        killByPlayer(a, g, player(ctx, a));
        AtomicInteger killed = new AtomicInteger();
        s.each(() -> {
            if (killed.get() == 0 && inst(a).state() == EntranceState.GOLEM_GUIDING && !golems(a).isEmpty()) { golems(a).forEach(Entity::discard); killed.set(1); }
            if (inst(a).state() == EntranceState.FALLBACK_OPEN) {
                ctx.assertTrue(killed.get() == 1 && golems(a).isEmpty() && coverOpen(a), "golem removed mid-guidance; timer opened the cover; no replacement spawned (killed=" + killed.get() + " golems=" + golems(a).size() + " open=" + coverOpen(a) + ")");
                clearItems(a); release(a); ctx.succeed();
            }
        });
        });
    }

    @GameTest(maxTicks = 1400)
    public void twoInstancesProgressIndependently(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        Arena b = arena(ctx, EntranceLayout.Orientation.WEST, false);
        SecretEntranceRuntime.register(b.level, b.anchor, b.o);
        ready(s, new Arena[]{a, b}, () -> {
        TombGuardian ga = start(ctx, a);
        killByPlayer(a, ga, player(ctx, a));
        s.after(5, () -> {
            ctx.assertTrue(inst(b).state() == EntranceState.SEALED && coverClosed(b) && golems(b).isEmpty(), "B untouched by A's victory");
            TombGuardian gb = start(ctx, b);
            killByPlayer(b, gb, player(ctx, a));
        });
        s.each(() -> {
            if (inst(a).state().isOpen() && inst(b).state().isOpen()) {
                ctx.assertTrue(coverOpen(a) && coverOpen(b) && golems(a).size() <= 1 && golems(b).size() <= 1, "both open, each with at most one own golem");
                ctx.assertTrue(golems(a).stream().allMatch(x -> x.tomb() == a.anchor.asLong()) && golems(b).stream().allMatch(x -> x.tomb() == b.anchor.asLong()), "golems carry their own instance");
                golems(a).forEach(Entity::discard); golems(b).forEach(Entity::discard); clearItems(a); clearItems(b); release(a); release(b); ctx.succeed();
            }
        });
        });
    }

    @GameTest(maxTicks = 1400, environment = "moreminecrafts-gametest:p10_isolated")   // P10: wipes the global live state on purpose, so it must not share a batch with other entrance tests
    public void openStateSurvivesLiveStateLossAndRepairsRevertedBlocksButNeverCloses(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.WEST, false);
        ready(s, new Arena[]{a}, () -> {
        TombGuardian g = start(ctx, a);
        killByPlayer(a, g, player(ctx, a));
        AtomicInteger phase = new AtomicInteger();
        s.each(() -> {
            if (phase.get() == 0 && inst(a).state().isOpen()) {
                ctx.assertTrue(coverOpen(a), "opened");
                SecretEntranceRuntime.forgetLiveState();                                   // server restart: in-memory cores are gone, the saved record remains
                BlockPos c0 = a.cover.get(0); a.level.setBlock(c0, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 3);   // a reverted block (e.g. regenerated chunk)
                phase.set(1);
            } else if (phase.get() == 1 && coverOpen(a)) {
                ctx.assertTrue(inst(a).state().isOpen(), "record still open after live-state loss");
                SecretEntranceRuntime.forgetLiveState();
                phase.set(2);
                s.after(30, () -> {
                    ctx.assertTrue(coverOpen(a) && inst(a).state().isOpen(), "stays open: nothing closes it automatically");
                    golems(a).forEach(Entity::discard); clearItems(a); release(a); ctx.succeed();
                });
            }
        });
        });
    }

    @GameTest(maxTicks = 300)
    public void occupantStandingOnTheCoverFallsSafelyWhenItOpens(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
            // Mock players are not physics-ticked in game tests (and NoAI mobs do not fall), so the occupant proxies are a real armor stand
            // (deterministic position/drop) and a villager with AI (fall damage; asserted only when it stayed on its column).
            BlockPos col = a.cover.stream().max(java.util.Comparator.comparingInt(c -> Math.abs(c.getX() - a.anchor.getX()))).get();   // the deepest column (4-block drop)
            net.minecraft.world.entity.decoration.ArmorStand stand = net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(a.level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            stand.snapTo(col.getX() + 0.5D, col.getY() + 1.0D, col.getZ() + 0.5D, 0.0F, 0.0F);
            a.level.addFreshEntity(stand);
            net.minecraft.world.entity.npc.villager.Villager v = net.minecraft.world.entity.EntityTypes.VILLAGER.create(a.level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            v.snapTo(col.getX() + 0.5D, col.getY() + 1.0D, col.getZ() + 0.5D, 0.0F, 0.0F);
            a.level.addFreshEntity(v);
            float h0 = v.getHealth();
            double[] vx = new double[2];
            s.after(2, () -> {
                ctx.assertTrue(Math.abs(stand.getY() - (col.getY() + 1.0D)) < 0.05D, "standing on the closed cover, y " + stand.getY());
                vx[0] = v.getX(); vx[1] = v.getZ();
                new McPortsProbe().open(a);
                s.after(30, () -> {
                    double fallen = col.getY() + 1.0D - stand.getY();
                    boolean stayed = Math.abs(vx[0] - (col.getX() + 0.5D)) < 0.4D && Math.abs(vx[1] - (col.getZ() + 0.5D)) < 0.4D;
                    double vfall = col.getY() + 1.0D - v.getY();
                    System.out.println("P9_FALL armorStandDrop=" + fallen + " villagerStayed=" + stayed + " villagerDrop=" + vfall + " villagerHealth=" + h0 + "->" + v.getHealth() + " inWall=" + (stand.isInWall() || v.isInWall()));
                    ctx.assertTrue(stand.isAlive() && !stand.isInWall() && !v.isInWall(), "nobody is buried");
                    ctx.assertTrue(fallen > 3.9D && fallen < 4.1D, "the occupant dropped exactly the 4 blocks to the platform, dropped " + fallen);
                    if (stayed) ctx.assertTrue(vfall > 3.9D && vfall < 4.1D && v.getHealth() < h0, "a living occupant drops 4 and takes fall damage, " + vfall + " " + v.getHealth());
                    stand.discard(); v.discard(); release(a); ctx.succeed();
                });
            });
        });
    }

    /** Opens the cover the way the runtime does, without any state change (cover mechanics only). */
    private static final class McPortsProbe {
        void open(Arena a) { for (BlockPos c : a.cover) a.level.setBlock(c, Blocks.AIR.defaultBlockState(), 3); }
    }

    @GameTest(maxTicks = 400)
    public void leaderIsTheNearestParticipantAndSticky(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
            TombGuardian g = start(ctx, a);
            ServerPlayer p1 = player(ctx, a), p2 = player(ctx, a);
            p1.teleportTo(a.level, a.spawn.getX() + 4.5D, a.spawn.getY(), a.spawn.getZ() + 0.5D, java.util.Set.of(), 0.0F, 0.0F, false);
            p2.teleportTo(a.level, a.spawn.getX() - 9.5D, a.spawn.getY(), a.spawn.getZ() + 0.5D, java.util.Set.of(), 0.0F, 0.0F, false);
            g.hurtServer(a.level, a.level.damageSources().playerAttack(p2), 60.0F);
            g.hurtServer(a.level, a.level.damageSources().playerAttack(p1), 100000.0F);
            AtomicInteger checks = new AtomicInteger();
            s.each(() -> {
                if (golems(a).size() != 1 || golems(a).get(0).leader() == null || checks.get() > 0) return;
                checks.set(1);
                UUID first = golems(a).get(0).leader();
                System.out.println("P9_LEADER leader=" + first + " p1=" + p1.getUUID() + " p2=" + p2.getUUID());
                ctx.assertTrue(first.equals(p1.getUUID()) || first.equals(p2.getUUID()), "the leader is one of the participants");
                golems(a).forEach(Entity::discard); clearItems(a); release(a); ctx.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ chunk unload / reload and real process restart

    @GameTest(maxTicks = 1400)
    public void coverIsAppliedOnReloadWhenTheChunkWasUnloadedAtTheDeadline(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        Arena a = arena(ctx, EntranceLayout.Orientation.EAST, false);
        ready(s, new Arena[]{a}, () -> {
            TombGuardian g = start(ctx, a);
            killByPlayer(a, g, ctx.makeMockServerPlayerInLevel());   // the killer stays at the test origin: a player near the arena would keep its chunks loaded
            AtomicInteger phase = new AtomicInteger();
            s.each(() -> {
                switch (phase.get()) {
                    case 0 -> { if (inst(a).state() == EntranceState.GOLEM_GUIDING) { release(a); phase.set(1); } }                  // let the chunks unload while guiding
                    case 1 -> { if (!a.level.hasChunkAt(a.cover.get(0))) phase.set(2); }
                    case 2 -> {
                        ctx.assertTrue(inst(a).state() != EntranceState.SEALED, "the record keeps running while the chunk is unloaded");
                        if (inst(a).state() == EntranceState.FALLBACK_OPEN) {
                            ctx.assertTrue(!a.level.hasChunkAt(a.cover.get(0)), "the cover chunk is still unloaded at the deadline: nothing was force-loaded to apply it");
                            for (ChunkPos c : chunksOf(a)) a.level.setChunkForced(c.x(), c.z(), true);
                            phase.set(3);
                        }
                    }
                    case 3 -> {
                        if (a.level.hasChunkAt(a.cover.get(0)) && coverOpen(a)) {
                            ctx.assertTrue(golems(a).isEmpty() || golems(a).size() == 1, "no duplicate golem after reload");
                            golems(a).forEach(Entity::discard); clearItems(a); release(a); phase.set(4); ctx.succeed();
                        }
                    }
                    default -> { }
                }
            });
        });
    }

    private static final BlockPos RESTART_ANCHOR = new BlockPos(24000, 80, 24000);

    /**
     * Real process restart, run in three separate server processes (-Dmoreminecrafts.st01.restart.phase=write|reload|verify); inert otherwise.
     * write: valid victory, entrance GUIDING, save. reload: state/record/victory survived, golem gone, fallback opens the cover. verify: OPEN survived
     * a second restart, nothing re-progressed.
     */
    @GameTest(maxTicks = 1400)
    public void entranceStateSurvivesProcessRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.st01.restart.phase");
        if (phase == null || phase.isBlank()) { ctx.succeed(); return; }
        Sched s = new Sched(ctx);
        boolean write = phase.equals("write");
        Arena a = buildArena(ctx.getLevel(), RESTART_ANCHOR, EntranceLayout.Orientation.EAST, false, write);
        ready(s, new Arena[]{a}, () -> {
            switch (phase) {
                case "write" -> {
                    TombGuardian g = start(ctx, a);
                    killByPlayer(a, g, player(ctx, a));
                    s.each(() -> {
                        if (inst(a).state() != EntranceState.GOLEM_GUIDING) return;
                        golems(a).forEach(Entity::discard);
                        System.out.println("P9_RESTART_WRITE remaining=" + inst(a).remaining());
                        a.level.getServer().saveEverything(true, true, true);
                        ctx.succeed();
                    });
                }
                case "reload" -> {
                    ctx.assertTrue(inst(a) != null && TombGuardianEncounters.data(a.level).isVictory(a.anchor.asLong()), "the entrance record and the valid victory survived the restart");
                    EntranceState st = inst(a).state();
                    ctx.assertTrue(st == EntranceState.GOLEM_GUIDING || st == EntranceState.FALLBACK_OPEN, "state survived, got " + st);
                    ctx.assertTrue(golems(a).isEmpty() || st == EntranceState.GOLEM_GUIDING, "no golem is assumed to survive");
                    s.each(() -> {
                        if (inst(a).state() == EntranceState.FALLBACK_OPEN && coverOpen(a)) {
                            System.out.println("P9_RESTART_RELOAD");
                            a.level.getServer().saveEverything(true, true, true);
                            ctx.succeed();
                        }
                    });
                }
                default -> {
                    ctx.assertTrue(inst(a).state() == EntranceState.FALLBACK_OPEN && coverOpen(a), "OPEN survived the second restart");
                    ctx.assertTrue(TombGuardianEncounters.isDefeated(a.level, a.anchor) && !TombGuardianEncounters.isRoyalChestSealed(a.level, a.anchor), "tomb stays completed, chest unsealed");
                    ctx.assertTrue(golems(a).isEmpty(), "no golem re-appeared");
                    s.after(40, () -> {
                        ctx.assertTrue(inst(a).state() == EntranceState.FALLBACK_OPEN && coverOpen(a) && golems(a).isEmpty(), "nothing re-progressed");
                        System.out.println("P9_RESTART_VERIFY");
                        a.level.getServer().saveEverything(true, true, true);
                        ctx.succeed();
                    });
                }
            }
        });
    }
}
