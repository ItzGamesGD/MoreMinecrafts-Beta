package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the controller needs from the world, so the state machine stays pure and JUnit-testable.
 * {@link ColosseumServerPort} implements it for a real {@code ServerLevel}; tests use an in-memory fake.
 */
public interface ColosseumWorldPort {
    /** Alive, non-spectator, non-creative players physically inside the arena combat zone. */
    Set<UUID> playersInCombatZone();

    /** Whether such a player stands at the arena center (round 1 starts on center approach). */
    boolean playerAtActivation();

    /**
     * A currently valid spawn position for {@code mob} in the 12-24 annulus, preferring positions not in
     * {@code reserved}; {@link ColosseumTrialState#NO_POSITION} when none exists anywhere.
     */
    long findSpawnPosition(ColosseumMob mob, Set<Long> reserved, Random random);

    boolean isValidSpawnPosition(ColosseumMob mob, long pos);

    /**
     * Creates the trial-owned mob at {@code pos}. Implementations must call {@code bind} with the new entity's
     * id and chunk before adding it to the world (so ownership validation already accepts it) and return false
     * if the entity could not be created or added.
     */
    boolean spawn(UUID trialId, int round, ColosseumTrialState.Allocation allocation, long pos, Binder bind);

    @FunctionalInterface
    interface Binder {
        void bind(UUID entity, long chunk);
    }

    enum Presence { ALIVE, GONE, NOT_LOADED }

    record Lookup(Presence presence, long chunk) {}

    /** Where an entity is: alive (with its current chunk), confirmed gone, or in an unloaded chunk. */
    Lookup locate(UUID entity, long lastKnownChunk);

    /** Removes an entity without dying (no loot, XP, death event or key); false if it is not loaded. */
    boolean discard(UUID entity);

    /** Detaches the ownership tag of a loaded entity so it becomes a plain external mob (Q116); false if not loaded. */
    boolean release(UUID entity);

    void warningEffect(long pos);

    void activationEffect();

    // Integration hooks for later stages (boss, rewards, supplies, structure).

    /** Fired exactly once per cleared wave, after the record is stored. */
    void waveCleared(WaveRecord record);

    /** Places R3/R5 supply chests; returns the generated positions to remove at trial end. */
    List<Long> placeSupplies(UUID trialId, int clearedRound, int count);

    void cleanupSupplies(UUID trialId, List<Long> positions);

    /**
     * Starts the R7 boss. The implementation must call {@code bind} with the boss entity's id and chunk before
     * adding it to the world. False while no boss integration claims this arena (the trial then waits in BOSS).
     */
    boolean startBoss(UUID trialId, List<UUID> lockedParticipants, Binder bind);

    enum RemovalCause {
        /** The boss was killed and the trial succeeded. */
        DEFEATED,
        /** Failure or reset removed the boss (Q102). */
        REMOVED,
        /** Time-out vanishing (Q117): not a kill. */
        VANISHED
    }

    /** Called after the boss left the world (or its trial ended), so the handler can clean up outside the entity. */
    void bossRemoved(UUID trialId, RemovalCause cause);

    /** Q117: the server-authoritative ending presentation at the central crystal. Notification only. */
    void crystalEnding(UUID trialId);

    /** A boss vanish work item finished. */
    void bossVanished(UUID trialId);

    /**
     * Creates the one shared normal Trial Key as an item entity at the exact arena center, tagged for
     * {@code (trial, round)}. Like {@link #spawn}, the implementation must call {@code bind} before adding the entity
     * so the key's own load is accepted. False when it cannot be created right now (center chunk not loaded); the
     * wave then stays PENDING and is retried.
     */
    boolean spawnKey(UUID trialId, int round, Binder bind);

    /**
     * Q118-A restore of one journal entry: an empty position (the state the boss left) is filled with the prior
     * state; a position already holding the prior state needs nothing; any other block is a conflict and is never
     * overwritten; a position in an unloaded chunk is skipped for now.
     */
    RestoreJournal.Attempt restoreBlock(RestoreJournal.Entry entry);

    void trialEnded(UUID trialId, ColosseumTrialState.Outcome outcome);

    long gameTime();

    long nextSeed();
}
