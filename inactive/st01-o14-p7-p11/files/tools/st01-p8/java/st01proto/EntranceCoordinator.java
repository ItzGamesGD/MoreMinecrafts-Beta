package st01proto;

import java.util.UUID;
import st01proto.Ports.*;

/**
 * One coordinator per structure instance. Implements the P7 state machine. State is progress of the INSTANCE, never of a player.
 * Truth sources: EncounterStore.isVictory (victory), this record (entrance state), the world blocks (cover). Events are only signals.
 */
public final class EntranceCoordinator {
    public enum State { SEALED, GUARDIAN_DEFEATED, GOLEM_GUIDING, ENTRANCE_OPEN, FALLBACK_OPEN;
        public boolean open() { return this == ENTRANCE_OPEN || this == FALLBACK_OPEN; } }

    private final InstanceId id; private final Config cfg;
    private final EncounterStore store; private final GolemPort golem; private final CoverPort cover;
    private State state = State.SEALED; private long remaining; private boolean coverApplied;
    private UUID guide; private boolean spawnPending; private int attempts; private boolean anomaly;

    public EntranceCoordinator(InstanceId id, Config cfg, EncounterStore store, GolemPort golem, CoverPort cover) {
        if (cfg.timerTicks() <= 0 && !cfg.testOnly()) throw new IllegalStateException("fallback timer is unapproved (O-1): no production default");
        this.id = id; this.cfg = cfg; this.store = store; this.golem = golem; this.cover = cover; this.remaining = cfg.timerTicks();
    }

    // ---- signals (never authoritative)
    public void onDefeatSignal() { reconcile(); }
    public void onChestInteraction() { /* the entrance never depends on chest interaction */ }
    public void requestOpen() { reconcile(); }

    // ---- recovery / idempotent reconcile
    public void reconcile() {
        if (store.isVictory(id) && state == State.SEALED) {
            state = State.GUARDIAN_DEFEATED; remaining = cfg.timerTicks(); attempts = 0;
            spawnPending = golem.aliveCount(id) == 0;
        }
        if (state.open() && !store.isVictory(id)) { anomaly = true; return; }    // saved-data mismatch (O-8): never open without a persisted victory; report, do not repair silently
        if (state.open() && cover.chunkLoaded(id)) { if (!cover.isOpen(id)) cover.openCells(id); coverApplied = true; }   // the OPEN record is authoritative; re-applying is idempotent and repairs reverted blocks
    }

    public void pumpSpawn() {
        if (!spawnPending || !(state == State.GUARDIAN_DEFEATED || (state == State.GOLEM_GUIDING && guide == null))) return;
        if (golem.aliveCount(id) > 0) { spawnPending = false; return; }       // uniqueness guard
        GolemPort.SpawnResult r = golem.spawn(id); attempts++;
        if (r.ok()) { state = State.GOLEM_GUIDING; guide = r.guide(); spawnPending = false; }
        else spawnPending = attempts < cfg.maxSpawnAttempts();               // stays GUARDIAN_DEFEATED; the timer keeps running
    }

    public void pollArrival() {
        if (state == State.GOLEM_GUIDING && guide != null && golem.isAlive(id, guide) && golem.atStopPoint(id, guide)) state = State.ENTRANCE_OPEN;
        reconcile();
    }

    public void tick() {
        if ((state == State.GUARDIAN_DEFEATED || state == State.GOLEM_GUIDING) && remaining >= 0) {
            remaining--;
            if (remaining <= 0 && store.isVictory(id)) state = State.FALLBACK_OPEN;
        }
        reconcile();
    }

    // ---- persistence (4 fields; the guide entity is not assumed to survive a restart)
    public String snapshot() { return id.dimension() + "|" + id.anchor() + "|" + state + "|" + remaining + "|" + (coverApplied ? 1 : 0); }
    public static EntranceCoordinator restore(String snap, Config cfg, EncounterStore store, GolemPort golem, CoverPort cover) {
        String[] p = snap.split("\\|"); EntranceCoordinator c = new EntranceCoordinator(new InstanceId(p[0], Long.parseLong(p[1])), cfg, store, golem, cover);
        c.state = State.valueOf(p[2]); c.remaining = Long.parseLong(p[3]); c.coverApplied = p[4].equals("1");
        c.spawnPending = false; c.guide = null;
        if (cfg.respawnAfterRestart() && (c.state == State.GUARDIAN_DEFEATED || c.state == State.GOLEM_GUIDING) && golem.aliveCount(c.id) == 0) c.spawnPending = true;
        c.reconcile(); return c;
    }

    public State state() { return state; } public long remaining() { return remaining; } public boolean coverApplied() { return coverApplied; }
    public boolean anomaly() { return anomaly; } public boolean spawnPending() { return spawnPending; } public UUID guide() { return guide; } public InstanceId id() { return id; }
}
