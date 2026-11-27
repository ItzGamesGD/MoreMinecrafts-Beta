package dev.itzgamesgd.moreminecrafts.st01;

import dev.itzgamesgd.moreminecrafts.st01.P10Support.Sched;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

/**
 * ST-01 P10/P11 game tests on the REAL royal-tomb geometry (P6 integrated model, candidate C overlay, CLOSED state), placed block-for-block from
 * tools/st01-p10/export_fixture.py. Part 1: fixture/voxel/coordinate conformance. Part 2: gold-guide-golem route sweeps using the P11 owner-approved
 * production configuration ({@link EntranceConfig#PRODUCTION}, stop point {@code (±9, -2, 3)}, spawn {@code BOSS_S}, scale 0.5, speed 0.25).
 */
public final class RoyalTombGolemGameTests {
    static {
        EntranceConfig.resetToProduction();
    }

    // ------------------------------------------------------------------ part 1: fixture, voxels, coordinates

    @GameTest(maxTicks = 400)
    public void realHallFixtureMatchesTheApprovedVoxelModelEastAndWest(GameTestHelper ctx) {
        RoyalTombFixture e = RoyalTombFixture.load("E"), w = RoyalTombFixture.load("W");
        RoyalTombFixture.Placed pe = e.place(ctx.getLevel(), P10Support.nextOrigin(ctx)), pw = w.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
        long msE = pe.build(), msW = pw.build();
        int[] de = pe.voxelDiff(), dw = pw.voxelDiff();
        System.out.println("P10_FIXTURE build_ms E=" + msE + " W=" + msW + " diff E=" + de[0] + " W=" + dw[0] + " solid E=" + de[1] + "/" + de[2] + " W=" + dw[1] + "/" + dw[2]);
        ctx.assertTrue(de[0] == 0 && dw[0] == 0, "placed world equals the model cell for cell (E " + de[0] + ", W " + dw[0] + " mismatching cells)");
        ctx.assertTrue(de[1] == e.nSolid && dw[1] == w.nSolid && e.nSolid == w.nSolid, "solid counts equal the exporter counts and the sides are equinumerous");
        // exact X mirror of the two exporter-built models (independently produced by the P1-P6 engines for each side)
        int mirrorBad = 0, kindBad = 0;
        for (int x = e.x0; x <= e.x1; x++) for (int y = e.y0; y <= e.y1; y++) for (int z = e.z0; z <= e.z1; z++) {
            if (e.modelSolid(x, y, z) != w.modelSolid(-x, y, z)) mirrorBad++;
            if (!e.modelKind(x, y, z).equals(w.modelKind(-x, y, z))) kindBad++;
        }
        System.out.println("P10_MIRROR modelSolidMismatch=" + mirrorBad + " modelKindMismatch=" + kindBad);
        ctx.assertTrue(mirrorBad == 0 && kindBad == 0, "WEST model is the exact X mirror of EAST (solid " + mirrorBad + ", element kind " + kindBad + ")");
        // the same property in the placed worlds
        int worldMirrorBad = 0;
        for (int x = e.x0; x <= e.x1; x++) for (int y = e.y0; y <= e.y1; y++) for (int z = e.z0; z <= e.z1; z++) {
            boolean a = !ctx.getLevel().getBlockState(pe.world(x, y, z)).isAir(), b = !ctx.getLevel().getBlockState(pw.world(-x, y, z)).isAir();
            if (a != b) worldMirrorBad++;
        }
        ctx.assertTrue(worldMirrorBad == 0, "placed EAST and WEST worlds are X mirrors (" + worldMirrorBad + " cells differ)");
        pe.release(); pw.release();
        ctx.succeed();
    }

