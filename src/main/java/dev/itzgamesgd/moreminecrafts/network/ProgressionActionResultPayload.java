package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionActionResultPayload(String actionId, int resultCode, int revision) implements CustomPacketPayload {
    public static final Type<ProgressionActionResultPayload> TYPE = new Type<>(ModIds.id("progression_action_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionActionResultPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ProgressionActionResultPayload::actionId,
                    ByteBufCodecs.VAR_INT, ProgressionActionResultPayload::resultCode,
                    ByteBufCodecs.VAR_INT, ProgressionActionResultPayload::revision,
                    ProgressionActionResultPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
