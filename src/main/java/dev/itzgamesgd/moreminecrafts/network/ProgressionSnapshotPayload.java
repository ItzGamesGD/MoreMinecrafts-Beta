package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionSnapshotPayload(int revision, Map<String,Integer> values) implements CustomPacketPayload {
    public static final Type<ProgressionSnapshotPayload> TYPE = new Type<>(ModIds.id("progression_snapshot"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String,Integer>> VALUES =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionSnapshotPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ProgressionSnapshotPayload::revision,
                    VALUES, ProgressionSnapshotPayload::values, ProgressionSnapshotPayload::new);
    public ProgressionSnapshotPayload { values = Map.copyOf(values); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
