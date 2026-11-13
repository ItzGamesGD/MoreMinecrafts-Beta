package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.pyramid.maptrade.St01MapPlaceholders;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ST-01 P8-D8: an ST-01 explorer-map trade generated from a vanilla trade pool while its search is not prepared yields an
 * unpurchasable placeholder in its slot instead of a synchronous structure search. Every other trade is untouched.
 */
@Mixin(VillagerTrade.class)
abstract class St01VillagerTradePlaceholderMixin {
    @Inject(method = "getOffer", at = @At("HEAD"), cancellable = true)
    private void moreminecrafts$st01Placeholder(LootContext context, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer placeholder = St01MapPlaceholders.intercept((VillagerTrade) (Object) this, context);
        if (placeholder != null) cir.setReturnValue(placeholder);
    }
}
