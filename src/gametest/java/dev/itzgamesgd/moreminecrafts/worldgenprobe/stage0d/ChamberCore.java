package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0d;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * ST-05 Stage 0-D probe: evidence-based B-prime natural chamber identity (pure Java, no Minecraft dependency).
 *
 * <p>Implements the engineering candidate of docs/st05/ST05_STAGE0_D01_CORE_OVERLAP_DECISION_2026-11-01.md:
 * broad traversable <i>core</i> regions are extracted with a deterministic geometric predicate (morphological opening of the open-voxel
 * mask by a cw x cw x ch cuboid), narrow connectors are excluded, and two observations are the same chamber only with a positive identity
 * witness (a shared core voxel, or an identical complete-component fingerprint). Everything here is experimental probe code: the opening
 * element sizes are EXPERIMENTAL PARAMETERS, not approved gameplay thresholds, and the suitability box 32x32x18 is kept strictly separate
 * from identity.
 */
public final class ChamberCore {
    private ChamberCore() { }

    public static final String ALGO = "st05-d01-bprime-core-opening-v1";
    public static final int OWN = 256, MARGIN = 128;
    /** Approved suitability size (width 32, height 18). Used ONLY for the separate suitability flag, never for identity. */
    public static final int SUIT_W = 32, SUIT_H = 18;
    public static final int SPACING = 256;
    private static final int OFF = 1 << 24, YOFF = 2048;
    private static final long SALT_PRIORITY = 0x5DEECE66DL, SALT_ROLL = 0xB5297A4DL, SALT_SET = 0x9E3779B97F4A7C15L;

    // ------------------------------------------------------------------ primitives

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Packs a world voxel so that numeric order == (x, z, y) lexicographic order. Valid for |x|,|z| &lt; 2^24 and y in [-2048,2047]. */
    public static long key(int x, int z, int y) {
        if (x <= -OFF || x >= OFF || z <= -OFF || z >= OFF || y < -YOFF || y >= YOFF) throw new IllegalArgumentException("voxel out of key range");
        return ((long) (x + OFF) << 37) | ((long) (z + OFF) << 12) | (y + YOFF);
    }
    public static int kx(long k) { return (int) (k >>> 37) - OFF; }
    public static int kz(long k) { return (int) ((k >>> 12) & 0x1FFFFFFL) - OFF; }
    public static int ky(long k) { return (int) (k & 0xFFFL) - YOFF; }
    public static int region(int c) { return Math.floorDiv(c, OWN); }

    public record Params(String name, int cw, int ch) {
        long hash() { return mix(((long) cw << 24) ^ ((long) ch << 8) ^ 0xC0DEL); }
    }

    /** Dense open-voxel snapshot over an XZ window and Y band, world-space origin (ox, oy, oz). */
    public static final class Window {
        public final int ox, oy, oz, wx, wz, h;
        public final boolean[] open;
        public final String snapshotKind;
        public Window(int ox, int oy, int oz, int wx, int wz, int h, boolean[] open, String snapshotKind) {
            this.ox = ox; this.oy = oy; this.oz = oz; this.wx = wx; this.wz = wz; this.h = h; this.open = open; this.snapshotKind = snapshotKind;
        }
        public int idx(int x, int z, int y) { return (x * wz + z) * h + y; }
        public long digest() {
            long d = 1125899906842597L;
            for (int i = 0; i < open.length; i++) d = 31 * d + (open[i] ? 1 : 0) + (i & 7);
            return mix(d ^ ((long) ox << 40) ^ ((long) oz << 16) ^ oy);
        }
    }

    /** One broad core region observed in one window. */
    public static final class Comp {
        public boolean complete;
        public long id;            // chamber id (complete only)
        public long anchor;        // min voxel key of the OBSERVED set
        public long[] keys;        // sorted observed core voxels
        public int size, minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        public long setHash;       // order independent fingerprint of the observed set
        public long suitKey = -1;  // first (x,z,y) 32x32x18 fully-open box origin inside this component, if any
        public int obsRx, obsRz;   // observing region
        public boolean suitable() { return suitKey >= 0; }
        public int centerX() { return kx(suitKey) + SUIT_W / 2; }
        public int centerZ() { return kz(suitKey) + SUIT_W / 2; }
    }

