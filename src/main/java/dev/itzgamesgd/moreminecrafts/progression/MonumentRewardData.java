package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/** World-persistent Ocean Monument completion state, keyed by monument start chunk. */
final class MonumentRewardData extends SavedData {
    private static final Codec<PendingMonument> PENDING_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("key").forGetter(PendingMonument::key),
            Codec.INT.fieldOf("min_x").forGetter(PendingMonument::minX),
            Codec.INT.fieldOf("min_y").forGetter(PendingMonument::minY),
            Codec.INT.fieldOf("min_z").forGetter(PendingMonument::minZ),
            Codec.INT.fieldOf("max_x").forGetter(PendingMonument::maxX),
            Codec.INT.fieldOf("max_y").forGetter(PendingMonument::maxY),
            Codec.INT.fieldOf("max_z").forGetter(PendingMonument::maxZ),
            Codec.DOUBLE.fieldOf("reward_x").forGetter(PendingMonument::rewardX),
            Codec.DOUBLE.fieldOf("reward_y").forGetter(PendingMonument::rewardY),
            Codec.DOUBLE.fieldOf("reward_z").forGetter(PendingMonument::rewardZ)
    ).apply(instance, PendingMonument::new));

    private static final Codec<Participant> PARTICIPANT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("monument").forGetter(Participant::monument),
            UUIDUtil.CODEC.fieldOf("player").forGetter(Participant::player),
            Codec.LONG.fieldOf("last_combat_tick").forGetter(Participant::lastCombatTick),
            Codec.BOOL.fieldOf("elder_damage").forGetter(Participant::elderDamage)
    ).apply(instance, Participant::new));

    private static final Codec<PendingCredit> CREDIT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(PendingCredit::player),
            Codec.LONG.fieldOf("monument").forGetter(PendingCredit::monument)
    ).apply(instance, PendingCredit::new));

    private static final Codec<SavedState> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf("claimed", List.of()).forGetter(SavedState::claimed),
            PENDING_CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(SavedState::pending),
            PARTICIPANT_CODEC.listOf().optionalFieldOf("participants", List.of()).forGetter(SavedState::participants),
            CREDIT_CODEC.listOf().optionalFieldOf("pending_credits", List.of()).forGetter(SavedState::pendingCredits)
    ).apply(instance, SavedState::new));

    private static final Codec<MonumentRewardData> CODEC = Codec.either(Codec.LONG.listOf(), STATE_CODEC)
            .xmap(value -> value.map(MonumentRewardData::fromLegacyList, MonumentRewardData::fromState),
                    data -> Either.right(data.toState()));

    static final SavedDataType<MonumentRewardData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:ocean_monument_rewards"), MonumentRewardData::new, CODEC, DataFixTypes.LEVEL);

    private final Set<Long> claimedMonuments = new HashSet<>();
    private final Map<Long, PendingMonument> pendingMonuments = new LinkedHashMap<>();
    /** Combat evidence per uncompleted monument: monument key to player to evidence. */
    private final Map<Long, Map<UUID, Participant>> participants = new LinkedHashMap<>();
    private final Set<PendingCredit> pendingCredits = new java.util.LinkedHashSet<>();
    static final int MAX_PARTICIPANTS_PER_MONUMENT = 64;
    static final int MAX_MONUMENTS_WITH_EVIDENCE = 256;
    static final int MAX_PENDING_CREDITS = 1024;

    private static MonumentRewardData fromLegacyList(List<Long> values) {
        MonumentRewardData data = new MonumentRewardData();
        data.claimedMonuments.addAll(values);
        return data;
    }

    private static MonumentRewardData fromState(SavedState state) {
        MonumentRewardData data = fromLegacyList(state.claimed());
        for (PendingMonument pending : state.pending()) data.pendingMonuments.put(pending.key(), pending);
        for (Participant participant : state.participants())
            data.participants.computeIfAbsent(participant.monument(), ignored -> new LinkedHashMap<>())
                    .put(participant.player(), participant);
        data.pendingCredits.addAll(state.pendingCredits());
        return data;
    }

    private SavedState toState() {
        List<Participant> flat = new ArrayList<>();
        participants.values().forEach(byPlayer -> flat.addAll(byPlayer.values()));
        return new SavedState(new ArrayList<>(claimedMonuments), new ArrayList<>(pendingMonuments.values()),
                flat, new ArrayList<>(pendingCredits));
    }

    boolean claim(long monumentKey) {
        if (!claimedMonuments.add(monumentKey)) return false;
        setDirty();
        return true;
    }

    boolean isClaimed(long monumentKey) { return claimedMonuments.contains(monumentKey); }

    void defer(long monumentKey, BoundingBox bounds, Vec3 rewardPosition) {
        if (claimedMonuments.contains(monumentKey)) return;
        PendingMonument pending = new PendingMonument(monumentKey, bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ(), rewardPosition.x, rewardPosition.y, rewardPosition.z);
        if (pending.equals(pendingMonuments.get(monumentKey))) return;
        pendingMonuments.put(monumentKey, pending);
        setDirty();
    }

    PendingMonument pending(long monumentKey) { return pendingMonuments.get(monumentKey); }

    List<PendingMonument> pendingMonuments() { return List.copyOf(pendingMonuments.values()); }

    CompletionResult finishIfReady(long monumentKey, boolean allChunksLoaded, boolean livingGuardianRemains) {
        if (!pendingMonuments.containsKey(monumentKey)) return CompletionResult.NOT_PENDING;
        if (claimedMonuments.contains(monumentKey)) {
            pendingMonuments.remove(monumentKey);
            setDirty();
            return CompletionResult.ALREADY_CLAIMED;
        }
        if (!allChunksLoaded) return CompletionResult.WAITING_FOR_CHUNKS;
        if (livingGuardianRemains) {
            pendingMonuments.remove(monumentKey);
            setDirty();
            return CompletionResult.GUARDIANS_REMAIN;
        }
        pendingMonuments.remove(monumentKey);
        claimedMonuments.add(monumentKey);
        setDirty();
        return CompletionResult.CLAIMED;
    }

    /** Records combat inside the monument (or Elder Guardian damage) while the monument is not yet completed. */
    void recordParticipation(long monumentKey, UUID player, long gameTick, boolean elderDamage) {
        if (claimedMonuments.contains(monumentKey)) return;
        Map<UUID, Participant> byPlayer = participants.get(monumentKey);
        if (byPlayer == null) {
            if (participants.size() >= MAX_MONUMENTS_WITH_EVIDENCE)
                participants.remove(participants.keySet().iterator().next());
            byPlayer = new LinkedHashMap<>();
            participants.put(monumentKey, byPlayer);
        }
        Participant old = byPlayer.get(player);
        Participant next = new Participant(monumentKey, player,
                Math.max(old == null ? Long.MIN_VALUE : old.lastCombatTick(), gameTick),
                elderDamage || (old != null && old.elderDamage()));
        if (next.equals(old)) return;
        byPlayer.remove(player);
        byPlayer.put(player, next);
        while (byPlayer.size() > MAX_PARTICIPANTS_PER_MONUMENT) {
            // Evict the oldest evidence that has no Elder Guardian damage; those never expire.
            UUID evict = byPlayer.values().stream().filter(p -> !p.elderDamage())
                    .min(java.util.Comparator.comparingLong(Participant::lastCombatTick))
                    .map(Participant::player).orElse(byPlayer.keySet().iterator().next());
            byPlayer.remove(evict);
        }
        setDirty();
    }

    /**
     * Players with any Elder Guardian damage evidence, plus players whose last verified combat inside the
     * monument is within {@code windowTicks} game ticks before {@code now}.
     */
    List<UUID> qualifiedParticipants(long monumentKey, long now, long windowTicks) {
        Map<UUID, Participant> byPlayer = participants.get(monumentKey);
        if (byPlayer == null) return List.of();
        return byPlayer.values().stream()
                .filter(p -> p.elderDamage() || now - p.lastCombatTick() <= windowTicks)
                .map(Participant::player).toList();
    }

    Participant participant(long monumentKey, UUID player) {
        Map<UUID, Participant> byPlayer = participants.get(monumentKey);
        return byPlayer == null ? null : byPlayer.get(player);
    }

    void clearParticipants(long monumentKey) {
        if (participants.remove(monumentKey) != null) setDirty();
    }

    void addPendingCredit(UUID player, long monumentKey) {
        if (pendingCredits.size() >= MAX_PENDING_CREDITS) return;
        if (pendingCredits.add(new PendingCredit(player, monumentKey))) setDirty();
    }

    List<Long> takePendingCredits(UUID player) {
        List<Long> taken = pendingCredits.stream().filter(c -> c.player().equals(player)).map(PendingCredit::monument).toList();
        if (!taken.isEmpty()) {
            pendingCredits.removeIf(c -> c.player().equals(player));
            setDirty();
        }
        return taken;
    }

    boolean hasPendingCredit(UUID player, long monumentKey) { return pendingCredits.contains(new PendingCredit(player, monumentKey)); }

    enum CompletionResult { NOT_PENDING, WAITING_FOR_CHUNKS, GUARDIANS_REMAIN, CLAIMED, ALREADY_CLAIMED }

    record PendingMonument(long key, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                           double rewardX, double rewardY, double rewardZ) {
        BoundingBox bounds() { return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ); }
        Vec3 rewardPosition() { return new Vec3(rewardX, rewardY, rewardZ); }
    }

    record Participant(long monument, UUID player, long lastCombatTick, boolean elderDamage) {}

    record PendingCredit(UUID player, long monument) {}

    private record SavedState(List<Long> claimed, List<PendingMonument> pending,
                              List<Participant> participants, List<PendingCredit> pendingCredits) {}
}
