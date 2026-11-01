package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Cave-connection corridors (WG-Q08, Q23-Q25, Q30): path of at most 32 steps from the entrance exterior to a genuinely traversable natural
 * cave, minimum 5-wide x 5-high clearance (tapering from the 7x8 entrance), stairs plus level stretches, masonry lining that gradually yields
 * to original rock. Pure Java over {@link Terrain}.
 */
public final class Corridors {
    private Corridors() { }

    public static final int PATH_CAP = 32, TAPER_END = 7, MAX_COMPONENT_NODES = 200_000;
    /** Natural cave qualification: a traversable component must offer a path of at least this many steps (WG-Q09 path length). */
    public static final int CAVE_MIN_PATH = 64;

    public static int widthAt(int s) { return s <= 4 ? 7 : 5; }
    public static int heightAt(int s) { return s <= 2 ? 8 : s <= 4 ? 7 : s <= 6 ? 6 : 5; }

    /** Natural standing positions (floor block y) that belong to a traversable component of path length >= 64, excluding the arena body. */
    public static final class Cave {
        public final Set<Long> standing = new HashSet<>();
        public int components, qualified;
        int bx0, bz0, bw, bd;
        short[] dist;   // Manhattan XZ distance to the nearest qualified standing position (admissible lower bound on path steps)
        public int h(int x, int z) {
            int ix = x - bx0, iz = z - bz0;
            if (ix < 0 || iz < 0 || ix >= bw || iz >= bd || dist == null) return 10_000;
            return dist[ix * bd + iz];
        }
        void buildDist(int x0, int x1, int z0, int z1) {
            bx0 = x0; bz0 = z0; bw = x1 - x0 + 1; bd = z1 - z0 + 1;
            dist = new short[bw * bd];
            java.util.Arrays.fill(dist, (short) 10_000);
            ArrayDeque<int[]> q = new ArrayDeque<>();
            for (long p : standing) { int ix = Plan.ux(p) - x0, iz = Plan.uz(p) - z0; if (ix >= 0 && iz >= 0 && ix < bw && iz < bd && dist[ix * bd + iz] != 0) { dist[ix * bd + iz] = 0; q.add(new int[]{ix, iz}); } }
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!q.isEmpty()) {
                int[] c = q.poll(); int dc = dist[c[0] * bd + c[1]];
                for (int[] dd : d) { int nx = c[0] + dd[0], nz = c[1] + dd[1]; if (nx < 0 || nz < 0 || nx >= bw || nz >= bd) continue; if (dist[nx * bd + nz] > dc + 1) { dist[nx * bd + nz] = (short) (dc + 1); q.add(new int[]{nx, nz}); } }
            }
        }
    }

    static boolean stand(Terrain t, int x, int yf, int z) {
        return t.kind(x, yf, z) == Kind.ROCK && t.kind(x, yf + 1, z) == Kind.AIR && t.kind(x, yf + 2, z) == Kind.AIR;
    }

    public static Cave findCave(Terrain t, int cx, int cz, int x0, int x1, int z0, int z1, int y0, int y1) {
        Cave c = new Cave();
        Map<Long, Integer> id = new HashMap<>();
        List<long[]> pos = new ArrayList<>();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            double dx = x - cx, dz = z - cz;
            if (dx * dx + dz * dz <= (ArenaDesign.EXT_R + 0.5) * (ArenaDesign.EXT_R + 0.5)) continue;
            for (int y = y0; y <= y1; y++) if (stand(t, x, y, z)) { id.put(Plan.pack(x, y, z), pos.size()); pos.add(new long[]{x, y, z}); }
        }
        int n = pos.size();
        int[] comp = new int[n];
        java.util.Arrays.fill(comp, -1);
        for (int s = 0; s < n; s++) {
            if (comp[s] != -1) continue;
            List<Integer> members = new ArrayList<>();
            ArrayDeque<Integer> q = new ArrayDeque<>();
            comp[s] = c.components; q.add(s);
            while (!q.isEmpty()) { int u = q.poll(); members.add(u); for (int v : nbrs(t, id, pos, u)) if (comp[v] == -1) { comp[v] = c.components; q.add(v); } }
            // double-sweep path length estimate (deterministic, no randomness)
            int far = bfsFar(t, id, pos, s, null);
            int[] depth = new int[1];
            bfsFar(t, id, pos, far, depth);
            c.components++;
            if (depth[0] >= CAVE_MIN_PATH) { c.qualified++; for (int u : members) c.standing.add(Plan.pack((int) pos.get(u)[0], (int) pos.get(u)[1], (int) pos.get(u)[2])); }
        }
        c.buildDist(x0, x1, z0, z1);
        return c;
    }

    static List<Integer> nbrs(Terrain t, Map<Long, Integer> id, List<long[]> pos, int u) {
        long[] p = pos.get(u);
        int x = (int) p[0], y = (int) p[1], z = (int) p[2];
        List<Integer> out = new ArrayList<>(4);
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dd : d) for (int dy = -1; dy <= 1; dy++) {
            Integer v = id.get(Plan.pack(x + dd[0], y + dy, z + dd[1]));
            if (v == null) continue;
            // headroom for a step up/down: the cell column above the higher floor must be open at feet+head+1
            int hi = Math.max(y, y + dy);
            if (dy != 0 && (t.kind(x, hi + 2, z) != Kind.AIR || t.kind(x + dd[0], hi + 2, z + dd[1]) != Kind.AIR)) continue;
            out.add(v);
            break;
        }
        return out;
    }

    static int bfsFar(Terrain t, Map<Long, Integer> id, List<long[]> pos, int src, int[] depthOut) {
        Map<Integer, Integer> dist = new HashMap<>();
        ArrayDeque<Integer> q = new ArrayDeque<>();
        dist.put(src, 0); q.add(src);
        int last = src, best = 0;
        while (!q.isEmpty()) {
            int u = q.poll(); int du = dist.get(u);
            if (du > best) { best = du; last = u; }
            for (int v : allNbrs(t, id, pos, u)) if (!dist.containsKey(v)) { dist.put(v, du + 1); q.add(v); }
        }
        if (depthOut != null) depthOut[0] = best;
        return last;
    }

    static List<Integer> allNbrs(Terrain t, Map<Long, Integer> id, List<long[]> pos, int u) {
        long[] p = pos.get(u);
        int x = (int) p[0], y = (int) p[1], z = (int) p[2];
        List<Integer> out = new ArrayList<>(8);
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dd : d) for (int dy = -1; dy <= 1; dy++) {
            Integer v = id.get(Plan.pack(x + dd[0], y + dy, z + dd[1]));
            if (v == null) continue;
            int hi = Math.max(y, y + dy);
            if (dy != 0 && (t.kind(x, hi + 2, z) != Kind.AIR || t.kind(x + dd[0], hi + 2, z + dd[1]) != Kind.AIR)) continue;
            out.add(v);
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------------ routing
    public static final class Cell { public int x, z, yf, h, s, dy; }
    public static final class Route {
        public boolean ok;
        public String reason = "";
        public int dir, length, stairCells, turns, boxFluidCells;
        public long work;
        public List<Cell> cells = new ArrayList<>();
        public int junctionOpen;
    }

    static final class Node {
        int x, z, y, h, ld, s, cost; long seq; Node prev; int dy;
    }

    /**
     * Dijkstra over (x, z, floor y, heading, last vertical move). Deterministic: fixed neighbour order, (cost, sequence) tie-break.
     * {@code blocked} holds cells reserved by an earlier corridor of the same arena. {@code work} is the deterministic budget (WG-Q43).
     */
    public static Route route(Terrain t, Cave cave, int cx, int cz, int floorY, int dir, Set<Long> blocked, long workBudget) {
        Route R = new Route();
        R.dir = dir;
        int ax = ArenaDesign.DIR[dir][0], az = ArenaDesign.DIR[dir][1];
        // A*: f = cost + 100 * (Manhattan XZ distance to the nearest qualified cave position); admissible and consistent (every step costs >= 100)
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> { int fa = a.cost + 100 * cave.h(a.x, a.z), fb = b.cost + 100 * cave.h(b.x, b.z); return fa != fb ? Integer.compare(fa, fb) : Long.compare(a.seq, b.seq); });
        Map<Long, Integer> best = new HashMap<>();
        long seq = 0;
        Node start = new Node();
        start.x = cx + ax * 35; start.z = cz + az * 35; start.y = floorY - 1; start.h = dir; start.ld = 0; start.s = 1; start.cost = 0; start.seq = seq++;
        if (!boxOk(t, start.x, start.z, start.y, start.h, 1, cx, cz, blocked, R)) { R.reason = "START_BLOCKED"; return R; }
        if (start.s + cave.h(start.x, start.z) > PATH_CAP + 1) { R.reason = "NO_CAVE_WITHIN_PATH_CAP"; return R; }
        pq.add(start);
        int[] turnOrder = {0, 1, 3};   // straight, right, left (heading delta)
        while (!pq.isEmpty()) {
            Node n = pq.poll();
            if (++R.work > workBudget) { R.reason = "WORK_BUDGET_EXHAUSTED"; return R; }
            long key = ((((long) (n.x - cx + 128) * 256 + (n.z - cz + 128)) * 256 + (n.y - floorY + 128)) * 4 + n.h) * 3 + n.ld;
            Integer seen = best.get(key);
            if (seen != null && seen <= n.cost) continue;
            best.put(key, n.cost);
            if (n.s >= TAPER_END && cave.standing.contains(Plan.pack(n.x, n.y, n.z))) {
                for (Node c = n; c != null; c = c.prev) { Cell k = new Cell(); k.x = c.x; k.z = c.z; k.yf = c.y; k.h = c.h; k.s = c.s; k.dy = c.dy; R.cells.add(k); }
                Collections.reverse(R.cells);
                R.ok = true; R.length = n.s;
                for (int i = 0; i < R.cells.size(); i++) { Cell k = R.cells.get(i); if (k.dy != 0) R.stairCells++; if (i > 0 && k.h != R.cells.get(i - 1).h) R.turns++; }
                int open = 0;
                for (int ddx = -2; ddx <= 2; ddx++) for (int ddz = -2; ddz <= 2; ddz++) for (int dy = -1; dy <= 1; dy++) if (cave.standing.contains(Plan.pack(n.x + ddx, n.y + dy, n.z + ddz))) { open++; break; }
                R.junctionOpen = open;
                return R;
            }
            if (n.s >= PATH_CAP) continue;
            for (int dyo = 0; dyo < 3; dyo++) {
                int dy = dyo == 0 ? 0 : dyo == 1 ? 1 : -1;
                if (dy == 1 && n.ld == 2) continue;
                if (dy == -1 && n.ld == 1) continue;
                for (int to : turnOrder) {
                    boolean turn = to != 0;
                    if (turn && (dy != 0 || n.ld != 0 || n.s < 8)) continue;       // turns only on level ground after the taper
                    int h = (n.h + to) & 3;
                    int nx = n.x + ArenaDesign.DIR[h][0], nz = n.z + ArenaDesign.DIR[h][1], ny = n.y + dy;
                    int ns = n.s + 1;
                    if (ns + cave.h(nx, nz) > PATH_CAP) continue;           // cannot reach any cave within the cap from here
                    if (!boxOk(t, nx, nz, ny, h, ns, cx, cz, blocked, R)) continue;
                    Node m = new Node();
                    m.x = nx; m.z = nz; m.y = ny; m.h = h; m.s = ns; m.dy = dy; m.prev = n; m.seq = seq++;
                    m.ld = dy == 0 ? 0 : dy > 0 ? 1 : 2;
                    m.cost = n.cost + 100 + (dy != 0 ? 3 : 0) + (turn ? 5 : 0) + R.boxFluidCells * 2;
                    R.boxFluidCells = 0;
                    pq.add(m);
                }
            }
        }
        R.reason = "NO_CAVE_WITHIN_PATH_CAP";
        return R;
    }

    /** All cells of the cross-section must be placeable: never PROTECTED/UNKNOWN, outside the arena body and other corridors. */
    static boolean boxOk(Terrain t, int x, int z, int yf, int h, int s, int cx, int cz, Set<Long> blocked, Route R) {
        int w = widthAt(s), hh = heightAt(s);
        int lx = -ArenaDesign.DIR[h][1], lz = ArenaDesign.DIR[h][0];
        int half = w / 2, fl = 0;
        for (int l = -half; l <= half; l++) {
            int px = x + lx * l, pz = z + lz * l;
            double dx = px - cx, dz = pz - cz;
            if (dx * dx + dz * dz < 35.0 * 35.0) return false;
            for (int v = 0; v <= hh; v++) {
                Kind k = t.kind(px, yf + v, pz);
                if (k == Kind.PROTECTED || k == Kind.UNKNOWN) return false;
                if (k == Kind.WATER || k == Kind.LAVA) fl++;
                if (blocked != null && blocked.contains(Plan.pack(px, yf + v, pz))) return false;
            }
        }
        // the ceiling row and side wall rows must at least be editable (or already solid natural rock)
        for (int l = -half - 1; l <= half + 1; l++) {
            Kind k = t.kind(x + lx * l, yf + hh + 1, z + lz * l);
            if (k == Kind.UNKNOWN) return false;
        }
        R.boxFluidCells = fl;
        return true;
    }

    // ------------------------------------------------------------------------------------------------------ build into the plan
    public static void addRoute(Plan plan, Terrain t, Route R, long id, int cx, int cz, int floorY) {
        for (int i = 0; i < R.cells.size(); i++) {
            Cell c = R.cells.get(i);
            int w = widthAt(c.s), hh = heightAt(c.s);
            int lx = -ArenaDesign.DIR[c.h][1], lz = ArenaDesign.DIR[c.h][0], half = w / 2;
            String hname = ArenaDesign.DIRNAME[c.h];
            for (int l = -half - 1; l <= half + 1; l++) {
                boolean side = Math.abs(l) == half + 1;
                int px = c.x + lx * l, pz = c.z + lz * l;
                for (int v = 0; v <= hh + 1; v++) {
                    int py = c.yf + v;
                    boolean inBox = !side && v >= 1 && v <= hh;
                    if (inBox) { plan.put(Plan.CARVE, px, py, pz, Plan.AIR); plan.open.add(Plan.pack(px, py, pz)); continue; }
                    // floor row (v==0, inside width) or lining (sides and ceiling)
                    Kind k = t.kind(px, py, pz);
                    if (k == Kind.PROTECTED || k == Kind.UNKNOWN) continue;
                    boolean floor = !side && v == 0;
                    if (floor) {
                        String spec;
                        if (c.dy != 0) {
                            String face = c.dy > 0 ? ArenaDesign.opposite(hname) : hname;
                            spec = ArenaDesign.stair(c.s <= 16 ? "minecraft:deepslate_brick_stairs" : "minecraft:cobbled_deepslate_stairs", face, "bottom");
                        } else if (c.s <= 16) spec = ((c.s + l) & 1) == 0 ? ArenaDesign.PD : ArenaDesign.DT;
                        else if (k == Kind.ROCK) continue;            // original rock floor stays
                        else spec = ArenaDesign.COB;
                        plan.put(Plan.SOLID, px, py, pz, spec); plan.solid.add(Plan.pack(px, py, pz)); plan.open.remove(Plan.pack(px, py, pz));
                        continue;
                    }
                    boolean mason = c.s <= 8 || (c.s <= 16 && Long.remainderUnsigned(ArenaDesign.cellHash(id, px, py, pz), 9) < 17 - c.s);
                    if (!mason) continue;
                    String m = c.s <= 8 ? (((v + c.s) & 1) == 0 ? ArenaDesign.DB : ArenaDesign.PD) : ArenaDesign.COB;
                    plan.put(Plan.SOLID, px, py, pz, m); plan.solid.add(Plan.pack(px, py, pz));
                }
            }
        }
    }

    /** Cells a route reserves (box + lining + one extra margin), used so a second route cannot overlap the first. */
    public static Set<Long> reserved(Route R) {
        Set<Long> s = new HashSet<>();
        for (Cell c : R.cells) {
            int w = widthAt(c.s), hh = heightAt(c.s), half = w / 2 + 2;
            int lx = -ArenaDesign.DIR[c.h][1], lz = ArenaDesign.DIR[c.h][0];
            for (int l = -half; l <= half; l++) for (int v = -1; v <= hh + 2; v++) s.add(Plan.pack(c.x + lx * l, c.yf + v, c.z + lz * l));
        }
        return s;
    }
}
