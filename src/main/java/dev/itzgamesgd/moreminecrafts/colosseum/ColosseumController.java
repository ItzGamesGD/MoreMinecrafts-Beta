package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Allocation;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Outcome;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Phase;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authoritative ST-05 trial state machine: READY -> ROUND -> INTERMISSION -> ... -> BOSS -> SUCCESS/FAILURE
 * -> COOLDOWN -> READY. The all-absent pause and the Q110 spawn-blocked clock are tracked independently of the
 * phase. One call to {@link #tick} is one server tick of active trial time.
 * <p>
 * Allocation accounting keeps pending, warning, alive and valid-kill counts distinct. A mob that leaves without an
 * eligible kill (discarded, despawned, changed dimension, confirmed missing after a restart, or killed while no
 * player was in the combat zone) returns its allocation to PENDING: Q34 preserves allocations "without duplication
 * or loss" and a round never completes on a disappearance.
 */
public final class ColosseumController {
    private ColosseumController() {}

    public static void tick(ColosseumTrialState state, ColosseumWorldPort port) {
        switch (state.phase) {
            case READY -> {
                if (port.playerAtActivation()) startTrial(state, port);
            }
            case COOLDOWN -> {
                if (--state.cooldownRemaining <= 0) {
                    state.cooldownRemaining = 0;
                    state.phase = Phase.READY;
                }
            }
            case ROUND, INTERMISSION, BOSS -> activeTick(state, port);
            case SUCCESS, FAILURE -> endTrial(state, port, state.lastOutcome);
        }
    }

    private static void startTrial(ColosseumTrialState state, ColosseumWorldPort port) {
        state.trialId = UUID.randomUUID();
        state.trialSerial++;
        state.seed = port.nextSeed();
        state.lastOutcome = Outcome.NONE;
        state.clearedWaves.clear();
        state.supplyPositions.clear();
        state.everParticipants.clear();
        state.bossStarted = false;
        state.absenceTicks = 0;
        port.activationEffect();
        startRound(state, port, 1, port.playersInCombatZone());
    }

    private static void activeTick(ColosseumTrialState state, ColosseumWorldPort port) {
        Set<UUID> present = port.playersInCombatZone();
        reconcileAlive(state, port);
        boolean combat = state.phase == Phase.ROUND || state.phase == Phase.BOSS;
        if (combat && !state.lockedParticipants.isEmpty() && state.deadThisRound.containsAll(state.lockedParticipants)) {
            fail(state, port, Outcome.FAILED_PARTY_DEFEAT);
            return;
        }
        if (!participantPresent(state, present)) {
            if (++state.absenceTicks >= ColosseumRules.ABSENCE_GRACE_TICKS) fail(state, port, Outcome.FAILED_ABSENCE_RESET);
            return; // paused: no progression, no new spawn batches; existing mobs keep their own AI
        }
        state.absenceTicks = 0;
        switch (state.phase) {
            case ROUND -> {
                tickWarnings(state, port);
                tickBatchCycle(state, port);
                tickBlockageClock(state, port);
                if (state.phase != Phase.ROUND) return;
                if (!state.allocations.isEmpty() && state.count(AllocationStatus.KILLED) == state.allocations.size())
                    completeRound(state, port, present);
            }
            case INTERMISSION -> {
                if (--state.intermissionRemaining > 0) return;
                if (state.round >= ColosseumRules.LAST_BUDGET_ROUND) enterBoss(state, present);
                else startRound(state, port, state.round + 1, present);
            }
            case BOSS -> {
                if (!state.bossStarted) state.bossStarted = port.startBoss(state.trialId, state.lockedParticipants());
            }
            default -> {}
        }
    }

    /** During combat only participants locked for this round who are not dead count; between rounds any present player does. */
    private static boolean participantPresent(ColosseumTrialState state, Set<UUID> present) {
        if (state.phase == Phase.INTERMISSION) return !present.isEmpty();
        for (UUID participant : state.lockedParticipants)
            if (present.contains(participant) && !state.deadThisRound.contains(participant)) return true;
        return false;
    }

    private static void startRound(ColosseumTrialState state, ColosseumWorldPort port, int round, Set<UUID> present) {
        state.round = round;
        lockParticipants(state, present);
        state.allocations.clear();
        ColosseumComposer.Composition composition;
        try {
            composition = ColosseumComposer.compose(round, Math.max(1, state.lockedParticipants.size()),
                    new Random(state.seed * 31L + round));
        } catch (ColosseumComposer.InvalidRosterException invalid) {
            fail(state, port, Outcome.FAILED_INVALID_ROSTER);
            return;
        }
        for (ColosseumComposer.Pick pick : composition.picks())
            state.allocations.add(new Allocation(state.allocations.size(), pick.mob(), pick.focal()));
        state.blockedTicks = 0;
        state.spawnBlocked = false;
        state.batchTimer = 0;
        state.phase = Phase.ROUND;
    }

    private static void enterBoss(ColosseumTrialState state, Set<UUID> present) {
        state.round = ColosseumRules.BOSS_ROUND;
        lockParticipants(state, present);
        state.allocations.clear();
        state.bossStarted = false;
        state.phase = Phase.BOSS;
    }

    /** Locks the eligible participant set and count for this round's difficulty; later entries wait for the next round. */
    private static void lockParticipants(ColosseumTrialState state, Set<UUID> present) {
        state.lockedParticipants.clear();
        List<UUID> sorted = new ArrayList<>(present);
        sorted.sort(null);
        state.lockedParticipants.addAll(sorted);
        state.everParticipants.addAll(sorted);
        state.deadThisRound.clear();
    }

    private static void reconcileAlive(ColosseumTrialState state, ColosseumWorldPort port) {
        for (Allocation allocation : state.allocations) {
            if (allocation.status != AllocationStatus.ALIVE || allocation.entity == null) continue;
            ColosseumWorldPort.Lookup lookup = port.locate(allocation.entity, allocation.chunk);
            switch (lookup.presence()) {
                case ALIVE -> allocation.chunk = lookup.chunk();
                case GONE -> requeue(allocation);
                case NOT_LOADED -> {} // possibly stored in an unloaded chunk: never spawn a duplicate for it
            }
        }
    }

    private static void tickWarnings(ColosseumTrialState state, ColosseumWorldPort port) {
        for (Allocation allocation : state.allocations) {
            if (allocation.status != AllocationStatus.WARNING) continue;
            port.warningEffect(allocation.warningPos);
            if (--allocation.warningTicks > 0) continue;
            long pos = allocation.warningPos;
            if (port.isValidSpawnPosition(allocation.mob(), pos) && port.spawn(state.trialId, allocation, pos,
                    (entity, chunk) -> {
                        allocation.status = AllocationStatus.ALIVE;
                        allocation.entity = entity;
                        allocation.chunk = chunk;
                        allocation.warningPos = ColosseumTrialState.NO_POSITION;
                        allocation.warningTicks = 0;
                    })) {
                state.blockedTicks = 0; // the spawn proved a valid position exists
                state.spawnBlocked = false;
            } else {
                allocation.toPending(); // a failed position check never consumes the allocation
            }
        }
    }

    /**
     * The normal batch-spawn cycle: once alive plus warned mobs are at or below 6, warn pending allocations up to the
     * 12 cap. A due batch that finds no valid position for any pending mob marks spawning blocked; one that finds a
     * position clears the mark and resets the Q110 clock.
     */
    private static void tickBatchCycle(ColosseumTrialState state, ColosseumWorldPort port) {
        if (--state.batchTimer > 0) return;
        state.batchTimer = ColosseumRules.BATCH_CYCLE_TICKS;
        int pending = state.count(AllocationStatus.PENDING);
        if (pending == 0) return;
        int active = state.count(AllocationStatus.ALIVE) + state.count(AllocationStatus.WARNING);
        if (active > ColosseumRules.REPLENISH_AT_OR_BELOW) return;
        int slots = ColosseumRules.MAX_ALIVE - active;
        Set<Long> reserved = new HashSet<>();
        for (Allocation allocation : state.allocations)
            if (allocation.status == AllocationStatus.WARNING) reserved.add(allocation.warningPos);
        Random random = new Random(state.seed ^ (state.round * 7919L) ^ port.gameTime());
        Set<ColosseumMob> unplaceable = EnumSet.noneOf(ColosseumMob.class);
        boolean found = false;
        for (Allocation allocation : state.allocations) {
            if (slots == 0) break;
            if (allocation.status != AllocationStatus.PENDING || unplaceable.contains(allocation.mob())) continue;
            long pos = port.findSpawnPosition(allocation.mob(), reserved, random);
            if (pos == ColosseumTrialState.NO_POSITION) {
                unplaceable.add(allocation.mob());
                continue;
            }
            found = true;
            reserved.add(pos);
            allocation.status = AllocationStatus.WARNING;
            allocation.warningPos = pos;
            allocation.warningTicks = ColosseumRules.SPAWN_WARNING_TICKS;
            slots--;
        }
        state.spawnBlocked = !found;
        if (found) state.blockedTicks = 0;
    }

    /**
     * Q110 clock, one tick per tick of active (unpaused) time while blocked and a batch is due. It holds without
     * resetting while the 12 cap or the at-or-below-6 threshold legitimately holds spawning back, and resets when a
     * valid position appears.
     */
    private static void tickBlockageClock(ColosseumTrialState state, ColosseumWorldPort port) {
        if (state.phase != Phase.ROUND || !state.spawnBlocked || state.count(AllocationStatus.PENDING) == 0) return;
        int active = state.count(AllocationStatus.ALIVE) + state.count(AllocationStatus.WARNING);
        if (active > ColosseumRules.REPLENISH_AT_OR_BELOW) return;
        if (++state.blockedTicks < ColosseumRules.SPAWN_BLOCKED_FAIL_TICKS) return;
        // Decide the failure on a fresh check, never on a stale cycle result.
        state.batchTimer = 0;
        tickBatchCycle(state, port);
        if (state.spawnBlocked) fail(state, port, Outcome.FAILED_SPAWN_BLOCKED);
    }

    private static void completeRound(ColosseumTrialState state, ColosseumWorldPort port, Set<UUID> present) {
        int round = state.round;
        if (state.waveCleared(round)) return; // never complete or pay a wave twice
        List<UUID> presentSorted = new ArrayList<>(present);
        presentSorted.sort(null);
        ColosseumTrialState.WaveRecord record = new ColosseumTrialState.WaveRecord(state.trialId, round,
                state.lockedParticipants(), List.copyOf(presentSorted), port.gameTime());
        state.clearedWaves.add(record);
        state.allocations.clear();
        state.deadThisRound.clear(); // dead participants may rejoin between this clear and the next round start
        state.blockedTicks = 0;
        state.spawnBlocked = false;
        state.phase = Phase.INTERMISSION;
        state.intermissionRemaining = ColosseumRules.intermissionTicksAfter(round);
        port.waveCleared(record);
        if (ColosseumRules.isSupplyRound(round))
            state.supplyPositions.addAll(port.placeSupplies(state.trialId, round, record.lockedParticipants().size()));
    }

    private static void fail(ColosseumTrialState state, ColosseumWorldPort port, Outcome outcome) {
        state.phase = Phase.FAILURE;
        state.lastOutcome = outcome;
        endTrial(state, port, outcome);
    }

    private static void endTrial(ColosseumTrialState state, ColosseumWorldPort port, Outcome outcome) {
        UUID trialId = state.trialId;
        state.lastOutcome = outcome;
        // Clear ownership first: any copy that is unloaded now is rejected when its chunk loads.
        state.trialId = null;
        state.lastTrialId = trialId;
        if (outcome != Outcome.SUCCESS) {
            for (Allocation allocation : state.allocations)
                if (allocation.status == AllocationStatus.ALIVE && allocation.entity != null) port.discard(allocation.entity);
        }
        state.allocations.clear();
        if (trialId != null) {
            port.cleanupBoss(trialId);
            port.cleanupSupplies(trialId, state.supplyPositions());
        }
        state.supplyPositions.clear();
        state.lockedParticipants.clear();
        state.deadThisRound.clear();
        state.absenceTicks = 0;
        state.blockedTicks = 0;
        state.spawnBlocked = false;
        state.intermissionRemaining = 0;
        state.bossStarted = false;
        state.phase = Phase.COOLDOWN;
        state.cooldownRemaining = ColosseumRules.COOLDOWN_TICKS;
        if (trialId != null) port.trialEnded(trialId, outcome);
    }

    private static void requeue(Allocation allocation) {
        allocation.toPending();
        allocation.requeues++;
    }

    // Event entry points.

    /** Whether a loaded entity carrying this trial's ownership tag is the live mob of its allocation. */
    public static boolean ownsEntity(ColosseumTrialState state, UUID trialId, int index, UUID entity) {
        if (state.trialId == null || !state.trialId.equals(trialId) || state.phase != Phase.ROUND) return false;
        Allocation allocation = state.allocation(index);
        return allocation != null && allocation.status == AllocationStatus.ALIVE && entity.equals(allocation.entity);
    }

    /**
     * A trial-owned mob died. It counts toward the wave only if at least one player is physically in the combat zone
     * now (Q107); otherwise its allocation returns to PENDING. Returns true when the kill counted.
     */
    public static boolean onTrialMobDeath(ColosseumTrialState state, UUID trialId, int index, UUID entity,
                                          boolean playerInCombatZone) {
        if (!ownsEntity(state, trialId, index, entity)) return false;
        Allocation allocation = state.allocations.get(index);
        if (!playerInCombatZone) {
            requeue(allocation);
            return false;
        }
        allocation.status = AllocationStatus.KILLED;
        return true;
    }

    /** A trial-owned mob was removed without dying (discard, despawn, dimension change). */
    public static void onTrialMobLost(ColosseumTrialState state, UUID trialId, int index, UUID entity) {
        if (ownsEntity(state, trialId, index, entity)) requeue(state.allocations.get(index));
    }

    /** Chunk unload: keep the binding and remember where the mob is stored. */
    public static void onTrialMobUnloaded(ColosseumTrialState state, UUID trialId, int index, UUID entity, long chunk) {
        if (ownsEntity(state, trialId, index, entity)) state.allocations.get(index).chunk = chunk;
    }

    public static void onPlayerDeath(ColosseumTrialState state, UUID player) {
        if ((state.phase == Phase.ROUND || state.phase == Phase.BOSS) && state.lockedParticipants.contains(player))
            state.deadThisRound.add(player);
    }

    /** Integration entry for the future R7 boss: success at boss death. Ignored unless this trial waits in BOSS. */
    public static boolean onBossDefeated(ColosseumTrialState state, ColosseumWorldPort port, @Nullable UUID trialId) {
        if (trialId == null || !trialId.equals(state.trialId) || state.phase != Phase.BOSS || !state.bossStarted) return false;
        state.phase = Phase.SUCCESS;
        endTrial(state, port, Outcome.SUCCESS);
        return true;
    }

    /** Conclusive operator/integration reset; applies the same cooldown as any failure. */
    public static boolean forceReset(ColosseumTrialState state, ColosseumWorldPort port) {
        if (!state.phase.active()) return false;
        fail(state, port, Outcome.FAILED_FORCED_RESET);
        return true;
    }

    /** Test/operator convenience: ends a cooldown immediately (the arena becomes READY). */
    public static boolean clearCooldown(ColosseumTrialState state) {
        if (state.phase != Phase.COOLDOWN) return false;
        state.cooldownRemaining = 0;
        state.phase = Phase.READY;
        return true;
    }
}
