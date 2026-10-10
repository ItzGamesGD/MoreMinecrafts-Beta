package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ResearchPurchaseRequestPayload(String researchId, int level) implements CustomPacketPayload {
    public static final Type<ResearchPurchaseRequestPayload> TYPE = new Type<>(ModIds.id("research_purchase_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchPurchaseRequestPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ResearchPurchaseRequestPayload::researchId,
                    ByteBufCodecs.VAR_INT, ResearchPurchaseRequestPayload::level, ResearchPurchaseRequestPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