    @GameTest(maxTicks = 400)
    public void coordinateTransformAnchorCoverStopAndSpawnCellsAreConsistent(GameTestHelper ctx) {
        StringBuilder log = new StringBuilder();
        for (String side : new String[]{"E", "W"}) {
            RoyalTombFixture f = RoyalTombFixture.load(side);
            RoyalTombFixture.Placed p = f.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
            p.build();
            EntranceLayout.Orientation o = f.orientation;
            // 1. cover: EntranceLayout (anchor-relative, P9 code) == exporter cover cells (P6 verified, model coordinates) after the pure translation
            Set<BlockPos> fromLayout = new HashSet<>(EntranceLayout.coverCells(p.anchor, o));
            Set<BlockPos> fromModel = new HashSet<>();
            for (int[] c : f.coverModel) fromModel.add(p.world(c[0], c[1], c[2]));
            ctx.assertTrue(fromLayout.equals(fromModel) && fromLayout.size() == 12, side + ": EntranceLayout cover cells equal the P6 model cover cells (12 cells)");
            for (BlockPos c : fromLayout) ctx.assertTrue(!ctx.getLevel().getBlockState(c).isAir(), side + ": cover cell solid in CLOSED state " + c);
            // 2. the anchor is the royal chest block of the model
            ctx.assertTrue(f.modelKind(p.model(p.anchor).getX(), p.model(p.anchor).getY(), p.model(p.anchor).getZ()).equals("chest_primary"), side + ": anchor sits on CHEST_PRIMARY");
            // 3. neighbours of the cut keep the floor (no hole): the ring around the cover is solid at the cover layer
            Set<BlockPos> ring = new HashSet<>();
            for (BlockPos c : fromLayout) for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { BlockPos n = c.offset(d[0], 0, d[1]); if (!fromLayout.contains(n)) ring.add(n); }
            for (BlockPos n : ring) ctx.assertTrue(!ctx.getLevel().getBlockState(n).isAir(), side + ": floor ring cell solid " + n);
            // 4. stop point and golem spawn: standable (feet and two cells above free, floor solid)
            for (BlockPos cell : new BlockPos[]{EntranceLayout.stopPoint(p.anchor, o), EntranceLayout.golemSpawn(p.anchor, o)}) {
                ctx.assertTrue(standable(ctx.getLevel(), cell), side + ": standable cell " + cell);
            }
            log.append(side).append(" anchor=").append(p.anchor).append(" stop=").append(EntranceLayout.stopPoint(p.anchor, o)).append(" spawn=").append(EntranceLayout.golemSpawn(p.anchor, o)).append("; ");
            p.release();
        }
        System.out.println("P10_COORD " + log);
        ctx.succeed();
    }

    static boolean standable(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && level.getBlockState(feet.above(2)).isAir() && !level.getBlockState(feet.below()).isAir();
    }

    // ------------------------------------------------------------------ part 2: route sweeps

    /** Named start cells in hall-local (u east, v south) offsets from the hall centre; u is mirrored for the WEST fixture so names keep their meaning. */
    record Start(String name, int u, int v, int dy) {
        static final Start BOSS_S = new Start("BOSS_S", 0, 6, 0), BOSS_N = new Start("BOSS_N", 0, -6, 0), BOSS_E = new Start("BOSS_E", 6, 0, 0), BOSS_W = new Start("BOSS_W", -6, 0, 0);
        static final Start DOOR_IN = new Start("DOOR_IN", 0, -15, 0), CORRIDOR = new Start("CORRIDOR_OUT", 0, -21, 0), NW_CORNER = new Start("NW_CORNER", -15, -15, 0);
        static final Start OBELISK_N = new Start("OBELISK_N", 0, -3, 0), DAIS_FRONT = new Start("DAIS_FRONT", 0, 8, 0);
    }

    record Cond(String id, Start start, double scale, double speed, int n, int stagger, int[] stopDelta, boolean openFirst) {
        Cond(String id, Start start, double scale, double speed, int n, int stagger) { this(id, start, scale, speed, n, stagger, null, false); }
        Cond(String id, Start start, double scale, double speed, int n, int stagger, int[] stopDelta) { this(id, start, scale, speed, n, stagger, stopDelta, false); }
        Cond opening() { return new Cond(id, start, scale, speed, n, stagger, stopDelta, true); }
    }

