package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Serverbound request generated only by a client left-click miss. */
public record OpenFishingEquipmentPayload(boolean offhand) implements CustomPacketPayload {
    public static final Type<OpenFishingEquipmentPayload> TYPE = new Type<>(ModIds.id("open_fishing_equipment"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFishingEquipmentPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenFishingEquipmentPayload::offhand, OpenFishingEquipmentPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
