package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FishingChallengePayload(boolean active, long startTick, int difficulty, int speciesOrdinal) implements CustomPacketPayload {
    public static final Type<FishingChallengePayload> TYPE = new Type<>(ModIds.id("fishing_challenge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FishingChallengePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FishingChallengePayload::active,
                    ByteBufCodecs.VAR_LONG, FishingChallengePayload::startTick,
                    ByteBufCodecs.VAR_INT, FishingChallengePayload::difficulty,
                    ByteBufCodecs.VAR_INT, FishingChallengePayload::speciesOrdinal,
                    FishingChallengePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
