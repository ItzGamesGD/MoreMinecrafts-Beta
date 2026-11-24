package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Serverbound action to configure the currently open HopperBlockEntity filter (`hopper_filter`). */
public record HopperFilterPayload(int action, int slotIndex) implements CustomPacketPayload {
    public static final int ACTION_TOGGLE_MODE = 0;
    public static final int ACTION_SET_OR_TOGGLE_SLOT = 1;
    public static final int ACTION_REMOVE_SLOT = 2;
    public static final int ACTION_CLEAR = 3;

    public static final Type<HopperFilterPayload> TYPE = new Type<>(ModIds.id("hopper_filter_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HopperFilterPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HopperFilterPayload::action,
            ByteBufCodecs.VAR_INT, HopperFilterPayload::slotIndex,
            HopperFilterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
