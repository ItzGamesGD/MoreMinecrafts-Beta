package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record QuestClaimRequestPayload(String questId) implements CustomPacketPayload {
    public static final Type<QuestClaimRequestPayload> TYPE = new Type<>(ModIds.id("quest_claim_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestClaimRequestPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, QuestClaimRequestPayload::questId,
                    QuestClaimRequestPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
