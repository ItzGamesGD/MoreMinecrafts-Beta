package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d.ChamberCore.*;

/**
 * Stage 0-D synthetic adversarial fixtures. SYNTHETIC ONLY: passing here does not substitute for the Minecraft 26.3 live run.
 * Each mandatory fixture asserts BOTH that the pre-D01 behaviour reproduces the defect and that the D01 algorithm resolves it.
 * Run: javac -d out stage0d/*.java &amp;&amp; java -ea -Xmx3g -cp out ...ChamberCoreSelfTest [resultsFile]
 */
public final class ChamberCoreSelfTest {
    static final long SEED = 7001;
    static final int Y0 = -48, H = 36;                 // analysis band
    static final Params P = new Params("synthetic-8x8x6", 8, 6);
    static final List<String> LOG = new ArrayList<>();
    static int fails;

    static void check(String name, boolean ok, String detail) {
        LOG.add((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : "  [" + detail + "]"));
        if (!ok) fails++;
    }

    /** Open boxes {x0,x1,z0,z1,y0,y1} half-open, in world coordinates. */
    static final class World {
        final List<int[]> boxes = new ArrayList<>();
        World box(int x0, int x1, int z0, int z1, int y0, int y1) { boxes.add(new int[]{x0, x1, z0, z1, y0, y1}); return this; }
        World chamber(int x0, int x1, int z0, int z1) { return box(x0, x1, z0, z1, Y0 + 10, Y0 + 30); }
        World chamberAt(int x0, int x1, int z0, int z1, int floorY) { return box(x0, x1, z0, z1, floorY, floorY + 20); }
        World tunnelX(int x0, int x1, int zc) { return box(x0, x1, zc - 2, zc + 2, Y0 + 18, Y0 + 22); }
        Window window(int rx, int rz) {
            int ox = rx * ChamberCore.OWN - ChamberCore.MARGIN, oz = rz * ChamberCore.OWN - ChamberCore.MARGIN, wxz = ChamberCore.OWN + 2 * ChamberCore.MARGIN;
            boolean[] open = new boolean[wxz * wxz * H];
            for (int[] b : boxes) {
                for (int x = Math.max(b[0], ox); x < Math.min(b[1], ox + wxz); x++) for (int z = Math.max(b[2], oz); z < Math.min(b[3], oz + wxz); z++) for (int y = Math.max(b[4], Y0); y < Math.min(b[5], Y0 + H); y++)
                    open[((x - ox) * wxz + (z - oz)) * H + (y - Y0)] = true;
            }
            return new Window(ox, Y0, oz, wxz, wxz, H, open, "SYNTHETIC");
        }
    }

    static Observation observe(Window w, int rx, int rz) { return new Observation(rx, rz, ChamberCore.extract(w, P, SEED, rx, rz, null), w.digest()); }

    static String digest(Resolution r) { return ChamberCore.sha256(ChamberCore.canonical(r, ChamberCore.decide(r, SEED))); }

    public static void main(String[] args) throws Exception {
        f1BroadChamberTwoRegions();
        f2NarrowTunnel();
        f3ClippedChamber();
        f4OverlappingDistinct();
        f5ThreeWayOverlapAndOrderIndependence();
        f9DisjointPartialsStayDeferred();
        fSpacingPropagation();
        fPlacementPipeline();
        fPersistence();
        StringBuilder sb = new StringBuilder();
        sb.append("ALGO=").append(ChamberCore.ALGO).append(" params=").append(P).append(" seed=").append(SEED).append(" band=").append(Y0).append("..").append(Y0 + H).append('\n');
        for (String s : LOG) sb.append(s).append('\n');
        sb.append(fails == 0 ? "ALL_PASS" : "FAILURES=" + fails).append('\n');
        System.out.print(sb);
        if (args.length > 0) Files.writeString(Path.of(args[0]), sb.toString());
        System.exit(fails == 0 ? 0 : 1);
    }

