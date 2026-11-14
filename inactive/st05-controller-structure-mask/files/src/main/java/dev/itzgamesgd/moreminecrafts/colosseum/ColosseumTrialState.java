package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Complete persistent state of one arena's trial controller. Everything a restart needs is here: the trial id,
 * round, locked participants, every allocation's PENDING/WARNING/ALIVE/KILLED status (with entity id and last
 * known chunk), generated supply positions, the boss record and all timers. It also holds the arena's append-only
 * {@link RewardLedger} and the Q117/Q118-A work items ({@link BossVanish}, {@link RestoreJournal}), which are
 * serialized at arena level (see {@link ColosseumArena}) because they outlive any one trial. Mutated only by
 * {@link ColosseumController}; positions and chunks are packed longs so the state stays plain data.
 */
public final class ColosseumTrialState {
    public enum Phase {
        READY, ROUND, INTERMISSION, BOSS,
        /** Transient: passed through within one tick on boss defeat, before COOLDOWN. */
        SUCCESS,
        /** Transient: passed through within one tick on failure or reset, before COOLDOWN. */
        FAILURE,
        /** Transient: passed through within one tick when the Q116 trial time limit expires, before COOLDOWN. */
        TIMED_OUT,
        COOLDOWN;

        public boolean active() { return this == ROUND || this == INTERMISSION || this == BOSS; }
    }

    public enum Outcome {
        NONE, SUCCESS,
        /** Total-party defeat: every participant locked for the current round died. */
        FAILED_PARTY_DEFEAT,
        /** The 60-second all-participants-absent grace period expired. */
        FAILED_ABSENCE_RESET,
        /** Q110: every eligible spawn position stayed invalid for 60 seconds of active trial time. */
        FAILED_SPAWN_BLOCKED,
        /** The active roster could not produce any composition (data-validation failure). */
        FAILED_INVALID_ROSTER,
        /** Reset by an operator command or integration call. */
        FAILED_FORCED_RESET,
        /** Q116/Q117: the 65-minute trial limit expired. Ordinary mobs and supplies stay; the boss vanishes. */
        TIMED_OUT
    }

    public enum AllocationStatus { PENDING, WARNING, ALIVE, KILLED }

    public static final long NO_POSITION = Long.MIN_VALUE;

    public static final class Allocation {
        private final int index;
        private final ColosseumMob mob;
        private final boolean focal;
        AllocationStatus status = AllocationStatus.PENDING;
        @Nullable UUID entity;
        long chunk;
        long warningPos = NO_POSITION;
        int warningTicks;
        /** Times this allocation returned to PENDING after a spawned mob was lost without an eligible kill. */
        int requeues;

        Allocation(int index, ColosseumMob mob, boolean focal) {
            this.index = index;
            this.mob = mob;
            this.focal = focal;
        }

        public int index() { return index; }
        public ColosseumMob mob() { return mob; }
        public boolean focal() { return focal; }
        public AllocationStatus status() { return status; }
        @Nullable public UUID entity() { return entity; }
        public long chunk() { return chunk; }
        public long warningPos() { return warningPos; }
        public int warningTicks() { return warningTicks; }
        public int requeues() { return requeues; }

        void toPending() {
            status = AllocationStatus.PENDING;
            entity = null;
            warningPos = NO_POSITION;
            warningTicks = 0;
        }
    }

    /** The Q117 boss record: whether the boss was created and where it lives, so a time-out can make it vanish. */
    public static final class BossRecord {
        boolean started;
        @Nullable UUID entity;
        long chunk;

        public boolean started() { return started; }
        @Nullable public UUID entity() { return entity; }
        public long chunk() { return chunk; }

        void clear() {
            started = false;
            entity = null;
            chunk = 0L;
        }
    }

