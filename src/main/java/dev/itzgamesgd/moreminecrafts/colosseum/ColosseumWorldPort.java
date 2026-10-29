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
    boolean spawn(UUID trialId, ColosseumTrialState.Allocation allocation, long pos, Binder bind);

    @FunctionalInterface
    interface Binder {
        void bind(UUID entity, long chunk);
    }

    enum Presence { ALIVE, GONE, NOT_LOADED }

    record Lookup(Presence presence, long chunk) {}

    /** Where a spawned trial mob is: alive (with its current chunk), confirmed gone, or in an unloaded chunk. */
    Lookup locate(UUID entity, long lastKnownChunk);

    /** Removes a trial-owned mob without loot or XP; false if it is not loaded (it is then rejected on load). */
    boolean discard(UUID entity);

    void warningEffect(long pos);

    void activationEffect();

    // Integration hooks for later stages (boss, rewards, supplies, structure).

    /** Fired exactly once per cleared wave, after the record is stored. */
    void waveCleared(ColosseumTrialState.WaveRecord record);

    /** Places R3/R5 supply chests; returns the generated positions to remove at trial end. */
    List<Long> placeSupplies(UUID trialId, int clearedRound, int count);

    void cleanupSupplies(UUID trialId, List<Long> positions);

    /** Starts the R7 boss; false while no boss integration claims this arena (the trial then waits in BOSS). */
    boolean startBoss(UUID trialId, List<UUID> lockedParticipants);

    void cleanupBoss(UUID trialId);

    void trialEnded(UUID trialId, ColosseumTrialState.Outcome outcome);

    long gameTime();

    long nextSeed();
}