    // ---- F1: one broad chamber straddling the x=256 ownership border, observed by two adjacent regions
    static void f1BroadChamberTwoRegions() {
        World w = new World().chamber(236, 276, 100, 140);
        Window w0 = w.window(0, 0), w1 = w.window(1, 0);
        // legacy defect: ownership-area cut gives two different ids for one chamber
        int[] l0 = new int[w0.open.length], l1 = new int[w1.open.length];
        List<LegacyComp> a = ChamberCore.legacySegment(w0, ChamberCore.MARGIN, ChamberCore.MARGIN + ChamberCore.OWN, ChamberCore.MARGIN, ChamberCore.MARGIN + ChamberCore.OWN, SEED, l0);
        List<LegacyComp> b = ChamberCore.legacySegment(w1, ChamberCore.MARGIN, ChamberCore.MARGIN + ChamberCore.OWN, ChamberCore.MARGIN, ChamberCore.MARGIN + ChamberCore.OWN, SEED, l1);
        check("F1.legacy_reproduces_duplicate_ids", a.size() == 1 && b.size() == 1 && a.get(0).id() != b.get(0).id(), "region0 id=" + Long.toHexString(a.get(0).id()) + " region1 id=" + Long.toHexString(b.get(0).id()));
        Resolution r = ChamberCore.resolve(List.of(observe(w0, 0, 0), observe(w1, 1, 0)), P, SEED);
        Outcome o = ChamberCore.decide(r, SEED);
        Chamber c = r.chambers.values().iterator().next();
        check("F1.d01_single_chamber_single_owner", r.chambers.size() == 1 && c.observers.size() == 2 && c.ownerRx == 0 && c.ownerRz == 0 && r.duplicateObservations == 1 && c.ownerObserved, "observers=" + c.observers + " owner=" + c.ownerRx + "," + c.ownerRz);
        check("F1.d01_single_finalization", o.accepted + o.rejectedSpacing + o.blockedByUnresolved == 1 && c.suitable, "status=" + c.status);
    }

    // ---- F2: two broad chambers joined by a narrow tunnel
    static void f2NarrowTunnel() {
        World w = new World().chamber(100, 140, 100, 140).chamber(180, 220, 100, 140).tunnelX(140, 180, 120);
        Window w0 = w.window(0, 0);
        int[] lab = new int[w0.open.length];
        List<LegacyComp> leg = ChamberCore.legacySegment(w0, 0, w0.wx, 0, w0.wz, SEED, lab);
        check("F2.legacy_reproduces_false_merge", leg.size() == 1, "legacy components=" + leg.size());
        Resolution r = ChamberCore.resolve(List.of(observe(w0, 0, 0)), P, SEED);
        check("F2.d01_two_distinct_chambers", r.chambers.size() == 2 && r.groups.isEmpty(), "chambers=" + r.chambers.size());
    }

    // ---- F3: chamber larger than the observation envelope (clipped)
    static void f3ClippedChamber() {
        World w = new World().chamber(100, 460, 100, 140);
        Window w0 = w.window(0, 0), w1 = w.window(1, 0);
        int[] l0 = new int[w0.open.length], l1 = new int[w1.open.length];
        List<LegacyComp> a = ChamberCore.legacySegment(w0, 0, w0.wx, 0, w0.wz, SEED, l0);
        List<LegacyComp> b = ChamberCore.legacySegment(w1, 0, w1.wx, 0, w1.wz, SEED, l1);
        check("F3.legacy_reproduces_clip_dependent_ids", a.size() == 1 && b.size() == 1 && a.get(0).id() != b.get(0).id() && a.get(0).touchBox(), "ids differ for one chamber");
        Resolution r = ChamberCore.resolve(List.of(observe(w0, 0, 0), observe(w1, 1, 0)), P, SEED);
        ChamberCore.decide(r, SEED);
        check("F3.d01_no_false_finalization_deferred", r.chambers.isEmpty() && r.groups.size() == 1 && r.groups.get(0).views.size() == 2, "chambers=" + r.chambers.size() + " groups=" + r.groups.size());
    }

    // ---- F4: nearby distinct cores whose observation windows overlap; naive air-overlap matching falsely merges
    static void f4OverlappingDistinct() {
        World w = new World().chamber(20, 60, 100, 140).chamber(300, 340, 100, 140).tunnelX(60, 300, 120);
        Window w0 = w.window(0, 0), w1 = w.window(1, 0);
        int[] l0 = new int[w0.open.length], l1 = new int[w1.open.length];
        List<LegacyComp> a = ChamberCore.legacySegment(w0, 0, w0.wx, 0, w0.wz, SEED, l0);
        List<LegacyComp> b = ChamberCore.legacySegment(w1, 0, w1.wx, 0, w1.wz, SEED, l1);
        check("F4.naive_air_overlap_reproduces_false_merge", a.size() == 1 && b.size() == 1 && ChamberCore.naiveAirOverlapMatch(w0, l0, 0, w1, l1, 0), "air components share open voxels");
        Resolution r = ChamberCore.resolve(List.of(observe(w0, 0, 0), observe(w1, 1, 0)), P, SEED);
        Chamber x = null, y = null;
        for (Chamber c : r.chambers.values()) { if (c.minX < 100) x = c; else y = c; }
        check("F4.d01_distinct_ids", r.chambers.size() == 2 && x != null && y != null && x.id != y.id && x.observers.size() == 1 && y.observers.size() == 2, "observers x=" + (x == null ? "-" : x.observers) + " y=" + (y == null ? "-" : y.observers));
    }