    public static final class ExtractStats {
        public long coreVoxels, windowVoxels, nanos;
        public int complete, partial;
    }

    // ------------------------------------------------------------------ core extraction (identity)

    private static int satIdx(int x, int z, int y, int wz, int h) { return (x * (wz + 1) + z) * (h + 1) + y; }

    private static int[] buildSat(Window w) {
        int wz1 = w.wz + 1, h1 = w.h + 1;
        int[] s = new int[(w.wx + 1) * wz1 * h1];
        for (int x = 0; x < w.wx; x++) for (int z = 0; z < w.wz; z++) for (int y = 0; y < w.h; y++) {
            int v = w.open[w.idx(x, z, y)] ? 1 : 0;
            s[satIdx(x + 1, z + 1, y + 1, w.wz, w.h)] = v
                + s[satIdx(x, z + 1, y + 1, w.wz, w.h)] + s[satIdx(x + 1, z, y + 1, w.wz, w.h)] + s[satIdx(x + 1, z + 1, y, w.wz, w.h)]
                - s[satIdx(x, z, y + 1, w.wz, w.h)] - s[satIdx(x, z + 1, y, w.wz, w.h)] - s[satIdx(x + 1, z, y, w.wz, w.h)]
                + s[satIdx(x, z, y, w.wz, w.h)];
        }
        return s;
    }

    private static boolean boxOpen(int[] s, Window w, int x, int z, int y, int bx, int bz, int bh) {
        int wz = w.wz, h = w.h;
        int sum = s[satIdx(x + bx, z + bz, y + bh, wz, h)]
            - s[satIdx(x, z + bz, y + bh, wz, h)] - s[satIdx(x + bx, z, y + bh, wz, h)] - s[satIdx(x + bx, z + bz, y, wz, h)]
            + s[satIdx(x, z, y + bh, wz, h)] + s[satIdx(x, z + bz, y, wz, h)] + s[satIdx(x + bx, z, y, wz, h)]
            - s[satIdx(x, z, y, wz, h)];
        return sum == bx * bz * bh;
    }

