package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

/**
 * Stage 1-D: builds one REAL ST-05 arena with the unchanged Stage 0-F production pipeline ({@link St05Stage0FLive#handle}: terrain snapshot, planner,
 * journaled application, verification, arena registration) on a running server, so a real client can play it. No plan is replayed or hand-made.
 */
public final class St05RealArenaBuilder {
    private St05RealArenaBuilder() {}

    /** @return {@code final_state | verify failures | plan mismatches}; the arena is registered with the colosseum runtime only when VERIFIED. */
    public static String build(MinecraftServer server, Path stateDir, String id, int cx, int cz, int floorY) throws Exception {
        var level = server.overworld();
        var world = new St05Stage0FLive.LevelWorld(server, level);
        var journal = new Construction.Journal(stateDir);
        Map<String, Object> d = St05Stage0FLive.handle(server, level, world, journal, new St05Stage0FLive.Res(id, cx, cz, floorY), -1, true, null);
        return d.get("final_state") + " | " + d.get("verify_failures") + " | mismatches=" + d.get("plan_mismatches") + " | ms=" + d.get("ms");
    }

    /** Number of plan cells that no longer hold the verified final block (0 = the structure is intact). */
    public static int mismatches(MinecraftServer server, Path stateDir, String id) throws Exception {
        var world = new St05Stage0FLive.LevelWorld(server, server.overworld());
        return Construction.mismatches(new Construction.Journal(stateDir).readPlan(id), world, null);
    }

    /** Same check, but returns the mismatching cells ("x,y,z expected/actual") so a failure can be located. */
    public static java.util.List<String> mismatchSamples(MinecraftServer server, Path stateDir, String id) throws Exception {
        var world = new St05Stage0FLive.LevelWorld(server, server.overworld());
        java.util.List<String> samples = new java.util.ArrayList<>();
        Construction.mismatches(new Construction.Journal(stateDir).readPlan(id), world, samples);
        return samples;
    }

    /** Floor Y the arena was verified at (may differ from the reservation). */
    public static int verifiedFloorY(Path stateDir, String id) throws Exception {
        return Construction.load(new Construction.Journal(stateDir), id).floorY;
    }

    /**
     * Diagnostic only: every tick, finds plan cells (within maxR of the centre) that were correct on the previous tick and are not now.
     * Each change is reported with the cell, the expected and actual state, the nearest non-player entities and the nearest player.
     */
    public static final class Tracer {
        private final Plan plan; private final St05Stage0FLive.LevelWorld w;
        private final long[] cells; private final String[] fin; private final boolean[] ok;
        public Tracer(MinecraftServer server, Path stateDir, String id, int cx, int cz, int maxR) throws Exception {
            plan = new Construction.Journal(stateDir).readPlan(id);
            w = new St05Stage0FLive.LevelWorld(server, server.overworld());
            List<Long> keep = new java.util.ArrayList<>();
            for (long c : new java.util.TreeSet<>(plan.cells())) {
                long dx = Plan.ux(c) - cx, dz = Plan.uz(c) - cz;
                if (dx * dx + dz * dz <= (long) maxR * maxR) keep.add(c);
            }
            cells = keep.stream().mapToLong(Long::longValue).toArray();
            fin = new String[cells.length]; ok = new boolean[cells.length];
            for (int i = 0; i < cells.length; i++) {
                long c = cells[i];
                fin[i] = w.canon(plan.finalSpecAt(Plan.ux(c), Plan.uy(c), Plan.uz(c)));
                ok[i] = fin[i].equals(w.get(Plan.ux(c), Plan.uy(c), Plan.uz(c)));
            }
        }
        public int size() { return cells.length; }
        public int badNow() { int n = 0; for (boolean b : ok) if (!b) n++; return n; }
        public List<String> tick(ServerLevel level, long gameTick) {
            List<String> out = new java.util.ArrayList<>();
            for (int i = 0; i < cells.length; i++) {
                long c = cells[i]; int x = Plan.ux(c), y = Plan.uy(c), z = Plan.uz(c);
                String now = w.get(x, y, z);
                boolean nowOk = fin[i].equals(now);
                if (ok[i] && !nowOk) {
                    StringBuilder e = new StringBuilder("tick=" + gameTick + " cell=" + x + "," + y + "," + z + " expected=" + fin[i] + " now=" + now);
                    var near = level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(x - 6, y - 6, z - 6, x + 7, y + 7, z + 7), en -> !(en instanceof net.minecraft.server.level.ServerPlayer));
                    e.append(" nearbyEntities=");
                    for (var en : near) e.append(en.getType().toShortString()).append(' ');
                    var pl = level.getNearestPlayer(x + 0.5, y + 0.5, z + 0.5, 40, false);
                    if (pl != null) e.append(" nearestPlayer=").append(String.format("%.1f,%.1f,%.1f", pl.getX(), pl.getY(), pl.getZ())).append(" dist=").append(String.format("%.1f", Math.sqrt(pl.distanceToSqr(x + 0.5, y + 0.5, z + 0.5))));
                    out.add(e.toString());
                }
                ok[i] = nowOk;
            }
            return out;
        }
    }
}
