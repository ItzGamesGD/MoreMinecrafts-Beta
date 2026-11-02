package dev.itzgamesgd.moreminecrafts.colosseum;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Per-arena controller: entry edge detection (state-diff, hence duplicate-free), the 78,000-tick cooldown, the current attempt, durable
 * entitlement records and the quarantine flag. Pure and serialisable; the runtime owns all world interaction.
 */
public final class ArenaController {
    public final String arenaId;
    long cooldownLeft;
    ArenaAttempt attempt;
    boolean quarantined;
    String quarantineReason = "";
    String lastOutcome = "NONE";
    final Set<UUID> prevInside = new HashSet<>();
    /** Permanent audit of shared-key round records, including attempts whose live state is later retired. */
    final Set<String> keyHistory = new LinkedHashSet<>();
    /** Permanent reward transaction ledger, keyed by challenge/round/reward ID across every attempt. */
    final java.util.Map<String, RewardDelivery> rewardDeliveries = new java.util.LinkedHashMap<>();
    /** Conflicting persisted copies are retained for operator review while the arena remains isolated. */
    final List<RewardDelivery> rewardConflicts = new ArrayList<>();
    /** Personal eligibility records are kept indefinitely (Q115-B); a personal claim is written only when a reward is actually delivered. */
    final Set<String> entitlements = new LinkedHashSet<>();
    final Set<String> claims = new LinkedHashSet<>();
    /** Attempts whose leftover trial mobs must be removed when their chunks load. */
    final Set<UUID> cleanupAttempts = new LinkedHashSet<>();
    RoundPlan.Timing timing = RoundPlan.Timing.APPROVED;

    public ArenaController(String arenaId) { this.arenaId = arenaId; }

    public void setTiming(RoundPlan.Timing t) { timing = t; if (attempt != null) attempt.setTiming(t); }
    public ArenaAttempt attempt() { return attempt; }
    public long cooldownLeft() { return cooldownLeft; }
    public boolean quarantined() { return quarantined; }
    public String quarantineReason() { return quarantineReason; }
    public String lastOutcome() { return lastOutcome; }
    public Set<String> entitlements() { return entitlements; }
    public Set<UUID> cleanupAttempts() { return cleanupAttempts; }
    public boolean active() { return attempt != null && !attempt.ended(); }

    public List<ArenaAttempt.Action> tick(long nowMillis, Set<UUID> insideAlive, boolean entitiesLoaded, ArenaAttempt.SpawnPlanner planner) {
        List<ArenaAttempt.Action> out = new ArrayList<>();
        Set<UUID> entries = new TreeSet<>(insideAlive);
        entries.removeAll(prevInside);
        Set<UUID> exits = new TreeSet<>(prevInside);
        exits.removeAll(insideAlive);
        prevInside.clear();
        prevInside.addAll(insideAlive);
        if (quarantined) return out;

        if (active()) {
            for (UUID u : exits) attempt.onPlayerGone(u);
            for (UUID u : entries) attempt.onEntry(u);
            List<ArenaAttempt.Action> acts = attempt.tick(nowMillis, new ArenaAttempt.Obs(insideAlive, entitiesLoaded), planner);
            out.addAll(acts);
            finish(acts);
        } else if (cooldownLeft > 0) {
            cooldownLeft--;
        } else if (!entries.isEmpty()) {
            // Gather the whole tick's entrants (everyone physically inside) before committing the round-1 snapshot.
            if (attempt != null) rememberAttemptRecords(attempt);
            UUID id = UUID.nameUUIDFromBytes((arenaId + ":" + nowMillis + ":" + entries).getBytes(StandardCharsets.UTF_8));
            List<ArenaAttempt.Action> acts = new ArrayList<>();
            attempt = ArenaAttempt.start(id, nowMillis, insideAlive, planner, acts);
            attempt.setTiming(timing);
            out.addAll(acts);
        }
        if (attempt != null) {
            rememberAttemptRecords(attempt);
            entitlements.addAll(attempt.entitlements);
        }
        return out;
    }

