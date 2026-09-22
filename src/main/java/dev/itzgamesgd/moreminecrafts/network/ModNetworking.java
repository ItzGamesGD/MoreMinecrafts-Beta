package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.ability.EquipmentAbilityBindings;
import dev.itzgamesgd.moreminecrafts.ability.EquipmentSkillInput;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(EquipmentSkillPayload.TYPE, EquipmentSkillPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EquipmentSkillPayload.TYPE, (payload, context) -> {
            EquipmentSkillInput input = EquipmentSkillInput.fromNetworkId(payload.inputId());
            if (input != null) {
                EquipmentAbilityBindings.activate(context.player(), input);
            }
        });
    }
}
