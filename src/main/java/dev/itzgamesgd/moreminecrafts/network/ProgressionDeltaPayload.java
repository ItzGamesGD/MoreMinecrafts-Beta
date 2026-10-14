package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionDeltaPayload(int revision, Map<String,Integer> changes) implements CustomPacketPayload {
    public static final Type<ProgressionDeltaPayload> TYPE = new Type<>(ModIds.id("progression_delta"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String,Integer>> CHANGES =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionDeltaPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ProgressionDeltaPayload::revision,
                    CHANGES, ProgressionDeltaPayload::changes, ProgressionDeltaPayload::new);
    public ProgressionDeltaPayload { changes = Map.copyOf(changes); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
