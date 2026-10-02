package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.MoreMinecraftsClient;
import dev.itzgamesgd.moreminecrafts.network.OpenFishingEquipmentPayload;
import dev.itzgamesgd.moreminecrafts.network.OpenPotionInjectorPayload;
import dev.itzgamesgd.moreminecrafts.equipment.PotionInjectorItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Converts only an actual attack miss; block breaking and entity attacks never enter this path. */
@Mixin(Minecraft.class)
public abstract class FishingRodInputClientMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$openFishingEquipment(CallbackInfoReturnable<Boolean> result) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.player == null || client.gui.screen() != null) return;
        InteractionHand injectorHand = client.player.getMainHandItem().getItem() instanceof PotionInjectorItem
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (client.player.getItemInHand(injectorHand).getItem() instanceof PotionInjectorItem
                && ClientPlayNetworking.canSend(OpenPotionInjectorPayload.TYPE)) {
            ClientPlayNetworking.send(new OpenPotionInjectorPayload(injectorHand == InteractionHand.OFF_HAND));
            result.setReturnValue(false);
            return;
        }
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.MISS
                || MoreMinecraftsClient.fishingChallengeActive()
                || !ClientPlayNetworking.canSend(OpenFishingEquipmentPayload.TYPE)) return;
        InteractionHand hand = client.player.getMainHandItem().getItem() instanceof FishingRodItem
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!(client.player.getItemInHand(hand).getItem() instanceof FishingRodItem)) return;
        ClientPlayNetworking.send(new OpenFishingEquipmentPayload(hand == InteractionHand.OFF_HAND));
        result.setReturnValue(false);
    }
}
