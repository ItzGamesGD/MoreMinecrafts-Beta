package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchVillagerDiscount;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after vanilla rebuilds this player's special prices and before the offers are sent to them. */
@Mixin(Villager.class)
abstract class ResearchVillagerDiscountMixin {
    @Inject(method = "updateSpecialPrices", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/villager/Villager;getTradingPlayer()Lnet/minecraft/world/entity/player/Player;"))
    private void moreminecrafts$applyTradingResearch(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer)
            ResearchVillagerDiscount.apply(serverPlayer, ((Villager) (Object) this).getOffers());
    }
}