    private void rememberAttemptRecords(ArenaAttempt a) {
        for (int round : a.keysAwarded) keyHistory.add(a.attemptId + ":round:" + round);
        for (RewardDelivery r : a.rewardDeliveries.values()) {
            RewardDelivery existing=rewardDeliveries.putIfAbsent(r.rewardId,r);
            if(existing!=null && (existing!=r && (!java.util.Objects.equals(existing.challengeId,r.challengeId)
                    || !java.util.Objects.equals(existing.roundId,r.roundId)
                    || !java.util.Objects.equals(existing.itemId,r.itemId)
                    || !java.util.Objects.equals(existing.rewardId,r.rewardId)))) {
                rewardConflicts.add(r);isolate("shared reward ledger identity collision");return;
            }
        }
    }

    private void finish(List<ArenaAttempt.Action> acts) {
        for (ArenaAttempt.Action a : acts) {
            if (!(a instanceof ArenaAttempt.End e)) continue;
            lastOutcome = e.outcome().name();
            if (e.outcome() == ArenaAttempt.Outcome.QUARANTINED) { quarantined = true; quarantineReason = attempt.quarantineReason(); }
            else cooldownLeft = RoundPlan.COOLDOWN_TICKS;
            if (e.cleanup()) cleanupAttempts.add(attempt.attemptId);
        }
    }

    public void onPlayerDeath(UUID p) { if (active()) attempt.onPlayerDeath(p); }
    public void onMobKilled(UUID e) { if (active()) attempt.onMobKilled(e); }
    public void onMobLost(UUID e) { if (active()) attempt.onMobLost(e); }

    /** Operator release of a quarantined arena: drops the isolated attempt (its record is logged by the caller) and allows new attempts. */
    public void isolate(String reason) {
        quarantined = true; quarantineReason = reason; lastOutcome = "QUARANTINED";
        if (active()) { attempt.quarantine(reason, new ArrayList<>()); }
    }

    public void release() {
        if(attempt!=null)rememberAttemptRecords(attempt);
        quarantined = false; quarantineReason = ""; attempt = null; cooldownLeft = 0;
    }

    // ------------------------------------------------------------------ persistence
    static final Gson GSON = new Gson();
    public static final int FORMAT = 1;

    static final class AllocDto { String id, mob, status, entity; int warnLeft, px, py, pz; long lastChunk; boolean chunkKnown; }
    static final class MemberDto { String id; int admitted; boolean dead; }
    static final class SnapDto { int round, n, mult, budget, supply; List<String> locked = new ArrayList<>(); }
    static final class RewardDto { String challenge, round, reward, item, state, entity; boolean positionKnown; double x, y, z; }
    static final class AttemptDto {
        String id, phase, outcome, qreason = "";
        long seed, start, grace, lastSeen, onlineTicks;
        int round, interLeft, blocked, peak;
        boolean bossReq, bossDef, bossChunkKnown, cleanupPending;
        String bossEntity; long bossChunk;
        double bossDeathX, bossDeathY, bossDeathZ;
        float bossHealth=-1; String bossState="";
        TerrainLedger terrain = new TerrainLedger();
        List<MemberDto> roster = new ArrayList<>();
        List<String> pendingIds = new ArrayList<>();
        List<Integer> pendingRounds = new ArrayList<>();
        SnapDto snap;
        List<AllocDto> allocs = new ArrayList<>();
        List<Integer> keys = new ArrayList<>(), supplies = new ArrayList<>();
        List<String> entitlements = new ArrayList<>(), admissions = new ArrayList<>();
        List<Long> chests = new ArrayList<>();
        /** Read compatibility for Stage 1-C receipts written inside the active-attempt DTO. */
        List<RewardDto> rewards = new ArrayList<>();
    }
    static final class Dto {
        int format = FORMAT;
        String arena, lastOutcome = "NONE", qreason = "";
        long cooldown;
        boolean quarantined;
        AttemptDto attempt;
        List<String> entitlements = new ArrayList<>(), claims = new ArrayList<>(), cleanup = new ArrayList<>();
        List<String> keyHistory = new ArrayList<>();
        List<RewardDto> rewards = new ArrayList<>();
        List<RewardDto> rewardConflicts = new ArrayList<>();
    }