    static final double[] RADII = {0.5, 1.0, 1.23, 1.5, 2.0};
    static final int TIMEOUT = 1500, AFTER_ARRIVAL = 80;

    static final class Trial {
        GoldGuideGolem g; int born, idx;
        final int[] reach = new int[RADII.length];
        int rest = -1, noPath, still, overlapTicks, fellTick = -1, removedTick = -1, firstPath = -1, stall, stallRun, lostPath;
        double maxDrift, minGap = 1e9, travelled, restDx, restDz, restGap = Double.NaN, restHitboxOverlapDepth;
        Vec3 last, atMain; int mainTick = -1; int stillRun;
        boolean done; String outcome = "TIMEOUT";
        double finalDx, finalDz, finalY;
        Trial() { java.util.Arrays.fill(reach, -1); }
    }

    /** Runs conditions sequentially; golems inside one condition run concurrently (team collision NEVER, so they do not push each other). */
    static final class Sweep {
        final GameTestHelper ctx; final RoyalTombFixture.Placed p; final Sched s; final String tag; final String label;
        final ArrayDeque_<Cond> queue = new ArrayDeque_<>();
        Cond cur; int spawned, condStart; List<Trial> trials = new ArrayList<>(); final List<String> rows = new ArrayList<>(); final Runnable onDone;
        final BlockPos defaultStop; BlockPos stop; double sx, sz; final double coverMinX, coverMaxX, coverMinZ, coverMaxZ; final PlayerTeam team; final Random rng = new Random(0x5710L);
        final List<String> summary = new ArrayList<>(); int condIndex; boolean coverOpened; int badTrials; final List<String> badWhere = new ArrayList<>();

        Sweep(GameTestHelper ctx, RoyalTombFixture.Placed p, Sched s, String label, List<Cond> conds, Runnable onDone) {
            this.ctx = ctx; this.p = p; this.s = s; this.label = label; this.tag = p.fixture().side; this.onDone = onDone;
            queue.addAll(conds);
            defaultStop = EntranceLayout.stopPoint(p.anchor, p.fixture().orientation);
            setStop(defaultStop);
            List<BlockPos> cover = EntranceLayout.coverCells(p.anchor, p.fixture().orientation);
            coverMinX = cover.stream().mapToInt(BlockPos::getX).min().getAsInt(); coverMaxX = cover.stream().mapToInt(BlockPos::getX).max().getAsInt() + 1;
            coverMinZ = cover.stream().mapToInt(BlockPos::getZ).min().getAsInt(); coverMaxZ = cover.stream().mapToInt(BlockPos::getZ).max().getAsInt() + 1;
            var sb = p.level.getScoreboard();
            String tn = "p10_" + label + "_" + Integer.toHexString(System.identityHashCode(this));
            team = sb.addPlayerTeam(tn.length() > 16 ? tn.substring(0, 16) : tn);
            team.setCollisionRule(Team.CollisionRule.NEVER);
            s.each(this::tick);
        }

        void setStop(BlockPos st) { stop = st; sx = st.getX() + 0.5D; sz = st.getZ() + 0.5D; }

        BlockPos startCell(Start st) {
            int sign = p.fixture().orientation.sign;
            RoyalTombFixture f = p.fixture();
            return p.world(sign > 0 ? f.hallCx + st.u : f.hallCx - st.u, f.hallFloor + st.dy, f.hallCz + st.v);
        }

        void tick() {
            if (cur == null) {
                if (queue.isEmpty()) return;
                cur = queue.poll(); spawned = 0; condStart = s.tick; trials = new ArrayList<>();
                BlockPos c = startCell(cur.start());
                setStop(cur.stopDelta() == null ? defaultStop : defaultStop.offset(p.fixture().orientation.sign * cur.stopDelta()[0], 0, cur.stopDelta()[1]));
                if (!standable(p.level, c)) System.out.println("P10_WARN start cell not standable: " + cur.id() + " " + c);
            }
            if (spawned < cur.n() && (s.tick - condStart) % Math.max(1, cur.stagger()) == 0) spawn();
            boolean allDone = spawned >= cur.n();
            for (Trial t : trials) { if (!t.done) { observe(t); allDone &= t.done; } }
            if (allDone) finishCondition();
        }

