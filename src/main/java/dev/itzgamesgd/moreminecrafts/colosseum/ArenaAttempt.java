package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * One challenge attempt: the pure, deterministic state machine for the seven rounds. It owns no world objects; the runtime feeds it
 * observations each server tick and executes the {@link Action}s it returns. Rules implemented:
 * <ul>
 * <li>Round-boundary admission (2026-11-02 amendment): roster is locked per round; players entering after a round began are pending and
 * are admitted (if still present and alive) when the next round starts; a re-entering admitted member is a return, never a new admission.</li>
 * <li>M(n) = 1 + 0.6(n-1) with n = max(current active, historical maximum active), never decreasing.</li>
 * <li>Wipe of all admitted members fails immediately; no admitted member present starts the 180 s grace (arena timers freeze); the 65 min cap
 * and the grace run on wall-clock time; whichever expires first wins.</li>
 * <li>Spawn plan is persisted before effect; 12-alive cap, replenish at 6 or fewer, 1 s warning, uncredited/lost mobs return to pending.</li>
 * </ul>
 */
public final class ArenaAttempt {
    public enum Phase { ROUND, INTERMISSION, ENDED }
    public enum Outcome { NONE, SUCCESS, WIPE, ABSENCE_EXPIRED, BLOCKED, TIMED_OUT, QUARANTINED }
    public enum AllocStatus { PENDING, WARNING, SPAWNED, KILLED }

    public static final class Alloc {
        public final UUID id;
        public final String mob;
        public AllocStatus status = AllocStatus.PENDING;
        public int warnLeft;
        public int px, py, pz;
        public UUID entity;
        public long lastChunk;
        public boolean chunkKnown;
        Alloc(UUID id, String mob) { this.id = id; this.mob = mob; }
    }

    public static final class Member {
        public final UUID id;
        public final int admittedRound;
        public boolean dead;
        Member(UUID id, int admittedRound) { this.id = id; this.admittedRound = admittedRound; }
    }

    /** Locked at each round start; never changed retroactively. */
    public record Snapshot(int round, int n, int multiplierMilli, int budget, int supplyChests, List<UUID> locked) {}

    public record Obs(Set<UUID> insideAlive, boolean entitiesLoaded) {}

    public interface SpawnPlanner { int[] position(Alloc alloc); }

    public sealed interface Action permits RoundStart, WarnSpawn, SpawnNow, DropKey, SupplyChests, BossStart, End {}
    public record RoundStart(int round, int n, int budget) implements Action {}
    public record WarnSpawn(Alloc alloc) implements Action {}
    public record SpawnNow(Alloc alloc) implements Action {}
    public record DropKey(int round, boolean ominous) implements Action {}
    public record SupplyChests(int round, int count) implements Action {}
    public record BossStart(int n, int maxHp) implements Action {}
    public record End(Outcome outcome, boolean cleanup) implements Action {}

    // ---- persisted state ----
    public final UUID attemptId;
    final long seed;
    Phase phase = Phase.ROUND;
    int round;
    Outcome outcome = Outcome.NONE;
    String quarantineReason = "";
    long startMillis, graceExpiryMillis, lastSeenMillis;
    long onlineTicks;
    int intermissionLeft, blockedTicks, peak;
    boolean bossRequested, bossDefeated;
    UUID bossEntity;
    long bossChunk;
    boolean bossChunkKnown, cleanupPending;
    double bossDeathX, bossDeathY, bossDeathZ;
    float bossHealth=-1;
    String bossState="";
    TerrainLedger terrain = new TerrainLedger();
    final Map<UUID, Member> roster = new LinkedHashMap<>();
    final Map<UUID, Integer> pending = new LinkedHashMap<>();
    Snapshot snap;
    final List<Alloc> allocs = new ArrayList<>();
    final Set<Integer> keysAwarded = new LinkedHashSet<>();
    final Set<Integer> suppliesPlaced = new LinkedHashSet<>();
    final Set<String> entitlements = new LinkedHashSet<>();
    final List<Long> supplyChests = new ArrayList<>();
    /** Roster ids for the log of admissions: "uuid@round". */
    final List<String> admissions = new ArrayList<>();

