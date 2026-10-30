package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** In-memory world for controller tests: players, spawn validity, entity presence, blocks and hook call records. */
final class FakeColosseumWorld implements ColosseumWorldPort {
    final Set<UUID> present = new LinkedHashSet<>();
    boolean atActivation;
    boolean positionsValid = true;
    /** Entities alive in the world, by id (value = allocation index, -1 for the boss, -2 for a shared key). */
    final Map<UUID, Integer> entities = new HashMap<>();
    boolean chunksLoaded = true;
    long time;
    int nextPos;
    final List<WaveRecord> waves = new ArrayList<>();
    final List<int[]> supplies = new ArrayList<>();
    final List<List<Long>> cleanedSupplies = new ArrayList<>();
    final List<UUID> discarded = new ArrayList<>();
    final List<UUID> released = new ArrayList<>();
    final List<ColosseumTrialState.Outcome> outcomes = new ArrayList<>();
    final List<RemovalCause> bossRemovals = new ArrayList<>();
    final List<UUID> crystalEndings = new ArrayList<>();
    final List<UUID> bossVanishedEvents = new ArrayList<>();
    /** Ordered log of the notable hook calls, to assert sequencing. */
    final List<String> order = new ArrayList<>();
    boolean bossAvailable;
    UUID bossEntity;
    int bossStarts, maxAliveSeen;
    /** The shared key item entities, by id, and whether the center chunk accepts new entities. */
    boolean centerLoaded = true;
    int keysSpawned;
    final Map<UUID, KeyInfo> keys = new HashMap<>();
    final Set<UUID> storedKeys = new LinkedHashSet<>();
    /** Blocks by packed position: absent = empty (air). Value is the block-state NBT. */
    final Map<Long, CompoundTag> blocks = new HashMap<>();
    final Set<Long> unloadedBlocks = new LinkedHashSet<>();

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

    record KeyInfo(UUID trial, int round) {}

    /** Simulates a player picking up (or the world otherwise removing) a shared key, via the unload event. */
    void removeKey(ColosseumTrialState state, UUID key) {
        KeyInfo info = keys.get(key);
        entities.remove(key);
        ColosseumController.onKeyUnloaded(state, info.trial(), info.round(), key, 0L, false);
    }

    /** Simulates the key's chunk unloading to disk (the entity leaves the world but is kept in storage). */
    void unloadKeyToStorage(ColosseumTrialState state, UUID key) {
        KeyInfo info = keys.get(key);
        entities.remove(key);
        storedKeys.add(key);
        ColosseumController.onKeyUnloaded(state, info.trial(), info.round(), key, 3L, true);
    }

    @Override public Set<UUID> playersInCombatZone() { return Set.copyOf(present); }
    @Override public boolean playerAtActivation() { return atActivation && !present.isEmpty(); }

    @Override
    public long findSpawnPosition(ColosseumMob mob, Set<Long> reserved, Random random) {
        return positionsValid ? nextPos++ : ColosseumTrialState.NO_POSITION;
    }

    @Override public boolean isValidSpawnPosition(ColosseumMob mob, long pos) { return positionsValid; }

    @Override
    public boolean spawn(UUID trialId, int round, ColosseumTrialState.Allocation allocation, long pos, Binder bind) {
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

    @Override
    public boolean release(UUID entity) {
        released.add(entity);
        return entities.containsKey(entity);
    }

    @Override public void warningEffect(long pos) {}
    @Override public void activationEffect() {}
    @Override public void waveCleared(WaveRecord record) { waves.add(record); }

    @Override
    public List<Long> placeSupplies(UUID trialId, int clearedRound, int count) {
        supplies.add(new int[]{clearedRound, count});
        List<Long> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) positions.add(1000L * clearedRound + i);
        return positions;
    }

    @Override public void cleanupSupplies(UUID trialId, List<Long> positions) { cleanedSupplies.add(positions); }

    @Override
    public boolean startBoss(UUID trialId, List<UUID> lockedParticipants, Binder bind) {
        if (!bossAvailable) return false;
        bossStarts++;
        bossEntity = UUID.randomUUID();
        bind.bind(bossEntity, 9L);
        entities.put(bossEntity, ColosseumOwnership.BOSS_ALLOCATION);
        return true;
    }

    @Override public void bossRemoved(UUID trialId, RemovalCause cause) { bossRemovals.add(cause); order.add("boss:" + cause); }
    @Override public void crystalEnding(UUID trialId) { crystalEndings.add(trialId); order.add("crystal"); }
    @Override public void bossVanished(UUID trialId) { bossVanishedEvents.add(trialId); order.add("vanished"); }

    @Override
    public boolean spawnKey(UUID trialId, int round, Binder bind) {
        if (!centerLoaded) return false;
        UUID key = UUID.randomUUID();
        bind.bind(key, 3L);
        entities.put(key, -2);
        keys.put(key, new KeyInfo(trialId, round));
        keysSpawned++;
        return true;
    }

    @Override
    public RestoreJournal.Attempt restoreBlock(RestoreJournal.Entry entry) {
        if (unloadedBlocks.contains(entry.pos())) return RestoreJournal.Attempt.of(RestoreJournal.RestoreResult.NOT_LOADED);
        CompoundTag current = blocks.get(entry.pos());
        if (entry.prior().equals(current)) return RestoreJournal.Attempt.of(RestoreJournal.RestoreResult.ALREADY_RESTORED);
        boolean empty = entry.left() != null ? entry.left().equals(current) : current == null;
        if (empty) {
            blocks.put(entry.pos(), entry.prior());
            return RestoreJournal.Attempt.of(RestoreJournal.RestoreResult.RESTORED);
        }
        return new RestoreJournal.Attempt(RestoreJournal.RestoreResult.CONFLICT, current);
    }

    @Override public void trialEnded(UUID trialId, ColosseumTrialState.Outcome outcome) { outcomes.add(outcome); order.add("ended:" + outcome); }
    @Override public long gameTime() { return time; }
    @Override public long nextSeed() { return 42L + time; }
}