        void spawn() {
            BlockPos c = startCell(cur.start());
            GoldGuideGolem g = SecretEntranceEntities.GOLD_GUIDE_GOLEM.create(p.level, EntitySpawnReason.EVENT);
            g.getAttribute(Attributes.SCALE).setBaseValue(cur.scale());
            g.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(cur.speed());
            g.refreshDimensions();
            Trial t = new Trial(); t.idx = spawned++; t.g = g; t.born = s.tick;
            double jx = (rng.nextDouble() - 0.5D) * 0.6D, jz = (rng.nextDouble() - 0.5D) * 0.6D;
            g.snapTo(c.getX() + 0.5D + jx, c.getY(), c.getZ() + 0.5D + jz, rng.nextFloat() * 360.0F, 0.0F);
            g.configureGuide(p.anchor.asLong(), stop);   // per-condition stop
            p.level.addFreshEntity(g);
            p.level.getScoreboard().addPlayerToTeam(g.getScoreboardName(), team);
            t.last = g.position();
            trials.add(t);
        }

        double gap(AABB b) {
            double gx = Math.max(0.0D, Math.max(coverMinX - b.maxX, b.minX - coverMaxX)), gz = Math.max(0.0D, Math.max(coverMinZ - b.maxZ, b.minZ - coverMaxZ));
            return Math.hypot(gx, gz);
        }

        void observe(Trial t) {
            GoldGuideGolem g = t.g; int age = s.tick - t.born;
            if (!g.isAlive() || g.isRemoved()) { t.done = true; t.outcome = "REMOVED"; t.removedTick = age; return; }
            Vec3 pos = g.position();
            double dx = pos.x - sx, dz = pos.z - sz, d = Math.hypot(dx, dz);
            boolean grounded = g.onGround() && Math.abs(pos.y - stop.getY()) < 2.0D;
            double step = pos.distanceTo(t.last); t.travelled += step; t.last = pos;
            boolean hasPath = g.getNavigation().getPath() != null;
            if (hasPath && t.firstPath < 0) t.firstPath = age;
            if (!hasPath && t.mainTick < 0) { t.noPath++; if (t.firstPath >= 0) t.lostPath++; }
            if (step < 0.004D) { t.still++; if (t.mainTick < 0 && t.firstPath >= 0) { t.stallRun++; t.stall = Math.max(t.stall, t.stallRun); } } else t.stallRun = 0;
            if (pos.y < stop.getY() - 1.5D && t.fellTick < 0) t.fellTick = age;
            for (int i = 0; i < RADII.length; i++) if (t.reach[i] < 0 && grounded && d <= RADII[i]) t.reach[i] = age;
            if (t.mainTick < 0 && t.reach[3] >= 0) {
                t.mainTick = age; t.atMain = pos;
                if (cur.openFirst() && !coverOpened) { coverOpened = true; for (BlockPos c : EntranceLayout.coverCells(p.anchor, p.fixture().orientation)) p.level.setBlock(c, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); }
            }
            if (t.mainTick >= 0) {
                t.maxDrift = Math.max(t.maxDrift, pos.distanceTo(t.atMain));
                double gp = gap(g.getBoundingBox());
                t.minGap = Math.min(t.minGap, gp);
                if (gp <= 0.0D) t.overlapTicks++;
                if (step < 0.01D) t.stillRun++; else t.stillRun = 0;
                if (t.rest < 0 && t.stillRun >= 10) { t.rest = age; t.restDx = dx; t.restDz = dz; t.restGap = gp; }
                if (age - t.mainTick >= AFTER_ARRIVAL) finish(t, "ARRIVED", dx, dz, pos.y);
            } else if (age >= TIMEOUT) finish(t, "TIMEOUT", dx, dz, pos.y);
        }