    // ---- F5: four anchors/windows observe one chamber at the 4-region corner; all orders identical; parallel extraction identical
    static void f5ThreeWayOverlapAndOrderIndependence() throws Exception {
        World w = new World().chamber(230, 290, 230, 290);
        Window[] ws = {w.window(0, 0), w.window(1, 0), w.window(0, 1), w.window(1, 1)};
        int[][] rg = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
        List<Observation> obs = new ArrayList<>();
        for (int i = 0; i < 4; i++) obs.add(observe(ws[i], rg[i][0], rg[i][1]));
        Resolution base = ChamberCore.resolve(obs, P, SEED);
        Chamber c = base.chambers.values().iterator().next();
        check("F5.d01_four_views_one_canonical_owner", base.chambers.size() == 1 && c.observers.size() == 4 && c.ownerRx == 0 && c.ownerRz == 0 && base.duplicateObservations == 3, "observers=" + c.observers);
        String d0 = digest(base);
        Set<String> digests = new HashSet<>();
        int perms = 0;
        for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) for (int cc = 0; cc < 4; cc++) for (int d = 0; d < 4; d++) {
            if (a == b || a == cc || a == d || b == cc || b == d || cc == d) continue;
            digests.add(digest(ChamberCore.resolve(List.of(obs.get(a), obs.get(b), obs.get(cc), obs.get(d)), P, SEED)));
            perms++;
        }
        check("F5.order_independent_all_24_permutations", perms == 24 && digests.size() == 1 && digests.contains(d0), "permutations=" + perms + " distinct digests=" + digests.size());
        // parallel extraction determinism
        ExecutorService ex = Executors.newFixedThreadPool(4);
        List<Future<Observation>> fs = new ArrayList<>();
        for (int i = 3; i >= 0; i--) { final int k = i; fs.add(ex.submit(() -> observe(ws[k], rg[k][0], rg[k][1]))); }
        List<Observation> par = new ArrayList<>();
        for (Future<Observation> f : fs) par.add(f.get());
        ex.shutdown();
        check("F5.parallel_extraction_identical", digest(ChamberCore.resolve(par, P, SEED)).equals(d0), "");
        // component order inside an observation must not matter
        List<Observation> shuffled = new ArrayList<>();
        for (Observation o : obs) { List<Comp> cs = new ArrayList<>(o.comps); Collections.shuffle(cs, new Random(5)); shuffled.add(new Observation(o.rx, o.rz, cs, o.snapshotDigest)); }
        check("F5.component_order_irrelevant", digest(ChamberCore.resolve(shuffled, P, SEED)).equals(d0), "");
    }

    // ---- F9: apparently disjoint partial views do not prove distinct chambers
    static void f9DisjointPartialsStayDeferred() {
        World w = new World().chamber(100, 700, 100, 140);
        Window w0 = w.window(0, 0), w2 = w.window(2, 0);
        Resolution r = ChamberCore.resolve(List.of(observe(w0, 0, 0), observe(w2, 2, 0)), P, SEED);
        ChamberCore.decide(r, SEED);
        check("F9.disjoint_partials_not_finalized_as_distinct", r.chambers.isEmpty() && r.groups.size() == 2, "groups=" + r.groups.size() + " (each deferred, not proven distinct)");
    }

    // ---- spacing + dependency propagation on constructed candidates (no voxels)
    static Chamber cham(long id, int cx, int cz) {
        Chamber c = new Chamber();
        c.id = id; c.anchor = id; c.suitable = true; c.cx = cx; c.cz = cz; c.ownerObserved = true; c.status = "UNSET";
        c.boxSuit = true; c.boxAnyY = true; c.boxCx = cx; c.boxCz = cz; c.boxPath = 1; c.boxFloorY = -30;
        return c;
    }
    static Resolution chain(int n, boolean withGroup, long shuffleSeed) {
        Resolution r = new Resolution();
        List<Chamber> cs = new ArrayList<>();
        for (int i = 0; i < n; i++) cs.add(cham(1000 + i, i * 200, 0));
        Collections.shuffle(cs, new Random(shuffleSeed));
        for (Chamber c : cs) r.chambers.put(c.id, c);
        if (withGroup) {
            // unresolved view adjacent to the HIGHEST-priority chamber: the worst case for dependency propagation along the chain
            Chamber top = cs.stream().max((a, b) -> Long.compareUnsigned(ChamberCore.priority(SEED, a.id), ChamberCore.priority(SEED, b.id))).orElseThrow();
            Group g = new Group(); g.provisionalKey = 1; g.minX = top.cx + 100; g.maxX = g.minX + 50; g.minZ = -20; g.maxZ = 20; r.groups.add(g);
        }
        return r;
    }
    static void fSpacingPropagation() {
        int n = 100;
        Resolution base = chain(n, false, 1), blocked = chain(n, true, 2), blocked2 = chain(n, true, 3);
        Outcome ob = ChamberCore.decide(base, SEED), og = ChamberCore.decide(blocked, SEED);
        ChamberCore.decide(blocked2, SEED);
        boolean spacingOk = true;
        List<Chamber> acc = base.chambers.values().stream().filter(c -> c.status.equals("ACCEPTED")).toList();
        for (Chamber a : acc) for (Chamber b : acc) if (a != b && ChamberCore.near(a.cx, a.cz, b.cx, b.cz)) spacingOk = false;
        check("F6.chain_spacing_invariant_no_accepted_pair_below_256", spacingOk && !acc.isEmpty(), "accepted=" + ob.accepted + " rejected=" + ob.rejectedSpacing);
        boolean monotone = true;
        for (Chamber c : base.chambers.values()) { String s = blocked.chambers.get(c.id).status; if (!s.equals(c.status) && !s.equals("BLOCKED_BY_UNRESOLVED")) monotone = false; }
        check("F6.unresolved_only_turns_outcomes_into_blocked_never_changes_decided", monotone && og.blockedByUnresolved > 1 && og.blockedByUnresolved < n, "blocked=" + og.blockedByUnresolved + "/" + n);
        boolean same = true;
        for (Chamber c : blocked.chambers.values()) if (!blocked2.chambers.get(c.id).status.equals(c.status)) same = false;
        check("F6.insertion_order_independent", same, "");
        check("F6.denominator_excludes_blocked", og.accepted + og.blockedByUnresolved + og.rejectedSpacing == og.suitableOwned && og.rollWins + og.rollFailures == og.accepted, "accepted=" + og.accepted);
        // exact-boundary: 255 apart rejects, 256 apart accepts
        Resolution rb = new Resolution();
        Chamber p = cham(1, 0, 0), q = cham(2, 255, 0), s = cham(3, 1000, 0), t = cham(4, 1256, 0);
        for (Chamber c : List.of(p, q, s, t)) rb.chambers.put(c.id, c);
        ChamberCore.decide(rb, SEED);
        check("F6.boundary_255_rejects_256_accepts", !(p.status.equals("ACCEPTED") && q.status.equals("ACCEPTED")) && s.status.equals("ACCEPTED") && t.status.equals("ACCEPTED"), p.status + "," + q.status + "," + s.status + "," + t.status);
    }

    // ---- Stage 0-E placement pipeline on voxel fixtures and constructed chains
    static final class SimpleEnv implements PlacementPipeline.Env {
        final List<int[]> boxes; final String fail;
        SimpleEnv(List<int[]> boxes, String fail) { this.boxes = boxes; this.fail = fail; }
        public List<int[]> structuresNear(int cx, int cz, int r) { return boxes; }
        public String preflight(Chamber c, PlacementPipeline.Measure m, int cx, int cz, int fy) { return fail; }
    }
    static Resolution voxelResolution(World w, int[][] regs) {
        List<Observation> obs = new ArrayList<>();
        for (int[] g : regs) obs.add(observe(w.window(g[0], g[1]), g[0], g[1]));
        return ChamberCore.resolve(obs, P, SEED);
    }
    static void fPlacementPipeline() {
        // two qualifying chambers 200 apart (< 256), each with a 60+ block tunnel so the >=64 traversable path is genuine
        World w = new World().chamber(100, 140, 100, 140).tunnelX(20, 100, 120).chamber(300, 340, 100, 140).tunnelX(340, 440, 120);
        Resolution r = voxelResolution(w, new int[][]{{0, 0}, {1, 0}});
        PlacementPipeline.Result a = PlacementPipeline.run(r, SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(List.of(), null));
        check("E1.two_real_voxel_chambers_one_accepted_one_spacing_rejected", r.chambers.size() == 2 && a.c.getOrDefault("accepted_denominator", 0) == 1 && a.c.getOrDefault("rejected_spacing", 0) == 1 && a.invariantViolations.isEmpty(), a.c + " " + a.invariantViolations);
        long rolled = a.c.getOrDefault("roll_won", 0) + a.c.getOrDefault("roll_failed", 0);
        check("E2.only_survivors_are_rolled_once", rolled == 1 && a.denominator == 1, "rolled=" + rolled);
        // path < 64: isolated chamber has no tunnel
        World iso = new World().chamber(100, 140, 100, 140);
        PlacementPipeline.Result b = PlacementPipeline.run(voxelResolution(iso, new int[][]{{0, 0}}), SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(List.of(), null));
        check("E3.isolated_chamber_fails_64_block_path", b.c.getOrDefault("not_qualifying_path_under_64", 0) == 1 && b.denominator == 0, b.c.toString());
        // reference Y out of band (floor -42 < -40)
        World hi = new World().box(100, 140, 100, 140, -42, -24).box(20, 100, 118, 122, -42, -37);
        PlacementPipeline.Result y = PlacementPipeline.run(voxelResolution(hi, new int[][]{{0, 0}}), SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(List.of(), null));
        check("E4.reference_y_band_enforced", y.c.getOrDefault("not_qualifying_reference_y_out_of_band", 0) == 1 && y.denominator == 0, y.c.toString());
        // structure collision against the body footprint (r=38) rejects pre-roll, corridor-envelope-only touch does not
        List<int[]> body = List.of(new int[]{110, 100, 120, 140}), far = List.of(new int[]{120 + 60, 100, 120 + 70, 140});
        PlacementPipeline.Result s1 = PlacementPipeline.run(r, SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(body, null));
        PlacementPipeline.Result s2 = PlacementPipeline.run(r, SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(far, null));
        check("E5.structure_body_conflict_rejects_envelope_touch_does_not", s1.c.getOrDefault("rejected_structure_body", 0) >= 1 && s2.c.getOrDefault("rejected_structure_body", 0) == 0 && s2.corridorEnvelopeStructureTouch >= 1, s1.c + " | " + s2.c);
        // observed-universe edge: neighbours we never saw -> blocked, not accepted
        PlacementPipeline.Result u = PlacementPipeline.run(r, SEED, PlacementPipeline.Measure.BOX32, new int[]{0, 0, 512, 256}, new SimpleEnv(List.of(), null));
        check("E6.universe_edge_blocks_instead_of_accepting", u.denominator == 0 && u.c.getOrDefault("blocked_by_unresolved", 0) == 2, u.c.toString());
        // denominator invariance (forced preflight failure/success) on the voxel case and on a larger constructed chain
        List<String> inv = PlacementPipeline.denominatorInvariance(r, SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(List.of(), null));
        Resolution ch = chain(100, true, 5);
        List<String> inv2 = PlacementPipeline.denominatorInvariance(ch, SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(List.of(), null));
        check("E7.denominator_invariant_under_preflight_failure_or_success", inv.isEmpty() && inv2.isEmpty(), inv + " " + inv2);
        PlacementPipeline.Result big = PlacementPipeline.run(ch, SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(List.of(), null));
        PlacementPipeline.Result big2 = PlacementPipeline.run(chain(100, true, 6), SEED, PlacementPipeline.Measure.BOX32, null, new SimpleEnv(List.of(), null));
        check("E8.chain_partition_and_order_independence", big.invariantViolations.isEmpty() && big.canonical().equals(big2.canonical()) && big.c.getOrDefault("blocked_by_unresolved", 0) > 1, big.c.toString());
        check("E9.post_roll_failures_never_reselect", big.c.getOrDefault("preflight_failed", 0) + big.c.getOrDefault("placement_reserved", 0) == big.c.getOrDefault("roll_won", 0), big.c.toString());
    }

    // ---- persistence / recovery / immutability / budget
    static void fPersistence() throws Exception {
        Path dir = Files.createTempDirectory("st05d-state");
        Path f = dir.resolve("state.txt");
        StateStore.Header h = new StateStore.Header(ChamberCore.ALGO, P.name(), SEED, "SYNTHETIC");
        Resolution r = chain(6, false, 9);
        ChamberCore.decide(r, SEED);
        Map<Long, StateStore.Rec> recs = new TreeMap<>(Long::compareUnsigned);
        for (Chamber c : r.chambers.values()) recs.put(c.id, StateStore.fromChamber(c, true));
        Map<String, StateStore.Budget> bud = new TreeMap<>();
        StateStore.charge(bud, "g1", 40, 100);
        StateStore.write(f, h, recs, bud);
        StateStore.Loaded L = StateStore.load(f, h);
        check("P.roundtrip_verified", L.headerOk && L.trailerOk && L.recs.size() == recs.size() && !L.damaged() && L.recs.equals(recs), "");
        StateStore.Reconciled rc = StateStore.reconcile(L, recs);
        check("P.restart_restores_identical_decisions", rc.integrityViolations.isEmpty() && rc.restoredVerified == recs.size(), "restored=" + rc.restoredVerified);
        // cumulative budget survives restart; exhaustion is sticky
        StateStore.charge(L.budgets, "g1", 70, 100);
        StateStore.write(f, h, L.recs, L.budgets);
        StateStore.Loaded L2 = StateStore.load(f, h);
        check("P.budget_cumulative_and_sticky_exhaustion", L2.budgets.get("g1").used() == 110 && L2.budgets.get("g1").exhausted() && StateStore.charge(L2.budgets, "g1", 0, 1_000_000).exhausted(), "used=" + L2.budgets.get("g1").used());
        // unauthorised change of a finalized decision
        Map<Long, StateStore.Rec> changed = new TreeMap<>(Long::compareUnsigned);
        for (StateStore.Rec x : recs.values()) changed.put(x.id(), x);
        StateStore.Rec any = recs.values().stream().filter(StateStore.Rec::finalized).findFirst().orElseThrow();
        changed.put(any.id(), new StateStore.Rec(any.id(), any.anchor(), any.ownerRx(), any.ownerRz(), any.size(), any.setHash(), any.suitable(), any.cx(), any.cz(), any.status().equals("ACCEPTED") ? "REJECTED_SPACING" : "ACCEPTED", !any.rollWin(), true, any.proof()));
        StateStore.Reconciled rc2 = StateStore.reconcile(L2, changed);
        check("P.finalized_decision_not_overwritten", rc2.integrityViolations.size() == 1 && rc2.recs.get(any.id()).equals(any), rc2.integrityViolations.toString());
        // damaged record: line tampered
        List<String> lines = new ArrayList<>(Files.readAllLines(f));
        lines.set(2, lines.get(2).replace("size=", "size=9"));
        Path f2 = dir.resolve("damaged.txt");
        Files.write(f2, lines);
        StateStore.Loaded D = StateStore.load(f2, h);
        StateStore.Reconciled rd = StateStore.reconcile(D, recs);
        check("P.damaged_record_quarantined_and_held", D.quarantinedLines.size() == 1 && !D.trailerOk && D.quarantinedFile != null && Files.exists(D.quarantinedFile) && rd.held.size() == 1 && rd.restoredVerified == recs.size() - 1, "quarantined=" + D.quarantinedLines.size() + " held=" + rd.held.size());
        // wrong header (other seed): whole file renamed aside, never deleted
        Path f3 = dir.resolve("seed.txt");
        Files.copy(f, f3);
        StateStore.Loaded W = StateStore.load(f3, new StateStore.Header(ChamberCore.ALGO, P.name(), SEED + 1, "SYNTHETIC"));
        check("P.mismatched_world_quarantined_not_deleted", !W.headerOk && W.recs.isEmpty() && W.quarantinedFile != null && Files.exists(W.quarantinedFile) && !Files.exists(f3), "");
        // truncated file (crash mid write is prevented by atomic move; simulate torn external copy)
        Path f4 = dir.resolve("torn.txt");
        List<String> all = Files.readAllLines(f);
        Files.write(f4, all.subList(0, all.size() - 2));
        StateStore.Loaded T = StateStore.load(f4, h);
        check("P.truncated_file_detected", !T.trailerOk && T.damaged(), "");
    }
}