    /**
     * Extracts every broad core component visible in {@code w}. A component is {@code complete} only if every voxel lies at least
     * (cw, ch) away from the window faces: then no opening box that could extend or join it reaches outside the snapshot, so the observed
     * component equals the true component. Otherwise it is {@code partial} (identity unprovable from this view).
     */
    public static List<Comp> extract(Window w, Params p, long seed, int rx, int rz, ExtractStats st) {
        long t0 = System.nanoTime();
        if (p.cw > SUIT_W || p.ch > SUIT_H) throw new IllegalArgumentException("opening element must not exceed the suitability box");
        int[] sat = buildSat(w);
        // union of all fully-open cw x cw x ch boxes via 3D difference array
        int wz1 = w.wz + 1, h1 = w.h + 1;
        int[] diff = new int[(w.wx + 1) * wz1 * h1];
        for (int x = 0; x + p.cw <= w.wx; x++) for (int z = 0; z + p.cw <= w.wz; z++) for (int y = 0; y + p.ch <= w.h; y++) {
            if (!boxOpen(sat, w, x, z, y, p.cw, p.cw, p.ch)) continue;
            int x2 = x + p.cw, z2 = z + p.cw, y2 = y + p.ch;
            diff[satIdx(x, z, y, w.wz, w.h)]++;
            diff[satIdx(x2, z, y, w.wz, w.h)]--; diff[satIdx(x, z2, y, w.wz, w.h)]--; diff[satIdx(x, z, y2, w.wz, w.h)]--;
            diff[satIdx(x2, z2, y, w.wz, w.h)]++; diff[satIdx(x2, z, y2, w.wz, w.h)]++; diff[satIdx(x, z2, y2, w.wz, w.h)]++;
            diff[satIdx(x2, z2, y2, w.wz, w.h)]--;
        }
        boolean[] core = new boolean[w.open.length];
        for (int x = 0; x < w.wx; x++) for (int z = 0; z < w.wz; z++) for (int y = 0; y < w.h; y++) {
            int v = diff[satIdx(x, z, y, w.wz, w.h)]
                + (x > 0 ? diff[satIdx(x - 1, z, y, w.wz, w.h)] : 0) + (z > 0 ? diff[satIdx(x, z - 1, y, w.wz, w.h)] : 0) + (y > 0 ? diff[satIdx(x, z, y - 1, w.wz, w.h)] : 0)
                - (x > 0 && z > 0 ? diff[satIdx(x - 1, z - 1, y, w.wz, w.h)] : 0) - (x > 0 && y > 0 ? diff[satIdx(x - 1, z, y - 1, w.wz, w.h)] : 0)
                - (z > 0 && y > 0 ? diff[satIdx(x, z - 1, y - 1, w.wz, w.h)] : 0)
                + (x > 0 && z > 0 && y > 0 ? diff[satIdx(x - 1, z - 1, y - 1, w.wz, w.h)] : 0);
            // diff[] currently holds raw corner deltas; fold into running prefix in place so later cells see prefix sums
            diff[satIdx(x, z, y, w.wz, w.h)] = v;
            core[w.idx(x, z, y)] = v > 0;
        }
        diff = null;
        // label 6-connected core components in (x,z,y) scan order => first voxel == min key == anchor
        int[] label = new int[w.open.length];
        Arrays.fill(label, -1);
        List<Comp> comps = new ArrayList<>();
        int[] stack = new int[1 << 16];
        long[] buf = new long[1 << 12];
        long coreVox = 0;
        for (int x = 0; x < w.wx; x++) for (int z = 0; z < w.wz; z++) for (int y = 0; y < w.h; y++) {
            int i = w.idx(x, z, y);
            if (!core[i] || label[i] != -1) continue;
            Comp c = new Comp();
            c.obsRx = rx; c.obsRz = rz;
            int lab = comps.size(), sp = 0, n = 0;
            stack[sp++] = i; label[i] = lab;
            while (sp > 0) {
                int cur = stack[--sp];
                int cy = cur % w.h, rest = cur / w.h, cz = rest % w.wz, cx = rest / w.wz;
                int wxw = w.ox + cx, wzw = w.oz + cz, wyw = w.oy + cy;
                if (n == buf.length) buf = Arrays.copyOf(buf, n * 2);
                buf[n++] = key(wxw, wzw, wyw);
                if (wxw < c.minX) c.minX = wxw; if (wxw > c.maxX) c.maxX = wxw;
                if (wzw < c.minZ) c.minZ = wzw; if (wzw > c.maxZ) c.maxZ = wzw;
                if (wyw < c.minY) c.minY = wyw; if (wyw > c.maxY) c.maxY = wyw;
                for (int d = 0; d < 6; d++) {
                    int nx = cx + (d == 0 ? 1 : d == 1 ? -1 : 0), nz = cz + (d == 2 ? 1 : d == 3 ? -1 : 0), ny = cy + (d == 4 ? 1 : d == 5 ? -1 : 0);
                    if (nx < 0 || nx >= w.wx || nz < 0 || nz >= w.wz || ny < 0 || ny >= w.h) continue;
                    int ni = w.idx(nx, nz, ny);
                    if (!core[ni] || label[ni] != -1) continue;
                    label[ni] = lab;
                    if (sp == stack.length) stack = Arrays.copyOf(stack, sp * 2);
                    stack[sp++] = ni;
                }
            }
            c.keys = Arrays.copyOf(buf, n);
            Arrays.sort(c.keys);
            c.size = n; c.anchor = c.keys[0];
            long sh = 0;
            for (long k : c.keys) sh += mix(k ^ SALT_SET);
            c.setHash = sh;
            c.complete = c.minX - w.ox >= p.cw && w.ox + w.wx - 1 - c.maxX >= p.cw
                && c.minZ - w.oz >= p.cw && w.oz + w.wz - 1 - c.maxZ >= p.cw
                && c.minY - w.oy >= p.ch && w.oy + w.h - 1 - c.maxY >= p.ch;
            if (c.complete) c.id = chamberId(seed, p, c.anchor);
            coreVox += n;
            comps.add(c);
        }
        // separate suitability pass: first fully-open 32x32x18 box per component (scan order, window independent for complete comps)
        for (int x = 0; x + SUIT_W <= w.wx; x++) for (int z = 0; z + SUIT_W <= w.wz; z++) for (int y = 0; y + SUIT_H <= w.h; y++) {
            if (!boxOpen(sat, w, x, z, y, SUIT_W, SUIT_W, SUIT_H)) continue;
            int lab = label[w.idx(x, z, y)];
            if (lab < 0) throw new IllegalStateException("open suitability box outside core: opening element larger than suitability box?");
            Comp c = comps.get(lab);
            if (c.suitKey < 0) c.suitKey = key(w.ox + x, w.oz + z, w.oy + y);
        }
        if (st != null) {
            st.coreVoxels = coreVox; st.windowVoxels = w.open.length; st.nanos = System.nanoTime() - t0;
            for (Comp c : comps) if (c.complete) st.complete++; else st.partial++;
        }
        return comps;
    }

