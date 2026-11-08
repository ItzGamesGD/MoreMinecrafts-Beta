package dev.itzgamesgd.moreminecrafts.st01;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardian;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import dev.itzgamesgd.moreminecrafts.st01.P10Support.Sched;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

/**
 * ST-01 P10: the whole P9 pipeline (valid victory -> guide golem -> arrival/fallback -> cover open) inside the REAL royal-tomb geometry, one fixture
 * per run. Participants are SIMULATED (mock server players moved by the test: no collision, no client). Results are written as CSV when
 * -Dmoreminecrafts.p10.out is set. EntranceConfig is the same TEST-ONLY 600 ticks / 1.5 blocks as the P9 tests (process-global).
 */
public final class RoyalTombFlowGameTests {
    static { EntranceConfig.install(EntranceConfig.testOnly(600L, 1.5D)); }

    static final int RUN_TIMEOUT = 1500;

    /** A simulated participant. */
    static final class Fol {
        ServerPlayer p; double speed; int delay; double startDx, startDz; boolean follows = true; int logoutAt = -1; boolean hits; double staticDist = -1;
    }

    static final class Flow {
        final GameTestHelper ctx; final Sched s; final RoyalTombFixture.Placed p; final String scenario; final int run; String params;
        final List<Fol> fols = new ArrayList<>(); int t0 = -1; boolean started, finished;
        int guidingTick = -1, openTick = -1, golemSeenTick = -1, arrivedUndetected = -1, undetRun; String finalState = "TIMEOUT"; double openDist = Double.NaN, openGap = Double.NaN; int openMismatch = -1, golemCountAtOpen = -1;
        double speedOverride = Double.NaN; int interfereAt = -1; boolean releaseChunks; int releasedTick = -1; boolean chunkWasUnloaded; String note = "";
        final java.util.function.Consumer<Flow> onFinish;
        final BlockPos spawnCell, stop; final List<BlockPos> cover; final RoyalTombFixture f;
        TombGuardian boss;
        final List<ServerPlayer> created = new ArrayList<>();

        Flow(GameTestHelper ctx, Sched s, RoyalTombFixture.Placed p, String scenario, int run, String params, java.util.function.Consumer<Flow> onFinish) {
            this.ctx = ctx; this.s = s; this.p = p; this.scenario = scenario; this.run = run; this.params = params; this.onFinish = onFinish; this.f = p.fixture();
            spawnCell = EntranceLayout.golemSpawn(p.anchor, f.orientation); stop = EntranceLayout.stopPoint(p.anchor, f.orientation); cover = EntranceLayout.coverCells(p.anchor, f.orientation);
            s.each(this::tick);
        }

        SecretEntranceData.Instance inst() { return SecretEntranceRuntime.data(p.level).get(p.anchor.asLong()); }
        List<GoldGuideGolem> golems() { return p.level.getEntitiesOfClass(GoldGuideGolem.class, new AABB(p.anchor).inflate(128.0D), g -> g.tomb() == p.anchor.asLong() && g.isAlive()); }

        ServerPlayer mock(double x, double z) {
            ServerPlayer pl = ctx.makeMockServerPlayerInLevel();
            pl.teleportTo(p.level, x, spawnCell.getY(), z, java.util.Set.of(), 0.0F, 0.0F, false);
            created.add(pl);
            return pl;
        }

