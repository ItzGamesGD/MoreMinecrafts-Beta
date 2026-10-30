package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumOwnership.Kind;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Allocation;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.AllocationStatus;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Outcome;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumTrialState.Phase;
import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyRecord;
import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyState;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authoritative ST-05 trial state machine: READY -> ROUND -> INTERMISSION -> ... -> BOSS -> SUCCESS /
 * FAILURE / TIMED_OUT -> COOLDOWN -> READY. The all-absent pause and the Q110 spawn-blocked clock are tracked
 * independently of the phase, and the 65-minute trial limit (Q116) runs separately from the cooldown. One call to
 * {@link #tick} is one server tick of online trial time.
 * <p>
 * Allocation accounting keeps pending, warning, alive and valid-kill counts distinct. A mob that leaves without an
 * eligible kill (discarded, despawned, changed dimension, confirmed missing after a restart, or killed while no
 * player was in the combat zone) returns its allocation to PENDING (Q111-A): the allocation is reused, never
 * duplicated or lost, and a round never completes on a disappearance. A mob in an unloaded chunk is not lost.
 * <p>
 * The three endings are distinct (see the C0 contract, section 4.4): success, failure/reset, and time-out. Only
 * time-out leaves ordinary mobs and supply chests in place (Q116) and makes the boss vanish without a kill (Q117).
 */
public final class ColosseumController {
    private ColosseumController() {}

    public static void tick(ColosseumTrialState state, ColosseumWorldPort port) {
        processWork(state, port);
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
            case SUCCESS, FAILURE, TIMED_OUT -> endTrial(state, port, state.lastOutcome);
        }
    }

    private static void startTrial(ColosseumTrialState state, ColosseumWorldPort port) {
        state.trialId = UUID.randomUUID();
        state.trialSerial++;
        state.seed = port.nextSeed();
        state.lastOutcome = Outcome.NONE;
        state.supplyPositions.clear();
        state.everParticipants.clear();
        state.boss.clear();
        state.absenceTicks = 0;
        state.trialRemaining = ColosseumRules.TRIAL_LIMIT_TICKS;
        port.activationEffect();
        startRound(state, port, 1, port.playersInCombatZone());
    }

    private static void activeTick(ColosseumTrialState state, ColosseumWorldPort port) {
        // Q116: the trial limit is checked first and keeps running while everyone is absent.
        if (--state.trialRemaining <= 0) {
            state.trialRemaining = 0;
            state.phase = Phase.TIMED_OUT;
            state.lastOutcome = Outcome.TIMED_OUT;
            endTrial(state, port, Outcome.TIMED_OUT);
            return;
        }
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
                if (!state.boss.started) startBoss(state, port);
            }
            default -> {}
        }
    }

    private static void startBoss(ColosseumTrialState state, ColosseumWorldPort port) {
        boolean started = port.startBoss(state.trialId, state.lockedParticipants(), (entity, chunk) -> {
            state.boss.started = true;
            state.boss.entity = entity;
            state.boss.chunk = chunk;
        });
        if (started) state.boss.started = true; // a handler that did not bind is tracked as started without an entity
        else state.boss.clear();
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
        state.boss.clear();
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
        // The boss is only tracked here. A boss that is gone is not a Q111-A requeue and never counts as a defeat;
        // whether to materialize it again is the boss handler's decision.
        if (state.phase == Phase.BOSS && state.boss.started && state.boss.entity != null) {
            ColosseumWorldPort.Lookup lookup = port.locate(state.boss.entity, state.boss.chunk);
            if (lookup.presence() == ColosseumWorldPort.Presence.ALIVE) state.boss.chunk = lookup.chunk();
        }
    }

    private static void tickWarnings(ColosseumTrialState state, ColosseumWorldPort port) {
        for (Allocation allocation : state.allocations) {
            if (allocation.status != AllocationStatus.WARNING) continue;
            port.warningEffect(allocation.warningPos);
            if (--allocation.warningTicks > 0) continue;
            long pos = allocation.warningPos;
            if (port.isValidSpawnPosition(allocation.mob(), pos) && port.spawn(state.trialId, state.round, allocation, pos,
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

    /**
     * Records the wave once per {@code (trial, round)} (D2: the ledger is never cleared), the Q114-C eligibility of
     * each locked participant who is still in the zone, and the wave's single shared Trial Key (Q113-B).
     */
    private static void completeRound(ColosseumTrialState state, ColosseumWorldPort port, Set<UUID> present) {
        int round = state.round;
        UUID trial = state.trialId;
        List<UUID> presentSorted = new ArrayList<>(present);
        presentSorted.sort(null);
        long now = port.gameTime();
        WaveRecord record = WaveRecord.cleared(trial, state.trialSerial, round, state.lockedParticipants(),
                List.copyOf(presentSorted), now, KeyState.PENDING);
        if (!state.ledger.addWave(record)) return; // never complete or pay a wave twice
        for (UUID player : state.lockedParticipants)
            if (present.contains(player))
                state.ledger.addEligibility(new RewardLedger.EligibilityRecord(trial, round, player,
                        RewardLedger.BASIS_Q114_C, now));
        state.allocations.clear();
        state.deadThisRound.clear(); // dead participants may rejoin between this clear and the next round start
        state.blockedTicks = 0;
        state.spawnBlocked = false;
        state.phase = Phase.INTERMISSION;
        state.intermissionRemaining = ColosseumRules.intermissionTicksAfter(round);
        port.waveCleared(record);
        if (ColosseumRules.isSupplyRound(round))
            state.supplyPositions.addAll(port.placeSupplies(trial, round, record.lockedParticipants().size()));
        processKeys(state, port);
    }

    private static void fail(ColosseumTrialState state, ColosseumWorldPort port, Outcome outcome) {
        state.phase = Phase.FAILURE;
        state.lastOutcome = outcome;
        endTrial(state, port, outcome);
    }

    /**
     * Ends the trial. The three endings differ (contract section 4.4):
     * <ul>
     *   <li>SUCCESS: every mob is already dead; supply chests are cleaned up (Q96).</li>
     *   <li>FAILED_*: this trial's mobs and boss are removed without loot or XP (Q102) and supply chests are cleaned.</li>
     *   <li>TIMED_OUT: ordinary mobs and supply chests stay (Q116, mobs are released to external); the boss vanishes
     *       without dying behind a crystal ending presentation (Q117).</li>
     * </ul>
     * The ledger, shared keys and restore journal are never touched by an ending.
     */
    private static void endTrial(ColosseumTrialState state, ColosseumWorldPort port, Outcome outcome) {
        UUID trialId = state.trialId;
        state.lastOutcome = outcome;
        // The trial no longer owns anything: an entity that loads later is judged by decideLoad.
        state.trialId = null;
        state.lastTrialId = trialId;
        boolean bossHad = state.boss.started;
        if (trialId != null) {
            switch (outcome) {
                case SUCCESS -> {
                    port.cleanupSupplies(trialId, state.supplyPositions());
                    if (bossHad) port.bossRemoved(trialId, ColosseumWorldPort.RemovalCause.DEFEATED);
                }
                case TIMED_OUT -> timeOutCleanup(state, port, trialId);
                default -> failureCleanup(state, port, trialId);
            }
        }
        state.allocations.clear();
        state.supplyPositions.clear();
        state.lockedParticipants.clear();
        state.deadThisRound.clear();
        state.absenceTicks = 0;
        state.blockedTicks = 0;
        state.spawnBlocked = false;
        state.intermissionRemaining = 0;
        state.trialRemaining = 0;
        state.boss.clear();
        state.phase = Phase.COOLDOWN;
        state.cooldownRemaining = ColosseumRules.COOLDOWN_TICKS;
        if (trialId != null) {
            if (outcome == Outcome.TIMED_OUT && bossHad) port.crystalEnding(trialId);
            port.trialEnded(trialId, outcome);
        }
    }

    private static void failureCleanup(ColosseumTrialState state, ColosseumWorldPort port, UUID trialId) {
        for (Allocation allocation : state.allocations)
            if (allocation.status == AllocationStatus.ALIVE && allocation.entity != null) port.discard(allocation.entity);
        port.cleanupSupplies(trialId, state.supplyPositions());
        if (state.boss.started) {
            if (state.boss.entity != null) port.discard(state.boss.entity); // not loaded: rejected on load (Q102)
            port.bossRemoved(trialId, ColosseumWorldPort.RemovalCause.REMOVED);
        }
    }

    private static void timeOutCleanup(ColosseumTrialState state, ColosseumWorldPort port, UUID trialId) {
        // Release first so a mob that loads from here on is accepted as external (Q116).
        state.ledger.release(trialId);
        for (Allocation allocation : state.allocations)
            if (allocation.status == AllocationStatus.ALIVE && allocation.entity != null) port.release(allocation.entity);
        // Supply chests are deliberately not cleaned up (Q116): forget them without removing anything.
        if (state.boss.started) {
            state.bossVanish.add(new BossVanish(trialId, state.boss.entity, state.boss.chunk, port.gameTime()));
            processBossVanish(state, port);
        }
    }

    private static void requeue(Allocation allocation) {
        allocation.toPending();
        allocation.requeues++;
    }

    // Work items: shared keys, boss vanishing and block restoration. Run every tick in every phase.

    private static void processWork(ColosseumTrialState state, ColosseumWorldPort port) {
        processKeys(state, port);
        processBossVanish(state, port);
        processJournal(state, port);
    }

    /**
     * Shared Trial Key lifecycle (Q113-B, Q97). PENDING is created now. A key whose entity is confirmed missing is
     * re-issued once if it was never seen reach storage (SPAWNED: lost to a crash) and treated as consumed otherwise
     * (CONFIRMED). In-session removals (pickup, merge, despawn) are recorded as CONSUMED by the unload event, so
     * they are never mistaken for a loss here.
     */
    private static void processKeys(ColosseumTrialState state, ColosseumWorldPort port) {
        for (WaveRecord wave : state.ledger.waves()) {
            KeyRecord key = wave.key();
            switch (key.state) {
                case PENDING -> {
                    boolean added = port.spawnKey(wave.trialId(), wave.round(), (entity, chunk) -> {
                        key.state = KeyState.SPAWNED;
                        key.entity = entity;
                        key.chunk = chunk;
                        key.issueCount++;
                    });
                    if (!added && key.state == KeyState.SPAWNED) { // bound but could not be added
                        key.state = KeyState.PENDING;
                        key.entity = null;
                        key.issueCount--;
                    }
                }
                case SPAWNED, CONFIRMED -> {
                    if (key.entity == null) {
                        key.state = KeyState.PENDING;
                        break;
                    }
                    ColosseumWorldPort.Lookup lookup = port.locate(key.entity, key.chunk);
                    switch (lookup.presence()) {
                        case ALIVE -> key.chunk = lookup.chunk();
                        case GONE -> {
                            if (key.state == KeyState.SPAWNED) {
                                key.state = KeyState.PENDING; // lost before it reached storage: one reissue
                                key.entity = null;
                            } else {
                                key.state = KeyState.CONSUMED;
                            }
                        }
                        case NOT_LOADED -> {}
                    }
                }
                default -> {}
            }
        }
    }

    private static void processBossVanish(ColosseumTrialState state, ColosseumWorldPort port) {
        if (state.bossVanish.isEmpty()) return;
        for (BossVanish task : new ArrayList<>(state.bossVanish)) {
            boolean done;
            if (task.entity() == null) {
                done = true; // a boss that was never bound has nothing to remove
            } else {
                ColosseumWorldPort.Lookup lookup = port.locate(task.entity(), task.chunk());
                switch (lookup.presence()) {
                    case ALIVE -> {
                        port.discard(task.entity()); // removal without dying: no death event, loot, XP or key
                        done = true;
                    }
                    case GONE -> done = true;
                    default -> done = false; // in an unloaded chunk: completes when it loads or is confirmed gone
                }
            }
            if (done) finishVanish(state, port, task);
        }
    }

    private static void finishVanish(ColosseumTrialState state, ColosseumWorldPort port, BossVanish task) {
        task.state(BossVanish.State.REMOVED);
        state.bossVanish.remove(task);
        port.bossRemoved(task.trial(), ColosseumWorldPort.RemovalCause.VANISHED);
        port.bossVanished(task.trial());
    }

    /**
     * Restores boss-destroyed blocks of trials that are no longer active (Q91, Q117) under Q118-A: only an empty
     * position is filled, a conflicting block is recorded and left alone, and unloaded positions wait. Entries of
     * the active trial are left broken until it ends.
     */
    private static void processJournal(ColosseumTrialState state, ColosseumWorldPort port) {
        if (state.journal.isEmpty() || --state.journalTimer > 0) return;
        state.journalTimer = ColosseumRules.JOURNAL_PERIOD_TICKS;
        int handled = 0;
        for (RestoreJournal.Entry entry : new ArrayList<>(state.journal.entries())) {
            if (handled >= ColosseumRules.JOURNAL_BATCH) break;
            if (entry.trial().equals(state.trialId)) continue;
            RestoreJournal.Attempt attempt = port.restoreBlock(entry);
            switch (attempt.result()) {
                case RESTORED, ALREADY_RESTORED -> {
                    state.journal.remove(entry);
                    handled++;
                }
                case CONFLICT -> {
                    entry.markConflict(port.gameTime(), attempt.found());
                    handled++;
                }
                case NOT_LOADED -> {}
            }
        }
    }

    // Load decisions (contract I-ID2) and event entry points.

    public enum LoadDecision {
        /** Load it as this trial's own mob or boss. */
        ACCEPT,
        /** Do not add the entity to the world. */
        REJECT,
        /** A time-out released this mob: load it, and strip its ownership tag so it is plain external (Q116). */
        ACCEPT_RELEASED
    }

    /**
     * The I-ID2 table. An entity tagged for the active trial loads only if the controller records it; a tag of an
     * ended trial is rejected, except that a time-out releases ordinary mobs (accepted as external) while its boss is
     * always rejected, which completes the Q117 vanishing.
     */
    public static LoadDecision decideLoad(ColosseumTrialState state, UUID trial, Kind kind, int allocation, UUID entity) {
        if (state.trialId != null && state.trialId.equals(trial)) {
            boolean owned = kind == Kind.MOB ? ownsEntity(state, trial, allocation, entity) : entity.equals(state.boss.entity);
            return owned ? LoadDecision.ACCEPT : LoadDecision.REJECT;
        }
        if (state.ledger.isReleased(trial)) return kind == Kind.MOB ? LoadDecision.ACCEPT_RELEASED : LoadDecision.REJECT;
        return LoadDecision.REJECT;
    }

    /** A rejected boss of a timed-out trial counts as vanished if it is the one the task was waiting for. */
    public static void onBossLoadRejected(ColosseumTrialState state, ColosseumWorldPort port, UUID trial, UUID entity) {
        for (BossVanish task : new ArrayList<>(state.bossVanish))
            if (task.trial().equals(trial) && entity.equals(task.entity())) finishVanish(state, port, task);
    }

    /** Keeps the remembered chunk of the boss (active or waiting to vanish) current when its chunk unloads. */
    public static void onBossUnloaded(ColosseumTrialState state, UUID trial, UUID entity, long chunk) {
        if (state.trialId != null && state.trialId.equals(trial) && entity.equals(state.boss.entity)) state.boss.chunk = chunk;
        for (BossVanish task : state.bossVanish)
            if (task.trial().equals(trial) && entity.equals(task.entity())) task.chunk(chunk);
    }

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

    /** A shared key entity left the world: to chunk storage (CONFIRMED) or for good (CONSUMED, never reissued). */
    public static void onKeyUnloaded(ColosseumTrialState state, UUID trial, int round, UUID entity, long chunk,
                                     boolean toStorage) {
        WaveRecord wave = state.ledger.wave(trial, round);
        if (wave == null) return;
        KeyRecord key = wave.key();
        if ((key.state != KeyState.SPAWNED && key.state != KeyState.CONFIRMED) || !entity.equals(key.entity)) return;
        key.chunk = chunk;
        key.state = toStorage ? KeyState.CONFIRMED : KeyState.CONSUMED;
    }

    /** A key entity is about to load: only the one recorded for its wave is accepted; any other copy is a duplicate. */
    public static boolean decideKeyLoad(ColosseumTrialState state, UUID trial, int round, UUID entity, boolean fromStorage) {
        WaveRecord wave = state.ledger.wave(trial, round);
        if (wave == null) return false;
        KeyRecord key = wave.key();
        if ((key.state != KeyState.SPAWNED && key.state != KeyState.CONFIRMED) || !entity.equals(key.entity)) return false;
        if (fromStorage) key.state = KeyState.CONFIRMED;
        return true;
    }

    public static void onPlayerDeath(ColosseumTrialState state, UUID player) {
        if ((state.phase == Phase.ROUND || state.phase == Phase.BOSS) && state.lockedParticipants.contains(player))
            state.deadThisRound.add(player);
    }

    /** Integration entry for the future R7 boss: success at boss death. Ignored unless this trial waits in BOSS. */
    public static boolean onBossDefeated(ColosseumTrialState state, ColosseumWorldPort port, @Nullable UUID trialId) {
        if (trialId == null || !trialId.equals(state.trialId) || state.phase != Phase.BOSS || !state.boss.started) return false;
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