    public static long chamberId(long seed, Params p, long anchorKey) {
        return mix(seed ^ mix(anchorKey ^ p.hash()));
    }

    // ------------------------------------------------------------------ legacy baseline (reproduces the defects)

    public record LegacyComp(long id, long anchor, int size, boolean touchBox) { }

    /** Pre-D01 identity: 6-connected air flood fill restricted to a box; id = hash of first voxel in scan order. Used only as the failing baseline. */
    public static List<LegacyComp> legacySegment(Window w, int bx0, int bx1, int bz0, int bz1, long seed, int[] labelOut) {
        if (labelOut != null) Arrays.fill(labelOut, -1);
        int[] lab = labelOut != null ? labelOut : new int[w.open.length];
        if (labelOut == null) Arrays.fill(lab, -1);
        List<LegacyComp> out = new ArrayList<>();
        int[] stack = new int[1 << 16];
        for (int x = bx0; x < bx1; x++) for (int z = bz0; z < bz1; z++) for (int y = 0; y < w.h; y++) {
            int i = w.idx(x, z, y);
            if (!w.open[i] || lab[i] != -1) continue;
            int l = out.size(), sp = 0, size = 0;
            boolean touch = false;
            stack[sp++] = i; lab[i] = l;
            while (sp > 0) {
                int cur = stack[--sp];
                int cy = cur % w.h, rest = cur / w.h, cz = rest % w.wz, cx = rest / w.wz;
                size++;
                if (cx == bx0 || cx == bx1 - 1 || cz == bz0 || cz == bz1 - 1) touch = true;
                for (int d = 0; d < 6; d++) {
                    int nx = cx + (d == 0 ? 1 : d == 1 ? -1 : 0), nz = cz + (d == 2 ? 1 : d == 3 ? -1 : 0), ny = cy + (d == 4 ? 1 : d == 5 ? -1 : 0);
                    if (nx < bx0 || nx >= bx1 || nz < bz0 || nz >= bz1 || ny < 0 || ny >= w.h) continue;
                    int ni = w.idx(nx, nz, ny);
                    if (!w.open[ni] || lab[ni] != -1) continue;
                    lab[ni] = l;
                    if (sp == stack.length) stack = Arrays.copyOf(stack, sp * 2);
                    stack[sp++] = ni;
                }
            }
            long ak = key(w.ox + x, w.oz + z, w.oy + y);
            out.add(new LegacyComp(mix(seed ^ mix(ak)), ak, size, touch));
        }
        return out;
    }