        void tick() {
            if (finished) return;
            if (!started) {
                for (var c : p.chunks()) if (!p.level.areEntitiesLoaded(c.pack())) return;
                started = true; t0 = s.tick;
                SecretEntranceRuntime.register(p.level, p.anchor, f.orientation);
                BlockPos bossAt = spawnCell.offset(f.orientation.sign * 4, 0, 0);
                var r = TombGuardianEncounters.onRoyalChestInteraction(p.level, p.anchor, bossAt, null);
                if (r != TombGuardianEncounters.StartResult.STARTED) { note += "start=" + r; end("START_FAILED"); return; }
                boss = p.level.getEntitiesOfClass(TombGuardian.class, new AABB(p.anchor).inflate(64.0D), e -> e.isAlive()).get(0);
                boss.skipAwakening();
                ServerPlayer killer = null;
                for (Fol fo : fols) {
                    fo.p = mock(spawnCell.getX() + 0.5D + f.orientation.sign * fo.startDx, spawnCell.getZ() + 0.5D + fo.startDz);   // start offsets are hall-local: mirrored for WEST
                    if (fo.hits) { if (killer != null) boss.hurtServer(p.level, p.level.damageSources().playerAttack(fo.p), 20.0F); killer = killer == null ? fo.p : killer; }
                }
                if (killer == null) { killer = mock(spawnCell.getX() + 0.5D - 3, spawnCell.getZ() + 0.5D); }
                boss.hurtServer(p.level, p.level.damageSources().playerAttack(killer), 100000.0F);
                if (!TombGuardianEncounters.data(p.level).isVictory(p.anchor.asLong())) { note += "no-victory"; end("NO_VICTORY"); }
                return;
            }
            int age = s.tick - t0;
            var in = inst();
            if (in == null) { end("NO_INSTANCE"); return; }
            EntranceState st = in.state();
            List<GoldGuideGolem> gs = golems();
            if (guidingTick < 0 && st == EntranceState.GOLEM_GUIDING) guidingTick = age;
            if (!gs.isEmpty() && golemSeenTick < 0) {
                golemSeenTick = age;
                if (!Double.isNaN(speedOverride)) gs.get(0).getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speedOverride);
            }
            if (interfereAt >= 0 && age == golemSeenTick + interfereAt && golemSeenTick >= 0) { SecretEntranceRuntime.forgetLiveState(); note += "forgetLiveState@" + age + ";"; }
            if (releaseChunks && releasedTick < 0 && st == EntranceState.GOLEM_GUIDING) { p.release(); releasedTick = age; }
            if (releasedTick >= 0 && !chunkWasUnloaded && !p.level.hasChunkAt(cover.get(0))) chunkWasUnloaded = true;
            moveParticipants(age, gs);
            // arrival that the entrance did not notice (lost guide): golem stands at the stop point but the record is not open
            if (!gs.isEmpty() && !st.isOpen() && arrivedUndetected < 0) {
                var g = gs.get(0);
                if (g.onGround() && Math.hypot(g.getX() - (stop.getX() + 0.5D), g.getZ() - (stop.getZ() + 0.5D)) <= 1.5D) { if (++undetRun == 10) arrivedUndetected = age - 9; } else undetRun = 0;
            }
            if (st.isOpen() && openTick < 0) {
                openTick = age; finalState = st.name();
                if (releasedTick >= 0 && !p.level.hasChunkAt(cover.get(0))) { note += "open-while-unloaded;"; }
                if (!gs.isEmpty()) {
                    var g = gs.get(0);
                    openDist = Math.hypot(g.getX() - (stop.getX() + 0.5D), g.getZ() - (stop.getZ() + 0.5D));
                    openGap = coverGap(g.getBoundingBox());
                }
                golemCountAtOpen = gs.size();
                if (releasedTick >= 0 && !p.level.hasChunkAt(cover.get(0))) {
                    for (var c : p.chunks()) p.level.setChunkForced(c.x(), c.z(), true);   // reload: the cover must be applied then
                    s.after(60, () -> { openMismatch = p.level.hasChunkAt(cover.get(0)) ? p.voxelDiff()[0] : -2; end(finalState); });
                } else s.after(10, () -> { openMismatch = p.voxelDiff()[0]; end(finalState); });
            }
            if (age > RUN_TIMEOUT && !finished) end(openTick >= 0 ? finalState : "TIMEOUT");
        }

        double coverGap(AABB b) {
            int minX = cover.stream().mapToInt(BlockPos::getX).min().getAsInt(), maxX = cover.stream().mapToInt(BlockPos::getX).max().getAsInt() + 1;
            int minZ = cover.stream().mapToInt(BlockPos::getZ).min().getAsInt(), maxZ = cover.stream().mapToInt(BlockPos::getZ).max().getAsInt() + 1;
            return Math.hypot(Math.max(0.0D, Math.max(minX - b.maxX, b.minX - maxX)), Math.max(0.0D, Math.max(minZ - b.maxZ, b.minZ - maxZ)));
        }