    // ---- transient ----
    private final List<UUID> deathQueue = new ArrayList<>();
    private final List<UUID> killedMobs = new ArrayList<>();
    private final List<UUID> lostMobs = new ArrayList<>();
    private Set<UUID> presentAlive = Set.of();

    ArenaAttempt(UUID attemptId, long seed) { this.attemptId = attemptId; this.seed = seed; }

    /** Starts an attempt whose first-round roster is every eligible entrant gathered in one tick (order-independent). */
    public static ArenaAttempt start(UUID id, long nowMillis, Collection<UUID> entrants, SpawnPlanner planner, List<Action> out) {
        ArenaAttempt a = new ArenaAttempt(id, id.getMostSignificantBits() ^ id.getLeastSignificantBits());
        a.startMillis = a.lastSeenMillis = nowMillis;
        for (UUID u : new TreeSet<>(entrants)) a.roster.put(u, new Member(u, 1));
        for (UUID u : a.roster.keySet()) a.admissions.add(u + "@1");
        a.beginRound(1, new LinkedHashSet<>(a.roster.keySet()), out);
        return a;
    }

    // ---- accessors ----
    public Phase phase() { return phase; }
    public int round() { return round; }
    public Outcome outcome() { return outcome; }
    public String quarantineReason() { return quarantineReason; }
    public boolean ended() { return phase == Phase.ENDED; }
    public Snapshot snapshot() { return snap; }
    public int peak() { return peak; }
    public Set<UUID> rosterIds() { return roster.keySet(); }
    public Set<UUID> pendingIds() { return pending.keySet(); }
    public Member member(UUID u) { return roster.get(u); }
    public List<Alloc> allocations() { return allocs; }
    public Set<Integer> keysAwarded() { return keysAwarded; }
    public Set<String> entitlements() { return entitlements; }
    public long graceExpiryMillis() { return graceExpiryMillis; }
    public long onlineTicks() { return onlineTicks; }
    public int blockedTicks() { return blockedTicks; }
    public List<Long> supplyChests() { return supplyChests; }
    public boolean bossDefeated() { return bossDefeated; }

    public int count(AllocStatus s) { int c = 0; for (Alloc a : allocs) if (a.status == s) c++; return c; }

    // ---- events ----
    public void onPlayerDeath(UUID player) { deathQueue.add(player); }
    public void onMobKilled(UUID entity) { killedMobs.add(entity); }
    public void onMobLost(UUID entity) { lostMobs.add(entity); }

    /** Runtime result of a {@link SpawnNow}: binds the entity, or returns the allocation to pending when the spawn could not happen. */
    public void spawnResult(Alloc alloc, UUID entity) {
        if (alloc.status != AllocStatus.WARNING) return;   // idempotent: a repeated result never double-binds
        if (entity == null) { alloc.status = AllocStatus.PENDING; alloc.entity = null; return; }
        alloc.status = AllocStatus.SPAWNED;
        alloc.entity = entity;
    }

    /** Boss integration: the boss subsystem calls this once when the Immortal Gladiator is killed by the normal route. */
    public void onBossDefeated() { if (bossRequested) bossDefeated = true; }

    public void quarantine(String reason, List<Action> out) {
        if (phase == Phase.ENDED) return;
        quarantineReason = reason;
        end(Outcome.QUARANTINED, false, out);
    }

