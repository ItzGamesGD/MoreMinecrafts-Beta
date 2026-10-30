package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

/**
 * One cleared wave: the once-only payout record (Q51), unique per {@code (trial, round)}. It carries the players
 * locked at the round's start and those present when it was cleared, and the state of the wave's single shared
 * Trial Key (Q113-B). The identity fields never change; only the key record advances.
 */
public final class WaveRecord {
    /**
     * Shared-key lifecycle. {@code NONE}: no key mechanism existed for this wave (migrated v0 data); never issued.
     * {@code PENDING}: decided, not yet created. {@code SPAWNED}: item entity added, disk copy unconfirmed.
     * {@code CONFIRMED}: the item entity is known to have reached chunk storage. {@code CONSUMED}: the item entity
     * left the world normally (picked up, merged, despawned); never reissued and never reclaimed (Q97).
     */
    public enum KeyState { NONE, PENDING, SPAWNED, CONFIRMED, CONSUMED }

    public static final class KeyRecord {
        KeyState state;
        @Nullable UUID entity;
        long chunk;
        /** Diagnostic: 1 for a normal issue, 2 or more after a crash-loss reissue. */
        int issueCount;

        KeyRecord(KeyState state, @Nullable UUID entity, long chunk, int issueCount) {
            this.state = state;
            this.entity = entity;
            this.chunk = chunk;
            this.issueCount = issueCount;
        }

        public KeyState state() { return state; }
        @Nullable public UUID entity() { return entity; }
        public long chunk() { return chunk; }
        public int issueCount() { return issueCount; }

        static final Codec<KeyRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.xmap(KeyState::valueOf, KeyState::name).fieldOf("state").forGetter(KeyRecord::state),
                UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(k -> Optional.ofNullable(k.entity)),
                Codec.LONG.optionalFieldOf("chunk", 0L).forGetter(KeyRecord::chunk),
                Codec.INT.optionalFieldOf("issue_count", 0).forGetter(KeyRecord::issueCount)
        ).apply(i, (state, entity, chunk, issues) -> new KeyRecord(state, entity.orElse(null), chunk, issues)));
    }

    private final UUID trialId;
    private final long serial;
    private final int round;
    private final List<UUID> lockedParticipants;
    private final List<UUID> presentAtClear;
    private final long clearedGameTime;
    private final KeyRecord key;

    WaveRecord(UUID trialId, long serial, int round, List<UUID> locked, List<UUID> present, long clearedGameTime,
               KeyRecord key) {
        this.trialId = trialId;
        this.serial = serial;
        this.round = round;
        this.lockedParticipants = List.copyOf(locked);
        this.presentAtClear = List.copyOf(present);
        this.clearedGameTime = clearedGameTime;
        this.key = key;
    }

    public static WaveRecord cleared(UUID trialId, long serial, int round, List<UUID> locked, List<UUID> present,
                                     long gameTime, KeyState initialKey) {
        return new WaveRecord(trialId, serial, round, locked, present, gameTime, new KeyRecord(initialKey, null, 0L, 0));
    }

    public UUID trialId() { return trialId; }
    public long serial() { return serial; }
    public int round() { return round; }
    public List<UUID> lockedParticipants() { return lockedParticipants; }
    public List<UUID> presentAtClear() { return presentAtClear; }
    public long gameTime() { return clearedGameTime; }
    public KeyRecord key() { return key; }

    public boolean is(UUID trial, int round) { return trialId.equals(trial) && this.round == round; }

    static final Codec<WaveRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("trial").forGetter(WaveRecord::trialId),
            Codec.LONG.optionalFieldOf("serial", 0L).forGetter(WaveRecord::serial),
            Codec.INT.fieldOf("round").forGetter(WaveRecord::round),
            UUIDUtil.CODEC.listOf().fieldOf("locked").forGetter(WaveRecord::lockedParticipants),
            UUIDUtil.CODEC.listOf().fieldOf("present").forGetter(WaveRecord::presentAtClear),
            Codec.LONG.fieldOf("game_time").forGetter(WaveRecord::gameTime),
            KeyRecord.CODEC.optionalFieldOf("key").forGetter(w -> Optional.of(w.key))
    ).apply(i, (trial, serial, round, locked, present, time, key) ->
            new WaveRecord(trial, serial, round, locked, present, time,
                    key.orElseGet(() -> new KeyRecord(KeyState.NONE, null, 0L, 0)))));
}