    Phase phase = Phase.READY;
    Outcome lastOutcome = Outcome.NONE;
    @Nullable UUID trialId;
    @Nullable UUID lastTrialId;
    long trialSerial;
    long seed;
    int round;
    final List<UUID> lockedParticipants = new ArrayList<>();
    final Set<UUID> deadThisRound = new LinkedHashSet<>();
    final Set<UUID> everParticipants = new LinkedHashSet<>();
    final List<Allocation> allocations = new ArrayList<>();
    final List<Long> supplyPositions = new ArrayList<>();
    int intermissionRemaining;
    int absenceTicks;
    int blockedTicks;
    /** The last due batch found no valid position for any pending mob. */
    boolean spawnBlocked;
    int batchTimer;
    int cooldownRemaining;
    /** Q116: ticks of online trial time left; separate from {@link #cooldownRemaining} (I10). */
    int trialRemaining;
    final BossRecord boss = new BossRecord();
    RewardLedger ledger = new RewardLedger();
    final List<BossVanish> bossVanish = new ArrayList<>();
    RestoreJournal journal = new RestoreJournal();
    /** Not persisted: spaces out journal passes; zero after a load so the first tick re-evaluates everything. */
    int journalTimer;

    public Phase phase() { return phase; }
    public Outcome lastOutcome() { return lastOutcome; }
    @Nullable public UUID trialId() { return trialId; }
    @Nullable public UUID lastTrialId() { return lastTrialId; }
    public long trialSerial() { return trialSerial; }
    public int round() { return round; }
    public List<UUID> lockedParticipants() { return List.copyOf(lockedParticipants); }
    public int lockedCount() { return lockedParticipants.size(); }
    public Set<UUID> deadThisRound() { return Set.copyOf(deadThisRound); }
    public Set<UUID> everParticipants() { return Set.copyOf(everParticipants); }
    public List<Allocation> allocations() { return List.copyOf(allocations); }
    public int trialRemaining() { return trialRemaining; }
    public BossRecord boss() { return boss; }
    public RewardLedger ledger() { return ledger; }
    public List<BossVanish> bossVanish() { return List.copyOf(bossVanish); }
    public RestoreJournal journal() { return journal; }
    public List<Long> supplyPositions() { return List.copyOf(supplyPositions); }
    public int intermissionRemaining() { return intermissionRemaining; }
    public int absenceTicks() { return absenceTicks; }
    public boolean paused() { return absenceTicks > 0; }
    public int blockedTicks() { return blockedTicks; }
    public boolean spawnBlocked() { return spawnBlocked; }
    public int cooldownRemaining() { return cooldownRemaining; }
    public boolean bossStarted() { return boss.started; }

    public int count(AllocationStatus status) {
        int count = 0;
        for (Allocation allocation : allocations) if (allocation.status == status) count++;
        return count;
    }

    public int allocatedThreat() { return allocations.stream().mapToInt(a -> a.mob.threat()).sum(); }

    /** Whether {@code round} of the given trial has already been cleared and paid (unique per trial and round). */
    public boolean waveCleared(UUID trial, int round) { return ledger.hasWave(trial, round); }

    /** Open work that must survive until done: no arena holding any may be unregistered (D3). */
    public boolean hasOpenWork() { return !bossVanish.isEmpty() || journal.hasPendingWork(); }

    @Nullable public Allocation allocation(int index) {
        return index >= 0 && index < allocations.size() ? allocations.get(index) : null;
    }

    // Persistence.

    private record AllocationData(int index, ColosseumMob mob, boolean focal, AllocationStatus status,
                                  Optional<UUID> entity, long chunk, long warningPos, int warningTicks, int requeues) {}

