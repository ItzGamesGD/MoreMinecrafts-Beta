package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Append-only reward and trial-history ledger of one arena (Q113-B, Q114-C, Q115-B). It is never cleared by a new
 * trial, a failure, a reset or a time-out. The shared Trial Key (in each {@link WaveRecord}) is independent of the
 * personal eligibility and claim lists: personal records never multiply the key and never touch vanilla Vaults.
 */
public final class RewardLedger {
    /** Why a player was recorded as eligible: Q114-C (locked at round start AND present in the zone at clear). */
    public static final String BASIS_Q114_C = "Q114-C";

    public record EligibilityRecord(UUID trial, int round, UUID player, String basis, long recordedGameTime) {
        static final Codec<EligibilityRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("trial").forGetter(EligibilityRecord::trial),
                Codec.INT.fieldOf("round").forGetter(EligibilityRecord::round),
                UUIDUtil.CODEC.fieldOf("player").forGetter(EligibilityRecord::player),
                Codec.STRING.optionalFieldOf("basis", BASIS_Q114_C).forGetter(EligibilityRecord::basis),
                Codec.LONG.optionalFieldOf("recorded_game_time", 0L).forGetter(EligibilityRecord::recordedGameTime)
        ).apply(i, EligibilityRecord::new));
    }

    public record ClaimRecord(UUID trial, int round, UUID player, String rewardId, long deliveredGameTime) {
        static final Codec<ClaimRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("trial").forGetter(ClaimRecord::trial),
                Codec.INT.fieldOf("round").forGetter(ClaimRecord::round),
                UUIDUtil.CODEC.fieldOf("player").forGetter(ClaimRecord::player),
                Codec.STRING.fieldOf("reward_id").forGetter(ClaimRecord::rewardId),
                Codec.LONG.optionalFieldOf("delivered_game_time", 0L).forGetter(ClaimRecord::deliveredGameTime)
        ).apply(i, ClaimRecord::new));
    }

    public enum ClaimResult { CLAIMED, NOT_ELIGIBLE, ALREADY_CLAIMED }

    private final List<WaveRecord> waves = new ArrayList<>();
    private final List<EligibilityRecord> eligibility = new ArrayList<>();
    private final List<ClaimRecord> claims = new ArrayList<>();
    private final Set<UUID> releasedTrials = new LinkedHashSet<>();

    public boolean isEmpty() {
        return waves.isEmpty() && eligibility.isEmpty() && claims.isEmpty() && releasedTrials.isEmpty();
    }

    public List<WaveRecord> waves() { return Collections.unmodifiableList(waves); }
    public List<EligibilityRecord> eligibility() { return Collections.unmodifiableList(eligibility); }
    public List<ClaimRecord> claims() { return Collections.unmodifiableList(claims); }
    public Set<UUID> releasedTrials() { return Collections.unmodifiableSet(releasedTrials); }

    @Nullable public WaveRecord wave(UUID trial, int round) {
        for (WaveRecord wave : waves) if (wave.is(trial, round)) return wave;
        return null;
    }

    public boolean hasWave(UUID trial, int round) { return wave(trial, round) != null; }

    /** Adds the wave unless one already exists for {@code (trial, round)}; returns whether it was added. */
    boolean addWave(WaveRecord wave) {
        if (hasWave(wave.trialId(), wave.round())) return false;
        waves.add(wave);
        return true;
    }

    /** Adds the eligibility record unless one already exists for {@code (trial, round, player)}. */
    boolean addEligibility(EligibilityRecord record) {
        if (isEligible(record.trial(), record.round(), record.player())) return false;
        eligibility.add(record);
        return true;
    }

    public boolean isEligible(UUID trial, int round, UUID player) {
        for (EligibilityRecord record : eligibility)
            if (record.trial().equals(trial) && record.round() == round && record.player().equals(player)) return true;
        return false;
    }

    /**
     * Q115-B: records a personal claim, to be called only when an actual personal reward is being delivered to
     * {@code player}. Picking up the shared key, being eligible, or opening a Vault is never a claim. The claim is
     * written before delivery so a repeated call can never deliver twice.
     */
    public ClaimResult tryClaim(UUID trial, int round, UUID player, String rewardId, long gameTime) {
        if (!isEligible(trial, round, player)) return ClaimResult.NOT_ELIGIBLE;
        for (ClaimRecord claim : claims)
            if (claim.trial().equals(trial) && claim.round() == round && claim.player().equals(player)
                    && claim.rewardId().equals(rewardId)) return ClaimResult.ALREADY_CLAIMED;
        claims.add(new ClaimRecord(trial, round, player, rewardId, gameTime));
        return ClaimResult.CLAIMED;
    }

    /** Marks a timed-out trial released (Q116/Q117). The list is append-only. */
    boolean release(UUID trial) { return releasedTrials.add(trial); }

    public boolean isReleased(UUID trial) { return releasedTrials.contains(trial); }

    /** Copies every record of {@code other} that this ledger does not already hold (used to restore an archive). */
    void absorb(RewardLedger other) {
        for (WaveRecord wave : other.waves) addWave(wave);
        for (EligibilityRecord record : other.eligibility) addEligibility(record);
        for (ClaimRecord claim : other.claims)
            if (claims.stream().noneMatch(c -> c.trial().equals(claim.trial()) && c.round() == claim.round()
                    && c.player().equals(claim.player()) && c.rewardId().equals(claim.rewardId()))) claims.add(claim);
        releasedTrials.addAll(other.releasedTrials);
    }

    private record Data(List<WaveRecord> waves, List<EligibilityRecord> eligibility, List<ClaimRecord> claims,
                        List<UUID> released) {}

    /**
     * Duplicate unique keys found on load keep their first record and drop the repeats (the log notes it); no other
     * record is ever dropped.
     */
    public static final Codec<RewardLedger> CODEC = RecordCodecBuilder.<Data>create(i -> i.group(
            WaveRecord.CODEC.listOf().optionalFieldOf("waves", List.of()).forGetter(Data::waves),
            EligibilityRecord.CODEC.listOf().optionalFieldOf("eligibility", List.of()).forGetter(Data::eligibility),
            ClaimRecord.CODEC.listOf().optionalFieldOf("claims", List.of()).forGetter(Data::claims),
            UUIDUtil.CODEC.listOf().optionalFieldOf("released_trials", List.of()).forGetter(Data::released)
    ).apply(i, Data::new)).xmap(RewardLedger::fromData, RewardLedger::toData);

    private Data toData() {
        return new Data(List.copyOf(waves), List.copyOf(eligibility), List.copyOf(claims), List.copyOf(releasedTrials));
    }

    private static RewardLedger fromData(Data data) {
        RewardLedger ledger = new RewardLedger();
        for (WaveRecord wave : data.waves()) ledger.addWave(wave);
        for (EligibilityRecord record : data.eligibility()) ledger.addEligibility(record);
        for (ClaimRecord claim : data.claims())
            if (ledger.claims.stream().noneMatch(c -> c.trial().equals(claim.trial()) && c.round() == claim.round()
                    && c.player().equals(claim.player()) && c.rewardId().equals(claim.rewardId()))) ledger.claims.add(claim);
        ledger.releasedTrials.addAll(data.released());
        return ledger;
    }
}
