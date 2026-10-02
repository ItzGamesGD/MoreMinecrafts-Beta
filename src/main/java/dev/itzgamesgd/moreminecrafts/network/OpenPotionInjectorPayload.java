package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenPotionInjectorPayload(boolean offhand) implements CustomPacketPayload {
    public static final Type<OpenPotionInjectorPayload> TYPE = new Type<>(ModIds.id("open_potion_injector"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPotionInjectorPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenPotionInjectorPayload::offhand, OpenPotionInjectorPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