        void finish(Trial t, String outcome, double dx, double dz, double y) {
            t.done = true; t.outcome = outcome; t.finalDx = dx; t.finalDz = dz; t.finalY = y;
        }

        void finishCondition() {
            final BlockPos stop = this.stop;
            double tickMs = p.level.getServer().getAverageTickTimeNanos() / 1.0e6D;
            int ok = 0; List<Integer> times = new ArrayList<>();
            for (Trial t : trials) {
                if (t.outcome.equals("ARRIVED")) { ok++; times.add(t.reach[3]); }
                if (!t.outcome.equals("ARRIVED") || t.fellTick >= 0) { badTrials++; badWhere.add(cur.id() + "#" + t.idx + ":" + t.outcome + (t.fellTick >= 0 ? " fell@" + t.fellTick : "")); }
                rows.add(String.join(",", tag, label, cur.id(), cur.start().name(), fmt(cur.scale()), fmt(cur.speed()), cur.stopDelta() == null ? "0" : Integer.toString(cur.stopDelta()[0]), cur.stopDelta() == null ? "0" : Integer.toString(cur.stopDelta()[1]), Integer.toString(t.idx), t.outcome,
                        Integer.toString(t.reach[0]), Integer.toString(t.reach[1]), Integer.toString(t.reach[2]), Integer.toString(t.reach[3]), Integer.toString(t.reach[4]),
                        Integer.toString(t.rest), fmt(t.restDx), fmt(t.restDz), fmt(t.restGap), fmt(t.maxDrift), fmt(t.minGap == 1e9 ? Double.NaN : t.minGap), Integer.toString(t.overlapTicks),
                        Integer.toString(t.noPath), Integer.toString(t.still), Integer.toString(t.firstPath), Integer.toString(t.lostPath), Integer.toString(t.stall), fmt(t.travelled), Integer.toString(t.fellTick), fmt(t.finalDx), fmt(t.finalDz), fmt(t.finalY - stop.getY()), fmt(tickMs), Integer.toString(t.born - condStart)));
                t.g.discard();
            }
            times.sort(null);
            String med = times.isEmpty() ? "-" : Integer.toString(times.get(times.size() / 2));
            String line = "P10_COND " + tag + " " + label + " " + cur.id() + " ok=" + ok + "/" + trials.size() + " median=" + med + " max=" + (times.isEmpty() ? "-" : times.get(times.size() - 1)) + " tickMs=" + fmt(tickMs);
            System.out.println(line); summary.add(line);
            if (coverOpened) { p.restoreCover(); coverOpened = false; }
            cur = null;
            if (queue.isEmpty()) { p.level.getScoreboard().removePlayerTeam(team); P10Support.csv("route_trials_" + label + ".csv", HEADER, rows); onDone.run(); }
        }
    }

    static final String HEADER = "side,label,cond,start,scale,speed,stop_dx,stop_dz,trial,outcome,t_r05,t_r10,t_r123,t_r15,t_r20,rest_tick,rest_dx,rest_dz,rest_gap,max_drift,min_gap,overlap_ticks,nopath_ticks,still_ticks,first_path,lost_path,max_stall,travelled,fell_tick,final_dx,final_dz,final_dy,avg_tick_ms,spawn_offset";
    static String fmt(double v) { return Double.isNaN(v) ? "" : String.format(Locale.ROOT, "%.4f", v); }

    /** Plain FIFO (java.util.ArrayDeque is fine; this tiny wrapper just keeps imports short). */
    static final class ArrayDeque_<T> extends java.util.ArrayDeque<T> {}

