package dev.itzgamesgd.moreminecrafts.network;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record FishingChallengePayload(boolean active) implements CustomPacketPayload {
 public static final Type<FishingChallengePayload> TYPE=new Type<>(ModIds.id("fishing_challenge"));
 public static final StreamCodec<RegistryFriendlyByteBuf,FishingChallengePayload> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,FishingChallengePayload::active,FishingChallengePayload::new);
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