        void moveParticipants(int age, List<GoldGuideGolem> gs) {
            for (Fol fo : fols) {
                if (fo.p == null || fo.p.isRemoved()) continue;
                if (fo.logoutAt >= 0 && age >= fo.logoutAt) { fo.p.discard(); continue; }
                if (!fo.follows || age < fo.delay || gs.isEmpty()) continue;
                var g = gs.get(0);
                double dx = g.getX() - fo.p.getX(), dz = g.getZ() - fo.p.getZ(), d = Math.hypot(dx, dz);
                double nx = fo.p.getX(), nz = fo.p.getZ();
                if (d > 2.5D) { double step = Math.min(fo.speed, d - 2.5D); nx += dx / d * step; nz += dz / d * step; }
                fo.p.teleportTo(p.level, nx, g.getY(), nz, java.util.Set.of(), 0.0F, 0.0F, false);
            }
        }

        void end(String state) {
            if (finished) return;
            finished = true;
            if (finalState.equals("TIMEOUT")) finalState = state;
            for (var g : golems()) g.discard();
            p.level.getEntitiesOfClass(Entity.class, new AABB(p.anchor).inflate(80.0D), e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
            // free the mock players now (each carries advancement/recipe state; the framework would only drop them when the whole test ends)
            for (ServerPlayer pl : created) { try { p.level.getServer().getPlayerList().remove(pl); } catch (RuntimeException ignored) { } if (!pl.isRemoved()) pl.discard(); }
            p.release();
            onFinish.accept(this);
        }

        String row(String side) {
            return String.join(",", side, scenario, Integer.toString(run), "\"" + params + "\"", finalState, Integer.toString(guidingTick), Integer.toString(golemSeenTick), Integer.toString(openTick),
                    Double.isNaN(openDist) ? "" : String.format(Locale.ROOT, "%.3f", openDist), Double.isNaN(openGap) ? "" : String.format(Locale.ROOT, "%.3f", openGap), Integer.toString(openMismatch),
                    Integer.toString(golemCountAtOpen), Integer.toString(arrivedUndetected), "\"" + note + "\"");
        }
    }

    static final String HEADER = "side,scenario,run,params,final_state,guiding_tick,golem_seen_tick,open_tick,golem_dist_to_stop,golem_gap_to_cover,voxel_mismatch_after_open,golems_at_open,arrived_undetected_tick,note";

    /** Runs the prepared flows (each builds its own fixture) in batches of {@code parallel}; asserts nothing about timing, only the structural invariants. */
    static void runFlows(GameTestHelper ctx, String side, String label, int count, int parallel, java.util.function.BiConsumer<Flow, Integer> configure, java.util.function.BiConsumer<GameTestHelper, Flow> verify) {
        Sched s = new Sched(ctx);
        List<String> rows = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        int[] started = {0}, done = {0};
        s.each(() -> {
            while (started[0] < count && started[0] - done[0] < parallel) {
                int i = started[0]++;
                RoyalTombFixture f = RoyalTombFixture.load(side);
                RoyalTombFixture.Placed p = f.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
                p.build();
                Flow fl = new Flow(ctx, s, p, label, i, "", x -> {
                    rows.add(x.row(side)); done[0]++;
                    System.out.println("P10_FLOW " + side + " " + label + " #" + x.run + " state=" + x.finalState + " open=" + x.openTick + " guiding=" + x.guidingTick + " dist=" + x.openDist + " gap=" + x.openGap + " mismatch=" + x.openMismatch + " undetected=" + x.arrivedUndetected + " " + x.params + " " + x.note);
                    if (!x.note.isEmpty() && x.note.contains("start=")) failures.add(x.note);
                    verify.accept(ctx, x);
                    if (done[0] >= count) { P10Support.csv("flow_runs.csv", HEADER, rows); ctx.succeed(); }
                });
                configure.accept(fl, i);
            }
        });
    }

