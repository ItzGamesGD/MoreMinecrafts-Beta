package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.ability.EquipmentAbilityBindings;
import dev.itzgamesgd.moreminecrafts.ability.EquipmentSkillInput;
import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.FishingRodItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(EquipmentSkillPayload.TYPE, EquipmentSkillPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenFishingEquipmentPayload.TYPE, OpenFishingEquipmentPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FishingChallengePayload.TYPE, FishingChallengePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EquipmentSkillPayload.TYPE, (payload, context) -> {
            EquipmentSkillInput input = EquipmentSkillInput.fromNetworkId(payload.inputId());
            if (input != null) {
                EquipmentAbilityBindings.activate(context.player(), input);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(OpenFishingEquipmentPayload.TYPE, (payload, context) -> {
            var player = context.player();
            var hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            var rod = player.getItemInHand(hand);
            if (player.containerMenu == player.inventoryMenu && !FishingChallenges.active(player)
                    && rod.getItem() instanceof FishingRodItem) FishingEquipmentMenu.open(player, rod);
        });
    }
}
