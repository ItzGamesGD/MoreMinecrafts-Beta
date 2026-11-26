package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Clientbound summary of available ingredients in chests near a crafting table (`nearby_chest_ingredients`). */
public record NearbyChestSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(String itemId, int totalCount) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::itemId,
                ByteBufCodecs.VAR_INT, Entry::totalCount,
                Entry::new);
    }

    public static final Type<NearbyChestSyncPayload> TYPE = new Type<>(ModIds.id("nearby_chest_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyChestSyncPayload> CODEC = StreamCodec.composite(
            Entry.CODEC.apply(ByteBufCodecs.list(12)), NearbyChestSyncPayload::entries,
            NearbyChestSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