    static Fol follower(double dx, double dz, double speed, int delay, boolean hits) {
        Fol fo = new Fol(); fo.startDx = dx; fo.startDz = dz; fo.speed = speed; fo.delay = delay; fo.hits = hits; return fo;
    }

    // -------------------------------------------------------------- S1: one follower, sampled distance / loot delay / walk speed

    private void sampled(GameTestHelper ctx, String side, String label, int n, long seed) {
        Random rng = new Random(seed);
        double[] dist = new double[n], speed = new double[n]; int[] delay = new int[n];
        int[] delays = {0, 60, 120, 240};
        for (int i = 0; i < n; i++) { dist[i] = 2.0D + rng.nextDouble() * 14.0D; speed[i] = new double[]{0.12D, 0.20D, 0.27D}[rng.nextInt(3)]; delay[i] = delays[rng.nextInt(delays.length)]; }
        runFlows(ctx, side, label, n, 8, (fl, i) -> {
            fl.fols.add(follower(-dist[i], 0.0D, speed[i], delay[i], true));
            fl.params = String.format(Locale.ROOT, "dist=%.1f speed=%.2f delay=%d", dist[i], speed[i], delay[i]);
        }, (c, x) -> {
            c.assertTrue(x.finalState.equals("ENTRANCE_OPEN") && x.openMismatch == 12 && x.arrivedUndetected < 0, side + " sampled follower flow #" + x.run + " (" + x.params + ") ends in a normal arrival with only the 12 cover cells changed, got " + x.finalState + " mismatch " + x.openMismatch);
            c.assertTrue(x.openTick < 600, "arrival before the TEST-ONLY fallback, open tick " + x.openTick);
        });
    }

    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_flows") public void flowSampledFollowerEast(GameTestHelper ctx) { sampled(ctx, "E", "sampled", P10Support.measure() ? 32 : 3, 0xE45L); }
    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_flows") public void flowSampledFollowerWest(GameTestHelper ctx) { sampled(ctx, "W", "sampled", P10Support.measure() ? 32 : 3, 0xE45L); }

    // -------------------------------------------------------------- S2..S4: several participants, logout, far-only

    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_flows")
    public void flowDispersedParticipants(GameTestHelper ctx) {
        int n = P10Support.measure() ? 6 : 1;
        runFlows(ctx, "E", "dispersed", n, n, (fl, i) -> {
            fl.fols.add(follower(-3.0D, 0.0D, 0.2D, 0, true));            // nearest, follows
            fl.fols.add(follower(-12.0D, 2.0D, 0.2D, 99999, true));       // 12 away, never moves (AFK)
            fl.fols.add(follower(-25.0D, -3.0D, 0.2D, 99999, true));      // 25 away, never moves
            fl.params = "3 participants at 3/12/25 blocks; only the nearest follows";
        }, (c, x) -> c.assertTrue(x.finalState.equals("ENTRANCE_OPEN") && x.openMismatch == 12, "nearest participant leads to a normal arrival, got " + x.finalState));
    }

    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_flows")
    public void flowLeaderLogsOut(GameTestHelper ctx) {
        int n = P10Support.measure() ? 6 : 2;
        runFlows(ctx, "E", "leader_logout", n, n, (fl, i) -> {
            Fol a = follower(-3.0D, 0.0D, 0.2D, 0, true); a.logoutAt = 40 + 30 * i;     // leader disappears at tick 40..190
            Fol b = follower(-8.0D, 1.0D, 0.2D, 0, true);                                    // second participant follows the golem
            fl.fols.add(a); fl.fols.add(b);
            fl.params = "leader logs out at tick " + (40 + 30 * i) + "; second participant 8 away follows";
        }, (c, x) -> c.assertTrue(x.finalState.equals("ENTRANCE_OPEN") && x.openMismatch == 12, "the second participant takes over, got " + x.finalState));
    }

    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_flows")
    public void flowAllParticipantsFarAndIdle(GameTestHelper ctx) {
        int n = P10Support.measure() ? 4 : 1;
        runFlows(ctx, "E", "all_far_idle", n, n, (fl, i) -> {
            fl.fols.add(follower(-14.0D - 2 * i, 0.0D, 0.2D, 99999, true));
            fl.params = "single participant " + (14 + 2 * i) + " blocks away that never moves";
        }, (c, x) -> c.assertTrue(x.finalState.equals("FALLBACK_OPEN") && x.openTick >= 600 && x.openMismatch == 12, "an absent participant ends in the fallback opening, got " + x.finalState + "@" + x.openTick));
    }

