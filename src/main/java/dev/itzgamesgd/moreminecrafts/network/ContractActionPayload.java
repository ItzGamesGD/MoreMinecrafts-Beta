package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.bounty.ContractService;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client request for one contract action. The server re-validates every field before acting. */
public record ContractActionPayload(String contractId, String action, int slot) implements CustomPacketPayload {
    public static final Type<ContractActionPayload> TYPE = new Type<>(ModIds.id("contract_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContractActionPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ContractActionPayload::contractId,
                    ByteBufCodecs.STRING_UTF8, ContractActionPayload::action,
                    ByteBufCodecs.VAR_INT, ContractActionPayload::slot, ContractActionPayload::new);

    /** Returns null for any name that is not a known action. */
    public static ContractService.Action parse(String name) {
        for (ContractService.Action action : ContractService.Action.values())
            if (action.name().equals(name)) return action;
        return null;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