    /** Naive rejected matcher: two observations "match" when their open-air components share any open voxel. */
    public static boolean naiveAirOverlapMatch(Window a, int[] labA, int compA, Window b, int[] labB, int compB) {
        int x0 = Math.max(a.ox, b.ox), x1 = Math.min(a.ox + a.wx, b.ox + b.wx), z0 = Math.max(a.oz, b.oz), z1 = Math.min(a.oz + a.wz, b.oz + b.wz);
        for (int x = x0; x < x1; x++) for (int z = z0; z < z1; z++) for (int y = 0; y < a.h; y++) {
            if (labA[a.idx(x - a.ox, z - a.oz, y)] == compA && labB[b.idx(x - b.ox, z - b.oz, y)] == compB) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ cross-observation resolution

    public static final class Observation {
        public final int rx, rz;
        public final List<Comp> comps;
        public final long snapshotDigest;
        public Observation(int rx, int rz, List<Comp> comps, long snapshotDigest) { this.rx = rx; this.rz = rz; this.comps = comps; this.snapshotDigest = snapshotDigest; }
    }

    public static final class Chamber {
        public long id, anchor, setHash;
        public int size, ownerRx, ownerRz, minX, maxX, minZ, maxZ, minY, maxY;
        public boolean suitable;
        public int cx, cz;
        public final List<String> observers = new ArrayList<>();   // "rx,rz" of complete observations
        public int partialWitnesses;                                // partial observations proven (by shared core voxel) to be this chamber
        public boolean ownerObserved;
        public long[] keys;
        // spacing / roll outcome
        public String status = "UNSET";
        public long priority;
        public boolean rollWin;
    }

    public static final class Group {
        public long provisionalKey;               // min observed voxel across linked partial views (NOT a chamber id)
        public int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        public final List<String> views = new ArrayList<>();
        public boolean knownSuitablePart;
        public long budgetUsed;
        public String status = "DEFERRED_BOUNDARY_PARTIAL";
        public String id() { return Long.toHexString(provisionalKey); }
    }

    public static final class Resolution {
        public final Map<Long, Chamber> chambers = new TreeMap<>(Long::compareUnsigned);
        public final List<Group> groups = new ArrayList<>();
        public final List<String> conflicts = new ArrayList<>();
        public long identityOps;
        public int duplicateObservations;          // complete observations beyond the first of the same chamber
        public int partialMergedIntoChamber;
        public int partialsTotal;
    }

    static boolean bboxOverlap(Comp a, Comp b) {
        return a.minX <= b.maxX && b.minX <= a.maxX && a.minZ <= b.maxZ && b.minZ <= a.maxZ && a.minY <= b.maxY && b.minY <= a.maxY;
    }

    /** True if two sorted key arrays share an element; adds merge steps to ops[0]. */
    static boolean intersects(long[] a, long[] b, long[] ops) {
        int i = 0, j = 0;
        while (i < a.length && j < b.length) {
            ops[0]++;
            if (a[i] == b[j]) return true;
            if (a[i] < b[j]) i++; else j++;
        }
        return false;
    }

    static boolean subset(long[] sub, long[] sup) {
        int j = 0;
        for (long k : sub) {
            while (j < sup.length && sup[j] < k) j++;
            if (j == sup.length || sup[j] != k) return false;
        }
        return true;
    }

    /** Resolves identities. Output is independent of the order of {@code obs} and of each observation's component order. */
    public static Resolution resolve(List<Observation> obsIn, Params p, long seed) {
        List<Observation> obs = new ArrayList<>(obsIn);
        obs.sort(Comparator.<Observation>comparingInt(o -> o.rx).thenComparingInt(o -> o.rz));
        Resolution r = new Resolution();
        long[] ops = new long[1];
        // 1. complete views: same id must carry the same fingerprint, otherwise determinism violation -> conflict (fail closed)
        Map<Long, Chamber> byId = r.chambers;
        List<Comp> partials = new ArrayList<>();
        Map<Comp, String> viewName = new HashMap<>();
        java.util.Set<Long> conflictIds = new java.util.TreeSet<>(Long::compareUnsigned);
        for (Observation o : obs) {
            List<Comp> cs = new ArrayList<>(o.comps);
            cs.sort(Comparator.comparingLong(c -> c.anchor));
            for (Comp c : cs) {
                viewName.put(c, o.rx + "," + o.rz);
                if (!c.complete) { partials.add(c); r.partialsTotal++; continue; }
                Chamber ch = byId.get(c.id);
                if (ch == null) {
                    ch = new Chamber();
                    ch.id = c.id; ch.anchor = c.anchor; ch.setHash = c.setHash; ch.size = c.size; ch.keys = c.keys;
                    ch.minX = c.minX; ch.maxX = c.maxX; ch.minZ = c.minZ; ch.maxZ = c.maxZ; ch.minY = c.minY; ch.maxY = c.maxY;
                    ch.suitable = c.suitable();
                    if (c.suitable()) { ch.cx = c.centerX(); ch.cz = c.centerZ(); }
                    ch.ownerRx = region(kx(c.anchor)); ch.ownerRz = region(kz(c.anchor));
                    byId.put(c.id, ch);
                } else {
                    r.duplicateObservations++;
                    if (ch.setHash != c.setHash || ch.size != c.size || ch.suitable != c.suitable() || (c.suitable() && (ch.cx != c.centerX() || ch.cz != c.centerZ()))) {
                        conflictIds.add(c.id);
                        r.conflicts.add("ID_FINGERPRINT_MISMATCH id=" + Long.toHexString(c.id) + " view=" + viewName.get(c));
                    }
                }
                ch.observers.add(o.rx + "," + o.rz);
                if (o.rx == ch.ownerRx && o.rz == ch.ownerRz) ch.ownerObserved = true;
            }
        }
        // 2. partial views: positive witness = shared core voxel with a complete chamber (and must then be a subset of it)
        List<Comp> leftover = new ArrayList<>();
        for (Comp pc : partials) {
            Chamber hit = null;
            for (Chamber ch : byId.values()) {
                if (pc.minX > ch.maxX || ch.minX > pc.maxX || pc.minZ > ch.maxZ || ch.minZ > pc.maxZ || pc.minY > ch.maxY || ch.minY > pc.maxY) continue;
                if (intersects(pc.keys, ch.keys, ops)) { hit = ch; break; }
            }
            if (hit == null) { leftover.add(pc); continue; }
            if (!subset(pc.keys, hit.keys)) {
                conflictIds.add(hit.id);
                r.conflicts.add("PARTIAL_NOT_SUBSET_OF_COMPLETE id=" + Long.toHexString(hit.id) + " view=" + viewName.get(pc));
                continue;
            }
            hit.partialWitnesses++;
            r.partialMergedIntoChamber++;
        }
        // 3. leftover partials: link by shared core voxels; the group still has no provable anchor -> deferred
        int n = leftover.size();
        int[] uf = new int[n];
        for (int i = 0; i < n; i++) uf[i] = i;
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
            if (!bboxOverlap(leftover.get(i), leftover.get(j))) continue;
            if (intersects(leftover.get(i).keys, leftover.get(j).keys, ops)) uf[find(uf, i)] = find(uf, j);
        }
        Map<Integer, Group> gm = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            Comp c = leftover.get(i);
            Group g = gm.computeIfAbsent(find(uf, i), k -> new Group());
            g.provisionalKey = g.views.isEmpty() ? c.anchor : Math.min(g.provisionalKey, c.anchor);
            g.minX = Math.min(g.minX, c.minX); g.maxX = Math.max(g.maxX, c.maxX); g.minZ = Math.min(g.minZ, c.minZ); g.maxZ = Math.max(g.maxZ, c.maxZ);
            g.views.add(viewName.get(c));
            if (c.suitable()) g.knownSuitablePart = true;
        }
        r.groups.addAll(gm.values());
        r.groups.sort(Comparator.comparingLong(g -> g.provisionalKey));
        for (Group g : r.groups) java.util.Collections.sort(g.views);
        r.identityOps = ops[0];
        // 4. ownership invariants
        for (Chamber ch : byId.values()) {
            if (conflictIds.contains(ch.id)) ch.status = "DEFERRED_IDENTITY_CONFLICT";
            else if (!ch.ownerObserved) ch.status = "OWNER_OUTSIDE_OBSERVED_UNIVERSE";
        }
        return r;
    }

