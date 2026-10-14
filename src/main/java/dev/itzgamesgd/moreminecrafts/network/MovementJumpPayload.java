package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client jump-edge notification; equipment and movement eligibility remain server-authoritative. */
public record MovementJumpPayload() implements CustomPacketPayload {
    public static final MovementJumpPayload INSTANCE = new MovementJumpPayload();
    public static final Type<MovementJumpPayload> TYPE = new Type<>(ModIds.id("movement_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MovementJumpPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
