package dev.itzgamesgd.moreminecrafts.st01;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Pure (Minecraft-free) per-instance entrance state machine implementing the P7 design as verified by the P8 harness.
 * Signals (the defeat event, arrival, open requests) are never authoritative: truth is {@link Ports#victory()} (the persisted
 * valid-victory record), this record, and the world blocks. Every operation is idempotent. There is no transition back to CLOSED.
 */
public final class EntranceCore {
    /** Instance-bound boundary to the world. */
    public interface Ports {
        boolean victory();
        boolean spawnAreaLoaded();
        boolean coverChunkLoaded();
        boolean coverOpen();
        void openCover();
        int golemCount();
        boolean golemAlive(UUID golem);
        @Nullable UUID spawnGolem();
        boolean golemAtStop(UUID golem);
    }

    /** The persisted part of the record. */
    public record Saved(EntranceState state, long remaining, boolean coverApplied) {}

    private final EntranceConfig cfg;
    private final Ports ports;
    private EntranceState state = EntranceState.SEALED;
    private long remaining;
    private boolean coverApplied;
    @Nullable private UUID guide;
    private boolean spawnPending;
    private int attempts;
    private boolean anomaly;

    public EntranceCore(EntranceConfig cfg, Ports ports, Saved saved) {
        if (cfg.fallbackTicks() <= 0) throw new IllegalStateException("fallbackTicks must be > 0 (approved O-1 is 600)");
        if (cfg.arrivalRadius() <= 0.0D) throw new IllegalStateException("arrivalRadius must be > 0 (approved O-4 is 1.5)");
        this.cfg = cfg;
        this.ports = ports;
        this.state = saved.state();
        this.remaining = saved.state() == EntranceState.SEALED ? cfg.fallbackTicks() : saved.remaining();
        this.coverApplied = saved.coverApplied();
        if (cfg.respawnAfterRestart() && (state == EntranceState.GUARDIAN_DEFEATED || state == EntranceState.GOLEM_GUIDING) && ports.golemCount() == 0) spawnPending = true;
    }

    public Saved saved() { return new Saved(state, remaining, coverApplied); }
    public EntranceState state() { return state; }
    public long remaining() { return remaining; }
    @Nullable public UUID guide() { return guide; }
    public boolean anomaly() { return anomaly; }
    public boolean spawnPending() { return spawnPending; }

    /** Derive/repair the record from the persisted victory and apply the cover. Safe to call at any time, any number of times. */
    public void reconcile() {
        if (ports.victory() && state == EntranceState.SEALED) {
            state = EntranceState.GUARDIAN_DEFEATED;
            remaining = cfg.fallbackTicks();      // the fallback timer starts at the valid victory, whether or not a golem can be created
            attempts = 0;
            spawnPending = ports.golemCount() == 0;
        }
        if (state.isOpen() && !ports.victory()) { anomaly = true; return; }   // inconsistent save: never open without a persisted victory (O-8)
        if (state.isOpen() && ports.coverChunkLoaded()) {
            if (!ports.coverOpen()) ports.openCover();           // idempotent; repairs reverted blocks; never closes
            coverApplied = true;
        }
    }

    public void pumpSpawn() {
        if (!spawnPending || !(state == EntranceState.GUARDIAN_DEFEATED || (state == EntranceState.GOLEM_GUIDING && guide == null))) return;
        if (ports.golemCount() > 0) { spawnPending = false; return; }       // at most one guide golem per instance
        if (!ports.spawnAreaLoaded()) return;                                // do not consume an attempt while the area is unloaded
        UUID g = ports.spawnGolem(); attempts++;
        if (g != null) { state = EntranceState.GOLEM_GUIDING; guide = g; spawnPending = false; }
        else spawnPending = attempts < cfg.maxSpawnAttempts();               // a failed spawn leaves the timer running
    }

    public void pollArrival() {
        if (state == EntranceState.GOLEM_GUIDING && guide != null && ports.golemAlive(guide) && ports.golemAtStop(guide)) state = EntranceState.ENTRANCE_OPEN;
        reconcile();
    }

    public void tick() {
        if ((state == EntranceState.GUARDIAN_DEFEATED || state == EntranceState.GOLEM_GUIDING) && remaining >= 0) {
            remaining--;
            if (remaining <= 0 && ports.victory()) state = EntranceState.FALLBACK_OPEN;
        }
        reconcile();
    }

    /** Full per-tick step used by the runtime. */
    public void step() { tick(); pumpSpawn(); pollArrival(); }
}