    public String toJson() {
        Dto d = new Dto();
        d.arena = arenaId; d.cooldown = cooldownLeft; d.quarantined = quarantined; d.qreason = quarantineReason; d.lastOutcome = lastOutcome;
        d.entitlements.addAll(entitlements); d.claims.addAll(claims);
        d.keyHistory.addAll(keyHistory);
        d.keyHistory.addAll(attempt == null ? List.of() : attempt.keysAwarded.stream().map(r -> attempt.attemptId + ":round:" + r).toList());
        for (RewardDelivery r : rewardDeliveries.values()) d.rewards.add(rewardDto(r));
        for (RewardDelivery r : rewardConflicts) d.rewardConflicts.add(rewardDto(r));
        for (UUID u : cleanupAttempts) d.cleanup.add(u.toString());
        if (attempt != null) d.attempt = dto(attempt);
        return GSON.toJson(d);
    }

    private static AttemptDto dto(ArenaAttempt a) {
        AttemptDto d = new AttemptDto();
        d.id = a.attemptId.toString(); d.seed = a.seed; d.phase = a.phase.name(); d.outcome = a.outcome.name(); d.qreason = a.quarantineReason;
        d.round = a.round; d.start = a.startMillis; d.grace = a.graceExpiryMillis; d.lastSeen = a.lastSeenMillis; d.onlineTicks = a.onlineTicks;
        d.bossEntity = a.bossEntity == null ? null : a.bossEntity.toString(); d.bossChunk = a.bossChunk; d.bossChunkKnown = a.bossChunkKnown; d.cleanupPending = a.cleanupPending;
        d.bossHealth=a.bossHealth; d.bossState=a.bossState;
        d.bossDeathX = a.bossDeathX; d.bossDeathY = a.bossDeathY; d.bossDeathZ = a.bossDeathZ; d.terrain = a.terrain;
        d.interLeft = a.intermissionLeft; d.blocked = a.blockedTicks; d.peak = a.peak; d.bossReq = a.bossRequested; d.bossDef = a.bossDefeated;
        for (ArenaAttempt.Member m : a.roster.values()) { MemberDto x = new MemberDto(); x.id = m.id.toString(); x.admitted = m.admittedRound; x.dead = m.dead; d.roster.add(x); }
        a.pending.forEach((u, r) -> { d.pendingIds.add(u.toString()); d.pendingRounds.add(r); });
        if (a.snap != null) {
            SnapDto s = new SnapDto();
            s.round = a.snap.round(); s.n = a.snap.n(); s.mult = a.snap.multiplierMilli(); s.budget = a.snap.budget(); s.supply = a.snap.supplyChests();
            for (UUID u : a.snap.locked()) s.locked.add(u.toString());
            d.snap = s;
        }
        for (ArenaAttempt.Alloc al : a.allocs) {
            AllocDto x = new AllocDto();
            x.id = al.id.toString(); x.mob = al.mob; x.status = al.status.name(); x.entity = al.entity == null ? null : al.entity.toString();
            x.lastChunk = al.lastChunk; x.chunkKnown = al.chunkKnown;
            x.warnLeft = al.warnLeft; x.px = al.px; x.py = al.py; x.pz = al.pz;
            d.allocs.add(x);
        }
        d.keys.addAll(a.keysAwarded); d.supplies.addAll(a.suppliesPlaced); d.entitlements.addAll(a.entitlements);
        d.chests.addAll(a.supplyChests); d.admissions.addAll(a.admissions);
        return d;
    }

    private static RewardDto rewardDto(RewardDelivery r) {
        RewardDto x=new RewardDto();x.challenge=r.challengeId;x.round=r.roundId;x.reward=r.rewardId;
        x.item=r.itemId;x.state=r.state.name();x.entity=r.entityId;x.positionKnown=r.positionKnown;x.x=r.x;x.y=r.y;x.z=r.z;
        return x;
    }

