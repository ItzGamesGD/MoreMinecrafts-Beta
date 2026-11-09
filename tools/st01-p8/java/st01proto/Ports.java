package st01proto;

import java.util.*;

/** Replaceable boundaries. The real mod would implement these against Minecraft; the prototype/tests use FakeWorld. */
public final class Ports {
    private Ports() {}
    public interface BossLifecycle { void setListener(java.util.function.Consumer<DefeatReport> listener); }
    public record DefeatReport(InstanceId id, boolean validVictory, Set<UUID> participants, String reason) {}

    /** Persistent encounter record. NOTE: the existing TombEncounterData has a single "defeated" flag; P8 needs death and victory to be distinct (see compatibility audit). */
    public interface EncounterStore {
        void markDeath(InstanceId id); void markVictory(InstanceId id);
        boolean isVictory(InstanceId id); boolean hasDied(InstanceId id);
        default boolean isChestSealed(InstanceId id) { return !isVictory(id); }
    }
    public interface GolemPort {
        record SpawnResult(boolean ok, UUID guide) {}
        SpawnResult spawn(InstanceId id);                 // must never create a second golem for the instance
        int aliveCount(InstanceId id);
        boolean isAlive(InstanceId id, UUID guide);
        boolean atStopPoint(InstanceId id, UUID guide);   // server-authoritative arrival decision
    }
    public interface CoverPort {
        boolean chunkLoaded(InstanceId id);
        boolean isOpen(InstanceId id);
        void openCells(InstanceId id);                    // idempotent world mutation of the 12 cells; never closes
    }
    public interface RewardPort { void grant(InstanceId id); }
}
