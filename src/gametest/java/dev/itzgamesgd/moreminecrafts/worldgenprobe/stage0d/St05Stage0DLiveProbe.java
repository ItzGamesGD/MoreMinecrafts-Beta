package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d.ChamberCore.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NoiseColumn;

/**
 * Stage 0-D live (Minecraft 26.3 dedicated server) driver. Modes: d-run1 (compute, compare orders, persist), d-restart (new JVM: load,
 * verify, recompute under another order, reconcile, corruption cases). Terrain source is the plan-time pure function
 * ChunkGenerator.getBaseColumn (snapshot kind PLAN_BASE_COLUMN_v1); it is identical for every observation, so observations are comparable.
 * Final (post-carver/feature) terrain is NOT used here; plan-vs-final divergence is a separate, previously recorded Stage 0-C finding.
 */
public final class St05Stage0DLiveProbe {
    private St05Stage0DLiveProbe() { }

    static final String SNAPSHOT = "PLAN_BASE_COLUMN_v1";
    static final int[][] REGIONS = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
    static final int WIN = ChamberCore.OWN + 2 * ChamberCore.MARGIN;

    static List<Params> paramSets() {
        List<Params> l = new ArrayList<>();
        for (String s : System.getProperty("mm.st05d.params", "A_10x10x6:10:6,B_16x16x8:16:8,C_24x24x12:24:12").split(",")) {
            String[] t = s.split(":");
            l.add(new Params(t[0], Integer.parseInt(t[1]), Integer.parseInt(t[2])));
        }
        return l;
    }

    static Window planWindow(ServerLevel level, int rx, int rz, int y0, int h, int threads, String order) throws Exception {
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        int ox = rx * ChamberCore.OWN - ChamberCore.MARGIN, oz = rz * ChamberCore.OWN - ChamberCore.MARGIN;
        boolean[] open = new boolean[WIN * WIN * h];
        List<int[]> cols = new ArrayList<>(WIN * WIN);
        for (int x = 0; x < WIN; x++) for (int z = 0; z < WIN; z++) cols.add(new int[]{x, z});
        if (order.equals("rev")) Collections.reverse(cols);
        else if (order.equals("shuf")) Collections.shuffle(cols, new Random(99));
        else if (order.equals("par")) Collections.shuffle(cols, new Random(4242));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> fs = new ArrayList<>();
        int stripe = Math.max(1, cols.size() / (threads * 8));
        for (int s = 0; s < cols.size(); s += stripe) {
            final int from = s, to = Math.min(cols.size(), s + stripe);
            fs.add(pool.submit(() -> {
                for (int k = from; k < to; k++) {
                    int[] c = cols.get(k);
                    NoiseColumn col = gen.getBaseColumn(ox + c[0], oz + c[1], level, rs);
                    for (int y = 0; y < h; y++) if (col.getBlock(y0 + y).isAir()) open[(c[0] * WIN + c[1]) * h + y] = true;
                }
            }));
        }
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        return new Window(ox, y0, oz, WIN, WIN, h, open, SNAPSHOT);
    }