    private static int find(int[] uf, int i) { while (uf[i] != i) { uf[i] = uf[uf[i]]; i = uf[i]; } return i; }

    // ------------------------------------------------------------------ spacing + one 75% roll (dependency propagation)

    public static final class Outcome {
        public int chambers, notSuitable, outsideUniverse, identityConflict, suitableOwned, blockedByUnresolved, rejectedSpacing, accepted, rollWins, rollFailures;
        public int unresolvedGroups;
        public double deferredFraction;   // (groups + blocked) over (all owned + groups)
    }

    public static long priority(long seed, long id) { return mix(seed ^ SALT_PRIORITY ^ id); }
    public static boolean rollWin(long seed, long id) { return (mix(seed ^ SALT_ROLL ^ id) >>> 11) * 0x1.0p-53 < 0.75; }

    static boolean near(double ax, double az, double bx, double bz) { double dx = ax - bx, dz = az - bz; return dx * dx + dz * dz < (double) SPACING * SPACING; }

    /** Distance from a point to a group's XZ bounding box, enlarged by half the suitability box (centres lie inside bboxes). */
    static boolean nearGroup(Chamber c, Group g) {
        double dx = Math.max(0, Math.max(g.minX - c.cx, c.cx - g.maxX)), dz = Math.max(0, Math.max(g.minZ - c.cz, c.cz - g.maxZ));
        return dx * dx + dz * dz < (double) SPACING * SPACING;
    }