    static List<Cond> baseConditions(int n, int stagger) {
        List<Cond> c = new ArrayList<>();
        for (Start st : new Start[]{Start.BOSS_S, Start.BOSS_N, Start.BOSS_E, Start.BOSS_W, Start.DOOR_IN, Start.CORRIDOR, Start.NW_CORNER, Start.OBELISK_N, Start.DAIS_FRONT})
            c.add(new Cond("base:" + st.name(), st, GoldGuideGolemTuning.SCALE, GoldGuideGolemTuning.MOVEMENT_SPEED, n, stagger));
        return c;
    }

    static List<Cond> o6Conditions(int n, int stagger) {
        List<Cond> c = new ArrayList<>();
        for (Start st : new Start[]{Start.BOSS_S, Start.NW_CORNER}) {
            for (double sc : new double[]{0.4D, 0.6D, 0.75D, 1.0D}) c.add(new Cond("scale" + sc + ":" + st.name(), st, sc, GoldGuideGolemTuning.MOVEMENT_SPEED, n, stagger));
            for (double sp : new double[]{0.15D, 0.35D, 0.5D}) c.add(new Cond("speed" + sp + ":" + st.name(), st, GoldGuideGolemTuning.SCALE, sp, n, stagger));
        }
        return c;
    }

    /** P7 arrival-zone candidates Z1..Z4 (cell-centre to cover-rectangle distance), enumerated on the placed hall floor relative to the P9/P10 Z2 reference cell; start BOSS_S. */
    static List<Cond> zoneConditions(RoyalTombFixture.Placed p, Start from, int n, int stagger) {
        List<Cond> out = new ArrayList<>();
        RoyalTombFixture f = p.fixture();
        List<BlockPos> cover = EntranceLayout.coverCells(p.anchor, f.orientation);
        int cMinX = cover.stream().mapToInt(BlockPos::getX).min().getAsInt(), cMaxX = cover.stream().mapToInt(BlockPos::getX).max().getAsInt();
        int cMinZ = cover.stream().mapToInt(BlockPos::getZ).min().getAsInt(), cMaxZ = cover.stream().mapToInt(BlockPos::getZ).max().getAsInt();
        BlockPos def = p.anchor.offset(f.orientation.sign * 10, -2, 3);
        int fy = def.getY();
        for (int dx = -40; dx <= 40; dx++) for (int dz = -40; dz <= 40; dz++) {
            BlockPos c = new BlockPos(def.getX() + dx, fy, def.getZ() + dz);
            if (!p.fixture().inRegion(c.getX() - p.origin.getX(), c.getY() - p.origin.getY(), c.getZ() - p.origin.getZ()) || !standable(p.level, c)) continue;
            if (cover.contains(c.below())) continue;
            double ex = Math.max(0.0D, Math.max(cMinX - (c.getX() + 0.5D), (c.getX() + 0.5D) - (cMaxX + 1)));
            double ez = Math.max(0.0D, Math.max(cMinZ - (c.getZ() + 0.5D), (c.getZ() + 0.5D) - (cMaxZ + 1)));
            double d = Math.hypot(ex, ez);
            String zone = d < 0.6D ? "Z0" : d < 0.8D ? "Z1" : d < 1.7D ? "Z2" : d < 2.7D ? "Z3" : d < 3.7D ? "Z4" : null;
            if (zone == null) continue;
            if (!reachableFromHallFloor(p, c)) continue;
            out.add(new Cond("zone:" + zone + "@" + String.format(Locale.ROOT, "%.3f", d) + ":" + dx + ";" + dz, from, GoldGuideGolemTuning.SCALE, GoldGuideGolemTuning.MOVEMENT_SPEED, n, stagger, new int[]{f.orientation.sign * dx, dz}));
        }
        return out;
    }

    /** Cheap guard: the cell must be a free hall-floor cell (the walking reachability itself is what the sweep measures). */
    static boolean reachableFromHallFloor(RoyalTombFixture.Placed p, BlockPos c) { return standable(p.level, c); }

    private void runSweep(GameTestHelper ctx, String side, String label, List<Cond> conds) { runSweep(ctx, side, label, conds, null, false); }