    static final com.sun.management.ThreadMXBean TMX = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    /** Stage 0-E coarse scan: stride-4 columns, necessary-condition box 20x20x18 (superset of 32x32x18 and of a 32-diameter disk). */
    static void scan(ServerLevel level, long seed, Map<String, Object> doc) throws Exception {
        int R = Integer.getInteger("mm.st05e.radius", 2048), y0 = Integer.getInteger("mm.st05d.y0", -64), h = Integer.getInteger("mm.st05d.h", 80);
        int threads = Integer.getInteger("mm.st05d.threads", 4), S = 4, TILE = 1024, PAD = 32;
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        List<int[]> hits = new ArrayList<>();   // world x,z (box origin*4), y
        long t0 = System.nanoTime();
        for (int tx = -R; tx < R; tx += TILE) for (int tz = -R; tz < R; tz += TILE) {
            int ox = tx - PAD, oz = tz - PAD, n = (TILE + 2 * PAD) / S;
            boolean[] open = new boolean[n * n * h];
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<?>> fs = new ArrayList<>();
            for (int cx0 = 0; cx0 < n; cx0 += 8) {
                final int a = cx0;
                fs.add(pool.submit(() -> {
                    for (int cx = a; cx < Math.min(n, a + 8); cx++) for (int cz = 0; cz < n; cz++) {
                        NoiseColumn col = gen.getBaseColumn(ox + cx * S, oz + cz * S, level, rs);
                        for (int y = 0; y < h; y++) if (col.getBlock(y0 + y).isAir()) open[(cx * n + cz) * h + y] = true;
                    }
                }));
            }
            for (Future<?> f : fs) f.get();
            pool.shutdown();
            Window w = new Window(0, y0, 0, n, n, h, open, "COARSE");
            for (int[] b : ChamberCore.openBoxOrigins(w, 5, 5, 18)) {
                int wx = ox + b[0] * S, wz = oz + b[1] * S;
                if (wx < tx || wx >= tx + TILE || wz < tz || wz >= tz + TILE) continue;   // own tile only (dedupe overlap)
                hits.add(new int[]{wx, wz, y0 + b[2]});
            }
        }
        // cluster hits within 48 blocks (greedy by scan order)
        List<int[]> clusters = new ArrayList<>();
        hits.sort((p, q) -> p[0] != q[0] ? Integer.compare(p[0], q[0]) : p[1] != q[1] ? Integer.compare(p[1], q[1]) : Integer.compare(p[2], q[2]));
        List<int[]> cnt = new ArrayList<>();
        for (int[] hit : hits) {
            boolean merged = false;
            for (int i = 0; i < clusters.size(); i++) {
                int[] c = clusters.get(i);
                if (Math.abs(c[0] - hit[0]) <= 48 && Math.abs(c[1] - hit[1]) <= 48) { cnt.get(i)[0]++; merged = true; break; }
            }
            if (!merged) { clusters.add(hit); cnt.add(new int[]{1}); }
        }
        List<String> cl = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) cl.add(clusters.get(i)[0] + "," + clusters.get(i)[1] + "," + clusters.get(i)[2] + " boxes=" + cnt.get(i)[0]);
        doc.put("scan_radius", R); doc.put("scan_ms", (System.nanoTime() - t0) / 1_000_000);
        doc.put("coarse_box_hits", hits.size()); doc.put("clusters", cl);
    }

    public static void run(MinecraftServer server, ServerLevel level, String mode, Path out, Map<String, Object> doc) throws Exception {
        long seed = level.getSeed();
        if (mode.equals("e-scan")) { scan(level, seed, doc); return; }
        int y0 = Integer.getInteger("mm.st05d.y0", -64), h = Integer.getInteger("mm.st05d.h", 80);
        int threads = Integer.getInteger("mm.st05d.threads", 4);
        Path stateDir = Path.of(System.getProperty("mm.st05d.statedir", "build/st05-worldgen-probe/stage0d-state"));
        Files.createDirectories(stateDir);
        doc.put("snapshot_kind", SNAPSHOT);
        doc.put("algo", ChamberCore.ALGO);
        doc.put("band", y0 + ".." + (y0 + h));
        doc.put("regions", "2x2 regions (0..1,0..1), observation window 512x512 each (256 + 2x128)");
        List<Params> params = paramSets();
        Map<String, Window> wins = new LinkedHashMap<>();
        Map<String, Object> gen = new LinkedHashMap<>();
        doc.put("snapshot_generation", gen);
        String[] orders = mode.equals("d-run1") ? new String[]{"fwd", "rev", "shuf"} : new String[]{"par"};
        Map<String, Map<String, String>> digestsByOrder = new LinkedHashMap<>();
        for (String order : orders) {
            List<int[]> rg = new ArrayList<>(List.of(REGIONS));
            if (order.equals("rev")) Collections.reverse(rg);
            if (order.equals("shuf") || order.equals("par")) Collections.shuffle(rg, new Random(order.equals("par") ? 17 : 3));
            Map<String, String> dg = new TreeMap<>();
            long t0 = System.nanoTime();
            for (int[] r : rg) {
                Window w = planWindow(level, r[0], r[1], y0, h, threads, order);
                dg.put(r[0] + "," + r[1], Long.toHexString(w.digest()));
                if (order.equals(orders[0])) wins.put(r[0] + "," + r[1], w);
            }
            digestsByOrder.put(order, dg);
            gen.put(order + "_ms", (System.nanoTime() - t0) / 1_000_000);
        }
        doc.put("window_digests_by_generation_order", digestsByOrder);
        boolean maskIdentical = digestsByOrder.values().stream().distinct().count() == 1;
        doc.put("snapshot_identical_across_generation_orders", maskIdentical);

        Map<String, Object> legacy = legacyMetrics(wins, seed);
        doc.put("legacy_baseline", legacy);

        Map<String, Object> per = new LinkedHashMap<>();
        doc.put("params", per);
        for (Params p : params) {
            Map<String, Object> m = new LinkedHashMap<>();
            per.put(p.name(), m);
            m.put("cw", p.cw()); m.put("ch", p.ch());
            List<Observation> obs = new ArrayList<>();
            long cpu = 0, wall = 0, coreVox = 0;
            int complete = 0, partial = 0;
            long memBefore = usedMem();
            long peak = 0;
            int falseMergeLegacy = 0, legacyComps = 0;
            for (int[] r : REGIONS) {
                Window w = wins.get(r[0] + "," + r[1]);
                ExtractStats st = new ExtractStats();
                long c0 = TMX.getCurrentThreadCpuTime(), t0 = System.nanoTime();
                List<Comp> cs = ChamberCore.extract(w, p, seed, r[0], r[1], st);
                cpu += TMX.getCurrentThreadCpuTime() - c0; wall += System.nanoTime() - t0;
                peak = Math.max(peak, usedMem());
                coreVox += st.coreVoxels; complete += st.complete; partial += st.partial;
                obs.add(new Observation(r[0], r[1], cs, w.digest()));
                int[] lab = new int[w.open.length];
                legacyComps += ChamberCore.legacySegment(w, 0, w.wx, 0, w.wz, seed, lab).size();
                Map<Integer, Integer> perLegacy = new TreeMap<>();
                for (Comp c : cs) perLegacy.merge(lab[w.idx(kxo(c.anchor) - w.ox, kzo(c.anchor) - w.oz, kyo(c.anchor) - w.oy)], 1, Integer::sum);
                for (int n : perLegacy.values()) if (n >= 2) falseMergeLegacy++;
            }
            m.put("extract_cpu_ms", cpu / 1_000_000); m.put("extract_wall_ms", wall / 1_000_000);
            m.put("peak_heap_mb_observed_after_extract", peak >> 20); m.put("heap_delta_mb", (peak - memBefore) >> 20);
            m.put("core_voxels_total_4_windows", coreVox);
            m.put("observed_components_complete", complete); m.put("observed_components_partial", partial);
            m.put("legacy_air_components_4_windows", legacyComps);
            m.put("legacy_components_containing_2plus_distinct_cores_FALSE_MERGE_SUM_OVER_WINDOWS", falseMergeLegacy);

            // order independence of resolution
            Resolution base = ChamberCore.resolve(obs, p, seed);
            Outcome o = ChamberCore.decide(base, seed);
            String d0 = ChamberCore.sha256(ChamberCore.canonical(base, o));
            List<Observation> rev = new ArrayList<>(obs); Collections.reverse(rev);
            List<Observation> shuf = new ArrayList<>(obs); Collections.shuffle(shuf, new Random(31));
            Resolution rr = ChamberCore.resolve(rev, p, seed), rs = ChamberCore.resolve(shuf, p, seed);
            boolean orderOk = ChamberCore.sha256(ChamberCore.canonical(rr, ChamberCore.decide(rr, seed))).equals(d0) && ChamberCore.sha256(ChamberCore.canonical(rs, ChamberCore.decide(rs, seed))).equals(d0);
            m.put("resolution_digest", d0);
            m.put("resolution_identical_fwd_rev_shuffle_observation_order", orderOk);
            m.put("chambers_owned_complete", o.chambers); m.put("chambers_outside_universe", o.outsideUniverse);
            m.put("duplicate_complete_observations_collapsed", base.duplicateObservations);
            m.put("partial_views_proven_same_as_complete_chamber", base.partialMergedIntoChamber);
            m.put("partial_views_total", base.partialsTotal);
            m.put("unresolved_groups_deferred", base.groups.size());
            m.put("identity_conflicts", base.conflicts.size());
            m.put("identity_ops", base.identityOps);
            m.put("suitable_owned", o.suitableOwned); m.put("not_suitable", o.notSuitable);
            m.put("blocked_by_unresolved", o.blockedByUnresolved); m.put("rejected_spacing", o.rejectedSpacing);
            m.put("roll_denominator_accepted", o.accepted); m.put("roll_wins", o.rollWins); m.put("roll_failures", o.rollFailures);
            m.put("deferred_fraction", o.deferredFraction);
            m.put("ownership_checks_owner_observed_all", base.chambers.values().stream().allMatch(c -> c.ownerObserved || c.status.equals("OWNER_OUTSIDE_OBSERVED_UNIVERSE")));
            long multi = base.chambers.values().stream().filter(c -> c.observers.size() >= 2).count();
            long triple = base.chambers.values().stream().filter(c -> c.observers.size() >= 3).count();
            m.put("chambers_seen_by_2plus_regions", multi); m.put("chambers_seen_by_3plus_regions", triple);
            // persistence
            StateStore.Header hd = new StateStore.Header(ChamberCore.ALGO, p.name(), seed, SNAPSHOT);
            Path sf = stateDir.resolve("state-" + p.name() + ".txt");
            persistAndRestore(mode, m, base, o, hd, sf, seed);
        }
        doc.put("heap_max_mb", Runtime.getRuntime().maxMemory() >> 20);
        doc.put("verdict_inputs", Map.of("snapshot_identical_across_generation_orders", maskIdentical));
    }

    /** Pre-D01 baseline on the same snapshots: ownership-area-cut flood fill; counts caves split across the 256 ownership borders (duplicate legacy identities). */
    static Map<String, Object> legacyMetrics(Map<String, Window> wins, long seed) {
        Map<String, Object> m = new LinkedHashMap<>();
        int M = ChamberCore.MARGIN, O = ChamberCore.OWN;
        Map<String, int[]> lab = new TreeMap<>();
        Map<String, List<LegacyComp>> comps = new TreeMap<>();
        long total = 0, touching = 0;
        for (int[] r : REGIONS) {
            String k = r[0] + "," + r[1];
            Window w = wins.get(k);
            int[] l = new int[w.open.length];
            List<LegacyComp> cs = ChamberCore.legacySegment(w, M, M + O, M, M + O, seed, l);
            lab.put(k, l); comps.put(k, cs);
            total += cs.size();
            for (LegacyComp c : cs) if (c.touchBox()) touching++;
        }
        // union-find over (region, comp) linked by open voxels adjacent across an ownership border
        Map<String, Integer> base = new TreeMap<>();
        int n = 0;
        for (int[] r : REGIONS) { base.put(r[0] + "," + r[1], n); n += comps.get(r[0] + "," + r[1]).size(); }
        int[] uf = new int[n];
        for (int i = 0; i < n; i++) uf[i] = i;
        int links = 0;
        for (int[] r : REGIONS) {
            for (int axis = 0; axis < 2; axis++) {
                int nr0 = r[0] + (axis == 0 ? 1 : 0), nr1 = r[1] + (axis == 1 ? 1 : 0);
                if (!wins.containsKey(nr0 + "," + nr1)) continue;
                Window a = wins.get(r[0] + "," + r[1]), b = wins.get(nr0 + "," + nr1);
                int[] la = lab.get(r[0] + "," + r[1]), lb = lab.get(nr0 + "," + nr1);
                for (int t = 0; t < O; t++) for (int y = 0; y < a.h; y++) {
                    int ax = axis == 0 ? M + O - 1 : M + t, az = axis == 0 ? M + t : M + O - 1;
                    int bx = axis == 0 ? ax + 1 : ax, bz = axis == 0 ? az : az + 1;   // neighbour voxel in a's coordinates (inside a's margin)
                    int wxB = a.ox + bx - b.ox, wzB = a.oz + bz - b.oz;
                    int ia = la[a.idx(ax, az, y)];
                    int ib = lb[b.idx(wxB, wzB, y)];
                    if (ia >= 0 && ib >= 0) {
                        int x1 = find(uf, base.get(r[0] + "," + r[1]) + ia), x2 = find(uf, base.get(nr0 + "," + nr1) + ib);
                        if (x1 != x2) { uf[x1] = x2; links++; }
                    }
                }
            }
        }
        java.util.Set<Integer> roots = new java.util.HashSet<>();
        for (int i = 0; i < n; i++) roots.add(find(uf, i));
        m.put("legacy_ownership_cut_components_total", total);
        m.put("legacy_components_touching_ownership_or_cut_boundary", touching);
        m.put("legacy_distinct_ids_after_physical_border_linking", roots.size());
        m.put("legacy_duplicate_identities_for_same_cave_across_borders", n - roots.size());
        return m;
    }

    static int find(int[] uf, int i) { while (uf[i] != i) { uf[i] = uf[uf[i]]; i = uf[i]; } return i; }

    static int kxo(long k) { return ChamberCore.kx(k); }
    static int kzo(long k) { return ChamberCore.kz(k); }
    static int kyo(long k) { return ChamberCore.ky(k); }
    static long usedMem() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    static void persistAndRestore(String mode, Map<String, Object> m, Resolution base, Outcome o, StateStore.Header hd, Path sf, long seed) throws Exception {
        Map<Long, StateStore.Rec> fresh = new TreeMap<>(Long::compareUnsigned);
        for (Chamber c : base.chambers.values()) fresh.put(c.id, StateStore.fromChamber(c, true));
        Map<String, Object> pm = new LinkedHashMap<>();
        m.put("persistence", pm);
        if (mode.equals("d-run1")) {
            Map<String, StateStore.Budget> bud = new TreeMap<>();
            for (Group g : base.groups) StateStore.charge(bud, g.id(), base.identityOps / Math.max(1, base.groups.size()), Long.getLong("mm.st05d.budgetcap", 50_000_000L));
            StateStore.write(sf, hd, fresh, bud);
            StateStore.Loaded L = StateStore.load(sf, hd);
            pm.put("written_records", fresh.size()); pm.put("reloaded_ok", L.headerOk && L.trailerOk && L.recs.equals(fresh));
            pm.put("finalized_records", fresh.values().stream().filter(StateStore.Rec::finalized).count());
            pm.put("budget_groups", bud.size());
            return;
        }
        // d-restart: new JVM
        StateStore.Loaded L = StateStore.load(sf, hd);
        StateStore.Reconciled R = StateStore.reconcile(L, fresh);
        long finalized = L.recs.values().stream().filter(StateStore.Rec::finalized).count();
        pm.put("loaded_headerOk", L.headerOk); pm.put("loaded_trailerOk", L.trailerOk); pm.put("loaded_records", L.recs.size());
        pm.put("stored_finalized", finalized); pm.put("restored_verified_against_fresh_recompute", R.restoredVerified);
        pm.put("integrity_violations", R.integrityViolations); pm.put("held", R.held);
        pm.put("loaded_budget_groups", L.budgets.size());
        pm.put("restart_identical", L.headerOk && L.trailerOk && R.integrityViolations.isEmpty() && R.restoredVerified == finalized && L.recs.equals(fresh));
        // corruption case 1: tamper one finalized record in a copy
        Path c1 = sf.resolveSibling(sf.getFileName() + ".tamper-copy");
        java.nio.file.Files.copy(sf, c1, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        List<String> lines = new ArrayList<>(Files.readAllLines(c1));
        if (lines.size() > 3) {
            lines.set(1, lines.get(1).replace("size=", "size=1"));
            Files.write(c1, lines);
            StateStore.Loaded D = StateStore.load(c1, hd);
            StateStore.Reconciled RD = StateStore.reconcile(D, fresh);
            pm.put("tamper_quarantined_lines", D.quarantinedLines.size()); pm.put("tamper_held", RD.held.size());
            pm.put("tamper_other_finalized_unchanged", RD.restoredVerified == Math.max(0, (int) finalized - RD.held.size()) && RD.integrityViolations.isEmpty());
        }
        // corruption case 2: wrong world/seed
        Path c2 = sf.resolveSibling(sf.getFileName() + ".seed-copy");
        Files.copy(sf, c2, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        StateStore.Loaded W = StateStore.load(c2, new StateStore.Header(ChamberCore.ALGO, hd.params(), seed + 1, SNAPSHOT));
        pm.put("wrong_seed_quarantined", !W.headerOk && W.recs.isEmpty() && W.quarantinedFile != null && !Files.exists(c2));
    }
}
