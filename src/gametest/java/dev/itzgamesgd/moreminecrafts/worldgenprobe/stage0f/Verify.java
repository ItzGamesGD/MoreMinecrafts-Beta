package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.Terrain.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Post-construction verification against the approved requirements, run on the FINAL state of the world (as a {@link Terrain} view), independent
 * of the planner's own bookkeeping. Used by the pure self-test (simulated world) and by the live Minecraft 26.3 run (real blocks).
 */
public final class Verify {
    private Verify() { }

    static boolean standing(Terrain t, int x, int yf, int z) {
        return t.kind(x, yf, z) == Kind.ROCK && t.kind(x, yf + 1, z) == Kind.AIR && t.kind(x, yf + 2, z) == Kind.AIR;
    }

    /** Player walk distance (steps, 1-block step up/down, 2-high clearance) from a standing position to any position in {@code targets}; -1 if none within max. */
    public static int walk(Terrain t, int sx, int sy, int sz, Set<Long> targets, int max) {
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        dist.put(Plan.pack(sx, sy, sz), 0); q.add(new int[]{sx, sy, sz});
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int dp = dist.get(Plan.pack(p[0], p[1], p[2]));
            if (targets.contains(Plan.pack(p[0], p[1], p[2]))) return dp;
            if (dp >= max) continue;
            for (int[] dd : d) for (int dy = -1; dy <= 1; dy++) {
                int nx = p[0] + dd[0], nz = p[2] + dd[1], ny = p[1] + dy;
                if (!standing(t, nx, ny, nz)) continue;
                int hi = Math.max(p[1], ny);
                if (dy != 0 && (t.kind(p[0], hi + 2, p[2]) != Kind.AIR || t.kind(nx, hi + 2, nz) != Kind.AIR)) continue;
                long k = Plan.pack(nx, ny, nz);
                if (dist.containsKey(k)) continue;
                dist.put(k, dp + 1); q.add(new int[]{nx, ny, nz});
                break;
            }
        }
        return -1;
    }

    /** Returns the list of failed checks (empty = all approved requirements hold in the final state). */
    public static List<String> check(Terrain after, Plan plan, List<Corridors.Route> routes, Corridors.Cave cave, int cx, int cz, int floorY,
                                     ArenaDesign.Variant variant, int vaultCount, Map<String, Object> measured) {
        List<String> f = new ArrayList<>();
        // 1. flood / support safety on the open volume
        int fluidAdj = 0, gravAdj = 0, unk = 0;
        for (long o : plan.open) {
            int x = Plan.ux(o), y = Plan.uy(o), z = Plan.uz(o);
            if (after.kind(x, y, z) != Kind.AIR) continue;
            for (int[] d : ConstructionPlanner.N6) {
                Kind k = after.kind(x + d[0], y + d[1], z + d[2]);
                if (k == Kind.WATER || k == Kind.LAVA) fluidAdj++;
                else if (k == Kind.UNKNOWN) unk++;
                else if (d[1] == 1 && k == Kind.GRAVITY) gravAdj++;
            }
        }
        if (fluidAdj > 0) f.add("FLUID_ADJACENT_TO_OPEN_VOLUME x" + fluidAdj);
        if (gravAdj > 0) f.add("GRAVITY_ABOVE_OPEN_VOLUME x" + gravAdj);
        if (unk > 0) f.add("UNKNOWN_ADJACENT x" + unk);
        measured.put("fluid_adjacent_open_cells", fluidAdj);
        int openMissing = 0;
        for (long o : plan.open) if (after.kind(Plan.ux(o), Plan.uy(o), Plan.uz(o)) != Kind.AIR) openMissing++;
        if (openMissing > 0) f.add("OPEN_VOLUME_NOT_AIR x" + openMissing);
        // 2. combat floor: flat solid floor radius 29, free spawn annulus 12-24 (3 layers), nothing intersecting
        int badFloor = 0, badSpawn = 0;
        for (int dx = -ArenaDesign.IN_R; dx <= ArenaDesign.IN_R; dx++) for (int dz = -ArenaDesign.IN_R; dz <= ArenaDesign.IN_R; dz++) {
            double r = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (r > ArenaDesign.IN_R) continue;
            if (after.kind(cx + dx, floorY - 1, cz + dz) != Kind.ROCK) badFloor++;
            if (r >= 12 && r <= 24) for (int o = 0; o < 3; o++) if (after.kind(cx + dx, floorY + o, cz + dz) != Kind.AIR) badSpawn++;
        }
        if (badFloor > 0) f.add("COMBAT_FLOOR_NOT_SOLID x" + badFloor);
        if (badSpawn > 0) f.add("SPAWN_ANNULUS_BLOCKED x" + badSpawn);
        // 3. exterior radius 34 / wall height 12 / total height 23 (from the plan's own solid extremes, confirmed in the final state)
        int maxRingR = 0, maxSolidOff = -99;
        for (long[] op : plan.ops) {
            if (op[2] == Plan.SEAL) continue;     // seals / gravity supports are safety work around the design, not part of its dimensions
            long c = op[0];
            String s = plan.palette.get((int) op[1]);
            if (s.equals(Plan.AIR) || s.contains("torch") || s.contains("redstone")) continue;
            double r = Math.sqrt((double) (Plan.ux(c) - cx) * (Plan.ux(c) - cx) + (double) (Plan.uz(c) - cz) * (Plan.uz(c) - cz));
            if (r <= ArenaDesign.EXT_R + 1e-9) {
                maxRingR = Math.max(maxRingR, (int) r);
                maxSolidOff = Math.max(maxSolidOff, Plan.uy(c) - floorY);
            }
        }
        measured.put("exterior_radius", maxRingR); measured.put("max_solid_offset", maxSolidOff);
        if (maxRingR != ArenaDesign.EXT_R) f.add("EXTERIOR_RADIUS " + maxRingR);
        if (variant != ArenaDesign.Variant.RUINED_NATURAL_CEILING && maxSolidOff != ArenaDesign.TOTAL_H - 1) f.add("TOTAL_HEIGHT_OFFSET " + maxSolidOff);
        if (variant == ArenaDesign.Variant.RUINED_NATURAL_CEILING && maxSolidOff != ArenaDesign.WALL_H - 1) f.add("WALL_HEIGHT_OFFSET " + maxSolidOff);
        int diag = (int) Math.round(32 / Math.sqrt(2));
        for (int o = 0; o < ArenaDesign.WALL_H; o++) {
            // diagonal wall cells (not an entrance) must be masonry for the full 12 blocks
            if (variant == ArenaDesign.Variant.INTACT && after.kind(cx + diag, floorY + o, cz + diag) != Kind.ROCK) { f.add("WALL_GAP_AT_OFFSET " + o); break; }
        }
        // 4. entrances N/E/S/W: 7 wide x 8 high clear through the wall
        int blockedEntr = 0;
        for (int d = 0; d < 4; d++) {
            int ax = ArenaDesign.DIR[d][0], az = ArenaDesign.DIR[d][1], lx = -az, lz = ax;
            for (int a = ArenaDesign.IN_R - 3; a <= ArenaDesign.EXT_R + 1; a++) for (int l = -3; l <= 3; l++) for (int o = 0; o < ArenaDesign.ENT_H; o++)
                if (after.kind(cx + ax * a + lx * l, floorY + o, cz + az * a + lz * l) != Kind.AIR) blockedEntr++;
        }
        if (blockedEntr > 0) f.add("ENTRANCE_NOT_7x8_CLEAR x" + blockedEntr);
        // 5. vault count (identity is checked by the caller on real block ids)
        measured.put("vault_blocks_expected", vaultCount);
        // 6. corridors: 5x5 minimum clearance everywhere, walkable, within 32 steps of the entrance exterior, ending in a qualified natural cave
        int corr = 0;
        for (Corridors.Route R : routes) {
            for (Corridors.Cell c : R.cells) {
                int lx = -ArenaDesign.DIR[c.h][1], lz = ArenaDesign.DIR[c.h][0];
                for (int l = -2; l <= 2; l++) for (int v = 1; v <= 5; v++) if (after.kind(c.x + lx * l, c.yf + v, c.z + lz * l) != Kind.AIR) corr++;
                for (int l = -2; l <= 2; l++) if (after.kind(c.x + lx * l, c.yf, c.z + lz * l) != Kind.ROCK) corr++;
            }
            Corridors.Cell s0 = R.cells.get(0), sN = R.cells.get(R.cells.size() - 1);
            int dist = walk(after, s0.x, s0.yf, s0.z, java.util.Set.of(Plan.pack(sN.x, sN.yf, sN.z)), 40);
            if (dist < 0 || dist > Corridors.PATH_CAP) f.add("ROUTE_NOT_WALKABLE_WITHIN_CAP dir=" + R.dir + " dist=" + dist);
            if (R.length > Corridors.PATH_CAP) f.add("ROUTE_LONGER_THAN_CAP");
            if (!cave.standing.contains(Plan.pack(sN.x, sN.yf, sN.z))) f.add("ROUTE_END_NOT_IN_QUALIFIED_CAVE");
            // full connectivity from inside the arena through the entrance to the cave
            int inside = walk(after, cx + ArenaDesign.DIR[R.dir][0] * 28, floorY - 1, cz + ArenaDesign.DIR[R.dir][1] * 28, java.util.Set.of(Plan.pack(sN.x, sN.yf, sN.z)), 80);
            if (inside < 0) f.add("ARENA_TO_CAVE_NOT_CONNECTED dir=" + R.dir);
            measured.put("walk_arena_to_cave_dir" + R.dir, inside);
        }
        if (corr > 0) f.add("CORRIDOR_CLEARANCE_OR_FLOOR_BAD x" + corr);
        measured.put("corridors", routes.size());
        return f;
    }
}