    private void runSweep(GameTestHelper ctx, String side, String label, List<Cond> conds, java.util.function.Function<RoyalTombFixture.Placed, List<Cond>> late) { runSweep(ctx, side, label, conds, late, false); }

    /** @param strict every golem must arrive and none may fall (regression check); measurement sweeps that expect failures pass false. */
    private void runSweep(GameTestHelper ctx, String side, String label, List<Cond> conds, java.util.function.Function<RoyalTombFixture.Placed, List<Cond>> late, boolean strict) {
        Sched s = new Sched(ctx);
        RoyalTombFixture f = RoyalTombFixture.load(side);
        RoyalTombFixture.Placed p = f.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
        long ms = p.build();
        System.out.println("P10_SWEEP " + side + " " + label + " fixture built in " + ms + " ms; conditions " + conds.size());
        // entity storage of far chunks loads asynchronously
        java.util.concurrent.atomic.AtomicBoolean started = new java.util.concurrent.atomic.AtomicBoolean();
        s.each(() -> {
            if (started.get()) return;
            for (var c : p.chunks()) if (!p.level.areEntitiesLoaded(c.pack())) return;
            started.set(true);
            Sweep[] sw = new Sweep[1];
            sw[0] = new Sweep(ctx, p, s, label, late != null ? late.apply(p) : conds, () -> {
                p.release();
                if (strict) ctx.assertTrue(sw[0].badTrials == 0, side + " " + label + ": every golem must reach the stop point without falling, bad: " + sw[0].badWhere);
                ctx.succeed();
            });
        });
    }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void routeSweepBaseEast(GameTestHelper ctx) { runSweep(ctx, "E", "base", baseSet(), null, true); }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void routeSweepBaseWest(GameTestHelper ctx) { runSweep(ctx, "W", "base", baseSet(), null, true); }

    /** Full measurement set (9 starts x 30) or, by default, a regression subset (3 starts x 3: the P9 spawn, the door and the farthest corner). */
    static List<Cond> baseSet() {
        if (P10Support.measure()) return baseConditions(30, 3);
        List<Cond> out = new ArrayList<>();
        for (Cond c : baseConditions(3, 3)) if (c.start() == Start.BOSS_S || c.start() == Start.DOOR_IN || c.start() == Start.NW_CORNER) out.add(c);
        return out;
    }

    private static boolean inert(GameTestHelper ctx) {
        if (P10Support.measure()) return false;
        ctx.succeed(); return true;      // measurement-only test: runs only with -Dmoreminecrafts.p10.measure=true
    }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void arrivalZoneSweepEast(GameTestHelper ctx) { if (inert(ctx)) return; runSweep(ctx, "E", "zones", List.of(), p -> zoneConditions(p, Start.BOSS_S, 6, 3)); }

    /**
     * Same routes while every server tick is deliberately stretched by a busy wait. Arrival measured in TICKS should not depend on wall time unless
     * something in the navigation uses the wall clock (vanilla PathNavigation's per-node timeout does: it accumulates Util.getMillis()).
     */
    private void loadSweep(GameTestHelper ctx, long burnMs, String label) {
        Sched burner = new Sched(ctx);
        burner.each(() -> { long end = System.nanoTime() + burnMs * 1_000_000L; while (System.nanoTime() < end) Thread.onSpinWait(); });
        runSweep(ctx, "E", label, List.of(new Cond("load:BOSS_S", Start.BOSS_S, GoldGuideGolemTuning.SCALE, GoldGuideGolemTuning.MOVEMENT_SPEED, 12, 3),
                new Cond("load:DOOR_IN", Start.DOOR_IN, GoldGuideGolemTuning.SCALE, GoldGuideGolemTuning.MOVEMENT_SPEED, 6, 3)));
    }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_load")
    public void routeSweepUnderTickLoadEast(GameTestHelper ctx) { if (inert(ctx)) return; loadSweep(ctx, 60L, "load60ms"); }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_load_150")
    public void routeSweepUnderTickLoad150East(GameTestHelper ctx) { if (inert(ctx)) return; loadSweep(ctx, 150L, "load150ms"); }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_load_250")
    public void routeSweepUnderTickLoad250East(GameTestHelper ctx) { if (inert(ctx)) return; loadSweep(ctx, 250L, "load250ms"); }