    // ---- the tick ----
    public List<Action> tick(long nowMillis, Obs obs, SpawnPlanner planner) {
        List<Action> out = new ArrayList<>();
        if (phase == Phase.ENDED) return out;
        if (nowMillis < lastSeenMillis) { quarantine("wall clock moved backwards " + (lastSeenMillis - nowMillis) + " ms", out); return out; }
        lastSeenMillis = nowMillis;
        onlineTicks++;
        presentAlive = obs.insideAlive();

        // 1. death events (deterministic: queue order)
        for (UUID u : deathQueue) {
            Member m = roster.get(u);
            if (m != null) m.dead = true;
            pending.remove(u);
        }
        deathQueue.clear();

        // 2. returns: an admitted dead member who is present between rounds is revived for the next round
        if (phase == Phase.INTERMISSION) for (Member m : roster.values()) if (m.dead && obs.insideAlive().contains(m.id)) m.dead = false;

        // 3. presence among admitted members; pending joiners never count (amendment item 6)
        int present = 0;
        for (Member m : roster.values()) if (!m.dead && obs.insideAlive().contains(m.id)) present++;
        boolean allDead = true;
        for (Member m : roster.values()) if (!m.dead) { allDead = false; break; }

        // 4. wall-clock limits: whichever of the 65-minute cap and the grace expires first wins
        RoundPlan.Timing t = timing;
        if (graceExpiryMillis > 0 && graceExpiryMillis <= startMillis + t.limitMillis() && nowMillis >= graceExpiryMillis) { end(Outcome.ABSENCE_EXPIRED, true, out); return out; }
        if (nowMillis - startMillis >= t.limitMillis()) { end(Outcome.TIMED_OUT, false, out); return out; }
        if (!roster.isEmpty() && allDead) { end(Outcome.WIPE, true, out); return out; }
        if (present == 0) {
            if (graceExpiryMillis == 0) graceExpiryMillis = nowMillis + t.graceMillis();
            if (nowMillis >= graceExpiryMillis) { end(Outcome.ABSENCE_EXPIRED, true, out); return out; }
        } else graceExpiryMillis = 0;

        boolean paused = present == 0;
        if (!obs.entitiesLoaded()) return out;  // Hold queued events AND every arena-owned timer until the world is verified.
        processMobEvents(present > 0);
        if (paused) return out;           // arena-owned timers freeze; 65 min and the grace keep elapsing on the wall clock

        if (phase == Phase.INTERMISSION) {
            if (--intermissionLeft <= 0) boundary(obs, out);
            return out;
        }
        roundTick(obs, planner, out);
        return out;
    }

    RoundPlan.Timing timing = RoundPlan.Timing.APPROVED;
    public void setTiming(RoundPlan.Timing t) { timing = t; }

    /** A re-entry or a first entry observed this tick by the controller. Returns true if it was a new pending joiner. */
    public boolean onEntry(UUID player) {
        if (phase == Phase.ENDED) return false;
        if (roster.containsKey(player)) return false;             // existing participant: a return, not a new admission
        return pending.putIfAbsent(player, round) == null;         // idempotent
    }

    public void onPlayerGone(UUID player) { if (!roster.containsKey(player)) pending.remove(player); }

    private void processMobEvents(boolean creditable) {
        for (UUID e : killedMobs) {
            Alloc a = byEntity(e);
            if (a == null || a.status != AllocStatus.SPAWNED) continue;
            if (creditable) a.status = AllocStatus.KILLED;
            else { a.status = AllocStatus.PENDING; a.entity = null; }   // Q111-A: uncredited death returns the allocation
        }
        killedMobs.clear();
        for (UUID e : lostMobs) {
            Alloc a = byEntity(e);
            if (a == null || a.status != AllocStatus.SPAWNED) continue;
            a.status = AllocStatus.PENDING; a.entity = null;
        }
        lostMobs.clear();
    }

    private Alloc byEntity(UUID e) { for (Alloc a : allocs) if (e.equals(a.entity)) return a; return null; }

