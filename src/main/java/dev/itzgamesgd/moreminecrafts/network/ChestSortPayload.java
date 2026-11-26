package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChestSortPayload(int mode) implements CustomPacketPayload {
    public static final Type<ChestSortPayload> TYPE = new Type<>(ModIds.id("chest_sort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestSortPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ChestSortPayload::mode, ChestSortPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