    /**
     * Greedy spacing in descending deterministic priority over suitable, provably owned chambers. A chamber whose outcome depends on an
     * unresolved view (a deferred group within spacing distance, or a higher-priority neighbour that is itself blocked) is BLOCKED and
     * is neither rolled nor counted in the roll denominator. Already-decided outcomes never change.
     */
    public static Outcome decide(Resolution r, long seed) {
        Outcome o = new Outcome();
        List<Chamber> cand = new ArrayList<>();
        for (Chamber c : r.chambers.values()) {
            o.chambers++;
            if (c.status.equals("OWNER_OUTSIDE_OBSERVED_UNIVERSE")) { o.outsideUniverse++; continue; }
            if (c.status.equals("DEFERRED_IDENTITY_CONFLICT")) { o.identityConflict++; continue; }
            if (!c.suitable) { c.status = "NOT_SUITABLE"; o.notSuitable++; continue; }
            c.priority = priority(seed, c.id);
            c.rollWin = rollWin(seed, c.id);
            cand.add(c);
        }
        cand.sort((a, b) -> { int k = Long.compareUnsigned(b.priority, a.priority); return k != 0 ? k : Long.compareUnsigned(a.id, b.id); });
        o.suitableOwned = cand.size();
        List<Chamber> done = new ArrayList<>();
        for (Chamber c : cand) {
            boolean rejected = false, blocked = false;
            for (Group g : r.groups) if (nearGroup(c, g)) { blocked = true; break; }
            for (Chamber d : done) {
                if (!near(c.cx, c.cz, d.cx, d.cz)) continue;
                if (d.status.equals("ACCEPTED")) { rejected = true; break; }
                if (d.status.equals("BLOCKED_BY_UNRESOLVED")) blocked = true;
            }
            if (rejected) { c.status = "REJECTED_SPACING"; o.rejectedSpacing++; }
            else if (blocked) { c.status = "BLOCKED_BY_UNRESOLVED"; o.blockedByUnresolved++; }
            else { c.status = "ACCEPTED"; o.accepted++; if (c.rollWin) o.rollWins++; else o.rollFailures++; }
            done.add(c);
        }
        o.unresolvedGroups = r.groups.size();
        int denomAll = o.chambers - o.outsideUniverse + r.groups.size();
        o.deferredFraction = denomAll == 0 ? 0 : (double) (r.groups.size() + o.blockedByUnresolved + o.identityConflict) / denomAll;
        return o;
    }

    // ------------------------------------------------------------------ canonical report digest

    public static String canonical(Resolution r, Outcome o) {
        StringBuilder sb = new StringBuilder();
        for (Chamber c : r.chambers.values()) {
            sb.append("C ").append(Long.toHexString(c.id)).append(' ').append(Long.toHexString(c.anchor)).append(" size=").append(c.size)
                .append(" set=").append(Long.toHexString(c.setHash)).append(" owner=").append(c.ownerRx).append(',').append(c.ownerRz)
                .append(" suit=").append(c.suitable).append('@').append(c.cx).append(',').append(c.cz).append(" st=").append(c.status)
                .append(" obs=").append(String.join("|", c.observers.stream().sorted().toList())).append(" pw=").append(c.partialWitnesses).append('\n');
        }
        for (Group g : r.groups) sb.append("G ").append(g.id()).append(" views=").append(String.join("|", g.views)).append(" st=").append(g.status).append('\n');
        for (String s : r.conflicts.stream().sorted().toList()) sb.append("X ").append(s).append('\n');
        sb.append("O ").append(o.accepted).append(' ').append(o.rollWins).append(' ').append(o.blockedByUnresolved).append(' ').append(o.rejectedSpacing).append('\n');
        return sb.toString();
    }

    public static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
