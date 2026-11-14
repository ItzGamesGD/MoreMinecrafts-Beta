package st01proto;

import java.util.*;
import st01proto.Ports.*;

/** In-memory stand-in for the server world (store + golem + cover + reward ports). Persistent: victories, deaths, cover blocks. Volatile: golems, chunk state. */
public final class FakeWorld implements EncounterStore, GolemPort, CoverPort, RewardPort {
    public static final class Golem { public UUID id = UUID.randomUUID(); public boolean alive = true, stalled, atStop; }
    public final Set<InstanceId> victories = new LinkedHashSet<>(), deaths = new LinkedHashSet<>(), coverOpen = new LinkedHashSet<>();
    public final Map<InstanceId, Golem> golems = new LinkedHashMap<>();
    public final Map<InstanceId, Boolean> chunkLoaded = new HashMap<>();
    public final Map<InstanceId, Integer> spawnCount = new HashMap<>(), openMutations = new HashMap<>(), rewards = new HashMap<>();
    public boolean nextSpawnOk = true;

    public void markDeath(InstanceId id) { deaths.add(id); }
    public void markVictory(InstanceId id) { victories.add(id); }
    public boolean isVictory(InstanceId id) { return victories.contains(id); }
    public boolean hasDied(InstanceId id) { return deaths.contains(id); }

    public GolemPort.SpawnResult spawn(InstanceId id) {
        if (!nextSpawnOk) return new GolemPort.SpawnResult(false, null);
        Golem g = golems.get(id);
        if (g != null && g.alive) return new GolemPort.SpawnResult(true, g.id);      // a real port never creates a second golem
        g = new Golem(); golems.put(id, g); spawnCount.merge(id, 1, Integer::sum);
        return new GolemPort.SpawnResult(true, g.id);
    }
    public int aliveCount(InstanceId id) { Golem g = golems.get(id); return g != null && g.alive ? 1 : 0; }
    public boolean isAlive(InstanceId id, UUID guide) { Golem g = golems.get(id); return g != null && g.alive && g.id.equals(guide); }
    public boolean atStopPoint(InstanceId id, UUID guide) { Golem g = golems.get(id); return g != null && g.alive && !g.stalled && g.atStop; }
    public Golem golem(InstanceId id) { return golems.get(id); }

    public boolean chunkLoaded(InstanceId id) { return chunkLoaded.getOrDefault(id, true); }
    public void openCells(InstanceId id) { if (coverOpen.add(id)) openMutations.merge(id, 1, Integer::sum); }   // never closes
    public boolean isOpen(InstanceId id) { return coverOpen.contains(id); }
    public boolean isCoverOpen(InstanceId id) { return coverOpen.contains(id); }

    public void grant(InstanceId id) { rewards.merge(id, 1, Integer::sum); }

    /** Server restart: persistent data stays; entities are not assumed to survive; all chunks start loaded. */
    public void restart() { for (Golem g : golems.values()) g.alive = false; chunkLoaded.clear(); }
}
