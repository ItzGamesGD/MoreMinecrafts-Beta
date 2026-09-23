package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.ability.EquipmentAbilityBindings;
import dev.itzgamesgd.moreminecrafts.ability.EquipmentSkillInput;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(EquipmentSkillPayload.TYPE, EquipmentSkillPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FishingConfirmPayload.TYPE, FishingConfirmPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FishingChallengePayload.TYPE, FishingChallengePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FishingConfirmPayload.TYPE, (payload, context) ->
                dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges.confirm(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(EquipmentSkillPayload.TYPE, (payload, context) -> {
            EquipmentSkillInput input = EquipmentSkillInput.fromNetworkId(payload.inputId());
            if (input != null) {
                EquipmentAbilityBindings.activate(context.player(), input);
            }
        });
    }
}
