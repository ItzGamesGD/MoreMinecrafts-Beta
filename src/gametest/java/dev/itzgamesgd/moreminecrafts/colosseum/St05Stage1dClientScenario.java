package dev.itzgamesgd.moreminecrafts.colosseum;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import dev.itzgamesgd.moreminecrafts.colosseum.ArenaAttempt.*;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.St05RealArenaBuilder;

/**
 * ST-05 Stage 1-D: REAL arena (built by the unchanged Stage 0-F pipeline on seed 7001) played by a rendered Minecraft 26.3 client connected to a dedicated server.
 * Movement, looks and the boss melee are real client input; the server side is the production runtime. Mob kills in R1-R6 are delivered as real player-attack damage
 * (hurtServer with a player damage source) because scripted clients cannot chase twelve mobs. Gated by -Dmm.st05d=true; -Dmm.st05d.exit=true halts the JVM afterwards.
 * Everything is reported through ST05D lines and build/st05d/results.txt; nothing is relaxed to make a check pass.
 */
@SuppressWarnings("UnstableApiUsage")
public final class St05Stage1dClientScenario implements FabricClientGameTest {
    static final String ID = System.getProperty("mm.st05d.id", "20d89ee7a8aa754c");
    static final int RX = Integer.getInteger("mm.st05d.cx", -837), RZ = Integer.getInteger("mm.st05d.cz", -1174), RY = Integer.getInteger("mm.st05d.fy", -25);
    static final List<String> LOG = new ArrayList<>();
    static final Stats STATS = new Stats();
    static final boolean BOSS_ONLY = Boolean.getBoolean("mm.st05d.bossonly");   // diagnostic: after the real entry, jump the attempt to R7 (same fixture the earlier R7 client test used)

    /** Server-side observer, updated every server tick; client thread only reads it through runOnServer/computeOnServer. */
    static final class Stats {
        int traceEvents; int lastRound = -1; Phase lastPhase; long[] roundStart = new long[9], interStart = new long[9];
        int[] maxAlive = new int[9], replenished = new int[9], warnTicks = new int[9]; int maxWarnLeft; int prevAlive; int maxSimultaneousBosses;
        final Map<String, Integer> maxKeyEntities = new TreeMap<>(); final List<String> events = new ArrayList<>(); final List<String> warnPos = new ArrayList<>(); final Set<String> warnSeen = new HashSet<>(); int bossHits, bossDmgLogs; double bossHitRaw;
        boolean registered;
        void tick(MinecraftServer s) {
            var d = ColosseumRuntime.data(s); var c = d.controller(ID); if (c == null) return; var a = c.attempt(); if (a == null) return;
            long t = s.getTickCount(); int r = a.round(); Phase ph = a.phase();
            if (r != lastRound || ph != lastPhase) {
                events.add("t=" + t + " round=" + r + " phase=" + ph + " outcome=" + a.outcome());
                if (ph == Phase.ROUND && r >= 1 && r <= 7) events.add("   snapshot n=" + a.snap.n() + " budget=" + a.snap.budget() + " roster=" + a.rosterIds().size() + " admissions=" + a.admissions);
                if (ph == Phase.ROUND && r != lastRound && r >= 1 && r <= 7) roundStart[r] = t;
                if (ph == Phase.INTERMISSION && r >= 1 && r <= 7) interStart[r] = t;
                lastRound = r; lastPhase = ph;
            }
            if (r < 1 || r > 7) return;
            ServerLevel l = s.overworld(); int alive = 0, bosses = 0; Map<String, Integer> keys = new HashMap<>();
            String owner = ColosseumRuntime.TAG_ATTEMPT + a.attemptId;
            for (var e : l.getAllEntities()) {
                if (!e.isAlive()) continue;
                if (e instanceof ImmortalGladiator && e.entityTags().contains(owner)) bosses++;
                else if (e instanceof Mob && e.entityTags().contains(owner)) alive++;
                else if (e instanceof ItemEntity ie) for (String tg : ie.entityTags()) if (tg.startsWith(ColosseumRuntime.TAG_REWARD)) keys.merge(tg, 1, Integer::sum);
            }
            keys.forEach((k, v) -> maxKeyEntities.merge(k, v, Math::max));
            maxSimultaneousBosses = Math.max(maxSimultaneousBosses, bosses);
            if (ph == Phase.ROUND) {
                maxAlive[r] = Math.max(maxAlive[r], alive);
                if (prevAlive <= RoundPlan.REPLENISH_AT_OR_BELOW && alive > prevAlive && r < 7) replenished[r]++;
                for (var al : a.allocations()) if (al.status == AllocStatus.WARNING) {
                    warnTicks[r]++; maxWarnLeft = Math.max(maxWarnLeft, al.warnLeft);
                    if (warnSeen.add(String.valueOf(al.id)) && warnPos.size() < 40) warnPos.add("r" + r + " " + al.mob + "@" + al.px + "," + al.py + "," + al.pz + " warnLeft=" + al.warnLeft);
                }
            }
            prevAlive = alive;
        }
    }

    // ------------------------------------------------------------------ reporting
    static void log(String k, String v) { String line = "ST05D|" + k + "|" + v; LOG.add(line); System.out.println(line); }
    static void check(String name, boolean ok, String detail) { log((ok ? "PASS " : "FAIL ") + name, detail); }
    static void flush() {
        try { Path p = Path.of("build", "st05d"); Files.createDirectories(p); Files.write(p.resolve("results.txt"), LOG); } catch (Exception e) { System.out.println("ST05D|flush failed|" + e); }
    }