    public static String checksum(String json) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** Restores a controller. Anything uncertain (bad checksum, unparsable, inconsistent, clock regression) yields a quarantined controller. */
    public static ArenaController restore(String arenaId, String json, String expectedChecksum, long nowMillis) {
        try {
            if (expectedChecksum != null && !expectedChecksum.equals(checksum(json))) return quarantinedShell(arenaId, "checksum mismatch");
            Dto d = GSON.fromJson(json, Dto.class);
            if (d == null || d.format != FORMAT) return quarantinedShell(arenaId, "unknown format");
            if (!arenaId.equals(d.arena)) return quarantinedShell(arenaId, "arena id mismatch");
            if (d.cooldown < 0 || d.cooldown > RoundPlan.COOLDOWN_TICKS) return quarantinedShell(arenaId, "cooldown out of range");
            ArenaController c = new ArenaController(arenaId);
            c.cooldownLeft = d.cooldown; c.quarantined = d.quarantined; c.quarantineReason = d.qreason == null ? "" : d.qreason;
            c.lastOutcome = d.lastOutcome; c.entitlements.addAll(d.entitlements); c.claims.addAll(d.claims);
            if(d.keyHistory!=null)c.keyHistory.addAll(d.keyHistory);
            if(d.rewards!=null)for(RewardDto x:d.rewards) {
                RewardDelivery r=rewardFromDto(x);
                RewardDelivery existing=c.rewardDeliveries.putIfAbsent(r.rewardId,r);
                if(existing!=null) {c.rewardConflicts.add(r);c.quarantined=true;c.quarantineReason="duplicate persisted reward identity";c.lastOutcome="QUARANTINED";}
            }
            if(d.attempt!=null && d.attempt.rewards!=null) for(RewardDto x:d.attempt.rewards) {
                RewardDelivery r=rewardFromDto(x), existing=c.rewardDeliveries.putIfAbsent(r.rewardId,r);
                if(existing!=null && (!sameReward(existing,r))) {c.rewardConflicts.add(r);c.quarantined=true;c.quarantineReason="conflicting persisted reward receipts";c.lastOutcome="QUARANTINED";}
            }
            if(d.rewardConflicts!=null)for(RewardDto x:d.rewardConflicts)c.rewardConflicts.add(rewardFromDto(x));
            if(!c.rewardConflicts.isEmpty()) {c.quarantined=true;c.quarantineReason="conflicting reward records retained for review";c.lastOutcome="QUARANTINED";}
            String ledgerProblem=verifyRewardLedger(c.rewardDeliveries.values());
            if(ledgerProblem!=null) {c.quarantined=true;c.quarantineReason=ledgerProblem;c.lastOutcome="QUARANTINED";}
            for (String u : d.cleanup) c.cleanupAttempts.add(UUID.fromString(u));
            if (d.attempt != null) {
                ArenaAttempt a = attemptFrom(d.attempt);
                for(RewardDelivery r:c.rewardDeliveries.values())if(a.attemptId.toString().equals(r.challengeId))a.rewardDeliveries.put(r.rewardId,r);
                String problem = verify(a);
                if (problem == null && a.phase != ArenaAttempt.Phase.ENDED && nowMillis < a.lastSeenMillis) problem = "wall clock earlier than last saved time";
                if (problem != null) { c.attempt = a; c.quarantined = true; c.quarantineReason = problem; a.outcome = ArenaAttempt.Outcome.QUARANTINED; a.phase = ArenaAttempt.Phase.ENDED; a.quarantineReason = problem; }
                else c.attempt = a;
            }
            return c;
        } catch (RuntimeException e) {
            return quarantinedShell(arenaId, "unreadable state: " + e);
        }
    }

    private static ArenaController quarantinedShell(String arenaId, String reason) {
        ArenaController c = new ArenaController(arenaId);
        c.quarantined = true; c.quarantineReason = reason; c.lastOutcome = "QUARANTINED";
        return c;
    }

