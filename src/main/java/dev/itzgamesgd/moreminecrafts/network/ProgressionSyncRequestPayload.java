package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionSyncRequestPayload() implements CustomPacketPayload {
    public static final ProgressionSyncRequestPayload INSTANCE = new ProgressionSyncRequestPayload();
    public static final Type<ProgressionSyncRequestPayload> TYPE = new Type<>(ModIds.id("progression_sync_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionSyncRequestPayload> CODEC =
            StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
