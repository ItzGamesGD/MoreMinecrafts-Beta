package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Serverbound action for `.68`/`.70` Potion Mixing/Concentration and `.69` 9+9 Process Control. */
public record BrewingResearchActionPayload(int action) implements CustomPacketPayload {
    public static final int ACTION_MIX_OR_CONCENTRATE = 0;
    public static final int ACTION_TOGGLE_AUTO = 1;
    public static final int ACTION_QUEUE_CARRIED = 2;
    public static final int ACTION_COLLECT_OUTPUTS = 3;

    public static final Type<BrewingResearchActionPayload> TYPE = new Type<>(ModIds.id("brewing_research_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BrewingResearchActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BrewingResearchActionPayload::action,
            BrewingResearchActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