    private static ArenaAttempt attemptFrom(AttemptDto d) {
        ArenaAttempt a = new ArenaAttempt(UUID.fromString(d.id), d.seed);
        a.phase = ArenaAttempt.Phase.valueOf(d.phase); a.outcome = ArenaAttempt.Outcome.valueOf(d.outcome); a.quarantineReason = d.qreason;
        a.round = d.round; a.startMillis = d.start; a.graceExpiryMillis = d.grace; a.lastSeenMillis = d.lastSeen; a.onlineTicks = d.onlineTicks;
        a.bossEntity = d.bossEntity == null ? null : UUID.fromString(d.bossEntity); a.bossChunk = d.bossChunk; a.bossChunkKnown = d.bossChunkKnown; a.cleanupPending = d.cleanupPending;
        a.bossHealth=d.bossHealth; a.bossState=d.bossState;
        a.bossDeathX = d.bossDeathX; a.bossDeathY = d.bossDeathY; a.bossDeathZ = d.bossDeathZ; a.terrain = d.terrain == null ? new TerrainLedger() : d.terrain;
        a.intermissionLeft = d.interLeft; a.blockedTicks = d.blocked; a.peak = d.peak; a.bossRequested = d.bossReq; a.bossDefeated = d.bossDef;
        for (MemberDto m : d.roster) { ArenaAttempt.Member x = new ArenaAttempt.Member(UUID.fromString(m.id), m.admitted); x.dead = m.dead; a.roster.put(x.id, x); }
        for (int i = 0; i < d.pendingIds.size(); i++) a.pending.put(UUID.fromString(d.pendingIds.get(i)), d.pendingRounds.get(i));
        if (d.snap != null) {
            List<UUID> locked = new ArrayList<>();
            for (String u : d.snap.locked) locked.add(UUID.fromString(u));
            a.snap = new ArenaAttempt.Snapshot(d.snap.round, d.snap.n, d.snap.mult, d.snap.budget, d.snap.supply, locked);
        }
        for (AllocDto x : d.allocs) {
            ArenaAttempt.Alloc al = new ArenaAttempt.Alloc(UUID.fromString(x.id), x.mob);
            al.status = ArenaAttempt.AllocStatus.valueOf(x.status); al.entity = x.entity == null ? null : UUID.fromString(x.entity);
            al.lastChunk = x.lastChunk; al.chunkKnown = x.chunkKnown;
            al.warnLeft = x.warnLeft; al.px = x.px; al.py = x.py; al.pz = x.pz;
            a.allocs.add(al);
        }
        a.keysAwarded.addAll(d.keys); a.suppliesPlaced.addAll(d.supplies); a.entitlements.addAll(d.entitlements);
        a.supplyChests.addAll(d.chests); a.admissions.addAll(d.admissions);
        return a;
    }

    private static RewardDelivery rewardFromDto(RewardDto x) {
        RewardDelivery r=new RewardDelivery();r.challengeId=x.challenge;r.roundId=x.round;r.rewardId=x.reward;
        r.itemId=x.item;r.state=RewardDelivery.State.valueOf(x.state);r.entityId=x.entity;
        r.positionKnown=x.positionKnown;r.x=x.x;r.y=x.y;r.z=x.z;
        if(r.rewardId==null)throw new IllegalArgumentException("reward ID is missing");
        return r;
    }

    private static boolean sameReward(RewardDelivery a,RewardDelivery b) {
        return java.util.Objects.equals(a.challengeId,b.challengeId) && java.util.Objects.equals(a.roundId,b.roundId)
                && java.util.Objects.equals(a.rewardId,b.rewardId) && java.util.Objects.equals(a.itemId,b.itemId)
                && a.state==b.state && java.util.Objects.equals(a.entityId,b.entityId)
                && a.positionKnown==b.positionKnown && a.x==b.x && a.y==b.y && a.z==b.z;
    }

