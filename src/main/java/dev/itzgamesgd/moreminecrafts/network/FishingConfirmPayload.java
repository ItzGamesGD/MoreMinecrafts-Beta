package dev.itzgamesgd.moreminecrafts.network;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record FishingConfirmPayload(int intent) implements CustomPacketPayload {
 public static final Type<FishingConfirmPayload> TYPE=new Type<>(ModIds.id("fishing_confirm"));
 public static final StreamCodec<RegistryFriendlyByteBuf,FishingConfirmPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,FishingConfirmPayload::intent,FishingConfirmPayload::new);
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