    private static final Codec<AllocationData> ALLOCATION_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("index").forGetter(AllocationData::index),
            Codec.STRING.xmap(ColosseumMob::valueOf, ColosseumMob::name).fieldOf("mob").forGetter(AllocationData::mob),
            Codec.BOOL.fieldOf("focal").forGetter(AllocationData::focal),
            Codec.STRING.xmap(AllocationStatus::valueOf, AllocationStatus::name).fieldOf("status").forGetter(AllocationData::status),
            UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(AllocationData::entity),
            Codec.LONG.fieldOf("chunk").forGetter(AllocationData::chunk),
            Codec.LONG.fieldOf("warning_pos").forGetter(AllocationData::warningPos),
            Codec.INT.fieldOf("warning_ticks").forGetter(AllocationData::warningTicks),
            Codec.INT.optionalFieldOf("requeues", 0).forGetter(AllocationData::requeues)
    ).apply(i, AllocationData::new));

    private record Core(Phase phase, Outcome lastOutcome, Optional<UUID> trialId, Optional<UUID> lastTrialId,
                        long trialSerial, long seed, int round) {}

    private record Timers(int intermissionRemaining, int absenceTicks, int blockedTicks, boolean spawnBlocked,
                          int batchTimer, int cooldownRemaining, int trialRemaining) {}

    private record BossData(boolean started, Optional<UUID> entity, long chunk) {}

    private record Party(List<UUID> locked, List<UUID> dead, List<UUID> ever) {}

    private record Content(List<AllocationData> allocations, List<Long> supplies, BossData boss) {}

    private record Snapshot(Core core, Timers timers, Party party, Content content) {}

    private static final Codec<Core> CORE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(Phase::valueOf, Phase::name).fieldOf("phase").forGetter(Core::phase),
            Codec.STRING.xmap(Outcome::valueOf, Outcome::name).fieldOf("last_outcome").forGetter(Core::lastOutcome),
            UUIDUtil.CODEC.optionalFieldOf("trial").forGetter(Core::trialId),
            UUIDUtil.CODEC.optionalFieldOf("last_trial").forGetter(Core::lastTrialId),
            Codec.LONG.fieldOf("serial").forGetter(Core::trialSerial),
            Codec.LONG.fieldOf("seed").forGetter(Core::seed),
            Codec.INT.fieldOf("round").forGetter(Core::round)
    ).apply(i, Core::new));

    private static final Codec<Timers> TIMERS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("intermission").forGetter(Timers::intermissionRemaining),
            Codec.INT.fieldOf("absence").forGetter(Timers::absenceTicks),
            Codec.INT.fieldOf("spawn_blocked_ticks").forGetter(Timers::blockedTicks),
            Codec.BOOL.fieldOf("spawn_blocked").forGetter(Timers::spawnBlocked),
            Codec.INT.fieldOf("batch_timer").forGetter(Timers::batchTimer),
            Codec.INT.fieldOf("cooldown").forGetter(Timers::cooldownRemaining),
            Codec.INT.optionalFieldOf("trial_remaining", ColosseumRules.TRIAL_LIMIT_TICKS).forGetter(Timers::trialRemaining)
    ).apply(i, Timers::new));

    private static final Codec<BossData> BOSS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("started", false).forGetter(BossData::started),
            UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(BossData::entity),
            Codec.LONG.optionalFieldOf("chunk", 0L).forGetter(BossData::chunk)
    ).apply(i, BossData::new));

    private static final Codec<Party> PARTY_CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.listOf().fieldOf("locked").forGetter(Party::locked),
            UUIDUtil.CODEC.listOf().fieldOf("dead").forGetter(Party::dead),
            UUIDUtil.CODEC.listOf().fieldOf("ever").forGetter(Party::ever)
    ).apply(i, Party::new));

    private static final Codec<Content> CONTENT_CODEC = RecordCodecBuilder.create(i -> i.group(
            ALLOCATION_CODEC.listOf().fieldOf("allocations").forGetter(Content::allocations),
            Codec.LONG.listOf().fieldOf("supplies").forGetter(Content::supplies),
            BOSS_CODEC.optionalFieldOf("boss", new BossData(false, Optional.empty(), 0L)).forGetter(Content::boss)
    ).apply(i, Content::new));

    private static final Codec<Snapshot> SNAPSHOT_CODEC = RecordCodecBuilder.create(i -> i.group(
            CORE_CODEC.fieldOf("core").forGetter(Snapshot::core),
            TIMERS_CODEC.fieldOf("timers").forGetter(Snapshot::timers),
            PARTY_CODEC.fieldOf("party").forGetter(Snapshot::party),
            CONTENT_CODEC.fieldOf("content").forGetter(Snapshot::content)
    ).apply(i, Snapshot::new));

    /** The {@code trial} section of an arena record. Ledger, vanish tasks and journal are separate sections. */
    public static final Codec<ColosseumTrialState> CODEC =
            SNAPSHOT_CODEC.xmap(ColosseumTrialState::fromSnapshot, ColosseumTrialState::toSnapshot);

    private Snapshot toSnapshot() {
        List<AllocationData> allocationData = new ArrayList<>();
        for (Allocation a : allocations)
            allocationData.add(new AllocationData(a.index, a.mob, a.focal, a.status, Optional.ofNullable(a.entity),
                    a.chunk, a.warningPos, a.warningTicks, a.requeues));
        return new Snapshot(
                new Core(phase, lastOutcome, Optional.ofNullable(trialId), Optional.ofNullable(lastTrialId),
                        trialSerial, seed, round),
                new Timers(intermissionRemaining, absenceTicks, blockedTicks, spawnBlocked, batchTimer, cooldownRemaining,
                        trialRemaining),
                new Party(List.copyOf(lockedParticipants), List.copyOf(deadThisRound), List.copyOf(everParticipants)),
                new Content(allocationData, List.copyOf(supplyPositions),
                        new BossData(boss.started, Optional.ofNullable(boss.entity), boss.chunk)));
    }

    private static ColosseumTrialState fromSnapshot(Snapshot snapshot) {
        ColosseumTrialState state = new ColosseumTrialState();
        Core core = snapshot.core();
        state.phase = core.phase();
        state.lastOutcome = core.lastOutcome();
        state.trialId = core.trialId().orElse(null);
        state.lastTrialId = core.lastTrialId().orElse(null);
        state.trialSerial = core.trialSerial();
        state.seed = core.seed();
        state.round = core.round();
        Timers timers = snapshot.timers();
        state.intermissionRemaining = timers.intermissionRemaining();
        state.absenceTicks = timers.absenceTicks();
        state.blockedTicks = timers.blockedTicks();
        state.spawnBlocked = timers.spawnBlocked();
        state.batchTimer = timers.batchTimer();
        state.cooldownRemaining = timers.cooldownRemaining();
        state.trialRemaining = timers.trialRemaining();
        state.boss.started = snapshot.content().boss().started();
        state.boss.entity = snapshot.content().boss().entity().orElse(null);
        state.boss.chunk = snapshot.content().boss().chunk();
        state.lockedParticipants.addAll(snapshot.party().locked());
        state.deadThisRound.addAll(snapshot.party().dead());
        state.everParticipants.addAll(snapshot.party().ever());
        for (AllocationData data : snapshot.content().allocations()) {
            Allocation a = new Allocation(data.index(), data.mob(), data.focal());
            a.status = data.status();
            a.entity = data.entity().orElse(null);
            a.chunk = data.chunk();
            a.warningPos = data.warningPos();
            a.warningTicks = data.warningTicks();
            a.requeues = data.requeues();
            state.allocations.add(a);
        }
        state.supplyPositions.addAll(snapshot.content().supplies());
        // A transient phase can only be persisted by an interrupted tick; finish it as the controller would.
        if (state.phase == Phase.SUCCESS || state.phase == Phase.FAILURE || state.phase == Phase.TIMED_OUT) {
            state.phase = Phase.COOLDOWN;
            state.cooldownRemaining = Math.max(state.cooldownRemaining, 1);
        }
        return state;
    }
}
