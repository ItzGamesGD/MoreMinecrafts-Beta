package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.ability.EquipmentSkillInput;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EquipmentSkillPayload(int inputId) implements CustomPacketPayload {
    public static final Type<EquipmentSkillPayload> TYPE = new Type<>(ModIds.id("equipment_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentSkillPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EquipmentSkillPayload::inputId,
            EquipmentSkillPayload::new
    );

    public EquipmentSkillPayload(EquipmentSkillInput input) {
        this(input.ordinal());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
