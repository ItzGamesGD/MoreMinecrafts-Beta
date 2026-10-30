package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-05 Stage 0 feasibility probe for audit gates A02 (cavern identity / spacing) and A03 (plan before chunk writes).
 * ISOLATED scratch code: no structure, feature or worldgen hook is registered; inert unless {@code -Dmm.st05.out} is set.
 * Driven by tools/st05-worldgen-probe/run_probe.sh. All thresholds and spacing values below are probe parameters used to
 * measure behaviour; they are NOT design decisions and are not adopted by this probe.
 */
public final class St05CavernIdentityProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st05-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Cavern analysis band (world Y). Wider than the owner's -40..-10 reference-Y band so connectivity is not cut early. */
    private static final int Y0 = -48, Y1 = 0, H = Y1 - Y0;
    private static final long SALT_PRIORITY = 0x5DEECE66DL, SALT_ROLL = 0xB5297A4DL;

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st05.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                run(server, Path.of(out));
            } catch (Throwable t) {
                LOG.error("st05 probe failed", t);
                try {
                    Files.createDirectories(Path.of(out).getParent());
                    Files.writeString(Path.of(out), GSON.toJson(Map.of("error", String.valueOf(t))), StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // missing output file is the failure signal for the driver
                }
            }
            Runtime.getRuntime().halt(0);
        });
    }

    // ---------------------------------------------------------------- mask + segmentation

    /** Dense open-voxel mask over an XZ window (array coords) and the analysis band. */
    private record Mask(int ox, int oz, int wx, int wz, boolean[] open, long fluid) {
        int idx(int x, int z, int y) { return (x * wz + z) * H + y; }
    }

    private static final class Comp {
        int size, ax, az, ay, minX = Integer.MAX_VALUE, maxX = -1, minZ = Integer.MAX_VALUE, maxZ = -1, minY = Integer.MAX_VALUE, maxY = -1;
        boolean touchXZ, touchY;
        long id;
        int chamberX = -1, chamberZ = -1;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static long cavernId(long seed, int wx, int wy, int wz) {
        return mix(seed ^ mix(wx * 0x9E3779B97F4A7C15L + mix(wy * 0xC2B2AE3D27D4EB4FL + mix(wz * 0x165667B19E3779F9L))));
    }

    /** 6-connected flood fill restricted to the box [bx0,bx1) x [bz0,bz1); anchor = first voxel in (x,z,y) scan order. */
    private static List<Comp> segment(Mask m, int bx0, int bx1, int bz0, int bz1, long seed, int[] labelOut) {
        java.util.Arrays.fill(labelOut, -1);
        List<Comp> comps = new ArrayList<>();
        int[] stack = new int[1 << 16];
        for (int x = bx0; x < bx1; x++) for (int z = bz0; z < bz1; z++) for (int y = 0; y < H; y++) {
            int i = m.idx(x, z, y);
            if (!m.open[i] || labelOut[i] != -1) continue;
            Comp c = new Comp();
            int label = comps.size();
            c.ax = x; c.az = z; c.ay = y;
            int sp = 0;
            stack[sp++] = i;
            labelOut[i] = label;
            while (sp > 0) {
                int cur = stack[--sp];
                int cy = cur % H, rest = cur / H, cz = rest % m.wz, cx = rest / m.wz;
                c.size++;
                if (cx < c.minX) c.minX = cx;
                if (cx > c.maxX) c.maxX = cx;
                if (cz < c.minZ) c.minZ = cz;
                if (cz > c.maxZ) c.maxZ = cz;
                if (cy < c.minY) c.minY = cy;
                if (cy > c.maxY) c.maxY = cy;
                if (cx == bx0 || cx == bx1 - 1 || cz == bz0 || cz == bz1 - 1) c.touchXZ = true;
                if (cy == 0 || cy == H - 1) c.touchY = true;
                for (int d = 0; d < 6; d++) {
                    int nx = cx + (d == 0 ? 1 : d == 1 ? -1 : 0), nz = cz + (d == 2 ? 1 : d == 3 ? -1 : 0), ny = cy + (d == 4 ? 1 : d == 5 ? -1 : 0);
                    if (nx < bx0 || nx >= bx1 || nz < bz0 || nz >= bz1 || ny < 0 || ny >= H) continue;
                    int ni = m.idx(nx, nz, ny);
                    if (!m.open[ni] || labelOut[ni] != -1) continue;
                    labelOut[ni] = label;
                    if (sp == stack.length) stack = java.util.Arrays.copyOf(stack, sp * 2);
                    stack[sp++] = ni;
                }
            }
            c.id = cavernId(seed, m.ox + c.ax, Y0 + c.ay, m.oz + c.az);
            comps.add(c);
        }
        return comps;
    }

    /**
     * Chamber proxy for Q09 (upper-bound necessary condition, NOT the final measurement): a 32x32 XZ footprint where every
     * column holds a vertical run of >=18 open voxels of the same cavern (run alignment between columns is not checked).
     */
    private static void chamberProxy(Mask m, int[] label, List<Comp> comps, int bx0, int bx1, int bz0, int bz1) {
        Map<Integer, boolean[]> cols = new HashMap<>();
        int w = bz1 - bz0;
        for (int x = bx0; x < bx1; x++) for (int z = bz0; z < bz1; z++) {
            int run = 0, prev = -1;
            for (int y = 0; y < H; y++) {
                int l = label[m.idx(x, z, y)];
                if (l >= 0 && l == prev) run++; else run = l >= 0 ? 1 : 0;
                prev = l;
                if (l >= 0 && run >= 18 && comps.get(l).size >= 4000) cols.computeIfAbsent(l, k -> new boolean[(bx1 - bx0) * w])[(x - bx0) * w + (z - bz0)] = true;
            }
        }
        for (var e : cols.entrySet()) {
            boolean[] ok = e.getValue();
            int[] sat = new int[((bx1 - bx0) + 1) * (w + 1)];
            for (int x = 0; x < bx1 - bx0; x++) for (int z = 0; z < w; z++)
                sat[(x + 1) * (w + 1) + z + 1] = (ok[x * w + z] ? 1 : 0) + sat[x * (w + 1) + z + 1] + sat[(x + 1) * (w + 1) + z] - sat[x * (w + 1) + z];
            search:
            for (int x = 0; x + 32 <= bx1 - bx0; x++) for (int z = 0; z + 32 <= w; z++) {
                int s = sat[(x + 32) * (w + 1) + z + 32] - sat[x * (w + 1) + z + 32] - sat[(x + 32) * (w + 1) + z] + sat[x * (w + 1) + z];
                if (s == 32 * 32) {
                    Comp c = comps.get(e.getKey());
                    c.chamberX = bx0 + x + 16;
                    c.chamberZ = bz0 + z + 16;
                    break search;
                }
            }
        }
    }

    // ---------------------------------------------------------------- mask sources

    /** Plan-time source: pure function of seed via ChunkGenerator.getBaseColumn (no chunk is loaded or written). */
    private static Mask planMask(ServerLevel level, int cx0, int cz0, int chunks, int threads) throws Exception {
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        int wx = chunks * 16, ox = cx0 * 16, oz = cz0 * 16;
        boolean[] open = new boolean[wx * wx * H];
        long[] fluid = new long[1];
        ExecutorService pool = threads > 1 ? Executors.newFixedThreadPool(threads) : null;
        List<int[]> cols = new ArrayList<>();
        for (int x = 0; x < wx; x++) for (int z = 0; z < wx; z++) cols.add(new int[]{x, z});
        if (threads > 1) Collections.shuffle(cols, new Random(99));
        List<Future<?>> fs = new ArrayList<>();
        int stripe = Math.max(1, cols.size() / (Math.max(1, threads) * 8));
        for (int s = 0; s < cols.size(); s += stripe) {
            final int from = s, to = Math.min(cols.size(), s + stripe);
            Runnable job = () -> {
                long f = 0;
                for (int k = from; k < to; k++) {
                    int[] c = cols.get(k);
                    NoiseColumn col = gen.getBaseColumn(ox + c[0], oz + c[1], level, rs);
                    for (int y = 0; y < H; y++) {
                        BlockState st = col.getBlock(Y0 + y);
                        if (st.isAir()) open[(c[0] * wx + c[1]) * H + y] = true;
                        else if (!st.getFluidState().isEmpty()) f++;
                    }
                }
                synchronized (fluid) { fluid[0] += f; }
            };
            if (pool == null) job.run(); else fs.add(pool.submit(job));
        }
        for (Future<?> f : fs) f.get();
        if (pool != null) pool.shutdown();
        return new Mask(ox, oz, wx, wx, open, fluid[0]);
    }

    /** Final source: block states of FULL chunks (after terrain, carvers and features), already generated by the caller. */
    private static Mask finalMask(ServerLevel level, int cx0, int cz0, int chunks) {
        int wx = chunks * 16, ox = cx0 * 16, oz = cz0 * 16;
        boolean[] open = new boolean[wx * wx * H];
        long fluid = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = 0; x < wx; x++) for (int z = 0; z < wx; z++) for (int y = 0; y < H; y++) {
            BlockState st = level.getBlockState(p.set(ox + x, Y0 + y, oz + z));
            if (st.isAir()) open[(x * wx + z) * H + y] = true;
            else if (!st.getFluidState().isEmpty()) fluid++;
        }
        return new Mask(ox, oz, wx, wx, open, fluid);
    }

    private static long digest(Mask m) {
        long h = 1125899906842597L;
        for (int i = 0; i < m.open.length; i++) h = 31 * h + (m.open[i] ? 1 : 0) + (i & 7);
        return mix(h);
    }

    // ---------------------------------------------------------------- run

    private static void run(MinecraftServer server, Path out) throws Exception {
        String mode = System.getProperty("mm.st05.mode", "plan");
        int finalRadius = Integer.getInteger("mm.st05.finalradius", 4);
        int planRadius = Integer.getInteger("mm.st05.planradius", 12);
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        long seed = level.getSeed();
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("mode", mode);
        doc.put("seed", seed);
        doc.put("y_band", Y0 + ".." + Y1);

        if (mode.equals("plan")) {
            planAnalysis(level, seed, planRadius, doc);
        } else {
            finalAnalysis(server, level, seed, mode, finalRadius, doc);
        }
        if (!mode.equals("plan") && !mode.equals("reload")) server.saveAllChunks(true, true, true);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(doc), StandardCharsets.UTF_8);
        LOG.info("ST05_PROBE_DONE mode={} seed={}", mode, seed);
    }

    private static void finalAnalysis(MinecraftServer server, ServerLevel level, long seed, String mode, int r, Map<String, Object> doc) throws Exception {
        int chunks = 2 * r + 1;
        List<int[]> order = new ArrayList<>();
        for (int cx = -r; cx <= r; cx++) for (int cz = -r; cz <= r; cz++) order.add(new int[]{cx, cz});
        switch (mode) {
            case "rev" -> Collections.reverse(order);
            case "shuf", "par" -> Collections.shuffle(order, new Random(12345));
            default -> { }
        }
        long t0 = System.nanoTime();
        if (!mode.equals("reload") && !mode.equals("fwd") && !mode.equals("rev") && !mode.equals("shuf") && !mode.equals("par")) throw new IllegalArgumentException(mode);
        if (mode.equals("par")) {
            var cache = level.getChunkSource();
            List<Object> futures = new ArrayList<>();
            for (int[] c : order) futures.add(cache.getChunkFuture(c[0], c[1], ChunkStatus.FULL, true));
        }
        for (int[] c : order) level.getChunk(c[0], c[1], ChunkStatus.FULL, true);
        long genMs = (System.nanoTime() - t0) / 1_000_000;

        Mask fin = finalMask(level, -r, -r, chunks);
        doc.put("chunk_order", mode);
        doc.put("window_chunks", chunks);
        doc.put("gen_ms", genMs);
        doc.put("final_mask_digest", Long.toHexString(digest(fin)));
        doc.put("final_open_voxels", count(fin));
        doc.put("final_fluid_voxels", fin.fluid());

        // Structure starts digest (analogue for A03: a start is a whole plan stored once, referenced by covered chunks).
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        TreeMap<String, String> starts = new TreeMap<>();
        long refCount = 0;
        for (int cx = -r; cx <= r; cx++) for (int cz = -r; cz <= r; cz++) {
            ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
            for (var e : chunk.getAllStarts().entrySet()) {
                StructureStart s = e.getValue();
                if (!s.isValid()) continue;
                starts.put(reg.getKey(e.getKey()) + "@" + cx + "," + cz, s.getBoundingBox().toString() + "#" + s.getPieces().size());
            }
            for (var e : chunk.getAllReferences().entrySet()) refCount += e.getValue().size();
        }
        doc.put("structure_starts_in_window", starts.size());
        doc.put("structure_starts_digest", Long.toHexString(mix(starts.toString().hashCode())));
        doc.put("structure_reference_count", refCount);

        int[] label = new int[fin.open.length];
        List<Comp> comps = segment(fin, 0, fin.wx, 0, fin.wz, seed, label);
        doc.put("final_components", comps.size());
        TreeMap<String, Long> ids = new TreeMap<>();
        for (Comp c : comps) ids.put(Long.toHexString(c.id), (long) c.size);
        doc.put("final_component_ids_digest", Long.toHexString(mix(ids.toString().hashCode())));

        if (mode.equals("fwd")) {
            // Plan-vs-final divergence inside the same window (A03: what a plan-time cave read can and cannot see).
            Mask plan = planMask(level, -r, -r, chunks, 1);
            long onlyPlan = 0, onlyFinal = 0, both = 0;
            for (int i = 0; i < plan.open.length; i++) {
                if (plan.open[i] && fin.open[i]) both++;
                else if (plan.open[i]) onlyPlan++;
                else if (fin.open[i]) onlyFinal++;
            }
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("plan_open", count(plan));
            d.put("final_open", count(fin));
            d.put("open_in_both", both);
            d.put("open_in_plan_only", onlyPlan);
            d.put("open_in_final_only", onlyFinal);
            d.put("plan_fluid_voxels", plan.fluid());
            d.put("final_fluid_voxels", fin.fluid());
            d.put("plan_mask_digest", Long.toHexString(digest(plan)));
            int[] pl = new int[plan.open.length];
            List<Comp> pc = segment(plan, 0, plan.wx, 0, plan.wz, seed, pl);
            chamberProxy(plan, pl, pc, 0, plan.wx, 0, plan.wz);
            chamberProxy(fin, label, comps, 0, fin.wx, 0, fin.wz);
            d.put("plan_components", pc.size());
            d.put("final_components", comps.size());
            d.put("plan_chamber_proxy_caverns", pc.stream().filter(c -> c.chamberX >= 0).count());
            d.put("final_chamber_proxy_caverns", comps.stream().filter(c -> c.chamberX >= 0).count());
            doc.put("plan_vs_final", d);
        }
    }

    private static long count(Mask m) {
        long n = 0;
        for (boolean b : m.open) if (b) n++;
        return n;
    }

    private static void planAnalysis(ServerLevel level, long seed, int R, Map<String, Object> doc) throws Exception {
        int chunks = 2 * R;
        long t0 = System.nanoTime();
        Mask m = planMask(level, -R, -R, chunks, 1);
        long singleMs = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        Mask m2 = planMask(level, -R, -R, chunks, 4);
        long multiMs = (System.nanoTime() - t0) / 1_000_000;
        doc.put("plan_window_chunks", chunks);
        doc.put("plan_mask_ms_single_thread", singleMs);
        doc.put("plan_mask_ms_4_threads_shuffled_columns", multiMs);
        doc.put("plan_mask_digest_single", Long.toHexString(digest(m)));
        doc.put("plan_mask_digest_4_threads_shuffled", Long.toHexString(digest(m2)));
        doc.put("plan_mask_order_independent", digest(m) == digest(m2));
        doc.put("plan_open_voxels", count(m));

        int[] label = new int[m.open.length];
        // Nested windows (in blocks, centred): shows how cavern identity/size depends on the analysis window.
        Map<String, Object> windows = new LinkedHashMap<>();
        Map<Integer, List<Comp>> byHalf = new LinkedHashMap<>();
        for (int half : new int[]{R / 3, 2 * R / 3, R}) {
            int lo = (R - half) * 16, hi = (R + half) * 16;
            List<Comp> comps = segment(m, lo, hi, lo, hi, seed, label);
            byHalf.put(half, comps);
            long open = 0, openInTouching = 0;
            int maxSize = 0, touching = 0;
            for (Comp c : comps) {
                open += c.size;
                if (c.touchXZ) { touching++; openInTouching += c.size; }
                maxSize = Math.max(maxSize, c.size);
            }
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("window_blocks", (hi - lo));
            w.put("components", comps.size());
            w.put("components_touching_window_edge", touching);
            w.put("open_voxel_fraction_in_edge_touching_components", open == 0 ? 0 : (double) openInTouching / open);
            w.put("largest_component_voxels", maxSize);
            windows.put("half_chunks_" + half, w);
        }
        doc.put("window_dependence", windows);

        // Identity consistency: components that are EDGE-INTERIOR in the small window must keep their id in the full window.
        List<Comp> full = byHalf.get(R);
        Map<Long, Comp> fullById = new HashMap<>();
        for (Comp c : full) fullById.put(c.id, c);
        int small = R / 3, mid = 2 * R / 3;
        Map<String, Object> idc = new LinkedHashMap<>();
        for (int half : new int[]{small, mid}) {
            int interiorKept = 0, interiorLost = 0, edgeKept = 0, edgeChanged = 0;
            for (Comp c : byHalf.get(half)) {
                boolean same = fullById.containsKey(c.id);
                if (!c.touchXZ) { if (same) interiorKept++; else interiorLost++; }
                else { if (same) edgeKept++; else edgeChanged++; }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("interior_components_id_kept_in_full_window", interiorKept);
            row.put("interior_components_id_lost", interiorLost);
            row.put("edge_touching_components_id_kept_by_anchor_luck", edgeKept);
            row.put("edge_touching_components_id_changed", edgeChanged);
            idc.put("half_chunks_" + half + "_vs_" + R, row);
        }
        doc.put("identity_consistency_across_windows", idc);

        // Spacing/priority experiment over resolved (edge-interior) chamber-proxy caverns of the full window.
        int[] fullLabel = new int[m.open.length];
        List<Comp> comps = segment(m, 0, m.wx, 0, m.wz, seed, fullLabel);
        chamberProxy(m, fullLabel, comps, 0, m.wx, 0, m.wz);
        List<Comp> qual = new ArrayList<>(), unresolved = new ArrayList<>();
        for (Comp c : comps) {
            if (c.chamberX < 0) continue;
            if (c.touchXZ) unresolved.add(c); else qual.add(c);
        }
        doc.put("chamber_proxy_caverns_resolved_interior", qual.size());
        doc.put("chamber_proxy_caverns_unresolved_edge_touching", unresolved.size());
        List<Long> sizes = new ArrayList<>();
        for (Comp c : qual) sizes.add((long) c.size);
        doc.put("chamber_proxy_resolved_sizes_voxels", sizes);
        Set<Long> uniq = new HashSet<>();
        for (Comp c : comps) uniq.add(c.id);
        doc.put("duplicate_ids_in_window", comps.size() - uniq.size());

        Map<String, Object> spacing = new LinkedHashMap<>();
        for (int s : new int[]{64, 96, 128, 192}) spacing.put("S" + s, spacingExperiment(qual, seed, s));
        doc.put("spacing_policy_experiment_ILLUSTRATIVE_not_adopted", spacing);
    }

    private static boolean near(Comp a, Comp b, int s) {
        long dx = a.chamberX - b.chamberX, dz = a.chamberZ - b.chamberZ;
        return dx * dx + dz * dz < (long) s * s;
    }

    private static boolean wins(Comp c) { return (mix(c.id ^ SALT_ROLL) >>> 11) * 0x1.0p-53 < 0.75; }
    private static long prio(Comp c) { return mix(c.id ^ SALT_PRIORITY); }

    /** Compares two deterministic spacing orderings and a global greedy reference on the same eligible set. */
    private static Map<String, Object> spacingExperiment(List<Comp> eligible, long seed, int s) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("eligible_E", eligible.size());
        int conflictPairs = 0;
        for (int i = 0; i < eligible.size(); i++) for (int j = i + 1; j < eligible.size(); j++) if (near(eligible.get(i), eligible.get(j), s)) conflictPairs++;
        r.put("conflict_pairs", conflictPairs);
        // P1: spacing priority BEFORE the roll (local maxima of priority among E roll once).
        List<Comp> p1 = new ArrayList<>();
        for (Comp c : eligible) {
            boolean top = true;
            for (Comp o : eligible) if (o != c && near(c, o, s) && prio(o) > prio(c)) { top = false; break; }
            if (top) p1.add(c);
        }
        int p1Placed = (int) p1.stream().filter(St05CavernIdentityProbe::wins).count();
        r.put("P1_spacing_before_roll_rolled", p1.size());
        r.put("P1_placed", p1Placed);
        // P2: roll first; among roll winners, local maxima of priority place.
        List<Comp> winners = eligible.stream().filter(St05CavernIdentityProbe::wins).toList();
        int p2Placed = 0;
        for (Comp c : winners) {
            boolean top = true;
            for (Comp o : winners) if (o != c && near(c, o, s) && prio(o) > prio(c)) { top = false; break; }
            if (top) p2Placed++;
        }
        r.put("P2_roll_first_rolled_winners", winners.size());
        r.put("P2_placed", p2Placed);
        // G: global greedy by priority over roll winners (needs transitive closure; unbounded locality).
        List<Comp> sorted = new ArrayList<>(winners);
        sorted.sort((a, b) -> Long.compareUnsigned(prio(b), prio(a)));
        List<Comp> kept = new ArrayList<>();
        for (Comp c : sorted) {
            boolean ok = true;
            for (Comp k : kept) if (near(c, k, s)) { ok = false; break; }
            if (ok) kept.add(c);
        }
        r.put("G_global_greedy_after_roll_placed", kept.size());
        // Order independence of P1/P2 is by construction (pure of ids); verify with a shuffled copy.
        List<Comp> shuffled = new ArrayList<>(eligible);
        Collections.shuffle(shuffled, new Random(7));
        int p1b = 0;
        for (Comp c : shuffled) {
            boolean top = true;
            for (Comp o : shuffled) if (o != c && near(c, o, s) && prio(o) > prio(c)) { top = false; break; }
            if (top && wins(c)) p1b++;
        }
        r.put("P1_placed_shuffled_input_order", p1b);
        return r;
    }
}