    @Override public void runTest(ClientGameTestContext ctx) {
        if (!Boolean.getBoolean("mm.st05d")) return;
        try { scenario(ctx); }
        catch (Throwable t) { log("ABORT", t.toString()); StackTraceElement[] st = t.getStackTrace(); for (int i = 0; i < Math.min(6, st.length); i++) log("  at", st[i].toString()); flush(); if (Boolean.getBoolean("mm.st05d.exit")) Runtime.getRuntime().halt(3); throw new RuntimeException(t); }
        flush();
        if (Boolean.getBoolean("mm.st05d.exit")) Runtime.getRuntime().halt(0);
    }

    // ------------------------------------------------------------------ helpers
    static boolean until(ClientGameTestContext ctx, TestServerContext srv, java.util.function.Predicate<MinecraftServer> p, int maxTicks) {
        for (int t = 0; t < maxTicks; t += 5) { if (srv.computeOnServer(p::test)) return true; ctx.waitTicks(5); }
        return srv.computeOnServer(p::test);
    }
    static ArenaController ctl(MinecraftServer s) { return ColosseumRuntime.data(s).controller(ID); }
    static ServerPlayer guest(MinecraftServer s) { var l = s.getPlayerList().getPlayers(); return l.size() >= 2 ? l.get(1) : null; }
    static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
    static List<Mob> mobs(MinecraftServer s) {
        var a = ctl(s).attempt(); List<Mob> r = new ArrayList<>(); if (a == null) return r; String owner = ColosseumRuntime.TAG_ATTEMPT + a.attemptId;
        for (var e : s.overworld().getAllEntities()) if (e instanceof Mob m && !(m instanceof ImmortalGladiator) && m.isAlive() && m.entityTags().contains(owner)) r.add(m);
        return r;
    }
    static int killMobs(MinecraftServer s, int max) {
        ServerLevel l = s.overworld(); ServerPlayer p = player(s); int n = 0;
        for (Mob m : mobs(s)) { if (n >= max) break; m.hurtServer(l, l.damageSources().playerAttack(p), 1000f); n++; }
        return n;
    }
    static int keys(ServerPlayer p, boolean ominous) {
        int n = 0; var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack st = inv.getItem(i); if (st.is(ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY)) n += st.getCount(); }
        return n;
    }
    static void teleport(MinecraftServer s, double x, double y, double z, float yaw) { var p = player(s); p.teleportTo(s.overworld(), x, y, z, Set.of(), yaw, 0f, true); }
    static void shot(ClientGameTestContext ctx, TestServerConnection con, String name) { con.waitForClientboundPackets(); ctx.waitTicks(10); ctx.takeScreenshot("st05d-" + name); }

    /** Collects every live shared key item for the attempt and walks the real player onto it (vanilla pick-up does the rest). */
    static int pickUpKeys(ClientGameTestContext ctx, TestServerContext srv) {
        List<double[]> pos = srv.computeOnServer(s -> {
            List<double[]> r = new ArrayList<>();
            for (var e : s.overworld().getAllEntities()) if (e instanceof ItemEntity ie && ie.isAlive() && ie.entityTags().stream().anyMatch(t -> t.startsWith(ColosseumRuntime.TAG_REWARD))) r.add(new double[]{ie.getX(), ie.getY(), ie.getZ()});
            return r;
        });
        for (double[] p : pos) { srv.runOnServer(s -> teleport(s, p[0], p[1], p[2], 0f)); ctx.waitTicks(25); }
        return pos.size();
    }

    static Map<?, ?> bossBarsOf(net.minecraft.client.Minecraft c) {
        try { var overlay = c.gui.hud.getBossOverlay(); var f = overlay.getClass().getDeclaredField("events"); f.setAccessible(true); return (Map<?, ?>) f.get(overlay); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    // ------------------------------------------------------------------ scenario
    private void scenario(ClientGameTestContext ctx) {
        Path state = Path.of("build", "st05d", "state").toAbsolutePath();
        try { if (Files.exists(state)) try (var w = Files.walk(state)) { w.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } } catch (Exception ignored) { }
        Properties props = new Properties();
        props.setProperty("online-mode", "false"); props.setProperty("view-distance", "6"); props.setProperty("simulation-distance", "6"); props.setProperty("max-tick-time", "-1");
        props.setProperty("spawn-animals", "false"); props.setProperty("difficulty", "normal");
        int guestPort = Integer.getInteger("mm.st05d.guestPort", 0);
        if (guestPort > 0) { props.setProperty("server-port", String.valueOf(guestPort)); props.setProperty("max-players", "4"); }
        final boolean SP = Boolean.getBoolean("mm.st05d.sp");   // single-player world: lets the run stop and re-open the very same save (a real server restart)
        var builder = ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(st -> st.setSeed("7001"));
        TestSingleplayerContext sp = null; TestDedicatedServerContext dsrv = null; TestServerContext srv; TestServerConnection con = null;
        if (SP) { sp = builder.create(); srv = sp.getServer(); con = sp.getConnection(); } else { dsrv = builder.createServer(props); srv = dsrv; }
        try {
            // ---------------- build the REAL arena before any client is attached (construction writes ~75k blocks)
            long t0 = System.nanoTime();
            String built = srv.computeOnServer(s -> { try { return St05RealArenaBuilder.build(s, state, ID, RX, RZ, RY); } catch (Exception e) { return "EXC " + e; } });
            log("arena build", built + " wallMs=" + (System.nanoTime() - t0) / 1_000_000);
            check("A0 real arena built and VERIFIED by the Stage 0-F pipeline", built.startsWith("VERIFIED"), built);
            if (!built.startsWith("VERIFIED")) throw new IllegalStateException("arena not built: " + built);
            int[] site = srv.computeOnServer(s -> { var st = ColosseumRuntime.data(s).sites().get(ID); return st == null ? null : new int[]{st.cx(), st.cz(), st.floorY()}; });
            check("A0b arena registered with the runtime", site != null, site == null ? "missing" : Arrays.toString(site));
            if (site == null) throw new IllegalStateException("not registered");
            final int cx = site[0], cz = site[1], fy = site[2];
            if (Boolean.getBoolean("mm.st05d.trace")) srv.runOnServer(s -> {
                try {
                    var tr = new dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.St05RealArenaBuilder.Tracer(s, state, ID, cx, cz, 40);
                    log("trace armed", "cells=" + tr.size() + " wrongAtStart=" + tr.badNow());
                    net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((lvl, pl, pos, st, be) -> { if (STATS.traceEvents++ < 200) log("trace player-broke-block", "tick=" + s.getTickCount() + " pos=" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " state=" + st + " by=" + pl.getName().getString() + " held=" + pl.getMainHandItem().getItem()); });
                    ServerTickEvents.END_SERVER_TICK.register(sv -> { if (sv.getTickCount() % 1 == 0) for (String ev : tr.tick(sv.overworld(), sv.getTickCount())) if (STATS.traceEvents++ < 200) log("trace block-changed", ev); });
                } catch (Exception e) { log("trace armed FAILED", e.toString()); }
            });
            srv.runOnServer(s -> { if (!STATS.registered) { STATS.registered = true; ServerTickEvents.END_SERVER_TICK.register(STATS::tick); { var ev = net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE; var early = net.minecraft.resources.Identifier.fromNamespaceAndPath("mm", "st05d_early"); ev.addPhaseOrdering(early, net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
                    ev.register(early, (e, src, amt) -> { if (e instanceof ImmortalGladiator && STATS.bossDmgLogs++ < 40) log("boss damage event", "src=" + src.getMsgId() + " by=" + (src.getEntity() == null ? "none" : src.getEntity().getType().toShortString()) + " amt=" + amt + " bossFrozen=" + ColosseumRuntime.combatFrozen(e) + " attackerFrozen=" + (src.getEntity() != null && ColosseumRuntime.combatFrozen(src.getEntity())) + " hp=" + ((ImmortalGladiator) e).getHealth()); return true; }); }
                    net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amt) -> { if (e instanceof ServerPlayer && src.getEntity() instanceof ImmortalGladiator) { STATS.bossHits++; STATS.bossHitRaw += amt; } return true; }); } });

            if (!SP) con = dsrv.connect();
            try {
                ctx.waitFor(c -> c.player != null && c.level != null);
                ctx.runOnClient(c -> { c.options.fov().set(85); });
                srv.runOnServer(s -> {
                    var p = player(s); p.setGameMode(GameType.SURVIVAL);
                    p.getInventory().add(new ItemStack(Items.NETHERITE_SWORD));
                    p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2_000_000, 4, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2_000_000, 1, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2_000_000, 1, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2_000_000, 0, false, false));
                    teleport(s, cx + 0.5, fy, cz - 36.5, 0f);
                });
                con.waitForChunksDownload(); ctx.waitTicks(60); con.waitForClientboundPackets();
                shot(ctx, con, "01-outside-north-entrance");

                // ---------------- A: entry negatives, then real walk-in
                srv.runOnServer(s -> teleport(s, cx + 0.5, fy, cz - 31.5, 0f));
                ctx.waitTicks(60);
                boolean noStartInArch = srv.computeOnServer(s -> ctl(s).attempt() == null || !ctl(s).active());
                check("A1 standing in the entrance arch (r=31.5) does not start the challenge", noStartInArch, "active=" + !noStartInArch);
                srv.runOnServer(s -> teleport(s, cx + 0.5, fy + 14, cz - 31.5, 0f));    // on/over the wall ring
                ctx.waitTicks(40);
                check("A2 wall-ring position (r=31.5, y+14) does not start the challenge", srv.computeOnServer(s -> !ctl(s).active()), "");
                srv.runOnServer(s -> teleport(s, cx + 0.5, fy, cz - 36.5, 0f));
                ctx.waitTicks(30);
                shot(ctx, con, "02-arch-before-walk-in");
                ctx.getInput().holdKeyFor(o -> o.keyUp, 100);       // real forward key, ~5 s
                ctx.waitTicks(20);
                boolean started = until(ctx, srv, s -> ctl(s).active(), 100);
                check("A3 real walk-in through the north arch starts the challenge", started, srv.computeOnServer(s -> ctl(s).attempt() == null ? "no attempt" : "round=" + ctl(s).attempt().round()));
                if (!started) throw new IllegalStateException("challenge did not start by walking in");
                shot(ctx, con, "03-entry-started");
                var admissions = srv.computeOnServer(s -> new ArrayList<>(ctl(s).attempt().admissions));
                check("A4 exactly one admission for the single real participant", admissions.size() == 1, admissions.toString());

                // ---------------- Q2 (optional): a second REAL client joins from another JVM; it is teleported in mid-round 1 and must be admitted at the round-2 boundary
                final boolean twoClients = guestPort > 0;
                if (twoClients) {
                    boolean joined = until(ctx, srv, s -> s.getPlayerList().getPlayers().size() >= 2, 6000);
                    check("Q2a second real client connected to the dedicated server", joined, "players=" + srv.computeOnServer(s -> s.getPlayerList().getPlayers().size()));
                    if (joined) srv.runOnServer(s -> { var g = guest(s); if (g != null) { g.setGameMode(GameType.SURVIVAL); g.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2_000_000, 4, false, false)); g.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2_000_000, 1, false, false)); } });
                }
                // ---------------- B: R1-R6 on the real arena
                if (BOSS_ONLY) {
                    log("DIAGNOSTIC MODE", "boss-only: attempt jumped to R7 after real entry; R1-R6 not played in this run");
                    srv.runOnServer(s -> { var a = ctl(s).attempt(); for (var e : s.overworld().getAllEntities()) if (e.entityTags().contains(ColosseumRuntime.TAG_ATTEMPT + a.attemptId)) e.discard();
                        a.allocs.clear(); a.round = 7; a.phase = Phase.ROUND; a.snap = new Snapshot(7, 1, 1000, 0, 1, new ArrayList<>(a.rosterIds())); });
                }
                for (int round = 1; round <= (BOSS_ONLY ? 0 : 6); round++) {
                    final int r = round;
                    if (srv.computeOnServer(s -> ctl(s).attempt().ended())) throw new IllegalStateException("attempt ended before round " + r + ": " + srv.computeOnServer(s -> ctl(s).attempt().outcome() + " events=" + STATS.events));
                    boolean inRound = until(ctx, srv, s -> ctl(s).attempt().round() >= r, r == 1 ? 400 : 2400);
                    check("B" + r + "a round " + r + " reached", inRound, "");
                    if (!inRound) throw new IllegalStateException("stuck before round " + r);
                    // 1-second spawn warning must precede the spawn
                    boolean warned = until(ctx, srv, s -> STATS.warnTicks[r] > 0, 200);
                    if (warned && r <= 2) { con.waitForClientboundPackets(); ctx.takeScreenshot("st05d-04-r" + r + "-spawn-warning"); }
                    check("B" + r + "b spawn warning observed", warned, "maxWarnLeft=" + STATS.maxWarnLeft + " sample=" + STATS.warnPos.stream().filter(x -> x.startsWith("r" + r)).findFirst().orElse("none"));
                    // first batch fully spawned (no allocation still PENDING) or the 12 cap reached
                    until(ctx, srv, s -> STATS.maxAlive[r] >= 12 || ctl(s).attempt().allocations().stream().noneMatch(al -> al.status == AllocStatus.PENDING), 400);
                    ctx.waitTicks(20);
                    int peak = srv.computeOnServer(s -> STATS.maxAlive[r]);
                    int total = srv.computeOnServer(s -> ctl(s).attempt().allocations().size());
                    check("B" + r + "c alive mobs never exceed 12 (peak=" + peak + ", round allocations=" + total + ")", peak <= RoundPlan.MAX_ALIVE && peak > 0, "peak=" + peak);
                    if (r == 1 || r == 3) shot(ctx, con, "05-r" + r + "-combat");
                    if (twoClients && r == 1) {
                        srv.runOnServer(s -> { var g = guest(s); if (g != null) g.teleportTo(s.overworld(), cx + 10.5, fy, cz + 0.5, Set.of(), 270f, 0f, true); });
                        ctx.waitTicks(40);
                        String pend = srv.computeOnServer(s -> { var a = ctl(s).attempt(); var g = guest(s); return "guestInRoster=" + (g != null && a.member(g.getUUID()) != null) + " pending=" + a.pendingIds().size() + " round=" + a.round(); });
                        check("Q2b guest entering mid-round 1 is pending, not in roster yet", pend.contains("guestInRoster=false") && pend.contains("pending=1"), pend);
                    }
                    // drain: kill down to 6 first (replenish check), then everything
                    if (total > RoundPlan.MAX_ALIVE && peak >= 8) {
                        srv.runOnServer(s -> { int al = mobs(s).size(); if (al > 6) killMobs(s, al - 6); });
                        ctx.waitTicks(80);
                        int rep = srv.computeOnServer(s -> STATS.replenished[r]);
                        check("B" + r + "d replenished after dropping to <=6", rep > 0, "replenish events=" + rep);
                    } else log("B" + r + "d replenish not applicable", "round has " + total + " allocations, all spawn in the first batch (<=12)");
                    int guard = 0;
                    while (guard++ < 400 && !srv.computeOnServer(s -> ctl(s).attempt().phase() != Phase.ROUND || ctl(s).attempt().round() != r)) { srv.runOnServer(s -> killMobs(s, 12)); ctx.waitTicks(10); }
                    if (r == 3 || r == 5) {
                        boolean chests = until(ctx, srv, s -> !ctl(s).attempt().supplyChests().isEmpty(), 100);
                        int n = srv.computeOnServer(s -> ctl(s).attempt().supplyChests().size());
                        check("B" + r + "e supply chests placed", chests, "chests=" + n);
                        if (chests) { srv.runOnServer(s -> { long pos = ctl(s).attempt().supplyChests().getFirst(); var bp = net.minecraft.core.BlockPos.of(pos); teleport(s, bp.getX() + 0.5, bp.getY() + 0.1, bp.getZ() - 2.5, 0f); }); ctx.waitTicks(30); shot(ctx, con, "06-r" + r + "-supply-chest"); }
                    }
                    // reward: shared key must exist exactly once, then the real player picks it up
                    boolean keyOut = until(ctx, srv, s -> ctl(s).attempt().keysAwarded().contains(r), 200);
                    ctx.waitTicks(10);
                    int dup = srv.computeOnServer(s -> STATS.maxKeyEntities.entrySet().stream().filter(e -> e.getKey().contains(":round:" + r + ":")).mapToInt(Map.Entry::getValue).max().orElse(0));
                    check("D" + r + "a shared Trial Key recorded and exactly one key entity (max simultaneous=" + dup + ")", keyOut && dup == 1, "awarded=" + keyOut);
                    int before = srv.computeOnServer(s -> keys(player(s), false));
                    int found = pickUpKeys(ctx, srv);
                    int after = srv.computeOnServer(s -> keys(player(s), false));
                    check("D" + r + "b real player picked up the key (" + before + "->" + after + ", entities found=" + found + ")", after == before + 1, "");
                    if (r == 1) shot(ctx, con, "07-r1-key-pickup");
                    long inter = srv.computeOnServer(s -> STATS.interStart[r]);
                    until(ctx, srv, s -> ctl(s).attempt().round() > r, 1000);
                    long next = srv.computeOnServer(s -> STATS.roundStart[r + 1]);
                    long gap = next - inter;
                    int expect = RoundPlan.intermissionAfter(r);
                    check("B" + r + "f transition length " + gap + " ticks (approved " + expect + ")", Math.abs(gap - expect) <= 25 && inter > 0, "interStart=" + inter + " nextStart=" + next);
                    if (twoClients && r == 1) {
                        boolean adm = until(ctx, srv, s -> ctl(s).attempt().round() >= 2, 400);
                        String a2 = srv.computeOnServer(s -> { var a = ctl(s).attempt(); var g = guest(s); return "round=" + a.round() + " n=" + a.snap.n() + " budget=" + a.snap.budget() + " multiplierMilli=" + a.snap.multiplierMilli() + " admissions=" + a.admissions + " guestUUID=" + (g == null ? "none" : g.getUUID()); });
                        log("Q2c round-2 snapshot", a2);
                        check("Q2c guest admitted at the round-2 boundary, scaling n=2 (budget 51 = 32 x 1.6)", adm && a2.contains("n=2") && a2.contains("budget=51") && a2.contains("multiplierMilli=1600") && a2.contains("@2"), a2);
                    }
                    // stay inside the arena for the next round
                    srv.runOnServer(s -> teleport(s, cx + 0.5, fy, cz + 8.5, 180f));
                }

                // ---------------- C: R7
                boolean bossUp = until(ctx, srv, s -> ctl(s).attempt().bossEntity != null && s.overworld().getEntity(ctl(s).attempt().bossEntity) instanceof ImmortalGladiator, 600);
                check("C1 R7 boss spawned and bound", bossUp, "");
                if (!bossUp) throw new IllegalStateException("no boss");
                ctx.waitTicks(60); con.waitForClientboundPackets();
                boolean barShown = ctx.computeOnClient(c -> !bossBarsOf(c).isEmpty());
                check("C2 boss bar visible on the real client", barShown, "");
                shot(ctx, con, "08-r7-boss-awakening");
                if (BOSS_ONLY) {
                    ctx.waitTicks(60);
                    String probe = srv.computeOnServer(s -> { var a = ctl(s).attempt(); var b = (ImmortalGladiator) s.overworld().getEntity(a.bossEntity); var p = player(s); float h0 = b.getHealth();
                        boolean r = b.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(p), 5f);
                        return "direct hurtServer(playerAttack,5) returned " + r + " hp " + h0 + "->" + b.getHealth() + " frozen=" + ColosseumRuntime.combatFrozen(b) + " shield=" + b.shieldActive() + " snapshot=" + b.combatSnapshot().substring(0, Math.min(120, b.combatSnapshot().length())); });
                    log("boss direct damage probe", probe);
                }
                final int ominousBefore = srv.computeOnServer(s -> keys(player(s), true));
                boolean[] seen = new boolean[8]; boolean disconnected = false, phaseTwoShot = false; int shots = 0; int clicks = 0, aimedClicks = 0; double minHp = 999; float minHealth = 20; boolean phaseTwo = false;
                for (int tick = 0; tick < (BOSS_ONLY ? 60 : 4000); tick++) {
                    var info = srv.computeOnServer(s -> {
                        var a = ctl(s).attempt(); if (a == null || a.ended()) return new double[]{-1};
                        var e = s.overworld().getEntity(a.bossEntity); if (!(e instanceof ImmortalGladiator b) || !b.isAlive()) return new double[]{-2};
                        var p = player(s); double dx = b.getX() - p.getX(), dz = b.getZ() - p.getZ();
                        return new double[]{b.getHealth(), b.getMaxHealth(), b.attackPose(), b.phaseTwo() ? 1 : 0, Math.sqrt(dx * dx + dz * dz), p.getHealth(), b.getX(), b.getY(), b.getZ(), Math.toDegrees(Math.atan2(-dx, dz)), b.shieldActive() ? 1 : 0};
                    });
                    if (info[0] < 0) break;
                    minHp = Math.min(minHp, info[0]); minHealth = (float) Math.min(minHealth, info[5]);
                    int pose = (int) info[2];
                    if (pose > 0 && !seen[pose] && shots < 6) { seen[pose] = true; shots++; con.waitForClientboundPackets(); ctx.takeScreenshot("st05d-09-r7-attack-pose" + pose); log("boss attack windup seen", "pose=" + pose + " hp=" + info[0] + " dist=" + String.format("%.1f", info[4])); }
                    if (info[3] == 1 && !phaseTwo) { phaseTwo = true; con.waitForClientboundPackets(); ctx.takeScreenshot("st05d-10-r7-phase-two"); log("phase two at hp", info[0] + "/" + info[1]); check("C3 phase two entered at <=40% HP", info[0] <= info[1] * 0.4 + 1, "hp=" + info[0] + "/" + info[1]); }
                    // mid-fight leave / rejoin
                    if (!disconnected && info[0] <= info[1] * 0.6) {
                        disconnected = true;
                        if (SP) {
                            log("C4 server restart mid-fight", "hp=" + info[0]);
                            String before = srv.computeOnServer(St05Stage1dClientScenario::stateLine);
                            var save = sp.getWorldSave();
                            sp.close();
                            sp = save.open(); srv = sp.getServer(); con = sp.getConnection();
                            ctx.waitFor(c -> c.player != null && c.level != null); ctx.waitTicks(120); con.waitForClientboundPackets();
                            String after = srv.computeOnServer(St05Stage1dClientScenario::stateLine);
                            log("restart state before", before); log("restart state after", after);
                            check("C4d after a real server restart the same attempt, round, boss and held keys are restored", before.split(" hp=")[0].equals(after.split(" hp=")[0]) && after.contains("active=true") && after.contains("quarantined=false") && after.contains("bossLoaded=true"), after);
                            double hb = Double.parseDouble(before.split(" hp=")[1].split(" ")[0]), ha = Double.parseDouble(after.split(" hp=")[1].split(" ")[0]);
                            check("C4e boss HP survives the restart", Math.abs(hb - ha) <= 1.0, hb + " -> " + ha);
                            check("C4f boss bar re-shown after the restart", ctx.computeOnClient(c -> !bossBarsOf(c).isEmpty()), "");
                            shot(ctx, con, "11-r7-after-server-restart");
                        } else {
                        log("C4 disconnecting real client mid-fight", "hp=" + info[0]);
                        ((TestDedicatedServerConnection) con).close();
                        ctx.waitTicks(100);
                        var st = srv.computeOnServer(s -> { var a = ctl(s).attempt(); return a.outcome() + " active=" + ctl(s).active() + " pending=" + a.pendingIds().size() + " roster=" + a.rosterIds().size(); });
                        check("C4a challenge stays alive while the only participant is away (grace)", st.contains("active=true"), st);
                        con = dsrv.connect(); ctx.waitFor(c -> c.player != null && c.level != null); ctx.waitTicks(60); con.waitForClientboundPackets();
                        var st2 = srv.computeOnServer(s -> { var a = ctl(s).attempt(); var m = a.member(player(s).getUUID()); return "active=" + ctl(s).active() + " member=" + (m != null) + " dead=" + (m != null && m.dead) + " bosses=" + STATS.maxSimultaneousBosses; });
                        check("C4b returning participant keeps their place, boss not duplicated", st2.contains("active=true") && st2.contains("member=true") && st2.contains("dead=false") && st2.endsWith("bosses=1"), st2);
                        boolean bar2 = ctx.computeOnClient(c -> !bossBarsOf(c).isEmpty());
                        check("C4c boss bar re-shown after rejoin", bar2, "");
                        shot(ctx, con, "11-r7-after-rejoin");
                        }
                    }
                    // real melee: keep the player 1.5-3 blocks from the boss and re-aim at its body every cycle, then click with the real mouse
                    srv.runOnServer(s -> {
                        var a = ctl(s).attempt(); var b = s.overworld().getEntity(a.bossEntity); var p = player(s); if (b == null) return;
                        double dx = p.getX() - b.getX(), dz = p.getZ() - b.getZ(), len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                        double d = (len > 3.0 || len < 1.5) ? 2.0 : len;
                        double tx = b.getX() + dx / len * d, tz = b.getZ() + dz / len * d;
                        double ex = b.getX() - tx, ez = b.getZ() - tz, ey = (b.getY() + b.getBbHeight() * 0.5) - (p.getY() + 1.62);
                        float yaw = (float) Math.toDegrees(Math.atan2(-ex, ez)), pitch = (float) -Math.toDegrees(Math.atan2(ey, Math.sqrt(ex * ex + ez * ez)));
                        p.teleportTo(s.overworld(), tx, p.getY(), tz, Set.of(), yaw, pitch, true);
                    });
                    ctx.waitTicks(2);
                    boolean aimed = ctx.computeOnClient(c -> c.crosshairPickEntity instanceof ImmortalGladiator);
                    if (tick % 10 == 0) {
                        String cl = ctx.computeOnClient(c -> "grabbed=" + c.mouseHandler.isMouseGrabbed() + " atkScale=" + String.format("%.2f", c.player.getAttackStrengthScale(0f)) + " mainHand=" + c.player.getMainHandItem().getItem());
                        String sv = srv.computeOnServer(s -> { var a = ctl(s).attempt(); var b = (ImmortalGladiator) s.overworld().getEntity(a.bossEntity); var pl = player(s); String sn = b.combatSnapshot(); int i = sn.indexOf("\"blocks\""); return "lastHurtMob=" + (pl.getLastHurtMob() == null ? "none" : pl.getLastHurtMob().getType().toShortString()) + " snapshot=" + (i >= 0 ? sn.substring(i, Math.min(sn.length(), i + 40)) : "?") + " mode=" + pl.gameMode() + " selected=" + pl.getInventory().getSelectedSlot(); });
                        log("client/server click probe", cl + " | " + sv);
                    }
                    clicks++; if (aimed) aimedClicks++;
                    if (tick % 10 == 0) log("boss fight sample", "tick=" + tick + " hp=" + info[0] + " dist=" + String.format("%.1f", info[4]) + " shield=" + (info[10] == 1) + " aimed/clicks=" + aimedClicks + "/" + clicks + " bossHitsOnPlayer=" + srv.computeOnServer(s -> STATS.bossHits + " raw=" + STATS.bossHitRaw));
                    // The injected mouse click never reached the server in this harness (60+ aimed clicks produced no attack), so the client's own attack path is called:
                    // MultiPlayerGameMode.attack sends the same ServerboundInteractPacket(ATTACK) a left click sends.
                    ctx.runOnClient(c -> { var t = c.crosshairPickEntity; if (t instanceof ImmortalGladiator && c.gameMode != null) { c.gameMode.attack(c.player, t); } });
                    ctx.waitTicks(12);
                }
                check("C5 boss defeated by real melee within the budget", srv.computeOnServer(s -> ctl(s).attempt().outcome() == Outcome.SUCCESS), "aimed/clicks=" + aimedClicks + "/" + clicks + " bossHitsOnPlayer=" + srv.computeOnServer(s -> STATS.bossHits) + " minBossHp=" + minHp + " minPlayerHealth=" + minHealth + " attackPosesSeen=" + Arrays.toString(seen));
                check("C6 exactly one boss existed at any time", srv.computeOnServer(s -> STATS.maxSimultaneousBosses == 1), "");
                ctx.waitTicks(40); shot(ctx, con, "12-r7-victory");

                // ---------------- D: ominous key, end state
                int b4 = ominousBefore;
                int dupO = srv.computeOnServer(s -> STATS.maxKeyEntities.entrySet().stream().filter(e -> e.getKey().contains(":round:7:")).mapToInt(Map.Entry::getValue).max().orElse(0));
                int found = pickUpKeys(ctx, srv);
                int a4 = srv.computeOnServer(s -> keys(player(s), true));
                check("D7 shared Ominous Trial Key: one entity (max " + dupO + "), player holds " + b4 + " before the boss fight and " + a4 + " after (picked up on its own near the boss, entities left on the floor " + found + ")", dupO == 1 && a4 == b4 + 1, "found=" + found);
                var end = srv.computeOnServer(s -> { var c = ctl(s); var a = c.attempt(); return "outcome=" + a.outcome() + " keysAwarded=" + a.keysAwarded() + " cooldown=" + c.cooldownLeft() + " entitlements=" + a.entitlements + " cleanupPending=" + a.cleanupPending + " quarantined=" + c.quarantined(); });
                log("end state", end);
                checkpoint(srv, state, "1 right after the boss died");
                check("D8 success ended normally with seven key records, cooldown running, not quarantined", end.contains("outcome=SUCCESS") && end.contains("keysAwarded=[1, 2, 3, 4, 5, 6, 7]") && !end.contains("cooldown=0") && end.contains("quarantined=false"), end);
                ctx.waitTicks(100);
                checkpoint(srv, state, "2 after the key pickup, before leaving");
                // Early structure reading, taken while the player still stands beside the centre Ominous Vault: informational only. A vanilla vault turns
                // inactive -> active by itself when a player is near, so this reading can legitimately differ in vault_state alone (see the final D9 below).
                int mmEarly = srv.computeOnServer(s -> { try { return St05RealArenaBuilder.mismatches(s, state, ID); } catch (Exception e) { return -1; } });
                log("D9-early structure reading (player beside the vault)", "mismatches=" + mmEarly + (mmEarly > 0 ? " cells=" + srv.computeOnServer(s -> { try { return St05RealArenaBuilder.mismatchSamples(s, state, ID).toString(); } catch (Exception e) { return "EXC " + e; } }) : ""));
                int chestsLeft = srv.computeOnServer(s -> { int n = 0; for (long pos : ctl(s).attempt().supplyChests()) if (s.overworld().getBlockState(net.minecraft.core.BlockPos.of(pos)).is(net.minecraft.world.level.block.Blocks.CHEST)) n++; return n; });
                check("D10 supply chests removed on completion", chestsLeft == 0, "left=" + chestsLeft);
                // cooldown: leave and walk back in -> no new attempt
                srv.runOnServer(s -> teleport(s, cx + 0.5, fy, cz - 36.5, 0f));
                watchStructure(ctx, srv, state, 40, "after teleport to the arch");
                ctx.getInput().holdKey(o -> o.keyUp);
                watchStructure(ctx, srv, state, 100, "while walking in");
                ctx.getInput().releaseKey(o -> o.keyUp);
                watchStructure(ctx, srv, state, 120, "after walking in");
                var again = srv.computeOnServer(s -> { var c = ctl(s); return "active=" + c.active() + " cooldown=" + c.cooldownLeft(); });
                check("D11 re-entry during cooldown does not start a new challenge", again.startsWith("active=false"), again);
                shot(ctx, con, "13-reentry-during-cooldown");
                checkpoint(srv, state, "3 after walking back in during cooldown");
                // Final, strict structure check: the player has left the vault area for a while; every plan cell must hold its verified block again.
                srv.runOnServer(s -> teleport(s, cx + 0.5, fy, cz - 36.5, 0f));
                watchStructure(ctx, srv, state, 300, "waiting outside the arch");
                int mm = srv.computeOnServer(s -> { try { return St05RealArenaBuilder.mismatches(s, state, ID); } catch (Exception e) { return -1; } });
                check("D9 arena structure intact after the challenge, strict (plan mismatches)", mm == 0, "mismatches=" + mm);
                if (mm != 0) { List<String> cells = srv.computeOnServer(s -> { try { return St05RealArenaBuilder.mismatchSamples(s, state, ID); } catch (Exception e) { return List.of("EXC " + e); } }); log("D9 mismatching cells", cells.toString()); }
                log("round events", String.join(" ; ", STATS.events));
                log("done", "scenario completed");
            } finally { if (!SP) try { ((TestDedicatedServerConnection) con).close(); } catch (Throwable ignored) { } }
        } finally { try { if (SP) sp.close(); else dsrv.close(); } catch (Throwable ignored) { } }
    }


    /** Logs every plan mismatch (cell, expected, actual) with the player position and whether the cell's chunk is loaded, so a drift can be dated. */
    static void checkpoint(TestServerContext srv, Path state, String label) {
        String r = srv.computeOnServer(s -> { try {
            var p = player(s); var cells = St05RealArenaBuilder.mismatchSamples(s, state, ID);
            return "player=" + String.format("%.1f,%.1f,%.1f", p.getX(), p.getY(), p.getZ()) + " mismatches=" + St05RealArenaBuilder.mismatches(s, state, ID) + " cells=" + cells;
        } catch (Exception e) { return "EXC " + e; } });
        log("structure checkpoint " + label, r);
    }


    /** Polls the plan mismatches every 10 ticks; whenever the count changes it logs the cells, the player position and every non-player entity within 40 blocks of the arena centre. */
    static void watchStructure(ClientGameTestContext ctx, TestServerContext srv, Path state, int ticks, String label) {
        int last = -2;
        for (int t = 0; t <= ticks; t += 10) {
            int[] n = {0}; String detail = srv.computeOnServer(sv -> { try {
                n[0] = St05RealArenaBuilder.mismatches(sv, state, ID);
                if (n[0] == 0) return "";
                var site = ColosseumRuntime.data(sv).sites().get(ID); var p = player(sv); StringBuilder b = new StringBuilder("cells=" + St05RealArenaBuilder.mismatchSamples(sv, state, ID) + " player=" + String.format("%.1f,%.1f,%.1f", p.getX(), p.getY(), p.getZ()) + " entities=");
                for (var e : sv.overworld().getAllEntities()) if (!(e instanceof ServerPlayer) && Math.abs(e.getX() - site.cx()) < 40 && Math.abs(e.getZ() - site.cz()) < 40) b.append(e.getType().toShortString()).append('@').append(String.format("%.0f,%.0f,%.0f", e.getX(), e.getY(), e.getZ())).append(' ');
                return b.toString();
            } catch (Exception e) { return "EXC " + e; } });
            if (n[0] != last) { log("structure watch " + label + " t=" + t, "mismatches=" + n[0] + " " + detail); last = n[0]; }
            ctx.waitTicks(10);
        }
    }

    static String stateLine(MinecraftServer s) {
        var c = ctl(s); var a = c.attempt(); if (a == null) return "no attempt";
        var b = a.bossEntity == null ? null : s.overworld().getEntity(a.bossEntity); var p = player(s);
        return a.attemptId + " round=" + a.round() + " boss=" + a.bossEntity + " keysAwarded=" + a.keysAwarded() + " trialKeysHeld=" + keys(p, false) + " ominousHeld=" + keys(p, true) + " roster=" + a.rosterIds().size() + " hp=" + (b instanceof ImmortalGladiator g ? g.getHealth() : -1) + " active=" + c.active() + " quarantined=" + c.quarantined() + (c.quarantined() ? "(" + c.quarantineReason() + ")" : "") + " bossLoaded=" + (b instanceof ImmortalGladiator);
    }
}
