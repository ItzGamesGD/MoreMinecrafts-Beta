package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.T1Supply;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reapplies the fixed retained-book price after vanilla reputation/hero modifiers. */
@Mixin(Villager.class)
abstract class T1LibrarianPriceMixin {
    @Inject(method="updateSpecialPrices(Lnet/minecraft/world/entity/player/Player;)V", at=@At("RETURN"))
    private void moreminecrafts$fixedRetainedBookPrice(Player player, CallbackInfo ci) {
        T1Supply.enforceFixedBookPrices(((Villager)(Object)this).getOffers());
    }
}
