package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.colosseum.ArenaAttempt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Drives an {@link ArenaController} like the runtime does, without a world. */
final class Sim {
    static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3), D = new UUID(0, 4);
    ArenaController c = new ArenaController("arena-1");
    long now = 1_000_000L;
    final Set<UUID> inside = new HashSet<>();
    boolean valid = true, loaded = true;
    final List<Action> log = new ArrayList<>();
    SpawnPlanner planner = al -> valid ? new int[] {1, 2, 3} : null;

    List<Action> tick() {
        now += 50;
        List<Action> acts = c.tick(now, new HashSet<>(inside), loaded, planner);
        for (Action a : acts) if (a instanceof SpawnNow s) c.attempt().spawnResult(s.alloc(), new UUID(7, s.alloc().id.getLeastSignificantBits()));
        log.addAll(acts);
        return acts;
    }
    void ticks(int n) { for (int i = 0; i < n; i++) tick(); }
    void kill(Alloc a) { c.onMobKilled(a.entity); }
    /** Kills every spawned mob each tick until the round clears or the attempt ends. */
    void clearRound() {
        int guard = 0;
        int startRound = c.attempt().round();
        while (c.attempt().round() == startRound && c.attempt().phase() == Phase.ROUND && guard++ < 5000) {
            for (Alloc a : c.attempt().allocations()) if (a.status == AllocStatus.SPAWNED) kill(a);
            tick();
        }
    }
    void waitIntermission() { int g = 0; while (c.attempt().phase() == Phase.INTERMISSION && g++ < 2000) tick(); }
    long count(Class<?> k) { return log.stream().filter(k::isInstance).count(); }
}
