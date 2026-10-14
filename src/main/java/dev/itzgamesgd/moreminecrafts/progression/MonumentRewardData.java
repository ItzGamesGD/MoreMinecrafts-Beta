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
import net.minecraft.core.BlockPos;
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

    private static final Codec<SavedState> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf("claimed", List.of()).forGetter(SavedState::claimed),
            PENDING_CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(SavedState::pending)
    ).apply(instance, SavedState::new));

    private static final Codec<MonumentRewardData> CODEC = Codec.either(Codec.LONG.listOf(), STATE_CODEC)
            .xmap(value -> value.map(MonumentRewardData::fromLegacyList, MonumentRewardData::fromState),
                    data -> Either.right(data.toState()));

    static final SavedDataType<MonumentRewardData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:ocean_monument_rewards"), MonumentRewardData::new, CODEC, DataFixTypes.LEVEL);

    private final Set<Long> claimedMonuments = new HashSet<>();
    private final Map<Long, PendingMonument> pendingMonuments = new LinkedHashMap<>();

    private static MonumentRewardData fromLegacyList(List<Long> values) {
        MonumentRewardData data = new MonumentRewardData();
        data.claimedMonuments.addAll(values);
        return data;
    }

    private static MonumentRewardData fromState(SavedState state) {
        MonumentRewardData data = fromLegacyList(state.claimed());
        for (PendingMonument pending : state.pending()) data.pendingMonuments.put(pending.key(), pending);
        return data;
    }

    private SavedState toState() {
        return new SavedState(new ArrayList<>(claimedMonuments), new ArrayList<>(pendingMonuments.values()));
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

    enum CompletionResult { NOT_PENDING, WAITING_FOR_CHUNKS, GUARDIANS_REMAIN, CLAIMED, ALREADY_CLAIMED }

    record PendingMonument(long key, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                           double rewardX, double rewardY, double rewardZ) {
        BoundingBox bounds() { return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ); }
        Vec3 rewardPosition() { return new Vec3(rewardX, rewardY, rewardZ); }
    }

    private record SavedState(List<Long> claimed, List<PendingMonument> pending) {}
}
