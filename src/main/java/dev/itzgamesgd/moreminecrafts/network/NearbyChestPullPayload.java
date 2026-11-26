package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Serverbound request to pull an ingredient from nearby chests into player inventory (`nearby_chest_ingredients`). */
public record NearbyChestPullPayload(String itemId, int count) implements CustomPacketPayload {
    public static final Type<NearbyChestPullPayload> TYPE = new Type<>(ModIds.id("nearby_chest_pull"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyChestPullPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NearbyChestPullPayload::itemId,
            ByteBufCodecs.VAR_INT, NearbyChestPullPayload::count,
            NearbyChestPullPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
