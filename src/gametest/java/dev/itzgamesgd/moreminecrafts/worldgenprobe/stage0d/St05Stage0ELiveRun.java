package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d.ChamberCore.*;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d.PlacementPipeline.Measure;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Stage 0-E live driver (Minecraft 26.3 dedicated server): N x N contiguous ownership regions, ONE shared plan-time snapshot cut into
 * independent 512x512 observation windows (the pure-function property was already verified in Stage 0-D; one window is re-derived
 * independently and compared here), core identity from Stage 0-D, then the placement pipeline with real structure data and a post-roll
 * preflight against final terrain. No arena block is ever written: "placement reserved" is a persisted record only.
 */
final class St05Stage0ELiveRun {
    private St05Stage0ELiveRun() { }

    static final com.sun.management.ThreadMXBean TMX = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    static Window bigSnapshot(ServerLevel level, int ox, int oz, int size, int y0, int h, int threads, String order) throws Exception {
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        boolean[] open = new boolean[size * size * h];
        List<Integer> xs = new ArrayList<>();
        for (int x = 0; x < size; x++) xs.add(x);
        if (order.equals("rev")) Collections.reverse(xs); else if (order.equals("shuf")) Collections.shuffle(xs, new Random(77));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> fs = new ArrayList<>();
        for (int x : xs) fs.add(pool.submit(() -> {
            for (int z = 0; z < size; z++) {
                NoiseColumn col = gen.getBaseColumn(ox + x, oz + z, level, rs);
                for (int y = 0; y < h; y++) if (col.getBlock(y0 + y).isAir()) open[(x * size + z) * h + y] = true;
            }
        }));
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        return new Window(ox, y0, oz, size, size, h, open, St05Stage0DLiveProbe.SNAPSHOT);
    }

    static Window cut(Window big, int rx, int rz) {
        int ox = rx * ChamberCore.OWN - ChamberCore.MARGIN, oz = rz * ChamberCore.OWN - ChamberCore.MARGIN, n = St05Stage0DLiveProbe.WIN, h = big.h;
        boolean[] open = new boolean[n * n * h];
        for (int x = 0; x < n; x++) System.arraycopy(big.open, ((ox - big.ox + x) * big.wz + (oz - big.oz)) * h, open, x * n * h, n * h);
        return new Window(ox, big.oy, oz, n, n, h, open, big.snapshotKind);
    }

    /** Real-world environment: structure data from STRUCTURE_STARTS chunks, preflight from FULL final terrain. */
    static final class LiveEnv implements PlacementPipeline.Env {
        final ServerLevel level;
        final Map<Long, List<int[]>> chunkBoxes = new HashMap<>();
        final Map<String, Integer> structureHits = new TreeMap<>();
        long chunkLoads;
        LiveEnv(ServerLevel level) { this.level = level; }

        List<int[]> boxesOf(int cxk, int czk) {
            return chunkBoxes.computeIfAbsent(((long) cxk << 32) ^ (czk & 0xFFFFFFFFL), k -> {
                chunkLoads++;
                ChunkAccess ch = level.getChunk(cxk, czk, ChunkStatus.STRUCTURE_STARTS, true);
                List<int[]> out = new ArrayList<>();
                for (var e : ch.getAllStarts().entrySet()) {
                    StructureStart st = e.getValue();
                    if (!st.isValid()) continue;
                    String name = String.valueOf(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(e.getKey()));
                    for (var piece : st.getPieces()) {
                        var b = piece.getBoundingBox();
                        out.add(new int[]{b.minX(), b.minZ(), b.maxX(), b.maxZ()});
                    }
                    structureHits.merge(name, 1, Integer::sum);
                }
                return out;
            });
        }

