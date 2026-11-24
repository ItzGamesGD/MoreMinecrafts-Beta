package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.HopperFilterState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Clientbound snapshot of the currently open HopperBlockEntity's filter state. */
public record HopperFilterSyncPayload(boolean whitelist, List<HopperFilterState.Entry> entries)
        implements CustomPacketPayload {
    public static final Type<HopperFilterSyncPayload> TYPE = new Type<>(ModIds.id("hopper_filter_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, HopperFilterState.Entry> ENTRY_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, HopperFilterState.Entry::itemId,
                    ByteBufCodecs.BOOL, HopperFilterState.Entry::matchComponents,
                    ByteBufCodecs.VAR_INT, HopperFilterState.Entry::componentHash,
                    HopperFilterState.Entry::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, HopperFilterSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HopperFilterSyncPayload::whitelist,
            ENTRY_CODEC.apply(ByteBufCodecs.list(HopperFilterState.MAX_ENTRIES)), HopperFilterSyncPayload::entries,
            HopperFilterSyncPayload::new);

    public HopperFilterSyncPayload(HopperFilterState state) {
        this(state.whitelist(), new ArrayList<>(state.entries()));
    }

    public HopperFilterState toState() {
        return new HopperFilterState(whitelist, entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
