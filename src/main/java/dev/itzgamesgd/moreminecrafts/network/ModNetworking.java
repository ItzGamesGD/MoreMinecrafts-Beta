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
        PayloadTypeRegistry.serverboundPlay().register(ProgressionSyncRequestPayload.TYPE, ProgressionSyncRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ResearchPurchaseRequestPayload.TYPE, ResearchPurchaseRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(QuestClaimRequestPayload.TYPE, QuestClaimRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ContractActionPayload.TYPE, ContractActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FishingChallengePayload.TYPE, FishingChallengePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ProgressionSnapshotPayload.TYPE, ProgressionSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ProgressionDeltaPayload.TYPE, ProgressionDeltaPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ProgressionActionResultPayload.TYPE, ProgressionActionResultPayload.CODEC);
        dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRuntime.initialize();
        ServerPlayNetworking.registerGlobalReceiver(MovementJumpPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (!dev.itzgamesgd.moreminecrafts.progression.ResearchMovementRuntime.onJump(context.player()))
                        EnchantmentRuntime.handleJump(context.player());
                }));
        ServerPlayNetworking.registerGlobalReceiver(ProgressionSyncRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRuntime.snapshot(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(ResearchPurchaseRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    var result = dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseService.purchase(
                            context.player(), payload.researchId(), payload.level());
                    dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRuntime.actionResult(
                            context.player(), "research:" + payload.researchId(), result.ordinal());
                }));
        ServerPlayNetworking.registerGlobalReceiver(QuestClaimRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    var result = dev.itzgamesgd.moreminecrafts.progression.QuestProgressService.claim(
                            context.player(), payload.questId());
                    dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRuntime.actionResult(
                            context.player(), "quest:" + payload.questId(), result.ordinal());
                }));
        ServerPlayNetworking.registerGlobalReceiver(ContractActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    var action = ContractActionPayload.parse(payload.action());
                    if (action == null || !dev.itzgamesgd.moreminecrafts.bounty.ContractService.act(
                            context.player(), payload.slot(), payload.contractId(), action))
                        context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "moreminecrafts.contract.action_failed"));
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