    /** Calibration for the fallback-race flows: BOSS_S arrival tick as a function of the movement-speed attribute (the relation is not linear). */
    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void speedCalibrationEast(GameTestHelper ctx) { if (inert(ctx)) return;
        List<Cond> c = new ArrayList<>();
        for (double sp : new double[]{0.060D, 0.065D, 0.070D, 0.075D, 0.080D, 0.085D, 0.090D, 0.095D, 0.100D, 0.110D, 0.120D}) c.add(new Cond("calib" + sp, Start.BOSS_S, GoldGuideGolemTuning.SCALE, sp, 3, 3));
        runSweep(ctx, "E", "calib", c);
    }

    /** O-6: does a bigger golem still get through the real 3x3 door / 2-wide aisles? Start outside the door (CORRIDOR_OUT), scale varied one variable at a time. */
    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void extraDoorPassageScaleSweepEast(GameTestHelper ctx) { if (inert(ctx)) return;
        List<Cond> c = new ArrayList<>();
        for (double sc : new double[]{0.5D, 0.75D, 1.0D, 1.1D, 1.2D, 1.5D}) c.add(new Cond("door-scale" + sc + ":CORRIDOR_OUT", Start.CORRIDOR, sc, GoldGuideGolemTuning.MOVEMENT_SPEED, 6, 3));
        runSweep(ctx, "E", "door", c);
    }

    /** Same stop-cell candidates, but approached from the door side (the route that ends beside the cover on its south edge). */
    @GameTest(maxTicks = 60000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void extraArrivalZoneSweepFromDoorEast(GameTestHelper ctx) { if (inert(ctx)) return; runSweep(ctx, "E", "zones_door", List.of(), p -> zoneConditions(p, Start.DOOR_IN, 4, 3)); }

    /** The cover is removed at the first golem arrival (like the runtime) and the golems keep moving: does anything fall into the opening? */
    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void extraGolemBesideTheOpenedCoverEast(GameTestHelper ctx) {
        int n = P10Support.measure() ? 6 : 2;
        List<Cond> c = new ArrayList<>();
        for (Start st : P10Support.measure() ? new Start[]{Start.BOSS_S, Start.BOSS_E, Start.DOOR_IN, Start.OBELISK_N} : new Start[]{Start.BOSS_S, Start.BOSS_E, Start.DOOR_IN})
            c.add(new Cond("open-scale0.5:" + st.name(), st, GoldGuideGolemTuning.SCALE, GoldGuideGolemTuning.MOVEMENT_SPEED, n, 3).opening());
        if (P10Support.measure()) for (double sc : new double[]{0.75D, 1.0D}) c.add(new Cond("open-scale" + sc + ":BOSS_E", Start.BOSS_E, sc, GoldGuideGolemTuning.MOVEMENT_SPEED, n, 3).opening());
        runSweep(ctx, "E", "open", c, null, true);
    }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void extraGolemBesideTheOpenedCoverWest(GameTestHelper ctx) {
        int n = P10Support.measure() ? 6 : 2;
        List<Cond> c = new ArrayList<>();
        for (Start st : new Start[]{Start.BOSS_S, Start.BOSS_E, Start.DOOR_IN}) c.add(new Cond("open-scale0.5:" + st.name(), st, GoldGuideGolemTuning.SCALE, GoldGuideGolemTuning.MOVEMENT_SPEED, n, 3).opening());
        runSweep(ctx, "W", "open", c, null, true);
    }

    @GameTest(maxTicks = 40000, environment = "moreminecrafts-gametest:p10_sweeps")
    public void golemConfigSweepEast(GameTestHelper ctx) { if (inert(ctx)) return; runSweep(ctx, "E", "o6", o6Conditions(20, 3)); }
}
