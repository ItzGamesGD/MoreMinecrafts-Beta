package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** In-memory world for controller tests: players, spawn validity, entity presence and hook call records. */
final class FakeColosseumWorld implements ColosseumWorldPort {
    final Set<UUID> present = new LinkedHashSet<>();
    boolean atActivation;
    boolean positionsValid = true;
    /** Entities alive in the world, by id (value = allocation index). */
    final Map<UUID, Integer> entities = new HashMap<>();
    boolean chunksLoaded = true;
    long time;
    int nextPos;
    final List<ColosseumTrialState.WaveRecord> waves = new ArrayList<>();
    final List<int[]> supplies = new ArrayList<>();
    final List<List<Long>> cleanedSupplies = new ArrayList<>();
    final List<UUID> discarded = new ArrayList<>();
    final List<ColosseumTrialState.Outcome> outcomes = new ArrayList<>();
    boolean bossAvailable;
    int bossStarts, bossCleanups, maxAliveSeen;

    UUID join() {
        UUID player = UUID.randomUUID();
        present.add(player);
        return player;
    }

    void tick(ColosseumTrialState state, int ticks) {
        for (int i = 0; i < ticks; i++) {
            time++;
            ColosseumController.tick(state, this);
            maxAliveSeen = Math.max(maxAliveSeen, state.count(ColosseumTrialState.AllocationStatus.ALIVE));
        }
    }

    /** Kills up to {@code count} alive trial mobs through the death entry point. */
    int kill(ColosseumTrialState state, int count) {
        int killed = 0;
        for (ColosseumTrialState.Allocation allocation : state.allocations()) {
            if (killed == count) break;
            if (allocation.status() != ColosseumTrialState.AllocationStatus.ALIVE) continue;
            UUID entity = allocation.entity();
            entities.remove(entity);
            if (ColosseumController.onTrialMobDeath(state, state.trialId(), allocation.index(), entity, !present.isEmpty()))
                killed++;
        }
        return killed;
    }

    /** Kills everything repeatedly until the round clears (or {@code maxTicks} pass). */
    void clearRound(ColosseumTrialState state, int maxTicks) {
        int round = state.round();
        for (int t = 0; t < maxTicks && state.phase() == ColosseumTrialState.Phase.ROUND && state.round() == round; t++) {
            kill(state, Integer.MAX_VALUE);
            tick(state, 1);
        }
    }

    @Override public Set<UUID> playersInCombatZone() { return Set.copyOf(present); }
    @Override public boolean playerAtActivation() { return atActivation && !present.isEmpty(); }

    @Override
    public long findSpawnPosition(ColosseumMob mob, Set<Long> reserved, Random random) {
        return positionsValid ? nextPos++ : ColosseumTrialState.NO_POSITION;
    }

    @Override public boolean isValidSpawnPosition(ColosseumMob mob, long pos) { return positionsValid; }

    @Override
    public boolean spawn(UUID trialId, ColosseumTrialState.Allocation allocation, long pos, Binder bind) {
        UUID entity = UUID.randomUUID();
        bind.bind(entity, 7L);
        entities.put(entity, allocation.index());
        return true;
    }

    @Override
    public Lookup locate(UUID entity, long lastKnownChunk) {
        if (entities.containsKey(entity)) return new Lookup(chunksLoaded ? Presence.ALIVE : Presence.NOT_LOADED, lastKnownChunk);
        return new Lookup(chunksLoaded ? Presence.GONE : Presence.NOT_LOADED, lastKnownChunk);
    }

    @Override
    public boolean discard(UUID entity) {
        discarded.add(entity);
        return entities.remove(entity) != null;
    }

    @Override public void warningEffect(long pos) {}
    @Override public void activationEffect() {}
    @Override public void waveCleared(ColosseumTrialState.WaveRecord record) { waves.add(record); }

    @Override
    public List<Long> placeSupplies(UUID trialId, int clearedRound, int count) {
        supplies.add(new int[]{clearedRound, count});
        List<Long> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) positions.add(1000L * clearedRound + i);
        return positions;
    }

    @Override public void cleanupSupplies(UUID trialId, List<Long> positions) { cleanedSupplies.add(positions); }

    @Override
    public boolean startBoss(UUID trialId, List<UUID> lockedParticipants) {
        if (!bossAvailable) return false;
        bossStarts++;
        return true;
    }

    @Override public void cleanupBoss(UUID trialId) { bossCleanups++; }
    @Override public void trialEnded(UUID trialId, ColosseumTrialState.Outcome outcome) { outcomes.add(outcome); }
    @Override public long gameTime() { return time; }
    @Override public long nextSeed() { return 42L + time; }
}
