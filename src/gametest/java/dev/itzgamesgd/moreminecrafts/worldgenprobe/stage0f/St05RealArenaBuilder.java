package dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f;

import java.nio.file.Path;
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
}