        public List<int[]> structuresNear(int cx, int cz, int r) {
            int reach = r + 160;   // structures start in one chunk but may extend ~10 chunks
            List<int[]> out = new ArrayList<>();
            for (int x = Math.floorDiv(cx - reach, 16); x <= Math.floorDiv(cx + reach, 16); x++)
                for (int z = Math.floorDiv(cz - reach, 16); z <= Math.floorDiv(cz + reach, 16); z++)
                    for (int[] b : boxesOf(x, z)) if (b[0] <= cx + r && b[2] >= cx - r && b[1] <= cz + r && b[3] >= cz - r) out.add(b);
            return out;
        }

        public String preflight(Chamber c, Measure m, int cx, int cz, int floorY) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int blocked = 0, total = 0;
            for (int dx = 0; dx < ChamberCore.SUIT_W; dx++) for (int dz = 0; dz < ChamberCore.SUIT_W; dz++) {
                double ex = dx + 0.5 - 16, ez = dz + 0.5 - 16;
                if (m == Measure.DISK32 && ex * ex + ez * ez > 256) continue;
                for (int dy = 0; dy < ChamberCore.SUIT_H; dy++) {
                    total++;
                    if (!level.getBlockState(p.set(cx - 16 + dx, floorY + dy, cz - 16 + dz)).isAir()) blocked++;
                }
            }
            if (floorY < ChamberCore.REF_Y_MIN || floorY > ChamberCore.REF_Y_MAX) return "REFERENCE_Y_OUT_OF_BAND";
            if (blocked > 0) return "FINAL_TERRAIN_NOT_OPEN blocked=" + blocked + "/" + total;
            return null;
        }
    }

    static StateStore.Rec rec(Chamber c, PlacementPipeline.Result res, Measure m, long seed) {
        String st = res.status.getOrDefault(c.id, "UNSET");
        boolean disk = m == Measure.DISK32;
        int cx = disk ? c.diskCx : c.boxCx, cz = disk ? c.diskCz : c.boxCz;
        String ev = c.id + ":" + c.anchor + ":" + c.setHash + ":" + st + ":" + res.reason.getOrDefault(c.id, "") + ":" + cx + "," + cz;
        return new StateStore.Rec(c.id, c.anchor, c.ownerRx, c.ownerRz, c.size, c.setHash, true, cx, cz, st, ChamberCore.rollWin(seed, c.id), StateStore.isFinalStatus(st), ChamberCore.sha256(ev).substring(0, 24));
    }

    static void run(MinecraftServer server, ServerLevel level, String mode, Path out, Map<String, Object> doc) throws Exception {
        long seed = level.getSeed();
        int rx0 = Integer.getInteger("mm.st05e.rx0", 0), rz0 = Integer.getInteger("mm.st05e.rz0", 0), N = Integer.getInteger("mm.st05e.n", 6);
        int y0 = Integer.getInteger("mm.st05d.y0", -64), h = Integer.getInteger("mm.st05d.h", 80), threads = Integer.getInteger("mm.st05d.threads", 4);
        Path stateDir = Path.of(System.getProperty("mm.st05d.statedir", "build/st05-worldgen-probe/stage0e-state"));
        Files.createDirectories(stateDir);
        boolean restart = mode.equals("e-restart");
        String order = restart ? "rev" : "fwd";
        int[] universe = {rx0 * ChamberCore.OWN, rz0 * ChamberCore.OWN, (rx0 + N) * ChamberCore.OWN, (rz0 + N) * ChamberCore.OWN};
        doc.put("seed", seed); doc.put("mode", mode); doc.put("generation_order", order);
        doc.put("universe_blocks_minX_minZ_maxX_maxZ_exclusive", List.of(universe[0], universe[1], universe[2], universe[3]));
        doc.put("regions", N + "x" + N + " (" + N * N + " ownership regions, windows 512x512)");
        doc.put("band", y0 + ".." + (y0 + h));
        long t0 = System.nanoTime();
        int bigSize = N * ChamberCore.OWN + 2 * ChamberCore.MARGIN;
        Window big = bigSnapshot(level, rx0 * ChamberCore.OWN - ChamberCore.MARGIN, rz0 * ChamberCore.OWN - ChamberCore.MARGIN, bigSize, y0, h, threads, order);
        doc.put("snapshot_ms", (System.nanoTime() - t0) / 1_000_000);
        doc.put("snapshot_digest", Long.toHexString(big.digest()));
        // independent re-derivation of one window (different column order, per-window snapshot) must equal the cut
        int crx = rx0 + N / 2, crz = rz0 + N / 2;
        Window own = St05Stage0DLiveProbe.planWindow(level, crx, crz, y0, h, threads, "shuf");
        doc.put("independent_window_matches_cut", own.digest() == cut(big, crx, crz).digest());

        List<Window> wins = new ArrayList<>();
        List<int[]> regs = new ArrayList<>();
        for (int rx = rx0; rx < rx0 + N; rx++) for (int rz = rz0; rz < rz0 + N; rz++) { regs.add(new int[]{rx, rz}); wins.add(cut(big, rx, rz)); }
        big = null;
        LiveEnv env = new LiveEnv(level);
        List<Params> params = St05Stage0DLiveProbe.paramSets();
        Map<String, Object> per = new LinkedHashMap<>();
        doc.put("params", per);
        for (Params p : params) {
            Map<String, Object> pm = new LinkedHashMap<>();
            per.put(p.name(), pm);
            List<Observation> obs = new ArrayList<>();
            long cpu = 0, coreVox = 0;
            for (int i = 0; i < regs.size(); i++) {
                ExtractStats st = new ExtractStats();
                long c0 = TMX.getCurrentThreadCpuTime();
                List<Comp> cs = ChamberCore.extract(wins.get(i), p, seed, regs.get(i)[0], regs.get(i)[1], st);
                cpu += TMX.getCurrentThreadCpuTime() - c0; coreVox += st.coreVoxels;
                obs.add(new Observation(regs.get(i)[0], regs.get(i)[1], cs, wins.get(i).digest()));
            }
            pm.put("extract_cpu_ms", cpu / 1_000_000); pm.put("core_voxels", coreVox);
            long r0 = TMX.getCurrentThreadCpuTime();
            Resolution base = ChamberCore.resolve(obs, p, seed);
            pm.put("resolve_cpu_ms", (TMX.getCurrentThreadCpuTime() - r0) / 1_000_000);
            pm.put("resolution_digest", ChamberCore.sha256(ChamberCore.canonical(base, ChamberCore.decide(base, seed))));
            pm.put("owned_chambers", base.chambers.size()); pm.put("unresolved_groups", base.groups.size()); pm.put("identity_conflicts", base.conflicts.size());
            pm.put("partial_views", base.partialsTotal); pm.put("duplicate_complete_observations", base.duplicateObservations);
            Map<String, Object> measures = new LinkedHashMap<>();
            pm.put("measures", measures);
            for (Measure m : Measure.values()) {
                Map<String, Object> mm = new LinkedHashMap<>();
                measures.put(m.name(), mm);
                long q0 = TMX.getCurrentThreadCpuTime();
                PlacementPipeline.Result res = PlacementPipeline.run(base, seed, m, universe, env);
                mm.put("pipeline_cpu_ms_incl_live_chunk_io", (TMX.getCurrentThreadCpuTime() - q0) / 1_000_000);
                mm.put("counters", res.c); mm.put("denominator", res.denominator); mm.put("deferred_fraction", res.deferredFraction);
                mm.put("footprint_only_rejections", res.footprintOnlyRejections);
                mm.put("corridor_envelope_touches_structure", res.corridorEnvelopeStructureTouch);
                mm.put("corridor_envelope_arena_overlap_pairs", res.corridorEnvelopeArenaOverlapPairs);
                mm.put("invariant_violations", res.invariantViolations);
                mm.put("denominator_invariance_violations", PlacementPipeline.denominatorInvariance(base, seed, m, universe, new PlacementPipeline.Env() {
                    public List<int[]> structuresNear(int cx, int cz, int r) { return env.structuresNear(cx, cz, r); }
                    public String preflight(Chamber c, Measure mmm, int cx, int cz, int fy) { return null; }
                }));
                // order independence of the whole pipeline
                List<Observation> rev = new ArrayList<>(obs); Collections.reverse(rev);
                List<Observation> shuf = new ArrayList<>(obs); Collections.shuffle(shuf, new Random(5));
                String d0 = ChamberCore.sha256(res.canonical());
                mm.put("pipeline_digest", d0);
                mm.put("pipeline_identical_fwd_rev_shuffle", d0.equals(ChamberCore.sha256(PlacementPipeline.run(ChamberCore.resolve(rev, p, seed), seed, m, universe, env).canonical()))
                    && d0.equals(ChamberCore.sha256(PlacementPipeline.run(ChamberCore.resolve(shuf, p, seed), seed, m, universe, env).canonical())));
                List<String> cands = new ArrayList<>();
                for (Chamber c : base.chambers.values()) {
                    String stt = res.status.get(c.id);
                    if (stt == null || stt.equals("NOT_QUALIFYING") || stt.equals("OUTSIDE_UNIVERSE")) continue;
                    boolean disk = m == Measure.DISK32;
                    cands.add(Long.toHexString(c.id) + " owner=" + c.ownerRx + "," + c.ownerRz + " center=" + (disk ? c.diskCx : c.boxCx) + "," + (disk ? c.diskCz : c.boxCz)
                        + " floorY=" + (disk ? c.diskFloorY : c.boxFloorY) + " path=" + (disk ? c.diskPath : c.boxPath) + " prio=" + Long.toHexString(ChamberCore.priority(seed, c.id))
                        + " roll=" + (ChamberCore.rollWin(seed, c.id) ? "WIN" : "FAIL") + " -> " + stt + " " + res.reason.getOrDefault(c.id, ""));
                }
                mm.put("candidates", cands);
                // persistence of every decision record
                StateStore.Header hd = new StateStore.Header(ChamberCore.ALGO + "+placement-v1", p.name() + "/" + m.name(), seed, St05Stage0DLiveProbe.SNAPSHOT + "|U" + rx0 + "," + rz0 + "," + N);
                Path sf = stateDir.resolve("state-E-" + p.name() + "-" + m.name() + ".txt");
                Map<Long, StateStore.Rec> fresh = new TreeMap<>(Long::compareUnsigned);
                for (Chamber c : base.chambers.values()) {
                    String stt = res.status.get(c.id);
                    if (stt == null || stt.equals("OUTSIDE_UNIVERSE")) continue;
                    fresh.put(c.id, rec(c, res, m, seed));
                }
                Map<String, Object> pr = new LinkedHashMap<>();
                mm.put("persistence", pr);
                if (!restart) {
                    StateStore.write(sf, hd, fresh, new TreeMap<>());
                    StateStore.Loaded L = StateStore.load(sf, hd);
                    pr.put("records", fresh.size()); pr.put("finalized", fresh.values().stream().filter(StateStore.Rec::finalized).count()); pr.put("reloaded_ok", L.headerOk && L.trailerOk && L.recs.equals(fresh));
                } else {
                    StateStore.Loaded L = StateStore.load(sf, hd);
                    StateStore.Reconciled R = StateStore.reconcile(L, fresh);
                    long fin = L.recs.values().stream().filter(StateStore.Rec::finalized).count();
                    pr.put("loaded_ok", L.headerOk && L.trailerOk); pr.put("stored_records", L.recs.size()); pr.put("stored_finalized", fin);
                    pr.put("restored_verified", R.restoredVerified); pr.put("integrity_violations", R.integrityViolations);
                    pr.put("restart_identical", L.headerOk && L.trailerOk && R.integrityViolations.isEmpty() && R.restoredVerified == fin && L.recs.equals(fresh));
                }
            }
            obs = null;
        }
        doc.put("structure_starts_chunks_loaded", env.chunkLoads);
        doc.put("structure_hits_by_type", env.structureHits);
        doc.put("heap_max_mb", Runtime.getRuntime().maxMemory() >> 20);
        doc.put("total_ms", (System.nanoTime() - t0) / 1_000_000);
    }
}