    // -------------------------------------------------------------- S5: replacement opening vs normal arrival race

    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_flows")
    public void flowFallbackVersusArrivalRace(GameTestHelper ctx) {
        // calibrated with RoyalTombGolemGameTests.speedCalibrationEast (BOSS_S route, arrival tick vs MOVEMENT_SPEED): 0.12 -> ~508, 0.11 -> ~574-597, 0.10 -> ~684-710
        double[] speeds = P10Support.measure() ? new double[]{0.120D, 0.115D, 0.112D, 0.111D, 0.1105D, 0.110D, 0.1095D, 0.109D, 0.108D, 0.106D, 0.104D, 0.100D} : new double[]{0.115D, 0.104D};
        runFlows(ctx, "E", "race", speeds.length, speeds.length, (fl, i) -> {
            fl.speedOverride = speeds[i];
            fl.fols.add(follower(-2.0D, 0.0D, 0.3D, 0, true));
            fl.params = String.format(Locale.ROOT, "golem speed attribute %.4f (arrival expected near the 600-tick fallback)", speeds[i]);
        }, (c, x) -> {
            c.assertTrue(x.openMismatch == 12 && (x.finalState.equals("ENTRANCE_OPEN") || x.finalState.equals("FALLBACK_OPEN")), "exactly one opening, only the cover cells changed, got " + x.finalState + " mismatch " + x.openMismatch);
            c.assertTrue(x.finalState.equals("ENTRANCE_OPEN") ? x.openTick <= 600 : x.openTick >= 600, "arrival beats the timer or the timer wins, never both: " + x.finalState + "@" + x.openTick);
        });
    }

    // -------------------------------------------------------------- S6: live-state loss while guiding (the suspected cause of the 300-tick misses)

    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_isolated")
    public void flowLiveStateLossWhileGuiding(GameTestHelper ctx) {
        int n = P10Support.measure() ? 4 : 1;
        runFlows(ctx, "E", "live_state_loss", n, 1, (fl, i) -> {
            fl.fols.add(follower(-2.0D, 0.0D, 0.3D, 0, true));
            fl.interfereAt = new int[]{3, 20, 60, 120}[i];
            fl.params = "SecretEntranceRuntime.forgetLiveState() " + fl.interfereAt + " ticks after the golem appeared";
        }, (c, x) -> {
            // documents the diagnosed mechanism (docs/design/st01-p10 deliverable 4): the golem arrives but the record lost its guide, so only the timer opens
            c.assertTrue(x.finalState.equals("FALLBACK_OPEN") && x.arrivedUndetected >= 0 && x.openMismatch == 12, "live-state loss while guiding ends in the fallback although the golem arrived, got " + x.finalState + " undetected@" + x.arrivedUndetected);
        });
    }

    // -------------------------------------------------------------- S7: chunks unloaded while guiding

    @GameTest(maxTicks = 20000, environment = "moreminecrafts-gametest:p10_flows")
    public void flowChunksUnloadWhileGuiding(GameTestHelper ctx) {
        int n = P10Support.measure() ? 3 : 1;
        runFlows(ctx, "E", "chunk_unload", n, 1, (fl, i) -> {
            fl.releaseChunks = true;
            fl.params = "fixture chunks un-forced while guiding; killer far away";
        }, (c, x) -> c.assertTrue(x.finalState.equals("FALLBACK_OPEN") && x.openMismatch == 12, "unloaded while guiding: the record keeps running, the fallback opens and the cover is applied on reload, got " + x.finalState + " mismatch " + x.openMismatch));
    }
}
