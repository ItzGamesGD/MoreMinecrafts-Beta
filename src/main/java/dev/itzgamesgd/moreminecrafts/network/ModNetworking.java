package dev.itzgamesgd.moreminecrafts.network;

import dev.itzgamesgd.moreminecrafts.fishing.FishingChallenges;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentMenu;
import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.FishingRodItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import dev.itzgamesgd.moreminecrafts.equipment.PotionInjectorItem;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(OpenFishingEquipmentPayload.TYPE, OpenFishingEquipmentPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenPotionInjectorPayload.TYPE, OpenPotionInjectorPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MovementJumpPayload.TYPE, MovementJumpPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FishingChallengePayload.TYPE, FishingChallengePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MovementJumpPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (!dev.itzgamesgd.moreminecrafts.progression.ResearchMovementRuntime.onJump(context.player()))
                        EnchantmentRuntime.handleJump(context.player());
                }));
        ServerPlayNetworking.registerGlobalReceiver(OpenFishingEquipmentPayload.TYPE, (payload, context) -> {
            var player = context.player();
            var hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            var rod = player.getItemInHand(hand);
            if (player.containerMenu == player.inventoryMenu && !FishingChallenges.active(player)
                    && rod.getItem() instanceof FishingRodItem) FishingEquipmentMenu.open(player, rod);
        });
        ServerPlayNetworking.registerGlobalReceiver(OpenPotionInjectorPayload.TYPE, (payload, context) -> {
            var player = context.player();
            var hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            var injector = player.getItemInHand(hand);
            if (player.containerMenu == player.inventoryMenu && injector.getItem() instanceof PotionInjectorItem)
                PotionInjectorItem.open(player, injector);
        });
    }
}