    private void roundTick(Obs obs, SpawnPlanner planner, List<Action> out) {
        if (!obs.entitiesLoaded()) return;      // Q40: cannot confirm entity presence -> no kill/replenish/clear decisions
        if (round == RoundPlan.ROUNDS) { bossTick(out); return; }
        // warnings count down, then the spawn is attempted
        for (Alloc a : allocs) {
            if (a.status != AllocStatus.WARNING) continue;
            if (--a.warnLeft <= 0) out.add(new SpawnNow(a));
        }
        int pend = count(AllocStatus.PENDING), warn = count(AllocStatus.WARNING), alive = count(AllocStatus.SPAWNED);
        // initial batch (nothing spawned yet) or replenishment at <= 6 alive, up to the 12-alive cap
        boolean cycle = pend > 0 && warn == 0 && (alive + warn <= RoundPlan.REPLENISH_AT_OR_BELOW);
        if (cycle) {
            int room = RoundPlan.MAX_ALIVE - alive - warn;
            boolean anyValid = false;
            for (Alloc a : allocs) {
                if (room <= 0) break;
                if (a.status != AllocStatus.PENDING) continue;
                int[] p = planner.position(a);
                if (p == null) continue;   // a failed position check never consumes an allocation
                a.px = p[0]; a.py = p[1]; a.pz = p[2]; a.status = AllocStatus.WARNING; a.warnLeft = RoundPlan.WARNING_TICKS;
                out.add(new WarnSpawn(a));
                room--; anyValid = true;
            }
            if (anyValid) blockedTicks = 0; else blockedTicks++;
        } else if (pend > 0 && alive + warn >= RoundPlan.MAX_ALIVE) blockedTicks = 0;   // legitimately held by the 12-alive cap
        if (blockedTicks >= RoundPlan.BLOCKAGE_TICKS) { end(Outcome.BLOCKED, true, out); return; }
        if (count(AllocStatus.KILLED) == allocs.size() && !allocs.isEmpty()) clearRound(out);
    }

    private void bossTick(List<Action> out) {
        if (!bossRequested) { bossRequested = true; out.add(new BossStart(snap.n(), RoundPlan.bossMaxHp(snap.n()))); }
        if (bossDefeated) {
            award(round, true, out);
            end(Outcome.SUCCESS, true, out);
        }
    }

    /** Wave clear: exactly one shared key per round, plus one personal entitlement per eligible (locked and present) participant. */
    private void clearRound(List<Action> out) {
        award(round, false, out);
        if (RoundPlan.hasSupplies(round) && suppliesPlaced.add(round)) out.add(new SupplyChests(round, snap.supplyChests()));
        phase = Phase.INTERMISSION;
        intermissionLeft = RoundPlan.intermissionAfter(round);
        allocs.clear();
    }

    private void award(int r, boolean ominous, List<Action> out) {
        if (!keysAwarded.add(r)) return;   // each wave is awarded once, ever
        out.add(new DropKey(r, ominous));
        // Q114: personally eligible only if locked at the round's start AND physically present (alive, inside) at clear
        for (UUID u : snap.locked()) { Member m = roster.get(u); if (presentAlive.contains(u) && m != null && !m.dead) entitlements.add(attemptId + ":" + r + ":" + u); }
    }

    private void boundary(Obs obs, List<Action> out) {
        // admit pending players still present and alive; others are dropped (no retroactive anything)
        int next = round + 1;
        List<UUID> admitted = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : new LinkedHashMap<>(pending).entrySet()) {
            UUID u = e.getKey();
            if (obs.insideAlive().contains(u) && !roster.containsKey(u)) { roster.put(u, new Member(u, next)); admitted.add(u); admissions.add(u + "@" + next); }
        }
        pending.clear();
        Set<UUID> active = new LinkedHashSet<>();
        for (Member m : roster.values()) if (!m.dead && obs.insideAlive().contains(m.id)) active.add(m.id);
        beginRound(next, active, out);
    }

    private void beginRound(int r, Set<UUID> active, List<Action> out) {
        round = r;
        phase = Phase.ROUND;
        blockedTicks = 0;
        peak = Math.max(peak, active.size());
        int n = Math.max(1, peak);
        int budget = RoundPlan.scaledBudget(Math.min(r, 6), n);
        if (r == RoundPlan.ROUNDS) budget = 0;
        snap = new Snapshot(r, n, RoundPlan.multiplierMilli(n), budget, n, new ArrayList<>(roster.keySet()));
        allocs.clear();
        if (r <= 6) {
            List<String> plan = RoundComposer.compose(seed, r, budget);
            long counter = 0;
            for (String mob : plan) allocs.add(new Alloc(derive(r, counter++), mob));
        }
        out.add(new RoundStart(r, n, budget));
    }

    private UUID derive(int r, long i) { return new UUID(seed * 1_000_003L + r * 131L + i, attemptId.getLeastSignificantBits() ^ (i + 1) * 0x9E3779B97F4A7C15L); }

    private void end(Outcome o, boolean cleanup, List<Action> out) {
        outcome = o;
        phase = Phase.ENDED;
        out.add(new End(o, cleanup));
    }
}
