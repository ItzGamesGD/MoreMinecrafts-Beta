package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.XpQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records XP Quest evidence on confirmed server-side Villager trades. */
@Mixin(AbstractVillager.class)
abstract class QuestVillagerTradeMixin {
    @Inject(method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V", at = @At("HEAD"))
    private void moreminecrafts$trackXpQuestTrade(MerchantOffer offer, CallbackInfo ci) {
        if ((Object) this instanceof Villager villager
                && villager.getTradingPlayer() instanceof ServerPlayer player
                && !villager.level().isClientSide()) {
            XpQuestHooks.villagerTraded(player, villager, offer);
        }
    }
}
