package st01proto;

import java.util.*;
import st01proto.Ports.*;

/** Per-instance coordinators; routes defeat reports by instance id. No state is shared between instances. */
public final class InstanceRegistry {
    private final Map<InstanceId, EntranceCoordinator> map = new LinkedHashMap<>();
    private final Config cfg; private final EncounterStore store; private final GolemPort golem; private final CoverPort cover;
    public InstanceRegistry(Config cfg, EncounterStore store, GolemPort golem, CoverPort cover) { this.cfg = cfg; this.store = store; this.golem = golem; this.cover = cover; }
    public EntranceCoordinator of(InstanceId id) { return map.computeIfAbsent(id, k -> new EntranceCoordinator(k, cfg, store, golem, cover)); }
    public void onDefeatReport(DefeatReport r) { of(r.id()).onDefeatSignal(); }   // validity was already decided by the boss side and persisted in the store
    public void tickAll() { for (EntranceCoordinator c : map.values()) { c.tick(); c.pumpSpawn(); c.pollArrival(); } }
    public Collection<EntranceCoordinator> all() { return map.values(); }
}