    private static String verifyRewardLedger(Collection<RewardDelivery> records) {
        Set<String> ids=new HashSet<>(),rounds=new HashSet<>();
        for(RewardDelivery r:records) {
            if(r.state==null || r.rewardId==null || !ids.add(r.rewardId) || r.challengeId==null) return "reward identity mismatch";
            UUID challenge;
            int round;
            try {challenge=UUID.fromString(r.challengeId);round=Integer.parseInt(r.roundId);}catch(RuntimeException e){return "invalid reward challenge/round";}
            if(round<1 || round>7 || !rounds.add(challenge+":"+round))return "duplicate or invalid shared-key round";
            if(!r.matches(challenge,round,round==7))return "reward kind/identity mismatch";
            if((r.state==RewardDelivery.State.INSERTING || r.state==RewardDelivery.State.DELIVERED)
                    && (r.entityId==null || !r.entityId.equals(RewardDelivery.entityUuid(r.rewardId).toString()) || !r.validPosition()))return "invalid recorded reward result";
            if(r.state==RewardDelivery.State.PLANNED && r.entityId!=null)return "planned reward has an entity ID";
        }
        return null;
    }

    /** Consistency verification on restore (Q37/Q38): roster, round, locked snapshot and the persisted spawn plan must agree. */
    static String verify(ArenaAttempt a) {
        if (a.keysAwarded.stream().anyMatch(r -> r < 1 || r > RoundPlan.ROUNDS)) return "key record out of range";
        Set<String> rewardIds = new HashSet<>();
        Set<Integer> rewardRounds = new HashSet<>();
        for (RewardDelivery r : a.rewardDeliveries.values()) {
            if (r.state == null || r.rewardId == null || !rewardIds.add(r.rewardId) || !a.attemptId.toString().equals(r.challengeId)) return "reward identity mismatch";
            int round;
            try { round = Integer.parseInt(r.roundId); } catch (RuntimeException e) { return "invalid reward round"; }
            if (!rewardRounds.add(round)) return "duplicate shared key for round";
            boolean ominous = round == 7;
            if (!r.matches(a.attemptId, round, ominous) || !a.keysAwarded.contains(round)) return "reward plan not backed by key record";
            if ((r.state == RewardDelivery.State.INSERTING || r.state == RewardDelivery.State.DELIVERED)
                    && (r.entityId == null || !r.entityId.equals(RewardDelivery.entityUuid(r.rewardId).toString()))) return "invalid recorded reward entity";
            if ((r.state == RewardDelivery.State.INSERTING || r.state == RewardDelivery.State.DELIVERED) && !r.validPosition()) return "invalid recorded reward position";
            if (r.state == RewardDelivery.State.PLANNED && r.entityId != null) return "planned reward already has entity identity";
        }
        if (a.phase == ArenaAttempt.Phase.ENDED) return null;
        if (a.round < 1 || a.round > RoundPlan.ROUNDS) return "round out of range";
        if (a.roster.isEmpty()) return "empty roster";
        if (a.snap == null || a.snap.round() != a.round) return "round snapshot missing or stale";
        if (a.peak < 1 || a.snap.n() != a.peak) return "peak active count inconsistent with snapshot";
        if (a.round <= 6 && a.snap.budget() != RoundPlan.scaledBudget(a.round, a.snap.n())) return "snapshot budget does not match M(n)";
        for (UUID u : a.snap.locked()) if (!a.roster.containsKey(u)) return "locked participant missing from roster";
        if (a.phase == ArenaAttempt.Phase.ROUND && a.round <= 6) {
            List<String> plan = new ArrayList<>(RoundComposer.compose(a.seed, a.round, a.snap.budget()));
            List<String> have = new ArrayList<>();
            Set<UUID> allocationIds = new HashSet<>(), entityIds = new HashSet<>();
            for (ArenaAttempt.Alloc al : a.allocs) {
                if (!allocationIds.add(al.id)) return "duplicate allocation identity";
                if (al.entity != null && !entityIds.add(al.entity)) return "duplicate entity identity";
                have.add(al.mob);
                if (al.status == ArenaAttempt.AllocStatus.SPAWNED && al.entity == null) return "spawned allocation without entity id";
            }
            java.util.Collections.sort(plan); java.util.Collections.sort(have);
            if (!plan.equals(have)) return "persisted spawn plan does not match deterministic composition";
        }
        if (a.phase == ArenaAttempt.Phase.INTERMISSION && !a.allocs.isEmpty()) return "allocations during intermission";
        return null;
    }
}
