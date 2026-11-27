package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A new key edge begun in the air; all eligibility, velocity and cooldown decisions are server-side. */
public record T1AirJumpPayload() implements CustomPacketPayload {
    public static final T1AirJumpPayload INSTANCE = new T1AirJumpPayload();
    public static final Type<T1AirJumpPayload> TYPE = new Type<>(ModIds.id("t1_air_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, T1AirJumpPayload> CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
