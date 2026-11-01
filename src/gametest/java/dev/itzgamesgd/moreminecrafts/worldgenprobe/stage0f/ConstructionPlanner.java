package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Corridors.Cave;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Corridors.Route;
import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic construction planner for ONE already-reserved placement. The XZ centre is fixed by the spacing/roll pipeline (RESOLVE Q09/Q11);
 * the approved "up to eight alternative placements" (WG-Q31/Q32) are realised here as eight deterministic floor levels at that fixed centre
 * ({@link #DY}), each evaluated with all four entrance directions. Hard constraints reject; the approved fixed-reference score
 * S = 0.4C + 0.4T + 0.1F + 0.1E (WG-Q34/Q35) only ranks eligible candidates. The 75% roll and the spacing decision are never revisited.
 */
public final class ConstructionPlanner {
    private ConstructionPlanner() { }

    public static final int[] DY = {0, 1, -1, 2, -2, 3, -3, 4};
    public static final int BAND_MIN = -40, BAND_MAX = -10;       // WG-Q03 reference-Y band
    public static final int BODY_R = ArenaDesign.EXT_R + ArenaDesign.REPAIR;
    public static final long ROUTE_WORK = 400_000;

    public static final class Cand {
        public int idx, floorY;
        public boolean ok;
        public String reason = "";
        public Plan plan;
        public String hash = "";
        public int scoreMilli;
        public Map<String, Integer> parts = new LinkedHashMap<>();
        public List<Route> routes = new ArrayList<>();
        public ArenaDesign.Layout layout;
        public long work;
    }

    public static final class Result {
        public Cand chosen;
        public final List<Cand> cands = new ArrayList<>();
        public String status = "";
        public Cave cave;
    }

    /** Structure boxes: {minX,minY,minZ,maxX,maxY,maxZ} of other structures' actual pieces (WG-Q27); unknown occupancy must be passed as boxes. */
    public static Result plan(Terrain t, long id, int cx, int cz, int baseFloorY, List<int[]> boxes) {
        Result res = new Result();
        int r = BODY_R + 2 * Corridors.PATH_CAP + 16;                      // inspected radius
        res.cave = Corridors.findCave(t, cx, cz, cx - r, cx + r, cz - r, cz + r, baseFloorY - 16, baseFloorY + 30);
        for (int i = 0; i < DY.length; i++) {
            Cand c = evaluate(t, res.cave, id, cx, cz, baseFloorY + DY[i], i, boxes, r);
            res.cands.add(c);
        }
        for (Cand c : res.cands) if (c.ok && (res.chosen == null || c.scoreMilli > res.chosen.scoreMilli)) res.chosen = c;   // tie: lower index
        if (res.chosen != null) { res.status = "PLAN_READY"; return res; }
        // failure reason = reason of the first candidate (deterministic) plus a histogram
        Map<String, Integer> h = new LinkedHashMap<>();
        for (Cand c : res.cands) h.merge(c.reason.split("[ (]")[0], 1, Integer::sum);
        res.status = "CONSTRUCTION_FAILED_NO_FIT " + h;
        return res;
    }

    static Cand evaluate(Terrain t, Cave cave, long id, int cx, int cz, int floorY, int idx, List<int[]> boxes, int inspectR) {
        Cand c = new Cand();
        c.idx = idx; c.floorY = floorY;
        if (floorY < BAND_MIN || floorY > BAND_MAX) { c.reason = "Y_BAND"; return c; }
        int yLo = floorY - 2, yHi = floorY + ArenaDesign.TOTAL_H;
        // body footprint: known terrain, no protected blocks, no structure collision
        for (int dx = -BODY_R; dx <= BODY_R; dx++) for (int dz = -BODY_R; dz <= BODY_R; dz++) {
            if (dx * dx + dz * dz > BODY_R * BODY_R) continue;
            boolean inner = dx * dx + dz * dz <= ArenaDesign.EXT_R * ArenaDesign.EXT_R;
            for (int y = yLo; y <= yHi; y++) {
                Kind k = t.kind(cx + dx, y, cz + dz);
                if (k == Kind.UNKNOWN) { c.reason = "TERRAIN_UNKNOWN"; return c; }
                if (inner && k == Kind.PROTECTED) { c.reason = "PROTECTED_BLOCK_IN_BODY"; return c; }
            }
        }
        for (int[] b : boxes) {
            if (b[3] < cx - BODY_R || b[0] > cx + BODY_R || b[5] < cz - BODY_R || b[2] > cz + BODY_R || b[4] < yLo || b[1] > yHi) continue;
            // circle vs box in XZ
            double ddx = Math.max(0, Math.max(b[0] - cx, cx - b[3])), ddz = Math.max(0, Math.max(b[2] - cz, cz - b[5]));
            if (ddx * ddx + ddz * ddz <= (double) BODY_R * BODY_R) { c.reason = "STRUCTURE_COLLISION_BODY"; return c; }
        }
        Plan plan = new Plan();
        c.layout = ArenaDesign.build(plan, t, id, cx, cz, floorY);
        // corridors: guarantee one, allow up to two (WG-Q30); directions rotate deterministically from the placement id
        int first = (int) Long.remainderUnsigned(ArenaDesign.mix(id, 0xD1F), 4);
        Set<Long> blocked = new HashSet<>();
        StringBuilder why = new StringBuilder();
        for (int k = 0; k < 4 && c.routes.size() < 2; k++) {
            int dir = (first + k) & 3;
            Route R = Corridors.route(t, cave, cx, cz, floorY, dir, blocked, ROUTE_WORK);
            c.work += R.work;
            if (!R.ok) { why.append(ArenaDesign.DIRNAME[dir]).append(':').append(R.reason).append(' '); continue; }
            if (collides(R, boxes)) { why.append(ArenaDesign.DIRNAME[dir]).append(":STRUCTURE_COLLISION_CORRIDOR "); continue; }
            c.routes.add(R);
            blocked.addAll(Corridors.reserved(R));
        }
        if (c.routes.isEmpty()) { c.reason = "NO_CAVE_CONNECTION " + why.toString().trim(); return c; }
        for (Route R : c.routes) Corridors.addRoute(plan, t, R, id, cx, cz, floorY);
        String f = safetyPass(plan, t, inspectR, cx, cz);
        if (f != null) { c.reason = f; return c; }
        // no write may touch protected or unknown blocks, and no write may touch another structure
        for (long[] op : plan.ops) {
            int x = Plan.ux(op[0]), y = Plan.uy(op[0]), z = Plan.uz(op[0]);
            Kind k = t.kind(x, y, z);
            if (k == Kind.PROTECTED || k == Kind.UNKNOWN) { c.reason = "WRITES_PROTECTED_OR_UNKNOWN"; return c; }
            for (int[] b : boxes) if (x >= b[0] && x <= b[3] && y >= b[1] && y <= b[4] && z >= b[2] && z <= b[5]) { c.reason = "STRUCTURE_COLLISION_PLAN"; return c; }
        }
        String inv = invariants(plan, t);
        if (inv != null) { c.reason = "INVARIANT_" + inv; return c; }
        plan.finish();
        score(c, plan, t);
        c.plan = plan; c.hash = plan.hash(); c.ok = true;
        return c;
    }

    static boolean collides(Route R, List<int[]> boxes) {
        for (Corridors.Cell k : R.cells) for (int[] b : boxes) {
            int w = Corridors.widthAt(k.s) / 2 + 2, h = Corridors.heightAt(k.s) + 2;
            if (k.x + w >= b[0] && k.x - w <= b[3] && k.z + w >= b[2] && k.z - w <= b[5] && k.yf + h >= b[1] && k.yf - 1 <= b[4]) return true;
        }
        return false;
    }

    static final int[][] N6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** Seal fluids next to the open volume (only when the fluid body is bounded inside the inspected region) and replace gravity blocks above it. */
    static String safetyPass(Plan plan, Terrain t, int inspectR, int cx, int cz) {
        Set<Long> sealed = new HashSet<>(), seenFluid = new HashSet<>();
        List<Long> opens = new ArrayList<>(plan.open);
        java.util.Collections.sort(opens);
        for (long o : opens) {
            int x = Plan.ux(o), y = Plan.uy(o), z = Plan.uz(o);
            for (int[] d : N6) {
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                long np = Plan.pack(nx, ny, nz);
                if (plan.open.contains(np)) continue;
                String fs = plan.finalSpecAt(nx, ny, nz);
                if (fs != null && !fs.equals(Plan.AIR)) continue;       // planned solid already
                Kind k = t.kind(nx, ny, nz);
                if (k == Kind.UNKNOWN) return "UNKNOWN_NEIGHBOUR_OF_OPEN_VOLUME";
                if (k == Kind.WATER || k == Kind.LAVA) {
                    if (!seenFluid.contains(np)) {
                        String b = boundedFluid(t, nx, ny, nz, seenFluid, inspectR, cx, cz);
                        if (b != null) return b;
                    }
                    if (sealed.add(np)) { plan.put(Plan.SEAL, nx, ny, nz, ArenaDesign.COB); plan.solid.add(np); plan.stat("sealed_fluid_cells", 1); }
                } else if (d[1] == 1 && k == Kind.GRAVITY) {
                    if (sealed.add(np)) { plan.put(Plan.SEAL, nx, ny, nz, ArenaDesign.DS); plan.solid.add(np); plan.stat("gravity_support_cells", 1); }
                }
            }
            // fluid cells INSIDE the open volume are removed by the carve; their bodies must be bounded as well
            if (t.fluid(x, y, z) && !seenFluid.contains(o)) {
                String b = boundedFluid(t, x, y, z, seenFluid, inspectR, cx, cz);
                if (b != null) return b;
                plan.stat("fluid_cells_removed_in_volume", 1);
            }
        }
        return null;
    }

    /**
     * WG-Q26: minor water/lava intrusions are sealed with masonry; a MAJOR LAVA LAKE is rejected. Sealing replaces every fluid cell that touches the
     * open volume, so a water body of any size is sealable (the water behind the seal cannot reach the volume). A lava body is only accepted if it
     * is fully contained in the inspected region; one that reaches the edge of what was inspected is an unbounded lake and fails closed.
     */
    static String boundedFluid(Terrain t, int x, int y, int z, Set<Long> seen, int inspectR, int cx, int cz) {
        if (t.kind(x, y, z) == Kind.WATER) return null;
        ArrayDeque<long[]> q = new ArrayDeque<>();
        List<Long> comp = new ArrayList<>();
        Set<Long> local = new HashSet<>();
        q.add(new long[]{x, y, z}); local.add(Plan.pack(x, y, z));
        boolean lava = t.kind(x, y, z) == Kind.LAVA;
        while (!q.isEmpty()) {
            long[] p = q.poll();
            comp.add(Plan.pack((int) p[0], (int) p[1], (int) p[2]));
            if (comp.size() > Corridors.MAX_COMPONENT_NODES) return "UNSEALABLE_FLUID_TOO_LARGE";
            for (int[] d : N6) {
                int nx = (int) p[0] + d[0], ny = (int) p[1] + d[1], nz = (int) p[2] + d[2];
                Kind k = t.kind(nx, ny, nz);
                if (k == Kind.UNKNOWN) return "UNSEALABLE_FLUID_UNBOUNDED(" + (lava ? "lava" : "water") + ")";
                if (Math.abs(nx - cx) > inspectR - 1 || Math.abs(nz - cz) > inspectR - 1) return "UNSEALABLE_FLUID_UNBOUNDED(" + (lava ? "lava" : "water") + ")";
                if ((k == Kind.WATER || k == Kind.LAVA) && local.add(Plan.pack(nx, ny, nz))) q.add(new long[]{nx, ny, nz});
            }
        }
        seen.addAll(local);
        return null;
    }

    /**
     * Flood-safety invariant (also run against the real world after construction): every neighbour of every open cell is open, a planned
     * non-air block, or a natural AIR/ROCK/PROTECTED block - never fluid, never UNKNOWN, and never a gravity block directly above.
     */
    public static String invariants(Plan plan, Terrain t) {
        for (long o : plan.open) {
            int x = Plan.ux(o), y = Plan.uy(o), z = Plan.uz(o);
            for (int[] d : N6) {
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                long np = Plan.pack(nx, ny, nz);
                if (plan.open.contains(np)) continue;
                String fs = plan.finalSpecAt(nx, ny, nz);
                if (fs != null && !fs.equals(Plan.AIR)) continue;
                Kind k = t.kind(nx, ny, nz);
                if (k == Kind.WATER || k == Kind.LAVA) return "FLUID_ADJACENT_TO_OPEN_VOLUME";
                if (k == Kind.UNKNOWN) return "UNKNOWN_ADJACENT_TO_OPEN_VOLUME";
                if (d[1] == 1 && k == Kind.GRAVITY) return "GRAVITY_ABOVE_OPEN_VOLUME";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------ scoring (fixed reference, 0-100)
    static void score(Cand c, Plan plan, Terrain t) {
        Route p = c.routes.get(0);
        int lenScore = 100 * (Corridors.PATH_CAP - p.length) / Corridors.PATH_CAP;
        int trans = 0;
        for (int i = 1; i < p.cells.size(); i++) if ((p.cells.get(i).dy != 0) != (p.cells.get(i - 1).dy != 0)) trans++;
        int stairScore = Math.max(0, 100 - 12 * trans);
        int junction = Math.min(100, 100 * p.junctionOpen / 25);
        int C = (40 * lenScore + 30 * stairScore + 30 * junction) / 100;
        int carvedRock = 0, total = 0;
        for (long[] op : plan.ops) { total++; if (op[2] == Plan.CARVE && t.kind(Plan.ux(op[0]), Plan.uy(op[0]), Plan.uz(op[0])) == Kind.ROCK) carvedRock++; }
        int modScore = total == 0 ? 100 : 100 - 100 * carvedRock / total;
        int T = (60 * modScore + 40 * 100) / 100;               // no natural traversable route is destroyed (hard gate), geometry preserved
        int sealed = plan.stats.getOrDefault("sealed_fluid_cells", 0);
        int F = (50 * Math.max(0, 100 - sealed) + 50 * 100) / 100;
        int dome = Math.max(1, plan.stats.getOrDefault("dome_holes", 0));
        int complete = plan.stats.getOrDefault("dome_holes", 0) == 0 ? 100 : Math.max(0, 100 - plan.stats.get("dome_holes") / 20);
        int E = (60 * junction + 40 * complete) / 100;
        c.parts.put("C", C); c.parts.put("T", T); c.parts.put("F", F); c.parts.put("E", E);
        c.scoreMilli = (400 * C + 400 * T + 100 * F + 100 * E);  // S*1000/ (scaled by 10 weights) = 0.4C+0.4T+0.1F+0.1E in thousandths
    }
}
