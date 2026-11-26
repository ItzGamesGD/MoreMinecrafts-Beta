package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Clientbound snapshot of `.69` Brewing Process Control state for the open Brewing Stand. */
public record BrewingResearchSyncPayload(boolean autoEnabled, int queuedCount, int outputCount)
        implements CustomPacketPayload {
    public static final Type<BrewingResearchSyncPayload> TYPE = new Type<>(ModIds.id("brewing_research_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BrewingResearchSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BrewingResearchSyncPayload::autoEnabled,
            ByteBufCodecs.VAR_INT, BrewingResearchSyncPayload::queuedCount,
            ByteBufCodecs.VAR_INT, BrewingResearchSyncPayload::outputCount,
            BrewingResearchSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
